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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class ControlService {

    private static final Logger log = LoggerFactory.getLogger(ControlService.class);

    @Value("${control.cortina.desactivar.delay-seg:5}")
    private int cortinaCierreDelaySeg;          // T-06-02: 5 s tras cierre

    @Value("${control.refrigeracion.retorno.delay-seg:30}")
    private int refrigeracionRetornoDelaySeg;   // T-08-02: 30 s tras cierre

    @Value("${control.refrigeracion.potencia.alta:100}")
    private int potenciaAlta;                   // T-08-01: 100%

    @Value("${control.refrigeracion.potencia.base:60}")
    private int potenciaBase;                   // T-08-02: 60%

    // ID del operador 'sistema' en tabla operadores (T-08-03)
    @Value("${control.sistema.operador-id:1}")
    private Integer sistemaOperadorId;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4);

    // Guarda los futures para cancelar si la puerta vuelve a abrirse antes del delay
    private final Map<Integer, ScheduledFuture<?>> pendientesCortina       = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> pendientesRefrigeracion = new ConcurrentHashMap<>();

    private final MqttPublisher                mqttPublisher;
    private final EventoPuertaRepository       eventoPuertaRepository;
    private final IntervencionManualRepository intervencionManualRepository;
    private final ObjectMapper                 objectMapper;

    public ControlService(MqttPublisher mqttPublisher,
                          EventoPuertaRepository eventoPuertaRepository,
                          IntervencionManualRepository intervencionManualRepository,
                          ObjectMapper objectMapper) {
        this.mqttPublisher                = mqttPublisher;
        this.eventoPuertaRepository       = eventoPuertaRepository;
        this.intervencionManualRepository = intervencionManualRepository;
        this.objectMapper                 = objectMapper;
    }

    @Transactional
    public void procesarEventoPuerta(Integer cuartoId, Boolean presencia,
                                     String sensorId, Double temperatura) {
        if (presencia == null) return;

        if (Boolean.TRUE.equals(presencia)) {
            manejarPuertaAbierta(cuartoId, temperatura);
        } else {
            manejarPuertaCerrada(cuartoId, temperatura);
        }
    }

    private void manejarPuertaAbierta(Integer cuartoId, Double temperatura) {
        // Cancelar tareas pendientes de cierre anterior si la puerta se reabre
        cancelarPendiente(pendientesCortina,       cuartoId);
        cancelarPendiente(pendientesRefrigeracion, cuartoId);

        // T-06-02: Activar cortina inmediatamente (< 500 ms — síncrono)
        activarCortina(cuartoId);

        // T-06-03: Persistir evento apertura en eventos_puerta
        persistirEventoPuerta(cuartoId, "apertura", temperatura, true);

        // T-08-01: Subir refrigeración a 100% (< 2 s — síncrono)
        ajustarRefrigeracion(cuartoId, potenciaAlta);

        // T-08-03: Registrar ajuste automático en intervenciones_manuales
        persistirIntervencionAutomatica(cuartoId,
                "ajuste_automatico_potencia",
                "Puerta abierta → refrigeración subida a " + potenciaAlta + "%");

        log.info("[Cuarto {}] Puerta ABIERTA — cortina activada, refrigeración {}%",
                cuartoId, potenciaAlta);
    }

    // ─────────────────────────────────────────────────────────
    //  PUERTA CERRADA
    // ─────────────────────────────────────────────────────────
    private void manejarPuertaCerrada(Integer cuartoId, Double temperatura) {
        // T-06-03: Persistir evento cierre
        persistirEventoPuerta(cuartoId, "cierre", temperatura, false);

        // T-06-02: Desactivar cortina tras 5 s
        ScheduledFuture<?> futuroCortina = scheduler.schedule(() -> {
            desactivarCortina(cuartoId);
            log.info("[Cuarto {}] Cortina DESACTIVADA (5 s tras cierre)", cuartoId);
        }, cortinaCierreDelaySeg, TimeUnit.SECONDS);
        pendientesCortina.put(cuartoId, futuroCortina);

        // T-08-02: Retornar refrigeración a 60% tras 30 s
        ScheduledFuture<?> futuroRefrig = scheduler.schedule(() -> {
            ajustarRefrigeracion(cuartoId, potenciaBase);
            // T-08-03: Registrar retorno en intervenciones_manuales
            persistirIntervencionAutomaticaAsync(cuartoId,
                    "ajuste_automatico_potencia",
                    "Puerta cerrada 30 s → refrigeración retornada a " + potenciaBase + "%");
            log.info("[Cuarto {}] Refrigeración retornada a {}% (30 s tras cierre)",
                    cuartoId, potenciaBase);
        }, refrigeracionRetornoDelaySeg, TimeUnit.SECONDS);
        pendientesRefrigeracion.put(cuartoId, futuroRefrig);

        log.info("[Cuarto {}] Puerta CERRADA — cortina desactivará en {}s, " +
                        "refrigeración retornará en {}s",
                cuartoId, cortinaCierreDelaySeg, refrigeracionRetornoDelaySeg);
    }

    private void activarCortina(Integer cuartoId) {
        String topic = "sei/cuartos/" + cuartoId + "/cortina/cmd";
        publicar(topic, Map.of(
                "cuarto_id", cuartoId,
                "comando",   "activar_cortina",
                "timestamp", Instant.now().toString()
        ));
    }

    // T-06-02
    private void desactivarCortina(Integer cuartoId) {
        String topic = "sei/cuartos/" + cuartoId + "/cortina/cmd";
        publicar(topic, Map.of(
                "cuarto_id", cuartoId,
                "comando",   "desactivar_cortina",
                "timestamp", Instant.now().toString()
        ));
    }

    private void ajustarRefrigeracion(Integer cuartoId, int potenciaPct) {
        String topic = "sei/cuartos/" + cuartoId + "/refrigeracion/cmd";
        publicar(topic, Map.of(
                "cuarto_id",   cuartoId,
                "potencia_pct", potenciaPct,
                "timestamp",   Instant.now().toString()
        ));
    }

    @Transactional
    public void persistirEventoPuerta(Integer cuartoId, String accion,
                                      Double temperatura, Boolean presencia) {
        EventoPuerta evento = new EventoPuerta(
                cuartoId, accion, "automatico", temperatura, presencia);
        eventoPuertaRepository.save(evento);
        log.debug("[Cuarto {}] EventoPuerta persistido: accion={}", cuartoId, accion);
    }

    // T-08-03 — versión síncrona (llamada desde hilo principal)
    @Transactional
    public void persistirIntervencionAutomatica(Integer cuartoId,
                                                String tipoAccion,
                                                String descripcion) {
        IntervencionManual intervencion = new IntervencionManual(
                sistemaOperadorId, cuartoId, tipoAccion, descripcion);
        intervencionManualRepository.save(intervencion);
        log.debug("[Cuarto {}] IntervencionManual persistida: tipo={}", cuartoId, tipoAccion);
    }

    // T-08-03 — versión para llamar desde el scheduler (hilo separado, sin @Transactional)
    private void persistirIntervencionAutomaticaAsync(Integer cuartoId,
                                                      String tipoAccion,
                                                      String descripcion) {
        try {
            IntervencionManual intervencion = new IntervencionManual(
                    sistemaOperadorId, cuartoId, tipoAccion, descripcion);
            intervencionManualRepository.save(intervencion);
        } catch (Exception e) {
            log.error("[Cuarto {}] Error persistiendo intervencion async: {}", cuartoId, e.getMessage());
        }
    }


    private void publicar(String topic, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            mqttPublisher.publish(topic, json);
        } catch (Exception e) {
            log.error("Error publicando en {}: {}", topic, e.getMessage());
        }
    }

    private void cancelarPendiente(Map<Integer, ScheduledFuture<?>> mapa, Integer cuartoId) {
        ScheduledFuture<?> futuro = mapa.remove(cuartoId);
        if (futuro != null && !futuro.isDone()) {
            futuro.cancel(false);
            log.debug("[Cuarto {}] Tarea pendiente cancelada (nueva apertura)", cuartoId);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}