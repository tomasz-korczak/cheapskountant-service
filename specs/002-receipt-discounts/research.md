# Research: Receipt Discounts

## Item discount shape

**Decision**: One optional `discount` object on the line item, with `description` and `total` only. `total` is a money string strictly less than zero. The object is omitted when the item has no discount. The item's own `total` stays the printed line total.

**Rationale**: On the sample receipt, "OPUST" is printed under "ChipsyLay soveB110g" with only "-8,69". It has no quantity, unit price, or tax category. Nesting it on that item preserves which product was reduced. A list was rejected because the spec allows one discount per item.

**Alternatives considered**: A sibling line item with a negative total, which is what the current model response does and what this feature replaces. A child array of discounts, rejected because a second discount line under the same item stays unparsed.

## Discount summary shape

**Decision**: One optional `discountSummary` object on the receipt, same fields as an item discount: `description` and negative `total`. For the sample, description is "OPUSTY ŁĄCZNIE" and total is "-8.69". It is omitted when that heading is absent.

**Rationale**: The heading and amount are one receipt-level fact, printed after the items and before the tax lines. Reusing the discount field pair keeps the model schema small. The later non-fiscal block "Udzielono łącznie opustów" / "Promocje" is a different breakdown and stays in `unparsedLines`.

**Alternatives considered**: Putting the summary amount on `totals`. That would drop the printed heading and mix it with tax and amount due. Parsing the non-fiscal promotion breakdown into the same object was rejected because the user asked only for "OPUSTY ŁĄCZNIE".

## Where the values live

**Decision**: `receipt_item.discount_description` and `receipt_item.discount_total` are nullable and both absent or both present. `discount_summary` is a new table, zero or one row per receipt, with `description` and `total`. A new Liquibase changeset `002-receipt-discounts` alters the existing database. Changeset `001` is not modified.

**Rationale**: Hibernate validates the schema at startup, so the columns must exist before the new mapping. Editing an applied changeset would not update a database that already ran it.

**Alternatives considered**: A `receipt_item_discount` table for a single pair of columns. Rejected as extra joins for a 0..1 value. Storing the summary as columns on `receipt`. Rejected so the summary stays a separate object with its own row, matching seller and totals.

## Validation

**Decision**: `ReceiptCompletenessChecker` accepts a null discount and a null summary. When either object is present, description must be non-blank and total must match the existing money pattern and parse to less than zero. JSON Schema uses a `negativeMoney` pattern for those two totals so the model is constrained the same way. Storage does not run JSON Schema itself, so the checker is the gate for `POST /api/receipt`.

**Rationale**: The existing money pattern allows positive amounts and "-0.00". Discounts are negative. Packaging deposits stay ordinary items and are not inferred from the word "kaucja" in code.

**Alternatives considered**: Inferring discounts by the description "OPUST" during storage. Rejected because the caller submits the structured object, and other chains use other words for a discount line.

## Transcription instructions

**Decision**: The system prompt tells the model to attach a discount line printed under an item, to put "OPUSTY ŁĄCZNIE" in `discountSummary`, to leave deposits as items, and to leave "Udzielono łącznie opustów" and "Promocje" in `unparsedLines`.

**Rationale**: The schema forbids extra discount fields, and the prompt is what stops the model from emitting "OPUST" as another item or leaving the summary in `unparsedLines`.
