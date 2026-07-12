# Production Readiness

An honest accounting of what's done, what's deliberately deferred, and what would need to change before this ran real traffic. Written to be read by a future maintainer (or an interviewer), not to make the project look more finished than it is.

## Done

**Security** — JWT access + rotating refresh tokens, BCrypt passwords, ownership enforcement on every resource, user-enumeration-resistant auth errors, no client-trusted user identity anywhere in the request path, fail-fast production secrets.

**Correctness** — Bean Validation on every request DTO, RFC 9457 structured error responses via a centralized `@RestControllerAdvice`, BigDecimal for all money (no `double`), server-side-recomputed financial totals, a real database unique constraint closing the invoice-number race condition (not just an application-level check).

**Infrastructure** — Flyway-managed schema (`ddl-auto=validate`, not `update`), dev/prod Spring profiles, multi-stage Docker builds for both services with non-root users and healthchecks, Docker Compose with healthcheck-gated startup ordering, Actuator health/readiness probes, Swagger UI with JWT bearer auth wired up, GitHub Actions CI running the backend test suite against a real Postgres service container and linting/building the frontend.

**Code quality** — dead code removed (two empty stub classes on the backend, one orphaned unrendered JSX block on the frontend), duplicated logic consolidated (dashboard stats had two independent implementations; PDF file paths were built independently in two places with no compiler-enforced link between them), magic strings extracted to named constants.

**Testing** — ~55 backend unit/slice tests covering JWT lifecycle, refresh token rotation, password policy, the full auth service (including a regression test locking in the user-enumeration fix), ownership enforcement on both read and write paths, invoice financial-integrity validation, hash determinism and tamper detection, and centralized-exception-handler HTTP-status mapping. Frontend: `tsc --noEmit` and `eslint` both pass clean with zero errors.

## Known gaps (disclosed, not hidden)

| Gap | Why it's still open | Where to start |
|---|---|---|
| No integration tests against a real database (Testcontainers or live Postgres) | The sandbox this project was iterated in had no Docker/Postgres access to actually run one | Add a `testcontainers-postgresql` dependency, one `@SpringBootTest` per controller exercising the real Flyway-migrated schema |
| No rate limiting on `/api/auth/login` or `/api/invoices/verify/**` | Needs an infra decision (gateway vs. in-process library vs. shared store for multi-instance) explicitly out of this project's scope list | Bucket4j for a single instance; needs Redis (also excluded) for multi-instance |
| PDF storage is local disk, not object storage | Fine for a single instance; breaks the moment there's more than one backend instance behind a load balancer | Swap `PDFGenerator`'s file-write for an S3-compatible client; `app.storage.pdf-dir` config already isolates this to one class |
| No `hashVersion` field on `Invoice` | Any future change to the hash's canonical-string format breaks verification for every invoice hashed under the old format, with no way to dispatch to the right algorithm | Add the column via a new Flyway migration, branch `InvoiceHashUtil` on it |
| No independent tax-rate verification | `tax` is checked for arithmetic consistency only, not re-derived from a jurisdiction/client tax-rate config that doesn't exist in the domain model | Add a tax-rate field to `Client` or a separate jurisdiction-rate table |
| No `GET /api/users/me` endpoint | Frontend profile page can only show what's in the JWT (email, user ID) — no display name | Small backend addition; frontend already isolates this to one page |
| `spring-boot-starter-parent` is a SNAPSHOT | Spring Boot 4.0.0 was unreleased at the time this was built — build reproducibility is only as good as that snapshot | Pin to the GA release the moment it ships; requires a working Maven build to verify (this project's tooling environment had none) |
| Not compiler-verified end to end | Backend code (all of Phases A–E) was reviewed carefully against documented Spring/JJWT/Hibernate API behavior but never actually compiled in this environment (no Maven, no JDK 21, no root access) | Run `./mvnw clean verify` locally before relying on this in a live demo — this is the single highest-priority verification step before an interview |

## Pre-deployment checklist

- [ ] Run `./mvnw clean verify` locally and confirm all ~55 tests pass
- [ ] Run `docker compose up --build` locally and confirm all three services reach a healthy state
- [ ] Generate a real `JWT_SECRET` (`openssl rand -base64 48`) and a real database password — never reuse the dev defaults
- [ ] Update `SecurityConfig`'s CORS allowed origins from `localhost:3000` to the real frontend domain
- [ ] Update `app.verification.base-url` / `NEXT_PUBLIC_API_URL` to production URLs
- [ ] Decide on a rate-limiting strategy for `/api/auth/login` and `/api/invoices/verify/**` before exposing either publicly
- [ ] Pin `spring-boot-starter-parent` to a GA release once Spring Boot 4.0.0 ships
- [ ] Point `app.storage.pdf-dir` at durable, backed-up storage (or migrate `PDFGenerator` to object storage) if running more than one backend instance

## Roadmap (beyond the pre-deployment checklist)

Rate limiting; S3-compatible PDF storage; `hashVersion`-aware hash verification; per-client tax-rate configuration; a `GET /api/users/me` endpoint and a real profile-editing page; Testcontainers-based integration tests; multi-currency reporting/export; email delivery of invoices with the verification link pre-filled.

## Final engineering self-assessment

Scored against what a Staff/Principal-level reviewer would look for, not against "does it demo well."

| Area | Score | Why |
|---|---|---|
| Security | 8/10 | Real JWT + rotation + ownership enforcement + BigDecimal money is genuinely solid; loses points for no rate limiting and an unverified (uncompiled) implementation |
| Architecture | 7/10 | Clean two-tier monolith, appropriately sized for scope, no premature complexity; loses points for the local-disk PDF storage single-instance assumption and the still-open hash-versioning gap |
| Code quality | 7/10 | Centralized error handling, no dead code, tests for the security-critical paths; loses points for being reviewed rather than compiler-verified on the backend |
| Production readiness | 6/10 | Docker/Compose/CI/Flyway/health-checks all real and wired together; loses points for zero integration-test coverage and the SNAPSHOT dependency risk |
| Resume/interview value | 9/10 | The security fixes (user-enumeration, IDOR via trusted userId, floating-point money) are concrete, explainable, and map directly to common interview questions — see `INTERVIEW_PREP.md` |

The honest summary: this is a strong "I understand production concerns and can articulate trade-offs" project, not a "this has run in production" project. Say exactly that if asked — it's a more credible answer than overclaiming, and every gap above has a specific, technically grounded reason it's still open rather than a vague "TODO."
