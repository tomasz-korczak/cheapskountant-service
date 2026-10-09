# Tasks: Expense Categories

**Input**: Design documents from `/specs/003-expense-categories/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/expense-categories.openapi.yaml, quickstart.md

**Tests**: Included because the constitution requires endpoint and business-rule coverage. Write the tests for a story so they fail before that story's implementation.

**Organization**: Tasks are grouped by user story. Story 1 stores expenses. Story 2 summarizes a date range by category and currency. Story 3 records a category on each receipt item. Story 6 lists the household names. Story 4 refuses a bad expense or summary request. Story 5 refuses a receipt item with no category.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4, US5, US6)
- Setup and foundational tasks have no story label

## Phase 1: Setup

**Purpose**: Require a category on the receipt item definition without publishing the allowed names

- [x] T001 Add required `items[].category` (`type` string, `minLength` 1, no `enum`) to the item `required` array in `.external-resources/receipt-schema.json` and `src/main/resources/schemas/receipt-schema.json`. Keep the two files identical. Do not add a category to seller, payments, tax summary, totals, discounts, the discount summary, or unparsed lines.

---

## Phase 2: Foundational

**Purpose**: Dictionary, expense table, receipt-item foreign key, and API-key protection that every story uses

**CRITICAL**: No user story work begins until this phase is complete

- [x] T002 [P] Add Liquibase changeset `cheapskountant:003-expense-categories` to `src/main/resources/db/changelog/db.changelog-master.sql`. Do not edit changesets `001` or `002`. Use `utf8mb4` / `utf8mb4_unicode_ci`. Create `expense_category` (`id` BIGINT AUTO_INCREMENT PK, `name` VARCHAR(50) NOT NULL UNIQUE, `created_at` and `updated_at` DATETIME(3) NOT NULL) and seed, in this order, the 34 household names from spec FR-002 and then `Unknown`. `Pieniądze, po prostu…` uses one ellipsis (U+2026), not three dots. `Naczynia,kuchnia` has no space after the comma. `Rachunki / podatki` has spaces around the slash. `Przybory toal.` includes the period. Create `expense` (`id` BIGINT AUTO_INCREMENT PK, `amount` DECIMAL(8,2) NOT NULL, `payment_date` DATE NOT NULL, `category_id` BIGINT NOT NULL, `currency` VARCHAR(3) NOT NULL, `description` VARCHAR(100) NULL, `created_at` and `updated_at` DATETIME(3) NOT NULL) with `category_id` referencing `expense_category` ON DELETE RESTRICT and an index on `payment_date`. Add nullable `receipt_item.category_id`, set every existing row to the `Unknown` id, then alter it to NOT NULL with the same ON DELETE RESTRICT foreign key.
- [x] T003 [P] Add `src/main/java/pl/tomaszko/cheapskountant/expense/persistence/ExpenseCategoryEntity.java` and `ExpenseCategoryRepository.java`. Map `name` as unique. Provide an exact-name lookup and a read of every row except `Unknown` in seed order (`id` ascending). No insert, update, or delete methods for callers.
- [x] T004 Add `src/main/java/pl/tomaszko/cheapskountant/expense/persistence/ExpenseEntity.java` and `ExpenseRepository.java`. Map `amount` as DECIMAL(8,2), `payment_date` as a date, `category_id` to `ExpenseCategoryEntity`, `currency` length 3, and nullable `description` length 100. Stamp both timestamps on insert to millisecond precision, the same way `src/main/java/pl/tomaszko/cheapskountant/receipt/persistence/Timestamped.java` does. Add a summary query for `payment_date` from `from` through `to` inclusive that groups by category id, category name, and currency and returns `SUM(amount)`.
- [x] T005 Add required `category` on `ReceiptDraft.Item` in `src/main/java/pl/tomaszko/cheapskountant/receipt/transcription/ReceiptDraft.java` so `StoredReceiptResponse` carries the name. Map `ReceiptItemEntity.category` to `ExpenseCategoryEntity` in `src/main/java/pl/tomaszko/cheapskountant/receipt/persistence/ReceiptItemEntity.java`. In `src/main/java/pl/tomaszko/cheapskountant/receipt/persistence/ReceiptMapper.java`, resolve the submitted name to that association on the way in and write the stored name on the way out. Set every item built in `src/test/java/pl/tomaszko/cheapskountant/receipt/ReceiptFixtures.java` to `Unknown`, and update other `ReceiptDraft.Item` constructors under `src/test/java` so the suite compiles.
- [x] T006 [P] Extend `requiresApiKey` in `src/main/java/pl/tomaszko/cheapskountant/config/SecurityConfig.java` so GET and POST `/api/expense` and GET `/api/expenses` require the bearer API key. Keep the existing body `{"reason":"not authorized","explanation":"A valid API key is required."}`. Leave POST `/api/receipt` and POST `/api/transcription` unchanged.

**Checkpoint**: The dictionary and expense tables exist, receipt items reference a category, and the three expense paths reject a missing API key.

---

## Phase 3: User Story 1 - Store household expenses (Priority: P1)

**Goal**: An authorized caller stores a list of 1 to 500 expenses in one shared ledger. The whole list is kept, or none of it is.

**Independent Test**: Submit one to 500 complete expenses that use household category names, including more than one expense on the same day and in the same category, and a missing description. All of them are retained. A second accepted request can add more expenses on that same payment date.

- [x] T007 [P] [US1] Write failing validation tests in `src/test/java/pl/tomaszko/cheapskountant/expense/application/ExpenseSubmissionValidatorTest.java`. Accept 1 to 500 expenses, zero and negative amounts inside DECIMAL(8,2), a missing or empty description, and currency kept as submitted (`eur` distinct from `EUR`). Reject an empty list, 501 expenses, a missing amount, `paymentDate`, category, or currency, an amount outside -999999.99 to 999999.99 or with more than two fractional digits, a payment date that includes a time or is not a real calendar date (31 February), a currency whose length is not 3, a category that is not an exact household name (wrong case, surrounding spaces, three dots instead of U+2026), the category `Unknown`, a description made only of spaces, and a description of 101 characters. One invalid expense rejects the whole list. A description of exactly 100 characters is valid.
- [x] T008 [P] [US1] Write a failing MariaDB Testcontainers test in `src/test/java/pl/tomaszko/cheapskountant/expense/persistence/ExpensePersistenceTest.java`. Store several expenses that share a payment date and a category, including one with no description, then store a second request on that same date. Read the rows back with the category name, amount scale 2, currency unchanged, and a null description when it was omitted. Three accepted batches on one payment date all remain stored. Database ids and timestamps are not part of the returned expense.
- [x] T009 [P] [US1] Write a failing POST `/api/expense` success test in `src/test/java/pl/tomaszko/cheapskountant/expense/api/ExpenseControllerTest.java`. An authorized JSON array returns HTTP 201 and the same expenses, with `category` still the name. An amount submitted as 12.5 is returned as 12.50. No `id`, `createdAt`, or currency conversion appears. Do not call OpenRouter.
- [x] T010 [US1] Implement `src/main/java/pl/tomaszko/cheapskountant/expense/application/ExpenseSubmissionValidator.java`. The body is a JSON array of 1 to 500 objects with `amount`, `paymentDate`, `category`, and `currency`. `description` is optional. Resolve `category` with an exact match to a household name from `ExpenseCategoryRepository`, and reject `Unknown`. Throw `pl.tomaszko.cheapskountant.receipt.ReceiptFailureException` with reason `invalid submission` so `ReceiptErrorHandler` returns HTTP 400 and `{reason, explanation}`. Do not insert rows. Store a missing or empty description as null.
- [x] T011 [US1] Implement `src/main/java/pl/tomaszko/cheapskountant/expense/application/StoreExpensesService.java`. Validate, then insert the whole list in one transaction. A failure while saving throws reason `storage failed` and leaves none of that list stored. Return the stored expenses with the category name, the submitted currency, and the description only when one was kept. Return each amount with scale 2. Do not record which caller submitted the expense, and do not create or change a receipt.
- [x] T012 [US1] Add POST `/api/expense` to `src/main/java/pl/tomaszko/cheapskountant/expense/api/ExpenseController.java`. Accept the JSON array from `contracts/expense-categories.openapi.yaml` schema `Expense`. Return HTTP 201 and that same array shape. An unreadable body stays reason `invalid submission` through the existing `ReceiptErrorHandler`.

**Checkpoint**: Authorized callers can store valid expense batches. Invalid batches are refused in full.

---

## Phase 4: User Story 2 - Review a date range by category and currency (Priority: P1)

**Goal**: An authorized caller receives one summed amount per category name and currency for an inclusive payment-date range.

**Independent Test**: Store expenses inside and outside a range, with a repeated category in more than one currency. The result has one sum for each category and currency used inside the range, keeps different currencies apart, and includes nothing from outside the range. An empty range is an empty array. The same date for both ends is that one day.

- [x] T013 [P] [US2] Write failing summary tests in `src/test/java/pl/tomaszko/cheapskountant/expense/application/ExpenseSummaryServiceTest.java`. Group by category name and currency for `payment_date` on or between `from` and `to`. Store one expense before `from` and one after `to`, and assert that neither amount appears in the summary. Add amounts only when both match. Keep `eur` and `EUR` as separate totals and do not convert. Return `category`, `currency`, and `amount`. Omit descriptions. Include a category and currency whose amounts cancel to 0.00. Omit a pair with no expenses in the range. Return an empty list when the range has no expenses. Treat equal `from` and `to` as that single day. The sum may exceed 999999.99. Order is not significant.
- [x] T014 [P] [US2] Write a failing GET `/api/expense?from=&to=` test in `src/test/java/pl/tomaszko/cheapskountant/expense/api/ExpenseControllerTest.java`. HTTP 200 returns `[{category, currency, amount}]` only. The same category in `PLN` and `EUR` is two totals. Asking for a summary does not change stored expenses.
- [x] T015 [US2] Implement `src/main/java/pl/tomaszko/cheapskountant/expense/application/ExpenseSummaryService.java` and GET `/api/expense` on `ExpenseController.java`. Require both dates. Parse them as calendar dates with no time. Group by category name and stored currency, and return that currency unchanged. Refuse a missing date, a date that is not a real calendar date, or a first date after the last date with reason `invalid submission`. A read that does not finish is reason `storage failed` and changes nothing. A range with no rows is HTTP 200 and `[]`.

**Checkpoint**: The date-range summary matches stored amounts per category and currency and does not list individual expenses.

---

## Phase 5: User Story 3 - Record an expense category on each receipt item (Priority: P1)

**Goal**: Every receipt line item has a category. Transcription sets it to `Unknown`. Storage keeps a submitted `Unknown` or household name.

**Independent Test**: A transcribed receipt has `Unknown` on every line item and stores nothing. A stored receipt returns each submitted category unchanged when it is `Unknown` or an exact household name. Any other name is `incomplete` and stores nothing. No other receipt part has a category.

- [x] T016 [P] [US3] Extend `src/test/java/pl/tomaszko/cheapskountant/receipt/transcription/ReceiptTranscriptionServiceTest.java` so both schema copies require `items[].category` and do not enumerate names, `src/main/resources/prompts/receipt-transcription-system.st` tells the model to set every item `category` to `Unknown` and not to infer one from the product, and a parsed draft has `Unknown` on every item even when the model JSON used another category. Transcription still does not store a receipt.
- [x] T017 [P] [US3] Extend `src/test/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptServiceTest.java`, `src/test/java/pl/tomaszko/cheapskountant/receipt/persistence/ReceiptPersistenceTest.java`, and `src/test/java/pl/tomaszko/cheapskountant/receipt/api/CreateReceiptControllerTest.java` so a stored item returns the submitted category for both `Unknown` and a household name such as `Jedzenie`. A name that is not an exact seeded match is reason `incomplete`, HTTP 422, and leaves no receipt stored.
- [x] T018 [US3] In `src/main/java/pl/tomaszko/cheapskountant/receipt/application/ReceiptCompletenessChecker.java`, require a non-blank item `category` that exactly matches `Unknown` or a household name, including case, surrounding spaces, and the single ellipsis. Any other name throws reason `incomplete` before save. Do not replace a caller-supplied category with `Unknown`. Do not require a category on any other receipt part.
- [x] T019 [US3] Instruct the model in `src/main/resources/prompts/receipt-transcription-system.st` to set every item `category` to the string `Unknown`. In `src/main/java/pl/tomaszko/cheapskountant/receipt/transcription/ReceiptTranscriptionService.java`, after the model JSON is parsed and before the completeness check, set `category` to `Unknown` on every item. Do not refuse the photo only because no category is printed.

**Checkpoint**: Transcription fills `Unknown`. Storage round-trips an exact seeded category and refuses any other name.

---

## Phase 6: User Story 6 - Read the household category names (Priority: P1)

**Goal**: An authorized caller reads the 34 household names, in listed order, and does not see `Unknown`.

**Independent Test**: GET `/api/expenses` returns exactly the FR-002 names in that order, each as `{name}`. `Unknown` is absent. A caller without the API key receives `not authorized`.

- [x] T020 [P] [US6] Write a failing test in `src/test/java/pl/tomaszko/cheapskountant/expense/api/ExpenseCategoryControllerTest.java`. Authorized GET `/api/expenses` returns HTTP 200 and 34 objects. The first `name` is `Jedzenie`, the last household name is `Fermentacja alkoholowa`, and `Unknown` is absent. The list does not change after expenses are stored. A missing API key returns the same `not authorized` reason and explanation.
- [x] T021 [US6] Add GET `/api/expenses` to `src/main/java/pl/tomaszko/cheapskountant/expense/api/ExpenseController.java`. Read household names from `ExpenseCategoryRepository` in seed order and return `[{name}]` only. Do not include `Unknown` or database ids.

**Checkpoint**: Callers can choose a household name from the list before storing an expense.

---

## Phase 7: User Story 4 - Refuse an expense that cannot be kept (Priority: P2)

**Goal**: One broken expense, a bad summary range, a failed save or read, or a missing API key refuses the request without changing the ledger or revealing whether expenses exist.

**Independent Test**: Submit 501 expenses, an empty or unreadable submission, one expense with a missing or invalid field, the category `Unknown`, a summary without both real dates or with the first date after the last, and an unauthorized call. Each is refused with the matching reason. A refused storage request leaves previous totals unchanged.

- [x] T022 [US4] Cover the storage refusal matrix in `src/test/java/pl/tomaszko/cheapskountant/expense/api/ExpenseControllerTest.java` and `src/test/java/pl/tomaszko/cheapskountant/expense/persistence/ExpensePersistenceTest.java`. HTTP 400 reason `invalid submission` for an empty array, a body that is not an array, 501 expenses, and a list that mixes valid expenses with one invalid expense (blank category, `Unknown`, non-calendar date, space-only description, or a missing required field). Assert none of that request is stored and earlier accepted expenses remain. A save that throws is HTTP 500 reason `storage failed` and retains nothing from that request.
- [x] T023 [US4] Cover summary refusal in `src/test/java/pl/tomaszko/cheapskountant/expense/api/ExpenseControllerTest.java`. Missing `from` or `to`, a value that is not a real calendar date, and `from` after `to` are HTTP 400 reason `invalid submission`. A summary read that throws is HTTP 500 reason `storage failed`, and stored expenses stay unchanged.
- [x] T024 [P] [US4] Add `src/test/java/pl/tomaszko/cheapskountant/expense/api/UnauthorizedExpenseTest.java`. POST `/api/expense`, GET `/api/expense`, and GET `/api/expenses` without a valid bearer key all return HTTP 401 with reason `not authorized` and explanation `A valid API key is required.`, whether or not expenses exist. Nothing is stored.

**Checkpoint**: Malformed, unauthorized, and failed expense calls leave the ledger unchanged.

---

## Phase 8: User Story 5 - Refuse a receipt item without a category (Priority: P2)

**Goal**: A missing or blank line-item category refuses the receipt. Transcription still fills `Unknown` because the photo has no category.

**Independent Test**: Store an otherwise complete receipt whose line item category is missing or blank. The reason is `incomplete` and no receipt is stored. A transcribed receipt still shows `Unknown` on every item. A non-blank `Unknown` or household name is not refused by this rule alone.

- [x] T025 [US5] Reject a missing or blank item `category` in `src/main/java/pl/tomaszko/cheapskountant/receipt/application/ReceiptCompletenessChecker.java` with reason `incomplete` and a short explanation, and cover it in `src/test/java/pl/tomaszko/cheapskountant/receipt/application/CreateReceiptFailureTest.java`. Assert no receipt is stored. Assert a complete receipt whose every item category is `Unknown` or a household name is not refused for the category rule alone.

**Checkpoint**: A receipt item cannot be stored without a category, and transcription still supplies `Unknown`.

---

## Phase 9: Polish

**Purpose**: Confirm the quickstart automated checks

- [x] T026 Run `mvn test` from the repository root with `JAVA_HOME` set to `C:\tools\jdk-23.0.2`. Docker must be available for the MariaDB Testcontainers tests. Confirm the expectations in `specs/003-expense-categories/quickstart.md`: category-name list, atomic expense storage, date-range summary, receipt item category, transcription `Unknown`, both schema copies, and unauthorized expense reads.

---

## Dependencies

- Phase 2 blocks every story. T004 and T005 depend on T003. T002, T003, and T006 can start together.
- US1 depends on the expense table, category lookup, and API-key filter. It does not depend on the summary or the receipt category behavior.
- US2 depends on US1's stored rows for an end-to-end check. Its grouping tests can use the repository directly after Phase 2.
- US3 depends on T001, T005, and the seeded dictionary. It does not create expenses.
- US6 depends on T003 and T006. It does not depend on stored expenses.
- US4 extends the store and summary paths from US1 and US2. It does not introduce a second error body.
- US5 extends the receipt check from US3.

## Parallel example

After Phase 2:

- US1 validation tests (T007), persistence tests (T008), and the controller success test (T009) are separate files.
- US6 (T020, T021) can proceed beside US1 because it only reads category names.
- US3 schema and transcription tests (T016) can proceed beside the expense tests.
- US4's unauthorized test (T024) can be written beside the other refusal tests.

## Implementation strategy

Story 1 is the MVP: one shared ledger accepts a valid batch and keeps all of it or none of it. Story 6 is the next increment so a caller can read the names that Story 1 requires. Story 2 adds the date-range totals, one per category and currency. Story 3 adds the receipt item category and the transcription placeholder. Stories 4 and 5 close the refusal cases for expenses and receipt items.

Storing an expense must not create a receipt. Storing or transcribing a receipt must not create an expense.

## Format validation

Every task uses a checkbox, an id, a story label only on story phases, and a file path.
