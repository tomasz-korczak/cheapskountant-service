# Data Model: Expense Categories

Source of the receipt field: `items[].category` added to `.external-resources/receipt-schema.json` and `src/main/resources/schemas/receipt-schema.json`. The schema requires the string and does not list allowed names.

Expense JSON uses the category name. The database stores `category_id`. Database ids, `created_at`, and `updated_at` are not returned. There is no user or caller column.

Charset is `utf8mb4` / `utf8mb4_unicode_ci`, matching the receipt tables.

Every new table has:

- `id BIGINT NOT NULL AUTO_INCREMENT`, primary key
- `created_at DATETIME(3) NOT NULL`
- `updated_at DATETIME(3) NOT NULL`

On insert both timestamps are the same value, truncated to milliseconds, as `Timestamped` does for receipt rows. This feature has no update operation. Expense entities stamp themselves the same way inside the expense persistence package.

Changeset id: `cheapskountant:003-expense-categories`. Do not edit changesets `001` or `002`.

## expense_category

Fixed dictionary. Seeded by the changeset. No caller writes.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key |
| name | yes | `VARCHAR(50)`, unique |
| created_at, updated_at | yes | |

Seed these names exactly, in this order. `Unknown` is last and is not a household name.

1. Jedzenie
2. Jedzenie na mieście
3. Browar
4. Kwiatki
5. Bilet ZTM
6. Wyjazdy
7. Telefon
8. Kino
9. Słodycze
10. Lekarstwa/suplementy
11. Przybory toal.
12. Alkohol inny
13. Lekarze
14. Łachy
15. Multimedia
16. Inne wydatki
17. Materiały biurowe
18. Pieniądze, po prostu…
19. Książki i gazety
20. Rachunki / podatki
21. Przybory czyszczące
22. Naczynia,kuchnia
23. Narzędzia/mat. Eksploatacyjne
24. Numizmatyka
25. Elektronika
26. Dzieciaki
27. Samochód
28. Strzelectwo
29. Ofiary/darowizny
30. Przesyłki pocztowe/kurier
31. Oszczędności
32. Dom/remonty
33. Opakowania (torby, butelki)
34. Fermentacja alkoholowa
35. Unknown

`Pieniądze, po prostu…` uses one ellipsis character (U+2026), not three dots. `Naczynia,kuchnia` has no space after the comma. `Rachunki / podatki` has spaces around the slash. `Przybory toal.` includes the period.

The longest name fits in `VARCHAR(50)`.

## expense

One stored payment. Many rows may share a category and a payment date.

| Column | Required | Notes |
|--------|----------|-------|
| id | yes | Primary key. Not returned. |
| amount | yes | `DECIMAL(8,2)`. Zero and negative allowed. |
| payment_date | yes | `DATE`. No time. |
| category_id | yes | Foreign key to `expense_category`. Must be a household name, not `Unknown`. |
| currency | yes | `VARCHAR(3)`. Exactly three characters. |
| description | no | `VARCHAR(100)`. Null when omitted or blank. |
| created_at, updated_at | yes | |

`ON DELETE` for `category_id` is `RESTRICT`. Deleting a category must not delete expenses. There is no category-delete operation.

Index `payment_date` so the summary range scan does not read the whole table by default.

Validation before insert:

- The request is a JSON array of 1 to 500 objects.
- `amount` is a JSON number that fits in `DECIMAL(8,2)` with at most two fractional digits.
- `paymentDate` is a real calendar date, with no time.
- `category` matches one household name exactly, including case and surrounding spaces.
- `currency` has length 3.
- `description`, when present, has at most 100 characters and contains a character other than a space. A missing or empty description is stored as null. A description made only of spaces rejects the array.
- One invalid object rejects the array. Nothing from that request is inserted, including the valid objects.

## Summary projection

Not a table. For `payment_date` between `from` and `to`, inclusive:

| Field | Notes |
|-------|-------|
| category | `expense_category.name` |
| currency | `expense.currency`, as stored |
| amount | `SUM(expense.amount)`, scale 2 |

Group by category id, category name, and currency. Add amounts only when both the category and the currency match. `eur` and `EUR` are separate groups. Exclude `Unknown` only because expenses cannot reference it. The sum may exceed eight digits; the eight-digit limit applies to each stored amount, not to the total. There is no limit on how many expense rows share a payment date. Order of the summary is not significant.

## Category name list

Not a table. `GET /api/expenses` reads `expense_category.name` for the 34 household names, in the seeded household order, and omits `Unknown`. Each item in the response is the name. The rows are not created or changed by this read.

## receipt_item.category_id

Added to the existing `receipt_item` table. `receipt_item` already has `created_at` and `updated_at`.

| Column | Required | Notes |
|--------|----------|-------|
| category_id | yes | Foreign key to `expense_category`. `ON DELETE RESTRICT`. |

Changeset order:

1. Create and seed `expense_category`.
2. Create `expense`.
3. Add `receipt_item.category_id` nullable.
4. Set every existing item's `category_id` to the id of `Unknown`.
5. Alter `category_id` to `NOT NULL` and add the foreign key.

`ReceiptItemEntity` maps the association. The HTTP and model object keep the name in `ReceiptDraft.Item.category`. `ReceiptMapper` resolves the name on the way in and writes the name on the way out. A name that is not seeded is `incomplete`, and no receipt is stored.

JSON schema fragment for each item:

```json
"category": { "type": "string", "minLength": 1 }
```

Add `"category"` to the item `required` array. Do not add an `enum`.

No other receipt object gains a category. Item `discount` stays description and total only.
