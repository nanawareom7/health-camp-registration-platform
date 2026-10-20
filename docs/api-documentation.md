# API Documentation
## Health Camp Registration and Deployment Platform

> **Status:** Planned. No endpoints are implemented yet. The following describes the intended REST API for the MVP. Base path: `/api/v1`.

## Conventions
- Content type: `application/json`.
- All endpoints below are **planned** and are not yet implemented.
- Errors (planned): `400` validation error, `404` not found, `409` conflict (e.g. duplicate registration), `500` server error.

## 1. Health camps (planned)
| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/v1/camps` | List camps. |
| GET | `/api/v1/camps/{id}` | Get a camp. |
| POST | `/api/v1/camps` | Create a camp (organizer). |

## 2. Participant registration (planned) — FR-1
`POST /api/v1/registrations`

Request body (planned):
```json
{
  "campId": 1,
  "participant": {
    "fullName": "Jane Doe",
    "email": "jane@example.com",
    "phone": "+919876543210",
    "age": 30,
    "gender": "female"
  }
}
```

Responses (planned):
- `201 Created` — registration created with status `PENDING`.
- `400 Bad Request` — validation error.
- `409 Conflict` — duplicate registration.

## 3. Registration approval and confirmation (planned) — FR-2
`GET /api/v1/registrations/{id}` — view a registration (planned).

`PATCH /api/v1/registrations/{id}/status`

Request body (planned):
```json
{ "status": "APPROVED", "reason": null }
```

Responses (planned):
- `200 OK` — updated registration.
- `400 Bad Request` — invalid transition.
- `404 Not Found` — registration does not exist.

## 4. Participant listing and status tracking (planned) — FR-3
`GET /api/v1/camps/{id}/participants?status=PENDING&q=jane&page=0&size=20`

Response (planned):
```json
{
  "content": [
    { "registrationId": 1, "participantName": "Jane Doe", "status": "PENDING" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

## 5. Status codes summary (planned)
| Code | Meaning |
| --- | --- |
| 200 | OK |
| 201 | Created |
| 400 | Validation error |
| 404 | Not found |
| 409 | Conflict |
| 500 | Server error |
