# Research: Expense Categories

## Category dictionary and Unknown

**Decision**: One `expense_category` table holds the 34 household names plus `Unknown`. Names are unique and matched exactly, including Polish characters, spaces, punctuation, and the single ellipsis in `Pieniądze, po prostu…`. Callers cannot change the rows. An expense may use only a household name. A receipt line item may use a household name or `Unknown`.

**Rationale**: The planning request makes the receipt item category a required foreign key, and transcription must set `Unknown`. A required foreign key needs a row for that name. The spec still refuses `Unknown` on an expense, so the service treats that name as unknown for `POST /api/expense` even though the row exists for receipts.

**Alternatives considered**: Leaving `Unknown` out of the table and storing a free-text category on the receipt item. Rejected because the planning request requires a foreign key. Treating `Unknown` as a valid expense category. Rejected because it is the transcription placeholder, not a household category.

## Expense amount and currency

**Decision**: `expense.amount` is `DECIMAL(8,2)`. Zero and negative values are allowed. A value with more than two fractional digits, or that does not fit in eight digits total, refuses the whole list with HTTP 400. Currency is `VARCHAR(3)`, exactly three characters, stored as submitted, and not checked against a catalog. `eur` and `EUR` are different values.

**Rationale**: The planning request fixes the column as eight digits including two decimal places. The spec allows zero and negative amounts and does not ask for currency conversion.

**Alternatives considered**: Reusing receipt `DECIMAL(14,2)`. Rejected because the planning request sets a smaller expense amount. Uppercasing currency. Rejected because the spec keeps the submitted characters.

## What the summary returns

**Decision**: `GET /api/expense?from=YYYY-MM-DD&to=YYYY-MM-DD` returns one object per category name and currency that has at least one expense with `payment_date` on or between those dates. The same date for both ends is that one day. Each object has `category` (the name), `currency` (the stored value), and `amount` (the sum, two decimal places). Descriptions are omitted. Amounts are added only when both the category and the currency match. `eur` and `EUR` are separate totals. Currencies are not converted. A category and currency with no expenses in the range is omitted. A pair whose amounts cancel to `0.00` is included. No rows is HTTP 200 and an empty array. The first date must be on or before the last date. Missing or non-calendar dates are HTTP 400. There is no cap on the number of summary rows and no cap on stored expenses for a date.

**Rationale**: The summary is category name, currency, and summed amount for a date range. The caller needs to see which currency the total is in. The limit of 500 is the size of one `POST /api/expense` body. Three accepted requests of 200 expenses may leave 600 rows on one payment date. Grouping by the fixed category names and the stored currency cannot be rejected for size.

**Alternatives considered**: One total per category, adding different currencies and omitting currency. Rejected because the summary must keep each currency separate. Refusing a summary of more than 500 groups. Rejected because 500 applies only to one storage request, not to stored rows or to the summary.

## Storing a list of expenses

**Decision**: `POST /api/expense` accepts a JSON array of 1 to 500 expenses. Category is the name. The service resolves it to `category_id` before insert. Any one of these refuses the whole array with HTTP 400 and reason `invalid submission`, including every valid expense in that request: empty array, more than 500 objects, a body that is not an array, a missing required field, a blank category, an amount outside `DECIMAL(8,2)`, a payment date that includes a time or is not a real calendar date, a currency that is not exactly three characters, a description made only of spaces, a description longer than 100 characters, a household name that does not match exactly, or the name `Unknown`. A missing or empty description is stored as null. One database transaction inserts the list. A failure while saving, or a summary that cannot be read, returns HTTP 500, reason `storage failed`. A failed save retains none of that list. A failed summary changes nothing. Success is HTTP 201 and returns the stored expenses with the category still shown as the name.

**Rationale**: The planning request says an unknown category or a missing required field rejects the whole request with HTTP 400. The spec already requires an atomic list of at most 500. Returning the stored names lets the caller see that the batch was kept without exposing database ids. Individual expense retrieval stays out of scope.

**Alternatives considered**: HTTP 422 and reason `incomplete`, matching receipt field errors. Rejected because the planning request uses HTTP 400 for expense validation. Returning only a count. Rejected because the caller should see the category names that were accepted.

## Receipt item category

**Decision**: Both copies of `receipt-schema.json` add `category` to each object in `items`. It is required, a string, and not an enum. `ReceiptDraft.Item` gains `category`. `StoredReceiptResponse` uses that record, so both HTTP bodies gain the field. `receipt_item.category_id` is a required foreign key to `expense_category`. Existing item rows are set to the `Unknown` id before the column becomes non-null. A missing or blank category, or a name that is not seeded, is reason `incomplete` and stores no receipt. Discounts, tax lines, seller, payments, totals, and unparsed lines do not gain a category.

**Rationale**: The planning request says to change the schema and every interface that carries the receipt object, to require the item category, and not to put the category values in the schema. Membership is enforced when resolving the foreign key, not by an enum in the schema sent to the model.

**Alternatives considered**: An enum of the 35 names in the JSON schema. Rejected because the planning request says not to include the values. A nullable category for old rows. Rejected because the column is required.

## Transcription

**Decision**: `receipt-transcription-system.st` tells the model to set every item's `category` to `Unknown` and not to infer a household category from the product. The photo does not contain a category, so that string is the only value the model can supply. After the model JSON is parsed, and before the completeness check, the service sets `category` to `Unknown` on every item. Transcription does not refuse the photo only because no category is printed. Storage of a caller-supplied receipt does not rewrite the category. A blank category on storage is `incomplete`.

**Rationale**: The schema requires the field, and the photo has nowhere to read a household category from. The prompt makes the model emit `Unknown`. The overwrite keeps that result even if the model guesses a product category.

**Alternatives considered**: Prompt only. Rejected because a model guess would then be returned and could be stored as a household category the caller did not choose. Overwrite only, with no prompt change. Rejected because the planning request asks for the prompt instruction, and a missing field would fail schema-constrained output before the overwrite can run.

## Authorization

**Decision**: `GET /api/expenses`, `GET /api/expense`, and `POST /api/expense` require the same bearer API key as receipt storage. The filter today returns early for every method other than `POST`, and only for `/api/receipt` and `/api/transcription`. It must allow `GET` for these paths and include both `/api/expense` and `/api/expenses`.

**Rationale**: The constitution requires authentication unless an endpoint is explicitly public. The category names and the expense summary are not public.

**Alternatives considered**: Leaving `GET` open because the current filter ignores it. Rejected as a new unauthenticated read of the ledger and of the category names.

## Category name list

**Decision**: `GET /api/expenses` returns the 34 household names, in the order listed in the spec, each as `{ "name": "<exact name>" }`. `Unknown` is not included. The list does not change when expenses are stored. A caller who is not authorized receives HTTP 401.

**Rationale**: The caller who stores an expense must choose a household name. The photo cannot supply one, and the receipt definition does not list the names. Returning only the name keeps database ids out of the response. `Unknown` stays available for receipt items and is refused on an expense, so it is not offered as a choice.

**Alternatives considered**: Including `Unknown` in the list. Rejected because an expense that uses it is refused. Returning bare strings. Rejected because the response is a list of dictionary items, and each item is the name.
