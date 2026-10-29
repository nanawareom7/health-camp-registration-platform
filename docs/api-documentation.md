# API Documentation
## Health Camp Registration and Deployment Platform

> **Status:** Registration, listing, individual status tracking, and approval/rejection are **implemented and verified** (Task 6, `mvn clean verify` — 19 tests pass). The camp-management endpoints in section 6 remain **planned / not implemented**. Base path: `/api/v1`.

## Conventions
- Content type: `application/json`.
- Date/time fields use ISO formats (`yyyy-MM-dd` for dates; ISO-8601 with `Z` offset for timestamps).
- Errors return a consistent body (see section 5).
- The listing, get-by-id, and status-update endpoints are **unprotected organizer/admin endpoints** — **demo-only**, no auth (see README "Security / demo-only").

## Implemented endpoints

| Method | Path | Status | Description |
| --- | --- | --- | --- |
| POST | `/api/v1/registrations` | Implemented | Create a registration (`PENDING`). |
| GET | `/api/v1/registrations[?status=]` | Implemented | List registrations, optionally filtered by status. |
| GET | `/api/v1/registrations/{id}` | Implemented | Get a single registration (status tracking). |
| PATCH | `/api/v1/registrations/{id}/status` | Implemented | Approve or reject a registration. |

## 1. Create registration — `POST /api/v1/registrations`

Request body:
```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "phone": "+919876543210",
  "preferredCampDate": "2026-10-28"
}
```

Field rules:
| Field | Rule |
| --- | --- |
| `fullName` | Required, not blank. |
| `email` | Required, valid email format. |
| `phone` | Required, 7-20 digits; hyphens/spaces allowed; optional leading `+`. |
| `preferredCampDate` | Required, ISO date, must be a future date. |

Responses:
- `201 Created` — registration created with `status: "PENDING"`. A `Location` header is set to `/api/v1/registrations/{id}`.
- `400 Bad Request` — validation error (see section 5).

Response body (201):
```json
{
  "id": 1,
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "phone": "+919876543210",
  "preferredCampDate": "2026-10-28",
  "status": "PENDING",
  "createdAt": "2026-10-20T12:00:00Z"
}
```

## 2. List registrations — `GET /api/v1/registrations`

Query parameters:
| Name | Required | Description |
| --- | --- | --- |
| `status` | no | Filter by `PENDING`, `APPROVED`, or `REJECTED`. Omit to list all. |

Responses:
- `200 OK` — array of registrations (ordered by ascending `id`); empty array when no matches.
- `400 Bad Request` — unknown `status` value.

Response body (200):
```json
[
  {
    "id": 1,
    "fullName": "Jane Doe",
    "email": "jane@example.com",
    "phone": "+919876543210",
    "preferredCampDate": "2026-10-28",
    "status": "PENDING",
    "createdAt": "2026-10-20T12:00:00Z"
  }
]
```

## 3. Individual status tracking — `GET /api/v1/registrations/{id}`

Responses:
- `200 OK` — the registration.
- `404 Not Found` — id does not exist.

The response body is the registration object shown in section 1.

## 4. Approve / reject — `PATCH /api/v1/registrations/{id}/status`

Request body:
```json
{ "status": "APPROVED" }
```

`status` must be `APPROVED` or `REJECTED`. Allowed transitions:

| Current status | Allowed target |
| --- | --- |
| `PENDING` | `APPROVED`, `REJECTED` |
| `APPROVED` | none (terminal) |
| `REJECTED` | none (terminal) |

Responses:
- `200 OK` — updated registration (persisted status).
- `400 Bad Request` — `status` missing/blank, or not `APPROVED`/`REJECTED` (e.g. `PENDING`).
- `404 Not Found` — registration id does not exist.
- `409 Conflict` — transition not allowed from the current state.

## 5. Error response shape (all endpoints)

```json
{
  "timestamp": "2026-10-20T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [ { "field": "email", "message": "Email must be a valid email address" } ]
}
```
- `status`/`error`/`message` are always present.
- `errors` (per-field list) is present for Bean Validation failures (`400`); absent for `404`/`409`.

Status codes:
| Code | Meaning | When |
| --- | --- | --- |
| 200 | OK | Successful GET / PATCH. |
| 201 | Created | Successful POST. |
| 400 | Bad Request | Validation error, malformed body, invalid status value. |
| 404 | Not Found | Registration id does not exist. |
| 409 | Conflict | Invalid status transition. |
| 500 | Server Error | Unexpected failure (not expected in normal use). |

## 6. Planned / not implemented

The following were described in the original plan but are **not implemented** (the implemented model is a simplified `Registration` without a camp entity):

| Method | Path | Status |
| --- | --- | --- |
| GET | `/api/v1/camps` | Planned (no `HealthCamp` entity yet). |
| GET | `/api/v1/camps/{id}` | Planned. |
| POST | `/api/v1/camps` | Planned. |
| GET | `/api/v1/camps/{id}/participants?status=&q=&page=&size=` | Planned (camp-based listing, name search, pagination). |

Related planned items: camp capacity enforcement on approval, duplicate registration prevention (email + camp), `CONFIRMED` state and notifications, and rejection reason storage. See `docs/data-model.md`, `docs/SRS.md`, and `docs/github-issues.md`.
