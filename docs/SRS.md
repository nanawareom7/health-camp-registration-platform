# Software Requirements Specification (SRS)
## Health Camp Registration and Deployment Platform

> **Status:** Draft for the MVP. The functional requirements below describe **planned** behavior. The current repository contains only the application skeleton; none of these requirements are implemented yet.

## 1. Introduction

### 1.1 Purpose
This document specifies the requirements for the Health Camp Registration and Deployment Platform, a web application that lets participants register for health camps and lets organizers review registrations and track participant status.

### 1.2 Scope
The platform covers participant registration, validation, organizer approval/confirmation, and participant listing with status tracking. The MVP does not include payment processing, medical records, or scheduling.

### 1.3 Definitions and acronyms
- **Participant** — a person registering for a health camp.
- **Organizer** — a staff member who reviews and approves registrations.
- **Health Camp** — a scheduled health-checkup event with a date and capacity.
- **Registration** — a participant's request to join a specific camp.
- **Status** — lifecycle state of a registration: `PENDING`, `APPROVED`, `REJECTED`, `CONFIRMED`.

### 1.4 Overview
The rest of this document describes the overall product, functional and non-functional requirements, and references to the planned data model and API.

## 2. Overall description

### 2.1 Product perspective
A standalone backend service (Spring Boot) exposing a REST API. A front-end is planned and out of scope for this task; it will consume the API.

### 2.2 User classes
- **Participant**: registers for a camp; receives confirmation.
- **Organizer**: reviews registrations; approves/rejects; views lists.
- **Administrator** (planned): manages camps and users.

### 2.3 Operating environment
- Server: JVM 17+; MySQL 8.x.
- Clients: any HTTP client / browser via the planned front-end.

### 2.4 Assumptions and dependencies
- Each camp has a fixed capacity.
- A participant may register once per camp.
- Notifications (email/SMS) are planned and may be stubbed/logged in the MVP.

## 3. Functional requirements

### FR-1: Participant registration and validation
- **FR-1.1** A participant can submit a registration for a camp with required personal and contact details.
- **FR-1.2** The system validates required fields, email format, phone format, and that the camp exists.
- **FR-1.3** The system rejects duplicate registrations for the same participant and camp.
- **FR-1.4** A registration is created with status `PENDING`.

### FR-2: Registration approval and confirmation
- **FR-2.1** An organizer can view a registration's details.
- **FR-2.2** An organizer can approve or reject a `PENDING` registration.
- **FR-2.3** On approval, the status becomes `APPROVED` and a confirmation is generated.
- **FR-2.4** On rejection, the status becomes `REJECTED` with an optional reason.
- **FR-2.5** A confirmation notification is sent to the participant (planned; may be logged in the MVP).

### FR-3: Participant listing and status tracking
- **FR-3.1** An organizer can list participants for a camp.
- **FR-3.2** The list shows each registration's current status.
- **FR-3.3** The list supports filtering by status and searching by name.
- **FR-3.4** The system enforces capacity: no approvals beyond a camp's capacity.

## 4. Non-functional requirements

- **NFR-1 Performance:** List endpoints return within 500 ms for up to 10,000 registrations.
- **NFR-2 Security:** Input validation on all endpoints; no secrets in the repository; database credentials supplied via environment variables.
- **NFR-3 Availability:** Designed for single-instance deployment in the MVP; horizontal scaling is a future goal.
- **NFR-4 Maintainability:** Layered architecture (controller -> service -> repository); unit and integration tests with JUnit 5.
- **NFR-5 Observability:** Structured logging (planned).
- **NFR-6 Portability:** Containerizable; configuration via environment variables.

## 5. References
- `docs/architecture.md` — component and deployment architecture.
- `docs/data-model.md` — planned entities and relationships.
- `docs/api-documentation.md` — planned REST endpoints.
- `docs/deployment.md` — deployment plan.
- `docs/github-issues.md` — prepared MVP issues with acceptance criteria.

## 6. Revision history
| Date | Version | Notes |
| --- | --- | --- |
| 2026-10-20 | 0.1 | Initial draft for the MVP (Task 4). Requirements are planned. |
