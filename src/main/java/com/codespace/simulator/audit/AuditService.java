package com.codespace.simulator.audit;

import com.codespace.simulator.entities.EventoPuerta;
import com.codespace.simulator.entities.LecturaTemperatura;
import com.codespace.simulator.models.TemperaturaEvent;
import com.codespace.simulator.repositories.EventoPuertaRepository;
import com.codespace.simulator.repositories.LecturaTemperaturaRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final LecturaTemperaturaRepository lecturaRepo;
    private final EventoPuertaRepository eventoPuertaRepo;

    public AuditService(LecturaTemperaturaRepository lecturaRepo,
                        EventoPuertaRepository eventoPuertaRepo) {
        this.lecturaRepo = lecturaRepo;
        this.eventoPuertaRepo = eventoPuertaRepo;
    }

    @Transactional
    public void procesarEvento(TemperaturaEvent event) {
        String topic = event.getTopic() != null ? event.getTopic() : "";
        try {
            if (topic.contains("presencia")) {
                persistirPresencia(event);
            } else {
                persistirTemperatura(event);
            }
        } catch (Exception e) {
            log.error("Error al persistir evento topic={}: {}", topic, e.getMessage(), e);
        }
    }

    private void persistirTemperatura(TemperaturaEvent event) {
        Integer sensorId = event.getSensorId() != null
                ? event.getSensorId()
                : event.getCuartoId();
        LecturaTemperatura lectura = new LecturaTemperatura(
                event.getCuartoId(), sensorId,
                event.getTemperatura(), event.getTimestamp(),
                event.getTopic()
        );
        lecturaRepo.save(lectura);
        log.debug("LecturaTemperatura guardada: cuarto={} sensor={} temp={}",
                lectura.getCuartoId(), lectura.getSensorId(), lectura.getTemperatura());
    }

    private void persistirPresencia(TemperaturaEvent envelope) {
        Integer sensorId = envelope.getSensorId() != null
                ? envelope.getSensorId()
                : envelope.getCuartoId();
        EventoPuerta e = new EventoPuerta();
        e.setCuartoId(envelope.getCuartoId());
        e.setAccion("presencia");
        e.setOrigen("sensor");
        e.setSensorId(sensorId);          // <- usar la variable
        e.setTimestamp(OffsetDateTime.now());
        e.setTimestampRegistro(OffsetDateTime.now());
        e.setTopic(envelope.getTopic());
        eventoPuertaRepo.save(e);
        log.debug("EventoPuerta guardado: cuarto={}", envelope.getCuartoId());
    }
}