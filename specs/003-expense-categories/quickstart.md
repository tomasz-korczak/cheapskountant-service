# Quickstart: Expense Categories

Validates expense storage, the category summary, and the receipt item category. The receipt flow and API key setup are in [the create-receipt quickstart](../001-create-receipt/quickstart.md). Field rules are in [data-model.md](./data-model.md). Requests and responses are in [expense-categories.openapi.yaml](./contracts/expense-categories.openapi.yaml).

## What changed

- `GET /api/expenses` returns the 34 household category names. Each item is the name. `Unknown` is not included.
- `POST /api/expense` stores 1 to 500 expenses. That limit is one request. Further requests may add more expenses on the same payment date. Category is sent as a name and stored as a foreign key.
- `GET /api/expense?from=YYYY-MM-DD&to=YYYY-MM-DD` returns one summed amount per category name and currency in that inclusive range.
- `GET /api/expense/daily?payment_date=YYYY-MM-DD` returns that day's standalone expenses, then one summed row per receipt. A standalone row uses the expense id and category name. A receipt row uses the receipt id and seller trade name.
- These calls require `Authorization: Bearer <API_KEY>`.
- Receipt `items[].category` is required. The schema does not list allowed names.
- Transcription sets every item category to `Unknown`.
- Storing a receipt creates one linked expense per receipt item in the same transaction.
- Schema copies: `.external-resources/receipt-schema.json` and `src/main/resources/schemas/receipt-schema.json`.
- Prompt: `src/main/resources/prompts/receipt-transcription-system.st`.

## Automated checks

From the repository root, with `JAVA_HOME` set to `C:\tools\jdk-23.0.2`:

```text
mvn test
```

Expected:

- `GET /api/expenses` returns the 34 household names and does not include `Unknown`.
- A list of household expenses is stored, and a later summary for those payment dates adds amounts that share a category name and a currency. A different currency is its own total.
- An empty list, a list of 501, one expense with a missing required field, a blank category, a description made only of spaces, an amount that does not fit eight digits with two decimal places, the category `Unknown`, or any other name that is not an exact household name is refused with HTTP 400 and reason `invalid submission`. None of the expenses in that request are stored.
- A payment date that cannot exist, such as 31 February, is `invalid submission` for storage and for a summary.
- Three accepted requests of 200 expenses on one payment date leave 600 stored expenses for that date.
- A summary with no expenses is an empty array. A missing date, or a first date after the last date, is `invalid submission`. The same date for both ends is that one day.
- A daily list returns each expense on that payment date with no receipt item, ordered by expense id, then one total per receipt on that date, ordered by receipt id. Receipt-linked expenses appear only in the receipt total. A date with no rows is an empty array. A missing or impossible `payment_date` is `invalid submission`.
- A stored receipt item returns its category name. A receipt item with no category, or a category that is not seeded, is `incomplete` and is not saved.
- A stored receipt creates one expense per item. An item expense is `line total + item discount total`, with a missing discount treated as zero. The receipt-level discount summary is ignored.
- Generated expenses use the receipt issue date, item category, and receipt currency; description is null. Each is uniquely linked to its source item.
- A generated amount outside `DECIMAL(8,2)` refuses and rolls back the whole receipt. Existing receipts are not backfilled.
- A transcribed receipt has `Unknown` on every item.
- The prompt tells the model to set `category` to `Unknown`.
- Both schema copies require `items[].category` and do not enumerate category names.
- `GET /api/expense`, `GET /api/expense/daily`, and `GET /api/expenses` without the API key are `not authorized`.

Docker is required for the MariaDB persistence test. Other tests do not call OpenRouter.

## Manual category names

Ask for the names with `GET /api/expenses` and header `Authorization: Bearer <API_KEY>`.

Expected: HTTP 200 and 34 objects, each with `name`. The first name is `Jedzenie`. `Unknown` is absent. Use one of those names as `category` on `POST /api/expense`.

## Manual expense check

Store two expenses with `POST /api/expense`:

```json
[
  {
    "amount": 12.5,
    "paymentDate": "2026-10-10",
    "category": "Jedzenie",
    "currency": "PLN",
    "description": "chleb"
  },
  {
    "amount": 3.5,
    "paymentDate": "2026-10-10",
    "category": "Jedzenie",
    "currency": "EUR"
  }
]
```

Expected: HTTP 201 and the same two expenses. `GET /api/expense?from=2026-10-10&to=2026-10-10` returns two objects: category `Jedzenie`, currency `PLN`, amount `12.50`; and category `Jedzenie`, currency `EUR`, amount `3.50`. Descriptions are absent. The amounts are not added together.

A third object with `"category": "Unknown"` in a new request returns HTTP 400, reason `invalid submission`, and does not change that total.

## Manual daily list

Ask for `GET /api/expense/daily?payment_date=2026-10-10` with the same API key, after the two expenses above are stored.

Expected: HTTP 200 and two objects, in expense-id order. Each has `type` `expense`, `date` `2026-10-10`, an `id`, `name` `Jedzenie`, and `amount` `12.50` or `3.50`. They stay separate rows. Currency and description are absent.

After the receipt check below, ask for the date part of that receipt's `issuedAt`. Expected: the receipt's line expenses are absent as `type` `expense`. One later object has `type` `receipt`, `id` equal to the stored receipt id, `name` equal to the seller trade name, and `amount` equal to the sum of that receipt's expense amounts. A missing `payment_date`, or `2026-02-31`, is HTTP 400 with reason `invalid submission`.

## Manual receipt check

Submit a receipt photo to `POST /api/transcription`. Every `items[]` entry has `"category": "Unknown"`.

Store that body with `POST /api/receipt`. The response keeps `Unknown` on each item and adds `id`. The response body is unchanged by expense generation.

Expected expense side effects:

- Exactly one expense is stored per receipt item.
- An item with line total `26.07` and item discount `-8.69` creates amount `17.38`; an item without a discount uses its line total.
- Payment date is the date part of `receipt.issuedAt`, category is `items[].category`, currency is `receipt.currency`, and description is null.
- `discountSummary` creates no expense and does not change item amounts.
- Re-evaluating generation for the same stored item creates no duplicate because `expense.receipt_item_id` is unique.

Replacing one item category with `Jedzenie` before storage returns `Jedzenie` for that item and uses `Jedzenie` on its generated expense. Replacing it with a name that is not seeded returns `incomplete` and stores neither the receipt nor any expense.

Set an item's net amount above `999999.99`. Expected: HTTP 400 with reason `invalid submission`, with neither the receipt nor any generated expense retained.
