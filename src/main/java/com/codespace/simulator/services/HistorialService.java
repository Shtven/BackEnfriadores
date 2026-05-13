package com.codespace.simulator.services;

import com.codespace.simulator.entities.LecturaTemperatura;
import com.codespace.simulator.repositories.LecturaTemperaturaRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class HistorialService {

    private final LecturaTemperaturaRepository repo;

    public HistorialService(LecturaTemperaturaRepository repo) {
        this.repo = repo;
    }

    /**
     * T-11-01/02: Consulta lecturas por cuarto y rango.
     * Soporta shortcuts: "24h", "7d", "30d" o timestamps explícitos.
     */
    public List<LecturaTemperatura> consultar(Integer cuartoId,
                                              String shortcut,
                                              OffsetDateTime desde,
                                              OffsetDateTime hasta) {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

        if (shortcut != null) {
            desde = switch (shortcut) {
                case "24h" -> ahora.minusHours(24);
                case "7d"  -> ahora.minusDays(7);
                case "30d" -> ahora.minusDays(30);
                default    -> ahora.minusHours(24);
            };
            hasta = ahora;
        }

        if (desde == null) desde = ahora.minusHours(24);
        if (hasta == null) hasta = ahora;

        return repo.findByRango(cuartoId, desde, hasta);
    }

    /**
     * T-11-03: Genera CSV con columnas timestamp, cuarto_id, temperatura, estado_alarma.
     */
    public String generarCsv(List<LecturaTemperatura> lecturas) {
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,cuarto_id,temperatura,estado_alarma\n");
        for (LecturaTemperatura l : lecturas) {
            sb.append(l.getTimestamp()).append(',')
                    .append(l.getCuartoId()).append(',')
                    .append(l.getTemperatura()).append(',')
                    .append(l.getEstadoAlarma()).append('\n');
        }
        return sb.toString();
    }

    /** Convierte lecturas a lista de maps para respuesta JSON */
    public List<Map<String, Object>> toJson(List<LecturaTemperatura> lecturas) {
        return lecturas.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("timestamp",    l.getTimestamp());
            m.put("cuarto_id",    l.getCuartoId());
            m.put("temperatura",  l.getTemperatura());
            m.put("estado_alarma", l.getEstadoAlarma());
            return m;
        }).toList();
    }
}