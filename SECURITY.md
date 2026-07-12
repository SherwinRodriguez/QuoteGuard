# Security

This document describes QuoteGuard's threat model, what's mitigated, and what's explicitly out of scope. Every claim below traces to a specific file.

## Authentication

Access tokens are HS256-signed JWTs (`security/JwtService.java`), subject = user ID (immutable, unlike email), 15-minute default lifetime. Refresh tokens are opaque random values, **not** JWTs — they're generated server-side, hashed with SHA-256 before storage (`security/RefreshTokenService.java`), and the raw value is returned to the client exactly once, at issuance. The database only ever holds the hash, the same principle as password storage: a leaked database dump doesn't hand out usable refresh tokens.

Refresh tokens rotate on every use — `consume()` deletes the old token and `issue()` creates a new one atomically within the same request. A stolen refresh token that's been used once by its rightful owner is worthless to an attacker holding a stale copy; the token they have was already deleted server-side. Logout revokes the current refresh token server-side (`revoke()`), and an hourly scheduled task (`RefreshTokenCleanupTask`) purges expired ones so the table doesn't grow unbounded.

Passwords are hashed with BCrypt (Spring Security's `PasswordEncoder`, cost factor default). `PasswordPolicy.validate()` requires at least 8 characters with a letter and a digit — deliberately simple; the security literature has moved away from complex composition rules (NIST SP 800-63B) in favor of length and breach-list checking, and breach-list checking is out of scope here (would require a third-party API).

## The user-enumeration fix

`AuthService.login()` and `.refresh()` throw the exact same `AuthenticationFailedException("Invalid email or password")` regardless of whether the email doesn't exist, the password is wrong, or the refresh token is invalid/expired. The original implementation distinguished "user not found" from "password mismatch" with different messages — a textbook user-enumeration vector, since an attacker could confirm which emails have accounts by watching which error came back. Locked in as a regression test: `AuthServiceTest.login_wrongPassword_throwsTheExactSameMessageAsUnknownEmail`.

## Authorization

Every resource (client, invoice) has an owning user. Every read/write/delete on a specific resource by ID goes through an ownership check that throws `AccessDeniedException` (→ 403) for a resource that exists but isn't yours, and `ResourceNotFoundException` (→ 404) for one that doesn't exist at all — the two are handled as genuinely different cases, not collapsed into a single "not found" for everything (collapsing them would technically hide *some* information from an attacker probing IDs, but at the cost of legitimate debugging and audit clarity; this is a judgment call, not an oversight).

The acting user's identity is **always** resolved server-side from the validated JWT (`@AuthenticationPrincipal(expression = "id")`), never from a request parameter, path variable, or request body. This closes what was the single largest vulnerability in the original prototype: `InvoiceRequest` used to accept a client-submitted `userId` field and trust it outright — any authenticated user could create an invoice under any other user's identity just by changing one field in the request body. That field no longer exists in the DTO at all; it isn't validated away, it's structurally impossible to submit.

## Financial integrity

`InvoiceService.createInvoice()` always recomputes `subtotal` from line items server-side and rejects any submitted `totalAmount` that doesn't equal `subtotal + tax`. A client can submit whatever subtotal it wants; it's discarded. This closes a class of attack where a manipulated frontend (or a direct API call bypassing the frontend entirely) submits a suppressed total.

One deliberate, disclosed limitation: `tax` itself is accepted as submitted and checked only for arithmetic consistency, not independently re-derived from a tax rate. The domain model has no per-client/jurisdiction tax-rate configuration to check against — closing this fully would mean adding that configuration, which is a real feature, not a bug fix, and is listed in `PRODUCTION_READINESS.md`'s roadmap rather than silently left unmentioned.

## Invoice integrity (the core feature)

Each invoice's SHA-256 hash (`utils/InvoiceHashUtil.java`) is computed once, at creation, from a canonical string built from every immutable financial and identity field (user ID, invoice number, dates, currency, subtotal, tax, total, and a deterministically sorted list of line items). The hash is stored and never recomputed to match new data — verification means recomputing the hash from the invoice's current row and comparing it to the stored one; any mismatch means the row was altered after the fact (or the code that reads it changed in a hash-affecting way — see `InvoiceHashUtil`'s Javadoc on the missing `hashVersion` field, an open gap).

Money is formatted via `BigDecimal.setScale(2, HALF_UP).toPlainString()`, deliberately not `String.format("%.2f", ...)`, which is locale-sensitive (some JVM locales render a decimal comma) — the same invoice could otherwise hash differently purely based on server locale, unrelated to its actual content.

The QR code embedded in the generated PDF encodes **only** a verification URL (`{baseUrl}/verify/{uuid}`) — never the invoice amount, client info, or hash value itself. This is intentional: the QR code is a pointer to a live lookup, not a self-contained bearer credential. If it encoded the amount directly, a tampered PDF with a matching tampered QR code would pass a naive "does the QR code agree with the PDF" check without ever touching the server.

## What's explicitly out of scope (and why)

**Rate limiting** is not implemented. The public verification endpoint and the login endpoint are both unauthenticated-reachable and have no throttling — a scripted client could hammer either. Mitigating this properly means either an API gateway (Spring Cloud Gateway) or an in-process library (Bucket4j), both of which are real infrastructure decisions with their own trade-offs (in-memory rate limiting doesn't survive a restart or work across multiple instances without a shared store like Redis, which was explicitly excluded from this project's scope). Listed as a known gap in `PRODUCTION_READINESS.md`, not silently omitted.

**CORS** is currently configured for `http://localhost:3000` only (`SecurityConfig.corsConfigurer`) — correct for local development, and something any real deployment must update to the actual production frontend origin before going live.

**Actuator exposure** is deliberately narrow: only `health` and `info` are exposed (`management.endpoints.web.exposure.include=health,info`). The rest of Actuator's surface (`env`, `beans`, `threaddump`, etc.) is not exposed, since several of those endpoints leak configuration or environment details that shouldn't be reachable on a public API even behind authentication.

**Secrets**: `JWT_SECRET`, database credentials, and all other sensitive configuration are environment-variable-driven with **no default** in the production profile (`application-prod.properties`) — a prod boot without `JWT_SECRET` set fails fast at startup instead of silently signing tokens with a well-known development secret. The development profile does have a throwaway default, clearly labeled as such, so local development doesn't require secret management for a trivial local run.
