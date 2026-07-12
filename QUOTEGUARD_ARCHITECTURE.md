# QuoteGuard — Architecture Document (Phase 1)

> **Status note:** this document is the Phase 1 audit, written *before* the Phase A–F rebuild (JWT auth, ownership enforcement, RFC 9457 error handling, BigDecimal money, Flyway/Docker/CI, backend refactoring, a real test suite, and a frontend rewrite off fake auth). It's kept as-is rather than rewritten, because the "Consolidated Architectural Weaknesses" section below is the actual before/after record this project was built against — most of the numbered weaknesses in Section 11 are now resolved. For the current state of the system, see [`README.md`](./README.md) and [`PRODUCTION_READINESS.md`](./PRODUCTION_READINESS.md). Everything in Sections 1–10 (overall shape, package structure, request lifecycle, the various flows) still accurately describes the system's *topology* even though implementation details inside each flow have since changed (e.g. "Request Lifecycle" now includes a JWT filter that didn't exist when this was written).

Scope: this document describes the system **as it existed in the repository at the start of this project**, not as it exists today. Every claim below traces to a specific class or file. Weaknesses are architectural/systemic — line-item bugs, OWASP findings, and code-style issues are deliberately deferred to Phase 2.

---

## 1. Overall Architecture

QuoteGuard is a two-tier monolith: a single Spring Boot backend and a single Next.js frontend, talking over plain REST/JSON, backed by one PostgreSQL database. There is no gateway, no BFF, no message broker, no cache layer, and no secondary service — everything lives in `backend/` (Spring Boot, Java 21, Maven) and `frontend/` (Next.js 15 App Router, React 19, TypeScript).

```
┌─────────────────────┐        HTTP/JSON        ┌──────────────────────────┐        JDBC        ┌──────────────┐
│  Next.js Frontend    │ ───────────────────────▶│  Spring Boot Backend     │ ───────────────────▶│  PostgreSQL   │
│  (App Router, React) │◀─────────────────────── │  (Controller→Service→    │◀─────────────────── │  (quoteguard) │
│  localhost:3000      │      raw fetch()         │   Repository, JPA)       │                     └──────────────┘
└─────────────────────┘                          └──────────────────────────┘
                                                            │
                                                            ▼
                                                   local filesystem
                                                   generated/invoices/*.pdf
```

This is an appropriate topology for the current scale (single team, pre-launch, single tenant model). The problems in this system are not "you need microservices" — they are that even within this simple two-tier shape, several trust and durability assumptions are wrong. That distinction matters: the fix is depth, not more infrastructure.

---

## 2. Package Structure

**Backend — `com.quoteguard`**

```
controller/   AuthController, ClientController, DashboardController, InvoiceController
service/      AuthService, ClientService, InvoiceService, DashboardService (dead), QRCodeService (empty)
repository/   UserRepository, ClientRepository, InvoiceRepository   (Spring Data JPA interfaces)
entity/       User, Client, Invoice, InvoiceItems, InvoiceStatus (enum)
dto/          LoginRequest, InvoiceRequest, InvoiceItemRequest, InvoiceResponse, InvoiceDetailResponse,
              ClientResponse, ItemResponse, RevokeInvoiceRequest, VerificationResponse
exception/    GlobalExceptionHandler (empty — no annotations, does nothing)
utils/        InvoiceHashUtil, PDFGenerator, TokenUtils (empty)
config/       SecurityConfig
BackendApplication.java  (entry point)
```

Flat, conventional layered-by-technical-concern packaging (`controller` / `service` / `repository` / `entity`), not domain/feature packaging. That's a reasonable default at this size (4 controllers, 4 real services). It stops being reasonable once the domain grows past ~2 bounded concepts (invoices, clients) — noted here, not acted on, because splitting packages now would be premature for a 1-week runway.

**Frontend — `frontend/src`**

```
app/            login/, register/, verify/, settings/, dashboard/{clients,invoices,profile}, layout.tsx, page.tsx
components/     auth/ProtectedRoute, layout/{Navbar,Sidebar,About}, invoice/{InvoiceItemRow,PdfPreview,QrDisplay} (empty),
                ui/{Button,Input,Table} (Table empty), shared/{Loader,Toaster}
context/        AuthContext.tsx
services/       invoice.ts (real), api.ts / auth.ts / client.ts (empty, unused)
lib/            auth.ts (empty), invoice.ts, utils.ts (empty)
types/          index.ts, invoice.ts
constants/      routes.ts (empty)
```

Structurally this looks like a "service layer + component library" architecture, but roughly a third of the files that imply that structure are empty stubs. **The intended architecture (what the folder names promise) and the actual architecture (what the code does) have already diverged** — most page components call `fetch()` directly against a hardcoded backend URL rather than going through `services/`. This is flagged here because it's the frontend's core architectural problem, not a style nitpick: there is no real API layer despite the folder existing for one.

---

## 3. Request Lifecycle (as implemented)

For a representative authenticated action — e.g., `GET /api/invoices?userId=3` — the actual path a request takes today is:

1. Browser reads `userId` out of `localStorage` (set once, at login, never re-validated).
2. `fetch()` call goes directly from a page component to `http://localhost:8080/api/invoices?userId=3` — no interceptor, no auth header, no shared client.
3. Spring's `DispatcherServlet` routes to `InvoiceController.getInvoicesByUser`.
4. Spring Security's filter chain runs first (`SecurityConfig.filterChain`) — but since `/api/**` is `permitAll()`, the chain does no meaningful authentication or authorization work; it passes every request through, including ones with a forged or absent `userId`.
5. Controller calls `InvoiceService.getAllInvoicesByUser(userId)` with the **client-supplied** id, no cross-check against any session.
6. Service calls `InvoiceRepository.findByUser_Id(userId)`, maps entities to `InvoiceResponse` DTOs.
7. Response serializes back as JSON; no request ID, no correlation ID, no structured log entry ties this request to anything traceable.

**Architecturally significant:** step 4 is supposed to be the trust boundary in a layered web application — the point where "outside world" becomes "authenticated internal request." In this system that boundary does not functionally exist. Every layer below it (service, repository) is written as if step 4 already validated identity, but it never does. This is the single largest architectural gap in the system and it is the reason the product's own core value proposition (tamper-evidence, ownership, revocation) cannot currently be trusted end-to-end.

---

## 4. Authentication Flow (as implemented)

```
POST /api/auth/register  →  AuthService.RegisterUser
    - checks email uniqueness
    - BCrypt-hashes password (correct, this part is sound)
    - saves User with role="USER" (role is never read anywhere downstream)

POST /api/auth/login  →  AuthService.LoginUser
    - looks up by email, compares BCrypt hash
    - returns a Map: {"message": "Successfully logged in", "userId": 3}
    - NO TOKEN IS ISSUED. There is no JWT, no session, no signed artifact of any kind.

Frontend (login/page.tsx):
    - stores localStorage["userId"] = 3           (plain, unsigned, editable in devtools)
    - calls AuthContext.login(data.message)        → stores localStorage["token"] = "Successfully logged in"
    - AuthContext only ever checks !!localStorage["token"] to decide isLoggedIn — it is a truthy-string check,
      not a credential. The "token" is never sent back to the server on any subsequent request.
```

There is no authentication flow in the architectural sense — there is a login **form** that produces two unsigned, unverifiable strings, one of which (`userId`) is subsequently treated by the entire backend as an authoritative identity claim. `TokenUtils.java` exists as an empty class, which tells you a JWT layer was planned and never built — the architecture has a JWT-shaped hole in it (`SecurityConfig`'s comment: *"TODO: Enable JWT auth for production"*).

---

## 5. Invoice Generation Flow

```
1. NewInvoicePage (frontend) collects client, items, tax rate.
2. Frontend computes subtotal = Σ(qty × unitPrice), tax = subtotal × rate, total = subtotal + tax — in the browser.
3. POST /api/invoices  body: { ..., subtotal, tax, totalAmount, items[], clientId, userId }
4. InvoiceController.createInvoice → InvoiceService.createInvoice (single @Transactional method):
     a. loads User by request.userId           (no ownership/session check — see §4)
     b. loads Client by request.clientId        (no check that the client belongs to that user)
     c. generates invoiceNumber if absent:  "INV-{userId}-{epochMillis}"
     d. checks existsByUserIdAndInvoiceNumber   (check-then-act — no DB unique constraint backs this)
     e. builds Invoice entity + UUID (also re-generated in @PrePersist if still null — double UUID logic, see below)
     f. builds InvoiceItems, attaches to invoice
     g. InvoiceHashUtil.generateHash(invoice)   → computed from the fields AS SUBMITTED, not re-derived
     h. invoiceRepository.saveAndFlush(invoice) → row committed, hash now physically immutable (updatable=false)
     i. PDFGenerator.generateInvoicePdf(...)     → synchronous, inside the same request thread
        - on exception: caught, printed to stdout, swallowed — method returns success regardless
5. Response: plain string "Invoice created with UUID: {uuid}" — not a typed DTO.
```

**Architecturally significant points:**
- Step (g) hashes numbers the server never independently verified — the hash is a strong guarantee of *no tampering after step (h)*, and zero guarantee of *correctness at* step (h). Those are different properties and the current design only delivers the first one, while the product pitch implies both.
- Step (i) means "invoice created successfully" and "PDF artifact exists" are two different facts that the architecture currently conflates into one API response. A caller who gets a 200 has no reliable signal about whether the PDF exists.
- UUID is generated in two places (`InvoiceService` before build, and again defensively in `Invoice.onCreate()` via `@PrePersist` if still null) — harmless today because the builder always sets it first, but it's a sign the entity's invariants are enforced in two different layers with duplicated logic instead of one authoritative place.

---

## 6. Verification Flow (public, unauthenticated by design — correctly so)

```
QR code (embedded in PDF) encodes ONLY:  {baseUrl}/verify/{uuid}
Scanner/browser → GET /api/invoices/verify/{uuid}   (correctly permitAll() in SecurityConfig)

InvoiceService.verifyInvoice(uuid):
    1. lookup by uuid — not found            → VerificationResponse.notFound()
    2. found, status == REVOKED              → VerificationResponse.revoked(...)
    3. found, status == ACTIVE:
         recompute hash from CURRENT stored row → InvoiceHashUtil.verifyHash(invoice)
         mismatch  → VerificationResponse.modified(...)
         match     → VerificationResponse.verified(...)
```

This flow is the best-designed part of the system. It is public by explicit, correct design (`SecurityConfig` carves out `/api/invoices/verify/**` and `/verify/**`), it fails closed (unknown UUID → `NOT_FOUND`, not a 500 or a leak), and the four-state result (`VERIFIED / REVOKED / MODIFIED / NOT_FOUND`) is a genuinely well-thought-out state machine for this domain. The only architectural weakness here is downstream of §5: verification can only be as trustworthy as invoice creation, and invoice creation currently has no identity or financial-integrity boundary.

---

## 7. Database Relationships

```
User (1) ──────< (N) Client
User (1) ──────< (N) Invoice
Client (1) ─────< (N) Invoice          [Client.invoices, CascadeType.ALL — cascades delete/update onto Invoice]
Invoice (1) ────< (N) InvoiceItems     [CascadeType.ALL, orphanRemoval = true]
```

- `Invoice.uuid` — `unique=true`, correct, and it's the only column with an explicit uniqueness constraint in the whole schema.
- `Invoice.invoiceNumber` — **no** `unique=true` and no composite DB constraint on `(user_id, invoice_number)`; uniqueness is enforced only by an application-level check-then-save with no locking, which is a real race window under concurrent requests.
- `Client.invoices` is `CascadeType.ALL` — this means a JPA-level cascading delete is *available* on the Client→Invoice edge, in direct tension with the product's own invariant that invoices are never deleted. Today `ClientService.deleteClient` guards this manually (rejects delete if the client has invoices), but the cascade is still configured at the ORM level as if deletion were an intended operation — the safety here lives in one `if` statement in a service method, not in the schema. That's a fragile place to put an invariant this important.
- No DB indexes are declared anywhere beyond what `unique=true` and primary keys imply. `findByUser_Id`, `findByUuid`, `countByUserId` etc. all rely on Postgres's automatic PK/unique indexes or full scans — acceptable at current data volume, a known scaling gap.
- `Invoice.paid` — a `@Deprecated boolean` column with no migration path off of it; it's still queried in `InvoiceRepository.countByUserIdAndPaidFalse` and surfaced on the dashboard, so "deprecated" here means "known to be wrong," not "unused."

---

## 8. PDF Generation Flow

```
PDFGenerator.generateInvoicePdf(invoice, filePath):
    - ensures parent dir exists (generated/invoices/)
    - iText5 (com.itextpdf.text.*) Document + PdfWriter → FileOutputStream(filePath)
    - writes freelancer info, client info, invoice metadata, line-item table, totals
    - generates QR image in-memory (ZXing) and embeds it
    - writes directly to local disk: "generated/invoices/invoice-{id}.pdf"
```

Two different PDF libraries are present in `pom.xml` simultaneously: **iText7 core/kernel/layout 8.0.2** and **legacy iTextPdf 5.5.13.3** — `PDFGenerator` only uses the legacy 5.x API (`com.itextpdf.text.*`). The 7.x dependency is dead weight pulled into every build. Architecturally, PDF generation is a filesystem side-effect coupled directly into the transactional invoice-creation request path, with no abstraction (`StorageService`, `DocumentGenerator` interface) between "generate a PDF" and "iText, specifically, writing to `java.io.File`, specifically." Swapping storage backends or PDF engines later means rewriting this class, not swapping a config value.

---

## 9. QR Verification Flow

```
QRCodeWriter (ZXing) encodes exactly one string: "{app.verification.base-url}/verify/{uuid}"
→ BitMatrix → BufferedImage → embedded into the PDF via iText Image.getInstance(...)
```

This is correctly minimal — the QR carries no amounts, no client data, no hash value, only a pointer to the public verification endpoint (documented explicitly in a code comment: *"DO NOT encode: Invoice amounts, Client info, Hash values"*). That's the right design decision: it means QR payload size stays small and scannable, and it means the QR itself carries no sensitive data if intercepted. `QRCodeService.java` exists as an empty class beside this — another planned-but-never-built abstraction; the real QR logic lives inline inside `PDFGenerator`, which is itself a minor SRP violation (a class named for PDF generation is also the class solely responsible for QR encoding).

---

## 10. Hash Generation Flow

```
InvoiceHashUtil.generateHash(invoice):
    canonical = "user_id:{id}|invoice_number:{num}|issue_date:{iso}|due_date:{iso}|currency:{cur}|
                 subtotal:{%.2f}|tax:{%.2f}|total_amount:{%.2f}|items:[{sorted by product name}]"
    SHA-256(canonical, UTF-8) → hex string → Invoice.invoiceHash  (column: updatable=false)

verifyHash(invoice):
    recompute generateHash(invoice) from the CURRENTLY STORED row, compare to stored invoiceHash
```

Genuinely good design decisions here: a **canonical string** (not raw JSON, which is not stable across serialization order/whitespace), **deterministic item ordering** (sorted by product name, so two logically-identical item lists always hash the same), and **decimal normalization** (`%.2f` before hashing, so `18.0` and `18.00` don't produce different hashes). This is exactly the kind of thinking a hashing scheme for long-lived financial documents needs.

**Two architectural gaps, both forward-looking rather than "broken today":**
- **No algorithm/schema versioning.** The canonical string format is hardcoded into one method with no `hashVersion` recorded per invoice. The moment this format needs to change — a bug fix in the rounding, a new field added to the hash — every previously issued invoice becomes unverifiable under the new code unless the old algorithm is preserved and dispatched on a version field. For a system whose entire value proposition is *long-term* verifiability, this is a real gap, not a hypothetical one.
- **`%.2f` uses default locale-sensitive formatting** (`String.format` without an explicit `Locale`) — on a JVM running under certain locales this can produce a decimal comma instead of a decimal point, silently changing the canonical string and therefore the hash, purely based on server locale configuration rather than invoice content. This is a subtle but real correctness risk for a hash function that must be 100% reproducible regardless of environment.

---

## 11. Consolidated Architectural Weaknesses

These are systemic/structural issues — each one reflects a wrong assumption baked into the shape of the system, not a single bad line:

1. **No functional trust boundary.** `permitAll()` on `/api/**` means the architecture's intended authentication layer is a no-op. Every downstream layer is written as if identity were already established; it never is.
2. **Inverted trust direction for identity.** The system asks the client which user it is (`userId` param) instead of deriving identity server-side from a verified credential. This is not a missing feature bolted onto an otherwise-sound design — it's the opposite of how the rest of the code is written, which is why fixing it touches every controller.
3. **Entity/DTO boundary collapse.** `ClientController` accepts and returns the JPA `@Entity Client` directly as the REST contract, coupling the public API shape to the persistence schema.
4. **Transactional and durability boundaries don't match.** Invoice creation and PDF generation are treated as one atomic unit of work by the caller (single request, single response) but are actually two independent failure domains — the DB commit can succeed while the filesystem write silently fails, and the API gives no way to detect or reconcile that split.
5. **Local disk as the artifact store.** PDFs are addressed by local file path on whichever instance generated them — incompatible with horizontal scaling or container restarts without a persistent, shared volume, and with no backup story.
6. **No storage/notification abstraction.** PDF generation is hard-wired to `java.io.File` with no interface boundary, so changing the storage target is a rewrite.
7. **Hash has no version field.** The verification guarantee is bound to one hardcoded canonical-string algorithm with no forward-compatibility path.
8. **The financial trust boundary is unguarded.** Client-submitted totals cross directly into the one artifact (the hash) the whole product is built around, with no server-side recomputation gate.
9. **Declared architecture and running architecture have diverged.** `DashboardService` is real code with zero callers; `DashboardController` duplicates its logic instead of using it — the package diagram lies about what actually executes.
10. **No observability seams.** No request/correlation IDs, no structured logging, no way to trace one user's request across controller → service → repository → filesystem in a running system.
11. **Fragile inter-layer error contract.** `AuthController` decides HTTP status by string-matching a `Map<String,String>` message (`"user not found".equals(...)`) produced by `AuthService` — the API's error semantics are implicitly encoded in string literals shared across two classes instead of a typed contract.
12. **No frontend data-access layer, despite one being scaffolded.** `services/api.ts`, `services/auth.ts`, `services/client.ts` exist and are empty; real pages call `fetch()` directly against a hardcoded host, so environment promotion (dev → staging → prod) requires editing source, not configuration.
13. **No health/readiness surface.** Nothing in the architecture lets an orchestrator, load balancer, or even a `docker-compose` healthcheck know whether the app is actually up and able to serve traffic.
14. **Fragmented client-side identity state.** The frontend has no single source of truth for "who is the current user" — `AuthContext` tracks only a boolean, while `userId`, `email`, and `name` are independently read from `localStorage` in different pages, meaning the same conceptual piece of state is represented four separate ways.

---

**End of Phase 1.** No code has been changed. Waiting for approval before Phase 2 (full production audit against OWASP Top 10, REST/DTO design, validation, error handling, logging, transactions, concurrency, DB design/indexes, money handling, config/secrets, testing, SOLID, performance, frontend architecture, dead code, and technical debt).
