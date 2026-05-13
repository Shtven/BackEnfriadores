package com.codespace.simulator.mqtt;

import com.codespace.simulator.models.TemperaturaEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codespace.simulator.dto.response.SilencioPayload;
import com.codespace.simulator.security.JwtService;
import com.codespace.simulator.services.AlarmaService;
import com.codespace.simulator.services.ControlService;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
public class MqttSubscriberService {

    private static final Logger log = LoggerFactory.getLogger(MqttSubscriberService.class);

    // Topic donde se reportan violaciones de seguridad
    private static final String TOPIC_SEGURIDAD = "sei/sistema/seguridad";

    private final MqttClient    mqttClient;
    private final AlarmaService alarmaService;
    private final ControlService controlService;
    private final JwtService    jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final BlockingQueue<TemperaturaEvent> auditQueue;  // ← nuevo campo

    public MqttSubscriberService(MqttClient mqttClient,
                                 AlarmaService alarmaService,
                                 ControlService controlService,
                                 JwtService jwtService,
                                 BlockingQueue<TemperaturaEvent> auditQueue) {  // ← nuevo param
        this.mqttClient    = mqttClient;
        this.alarmaService = alarmaService;
        this.controlService = controlService;
        this.jwtService    = jwtService;
        this.auditQueue    = auditQueue;
    }

    @PostConstruct
    public void suscribir() throws MqttException {
        // Sprint 2 (sin cambios)
        mqttClient.subscribe("sei/cuartos/+/temperatura",  1, this::manejarTemperatura);
        mqttClient.subscribe("sei/cuartos/+/silencio",     1, this::manejarSilencio);
        mqttClient.subscribe("sei/cuartos/+/presencia",    1, this::manejarPresencia);

        // Sprint 3: topics de comando — requieren JWT válido de operador
        mqttClient.subscribe("sei/cuartos/+/cmd",          1, this::manejarComando);
        mqttClient.subscribe("sei/cuartos/+/puerta/cmd",   1, this::manejarComandoPuerta);

        log.info("[MQTT] Suscripciones activas (Sprint 2 + Sprint 3)");
    }

    // ── Sprint 2 ──────────────────────────────────────────────────

    // ── manejarTemperatura — ahora también encola + llama actualizarTemperatura
    private void manejarTemperatura(String topic, MqttMessage message) {
        try {
            int      cuartoId   = extraerCuartoId(topic);
            JsonNode json       = objectMapper.readTree(message.getPayload());
            double   temperatura = json.get("temperatura").asDouble();

            // Sprint 2: evaluar alarma
            alarmaService.evaluar(cuartoId, temperatura);

            // Sprint 3 T-07-02: actualizar estado de temperatura
            controlService.actualizarTemperatura(cuartoId, temperatura);

            // Auditoría: encolar para AuditThread (reemplaza lo que hacía MqttListenerThread)
            TemperaturaEvent event = objectMapper.readValue(message.getPayload(), TemperaturaEvent.class);
            event.setTopic(topic);
            if (!auditQueue.offer(event)) {
                log.warn("[MQTT] AuditQueue llena, temperatura descartada topic={}", topic);
            }

        } catch (Exception e) {
            log.error("[MQTT] Error en temperatura {}: {}", topic, e.getMessage());
        }
    }

    // ── manejarPresencia — ahora extrae sensorId y delega todo a ControlService
    private void manejarPresencia(String topic, MqttMessage message) {
        try {
            int      cuartoId = extraerCuartoId(topic);
            JsonNode json     = objectMapper.readTree(message.getPayload());
            boolean  presencia = json.get("presencia").asBoolean();
            String   sensorId  = json.has("sensor_id")
                    ? json.get("sensor_id").asText()
                    : "desconocido";

            // Sprint 3: actualiza presencia + evalúa T-07 + delega Sprint 2
            controlService.actualizarPresencia(cuartoId, presencia, sensorId);

        } catch (Exception e) {
            log.error("[MQTT] Error en presencia {}: {}", topic, e.getMessage());
        }
    }

    private void manejarSilencio(String topic, MqttMessage message) {
        try {
            SilencioPayload payload = objectMapper
                    .readValue(message.getPayload(), SilencioPayload.class);
            alarmaService.silenciarCritica(payload.getCuarto(), payload.getOperadorId());
        } catch (Exception e) {
            log.error("[MQTT] Error en silencio {}: {}", topic, e.getMessage());
        }
    }

    // ── T-13-05: Comandos con validación JWT ──────────────────────

    private void manejarComando(String topic, MqttMessage message) {
        try {
            int      cuartoId = extraerCuartoId(topic);
            JsonNode json     = objectMapper.readTree(message.getPayload());

            // 1. Extraer y validar JWT del payload
            if (!json.has("jwt_token")) {
                rechazarComando(topic, cuartoId, "Sin jwt_token en payload", null);
                return;
            }

            String token = json.get("jwt_token").asText();

            if (!jwtService.esValido(token)) {
                rechazarComando(topic, cuartoId, "JWT inválido o expirado", null);
                return;
            }

            String  rol        = jwtService.getRol(token);
            Integer operadorId = jwtService.getOperadorId(token);

            // 2. T-13-05: supervisores NO pueden ejecutar comandos operativos
            if ("supervisor".equals(rol)) {
                rechazarComando(topic, cuartoId, "Supervisor no autorizado para comandos", operadorId);
                return;
            }

            // 3. Procesar la acción del comando
            String accion = json.has("accion") ? json.get("accion").asText() : "";

            switch (accion) {
                case "forzar_refrigeracion" ->
                    // T-10-03/04/05
                        controlService.forzarRefrigeracion(cuartoId, operadorId);

                case "forzar_puerta" -> {
                    String estadoPuerta = json.has("estado") ? json.get("estado").asText() : "cerrada";
                    controlService.forzarPuerta(cuartoId, operadorId,
                            estadoPuerta, "forzar_puerta_" + estadoPuerta);
                }

                default ->
                        log.warn("[MQTT][CMD] Acción desconocida '{}' en cuarto {}", accion, cuartoId);
            }

        } catch (Exception e) {
            log.error("[MQTT] Error procesando comando en {}: {}", topic, e.getMessage());
        }
    }

    /** Publica rechazo en sei/sistema/seguridad (T-13-05) */
    private void rechazarComando(String topic, int cuartoId,
                                 String motivo, Integer operadorId) {
        try {
            String payload = String.format(
                    "{\"topic\":\"%s\",\"cuarto\":%d,\"motivo\":\"%s\",\"operador_id\":%s,\"timestamp\":\"%s\"}",
                    topic, cuartoId, motivo,
                    operadorId != null ? operadorId : "null",
                    java.time.Instant.now()
            );
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(1);
            mqttClient.publish(TOPIC_SEGURIDAD, msg);
            log.warn("[SEGURIDAD] Comando rechazado en {} — {}", topic, motivo);

        } catch (MqttException e) {
            log.error("[MQTT] Error publicando en seguridad: {}", e.getMessage());
        }
    }

    // ── Handler dedicado para sei/cuartos/+/puerta/cmd ────────────
    //
    // Payload esperado:
    //   { "comando": "forzar_cierre" | "forzar_apertura",
    //     "rol": "operador", "jwt_token": "...",
    //     "operador_id": N, "cuarto_id": N }
    //
    // Valida JWT + rol=operador, persiste intervención manual y
    // publica el estado resultante en sei/cuartos/{n}/puerta con
    // origen="manual".
    private void manejarComandoPuerta(String topic, MqttMessage message) {
        try {
            int      cuartoId = extraerCuartoId(topic);
            JsonNode json     = objectMapper.readTree(message.getPayload());

            if (!json.has("jwt_token")) {
                rechazarComando(topic, cuartoId, "Sin jwt_token en payload", null);
                return;
            }

            String token = json.get("jwt_token").asText();
            if (!jwtService.esValido(token)) {
                rechazarComando(topic, cuartoId, "JWT inválido o expirado", null);
                return;
            }

            String  rol        = jwtService.getRol(token);
            Integer operadorId = jwtService.getOperadorId(token);

            if (!"operador".equals(rol)) {
                rechazarComando(topic, cuartoId,
                        "Rol no autorizado para puerta/cmd: " + rol, operadorId);
                return;
            }

            String comando = json.has("comando") ? json.get("comando").asText() : "";
            String estado = switch (comando) {
                case "forzar_cierre"   -> "cerrada";
                case "forzar_apertura" -> "abierta";
                default                -> null;
            };

            if (estado == null) {
                log.warn("[MQTT][PUERTA/CMD] Comando desconocido '{}' en cuarto {}", comando, cuartoId);
                return;
            }

            controlService.forzarPuerta(cuartoId, operadorId, estado, comando);

        } catch (Exception e) {
            log.error("[MQTT] Error procesando comando puerta en {}: {}", topic, e.getMessage());
        }
    }

    private int extraerCuartoId(String topic) {
        return Integer.parseInt(topic.split("/")[2]);
    }
}