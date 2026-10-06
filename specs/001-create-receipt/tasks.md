# Tasks: Create Receipt from Photo

**Input**: Design documents from `/specs/001-create-receipt/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/create-receipt.openapi.yaml, quickstart.md

**Tests**: Included. The constitution requires an automated test for every endpoint outcome and unit tests for business rules. Write each story's tests first and confirm they fail before implementing that story. Default `mvn test` must not call OpenRouter.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: User story label (`[US1]`, `[US2]`, `[US3]`). Setup, foundational, and polish tasks have no story label.
- Every task includes a file path.

## Path Conventions

- Maven module at the repository root
- Java package `pl.tomaszko.cheapskountant`
- Sources under `src/main/java/pl/tomaszko/cheapskountant/`
- Tests under `src/test/java/pl/tomaszko/cheapskountant/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the Spring Boot 4.1.1 / Java 23 project described in plan.md

- [X] T001 Create `pom.xml` with group `pl.tomaszko`, artifact `cheapskountant-service`, parent Spring Boot 4.1.1, Java release 23, Spring Boot repackage final name `cheapskountant-service`, and dependencies `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`, `spring-boot-starter-liquibase`, `spring-boot-starter-actuator`, Spring AI BOM 2.0.1, `spring-ai-starter-model-openai`, `spring-boot-starter-test`, and Testcontainers MariaDB. The runtime image is already defined in `Dockerfile` and expects `target/cheapskountant-service.jar`
- [X] T002 [P] Create `src/main/java/pl/tomaszko/cheapskountant/CheapskountantServiceApplication.java`
- [X] T003 [P] Create `src/main/resources/application.yaml` with `OPENROUTER_API_KEY`, `DB_CONNECTION`, `DB_USERNAME`, `DB_PASSWORD`, `API_KEY`, OpenRouter base URL `https://openrouter.ai/api/v1`, `app.transcription.model` default `google/gemini-2.5-flash`, a 60 second transcription budget, Liquibase changelog `classpath:db/changelog/db.changelog-master.sql`, `spring.jpa.hibernate.ddl-auto=validate`, and log file `logs/cheapskountant-service.log`
- [X] T004 [P] Copy `.external-resources/receipt-schema.json` to `src/main/resources/schemas/receipt-schema.json`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, shared error shape, configuration, and model client used by every story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Create Liquibase formatted SQL changeset `001-create-receipt-tables` in `src/main/resources/db/changelog/db.changelog-master.sql` for every table, key, and timestamp in `specs/001-create-receipt/data-model.md`
- [X] T006 [P] Create JPA entities and `ReceiptRepository` for `receipt`, `seller`, `address`, `receipt_header`, `receipt_item`, `tax_summary`, `receipt_totals`, `payment`, `fiscal_data`, and `unparsed_line` in `src/main/java/pl/tomaszko/cheapskountant/receipt/persistence/`
- [X] T007 [P] Create the failure body with `reason` and `explanation` in `src/main/java/pl/tomaszko/cheapskountant/receipt/api/ReceiptFailure.java` and map it in `src/main/java/pl/tomaszko/cheapskountant/receipt/api/ReceiptErrorHandler.java`
- [X] T008 [P] Create `src/main/java/pl/tomaszko/cheapskountant/config/TranscriptionProperties.java` and the system prompt in `src/main/resources/prompts/receipt-transcription-system.st` using the transcription rules in `specs/001-create-receipt/research.md`
- [X] T009 [P] Create `src/main/java/pl/tomaszko/cheapskountant/config/SecurityConfig.java` so `POST /api/receipt` and `POST /api/transcription` require `Authorization: Bearer` matching `API_KEY` and `GET /actuator/health` is public
- [X] T035 [P] Write a failing-then-passing test that `GET /actuator/health` returns success without `API_KEY` in `src/test/java/pl/tomaszko/cheapskountant/config/HealthEndpointTest.java`
- [X] T010 [P] Create the OpenRouter `ChatClient` bean in `src/main/java/pl/tomaszko/cheapskountant/config/OpenRouterConfig.java`
- [X] T011 [P] Create `src/main/java/pl/tomaszko/cheapskountant/config/ModelCallLoggingAdvisor.java` to log the system prompt, tool definitions, user text, image count, media type, and byte size, plus the response text, and never log API keys, database passwords, or image bytes

**Checkpoint**: Foundation ready. User story implementation can begin.

---

## Phase 3: User Story 1 - Store a transcribed receipt (Priority: P1) 🎯 MVP

**Goal**: An authorized caller submits one to five photos of one Polish fiscal receipt and receives the stored receipt.

**Independent Test**: Submit one clear photo of a complete receipt, and submit two to five ordered photos of one complete receipt. Each response is HTTP 201 and includes `id`, seller, receipt header, at least one line item, at least one tax summary entry, totals, and at least one payment. The database contains those rows and no image bytes.

### Tests for User Story 1

- [X] T012 [P] [US1] Write failing transcription success tests in `src/test/java/pl/tomaszko/cheapskountant/receipt/transcription/ReceiptTranscriptionServiceTest.java`
- [X] T013 [P] [US1] Write failing save-and-return tests in `src/test/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptServiceTest.java`
- [X] T014 [P] [US1] Write failing one-image and two-to-five-image HTTP 201 tests in `src/test/java/pl/tomaszko/cheapskountant/receipt/api/CreateReceiptControllerTest.java`
- [X] T015 [P] [US1] Write a failing MariaDB round-trip test in `src/test/java/pl/tomaszko/cheapskountant/receipt/persistence/ReceiptPersistenceTest.java`

### Implementation for User Story 1

- [X] T016 [P] [US1] Create the schema draft type without a database id in `src/main/java/pl/tomaszko/cheapskountant/receipt/transcription/ReceiptDraft.java`
- [X] T017 [P] [US1] Create the stored response type with `id` in `src/main/java/pl/tomaszko/cheapskountant/receipt/api/StoredReceiptResponse.java` matching `specs/001-create-receipt/contracts/create-receipt.openapi.yaml`
- [X] T018 [US1] Implement `src/main/java/pl/tomaszko/cheapskountant/receipt/transcription/ReceiptTranscriptionService.java` to send ordered images through `ChatClient`, use `src/main/resources/schemas/receipt-schema.json` with provider structured output, and default a missing media type to `image/jpeg`
- [X] T019 [US1] Implement `src/main/java/pl/tomaszko/cheapskountant/receipt/persistence/ReceiptMapper.java` to map a valid draft to the child rows in `data-model.md`, including joined file names and no image bytes
- [X] T020 [US1] Implement the successful transaction in `src/main/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptService.java`
- [X] T021 [US1] Implement `POST /api/receipt` to persist a JSON `StoredReceiptResponse` in `src/main/java/pl/tomaszko/cheapskountant/receipt/api/ReceiptController.java`. Image upload moved to `POST /api/transcription` in Phase 6.

**Checkpoint**: User Story 1 is functional on its own for an authorized caller and valid photos.

---

## Phase 4: User Story 2 - Reject a photo that cannot become a receipt (Priority: P1)

**Goal**: Unreadable input, an incomplete receipt, a transcription outage, and a failed save each return one fixed reason and leave nothing stored.

**Independent Test**: Submit an unreadable image, a receipt with no tax summary entry or a malformed required value, a provider failure, a timeout past 60 seconds, and a database failure after a valid transcription. The reasons are `unreadable` (422), `incomplete` (422), `transcription unavailable` (503 or 504), and `storage failed` (500). No receipt row remains.

### Tests for User Story 2

- [X] T022 [P] [US2] Write failing reason-mapping tests, including an invalid line item and a payment method outside cash, card, transfer, voucher, mobile, and other, in `src/test/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptFailureTest.java`
- [X] T023 [P] [US2] Write failing HTTP tests for 422, 503, 504, and 500 in `src/test/java/pl/tomaszko/cheapskountant/receipt/api/ReceiptFailureControllerTest.java`

### Implementation for User Story 2

- [X] T024 [US2] Classify blank, unreadable, multi-receipt, non-object, incomplete, and provider or timeout failures in `src/main/java/pl/tomaszko/cheapskountant/receipt/transcription/ReceiptTranscriptionService.java`
- [X] T025 [US2] Before insert, reject a draft that lacks a complete line item (description, quantity greater than zero, unit price, line total, tax category), a complete tax summary entry, or a payment whose method is cash, card, transfer, voucher, mobile, or other; also reject a tax id that is not 10 digits, money that is not a two-fraction amount, a currency that is not three letters, a present BDO that is not 9 digits, or a present address missing street, postal code, city, or country code, in `src/main/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptService.java`
- [X] T026 [US2] Roll back and return `storage failed` when save fails in `src/main/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptService.java`
- [X] T027 [US2] Map `unreadable`, `incomplete`, `transcription unavailable`, and `storage failed` to HTTP 422, 503, 504, and 500 in `src/main/java/pl/tomaszko/cheapskountant/receipt/api/ReceiptErrorHandler.java`

**Checkpoint**: User Stories 1 and 2 both work. A failed transcription or save never leaves a receipt.

---

## Phase 5: User Story 3 - Reject an invalid or unauthorized submission (Priority: P2)

**Goal**: Invalid files and unauthorized callers are refused before transcription, and nothing is stored.

**Independent Test**: Submit no image, an empty image, a declared type other than JPEG, PNG, or WebP, an image over 10 MB, or six images. Each returns `invalid submission` and does not call the model. An image with no media type is accepted as JPEG. A missing or wrong bearer token returns `not authorized` and does not call the model.

### Tests for User Story 3

- [X] T028 [P] [US3] Write failing submission-limit tests in `src/test/java/pl/tomaszko/cheapskountant/receipt/application/ReceiptSubmissionValidatorTest.java`
- [X] T029 [P] [US3] Write failing unauthorized HTTP tests in `src/test/java/pl/tomaszko/cheapskountant/receipt/api/UnauthorizedReceiptTest.java`

### Implementation for User Story 3

- [X] T030 [US3] Implement the one-to-five image, 10 MB, declared-type, and missing-type rules in `src/main/java/pl/tomaszko/cheapskountant/receipt/application/ReceiptSubmissionValidator.java`
- [X] T031 [US3] Reject invalid submissions before transcription in `src/main/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptService.java`
- [X] T032 [US3] Return `not authorized` from `src/main/java/pl/tomaszko/cheapskountant/config/SecurityConfig.java` without calling `ReceiptTranscriptionService`

**Checkpoint**: All three stories work. Invalid and unauthorized requests never reach the model.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Logging safety and the quickstart validation

- [X] T033 [P] Write failing-then-passing advisor tests in `src/test/java/pl/tomaszko/cheapskountant/config/ModelCallLoggingAdvisorTest.java` that require prompt and response logging and forbid API keys, database passwords, and image bytes
- [X] T034 Run `mvn test` and the manual checks in `specs/001-create-receipt/quickstart.md`
- [X] T036 Package `target/cheapskountant-service.jar` and build the image from `Dockerfile` using `eclipse-temurin:23.0.2_7-jre`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies. Start immediately.
- **Foundational (Phase 2)**: Depends on Setup. Blocks every user story.
- **User Story 1 (Phase 3)**: Depends on Foundational. No dependency on later stories.
- **User Story 2 (Phase 4)**: Depends on Foundational and on the transcription, application, and error types created in User Story 1.
- **User Story 3 (Phase 5)**: Depends on Foundational and on `CreateReceiptService` from User Story 1. Implement it after User Story 2 because both edit that service.
- **Polish (Phase 6)**: Depends on the stories you choose to deliver.

### User Story Dependencies

- **User Story 1 (P1)**: First story. Delivers the stored receipt.
- **User Story 2 (P1)**: Extends User Story 1 failure handling. Its tests use a fake model and a failing repository, so they do not need a live OpenRouter call.
- **User Story 3 (P2)**: Adds the pre-transcription gate and the unauthorized response. It does not change the success or transcription-failure behavior.

### Within Each User Story

- Tests are written and fail before that story's implementation.
- Draft and response types come before the transcription service.
- The transcription service and mapper come before the application service.
- The application service comes before the controller.
- A story is complete before the next story edits the same classes.

### Parallel Opportunities

- T002, T003, and T004 can run in parallel after T001 starts.
- T006 through T011 can run in parallel with each other. T005 can run beside them. T035 runs after T009 and can run beside the other foundational tasks.
- T012 through T015 can run in parallel.
- T016 and T017 can run in parallel.
- T022 and T023 can run in parallel.
- T028 and T029 can run in parallel.
- T033 can run beside other polish work. T034 and T036 wait until the selected stories are implemented. T036 also waits for `target/cheapskountant-service.jar`.

---

## Parallel Example: User Story 1

```text
T012 ReceiptTranscriptionServiceTest.java
T013 CreateReceiptServiceTest.java
T014 CreateReceiptControllerTest.java
T015 ReceiptPersistenceTest.java
```

After those tests exist and fail:

```text
T016 ReceiptDraft.java
T017 StoredReceiptResponse.java
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Stop and validate the independent test for User Story 1.

User Story 1 alone transcribes a receipt and stores nothing. Storage, failed photos, outages, and unauthorized callers are Stories 2, 3, and 4. Phase 6 is the current endpoint split.

### Incremental Delivery

1. Setup and Foundational make the process, database, and model client ready.
2. User Story 1 transcribes one receipt from one to five photos.
3. User Story 2 stores that structured receipt and rolls back a failed save.
4. User Story 3 stops unreadable photos and transcription outages from returning a receipt.
5. User Story 4 stops invalid files and unauthorized callers before the model is called.
6. Phase 6 keeps transcription and storage on separate endpoints. Polish confirms the log file is safe and `mvn test` passes.

### Parallel Team Strategy

One developer should own `CreateReceiptService.java` and `ReceiptController.java` through Stories 1, 2, and 3, in that order. Tests in separate files can be written ahead of that owner. Entity, prompt, security, and logging files in Phase 2 can be split across people.

---

## Phase 6: Split transcription from storage

**Purpose**: `POST /api/transcription` returns a structured receipt from images and stores nothing. `POST /api/receipt` accepts that object as JSON and only persists it.

- [X] T037 Record the split in `specs/001-create-receipt/spec.md`, `plan.md`, `research.md`, `data-model.md`, `quickstart.md`, and `contracts/create-receipt.openapi.yaml`
- [X] T038 Add `POST /api/transcription` in `src/main/java/pl/tomaszko/cheapskountant/receipt/api/TranscriptionController.java` and `src/main/java/pl/tomaszko/cheapskountant/receipt/application/TranscribeReceiptService.java`
- [X] T039 Change `POST /api/receipt` to accept and persist `StoredReceiptResponse` only in `src/main/java/pl/tomaszko/cheapskountant/receipt/api/ReceiptController.java` and `src/main/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptService.java`
- [X] T040 Require `API_KEY` for both endpoints in `src/main/java/pl/tomaszko/cheapskountant/config/SecurityConfig.java`

---

## Notes

- `[P]` tasks use different files and do not depend on an unfinished task.
- User Story 2 and User Story 3 both modify `CreateReceiptService.java`, so they are sequential.
- Do not store image bytes and do not log them.
- Money values are `DECIMAL(14,2)` in MariaDB and two-fraction strings in the API.
- `created_at` and `updated_at` are set to the same value on insert.
