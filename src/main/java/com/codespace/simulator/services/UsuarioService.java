package com.codespace.simulator.services;

import com.codespace.simulator.dto.request.RegistroRequest;
import com.codespace.simulator.dto.response.AprobacionResponse;
import com.codespace.simulator.dto.response.RegistroResponse;
import com.codespace.simulator.dto.response.UsuarioPendienteResponse;
import com.codespace.simulator.entities.Operador;
import com.codespace.simulator.repositories.OperadorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para el flujo de registro dinamico de usuarios con
 * aprobacion HACCP. La autorizacion fina (operador activo) se
 * resuelve aqui contra la BD, no solo contra el JWT, para evitar
 * que un token emitido a un operador luego desactivado siga teniendo
 * privilegios de aprobacion.
 */
@Service
public class UsuarioService {

    private static final String ROL_OPERADOR   = "operador";
    private static final String ROL_SUPERVISOR = "supervisor";

    private final OperadorRepository operadorRepository;
    private final PasswordEncoder    passwordEncoder;

    public UsuarioService(OperadorRepository operadorRepository, PasswordEncoder passwordEncoder) {
        this.operadorRepository = operadorRepository;
        this.passwordEncoder    = passwordEncoder;
    }

    // ── Resultado tipado para que el controller decida el status code ──
    public static final class Resultado<T> {
        public final int status;
        public final T   body;
        public final String error;

        private Resultado(int status, T body, String error) {
            this.status = status;
            this.body   = body;
            this.error  = error;
        }
        public static <T> Resultado<T> ok(int status, T body) {
            return new Resultado<>(status, body, null);
        }
        public static <T> Resultado<T> fail(int status, String error) {
            return new Resultado<>(status, null, error);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // 1) Registro publico
    // ──────────────────────────────────────────────────────────────────
    public Resultado<RegistroResponse> registrar(RegistroRequest req) {
        if (req == null) {
            return Resultado.fail(400, "Cuerpo de la peticion vacio");
        }

        String nombre   = trim(req.getNombre());
        String usuario  = trim(req.getUsuario());
        String password = req.getPassword();   // sin trim, no alterar contrasena
        String rol      = trim(req.getRol());

        // Validaciones de formato
        if (nombre == null || nombre.isEmpty()) {
            return Resultado.fail(400, "El nombre es obligatorio");
        }
        if (nombre.length() > 100) {
            return Resultado.fail(400, "El nombre no puede superar 100 caracteres");
        }
        if (usuario == null || usuario.isEmpty()) {
            return Resultado.fail(400, "El usuario es obligatorio");
        }
        if (usuario.length() > 50) {
            return Resultado.fail(400, "El usuario no puede superar 50 caracteres");
        }
        if (usuario.contains(" ")) {
            return Resultado.fail(400, "El usuario no puede contener espacios");
        }
        if (password == null || password.isEmpty()) {
            return Resultado.fail(400, "La password es obligatoria");
        }
        if (password.length() < 8) {
            return Resultado.fail(400, "La password debe tener al menos 8 caracteres");
        }
        if (!ROL_OPERADOR.equals(rol) && !ROL_SUPERVISOR.equals(rol)) {
            return Resultado.fail(400, "El rol debe ser 'operador' o 'supervisor'");
        }

        // Unicidad de usuario
        if (operadorRepository.existsByUsuario(usuario)) {
            return Resultado.fail(409, "El nombre de usuario ya esta en uso");
        }

        // Persistencia
        Operador nuevo = new Operador();
        nuevo.setNombre(nombre);
        nuevo.setUsuario(usuario);
        nuevo.setPasswordHash(passwordEncoder.encode(password));
        nuevo.setRol(rol);
        nuevo.setCreadoEn(OffsetDateTime.now());

        // Supervisor: activo de inmediato; Operador: pendiente de aprobacion
        boolean activoInicial = ROL_SUPERVISOR.equals(rol);
        nuevo.setActivo(activoInicial);
        nuevo.setAprobadoPor(null);
        nuevo.setTimestampAprobacion(null);

        Operador guardado = operadorRepository.save(nuevo);

        String mensaje = ROL_SUPERVISOR.equals(rol)
                ? "Cuenta creada. Puedes iniciar sesion."
                : "Cuenta creada. Pendiente de aprobacion por un operador activo.";

        return Resultado.ok(201, new RegistroResponse(
                guardado.getId(),
                guardado.getUsuario(),
                guardado.getRol(),
                guardado.isActivo(),
                mensaje
        ));
    }

    // ──────────────────────────────────────────────────────────────────
    // 2) Aprobacion (PATCH)
    // ──────────────────────────────────────────────────────────────────
    public Resultado<AprobacionResponse> aprobar(Integer idObjetivo, Integer aprobadorIdJwt, String aprobadorRolJwt) {
        // Solo operadores ACTIVOS pueden aprobar (defensa en profundidad:
        // re-verificamos el rol y el flag activo contra BD, no solo el JWT).
        if (!ROL_OPERADOR.equals(aprobadorRolJwt)) {
            return Resultado.fail(403, "Solo un operador activo puede aprobar registros");
        }
        Optional<Operador> aprobadorOpt = operadorRepository.findById(aprobadorIdJwt);
        if (aprobadorOpt.isEmpty() || !aprobadorOpt.get().isActivo()
                || !ROL_OPERADOR.equals(aprobadorOpt.get().getRol())) {
            return Resultado.fail(403, "Solo un operador activo puede aprobar registros");
        }

        Optional<Operador> objetivoOpt = operadorRepository.findById(idObjetivo);
        if (objetivoOpt.isEmpty()) {
            return Resultado.fail(404, "Cuenta no encontrada");
        }
        Operador objetivo = objetivoOpt.get();

        if (ROL_SUPERVISOR.equals(objetivo.getRol())) {
            return Resultado.fail(400, "Las cuentas de supervisor no requieren aprobacion");
        }
        if (objetivo.isActivo()) {
            return Resultado.fail(400, "La cuenta ya esta activa");
        }

        objetivo.setActivo(true);
        objetivo.setAprobadoPor(aprobadorIdJwt);
        objetivo.setTimestampAprobacion(OffsetDateTime.now());
        Operador actualizado = operadorRepository.save(objetivo);

        return Resultado.ok(200, new AprobacionResponse(
                actualizado.getId(),
                actualizado.getUsuario(),
                actualizado.isActivo(),
                actualizado.getAprobadoPor(),
                actualizado.getTimestampAprobacion(),
                "Cuenta aprobada exitosamente"
        ));
    }

    // ──────────────────────────────────────────────────────────────────
    // 3) Listado de pendientes
    // ──────────────────────────────────────────────────────────────────
    public Resultado<List<UsuarioPendienteResponse>> listarPendientes(Integer solicitanteIdJwt, String solicitanteRolJwt) {
        if (!ROL_OPERADOR.equals(solicitanteRolJwt)) {
            return Resultado.fail(403, "Solo un operador activo puede consultar pendientes");
        }
        Optional<Operador> solicitante = operadorRepository.findById(solicitanteIdJwt);
        if (solicitante.isEmpty() || !solicitante.get().isActivo()
                || !ROL_OPERADOR.equals(solicitante.get().getRol())) {
            return Resultado.fail(403, "Solo un operador activo puede consultar pendientes");
        }

        List<UsuarioPendienteResponse> lista = operadorRepository
                .findByRolAndActivoFalseOrderByCreadoEnAsc(ROL_OPERADOR)
                .stream()
                .map(o -> new UsuarioPendienteResponse(
                        o.getId(),
                        o.getNombre(),
                        o.getUsuario(),
                        o.getRol(),
                        o.getCreadoEn()))
                .toList();

        return Resultado.ok(200, lista);
    }

    // ── helpers ───────────────────────────────────────────────────────
    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
