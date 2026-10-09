# Data Model: Create Receipt from Photo

Source of the field set: `.external-resources/receipt-schema.json`. Item discounts and the receipt discount summary are added in `specs/002-receipt-discounts/data-model.md`.

The model response and the HTTP success body follow that schema. The HTTP body adds `id`. Database ids, foreign keys, `created_at`, and `updated_at` are not sent to the model and are not returned except for the root `id`.

Photos are not stored. There is no user or caller table.

All primary keys are `BIGINT` autoincrement. All foreign keys are `ON DELETE CASCADE`. Every table has:

- `created_at DATETIME(3) NOT NULL`
- `updated_at DATETIME(3) NOT NULL`

On insert both timestamps are the same value. This feature has no update operation.

Money columns are `DECIMAL(14,2)`. The API writes them as strings with two fractional digits, matching the schema pattern. Quantity is `DECIMAL(12,3)` and must be greater than zero. Tax rate is `DECIMAL(5,2)` from 0 through 100.

Charset is `utf8mb4`.

## receipt

Root of one stored fiscal receipt.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key. Returned as `id`. |
| document_type | yes | Always `fiscal_receipt`. |
| source_file_name | no | File names joined in upload order by transcription into `source.fileName`, then stored from the receipt body. Omitted when every part is nameless. |
| source_raw_text | no | Model text only. |
| created_at, updated_at | yes | |

## seller

Exactly one row per stored receipt.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| receipt_id | yes | Unique foreign key to `receipt` |
| trade_name | yes | |
| legal_name | no | |
| tax_id | yes | 10 digits |
| bdo_number | no | 9 digits when present |
| created_at, updated_at | yes | |

## address

Zero, one, or two rows per seller. Unique on (`seller_id`, `role`).

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| seller_id | yes | Foreign key to `seller` |
| role | yes | `BUSINESS` or `REGISTERED` |
| street | yes | |
| postal_code | yes | `NN-NNN` |
| city | yes | |
| country_code | yes | Two letters |
| created_at, updated_at | yes | |

## receipt_header

Exactly one row per stored receipt. Holds the schema object named `receipt` so it does not clash with the root table.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| receipt_id | yes | Unique foreign key to `receipt` |
| receipt_number | yes | |
| issued_at | yes | Date and time |
| currency | yes | Three letters |
| order_number | no | |
| created_at, updated_at | yes | |

## receipt_item

At least one row per stored receipt. `line_no` starts at 1 and preserves image and print order.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| receipt_id | yes | Foreign key to `receipt` |
| line_no | yes | Unique with `receipt_id` |
| description | yes | |
| item_type | no | `product`, `service`, `packaging`, or `other` |
| quantity | yes | Greater than zero |
| unit | no | |
| unit_price | yes | |
| line_total | yes | |
| tax_category | yes | |
| created_at, updated_at | yes | |

## tax_summary

At least one row per stored receipt. The spec requires at least one tax summary entry. An empty tax summary is `incomplete` and is not stored, even though the schema file does not set `minItems`.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| receipt_id | yes | Foreign key to `receipt` |
| line_no | yes | Unique with `receipt_id` |
| tax_category | yes | |
| tax_rate | yes | 0 through 100 |
| taxable_sales | yes | |
| tax_amount | yes | |
| created_at, updated_at | yes | |

## receipt_totals

Exactly one row per stored receipt.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| receipt_id | yes | Unique foreign key to `receipt` |
| tax_amount | yes | |
| gross_amount | yes | |
| amount_due | yes | |
| created_at, updated_at | yes | |

## payment

At least one row per stored receipt.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| receipt_id | yes | Foreign key to `receipt` |
| line_no | yes | Unique with `receipt_id` |
| method | yes | `cash`, `card`, `transfer`, `voucher`, `mobile`, or `other` |
| amount | yes | |
| transaction_id | no | |
| created_at, updated_at | yes | |

## fiscal_data

Zero or one row per receipt.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| receipt_id | yes | Unique foreign key to `receipt` |
| cash_register_code | no | |
| cashier_code | no | |
| fiscal_device_number | no | |
| verification_hash | no | |
| raw_identification_line | no | |
| created_at, updated_at | yes | |

## unparsed_line

Zero or more rows.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| receipt_id | yes | Foreign key to `receipt` |
| line_no | yes | Unique with `receipt_id` |
| line_text | yes | |
| created_at, updated_at | yes | |

## Relationships

```text
receipt 1──1 seller 1──0..2 address
receipt 1──1 receipt_header
receipt 1──1 receipt_totals
receipt 1──0..1 fiscal_data
receipt 1──1..n receipt_item
receipt 1──1..n tax_summary
receipt 1──1..n payment
receipt 1──0..n unparsed_line
```

## Validation before insert

Insert runs in one transaction only after all of the following hold:

- `documentType` is `fiscal_receipt`.
- Seller trade name and 10-digit tax id are present. BDO, when present, is 9 digits.
- Header number, issue timestamp, and currency are present.
- At least one line item has description, quantity greater than zero, unit price, line total, and tax category.
- At least one tax summary entry has category, rate, taxable sales, and tax amount.
- Totals include tax, gross amount, and amount due.
- At least one payment has an allowed method and an amount.
- Money values have two fractional digits. Currency is three letters.
- Addresses, when present, have street, postal code, city, and country code.
- Optional objects that are absent produce no child row.
- Line totals, tax, and amount due are not required to match each other.

Transcription applies the same checks to the model output and returns `incomplete` or `unreadable` without inserting a row. Receipt storage applies them to the JSON body. If any required check fails, the reason is `incomplete` and the transaction does not start. If the body is not a fiscal receipt object, the reason is `unreadable`.

## State

A receipt is either stored completely or not stored. There is no draft, edited, or deleted state in this feature.

The same paper receipt may be stored more than once. No uniqueness constraint is placed on seller tax id plus receipt number.

## Changelog

One Liquibase formatted SQL changeset, `001-create-receipt-tables`, creates the tables above. Later features add new changesets. Hibernate does not create or alter tables.
