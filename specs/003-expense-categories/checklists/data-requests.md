# Data and Request Requirements Checklist: Expense Categories

**Purpose**: Release-gate review of whether the expense data rules and the accept, refuse, and return outcomes are complete, clear, consistent, and measurable
**Created**: 2026-10-10
**Feature**: [spec.md](../spec.md)
**Audience**: Author, before tasks are written
**Depth**: Release gate

## Requirement Completeness

- [x] CHK001 Are the exact household category names, including punctuation and the single ellipsis, specified as the only names an expense may use? [Completeness, Spec §FR-002, Spec §FR-007] Resolved: FR-002 lists the names, and FR-007 limits an expense to those names.
- [x] CHK002 Is Unknown specified as a stored name that receipt line items may use and that expenses must refuse? [Completeness, Spec §FR-002, Spec §FR-007, Spec §FR-022] Resolved: Unknown is stored for receipt items and refused on an expense.
- [x] CHK003 Is the storage list specified as 1 to 500 expenses, with both an empty list and a list of 501 refused in full? [Completeness, Spec §FR-010, Spec §FR-011] Resolved: 1 to 500 is accepted. Empty and 501 are invalid submission, and nothing from that request is kept.
- [x] CHK004 Is atomic retention specified so that one invalid expense, or a save that does not finish, retains none of that list while earlier successful lists remain? [Completeness, Spec §FR-010, Spec §FR-018, Edge Cases] Resolved: one request is all or nothing. Earlier successful requests stay.
- [x] CHK005 Is the summary specified as the only expense retrieval, for an inclusive first and last payment date, with one total per category name and currency? [Completeness, Spec §FR-013] Resolved: stored amounts are retrieved only as that summary. Each total includes the stored currency. Category names are a separate read.
- [x] CHK006 Is a receipt line item's category specified as required, limited at storage to Unknown or a household name, while the receipt definition does not list those names? [Completeness, Spec §FR-019, Spec §FR-021, Spec §FR-022] Resolved: the definition requires the category and does not list names. Storage accepts only Unknown or a household name.
- [x] CHK007 Is the category requirement specified to apply only to receipt line items, and not to discounts, the discount summary, seller, payments, tax summary, totals, or unplaced lines? [Completeness, Spec §FR-023] Resolved: only a line item has a category.
- [x] CHK008 Is it specified that storing an expense does not create or change a receipt, and that transcribing or storing a receipt does not create an expense? [Completeness, Spec §FR-024] Resolved: the two records stay separate.

## Requirement Clarity

- [x] CHK009 Are amount limits specified as at most six digits before the decimal separator and at most two after it, including which zero and negative amounts are accepted? [Clarity, Spec §FR-005] Resolved: zero and negative amounts inside that size are accepted. Larger amounts are refused.
- [x] CHK010 Is "kept to two decimal places" defined for an amount submitted with fewer than two fractional digits? [Ambiguity, Spec §FR-005] Resolved: at most two fractional digits are allowed, so fewer than two is accepted, and amounts are expressed with two decimal places.
- [x] CHK011 Is it specified whether a category total may be larger than the maximum size of one expense amount? [Gap, Spec §FR-005, Spec §FR-013] Resolved: the size limit applies to each stored amount. The summed total may be larger.
- [x] CHK012 Is a payment date specified as a calendar date with no time and with no earliest or latest date? [Clarity, Spec §FR-006] Resolved: any calendar date is accepted, including a future date. A date that includes a time is refused.
- [x] CHK013 Is an impossible calendar date, such as a day that does not exist in that month, given an explicit refusal reason for both storage and summary? [Gap, Spec §FR-006, Spec §FR-015, Spec §FR-017] Resolved: both use "invalid submission".
- [x] CHK014 Is currency specified as exactly three characters, kept as submitted, and not checked against a currency catalog? [Clarity, Spec §FR-008] Resolved: exactly three characters, kept unchanged. `eur` and `EUR` differ. No conversion.
- [x] CHK015 Is description length specified as at most 100 characters, counting Polish letters as one character each, and is a missing or blank description accepted? [Clarity, Spec §FR-009, Assumptions] Resolved: 100 characters is accepted and 101 refuses the list. A missing or empty description is accepted. A description made only of spaces refuses the list.
- [x] CHK016 Is a description made only of spaces specified as blank or as present text? [Gap, Spec §FR-009] Resolved: it is not blank. It refuses the whole storage request. A missing or empty description is still accepted.
- [x] CHK017 Is the successful storage outcome specified beyond telling the caller that the whole list was stored, including whether amount, date, category, currency, and description are returned? [Gap, Spec §User Story 1] Resolved: a successful store returns the stored expenses, with the category still shown as the name. Description is included only when present.
- [x] CHK018 Is a summary range whose first and last dates are the same day explicitly accepted? [Clarity, Spec §FR-013, Gap] Resolved: that range includes only that day.

## Requirement Consistency

- [x] CHK019 Do the refusal reasons stay distinct and consistent: "invalid submission" for a bad expense or summary request, "not authorized" for a caller who is not allowed, "storage failed" for an unfinished save, and "incomplete" for a bad receipt category? [Consistency, Spec §FR-015, Spec §FR-016, Spec §FR-018, Spec §FR-022, Assumptions] Resolved: those four reasons stay on those cases.
- [x] CHK020 Do FR-019 and FR-020 agree on whether transcription refuses a missing item category or always sets that category to Unknown? [Conflict, Spec §FR-019, Spec §FR-020] Resolved: the photo has no category, so transcription sets the string Unknown and does not refuse the photo for that reason. Storage still refuses a blank category.
- [x] CHK021 Does User Story 5 scenario 3 agree with FR-022 that a non-blank category other than Unknown or a household name is refused? [Conflict, Spec §User Story 5, Spec §FR-022] Resolved: a category is never blank. A non-blank receipt category is accepted only when it is Unknown or a household name. The caller who stores an expense chooses a household name. `GET /api/expenses` returns those names.
- [x] CHK022 Does the User Story 2 title still describe one day while its scenarios require a first and last date? [Consistency, Spec §User Story 2] Resolved: the title is now a date range. The same date for both ends is that one day.
- [x] CHK023 Does the spec state that the 2026-10-10 clarifications replace the Input paragraph's single-day summary and unrestricted amount? [Conflict, Spec §Input, Spec §Clarifications] Resolved: the Input now says a date range. Unrestricted means many expenses of one category on one payment date. The limit of 500 applies only to one storage request.
- [x] CHK024 Are exact category match rules for spelling, case, surrounding spaces, and the ellipsis specified for expenses, and are those same rules specified for receipt item categories? [Consistency, Edge Cases, Spec §FR-022, Gap] Resolved: both must match exactly. A different case or surrounding spaces is refused.

## Acceptance Criteria Quality

- [x] CHK025 Does SC-003 state that the one-day check is a summary range whose first and last dates are that payment date? [Clarity, Spec §SC-003, Spec §FR-013] Resolved: a summary for one date is a range whose first and last dates are that day, and SC-003 uses that summary.
- [x] CHK026 Is "a short explanation" measurable, or does the spec require only that some explanation accompanies each refusal reason? [Measurability, Spec §User Story 4, Gap] Resolved: each refusal includes a reason and a short explanation. The wording of the explanation is not fixed.
- [x] CHK027 Does SC-006 cover every refused expense submission, and is an equivalent no-change outcome specified for a refused summary? [Coverage, Spec §SC-006, Edge Cases] Resolved: a refused expense submission does not change summaries. Asking for a summary, including a refused one, does not change stored expenses.

## Scenario and Edge Case Coverage

- [x] CHK028 Are exception outcomes specified for an unreadable expense submission, a missing required expense field, an unknown category, Unknown used as an expense, and a receipt item category that is missing or not stored? [Coverage, Spec §User Story 4, Spec §User Story 5, Spec §FR-022] Resolved: expense cases use "invalid submission". A missing or unknown receipt category uses "incomplete".
- [x] CHK029 Is a summary with no matching expenses specified to succeed with no totals, and is a category with no expenses in the range omitted rather than returned as zero? [Coverage, Spec §FR-014, Edge Cases] Resolved: an empty range succeeds with no totals, and unused categories are omitted.
- [x] CHK030 Is a category and currency whose amounts cancel specified to appear with a total of zero? [Edge Case, Edge Cases] Resolved: 10.00 and -10.00 in one category and the same currency produce a total of 0.00, and that pair is included. A different currency does not cancel it.
- [x] CHK031 Is there explicitly no cap on expenses per day or per category, separate from the 500-expense submission cap and the 500-total summary cap? [Clarity, Spec §FR-012, Spec §FR-017] Resolved: there is no per-day cap and no summary-size cap. Five Jedzenie expenses on one date are allowed. The limit of 500 is one storage request, so three requests of 200 can leave 600 expenses on that date.
- [x] CHK032 Is submitting the same expense twice specified as two retained expenses, with duplicate detection out of scope? [Clarity, Edge Cases] Resolved: the same expense stored twice is retained twice.
- [x] CHK033 Are requirements specified for receipts already stored before an item category became required? [Gap, Assumptions] Resolved: existing receipt items are given the category Unknown.

## Non-Functional Requirements

- [x] CHK034 Is authorization specified for both storing expenses and requesting a summary, and is it the same authorization already required for receipts? [Coverage, Spec §FR-016, Assumptions, Constitution §IV] Resolved: storing, summarizing, and reading category names use the same authorization as receipts. A caller who is not authorized receives "not authorized".
- [x] CHK035 Is it specified that an expense does not record which caller submitted it, and that every authorized caller uses one shared ledger? [Completeness, Spec §FR-001, Assumptions] Resolved: one shared ledger, and the expense does not record the caller.
- [x] CHK036 Are requirements defined for what an unauthorized summary request may reveal about whether expenses exist? [Gap, Security, Spec §FR-016] Resolved: storing, summarizing, and reading category names return the same "not authorized" reason and explanation whether or not expenses exist.
- [x] CHK037 Is an unfinished save specified to leave none of that list retained, and is a later submission of the same list then treated as a new store rather than a resume? [Recovery, Spec §FR-018, Edge Cases] Resolved: a failed save keeps nothing from that list. A later submission is a new store, not a resume. One invalid expense also rejects every valid expense in that same request.
- [x] CHK038 Is a summary read that does not finish given a refusal reason and a requirement that stored expenses stay unchanged? [Gap, Recovery, Spec §FR-018] Resolved: the reason is "storage failed", and stored expenses stay unchanged.

## Dependencies and Assumptions

- [x] CHK039 Is the decision to keep each currency as its own total documented as intentional, including that no conversion is performed and the stored currency is shown? [Assumption, Spec §FR-008, Spec §FR-013] Resolved: amounts are added only when category and currency both match. The summary returns that currency as stored and does not convert it.
- [x] CHK040 Is the boundary between this capability and individual expense listing, editing, deletion, and turning a receipt into expenses explicitly out of scope? [Completeness, Assumptions] Resolved: those actions are out of scope. Stored amounts are retrieved only as the summary.

## Notes

- Items are questions about the requirements, for the author to answer before tasks are written.
- Mark an item `[x]` only when the spec, or a recorded clarification, already answers it.
- All 40 items are closed. One invalid expense refuses the whole storage request.
