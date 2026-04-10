package com.codespace.simulator.mqtt;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MqttPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttPublisher.class);

    @Value("${mqtt.broker.url}")
    private String brokerUrl;
    @Value("${mqtt.username:}")
    private String mqttUsername;

    @Value("${mqtt.password:}")
    private String mqttPassword;

    private MqttClient mqttClient;

    public MqttPublisher() {}

    public void publish(String topic, String payload) {
        try {
            ensureConnected();
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            log.error("Error al publicar en topic={}: {}", topic, e.getMessage(), e);
        }
    }

    private synchronized void ensureConnected() throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) return;
        String uniqueId = "sei-publisher-" + java.util.UUID.randomUUID();
        mqttClient = new MqttClient(brokerUrl, uniqueId, new MemoryPersistence());
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(false);
        opts.setCleanSession(true);
        opts.setConnectionTimeout(10);

        if (mqttUsername != null && !mqttUsername.isBlank()) {
            opts.setUserName(mqttUsername);
            opts.setPassword(mqttPassword.toCharArray());
        }

        mqttClient.connect(opts);
        log.info("MqttPublisher conectado: {}", uniqueId);
    }
}