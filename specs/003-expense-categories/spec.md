# Feature Specification: Expense Categories

**Feature Branch**: `003-expense-categories`

**Created**: 2026-10-09

**Status**: Draft

**Input**: User description: "Add support for expenses and expense categories. The category dictionary contains a fixed Polish household list. The service stores and retrieves expenses. Each expense has a required amount with two decimal places, a required payment date with no time, a required category from the dictionary, a required 3-character currency, and an optional description of at most 100 characters. Many expenses of the same category may be stored on one payment date. One storage request accepts at most 500 expenses, and further requests may add more on that same date. Retrieval of stored expenses is a summary for a payment-date range, from a first date through a last date: the amount sum of each category and currency, without descriptions. The caller can also read the household category names. Receipt line items also have a required expense category. The receipt definition requires that category and does not list allowed values. Transcription always sets each line item's expense category to Unknown, because the category is not on the receipt photo."

## Clarifications

### Session 2026-10-10

- Q: How large may one expense amount be, and how is a range of days summarized? → A: An amount has at most six digits before the decimal separator and two after it. The summary takes a first and last payment date, inclusive, and returns one summed amount per category name and currency. Amounts are added only when both the category and the currency match. The summary includes that currency and does not include a description.
- Q: Are amounts in different currencies added into one category total? → A: No. Each total is one category name and one currency, as stored. `eur` and `EUR` stay separate. Currencies are not converted. Descriptions are omitted.
- Q: What does an unrestricted number of expenses mean, and what does the limit of 500 apply to? → A: One payment date may have many expenses of the same category, such as five separate Jedzenie expenses. The limit of 500 applies only to one storage request. It is not a limit on stored rows. Three accepted requests of 200 expenses can leave 600 expenses on one payment date.
- Q: What happens when an expense category is not one of the household names? → A: The whole expense list is refused. Nothing from that list is retained. The name Unknown is reserved for receipt line items and is refused on an expense.
- Q: Must a receipt line item's category be one of the stored category names? → A: Yes. The receipt definition requires the category and does not list the names. Storage accepts a line item only when the category is a household name or Unknown. A blank category is refused.
- Q: Does transcription refuse a receipt because the photo has no category, or does it fill one? → A: The photo has no category. Transcription sets every line item's category to the string Unknown. It does not refuse the photo for a missing printed category.
- Q: Who chooses the category on a stored expense, and how do they learn the allowed names? → A: The caller who stores expenses sets a non-blank category on each expense. That name must be one of the household names. The caller can read those names. Unknown is not one of them.
- Q: What refusal applies when a payment date cannot exist, such as 31 February? → A: Storing expenses and asking for a summary both use the reason "invalid submission".
- Q: Is an expense description made only of spaces blank? → A: No. A missing or empty description is accepted. A description made only of spaces refuses the whole storage request.
- Q: Do receipt item categories use the same exact-name rules as expenses? → A: Yes. The category must match Unknown or a household name exactly, including spelling, case, surrounding spaces, and the single ellipsis.
- Q: What may an unauthorized request reveal? → A: Storing expenses, asking for a summary, and reading category names all return the same "not authorized" reason and explanation, whether or not expenses exist.
- Q: What happens when a summary cannot be read? → A: The reason is "storage failed", and stored expenses stay unchanged.
- Q: Does one invalid expense reject the rest of the storage request? → A: Yes. One expense with a blank or unknown category, a payment date that is not a real calendar date, or a missing required part refuses the whole request. No expense from that request is stored.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Store household expenses (Priority: P1)

An authorized caller submits a list of expenses to one shared ledger. Each expense records how much was paid, on which calendar day, in which currency, and under which household category. A short description may be included.

**Why this priority**: Stored expenses are the record the daily summary is built from.

**Independent Test**: Submit a list of one to 500 complete expenses that use dictionary categories, including more than one expense on the same day and in the same category. All of them are retained. A later summary for each payment date includes their amounts.

**Acceptance Scenarios**:

1. **Given** an authorized caller and a list of one to 500 complete expenses, **When** the caller stores that list, **Then** every expense in the list is retained and the caller is told that the whole list was stored.
2. **Given** a stored expense, **When** it is later included in a summary, **Then** it contributes its amount, category name, and currency. Its description does not appear.
3. **Given** several expenses on the same payment date, including repeats of the same category, **When** the caller stores them in one or more submissions, **Then** every expense is retained. There is no daily limit and no per-category limit.
4. **Given** an expense with no description, **When** the caller stores it, **Then** the expense is retained without a description.

---

### User Story 2 - Review a date range by category and currency (Priority: P1)

An authorized caller asks for a first and last payment date and receives one summed amount for each category name and currency used in that range. Descriptions are not included. Amounts are added only when both the category and the currency match. Currencies are not converted.

**Why this priority**: The caller needs spending by category and currency for a range of days, not a line-by-line ledger.

**Independent Test**: Store expenses inside and outside a date range, using a repeated category in more than one currency. Ask for that range. The result contains one sum for each category and currency used inside the range, keeps different currencies apart, and includes nothing from outside the range.

**Acceptance Scenarios**:

1. **Given** stored expenses whose payment dates fall in the requested range, **When** the caller asks for that first and last date, **Then** the result has one summed amount for each category name and currency used in the range, each total includes that currency, and no descriptions.
2. **Given** two expenses in the same category and the same currency in the requested range, **When** the caller asks for that range, **Then** those amounts are added into one total.
3. **Given** two expenses in the same category and different currencies in the requested range, **When** the caller asks for that range, **Then** the result has two totals. Each keeps its own currency. The amounts are not converted or added together.
4. **Given** expenses stored outside the requested range, **When** the caller asks for a range, **Then** only expenses whose payment date is on or between the first and last date are included.
5. **Given** no expenses in the requested range, **When** the caller asks for that range, **Then** the summary succeeds and contains no totals.
6. **Given** the first and last dates are the same day, **When** the caller asks for that range, **Then** the summary includes only expenses whose payment date is that day.

---

### User Story 3 - Record an expense category on each receipt item (Priority: P1)

An authorized caller transcribes or stores a receipt. Every line item has an expense category. Transcription sets that category to Unknown on every line item, because a receipt photo does not state the household category. A caller who stores a structured receipt supplies the category, and storage keeps the supplied value.

**Why this priority**: A receipt item is an expense line. It cannot be stored or returned without a category, and transcription still has to produce a complete receipt before anyone has chosen a household category.

**Independent Test**: Transcribe a complete receipt photo and confirm every returned line item has the expense category Unknown. Store a structured receipt whose items use Unknown and whose items use household category names. Each stored item returns that category name. A category outside those names is refused and nothing is stored. Seller, payments, tax summary, totals, discounts, and the discount summary have no expense category.

**Acceptance Scenarios**:

1. **Given** an authorized caller and a readable photo of a complete receipt, **When** the caller transcribes it, **Then** every returned line item has the expense category Unknown, and nothing is stored.
2. **Given** a structured receipt whose every line item uses Unknown or a household dictionary name, **When** the caller stores it, **Then** the stored receipt returns each item with the category that was submitted.
3. **Given** a line item category of Unknown or one of the household dictionary names, **When** the caller stores that receipt, **Then** the category is accepted and returned unchanged.
4. **Given** a line item category that is not an exact match for Unknown or a household name, including a different case or surrounding spaces, **When** the caller stores that receipt, **Then** the service returns the reason "incomplete" and a short explanation, and no receipt is stored.
5. **Given** a receipt with several line items, **When** it is transcribed or stored, **Then** each line item has its own expense category. No other receipt part has an expense category.

---

### User Story 4 - Refuse an expense that cannot be kept (Priority: P2)

An authorized caller submits an expense list that breaks a submission rule, or a caller who is not authorized tries to store or summarize expenses. The service refuses the request and keeps nothing from that submission.

**Why this priority**: A partial or malformed batch would make the daily sums untrustworthy.

**Independent Test**: Submit 501 expenses, an empty or unreadable submission, an expense missing a required part, an amount that does not fit six digits before the decimal separator and two after it, a currency that is not exactly three characters, a category outside the household names, the category Unknown, a description longer than 100 characters, a summary request without both dates, and an unauthorized call. Each is refused with the matching reason. A refused storage submission leaves the ledger unchanged.

**Acceptance Scenarios**:

1. **Given** an authorized caller and a list of 501 expenses, **When** the caller submits it, **Then** the service returns the reason "invalid submission" and a short explanation, and none of those expenses are retained.
2. **Given** an authorized caller and a submission that is empty, unreadable, or not a list of expenses, **When** the caller submits it, **Then** the service returns the reason "invalid submission" and a short explanation, and nothing is retained.
3. **Given** an authorized caller and a list in which any expense lacks an amount, payment date, category, or currency, or has an amount that is not a number with at most six digits before the decimal separator and two after it, a payment date that includes a time or is not a real calendar date, a currency that is not exactly three characters, a category that is not an exact household name, the category Unknown, a description made only of spaces, or a description longer than 100 characters, **When** the caller submits that list, **Then** the service returns the reason "invalid submission" and a short explanation, and none of the expenses in that list are retained, including the valid ones.
4. **Given** a caller who is not authorized, **When** that caller stores expenses, asks for a summary, or reads the category names, **Then** the service returns the same reason "not authorized" and the same short explanation whether or not any expenses exist, and nothing is retained or revealed.
5. **Given** an authorized caller and a summary request that lacks a first date or a last date, whose dates are not real calendar dates, or whose first date is after the last date, **When** the caller submits it, **Then** the service returns the reason "invalid submission" and a short explanation.
6. **Given** a complete list of expenses and a failure while saving it, **When** the save does not finish, **Then** the service returns the reason "storage failed" and a short explanation, and none of the expenses in that list remain retained.
7. **Given** stored expenses and a summary that cannot be read, **When** the read does not finish, **Then** the service returns the reason "storage failed" and a short explanation, and the stored expenses stay unchanged.

---

### User Story 5 - Refuse a receipt item without a category (Priority: P2)

An authorized caller stores a receipt whose line item has no expense category. The receipt is refused and nothing is stored. Transcription does not refuse a photo for a missing category. The photo has no category, so transcription sets every line item to Unknown.

**Why this priority**: A stored receipt item without a category would break the rule that every item is categorized. A photo cannot supply that category.

**Independent Test**: Store a receipt that is otherwise complete but has a line item with a missing or blank expense category. The reason is "incomplete", and no receipt is stored. Transcribe a complete receipt photo and confirm every line item category is the string Unknown.

**Acceptance Scenarios**:

1. **Given** an authorized caller and a structured receipt with a line item whose expense category is missing or blank, **When** the caller stores that receipt, **Then** the service returns the reason "incomplete" and a short explanation, and no receipt is stored.
2. **Given** a transcribed receipt, **When** the caller inspects its line items, **Then** every line item has the category Unknown, because the photo does not contain a category.
3. **Given** a complete receipt whose every line item category is non-blank and is Unknown or a household name, **When** the caller stores it, **Then** the category requirement does not by itself cause refusal.

---

### User Story 6 - Read the household category names (Priority: P1)

An authorized caller reads the household category names before storing expenses. The caller sets one of those names on each expense. Unknown is not in the list.

**Why this priority**: The caller who stores an expense chooses its category, and that name must be one of the household names.

**Independent Test**: Ask for the category names. The result is the 34 household names in the order listed in FR-002, starting with Jedzenie and ending with Fermentacja alkoholowa, and does not include Unknown. A caller who is not authorized is refused.

**Acceptance Scenarios**:

1. **Given** an authorized caller, **When** the caller asks for the category names, **Then** the result is exactly the household names, in the order given for those names, and each entry is the name.
2. **Given** that result, **When** the caller inspects it, **Then** Unknown is not included.
3. **Given** a caller who is not authorized, **When** that caller asks for the category names, **Then** the service returns the reason "not authorized" and a short explanation.

---

### Edge Cases

- A submission of exactly 500 complete expenses is accepted. A submission of 501 is refused in full.
- An empty list is not a successful store. It is an invalid submission.
- Zero, negative, and positive amounts are accepted when they have at most six digits before the decimal separator and at most two after it. A larger amount, or one with more than two fractional digits, refuses the whole list.
- Any calendar date is accepted, including future dates. A date that includes a time, or a date that cannot exist, such as 31 February, refuses the whole storage request. The same impossible date refuses a summary.
- A description that is missing or empty is accepted. A description made only of spaces refuses the whole storage request.
- The same category may be used any number of times on the same day. Repeated expenses are separate records and are all included in that day's sum.
- Amounts that cancel out, such as 10.00 and -10.00 in the same category and the same currency, still produce a total of 0.00 for that pair. A different currency does not cancel that total.
- A category and currency with no expenses in the requested range is omitted. It is not returned as zero.
- Amounts are added only when they share both a category name and a currency. The summary returns that currency as stored and does not convert currencies. `eur` and `EUR` are different totals.
- Category names match exactly, including Polish characters, spaces, punctuation, and case. A different spelling, a different case, or surrounding spaces is refused for an expense and for a receipt line item.
- The dictionary name "Pieniądze, po prostu…" uses a single ellipsis character. Three separate dots are a different name and are refused for an expense.
- "Naczynia,kuchnia" has no space after the comma. "Rachunki / podatki" has spaces around the slash. "Przybory toal." includes the final period.
- Unknown is the transcription value for a receipt line item. It is stored as a category name so a receipt item can refer to it. It is refused when used as an expense category.
- A receipt line item may use Unknown or a household dictionary name. Any other category refuses the receipt and stores nothing.
- Only receipt line items have an expense category. Item discounts, the discount summary, seller, payments, tax summary, totals, and unplaced lines do not.
- Transcription sets Unknown on every line item even when the caller might later choose a dictionary category. Storage of a structured receipt does not replace a submitted category with Unknown.
- One invalid expense rejects the whole storage request, including every valid expense in that same request. A blank or unknown category, a payment date that is not a real calendar date, a description made only of spaces, or a missing required part is enough. Expenses from earlier successful requests stay retained.
- The same expense submitted twice is retained twice. Duplicate detection is outside this capability.
- A description of exactly 100 characters is accepted. A description of 101 characters rejects the whole list.
- The first and last dates of a summary are included. A range whose first and last dates are the same day is that one day. A range that starts after it ends is refused.
- Asking for a summary does not change stored expenses.
- The limit of 500 applies only to one storage request. It does not limit how many expenses may be stored for one payment date. Three accepted requests of 200 expenses on the same date retain 600 expenses.
- A category is never blank. Transcription fills Unknown on a receipt item because the photo has no category. An expense category is chosen by the caller and must be a household name.
- The household dictionary is fixed. This capability does not add, rename, or remove categories.
- Storing expenses does not create or change a receipt. Transcribing or storing a receipt does not create an expense.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The service MUST keep one shared expense ledger for every authorized caller. An expense MUST NOT record which caller submitted it.
- **FR-002**: The household category names MUST be exactly these, and no others: Jedzenie; Jedzenie na mieście; Browar; Kwiatki; Bilet ZTM; Wyjazdy; Telefon; Kino; Słodycze; Lekarstwa/suplementy; Przybory toal.; Alkohol inny; Lekarze; Łachy; Multimedia; Inne wydatki; Materiały biurowe; Pieniądze, po prostu…; Książki i gazety; Rachunki / podatki; Przybory czyszczące; Naczynia,kuchnia; Narzędzia/mat. Eksploatacyjne; Numizmatyka; Elektronika; Dzieciaki; Samochód; Strzelectwo; Ofiary/darowizny; Przesyłki pocztowe/kurier; Oszczędności; Dom/remonty; Opakowania (torby, butelki); Fermentacja alkoholowa. The stored category names MUST be those household names plus Unknown.
- **FR-003**: Callers MUST NOT add, rename, or remove dictionary categories.
- **FR-004**: An expense MUST have an amount, a payment date, a category, and a currency. The description is optional.
- **FR-005**: The amount MUST be a number with at most six digits before the decimal separator and at most two digits after it, kept to two decimal places. Zero and negative amounts within that size MUST be accepted. An amount outside that size MUST be refused.
- **FR-006**: The payment date MUST be a calendar date with no time. There MUST be no earliest or latest date.
- **FR-007**: The category on an expense MUST be exactly one of the household names in FR-002. The name Unknown MUST be refused for an expense.
- **FR-008**: The currency MUST be exactly three characters and MUST be kept as submitted. The service MUST NOT check it against a currency catalog and MUST NOT convert it.
- **FR-009**: A description, when present, MUST be at most 100 characters and MUST contain a character other than a space. A missing or empty description MUST be accepted. A description made only of spaces MUST refuse the whole storage request.
- **FR-010**: Expense storage MUST accept a list of 1 to 500 expenses. The whole list MUST be retained, or none of it MUST be retained.
- **FR-011**: A list of more than 500 expenses, an empty list, or a submission that cannot be read as a list of expenses MUST be refused with the reason "invalid submission". Nothing from that submission is retained.
- **FR-012**: There MUST be no limit on how many expenses, or how many expenses of one category, may fall on one payment date. The limit of 500 applies only to one storage request. Several accepted requests MAY leave more than 500 expenses on one payment date.
- **FR-013**: The only retrieval of stored expense amounts MUST be a summary for an inclusive payment-date range. The summary MUST contain one summed amount for each category name and currency that has at least one expense in that range. Amounts MUST be added only when both the category and the currency match. Each total MUST include that currency as stored. The summary MUST NOT convert currencies, and MUST NOT include descriptions or individual expenses. A range whose first and last dates are the same day MUST include only that day.
- **FR-014**: A summary for a range with no expenses MUST succeed and MUST contain no totals. A category and currency with no expenses in that range MUST be omitted.
- **FR-015**: One invalid expense MUST cause the whole storage request to be refused with the reason "invalid submission". No expense from that request is retained, including the valid ones. An expense is invalid when it lacks an amount, payment date, category, or currency, uses a category that is not an exact household name, uses the category Unknown, uses an amount outside the size in FR-005, uses a payment date that includes a time or is not a real calendar date, uses a currency that is not exactly three characters, uses a description made only of spaces, or uses a description longer than 100 characters.
- **FR-016**: A caller who is not authorized MUST receive the reason "not authorized" and the same short explanation when storing expenses, requesting a summary, or reading the category names. That response MUST be the same whether or not any expenses exist, and it MUST NOT reveal whether they exist. Nothing is retained.
- **FR-017**: A summary request without a first date or a last date, with a value that is not a real calendar date, or with a first date after the last date MUST be refused with the reason "invalid submission".
- **FR-018**: If saving a valid expense list does not finish, the service MUST return the reason "storage failed" and MUST NOT retain any expense from that list. If a summary cannot be read, the service MUST return the reason "storage failed" and MUST NOT change stored expenses.
- **FR-019**: Every receipt line item MUST have a non-blank expense category. Receipt storage MUST treat a missing or blank item category as incomplete, return the reason "incomplete", and store no receipt.
- **FR-020**: Transcription MUST set the expense category of every line item to the string Unknown, because the receipt photo does not contain a category. Transcription MUST NOT refuse the photo only because no category is printed.
- **FR-021**: The receipt definition MUST require an expense category on each line item and MUST NOT list the allowed category names.
- **FR-022**: Receipt storage MUST keep and return the expense category submitted on each line item only when that category is an exact match for Unknown or a household name, including spelling, case, surrounding spaces, and the single ellipsis. Any other category MUST be refused with the reason "incomplete", and no receipt is stored.
- **FR-023**: No receipt part other than a line item may have an expense category.
- **FR-024**: Storing an expense MUST NOT create or change a receipt. Storing or transcribing a receipt MUST NOT create an expense.
- **FR-025**: An authorized caller MUST be able to read the household category names. The result MUST be exactly the names in FR-002, in that order, and each result MUST be the name. The result MUST NOT include Unknown. A caller who is not authorized MUST receive the reason "not authorized".

### Key Entities

- **Expense category**: One stored name. The household names are the only names an expense may use. Unknown is an additional stored name used by receipt line items.
- **Expense**: One recorded payment in the shared ledger. It has an amount, a payment date, a household category, a currency, and an optional description.
- **Category total**: The summed amount of one category name and one currency across an inclusive payment-date range. It has the category name, the currency, and the amount. It has no description.
- **Receipt line item**: A line on a transcribed or stored receipt. It has a required expense category. Transcription sets that category to Unknown.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An authorized caller can store 500 complete expenses in one submission, and a submission of 501 expenses is refused with none of those 501 retained.
- **SC-002**: For every requested payment-date range, from the first date through the last date, the summary matches the stored amounts: one total per category name and currency used in that range, amounts added only when both match, the currency included as stored, and no descriptions included.
- **SC-007**: An authorized caller can read all 34 household category names, and that list does not include Unknown. Three accepted storage requests of 200 expenses on one payment date leave 600 expenses stored for that date.
- **SC-003**: 100% of the 34 dictionary categories can be stored on a single payment date and each appears in that date's summary.
- **SC-004**: 100% of successfully transcribed receipt line items show the expense category Unknown.
- **SC-005**: 100% of receipt submissions whose only defect is a missing or blank line-item expense category are refused, and no receipt is stored.
- **SC-006**: A refused expense submission changes none of the daily summaries.

## Assumptions

- Expense storage, the date-range summary, and the household category names are caller-facing capabilities. They use the same authorization and the same failure body already used for receipts. A bad expense or summary request uses the reason "invalid submission". A receipt item category that is missing or not a stored name uses the reason "incomplete". A save that does not finish uses "storage failed". An unauthorized caller receives "not authorized".
- The ledger is shared. Any authorized caller stores into the same expenses and reads the same daily summaries.
- A submission is atomic. One invalid expense, or a save that does not finish, retains none of that list. Earlier successful submissions stay as they were.
- An empty expense list is an invalid submission, not a request to store nothing.
- "Any amount" of expenses means many separate expenses may share one category on one payment date. It does not remove the amount size in FR-005. The limit of 500 is the size of one storage request, not a limit on stored rows for a date.
- The summary adds every amount that shares a category and a currency across the requested dates. It returns that currency as stored. It does not convert currencies.
- A category and currency are included only when at least one expense used that pair in the range, even when the summed amount is 0.00.
- Summary order is not significant. Each category name and currency appears once.
- Expense amounts and category totals are expressed with two decimal places.
- Currency is any three characters, kept unchanged. "eur" and "EUR" are different values.
- Description length counts characters, so Polish letters count as one character each.
- Unknown is a stored category name so a transcribed receipt item can refer to it. It is not a household name and cannot be used on an expense.
- The receipt definition requires a category and does not publish the allowed names. Storage still accepts only Unknown or a household name.
- This capability does not list, edit, or delete individual expenses, and it does not turn a receipt into expenses.
- Existing receipt rules stay in place. A receipt that was complete before this change must also include an expense category on every line item.
