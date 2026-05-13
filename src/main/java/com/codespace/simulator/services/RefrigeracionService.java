package com.codespace.simulator.services;

import com.codespace.simulator.entities.RefrigeracionEstado;
import com.codespace.simulator.mqtt.MqttPublisher;
import com.codespace.simulator.repositories.RefrigeracionEstadoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class RefrigeracionService {

    private static final Logger log = LoggerFactory.getLogger(RefrigeracionService.class);

    @Value("${control.refrigeracion.potencia.alta:100}")
    private int potenciaAlta;

    @Value("${control.refrigeracion.potencia.base:60}")
    private int potenciaBase;

    private final RefrigeracionEstadoRepository repo;
    private final MqttPublisher                 mqttPublisher;
    private final ObjectMapper                  objectMapper;

    public RefrigeracionService(RefrigeracionEstadoRepository repo,
                                MqttPublisher mqttPublisher,
                                ObjectMapper objectMapper) {
        this.repo          = repo;
        this.mqttPublisher = mqttPublisher;
        this.objectMapper  = objectMapper;
    }

    /**
     * T-08-02: Puerta abierta → subir a 100% en < 2 segundos.
     */
    @Transactional
    public void subirPotencia(Integer cuartoId) {
        persistirYPublicar(cuartoId, potenciaAlta, "PUERTA_ABIERTA");
        log.info("[Cuarto {}] Refrigeración subida a {}% (PUERTA_ABIERTA)", cuartoId, potenciaAlta);
    }

    /**
     * T-08-03: Puerta cerrada → restaurar al valor previo o al 60% por defecto.
     */
    @Transactional
    public void restaurarPotencia(Integer cuartoId) {
        // Busca el último estado previo al PUERTA_ABIERTA
        int potenciaAnterior = repo
                .findTopByCuartoIdOrderByTimestampDesc(cuartoId)
                .map(r -> "NORMAL".equals(r.getMotivo()) ? r.getPotenciaPorcentaje() : potenciaBase)
                .orElse(potenciaBase);

        persistirYPublicar(cuartoId, potenciaAnterior, "NORMAL");
        log.info("[Cuarto {}] Refrigeración restaurada a {}% (PUERTA_CERRADA)", cuartoId, potenciaAnterior);
    }

    /**
     * T-10-03: Forzado manual desde operador.
     */
    @Transactional
    public void forzarManual(Integer cuartoId, Integer operadorId) {
        persistirYPublicar(cuartoId, potenciaAlta, "FORZADO_MANUAL");
        log.info("[Cuarto {}] Refrigeración FORZADA al {}% por operador {}", cuartoId, potenciaAlta, operadorId);
    }

    // ── Helper ────────────────────────────────────────────────────

    private void persistirYPublicar(Integer cuartoId, int potencia, String motivo) {
        // Persistir en BD
        repo.save(new RefrigeracionEstado(cuartoId, potencia, motivo));

        // Publicar estado en MQTT para el HMI (topic /estado, no /cmd)
        String topic = "sei/cuartos/" + cuartoId + "/refrigeracion/estado";
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "cuarto_id",  cuartoId,
                    "potencia",   potencia,
                    "motivo",     motivo,
                    "timestamp",  Instant.now().toString()
            ));
            mqttPublisher.publish(topic, json);
        } catch (Exception e) {
            log.error("[Cuarto {}] Error publicando estado refrigeracion: {}", cuartoId, e.getMessage());
        }
    }
}