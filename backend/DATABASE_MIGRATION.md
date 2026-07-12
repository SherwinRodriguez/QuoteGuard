# Database Migrations

Schema changes are managed entirely by [Flyway](https://flywaydb.org/), not manually. Migration scripts live in `src/main/resources/db/migration/` (currently just `V1__init_schema.sql`, the full baseline schema).

`spring.jpa.hibernate.ddl-auto=validate` (see `application.properties`) means Hibernate checks the entity mappings against whatever schema Flyway has produced at startup and **fails fast on any drift** — it will never auto-create or auto-alter the schema. This replaced an earlier `ddl-auto=update` setup, which silently mutated the live schema on every boot and was not safe with more than one developer or environment.

## Adding a new migration

1. Create a new file in `src/main/resources/db/migration/`, named `V{next_number}__{description}.sql` (e.g. `V2__add_client_tax_rate.sql`). Flyway applies migrations in version order and tracks which ones have already run in a `flyway_schema_history` table it manages itself.
2. Write plain SQL — `ALTER TABLE`, `CREATE INDEX`, etc.
3. Update the corresponding JPA entity to match.
4. Start the application. Flyway applies the new migration automatically on boot, then Hibernate's `validate` mode confirms the entity mapping agrees with the result.

Do not hand-edit the database schema outside of a Flyway migration file — Flyway checksums each applied migration, and manual out-of-band changes will desync the schema history and cause `ddl-auto=validate` to fail on the next boot (which is the intended, correct failure mode: better a fast failure at startup than silent drift).

## Local reset

For local development, the simplest reset is to drop and recreate the database, then let Flyway rebuild it from scratch on next boot:

```sql
DROP DATABASE quoteguard;
CREATE DATABASE quoteguard;
```

(Or, with Docker Compose: `docker compose down -v` to drop the named volume, then `docker compose up` to rebuild from an empty database.)
