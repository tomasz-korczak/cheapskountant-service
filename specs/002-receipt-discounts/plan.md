# Implementation Plan: Receipt Discounts

**Branch**: `002-receipt-discounts` | **Date**: 2026-10-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-receipt-discounts/spec.md`

## Summary

A discount printed directly under a line item is stored on that item. It has a description and a negative total, and it has no quantity, unit, unit price, or tax category. The printed heading "OPUSTY ŁĄCZNIE" and its negative amount become one optional `discountSummary` on the receipt. Both are omitted when the paper does not show them. A present discount or summary with a blank description or an amount that is not below zero is `incomplete`, and nothing is stored.

This extends the existing transcription and storage flow. The model response schema, the receipt draft, the HTTP body, and the MariaDB schema all gain the same two facts. No new endpoint is added.

## Technical Context

**Language/Version**: Java 23, JDK home `C:\tools\jdk-23.0.2`, Maven compiler release 23

**Primary Dependencies**: Spring Boot 4.1.1, Spring AI 2.0.1 with `spring-ai-starter-model-openai` against OpenRouter, Spring Data JPA, Liquibase

**Storage**: MariaDB. A new Liquibase SQL changeset adds nullable discount columns on `receipt_item` and a `discount_summary` table. Hibernate `ddl-auto` stays `validate`.

**Testing**: JUnit 5 and Spring Boot Test. Completeness rules are unit-tested. Persistence of a discounted receipt is tested against MariaDB via Testcontainers. Controller tests keep covering `POST /api/receipt` without calling OpenRouter.

**Target Platform**: Existing Docker image of this service. No frontend.

**Project Type**: web-service (REST API only)

**Performance Goals**: Unchanged. Transcription still returns within 60 seconds. Storage does not call the model.

**Constraints**: Photos are not retained. No new failure reason. Amounts are not reconciled. One discount per item. The non-fiscal "Udzielono łącznie opustów" / "Promocje" block stays in `unparsedLines`.

**Scale/Scope**: Same two endpoints and one shared ledger. The change is the receipt shape only.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Result |
|------|--------|
| I. REST API only. JSON HTTP endpoints. No frontend. | Pass. `POST /api/transcription` and `POST /api/receipt` keep their paths. The success body gains optional discount fields. |
| II. Explicit API contracts. Correct HTTP usage. One error format. | Pass. The receipt body change is in `contracts/receipt-discounts.openapi.yaml` and in `receipt-schema.json`. Failures still use the existing reason plus explanation. |
| III. Tested behavior. Endpoint and business-rule tests. | Pass. Completeness, mapping, persistence, and the receipt HTTP body are covered. A live OpenRouter call is not part of the default test run. |
| IV. Secure by default. Validate input. Authenticate. No secrets in source or logs. | Pass. Both endpoints still require `API_KEY`. Discount amounts are validated before insert. |
| Configuration is external. | Pass. No new secret or setting. |
| Database changes are Liquibase SQL changesets. | Pass. New formatted SQL changeset only. The applied `001` changeset is not edited. |
| Health endpoint. | Pass. Unchanged. |

Post-design re-check: the data model adds optional discount facts and does not add a frontend, a second error shape, anonymous access, or stored photos. Gates still pass.

## Project Structure

### Documentation (this feature)

```text
specs/002-receipt-discounts/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── receipt-discounts.openapi.yaml
└── tasks.md
```

### Source Code (repository root)

```text
.external-resources/receipt-schema.json
src/main/resources/schemas/receipt-schema.json
src/main/resources/prompts/receipt-transcription-system.st
src/main/resources/db/changelog/db.changelog-master.sql
src/main/java/pl/tomaszko/cheapskountant/receipt/
├── transcription/ReceiptDraft.java
├── api/StoredReceiptResponse.java
├── application/ReceiptCompletenessChecker.java
└── persistence/
    ├── ReceiptItemEntity.java
    ├── ReceiptEntity.java
    ├── DiscountSummaryEntity.java
    └── ReceiptMapper.java
src/test/java/pl/tomaszko/cheapskountant/receipt/
├── ReceiptFixtures.java
├── application/CreateReceiptFailureTest.java
├── application/CreateReceiptServiceTest.java
├── persistence/ReceiptPersistenceTest.java
└── api/CreateReceiptControllerTest.java
```

**Structure Decision**: Keep the single Maven module. Discount data is part of the existing receipt aggregate. The model is constrained by the classpath copy of `receipt-schema.json`. `.external-resources/receipt-schema.json` stays the source copy and is updated to the same document.

## Complexity Tracking

No constitution gates fail. Item discount columns live on `receipt_item` because a line has at most one discount. The receipt total is its own `discount_summary` row because it is a separate object with its own lifecycle from the line items.
