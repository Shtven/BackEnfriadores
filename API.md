# API Endpoints

Base URL: /api
Autenticacion: JWT en header Authorization: Bearer <token> (excepto /api/login)

## AuthController

### POST /api/login
- Auth: Publico
- Body (JSON):
  - usuario: string
  - password: string
- Respuestas:
  - 200: LoginResponse
    - operador_id: integer
    - nombre: string
    - rol: string
    - jwt_token: string
  - 401: {"error": "Credenciales invalidas"}

## AlarmaController

### POST /api/alarmas/{cuartoId}/silenciar
- Auth: JWT requerido
- Path params:
  - cuartoId: integer
- Body (JSON):
  - operador_id: integer (requerido)
- Respuestas:
  - 200: {"mensaje": "Alarma critica silenciada correctamente", "cuarto_id": ..., "operador_id": ...}
  - 400: {"error": "operador_id es requerido y debe ser un entero"}
  - 409: {"error": "..."}

### GET /api/alarmas/cuartos/{id}/alarma-activa
- Auth: JWT requerido
- Path params:
  - id: integer
- Respuestas:
  - 200 (alarma activa):
    - cuarto_id: integer
    - estado: string
    - tipo: string
    - temperatura_pico: number
    - timestamp_inicio: string (ISO-8601)
  - 200 (sin alarma): {"cuarto_id": ..., "estado": "normal"}

## HistorialController

### GET /api/cuartos/{id}/historial
- Auth: JWT requerido (operador o supervisor)
- Path params:
  - id: integer
- Query params (opcionales):
  - shortcut: 24h | 7d | 30d
  - desde: ISO-8601 OffsetDateTime
  - hasta: ISO-8601 OffsetDateTime
  - formato: json | csv (default json)
- Respuestas:
  - 200 (json): respuesta JSON generada desde lecturas
  - 200 (csv): text/csv con Content-Disposition attachment

## IntervencionesController

### GET /api/intervenciones
- Auth: JWT requerido
- Query params (opcionales):
  - cuartoId: integer
  - desde: ISO-8601 OffsetDateTime (default: now - 30d UTC)
  - hasta: ISO-8601 OffsetDateTime (default: now UTC)
- Behavior:
  - rol=supervisor: devuelve todas las intervenciones
  - rol=operador: solo sus propias intervenciones
- Respuesta:
  - 200: lista de IntervencionManual
