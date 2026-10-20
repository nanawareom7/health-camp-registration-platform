# Prepared GitHub Issues
## Health Camp Registration and Deployment Platform

> These are **prepared** issue contents for the MVP. They have **not** been created on GitHub yet (no remote is configured). Copy each block into a new GitHub issue once the repository is pushed.

---

## Issue 1: Participant registration and validation

**Title:** feat: participant registration and validation
**Labels:** enhancement, feature

### Description
Implement the participant registration flow (FR-1). A participant should be able to submit a registration for a health camp with required personal and contact details. The system must validate inputs, prevent duplicate registrations, and create the record in `PENDING` status.

This delivers the planned endpoint `POST /api/v1/registrations` (see `docs/api-documentation.md`) and the `Registration` / `Participant` entities (see `docs/data-model.md`).

### Tasks
- Add `Participant` and `Registration` JPA entities.
- Add a `RegistrationService` with create + duplicate check.
- Add `POST /api/v1/registrations` controller with Bean Validation.
- Add unit tests for the service and an integration test for the endpoint (H2).
- Persist registrations with status `PENDING`.

### Acceptance criteria
- [ ] `POST /api/v1/registrations` accepts a valid payload and returns `201` with a `PENDING` registration.
- [ ] Missing/invalid required fields (name, email, phone, campId) return `400` with a clear validation error.
- [ ] A duplicate registration (same participant email + camp) returns `409`.
- [ ] Registering for a non-existent camp returns `404` (or `400`).
- [ ] All new code is covered by JUnit 5 tests (service + web layer).
- [ ] No secrets are committed; configuration uses environment variables.

---

## Issue 2: Registration approval and confirmation

**Title:** feat: registration approval and confirmation
**Labels:** enhancement, feature

### Description
Implement the organizer approval flow (FR-2). An organizer can view a registration and transition it from `PENDING` to `APPROVED` or `REJECTED`. On approval a confirmation is generated (notification is planned and may be logged in the MVP).

This delivers the planned endpoints `GET /api/v1/registrations/{id}` and `PATCH /api/v1/registrations/{id}/status`.

### Tasks
- Add `RegistrationService` methods for status transition with validation of allowed transitions.
- Add `GET /api/v1/registrations/{id}` and `PATCH /api/v1/registrations/{id}/status`.
- Store an optional rejection reason.
- On approval, generate a confirmation reference and (planned) log a notification.
- Add JUnit 5 tests for transitions and error cases.

### Acceptance criteria
- [ ] `GET /api/v1/registrations/{id}` returns the registration or `404`.
- [ ] `PATCH` with `APPROVED` moves a `PENDING` registration to `APPROVED` and returns `200`.
- [ ] `PATCH` with `REJECTED` moves a `PENDING` registration to `REJECTED` and stores an optional reason.
- [ ] Invalid transitions (e.g. approving an already-rejected registration) return `400`.
- [ ] Approving beyond camp capacity is prevented (ties to FR-3.4) with a `409`.
- [ ] Confirmation generation is covered by tests; notification is stubbed/logged if not implemented.

---

## Issue 3: Participant listing and status tracking

**Title:** feat: participant listing and status tracking
**Labels:** enhancement, feature

### Description
Implement participant listing and status tracking (FR-3). Organizers can list participants for a camp, view each registration's status, filter by status, and search by name. The system enforces camp capacity during approval.

This delivers the planned endpoint `GET /api/v1/camps/{id}/participants`.

### Tasks
- Add a query/repository method to list registrations by camp with filters.
- Add `GET /api/v1/camps/{id}/participants?status=&q=&page=&size=`.
- Return paged results with the current status per registration.
- Enforce capacity in the approval path (cross-link with Issue 2).
- Add JUnit 5 tests for filtering, search, and pagination.

### Acceptance criteria
- [ ] `GET /api/v1/camps/{id}/participants` returns a paged list with `200`.
- [ ] The `status` query filters by `PENDING`/`APPROVED`/`REJECTED`/`CONFIRMED`.
- [ ] The `q` query searches participant names (case-insensitive).
- [ ] Pagination (`page`, `size`) is respected and reflected in the response.
- [ ] Approving a registration beyond camp capacity returns `409`.
- [ ] Listing a non-existent camp returns `404`.
- [ ] All new code is covered by JUnit 5 tests.
