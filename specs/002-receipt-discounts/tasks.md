# Tasks: Receipt Discounts

**Input**: Design documents from `/specs/002-receipt-discounts/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included because the constitution requires endpoint and business-rule coverage.

**Organization**: Tasks are grouped by user story. Story 1 attaches a discount to an item. Story 2 records the receipt discount total. Story 3 rejects a malformed discount.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

**Purpose**: Extend the model response schema

- [x] T001 Add optional item `discount` and receipt `discountSummary` to `.external-resources/receipt-schema.json` and `src/main/resources/schemas/receipt-schema.json`

---

## Phase 2: Foundational

**Purpose**: Shared types the stories depend on

- [x] T002 Add `ReceiptDraft.Discount`, optional `Item.discount`, and optional `discountSummary` on `ReceiptDraft` and `StoredReceiptResponse` in `src/main/java/pl/tomaszko/cheapskountant/receipt/transcription/ReceiptDraft.java` and `src/main/java/pl/tomaszko/cheapskountant/receipt/api/StoredReceiptResponse.java`
- [x] T003 Add Liquibase changeset `002-receipt-discounts` to `src/main/resources/db/changelog/db.changelog-master.sql`
- [x] T004 State the discount rules in `src/main/resources/prompts/receipt-transcription-system.st`

---

## Phase 3: User Story 1 - Item discount (Priority: P1)

**Goal**: A discount printed under an item is stored on that item and is not a separate line.

**Independent Test**: Store a receipt whose second item has discount "OPUST" / "-8.69". The response keeps that item's own total and does not contain an item named "OPUST".

- [x] T005 [US1] Map item discount columns in `src/main/java/pl/tomaszko/cheapskountant/receipt/persistence/ReceiptItemEntity.java` and `src/main/java/pl/tomaszko/cheapskountant/receipt/persistence/ReceiptMapper.java`
- [x] T006 [US1] Accept a negative item discount and reject a blank or non-negative one in `src/main/java/pl/tomaszko/cheapskountant/receipt/application/ReceiptCompletenessChecker.java`
- [x] T007 [US1] Cover item-discount storage in `src/test/java/pl/tomaszko/cheapskountant/receipt/ReceiptFixtures.java`, `src/test/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptServiceTest.java`, and `src/test/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptFailureTest.java`

---

## Phase 4: User Story 2 - Discount summary (Priority: P1)

**Goal**: "OPUSTY ŁĄCZNIE" is one receipt-level object and round-trips through storage.

**Independent Test**: Store a receipt with `discountSummary` description "OPUSTY ŁĄCZNIE" and total "-8.69". The stored response returns that object, and the database has one `discount_summary` row.

- [x] T008 [US2] Add `src/main/java/pl/tomaszko/cheapskountant/receipt/persistence/DiscountSummaryEntity.java` and map it from `src/main/java/pl/tomaszko/cheapskountant/receipt/persistence/ReceiptEntity.java` and `ReceiptMapper.java`
- [x] T009 [US2] Round-trip the summary in `src/test/java/pl/tomaszko/cheapskountant/receipt/persistence/ReceiptPersistenceTest.java` and accept it on `POST /api/receipt` in `src/test/java/pl/tomaszko/cheapskountant/receipt/api/CreateReceiptControllerTest.java`
- [x] T010 [US2] Assert the transcription schema and prompt include `discountSummary` in `src/test/java/pl/tomaszko/cheapskountant/receipt/transcription/ReceiptTranscriptionServiceTest.java`

---

## Phase 5: User Story 3 - Malformed discount (Priority: P2)

**Goal**: A present discount summary with a blank description or an amount that is not below zero is `incomplete` and is not saved.

**Independent Test**: Submit a discount summary total of "0.00" or "-0.00", and a summary with a blank description. Nothing is saved.

- [x] T011 [US3] Reject a malformed discount summary in `ReceiptCompletenessChecker.java` and `src/test/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptFailureTest.java`

---

## Phase 6: Polish

- [x] T012 Update `specs/001-create-receipt/contracts/create-receipt.openapi.yaml` so the published receipt body includes `discount` and `discountSummary`
- [x] T013 Run `mvn test`

---

## Dependencies

- T002 depends on T001's field names. T005 and T008 depend on T002 and T003. T006 and T011 depend on T002. Story 2 can share the fixture from Story 1.
- Story 3 extends the checker started in Story 1.

## Parallel example

- T005 and T006 touch different files and can proceed together after T002.
- T009 and T010 can proceed together after the mapper exposes `discountSummary`.

## Implementation strategy

Story 1 is the MVP: item discounts survive storage. Story 2 adds the receipt total. Story 3 closes the invalid amounts.

## Format validation

Every task uses a checkbox, an id, a story label only on story phases, and a file path.
