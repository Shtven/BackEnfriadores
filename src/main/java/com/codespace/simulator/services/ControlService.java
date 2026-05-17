package com.codespace.simulator.services;

import com.codespace.simulator.entities.EventoPuerta;
import com.codespace.simulator.entities.IntervencionManual;
import com.codespace.simulator.mqtt.MqttPublisher;
import com.codespace.simulator.repositories.EventoPuertaRepository;
import com.codespace.simulator.repositories.IntervencionManualRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class ControlService {

    private static final Logger log = LoggerFactory.getLogger(ControlService.class);

    // ── Umbrales Sprint 3 ─────────────────────────────────────────
    private static final double UMBRAL_CIERRE_AUTOMATICO = 4.0;  // T-07-02
    private static final int    DELAY_CIERRE_SEG         = 5;    // T-07-03
    private static final int    DELAY_REF_FORZADA_MIN    = 10;   // T-10-05

    // ── Configuración Sprint 2 ────────────────────────────────────
    @Value("${control.cortina.desactivar.delay-seg:5}")
    private int cortinaCierreDelaySeg;

    @Value("${control.refrigeracion.retorno.delay-seg:30}")
    private int refrigeracionRetornoDelaySeg;

    @Value("${control.refrigeracion.potencia.alta:100}")
    private int potenciaAlta;

    @Value("${control.refrigeracion.potencia.base:60}")
    private int potenciaBase;

    @Value("${control.sistema.operador-id:1}")
    private Integer sistemaOperadorId;

    // ── Scheduler ─────────────────────────────────────────────────
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(6);

    // ── Mapas de estado Sprint 2 ──────────────────────────────────
    private final Map<Integer, ScheduledFuture<?>> pendientesCortina       = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> pendientesRefrigeracion = new ConcurrentHashMap<>();

    // ── Mapas de estado Sprint 3 ──────────────────────────────────
    private final Map<Integer, Double>           temperaturaActual      = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean>          presenciaActual        = new ConcurrentHashMap<>();
    // Cache del último estado de puerta publicado, usado por ajustarRefrigeracion
    // para inferir el motivo (PUERTA_ABIERTA vs NORMAL) sin recalcular.
    private final Map<Integer, String>           estadoPuertaActual     = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> pendientesCierrePuerta = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> pendientesRefForzada   = new ConcurrentHashMap<>();

    // ── Dependencias ──────────────────────────────────────────────
    private final MqttPublisher                mqttPublisher;
    private final EventoPuertaRepository       eventoPuertaRepository;
    private final IntervencionManualRepository intervencionManualRepository;
    private final ObjectMapper                 objectMapper;
    private final RefrigeracionService         refrigeracionService;

    public ControlService(MqttPublisher mqttPublisher,
                          EventoPuertaRepository eventoPuertaRepository,
                          IntervencionManualRepository intervencionManualRepository,
                          ObjectMapper objectMapper,
                          RefrigeracionService refrigeracionService) {
        this.mqttPublisher                = mqttPublisher;
        this.eventoPuertaRepository       = eventoPuertaRepository;
        this.intervencionManualRepository = intervencionManualRepository;
        this.objectMapper                 = objectMapper;
        this.refrigeracionService         = refrigeracionService;
    }

    // ════════════════════════════════════════════════════════════════
    //  SPRINT 2 — Lógica de puerta por presencia
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public void procesarEventoPuerta(Integer cuartoId, Boolean presencia,
                                     String sensorId, Double temperatura) {
        if (presencia == null) return;
        if (Boolean.TRUE.equals(presencia)) manejarPuertaAbierta(cuartoId, temperatura);
        else                                manejarPuertaCerrada(cuartoId, temperatura);
    }

    private void manejarPuertaAbierta(Integer cuartoId, Double temperatura) {
        publicarEstadoPuerta(cuartoId, "abierta");
        cancelarPendiente(pendientesCortina,       cuartoId);
        cancelarPendiente(pendientesRefrigeracion, cuartoId);
        activarCortina(cuartoId);
        persistirEventoPuerta(cuartoId, "apertura", temperatura, true, cuartoId);
        ajustarRefrigeracion(cuartoId, potenciaAlta);
        persistirIntervencionConRol(cuartoId, sistemaOperadorId,
                "ajuste_automatico_potencia",
                "Puerta abierta → refrigeración subida a " + potenciaAlta + "%",
                "operador");
        log.info("[Cuarto {}] Puerta ABIERTA — cortina activada, ref {}%", cuartoId, potenciaAlta);
        refrigeracionService.subirPotencia(cuartoId);
    }

    private void manejarPuertaCerrada(Integer cuartoId, Double temperatura) {
        persistirEventoPuerta(cuartoId, "cierre", temperatura, false, cuartoId);

        ScheduledFuture<?> futuroCortina = scheduler.schedule(() -> {
            desactivarCortina(cuartoId);
            refrigeracionService.restaurarPotencia(cuartoId);
            log.info("[Cuarto {}] Cortina desactivada ({}s tras cierre)", cuartoId, cortinaCierreDelaySeg);
        }, cortinaCierreDelaySeg, TimeUnit.SECONDS);
        pendientesCortina.put(cuartoId, futuroCortina);

        ScheduledFuture<?> futuroRef = scheduler.schedule(() -> {
            ajustarRefrigeracion(cuartoId, potenciaBase);
            persistirIntervencionConRolAsync(cuartoId, sistemaOperadorId,
                    "ajuste_automatico_potencia",
                    "Puerta cerrada " + refrigeracionRetornoDelaySeg + "s → ref retornada a " + potenciaBase + "%",
                    "operador");
        }, refrigeracionRetornoDelaySeg, TimeUnit.SECONDS);
        pendientesRefrigeracion.put(cuartoId, futuroRef);
    }

    // ════════════════════════════════════════════════════════════════
    //  SPRINT 3 — T-07: Cierre automático por temperatura + presencia
    // ════════════════════════════════════════════════════════════════

    /**
     * T-07-02: Llamado desde MqttSubscriberService cada vez que llega
     * una lectura de temperatura. Actualiza estado y evalúa condición de cierre.
     */
    public void actualizarTemperatura(Integer cuartoId, Double temperatura) {
        temperaturaActual.put(cuartoId, temperatura);
        evaluarCondicionCierrePuerta(cuartoId);
    }

    /**
     * T-07-02 / T-07-03 cancel: Llamado desde MqttSubscriberService cuando
     * llega un evento de presencia.
     * - presencia=false → evalúa si iniciar ciclo de cierre automático
     * - presencia=true  → cancela ciclo en curso si lo hay
     */
    public void actualizarPresencia(Integer cuartoId, Boolean presencia, String sensorId) {
        presenciaActual.put(cuartoId, presencia);
        String estadoPuerta = estadoPuertaActual.getOrDefault(cuartoId, "cerrada");

        if (Boolean.TRUE.equals(presencia)) {
            // Alguien entró → cancelar cierre auto en curso si lo hay
            cancelarCiclosCierrePuerta(cuartoId);
            // Edge-trigger: solo abrir la puerta si NO estaba ya abierta.
            // Antes esto se llamaba en cada evento de presencia=true, lo que
            // re-abría la puerta inmediatamente después de un cierre auto
            // (DEF-02) y disparaba la cortina/refrigeración en cada ciclo.
            if (!"abierta".equals(estadoPuerta)) {
                manejarPuertaAbierta(cuartoId, temperaturaActual.get(cuartoId));
            }
        } else {
            // Sin presencia: solo evaluar cierre si la puerta estaba abierta.
            // No procesamos cierre cuando ya está cerrada o en proceso.
            if ("abierta".equals(estadoPuerta)) {
                evaluarCondicionCierrePuerta(cuartoId);
            }
        }
    }

    /** T-07-02: Condición = temp > 4°C AND presencia=false AND sin ciclo activo */
    private void evaluarCondicionCierrePuerta(Integer cuartoId) {
        Double  temp      = temperaturaActual.get(cuartoId);
        Boolean presencia = presenciaActual.get(cuartoId);

        // Esperar hasta tener AMBOS valores reales para no decidir con presencia=true
        // por defecto cuando aún no llega su primer evento del simulador.
        if (temp == null || presencia == null) return;

        boolean cicloActivo = pendientesCierrePuerta.containsKey(cuartoId)
                && !pendientesCierrePuerta.get(cuartoId).isDone();

        if (temp > UMBRAL_CIERRE_AUTOMATICO && Boolean.FALSE.equals(presencia) && !cicloActivo) {
            log.warn("[Cuarto {}] Condición T-07 cumplida: temp={}°C presencia=false → iniciando cierre",
                    cuartoId, temp);
            iniciarCicloCierrePuerta(cuartoId);
        }
    }

    /**
     * T-07-03: Ciclo de cierre automático.
     * 1. Publica estado='cerrando'
     * 2. Espera 5 s
     * 3. Si llegó presencia=true → ya fue cancelado en actualizarPresencia
     * 4. Si completa → publica estado='cerrada'
     */
    private void iniciarCicloCierrePuerta(Integer cuartoId) {
        // Paso 1: publicar 'cerrando' + persistir  (T-07-04)
        publicarEstadoPuerta(cuartoId, "cerrando");
        persistirEventoPuerta(cuartoId, "cierre_automatico_iniciado",
                temperaturaActual.get(cuartoId), false, cuartoId);

        // Paso 2: temporizador 5 s
        ScheduledFuture<?> futuro = scheduler.schedule(() -> {
            // Solo ejecuta si no fue cancelado
            publicarEstadoPuerta(cuartoId, "cerrada");
            persistirEventoPuertaAsync(cuartoId, "cierre_automatico_completado",
                    temperaturaActual.get(cuartoId), false, cuartoId);
            pendientesCierrePuerta.remove(cuartoId);
            // Sincronizar actuadores: cortina off + refrigeración a base.
            // Sin esto la cortina queda activa tras el cierre (DEF-07 análogo).
            desactivarCortina(cuartoId);
            refrigeracionService.restaurarPotencia(cuartoId);
            log.info("[Cuarto {}] Ciclo de cierre COMPLETADO", cuartoId);
        }, DELAY_CIERRE_SEG, TimeUnit.SECONDS);

        pendientesCierrePuerta.put(cuartoId, futuro);
    }

    /** T-07-03: Cancela ciclo si llega presencia=true */
    private void cancelarCiclosCierrePuerta(Integer cuartoId) {
        ScheduledFuture<?> futuro = pendientesCierrePuerta.remove(cuartoId);
        if (futuro != null && !futuro.isDone()) {
            futuro.cancel(false);
            publicarEstadoPuerta(cuartoId, "cierre_cancelado");
            persistirEventoPuertaAsync(cuartoId, "cierre_automatico_cancelado",
                    temperaturaActual.get(cuartoId), true, cuartoId);

            // ← AGREGAR: persistir en intervenciones_manuales
            persistirIntervencionConRolAsync(cuartoId, sistemaOperadorId,
                    "cancelar_cierre",
                    "Cierre automático cancelado — presencia detectada",
                    "operador");

            log.info("[Cuarto {}] Ciclo de cierre CANCELADO", cuartoId);
        }
    }

    /** Publica en sei/cuartos/N/puerta con el estado dado y origen=automatico */
    private void publicarEstadoPuerta(Integer cuartoId, String estado) {
        publicarEstadoPuerta(cuartoId, estado, "automatico");
    }

    private void publicarEstadoPuerta(Integer cuartoId, String estado, String origen) {
        estadoPuertaActual.put(cuartoId, estado);
        String topic = "sei/cuartos/" + cuartoId + "/puerta";
        publicar(topic, Map.of(
                "cuarto_id", cuartoId,
                "estado",    estado,
                "origen",    origen,
                "timestamp", Instant.now().toString()
        ));
    }

    // ════════════════════════════════════════════════════════════════
    //  SPRINT 3 — T-10: Refrigeración forzada por operador
    // ════════════════════════════════════════════════════════════════

    /**
     * T-10-03: Inicia refrigeración forzada al 100%.
     * T-10-04: Persiste en intervenciones_manuales con rol_en_ejecucion='operador'.
     * T-10-05: Temporizador de 10 min que revierte automáticamente.
     */
    @Transactional
    public void forzarRefrigeracion(Integer cuartoId, Integer operadorId) {

        // Cancelar si ya había un forzado activo para este cuarto
        cancelarPendiente(pendientesRefForzada, cuartoId);

        // T-10-03: publicar estado resultante de refrigeración al 100%.
        // El back publica en /refrigeracion/estado (no /cmd, que es el topic
        // de comandos entrantes desde el HMI). El motivo viaja como
        // FORZADO_MANUAL para que el HMI muestre el badge correcto.
        String topic = "sei/cuartos/" + cuartoId + "/refrigeracion/estado";
        publicar(topic, Map.of(
                "cuarto_id",    cuartoId,
                "potencia_pct", 100,
                "motivo",       "FORZADO_MANUAL",
                "operador_id",  operadorId,
                "timestamp",    Instant.now().toString()
        ));
        log.info("[Cuarto {}] Refrigeración FORZADA al 100% por operador {}", cuartoId, operadorId);

        // T-10-04: persistir intervención con rol_en_ejecucion='operador'
        persistirIntervencionConRol(cuartoId, operadorId,
                "forzar_refrigeracion",
                "Operador forzó refrigeración al 100% manualmente",
                "operador");

        // T-10-05: temporizador de 10 minutos → revertir automáticamente
        ScheduledFuture<?> futuro = scheduler.schedule(() -> {
            // Revertir a potencia base — mismo topic /estado, motivo NORMAL.
            publicar(topic, Map.of(
                    "cuarto_id",    cuartoId,
                    "potencia_pct", potenciaBase,
                    "motivo",       "NORMAL",
                    "timestamp",    Instant.now().toString()
            ));
            // Persistir cancelación automática
            persistirIntervencionConRolAsync(cuartoId, sistemaOperadorId,
                    "cancelar_refrigeracion_forzada",
                    "Refrigeración forzada cancelada automáticamente tras 10 min",
                    "operador");
            pendientesRefForzada.remove(cuartoId);
            log.info("[Cuarto {}] Refrigeración forzada REVERTIDA (10 min cumplidos)", cuartoId);
        }, DELAY_REF_FORZADA_MIN, TimeUnit.MINUTES);

        pendientesRefForzada.put(cuartoId, futuro);
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers — MQTT
    // ════════════════════════════════════════════════════════════════

    private void activarCortina(Integer cuartoId) {
        // El back publica el ESTADO de la cortina en /cortina (no /cortina/cmd,
        // que sería un comando entrante). El HMI espera estado activa|inactiva.
        publicar("sei/cuartos/" + cuartoId + "/cortina", Map.of(
                "cuarto_id", cuartoId, "estado", "activa",
                "origen", "automatico",
                "timestamp", Instant.now().toString()));
    }

    private void desactivarCortina(Integer cuartoId) {
        publicar("sei/cuartos/" + cuartoId + "/cortina", Map.of(
                "cuarto_id", cuartoId, "estado", "inactiva",
                "origen", "automatico",
                "timestamp", Instant.now().toString()));
    }

    private void ajustarRefrigeracion(Integer cuartoId, int potenciaPct) {
        // Estado de refrigeración del back → /refrigeracion/estado.
        // Motivo se infiere del contexto: si la puerta está abierta es
        // PUERTA_ABIERTA, si no es NORMAL.
        String motivo = "abierta".equals(estadoPuertaActual.getOrDefault(cuartoId, "cerrada"))
                ? "PUERTA_ABIERTA"
                : "NORMAL";
        publicar("sei/cuartos/" + cuartoId + "/refrigeracion/estado", Map.of(
                "cuarto_id", cuartoId, "potencia_pct", potenciaPct,
                "motivo", motivo,
                "timestamp", Instant.now().toString()));
    }

    private void publicar(String topic, Map<String, Object> payload) {
        try {
            mqttPublisher.publish(topic, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Error publicando en {}: {}", topic, e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers — Persistencia
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public void persistirEventoPuerta(Integer cuartoId, String accion,
                                      Double temperatura, Boolean presencia,
                                      Integer sensorId) {
        EventoPuerta e = new EventoPuerta();
        e.setCuartoId(cuartoId);
        e.setAccion(accion);
        e.setOrigen("automatico");
        e.setTemperatura(temperatura);
        e.setPresencia(presencia);
        e.setSensorId(sensorId != null ? sensorId : cuartoId);
        e.setTimestamp(OffsetDateTime.now());
        e.setTimestampEvento(OffsetDateTime.now());
        e.setTimestampRegistro(OffsetDateTime.now());
        eventoPuertaRepository.save(e);
        log.debug("[Cuarto {}] EventoPuerta: {}", cuartoId, accion);
    }

    /** Versión async (llamada desde scheduler - sin @Transactional de contexto Spring) */
    private void persistirEventoPuertaAsync(Integer cuartoId, String accion,
                                            Double temperatura, Boolean presencia,
                                            Integer sensorId) {
        try {
            persistirEventoPuerta(cuartoId, accion, temperatura, presencia, sensorId);
        } catch (Exception ex) {
            log.error("[Cuarto {}] Error persistiendo EventoPuerta async: {}", cuartoId, ex.getMessage());
        }
    }

    /**
     * Persiste en intervenciones_manuales con rol_en_ejecucion explícito.
     * Corrige T-13-06: siempre se debe poblar rol_en_ejecucion.
     */
    @Transactional
    public void persistirIntervencionConRol(Integer cuartoId, Integer operadorId,
                                            String tipoAccion, String descripcion,
                                            String rolEnEjecucion) {
        IntervencionManual im = new IntervencionManual(operadorId, cuartoId, tipoAccion, descripcion);
        im.setRolEnEjecucion(rolEnEjecucion);   // ← T-13-06 fix
        intervencionManualRepository.save(im);
        log.debug("[Cuarto {}] IntervencionManual: tipo={} rol={}", cuartoId, tipoAccion, rolEnEjecucion);
    }

    private void persistirIntervencionConRolAsync(Integer cuartoId, Integer operadorId,
                                                  String tipoAccion, String descripcion,
                                                  String rolEnEjecucion) {
        try {
            persistirIntervencionConRol(cuartoId, operadorId, tipoAccion, descripcion, rolEnEjecucion);
        } catch (Exception ex) {
            log.error("[Cuarto {}] Error persistiendo Intervencion async: {}", cuartoId, ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers — Generales
    // ════════════════════════════════════════════════════════════════

    private void cancelarPendiente(Map<Integer, ScheduledFuture<?>> mapa, Integer cuartoId) {
        ScheduledFuture<?> f = mapa.remove(cuartoId);
        if (f != null && !f.isDone()) f.cancel(false);
    }

    @Transactional
    public void forzarPuerta(Integer cuartoId, Integer operadorId,
                             String estado, String tipoAccion) {
        // Cancelar cualquier ciclo automático en curso para evitar publicaciones contradictorias
        cancelarPendiente(pendientesCierrePuerta, cuartoId);

        publicarEstadoPuerta(cuartoId, estado, "manual");
        persistirEventoPuerta(cuartoId, tipoAccion,
                temperaturaActual.get(cuartoId), "abierta".equals(estado), cuartoId);
        persistirIntervencionConRol(cuartoId, operadorId,
                tipoAccion,
                "Operador ejecutó " + tipoAccion + " — puerta " + estado,
                "operador");

        // Sincronizar actuadores con el nuevo estado de la puerta.
        // Antes el forzado manual no tocaba cortina/refrigeración (DEF-07):
        // al forzar cierre la cortina seguía activa.
        if ("abierta".equals(estado)) {
            cancelarPendiente(pendientesCortina, cuartoId);
            cancelarPendiente(pendientesRefrigeracion, cuartoId);
            activarCortina(cuartoId);
            refrigeracionService.subirPotencia(cuartoId);
        } else {
            cancelarPendiente(pendientesCortina, cuartoId);
            cancelarPendiente(pendientesRefrigeracion, cuartoId);
            desactivarCortina(cuartoId);
            refrigeracionService.restaurarPotencia(cuartoId);
        }

        log.info("[Cuarto {}] Puerta forzada MANUAL a '{}' por operador {} (tipo_accion={})",
                cuartoId, estado, operadorId, tipoAccion);
    }

    @PreDestroy
    public void shutdown() { scheduler.shutdownNow(); }
}