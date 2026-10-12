# Research: Expense Categories

## Category dictionary and Unknown

**Decision**: One `expense_category` table holds the 34 household names plus `Unknown`. Names are unique and matched exactly, including Polish characters, spaces, punctuation, and the single ellipsis in `Pieniądze, po prostu…`. Callers cannot change the rows. A manually submitted expense may use only a household name. A receipt line item may use a household name or `Unknown`, and its generated expense inherits that category.

**Rationale**: The planning request makes the receipt item category a required foreign key, and transcription must set `Unknown`. A required foreign key needs a row for that name. The service still refuses `Unknown` on `POST /api/expense`; generated expenses bypass that caller-input rule and copy the already validated receipt-item association.

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

**Rationale**: The planning request says an unknown category or a missing required field rejects the whole request with HTTP 400. The spec already requires an atomic list of at most 500. Returning the stored names lets the caller see that the batch was kept without exposing database ids. The storage response does not return those ids. The daily list is the read that returns an expense id or a receipt id.

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

**Decision**: `GET /api/expenses`, `GET /api/expense`, `GET /api/expense/daily`, and `POST /api/expense` require the same bearer API key as receipt storage. The filter today returns early for every method other than `POST`, and only for `/api/receipt` and `/api/transcription`. It must allow `GET` for these paths and include `/api/expense`, `/api/expense/daily`, and `/api/expenses`.

**Rationale**: The constitution requires authentication unless an endpoint is explicitly public. The category names, the expense summary, and the daily list are not public.

**Alternatives considered**: Leaving `GET` open because the current filter ignores it. Rejected as a new unauthenticated read of the ledger and of the category names.

## Daily list

**Decision**: `GET /api/expense/daily?payment_date=YYYY-MM-DD` returns the standalone expenses for that date, then one total per receipt. A standalone expense has `receipt_item_id` null. Its row is type `expense`, the payment date, `expense.id`, `expense_category.name`, and `expense.amount`. A receipt row joins `expense` to `receipt_item`, `receipt`, and `seller`, groups by receipt id, and sums `expense.amount` for that payment date. Its row is type `receipt`, the payment date, `receipt.id`, `seller.trade_name`, and that sum. Standalone rows are ordered by expense id. Receipt rows follow, ordered by receipt id. Currency and description are omitted. Amounts use two decimal places. No rows is HTTP 200 and an empty array. A missing or non-calendar `payment_date` is HTTP 400, reason `invalid submission`. A read that cannot finish is HTTP 500, reason `storage failed`, and changes nothing.

**Rationale**: The caller needs every receipt and every non-receipt expense for one day, and needs the id of each so a later request can address it. Receipt lines are already expenses, so summing them by receipt avoids listing the same payment twice. The range summary remains the category-and-currency total across dates.

**Alternatives considered**: Returning every receipt line as its own expense row. Rejected because the caller asked for one row per receipt. Including currency. Rejected because the requested object has type, date, id, name, and amount. Ordering by name. Rejected because the id order is stable and does not depend on category or seller text.

## Category name list

**Decision**: `GET /api/expenses` returns the 34 household names, in the order listed in the spec, each as `{ "name": "<exact name>" }`. `Unknown` is not included. The list does not change when expenses are stored. A caller who is not authorized receives HTTP 401.

**Rationale**: The caller who stores an expense must choose a household name. The photo cannot supply one, and the receipt definition does not list the names. Returning only the name keeps database ids out of the response. `Unknown` stays available for receipt items and is refused on an expense, so it is not offered as a choice.

**Alternatives considered**: Including `Unknown` in the list. Rejected because an expense that uses it is refused. Returning bare strings. Rejected because the response is a list of dictionary items, and each item is the name.

## Generating expenses from stored receipt items

**Decision**: `POST /api/receipt` creates one expense per receipt item in the same transaction. Amount is `line_total + COALESCE(discount_total, 0)`. Payment date is the date component of `receipt_header.issued_at`; category is the item's category; currency is the header currency; description is null. The receipt-level discount summary is ignored.

**Rationale**: Item discounts are already associated with the item and are negative values, so adding them produces the net item expense. Applying the receipt-level summary would count those discounts a second time. A single transaction prevents a stored receipt from existing without its corresponding expense rows.

**Alternatives considered**: Creating one expense for the receipt total. Rejected because the requirement is one expense per item and each item can have a different category. Creating an extra expense for the receipt-level discount summary. Rejected because it duplicates item discounts. Reusing `StoreExpensesService`. Rejected because that path validates caller submissions, refuses `Unknown`, parses date strings, and has a 500-row request limit that does not describe receipt persistence.

## Receipt-item link and duplicate prevention

**Decision**: Add nullable `expense.receipt_item_id`, unique and foreign-keyed to `receipt_item` with `ON DELETE SET NULL`. Directly submitted and existing expenses leave it null. Existing receipts are not backfilled. Before generating an expense for an existing item, the service checks whether the link already exists; the unique constraint provides the concurrency-safe final guard.

**Rationale**: The link records provenance and enforces at most one generated expense per item without preventing standalone expenses. `ON DELETE SET NULL` preserves ledger history if a receipt item is removed. A new receipt submission creates new item ids, so it correctly creates a new set of expenses.

**Alternatives considered**: No source link. Rejected because generation could not be made idempotent per item. A required link. Rejected because manual and existing expenses have no receipt. Linking to the receipt rather than the item. Rejected because it would not identify or uniquely guard each one-to-one item mapping.

## Generated amount overflow and rollback

**Decision**: Validate every generated net amount against `DECIMAL(8,2)` before inserting the receipt. An out-of-range amount returns `invalid submission`. A database failure returns `storage failed`. Both outcomes roll back the receipt and every generated expense.

**Rationale**: Receipt line amounts use `DECIMAL(14,2)`, so valid receipt input can exceed the expense column. Pre-validation gives a deterministic client error and avoids partially persisted data.

**Alternatives considered**: Widening `expense.amount`. Rejected because the chosen behavior is to preserve the existing expense range. Saving the receipt but skipping the oversized item. Rejected because every saved receipt item must create an expense.
