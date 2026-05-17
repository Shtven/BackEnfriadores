# Inicializacion de la base de datos

Este directorio contiene el script de inicializacion **manual** para
crear la base de datos `backenfriadores` desde cero.

> No confundir con `src/main/resources/schema.sql` (al lado del jar),
> que es una migracion puntual que Spring ejecuta en cada arranque
> via `spring.sql.init.mode=always`.

## Pre-requisitos

- PostgreSQL 16 corriendo en `localhost:5432` (o el host configurado
  en `application.properties`).
- Acceso al usuario administrador `postgres` (o equivalente).

## Pasos

1. Crear la base de datos:

   ```bash
   psql -U postgres -h localhost -c "CREATE DATABASE backenfriadores;"
   ```

2. Ejecutar el schema:

   ```bash
   psql -U postgres -h localhost -d backenfriadores \
        -f src/main/resources/sql/schema.sql
   ```

   En Windows / PowerShell:

   ```powershell
   psql -U postgres -h localhost -d backenfriadores `
        -f src\main\resources\sql\schema.sql
   ```

3. **Generar los hashes BCrypt para los usuarios seed** (`jperez`,
   `alopez`, `cruiz`, todos con password `1234`). El script deja
   placeholders `<bcrypt_hash_de_1234>` que hay que reemplazar
   antes de correrlo, o bien aplicar los `UPDATE` manualmente
   despues.

   Opciones para generar los hashes:

   - **Via el endpoint del back** (mas comodo, requiere el back
     levantado): `POST /api/usuarios/registro` con
     `{ "nombre": "...", "usuario": "...", "password": "1234",
        "rol": "operador" }`. El back hashea internamente con BCrypt.
     Despues, para operadores, hay que aprobar la cuenta via
     `PATCH /api/usuarios/{id}/aprobar`.
   - **Manual con htpasswd**:
     `htpasswd -nbB jperez 1234` (toma la parte despues de `:`).
   - **Via pgcrypto en PostgreSQL**:
     ```sql
     CREATE EXTENSION IF NOT EXISTS pgcrypto;
     UPDATE operadores
        SET password_hash = crypt('1234', gen_salt('bf', 10))
      WHERE usuario IN ('jperez', 'alopez', 'cruiz');
     ```

## Usuarios de desarrollo

| Usuario  | Password | Rol         | Estado |
|----------|----------|-------------|--------|
| jperez   | 1234     | operador    | activo |
| alopez   | 1234     | operador    | activo |
| cruiz    | 1234     | supervisor  | activo |

Estas credenciales son SOLO para desarrollo y demo academica.
No usar en ambientes con datos reales.

## Re-inicializar desde cero

Si necesitas resetear todo (por ejemplo despues de un cambio de
schema que `ddl-auto=update` no puede aplicar):

```bash
psql -U postgres -h localhost -c "DROP DATABASE backenfriadores;"
psql -U postgres -h localhost -c "CREATE DATABASE backenfriadores;"
psql -U postgres -h localhost -d backenfriadores \
     -f src/main/resources/sql/schema.sql
```
