# Feature Specification: Create Receipt from Photo

**Feature Branch**: `001-create-receipt`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Expense-tracking service. The primary capability accepts a receipt photo, transcribes it into a structured receipt with automated transcription, stores that receipt only when transcription succeeds, and returns the stored receipt. When transcription does not succeed, the caller receives an error and nothing is stored."

## Clarifications

### Session 2026-09-26

- Q: Should each stored receipt belong to the person who submitted the photo, or should every authorized caller add receipts to one shared ledger? → A: One shared ledger. Any authorized caller adds receipts to the same collection, and the receipt does not record who submitted it.
- Q: When a submission does not store a receipt, should the caller receive a fixed reason as well as a short explanation? → A: Fixed reason plus a short explanation. The reasons are invalid submission, not authorized, unreadable, incomplete, and transcription unavailable.
- Q: Should the first release be accepted only when 90% of a fixed set of real receipt photos succeed, or is that target checked later? → A: Remove the 90% target. Acceptance is only the scenario tests in this specification.
- Q: How many images may one receipt submission contain? → A: One to five images, in order, of one receipt.
- Q: Should a database failure after a valid transcription be its own failure reason? → A: Add `storage failed`. Nothing remains stored.
- Q: What should happen when an uploaded image has no declared media type? → A: Accept it and treat it as JPEG.
- Q: Is a receipt incomplete when the tax summary has no entries? → A: At least one tax summary entry is required.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Store a transcribed receipt (Priority: P1)

An authorized caller submits one to five photos of one Polish fiscal receipt, in page order. The service transcribes those photos into one structured receipt, stores that receipt, and returns the same structured receipt to the caller.

**Why this priority**: This is the only capability that turns a receipt photo into a retained expense record. Without it, the service does not deliver expense tracking.

**Independent Test**: Submit one clear photo of a complete Polish fiscal receipt, and submit two to five ordered photos of one complete receipt. Each response is the stored receipt, including seller, receipt identifiers, at least one line item, at least one tax summary entry, totals, and at least one payment.

**Acceptance Scenarios**:

1. **Given** an authorized caller and one readable photo of one complete Polish fiscal receipt, **When** the caller submits that photo, **Then** the service stores one receipt and returns that stored receipt.
2. **Given** an authorized caller and two to five ordered photos of one complete Polish fiscal receipt, **When** the caller submits those photos, **Then** the service stores one receipt and returns that stored receipt.
3. **Given** a stored receipt returned to the caller, **When** the caller inspects it, **Then** it contains the transcribed seller, receipt header, at least one line item, at least one tax summary entry, totals, and at least one payment, and it matches what was retained.
4. **Given** a receipt photo set whose transcription includes optional details such as addresses, fiscal device data, or lines that could not be placed in a field, **When** all required receipt parts are present, **Then** the service stores the receipt and includes those optional details in the returned receipt.

---

### User Story 2 - Reject a photo that cannot become a receipt (Priority: P1)

An authorized caller submits a photo that cannot be turned into a complete receipt. The caller receives a fixed reason and a short explanation, and no receipt is stored.

**Why this priority**: A failed transcription must never create an expense record. Storing a partial or invented receipt would corrupt the expense history.

**Independent Test**: Submit an unreadable image, a receipt missing a required part or tax summary entry, a transcription that does not finish, and a save that fails. The reasons are "unreadable", "incomplete", "transcription unavailable", and "storage failed", and no receipt remains stored.

**Acceptance Scenarios**:

1. **Given** an authorized caller and a photo that is unreadable, blank, or not a receipt, **When** the caller submits that photo, **Then** the service returns the reason "unreadable" and a short explanation, and no receipt is stored.
2. **Given** an authorized caller and a photo of a receipt that is missing one or more required parts, has no tax summary entry, or has a malformed required value, **When** the caller submits that photo, **Then** the service returns the reason "incomplete" and a short explanation, and no receipt is stored.
3. **Given** transcription is unavailable or does not finish in time, **When** the caller submits a photo, **Then** the service returns the reason "transcription unavailable" and a short explanation, and no receipt is stored.
4. **Given** a complete transcription and a failure while saving it, **When** the save does not finish, **Then** the service returns the reason "storage failed" and a short explanation, and no receipt remains stored.

---

### User Story 3 - Reject an invalid or unauthorized submission (Priority: P2)

A caller submits something the service should not attempt to transcribe, or a caller who is not authorized attempts to submit a photo. The service refuses the submission before creating a receipt.

**Why this priority**: Invalid files and unauthorized use must be stopped, but the product value still depends on the successful transcription path above.

**Independent Test**: Submit an empty file, an unsupported file, an oversized image, or a request from an unauthorized caller, and confirm each is refused with the matching reason and that no receipt is stored.

**Acceptance Scenarios**:

1. **Given** an authorized caller and no image, an empty image, or a file that is not an accepted image, **When** the caller submits it, **Then** the service returns the reason "invalid submission" and a short explanation, does not attempt transcription, and does not store a receipt.
2. **Given** an authorized caller and an image larger than the accepted size, **When** the caller submits it, **Then** the service returns the reason "invalid submission" and a short explanation, does not attempt transcription, and does not store a receipt.
3. **Given** a caller who is not authorized, **When** that caller submits a receipt photo, **Then** the service returns the reason "not authorized" and a short explanation, does not attempt transcription, and does not store a receipt.
4. **Given** an authorized caller and six or more images, **When** the caller submits them, **Then** the service returns the reason "invalid submission" and a short explanation, does not attempt transcription, and does not store a receipt.

### Edge Cases

- A photo contains more than one receipt. The caller receives the reason "unreadable" and nothing is stored, because one submission captures exactly one receipt.
- One image in a two-to-five image submission is blank or unreadable. The caller receives the reason "unreadable" and nothing is stored.
- A photo is a receipt from outside the supported Polish fiscal receipt format. The caller receives the reason "unreadable" and nothing is stored.
- Line totals, tax amounts, and the amount due do not reconcile. If all required parts are present and amounts are valid currency amounts, the receipt is stored as transcribed. Correcting arithmetic is outside this capability.
- The same photo is submitted twice. Each successful submission stores a separate receipt. Detecting duplicates is outside this capability.
- The photo file name is missing. Transcription and storage still proceed; the file name is omitted.
- Optional fields that are absent on the paper receipt are omitted rather than filled with placeholders.
- Amounts that are not valid currency amounts, or a seller tax identifier that is not a 10-digit Polish tax identifier, produce the reason "incomplete". Nothing is stored.
- A submission contains six or more images. The caller receives the reason "invalid submission" and nothing is stored.
- An image has no declared media type. It is accepted and treated as a JPEG. A declared type other than JPEG, PNG, or WebP is "invalid submission".
- The tax summary is missing or has no entries. The caller receives the reason "incomplete" and nothing is stored.
- Saving a valid transcription fails. The caller receives the reason "storage failed", and no partial receipt remains stored.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The service MUST accept one to five receipt photos of one receipt from an authorized caller and return the stored structured receipt when transcription succeeds.
- **FR-002**: The service MUST transcribe the photos, in submission order, into one Polish fiscal receipt before anything is stored.
- **FR-003**: The service MUST store a receipt only after transcription produces all required parts: seller trade name and tax identifier, receipt number, issue date and time, currency, at least one line item, at least one tax summary entry, totals, and at least one payment.
- **FR-004**: The service MUST return the same receipt that was stored, including any optional details that were transcribed.
- **FR-005**: Each stored receipt MUST have a unique identifier assigned by the service.
- **FR-006**: The service MUST retain the stored receipt after the submission completes. The original photo MUST NOT be retained.
- **FR-007**: When any submitted photo is blank or unreadable, or the photos are not exactly one Polish fiscal receipt, the service MUST return the reason "unreadable" and a short explanation, and MUST NOT store a receipt.
- **FR-008**: When a required part is missing or a required value is malformed, the service MUST return the reason "incomplete" and a short explanation, and MUST NOT store a receipt.
- **FR-009**: When transcription is unavailable or does not complete within the allowed time, the service MUST return the reason "transcription unavailable" and a short explanation, and MUST NOT store a receipt.
- **FR-010**: The service MUST refuse a submission that has no image, an empty image, a declared type other than JPEG, PNG, or WebP, an image above 10 MB, or more than five images. The reason MUST be "invalid submission". The service MUST NOT attempt transcription and MUST NOT store a receipt.
- **FR-011**: The service MUST accept JPEG, PNG, and WebP images. An image with no declared media type MUST be accepted and treated as JPEG. Each image MUST be at most 10 MB.
- **FR-012**: Callers who are not authorized MUST be refused with the reason "not authorized" and a short explanation. The service MUST NOT attempt transcription, and no receipt MUST be stored.
- **FR-013**: Every failure MUST include exactly one fixed reason and a short explanation of why the receipt was not stored. The reason MUST be one of: invalid submission, not authorized, unreadable, incomplete, transcription unavailable, or storage failed.
- **FR-014**: A stored line item MUST include description, quantity greater than zero, unit price, line total, and tax category.
- **FR-015**: A stored payment MUST include a payment method and an amount. Allowed methods are cash, card, transfer, voucher, mobile, and other.
- **FR-016**: Currency amounts MUST be expressed as values with two fractional digits. Currency MUST be a three-letter currency code.
- **FR-017**: The seller tax identifier MUST be a 10-digit Polish tax identifier when the receipt is stored.
- **FR-018**: When a valid transcription cannot be saved, the service MUST return the reason "storage failed" and MUST leave no stored receipt.

### Key Entities

- **Receipt**: One stored Polish fiscal receipt. It has a service-assigned identifier, a seller, receipt header, one or more line items, a tax summary, totals, and one or more payments. It may also include source file name, raw transcribed text, addresses, fiscal device details, and lines that could not be placed in a field.
- **Seller**: The business on the receipt, identified by trade name and Polish tax identifier. It may include a legal name, BDO number, business address, and registered address.
- **Receipt header**: Receipt number, date and time of issue, and currency. It may include an order number.
- **Line item**: A product, service, packaging charge, or other charge, with description, quantity, unit price, line total, and tax category. Unit of measure is optional.
- **Tax summary entry**: One tax category with its rate, taxable sales, and tax amount. A stored receipt has at least one entry.
- **Totals**: Total tax, gross amount, and amount due.
- **Payment**: How the receipt was paid, with method and amount. A transaction identifier is optional.
- **Caller**: Any party authorized to use the service. Authorization allows or refuses a submission. A receipt does not record which caller submitted it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An authorized caller can turn one to five clear photos of one complete receipt into one stored receipt in a single submission, without typing receipt fields.
- **SC-002**: The caller receives either the stored receipt or a fixed failure reason with a short explanation within 60 seconds of submitting one to five photos.
- **SC-003**: 100% of refused or failed submissions leave no stored receipt.
- **SC-004**: 100% of submissions from unauthorized callers are refused and leave no stored receipt.
- **SC-005**: Every returned receipt includes seller, receipt header, at least one line item, at least one tax summary entry, totals, and at least one payment.

## Assumptions

- This capability covers only creating one receipt from one to five photos of that receipt. Listing, viewing, editing, deleting, categorizing, and exporting expenses are separate capabilities.
- Supported documents are Polish fiscal receipts. Invoices, foreign receipts, and multi-receipt photos are refused.
- Transcription is performed by an external language-model capability. The provider is not chosen by this specification. The service depends on that capability being available.
- The original photo is used only to produce the transcription and is then discarded. Only the structured receipt is retained.
- Callers must already be authorized. How a caller signs in is outside this capability. This submission capability is not available to anonymous callers.
- Accepted declared image types are JPEG, PNG, and WebP. An image with no declared type is treated as JPEG. Each image is at most 10 MB. A submission contains one to five images.
- Transcription is allowed up to 60 seconds. If it does not finish, the submission fails and nothing is stored.
- Duplicate receipts are not detected. Submitting the same photo again stores another receipt.
- Arithmetic reconciliation between line items, tax, and totals is not required for acceptance.
- Acceptance of this capability is the scenario tests in this specification. A percentage accuracy target is not a release gate.
- All stored receipts belong to one shared ledger. A receipt does not record who submitted it. Separate personal ledgers and sharing are outside this capability.
- No screen or page is part of this capability. A separate client submits the photo and receives the receipt or the failure explanation.
