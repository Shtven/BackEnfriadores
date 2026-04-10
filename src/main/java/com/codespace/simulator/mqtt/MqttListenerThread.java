package com.codespace.simulator.mqtt;

import com.codespace.simulator.models.PresenciaEvent;
import com.codespace.simulator.models.TemperaturaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MqttListenerThread implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttListenerThread.class);

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String mqttUsername;

    @Value("${mqtt.password:}")
    private String mqttPassword;

    private final BlockingQueue<TemperaturaEvent> auditQueue;
    private final MqttPublisher mqttPublisher;
    private final ObjectMapper objectMapper;

    private MqttClient mqttClient;

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    private final Map<Integer, Boolean> presenciaState = new ConcurrentHashMap<>();

    public MqttListenerThread(BlockingQueue<TemperaturaEvent> auditQueue,
                              MqttPublisher mqttPublisher,
                              ObjectMapper objectMapper) {
        this.auditQueue    = auditQueue;
        this.mqttPublisher = mqttPublisher;
        this.objectMapper  = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws MqttException {
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        mqttClient.setCallback(this);
        mqttClient.connect(buildConnectOptions());
        mqttClient.subscribe("sei/cuartos/+/#", 1);
        log.info("MqttListenerThread suscrito a: sei/cuartos/+/#");
    }

    private MqttConnectOptions buildConnectOptions() {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setKeepAliveInterval(60);
        opts.setCleanSession(false);

        if (mqttUsername != null && !mqttUsername.isBlank()) {
            opts.setUserName(mqttUsername);
            opts.setPassword(mqttPassword.toCharArray());
        }
        return opts;
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Conexión MQTT perdida: {}. Paho intentará reconectar…", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        if (topic.contains("temperatura")) {
            handleTemperatura(topic, payload);
        } else if (topic.contains("presencia")) {
            handlePresencia(topic, payload);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    private void handleTemperatura(String topic, String payload) throws Exception {
        TemperaturaEvent event = objectMapper.readValue(payload, TemperaturaEvent.class);
        event.setTopic(topic);
        boolean enqueued = auditQueue.offer(event);
        // ✅ FIX 2: log ya no es null
        if (!enqueued) log.warn("AuditQueue llena, evento descartado topic={}", topic);
        else log.debug("TemperaturaEvent encolado: {}", event);
    }

    private void handlePresencia(String topic, String payload) throws Exception {
        PresenciaEvent event = objectMapper.readValue(payload, PresenciaEvent.class);
        event.setTopic(topic);

        presenciaState.put(event.getCuartoId(), event.getPresencia());

        String ackTopic = "sei/cuartos/" + event.getCuartoId() + "/presencia/ack";
        mqttPublisher.publish(ackTopic, "{\"status\":\"ok\"}");

        TemperaturaEvent envelope = new TemperaturaEvent();
        envelope.setTopic(topic);
        envelope.setCuartoId(event.getCuartoId());
        envelope.setSensorId(event.getSensorId());
        envelope.setTimestamp(event.getTimestamp());
        auditQueue.offer(envelope);
    }

    public Boolean getPresencia(Integer cuartoId) {
        return presenciaState.getOrDefault(cuartoId, false);
    }
}