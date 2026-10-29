# Prepared GitHub Issues (Backlog)
## Health Camp Registration and Deployment Platform

> These are **prepared** issue contents for the MVP. They have **not** been created on GitHub yet (no remote is configured). Each issue below now includes a **Verified completion status** reflecting what was actually implemented and tested (`mvn clean verify` — 19 tests, 0 failures).
>
> Legend: ✅ done · ⚠️ partial / refined · ❌ not done (planned)

---

## Issue 1: Participant registration and validation

**Title:** feat: participant registration and validation
**Labels:** enhancement, feature

### Description
Implement the participant registration flow (FR-1). A participant submits a registration for a health camp with required personal and contact details. The system validates inputs and creates the record in `PENDING` status.

This delivers `POST /api/v1/registrations` (see `docs/api-documentation.md`).

### Verified completion status: Implemented ✅
Verified by `RegistrationControllerTest` (3 tests) + `HealthCampApplicationTests` (context loads).

**Deviation from original plan:** The implemented model is a single `Registration` entity with `fullName`, `email`, `phone`, `preferredCampDate`, `status`, `createdAt` — there is no `HealthCamp` entity, no `campId`, and no duplicate check. These remain planned (see Limitations in README).

### Acceptance criteria
- [x] ✅ `POST /api/v1/registrations` accepts a valid payload and returns `201` with a `PENDING` registration.
- [x] ✅ Missing/invalid required fields (`fullName`, `email`, `phone`, `preferredCampDate`) return `400` with a clear validation error. *(No `campId` in the simplified model.)*
- [ ] ❌ A duplicate registration (same email + camp) returns `409` — **not implemented** (no camp concept; duplicates are currently allowed).
- [ ] ❌ Registering for a non-existent camp returns `404`/`400` — **not applicable** (no camp entity).
- [x] ✅ All new code is covered by JUnit 5 tests (web layer; `RegistrationControllerTest`).
- [x] ✅ No secrets are committed; configuration uses environment variables.

---

## Issue 2: Registration approval and confirmation

**Title:** feat: registration approval and confirmation
**Labels:** enhancement, feature

### Description
Implement the organizer approval flow (FR-2). An organizer can view a registration and transition it from `PENDING` to `APPROVED` or `REJECTED`.

This delivers `GET /api/v1/registrations/{id}` and `PATCH /api/v1/registrations/{id}/status`.

### Verified completion status: Implemented (approval/rejection) ✅ ; confirmation not done ❌
Verified by `RegistrationWorkflowTest` (15 tests, including approve/reject persistence and transition guards).

**Refinement:** Invalid status **transitions** return `409 Conflict` (not `400`) — a conflict with the current resource state is the more appropriate HTTP semantics; `400` is used for malformed/invalid status **values**. See `docs/api-documentation.md` section 4.

### Acceptance criteria
- [x] ✅ `GET /api/v1/registrations/{id}` returns the registration or `404`.
- [x] ✅ `PATCH` with `APPROVED` moves a `PENDING` registration to `APPROVED` and returns `200` (persisted).
- [ ] ⚠️ `PATCH` with `REJECTED` moves a `PENDING` registration to `REJECTED` — rejection works and persists, but **an optional `reason` is not stored** (not implemented).
- [x] ✅ Invalid transitions (e.g. approving an already-rejected/already-approved registration) are prevented — implemented as `409 Conflict` (refined from `400`; see note above).
- [ ] ❌ Approving beyond camp capacity is prevented with `409` — **not implemented** (no camp/capacity).
- [ ] ❌ Confirmation generation / notification is **not implemented** (no `CONFIRMED` state; notifications planned).

---

## Issue 3: Participant listing and status tracking

**Title:** feat: participant listing and status tracking
**Labels:** enhancement, feature

### Description
Implement participant listing and status tracking (FR-3). Organizers can list registrations, filter by status, and view each registration's current status.

### Verified completion status: Listing (global, with status filter) and status tracking implemented ✅ ; camp-based listing / search / pagination not done ❌
Verified by `RegistrationWorkflowTest` (list-all, list-by-status, empty-filter, invalid-filter-400, get-by-id, get-404).

**Deviation from original plan:** Listing is global at `GET /api/v1/registrations[?status=]`, not camp-based at `/api/v1/camps/{id}/participants`, because there is no `HealthCamp` entity. There is no `q` name search and no pagination.

### Acceptance criteria
- [ ] ⚠️ Listing returns a `200` list — **implemented** at `GET /api/v1/registrations[?status=]` (global, not camp-based; no pagination).
- [x] ✅ The `status` query filters by `PENDING`/`APPROVED`/`REJECTED`. *(Filtering by `CONFIRMED` returns `400` — `CONFIRMED` is not implemented.)*
- [ ] ❌ The `q` query searches participant names — **not implemented**.
- [ ] ❌ Pagination (`page`, `size`) is respected — **not implemented** (full list returned).
- [ ] ❌ Approving beyond camp capacity returns `409` — **not implemented** (no camp/capacity).
- [ ] ❌ Listing a non-existent camp returns `404` — **not applicable** (no camp entity).
- [x] ✅ All new code is covered by JUnit 5 tests (`RegistrationWorkflowTest`).

---

## Remaining backlog (new / derived from Task 6)

These items were surfaced as limitations during Task 6 and are tracked for future work:

- **Security:** Add authentication and role-based authorization (organizer/admin) to the listing, get-by-id, and status-update endpoints (currently demo-only/unprotected).
- **Data model:** Introduce the `HealthCamp` entity and `campId`; switch listing to camp-based.
- **Capacity:** Enforce camp capacity during approval (`409` when exceeded).
- **Deduplication:** Prevent duplicate registrations per participant email + camp (`409`).
- **Pagination & search:** Add `page`/`size` and `q` (name) search to listing.
- **Confirmation:** Add the `CONFIRMED` state and notification (email/SMS or logged stub).
- **Rejection reason:** Store an optional `reason` on rejection.
- **Schema migrations:** Adopt Flyway/Liquibase instead of manual DDL + `ddl-auto: none`.
- **CI/CD:** Run `mvn verify` on pushes/PRs; container build and deploy (see `docs/deployment.md`).
