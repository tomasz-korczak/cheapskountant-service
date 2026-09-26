# Implementation Plan: Create Receipt from Photo

**Branch**: `001-create-receipt` | **Date**: 2026-09-26 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-create-receipt/spec.md`

## Summary

An authorized caller submits one to five photos of a single Polish fiscal receipt. The service transcribes them with a multimodal model into one structured receipt, stores that receipt only when the transcription is complete, and returns it. Failures return one fixed reason and a short explanation, and they leave nothing stored.

Technical approach: a Maven Spring Boot 4.1.1 service on JDK 23 (`C:\tools\jdk-23.0.2`), package `pl.tomaszko`, artifact `cheapskountant-service`. Transcription is a Spring AI `ChatClient` call through the OpenAI-compatible starter pointed at OpenRouter, model `google/gemini-2.5-flash`. The model must return JSON matching `.external-resources/receipt-schema.json`. MariaDB holds the ledger. Liquibase SQL changesets run at startup before the application serves requests. The original photos are not stored.

The spec and this plan agree on these rules:

- One request contains one to five images, in order, of one receipt. More than five images is `invalid submission`. More than one receipt in the images is `unreadable`.
- A missing image media type is sent to the model as `image/jpeg`. A declared type other than JPEG, PNG, or WebP is `invalid submission`.
- A stored receipt has at least one tax summary entry. An empty tax summary is `incomplete`.
- A database failure after a successful transcription uses reason `storage failed` and HTTP 500. The transaction rolls back, so nothing remains stored.
- Image bytes and base64 payloads are not written to the log. The log records image count, media type, and size instead, because the spec forbids retaining the photo and the constitution forbids sensitive data in logs.

## Technical Context

**Language/Version**: Java 23, JDK home `C:\tools\jdk-23.0.2`, Maven compiler release 23

**Primary Dependencies**: Spring Boot 4.1.1 (latest stable as of 2026-09-26), Spring AI 2.0.1 (latest stable release built for Spring Boot 4.1), `spring-ai-starter-model-openai` against OpenRouter, Spring Web, Validation, Data JPA, Liquibase, Actuator

**Storage**: MariaDB. JDBC URL, username, and password come from `DB_CONNECTION`, `DB_USERNAME`, and `DB_PASSWORD`. Schema owned by Liquibase formatted SQL. Hibernate `ddl-auto` is `validate`.

**Testing**: JUnit 5 and Spring Boot Test. Controller tests cover success and every failure reason without calling OpenRouter. Transcription rules are unit-tested with a fake chat model. Persistence is tested against MariaDB via Testcontainers.

**Target Platform**: Docker container running Eclipse Temurin JRE 23.0.2 (`eclipse-temurin:23.0.2_7-jre`). The image is built from `Dockerfile` after Maven writes `target/cheapskountant-service.jar`. No frontend.

**Project Type**: web-service (REST API only)

**Performance Goals**: The caller receives the stored receipt or a failure within 60 seconds of submitting one request. This is a single-user expense ledger, not a high-throughput service.

**Constraints**: Photos are not retained. Secrets and image payloads are not logged. One to five images per request, each at most 10 MB. Accepted declared types are JPEG, PNG, and WebP. A missing media type is treated as JPEG. A stored receipt requires at least one tax summary entry. Duplicate receipts are allowed. Amounts are not reconciled arithmetically. No user accounts.

**Scale/Scope**: One create-receipt endpoint, one shared ledger, one transcription provider. Listing and editing receipts are out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Result |
|------|--------|
| I. REST API only. JSON HTTP endpoints. No frontend, views, or static site. | Pass. `POST /api/receipt` plus the operational health endpoint. |
| II. Explicit API contracts. Correct HTTP usage. One error format. Breaking changes versioned. | Pass. Contract in `contracts/create-receipt.openapi.yaml`. |
| III. Tested behavior. Endpoint success and error tests. Unit tests for business rules. Tests pass before merge. | Pass. Quickstart lists the required tests. Real OpenRouter calls are manual, not the default test run. |
| IV. Secure by default. Validate input. Authenticate unless the endpoint is explicitly public. No secrets in source or logs. | Pass. `/api/receipt` requires `API_KEY`. `/actuator/health` is explicitly public. Input is validated before transcription. `OPENROUTER_API_KEY` and `DB_PASSWORD` are environment variables and are excluded from logs. |
| Configuration is external. | Pass. API key, database, model name, and system prompt path are configuration. |
| Database changes are Liquibase SQL changesets. | Pass. Formatted SQL changelog only. No XML changeset. |
| Health endpoint. | Pass. Spring Boot Actuator health. |

Post-design re-check: the data model, HTTP contract, and quickstart do not add a frontend, a second error shape, anonymous access to receipt creation, or stored photos. Gates still pass.

## Project Structure

### Documentation (this feature)

```text
specs/001-create-receipt/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── create-receipt.openapi.yaml
└── tasks.md              # created later by /speckit-tasks
```

### Source Code (repository root)

```text
pom.xml
Dockerfile
.dockerignore
src/main/java/pl/tomaszko/cheapskountant/
├── CheapskountantServiceApplication.java
├── config/
│   ├── OpenRouterConfig.java
│   ├── TranscriptionProperties.java
│   ├── SecurityConfig.java
│   └── ModelCallLoggingAdvisor.java
├── receipt/
│   ├── api/
│   │   ├── ReceiptController.java
│   │   └── ReceiptErrorHandler.java
│   ├── application/
│   │   └── CreateReceiptService.java
│   ├── transcription/
│   │   └── ReceiptTranscriptionService.java
│   └── persistence/
│       ├── ReceiptEntity.java
│       ├── ReceiptRepository.java
│       └── child entities and repositories
src/main/resources/
├── application.yaml
├── prompts/receipt-transcription-system.st
├── schemas/receipt-schema.json
└── db/changelog/db.changelog-master.sql
src/test/java/pl/tomaszko/cheapskountant/
├── receipt/api/
├── receipt/application/
├── receipt/transcription/
└── receipt/persistence/
```

**Structure Decision**: One Maven module at the repository root. The web layer accepts the multipart request and maps errors. `CreateReceiptService` orders authorization outcomes, validation, transcription, and a single database transaction. `ReceiptTranscriptionService` is the only type that talks to Spring AI. Persistence maps the schema tables and does not see HTTP or the model client.

## Complexity Tracking

No constitution gates fail. The spec and plan agree on one to five images, JPEG when the media type is missing, at least one tax summary entry, and `storage failed`. Image bytes stay out of the log, as recorded in [research.md](./research.md).
