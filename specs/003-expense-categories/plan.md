# Implementation Plan: Expense Categories

**Branch**: `003-expense-categories` | **Date**: 2026-10-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-expense-categories/spec.md`, plus the planning request: Liquibase tables for the category dictionary and expenses, a required category on each receipt item, `category` on the receipt item in `receipt-schema.json` without listing allowed names, `GET` and `POST /api/expense`, `GET /api/expenses` for the household category names, and a transcription prompt that sets every item category to Unknown.

## Summary

Authorized callers read the household category names, store a list of household expenses, and read those expenses back only as totals for an inclusive payment-date range. Each expense keeps an amount of at most six digits before the decimal separator and two after it, a calendar date, a household category name, a three-character currency, and an optional description of at most 100 characters. The caller who stores an expense chooses its category from those names. The summary adds amounts that share a category name and a currency, and returns the category name, that currency, and the summed amount. Different currencies stay in separate totals and are not converted. One storage request holds at most 500 expenses. Further requests may store more on the same payment date.

Receipt line items gain a required `category`. The receipt JSON schema requires that string and does not list allowed names. The database stores it as a required foreign key to the category dictionary. The dictionary is seeded with the 34 household names plus Unknown. A receipt photo has no category, so the transcription prompt tells the model to set Unknown, and the service then sets Unknown on every parsed item. Expense storage rejects a blank category, Unknown, and any other name that is not a household name. `GET /api/expenses` returns the 34 household names so the caller can choose one.

## Technical Context

**Language/Version**: Java 23, JDK home `C:\tools\jdk-23.0.2`, Maven compiler release 23

**Primary Dependencies**: Spring Boot 4.1.1, Spring AI 2.0.1 with `spring-ai-starter-model-openai` against OpenRouter, Spring Data JPA, Liquibase

**Storage**: MariaDB. A new Liquibase SQL changeset `003-expense-categories` creates `expense_category` and `expense`, seeds the category names, and adds a required `category_id` on `receipt_item`. Hibernate `ddl-auto` stays `validate`. Changesets `001` and `002` are not edited.

**Testing**: JUnit 5 and Spring Boot Test. Expense validation and the summary grouping are unit-tested. Persistence of expenses and of a receipt item category is tested against MariaDB via Testcontainers. Controller tests cover `GET` and `POST /api/expense` and the new receipt item field without calling OpenRouter.

**Target Platform**: Existing Docker image of this service. No frontend.

**Project Type**: web-service (REST API only)

**Performance Goals**: Unchanged. Transcription still returns within 60 seconds. Expense storage and the summary do not call the model. The summary groups one date range by category name and currency.

**Constraints**: One shared ledger. `POST /api/expense` accepts 1 to 500 expenses and keeps the whole list or none of it. That limit is per request, not per payment date. Expense validation failures are HTTP 400 with the existing `{reason, explanation}` body. Receipt category failures stay on the existing receipt reasons. Photos are not retained. Expense amounts are `DECIMAL(8,2)`. Currencies are not converted. `GET /api/expenses` returns names only.

**Scale/Scope**: `GET /api/expenses`, `GET /api/expense`, and `POST /api/expense`. The receipt body gains `items[].category`. The category dictionary is fixed at 35 seeded names. The name list returns the 34 household names.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Result |
|------|--------|
| I. REST API only. JSON HTTP endpoints. No frontend. | Pass. New behavior is `GET /api/expenses`, `GET /api/expense`, and `POST /api/expense`. Receipt transcription and storage keep their paths and gain `items[].category`. |
| II. Explicit API contracts. Correct HTTP usage. One error format. | Pass. `contracts/expense-categories.openapi.yaml` defines the three expense operations and the receipt item field. Failures reuse `{reason, explanation}`. Expense validation is HTTP 400 with reason `invalid submission`. Unauthorized stays HTTP 401. A failed save is HTTP 500 with reason `storage failed`. |
| III. Tested behavior. Endpoint and business-rule tests. | Pass. Store, summary, receipt category, schema, and prompt behavior are covered. A live OpenRouter call is not part of the default test run. |
| IV. Secure by default. Validate input. Authenticate. No secrets in source or logs. | Pass. `GET /api/expenses`, `GET /api/expense`, and `POST /api/expense` require `API_KEY`. The current filter only checks `POST /api/receipt` and `POST /api/transcription`, so it must be extended. Amount, date, currency, description, list size, and category name are validated before insert. |
| Configuration is external. | Pass. No new secret or setting. |
| Database changes are Liquibase SQL changesets. | Pass. New formatted SQL changeset only. |
| Health endpoint. | Pass. Unchanged. |

Post-design re-check: the contract uses the existing error body, `GET /api/expenses`, `GET /api/expense`, and `POST /api/expense` require the API key, and the schema change is SQL in a new changeset. No frontend, anonymous expense access, or second error shape. Gates still pass.

## Project Structure

### Documentation (this feature)

```text
specs/003-expense-categories/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── expense-categories.openapi.yaml
└── tasks.md
```

### Source Code (repository root)

```text
.external-resources/receipt-schema.json
src/main/resources/schemas/receipt-schema.json
src/main/resources/prompts/receipt-transcription-system.st
src/main/resources/db/changelog/db.changelog-master.sql
src/main/java/pl/tomaszko/cheapskountant/config/SecurityConfig.java
src/main/java/pl/tomaszko/cheapskountant/expense/
├── api/ExpenseController.java
├── application/StoreExpensesService.java
├── application/ExpenseSummaryService.java
└── persistence/
    ├── ExpenseCategoryEntity.java
    ├── ExpenseCategoryRepository.java
    ├── ExpenseEntity.java
    └── ExpenseRepository.java
src/main/java/pl/tomaszko/cheapskountant/receipt/
├── transcription/ReceiptDraft.java
├── transcription/ReceiptTranscriptionService.java
├── application/ReceiptCompletenessChecker.java
├── persistence/ReceiptItemEntity.java
└── persistence/ReceiptMapper.java
src/test/java/pl/tomaszko/cheapskountant/expense/
src/test/java/pl/tomaszko/cheapskountant/receipt/
```

**Structure Decision**: Keep the single Maven module. Expenses are a new package beside `receipt`. Receipt items reference `ExpenseCategoryEntity`. `ReceiptDraft.Item` is the receipt object used by transcription and by `StoredReceiptResponse`, so adding `category` there covers both HTTP bodies. `.external-resources/receipt-schema.json` and `src/main/resources/schemas/receipt-schema.json` stay identical. The classpath copy is what the model receives.

## Complexity Tracking

No constitution gates fail. Unknown is a seeded category row because a receipt item's category is a required foreign key, while transcription has no household category to assign. Expense storage still rejects that name.
