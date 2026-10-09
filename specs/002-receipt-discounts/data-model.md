# Data Model: Receipt Discounts

Extends [the receipt model](../001-create-receipt/data-model.md). Unlisted tables and columns are unchanged.

The model response and the HTTP success body follow `.external-resources/receipt-schema.json`. `discount` and `discountSummary` are optional. Database ids and timestamps are not returned.

Money columns stay `DECIMAL(14,2)`. A discount total is stored as a negative amount. The API writes it as a string with two fractional digits, such as `-8.69`.

## Item discount

Optional object on a line item in the schema and in `ReceiptDraft.Item`. At most one. Omitted when the item has none.

| Field | Required when the object is present | Notes |
|-------|--------------------------------------|-------|
| description | yes | Printed label, such as `OPUST`. Non-blank. |
| total | yes | Money strictly less than zero. No quantity, unit, unit price, or tax category. |

The item's own `total` remains the printed line total.

### receipt_item columns

Added by changeset `002-receipt-discounts`. Both null, or both set.

| Column | Required | Notes |
|--------|----------|-------|
| discount_description | no | `VARCHAR(1024)`. Set only with `discount_total`. |
| discount_total | no | `DECIMAL(14,2)`. Strictly less than zero when set. |

A check constraint requires the pair to be both null, or a non-blank description with a negative total.

## Discount summary

Optional object on the receipt, schema name `discountSummary`. At most one. Omitted when "OPUSTY ŁĄCZNIE" is not printed.

| Field | Required when the object is present | Notes |
|-------|--------------------------------------|-------|
| description | yes | Printed heading, such as `OPUSTY ŁĄCZNIE`. Non-blank. |
| total | yes | Money strictly less than zero. |

### discount_summary

Zero or one row per receipt.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| receipt_id | yes | Unique foreign key to `receipt`, `ON DELETE CASCADE` |
| description | yes | |
| total | yes | Strictly less than zero |
| created_at, updated_at | yes | Same value on insert |

## Validation rules

- A null item discount and a null discount summary are valid.
- A present discount or summary with a blank description, a missing amount, a non-money amount, zero, or a positive amount is `incomplete`. Nothing is stored.
- Item discounts are not required to sum to the discount summary.
- "Udzielono łącznie opustów" and "Promocje" stay in `unparsed_line` when the model cannot place them elsewhere.
- A packaging deposit stays a `receipt_item` row.

## Domain types

- `ReceiptDraft.Discount`: `description`, `total`.
- `ReceiptDraft.Item` gains optional `discount`.
- `ReceiptDraft` and `StoredReceiptResponse` gain optional `discountSummary`.
