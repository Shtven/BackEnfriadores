package com.codespace.simulator.mqtt;

import com.codespace.simulator.models.PresenciaEvent;
import com.codespace.simulator.models.TemperaturaEvent;
import com.codespace.simulator.services.ControlService;
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
    private MqttClient mqttClient;
    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    private final Map<Integer, Boolean> presenciaState = new ConcurrentHashMap<>();

    public Boolean getPresencia(Integer cuartoId) {
        return presenciaState.getOrDefault(cuartoId, false);
    }


    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // no-op — mensajes manejados por MqttSubscriberService
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // no-op
    }
}