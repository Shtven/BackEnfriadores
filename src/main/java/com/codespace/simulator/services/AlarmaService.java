package com.codespace.simulator.services;

import com.codespace.simulator.entities.Alarma;
import com.codespace.simulator.mqtt.MqttPublisher;
import com.codespace.simulator.repositories.AlarmaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlarmaService {

    private static final Logger log = LoggerFactory.getLogger(AlarmaService.class);

    @Value("${alarma.umbral.preventiva:3.0}")
    private double umbralPreventiva;

    @Value("${alarma.umbral.critica:4.0}")
    private double umbralCritica;

    @Value("${alarma.setpoint:20.0}")
    private double setpoint;

    private final Map<Integer, String>        estadoActual    = new ConcurrentHashMap<>();
    private final Map<Integer, Deque<Double>> ultimasLecturas = new ConcurrentHashMap<>();

    private final AlarmaRepository alarmaRepository;
    private final MqttPublisher    mqttPublisher;
    private final ObjectMapper     objectMapper;

    public AlarmaService(AlarmaRepository alarmaRepository,
                         MqttPublisher mqttPublisher,
                         ObjectMapper objectMapper) {
        this.alarmaRepository = alarmaRepository;
        this.mqttPublisher    = mqttPublisher;
        this.objectMapper     = objectMapper;
    }


    @Transactional
    public void evaluar(Integer cuartoId, Double temperatura) {
        registrarLectura(cuartoId, temperatura);

        String estadoAnterior = estadoActual.getOrDefault(cuartoId, "normal");

        if ("critica".equals(estadoAnterior)) {
            if (temperatura > setpoint + umbralCritica) {
                actualizarPicoCritica(cuartoId, temperatura);
            }
            return;
        }

        if (temperatura > setpoint + umbralCritica) {
            manejarCritica(cuartoId, temperatura, estadoAnterior);
            return;
        }

        if (temperatura > setpoint + umbralPreventiva) {
            if (dosConsecutivasSobreUmbral(cuartoId, setpoint + umbralPreventiva)) {
                manejarPreventiva(cuartoId, temperatura, estadoAnterior);
            }
            return;
        }

        if ("preventiva".equals(estadoAnterior)) {
            resolverPreventiva(cuartoId, temperatura);
        }
    }


    @Transactional
    public void silenciarCritica(Integer cuartoId, Integer operadorId) {
        if (!"critica".equals(estadoActual.get(cuartoId))) {
            throw new IllegalStateException(
                    "No hay alarma crítica activa en cuarto " + cuartoId);
        }

        alarmaRepository
                .findByCuartoIdAndTimestampFinIsNull(cuartoId)
                .ifPresent(alarma -> {
                    alarma.setTimestampFin(Instant.now());
                    alarma.setSilenciadaPor(operadorId);
                    alarma.setTimestampSilencio(Instant.now());
                    alarmaRepository.save(alarma);
                });

        estadoActual.put(cuartoId, "normal");
        publicarEstadoAlarma(cuartoId, "normal", 0.0, null);
        log.info("[Cuarto {}] Alarma crítica silenciada — operador_id={}", cuartoId, operadorId);
    }



    private void manejarPreventiva(Integer cuartoId, Double temperatura, String estadoAnterior) {
        if ("preventiva".equals(estadoAnterior)) return;

        estadoActual.put(cuartoId, "preventiva");

        Alarma alarma = new Alarma(cuartoId, "preventiva", temperatura);
        alarmaRepository.save(alarma);

        publicarEstadoAlarma(cuartoId, "preventiva", temperatura, alarma.getId());
        log.warn("[Cuarto {}] ⚠ PREVENTIVA — temp={}°C", cuartoId, temperatura);
    }

    private void manejarCritica(Integer cuartoId, Double temperatura, String estadoAnterior) {
        if ("preventiva".equals(estadoAnterior)) {
            alarmaRepository
                    .findByCuartoIdAndTimestampFinIsNull(cuartoId)
                    .ifPresent(a -> {
                        a.setTimestampFin(Instant.now());
                        alarmaRepository.save(a);
                    });
        }

        estadoActual.put(cuartoId, "critica");

        Alarma alarma = new Alarma(cuartoId, "critica", temperatura);
        alarmaRepository.save(alarma);

        // T-05-01: Publicar estado crítico
        publicarEstadoAlarma(cuartoId, "critica", temperatura, alarma.getId());
        log.error("[Cuarto {}] CRÍTICA — temp={}°C", cuartoId, temperatura);
    }

    private void resolverPreventiva(Integer cuartoId, Double temperatura) {
        alarmaRepository
                .findByCuartoIdAndTimestampFinIsNull(cuartoId)
                .ifPresent(a -> {
                    a.setTimestampFin(Instant.now());
                    alarmaRepository.save(a);
                });

        estadoActual.put(cuartoId, "normal");
        publicarEstadoAlarma(cuartoId, "normal", temperatura, null);
        log.info("[Cuarto {}] Preventiva resuelta — temp={}°C", cuartoId, temperatura);
    }

    private void actualizarPicoCritica(Integer cuartoId, Double temperatura) {
        alarmaRepository
                .findByCuartoIdAndTimestampFinIsNull(cuartoId)
                .ifPresent(a -> {
                    if (temperatura > a.getTemperaturaPico()) {
                        a.setTemperaturaPico(temperatura);
                        alarmaRepository.save(a);
                    }
                });
    }


    private void publicarEstadoAlarma(Integer cuartoId, String estado,
                                      Double temperatura, Long alarmaId) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("cuarto_id",   cuartoId);
            payload.put("estado",      estado);
            payload.put("tipo",        "normal".equals(estado) ? null : estado);
            payload.put("temperatura", temperatura);
            payload.put("timestamp",   Instant.now().toString());
            payload.put("alarma_id",   alarmaId);

            String json  = objectMapper.writeValueAsString(payload);
            String topic = "sei/cuartos/" + cuartoId + "/alarma";
            mqttPublisher.publish(topic, json);

        } catch (Exception e) {
            log.error("[Cuarto {}] Error publicando alarma MQTT: {}", cuartoId, e.getMessage());
        }
    }


    private void registrarLectura(Integer cuartoId, Double temperatura) {
        ultimasLecturas.computeIfAbsent(cuartoId, k -> new ArrayDeque<>(2));
        Deque<Double> ventana = ultimasLecturas.get(cuartoId);
        if (ventana.size() == 2) ventana.pollFirst();
        ventana.addLast(temperatura);
    }

    private boolean dosConsecutivasSobreUmbral(Integer cuartoId, double umbral) {
        Deque<Double> ventana = ultimasLecturas.getOrDefault(cuartoId, new ArrayDeque<>());
        if (ventana.size() < 2) return false;
        return ventana.stream().allMatch(t -> t > umbral);
    }
}