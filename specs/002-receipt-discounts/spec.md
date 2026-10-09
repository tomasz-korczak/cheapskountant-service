# Feature Specification: Receipt Discounts

**Feature Branch**: `002-receipt-discounts`

**Created**: 2026-10-07

**Status**: Draft

**Input**: User description: "A discount printed directly under a purchased item belongs to that item. It has a description and a negative total, and it has no quantity, unit price, or tax category. The printed heading OPUSTY ŁĄCZNIE is the total of all discounts and must be its own object on the receipt, not a line item and not leftover unparsed text."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Keep an item discount with the item it reduces (Priority: P1)

An authorized caller transcribes or stores a Polish fiscal receipt that prints a discount directly under a purchased item. The discount stays with that item. It is not returned or stored as a separate purchase.

**Why this priority**: Treating the discount as its own item misstates what was bought and hides which product was reduced.

**Independent Test**: Transcribe or store a receipt whose second item is followed by a discount line, such as a product line and then "OPUST" with only a negative amount. The product remains one line item with its printed quantity, unit price, line total, and tax category. The discount is part of that item, with the printed description and the negative amount. The discount is not a separate line item.

**Acceptance Scenarios**:

1. **Given** an authorized caller and a receipt photo where a discount line is printed directly under one item, **When** the caller transcribes that photo, **Then** the returned item includes that discount and the discount is not a separate line item.
2. **Given** a structured receipt whose item includes a discount, **When** the caller stores that receipt, **Then** the stored receipt returns the same item with the same discount.
3. **Given** an item with no discount printed under it, **When** the receipt is transcribed or stored, **Then** that item has no discount.
4. **Given** a packaging deposit printed as its own charge, **When** the receipt is transcribed or stored, **Then** the deposit remains a line item and is not treated as a discount.

---

### User Story 2 - Record the receipt discount total (Priority: P1)

An authorized caller transcribes or stores a receipt that prints a discount total, headed "OPUSTY ŁĄCZNIE", with one negative amount. That total is a separate discount summary on the receipt.

**Why this priority**: The discount total is a receipt fact. Leaving it among unplaced lines loses the sum of the discounts.

**Independent Test**: Transcribe or store a receipt that prints "OPUSTY ŁĄCZNIE" and a negative amount. The response includes one discount summary with that heading and that amount. The heading and amount are not a line item.

**Acceptance Scenarios**:

1. **Given** an authorized caller and a receipt photo that prints "OPUSTY ŁĄCZNIE" with a negative amount, **When** the caller transcribes that photo, **Then** the returned receipt includes one discount summary with that heading and amount.
2. **Given** a structured receipt that includes a discount summary, **When** the caller stores that receipt, **Then** the stored receipt returns the same discount summary.
3. **Given** a receipt that does not print a discount total, **When** the caller transcribes or stores it, **Then** the receipt has no discount summary.

---

### User Story 3 - Refuse a malformed discount (Priority: P2)

An authorized caller submits a discount, or a discount summary, that is missing its description or whose amount is not a negative currency amount. Transcription and storage refuse the receipt and store nothing.

**Why this priority**: A discount that is not a negative amount would corrupt the expense record.

**Independent Test**: Submit an item discount or a discount summary with a blank description, a missing amount, zero, or a positive amount. The reason is "incomplete", and no receipt is stored.

**Acceptance Scenarios**:

1. **Given** an item discount or a discount summary whose description is missing or blank, **When** the caller transcribes or stores that receipt, **Then** the service returns the reason "incomplete" and stores nothing.
2. **Given** an item discount or a discount summary whose amount is missing, not a currency amount, zero, or greater than zero, **When** the caller transcribes or stores that receipt, **Then** the service returns the reason "incomplete" and stores nothing.
3. **Given** a complete receipt with no discounts, **When** the caller transcribes or stores it, **Then** the receipt is accepted.

### Edge Cases

- A receipt has several items and only some of them have a discount. Each discounted item carries its own discount. Items without a discount do not.
- An item has at most one discount, the line printed directly under it. A further discount line under that same item is left unplaced rather than stored as another purchase.
- The item's own line total stays the amount printed on the item line. The discount does not replace that total.
- A receipt may include item discounts without a discount summary, or a discount summary without item discounts. Neither case is rejected for that reason alone.
- The service does not check that item discounts add up to the discount summary, or that line totals minus discounts equal the amount due.
- A packaging deposit, including a bottle deposit, remains a line item.
- The non-fiscal breakdown of discounts granted, such as "Udzielono łącznie opustów" and "Promocje", is not the discount summary. Those lines stay unplaced.
- When "OPUSTY ŁĄCZNIE" is recognized, that heading and its amount are the discount summary. They are not also returned as a line item.
- Optional discount details that are absent on the paper are omitted rather than filled with placeholders.
- A receipt that was already valid without discounts stays valid.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Transcription and storage MUST attach a discount to the line item printed directly above it. The discount MUST NOT be a separate line item.
- **FR-002**: An item discount MUST include a printed description and a total. The total MUST be a currency amount less than zero. The discount MUST NOT include a quantity, unit, unit price, or tax category.
- **FR-003**: An item without a printed discount MUST omit the discount.
- **FR-004**: An item MUST have at most one discount.
- **FR-005**: The item line total MUST remain the amount printed for that item. The discount amount is separate.
- **FR-006**: A printed discount total headed "OPUSTY ŁĄCZNIE" MUST be returned and stored as one discount summary on the receipt. The summary MUST include that heading and a total less than zero.
- **FR-007**: A receipt without a printed discount total MUST omit the discount summary.
- **FR-008**: The discount summary MUST NOT be a line item. When transcription recognizes it, the heading and its amount MUST NOT be returned only as unplaced text.
- **FR-009**: A packaging deposit MUST remain a line item and MUST NOT be recorded as a discount or as the discount summary.
- **FR-010**: When a present item discount or discount summary has a blank description, or an amount that is missing, not a currency amount, zero, or greater than zero, transcription and storage MUST each return the reason "incomplete" and MUST NOT store a receipt.
- **FR-011**: A receipt with no discounts MUST still be accepted when its other required parts are present.
- **FR-012**: Storage MUST return a stored item discount and a stored discount summary unchanged, together with the rest of the stored receipt.
- **FR-013**: This capability MUST NOT add a new endpoint. It extends the existing transcription and receipt storage behavior.

### Key Entities

- **Line item**: A purchased product, service, packaging charge, or other charge. It may include one discount.
- **Item discount**: The reduction printed directly under one line item. It has a description and a negative total.
- **Discount summary**: The receipt-level total of discounts, printed as "OPUSTY ŁĄCZNIE" and one negative amount. A receipt has at most one.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For a receipt that prints a discount directly under an item, the caller receives that item with the discount attached, and the discount is not a separate purchased item, in one transcription request and again after storage.
- **SC-002**: For a receipt that prints "OPUSTY ŁĄCZNIE" and a negative amount, the caller receives one discount summary with that heading and amount, and that heading is not a purchased item.
- **SC-003**: 100% of receipts whose only change from an already accepted receipt is the absence of discounts are still accepted.
- **SC-004**: 100% of submissions with a present discount or discount summary that lacks a description or has an amount that is not below zero are refused, and no receipt is stored.

## Assumptions

- This capability extends the existing transcription and storage of one Polish fiscal receipt. Endpoints, authorization, image limits, failure reasons, and the rule that photos are not retained stay as they are.
- On the supported receipts, one discount line belongs to the single item printed above it. The sample is a product line followed by "OPUST" and a negative amount, with no quantity, unit price, or tax category on that discount line.
- "OPUSTY ŁĄCZNIE" is the fiscal total of discounts. The later non-fiscal lines "Udzielono łącznie opustów" and "Promocje" are a separate breakdown and stay unplaced.
- A bottle or other packaging deposit is a charge, not a discount.
- Discount amounts are not reconciled with line totals, tax, the discount summary, or the amount due.
- A second discount line under the same item is out of scope for attachment. It stays unplaced and is not stored as a purchase.
- Currency amounts keep two fractional digits, as they do for other receipt amounts.
