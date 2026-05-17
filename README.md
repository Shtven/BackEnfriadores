# SEI - Backend (BackEnfriadores)

Servicio de logica de negocio, auditoria y API REST para el Sistema
de Enfriamiento Inteligente (SEI). Implementado con **Spring Boot**,
**Eclipse Paho** (MQTT) y **PostgreSQL**.

Es la **autoridad** del sistema: valida JWT, calcula alarmas, ejecuta
control de actuadores (cortina, refrigeracion, cierre automatico de
puerta) y persiste toda la trazabilidad HACCP.

## Responsabilidades del modulo

- Suscribirse al broker EMQX y consumir lecturas de los simuladores
  (temperatura, presencia, comandos del HMI).
- Evaluar umbrales y generar alarmas (`preventiva` > 3.0 °C,
  `critica` > 4.0 °C); silenciar criticas previa autorizacion.
- Ejecutar logica de control automatico: activacion/desactivacion de
  cortina, ajuste de potencia de refrigeracion y cierre automatico
  de puerta cuando temp > 4.0 °C y no hay presencia.
- Validar comandos manuales del HMI (re-verifica JWT firmado + rol).
- Persistir lecturas, alarmas, eventos de puerta, intervenciones
  manuales y estados de refrigeracion en PostgreSQL.
- Exponer API REST autenticada via JWT (login, historial, alarmas,
  intervenciones, registro dinamico de usuarios).

## Arquitectura interna

| Paquete                                     | Rol                                                                 |
|---------------------------------------------|---------------------------------------------------------------------|
| `com.codespace.simulator`                   | `SimulatorApplication` (entry point).                               |
| `com.codespace.simulator.config`            | Beans de Spring (CORS, Security, MQTT, seed inicial).               |
| `com.codespace.simulator.security`          | `JwtService` + `JwtAuthFilter` (HS256, validacion stateless).       |
| `com.codespace.simulator.controllers`       | Endpoints REST (`/api/*`).                                          |
| `com.codespace.simulator.dto`               | Request/Response DTOs.                                              |
| `com.codespace.simulator.entities`          | Entidades JPA (`Operador`, `Alarma`, `LecturaTemperatura`, ...).    |
| `com.codespace.simulator.repositories`      | `JpaRepository` por entidad.                                        |
| `com.codespace.simulator.services`          | Logica de negocio (alarmas, control, refrigeracion, usuarios).      |
| `com.codespace.simulator.mqtt`              | Cliente MQTT (`MqttSubscriberService`, `MqttPublisher`).            |
| `com.codespace.simulator.audit`             | Cola asincrona de auditoria (`AuditThread`, `AuditService`).        |
| `com.codespace.simulator.models`            | Eventos de dominio (`TemperaturaEvent`, `PresenciaEvent`).          |

## Requisitos previos

- Java 17
- Maven >= 3.8 (o usar el wrapper `mvnw` / `mvnw.cmd` incluido)
- PostgreSQL 16 corriendo en `localhost:5432`
- Broker EMQX accesible (default `1883`) - el SEI usa EMQX 5.x
  dockerizado en el laboratorio.

## Configuracion

1. Copiar la plantilla y editar con los valores reales:

   ```bash
   cp src/main/resources/application.properties.example \
      src/main/resources/application.properties
   ```

   En PowerShell:

   ```powershell
   Copy-Item src\main\resources\application.properties.example `
             src\main\resources\application.properties
   ```

2. Editar `application.properties` y completar:

   - `spring.datasource.url`, `spring.datasource.username`,
     `spring.datasource.password` (BD PostgreSQL).
   - `mqtt.broker.url`, `mqtt.username`, `mqtt.password` (broker EMQX).
   - `jwt.secret` (>= 32 caracteres, generar uno propio).

3. (Opcional) Crear `src/main/resources/application-local.properties`
   para overrides per-developer. Tambien esta en `.gitignore`.

## Inicializacion de la base de datos

Ver `src/main/resources/sql/README.md` para los pasos detallados.
Resumen:

```bash
psql -U postgres -h localhost -c "CREATE DATABASE backenfriadores;"
psql -U postgres -h localhost -d backenfriadores \
     -f src/main/resources/sql/schema.sql
```

El script crea las tablas (`cuartos`, `operadores`, `alarmas`,
`lecturas_temperatura`, `eventos_puerta`, `intervenciones_manuales`,
`refrigeracion_estado`) y siembra los 5 cuartos y 3 usuarios de
desarrollo. **Los hashes BCrypt de las passwords son placeholders**
que hay que reemplazar antes de aplicar el seed (ver el README de
`sql/` para opciones de generacion).

## Levantar en desarrollo

Linux / macOS:

```bash
./mvnw spring-boot:run
```

Windows / PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Verificar que arranco:

```bash
curl http://localhost:8080/api/login -X POST \
     -H "Content-Type: application/json" \
     -d '{"usuario":"jperez","password":"1234"}'
```

## Endpoints REST disponibles

Base URL: `/api` - Autenticacion JWT en header
`Authorization: Bearer <token>` salvo donde se indica.

| Metodo | Ruta                                     | Auth          | Descripcion                                                  |
|--------|------------------------------------------|---------------|--------------------------------------------------------------|
| POST   | `/api/login`                             | Publico       | Devuelve JWT a partir de `usuario` + `password`.             |
| POST   | `/api/usuarios/registro`                 | Publico       | Registro dinamico HACCP (supervisor: activo; operador: pendiente). |
| PATCH  | `/api/usuarios/{id}/aprobar`             | Operador activo | Aprueba una cuenta operadora pendiente.                    |
| GET    | `/api/usuarios/pendientes`               | Operador activo | Lista cuentas operadoras a la espera de aprobacion.        |
| POST   | `/api/alarmas/{cuartoId}/silenciar`      | JWT requerido | Silencia la alarma critica activa de un cuarto.              |
| GET    | `/api/alarmas/cuartos/{id}/alarma-activa`| JWT requerido | Devuelve la alarma activa del cuarto o `estado=normal`.      |
| GET    | `/api/cuartos/{id}/historial`            | JWT requerido | Historial de lecturas (`shortcut`, `desde`, `hasta`, `formato`). |
| GET    | `/api/cuartos/{id}/alarma-activa`        | JWT requerido | Variante de alarma activa expuesta por `HistorialController`. |
| GET    | `/api/intervenciones`                    | JWT requerido | Lista de intervenciones manuales (filtro por cuarto/rango).  |

Ver `API.md` en la raiz del repo para los payloads completos.

## Topicos MQTT publicados

Topics que el backend **publica** (el HMI y los simuladores los
consumen). Para los topics que el backend **escucha** ver el
contrato `10_E7_Contrato_MQTT.docx` del repo principal del proyecto.

| Topic                                       | Origen           | Payload (campos clave)                                                  |
|---------------------------------------------|------------------|-------------------------------------------------------------------------|
| `sei/cuartos/{n}/alarma`                    | `AlarmaService`  | `cuarto_id`, `estado` (normal/preventiva/critica), `temperatura`, `timestamp`. |
| `sei/cuartos/{n}/puerta`                    | `ControlService` | `cuarto_id`, `estado` (abierta/cerrada/cerrando/cierre_cancelado), `origen`, `timestamp`. |
| `sei/cuartos/{n}/cortina`                   | `ControlService` | `cuarto_id`, `estado` (activa/inactiva), `origen`, `timestamp`.         |
| `sei/cuartos/{n}/refrigeracion/estado`      | `ControlService` / `RefrigeracionService` | `cuarto_id`, `potencia_pct`, `motivo` (NORMAL/PUERTA_ABIERTA/FORZADO_MANUAL), `timestamp`. |

## Credenciales de desarrollo

Tras correr el seed (y reemplazar los hashes BCrypt placeholders):

| Usuario  | Password | Rol         |
|----------|----------|-------------|
| jperez   | 1234     | operador    |
| alopez   | 1234     | operador    |
| cruiz    | 1234     | supervisor  |

Recordatorio HACCP del SEI v3.0: **operador ACTUA, supervisor AUDITA**.

## Estructura del proyecto

```
BackEnfriadores/
├── .mvn/wrapper/
├── src/
│   ├── main/
│   │   ├── java/com/codespace/simulator/
│   │   │   ├── SimulatorApplication.java
│   │   │   ├── audit/             # AuditService, AuditThread
│   │   │   ├── config/            # CorsConfig, SecurityConfig, MqttConfig, AppConfig, OperadorSeedConfig
│   │   │   ├── controllers/       # AlarmaController, AuthController, HistorialController, IntervencionesController, UsuarioController
│   │   │   ├── dto/
│   │   │   │   ├── request/       # LoginRequest, RegistroRequest
│   │   │   │   └── response/      # LoginResponse, SilencioPayload, RegistroResponse, AprobacionResponse, UsuarioPendienteResponse
│   │   │   ├── entities/          # Alarma, EventoPuerta, IntervencionManual, LecturaTemperatura, Operador, RefrigeracionEstado
│   │   │   ├── models/            # PresenciaEvent, TemperaturaEvent
│   │   │   ├── mqtt/              # MqttListenerThread, MqttPublisher, MqttSubscriberService
│   │   │   ├── repositories/      # JpaRepository por entidad
│   │   │   ├── security/          # JwtAuthFilter, JwtService
│   │   │   └── services/          # AlarmaService, ControlService, HistorialService, RefrigeracionService, UsuarioService
│   │   └── resources/
│   │       ├── application.properties.example   # plantilla (versionada)
│   │       ├── schema.sql                       # migracion de arranque (Spring boot init)
│   │       └── sql/
│   │           ├── schema.sql                   # schema completo (inicializacion manual)
│   │           └── README.md
│   └── test/java/com/codespace/simulator/
├── API.md                                       # spec detallada de endpoints
├── README.md
├── pom.xml
├── mvnw / mvnw.cmd
└── .gitignore
```

## Dependencias del sistema completo

El SEI esta compuesto por cuatro modulos que viven en repos
separados pero comparten el broker MQTT como bus de integracion:

- **BackEnfriadores** (este repo) - autoridad de logica + API REST.
- **EMQX** (broker MQTT, dockerizado) - bus de integracion (1883).
- **Simuladores** (Python) - publican `sei/cuartos/{n}/temperatura`
  y `sei/cuartos/{n}/presencia`.
- **HMI** (`sei-hmi/`) - cliente React + bridge Node.js que traduce
  Socket.IO <-> MQTT y firma comandos con JWT.

Defensa en profundidad para acciones privilegiadas (silenciar
alarma, forzar cierre, forzar refrigeracion): el HMI esconde el
boton segun rol, el bridge re-valida `rol === 'supervisor'` (o
`operador`) antes de publicar, y este backend re-valida la firma
del JWT antes de ejecutar.

## Tests

```bash
./mvnw test                       # corre la suite JUnit
./mvnw -Dtest=ClassName test      # un solo test
./mvnw package                    # genera el jar en target/
```

Por ahora la cobertura es minima (solo el smoke test
`SimulatorApplicationTests`). El plan de pruebas funcionales del
proyecto vive en el repo `sei-hmi/` (checklists por HU).

## Notas de convencion

- Codigo, comentarios, mensajes de log y commits **en espanol**.
- Identificadores en espanol (`cuartoId`, `silenciarAlarma`,
  `forzar_cierre`, etc.).
- Conventional Commits para los mensajes (`feat:`, `fix:`,
  `chore:`, `docs:`, `refactor:`).
