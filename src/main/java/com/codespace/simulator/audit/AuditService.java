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

    @Transactional  // ✅ aquí sí funciona — Spring intercepta via proxy
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
        LecturaTemperatura lectura = new LecturaTemperatura(
                event.getCuartoId(), event.getSensorId(),
                event.getTemperatura(), event.getHumedad(),
                event.getTimestamp(), event.getTopic()
        );
        lecturaRepo.save(lectura);
        log.debug("LecturaTemperatura guardada: cuarto={} temp={}",
                lectura.getCuartoId(), lectura.getTemperatura());
    }

    private void persistirPresencia(TemperaturaEvent envelope) {
        EventoPuerta evento = new EventoPuerta(
                envelope.getCuartoId(), envelope.getSensorId(),
                "deteccion_presencia",
                Boolean.TRUE,
                envelope.getTimestamp(), envelope.getTopic()
        );
        eventoPuertaRepo.save(evento);
        log.debug("EventoPuerta guardado: cuarto={}", evento.getCuartoId());
    }
}