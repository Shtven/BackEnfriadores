# Simulator Backend

## Quick context
This service consumes and publishes MQTT events and persists door events in PostgreSQL. The current domain model for `eventos_puerta` uses `accion` and `origen` (no mandatory `tipo`).

## MQTT configuration
The broker URL is built from environment variables:
- `MQTT_BROKER_HOST` (default: `192.168.137.9`)
- `MQTT_BROKER_PORT` (default: `1883`)

If the broker runs on another machine or network, set these vars so the backend can reach it.

## Database schema alignment
If your database still requires `eventos_puerta.tipo NOT NULL`, inserts will fail. This repo includes a startup migration in `src/main/resources/schema.sql` that drops the NOT NULL constraint.

Notes:
- The startup SQL runs because `spring.sql.init.mode=always` is set in `application.properties`.
- `spring.sql.init.continue-on-error=true` keeps startup from failing if the column doesn't exist.
- The SQL only relaxes the constraint; it does not remove the `tipo` column.

## Running locally
Use your usual Spring Boot workflow (Maven wrapper or IDE run). Adjust `application.properties` or environment variables as needed for your database and broker.
