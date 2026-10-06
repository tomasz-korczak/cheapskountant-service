# Feature Specification: Create Receipt from Photo

**Feature Branch**: `001-create-receipt`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Expense-tracking service. Transcription and storage are separate. `POST /api/transcription` accepts receipt photos, transcribes them into a structured receipt, and returns that object without storing it. `POST /api/receipt` accepts that structured receipt and only persists it. When transcription or storage does not succeed, the caller receives an error and nothing is stored."

## Clarifications

### Session 2026-09-26

- Q: Should each stored receipt belong to the person who submitted the photo, or should every authorized caller add receipts to one shared ledger? → A: One shared ledger. Any authorized caller adds receipts to the same collection, and the receipt does not record who submitted it.
- Q: When a submission does not store a receipt, should the caller receive a fixed reason as well as a short explanation? → A: Fixed reason plus a short explanation. The reasons are invalid submission, not authorized, unreadable, incomplete, and transcription unavailable.
- Q: Should the first release be accepted only when 90% of a fixed set of real receipt photos succeed, or is that target checked later? → A: Remove the 90% target. Acceptance is only the scenario tests in this specification.
- Q: How many images may one receipt submission contain? → A: One to five images, in order, of one receipt.
- Q: Should a database failure after a valid transcription be its own failure reason? → A: Add `storage failed`. Nothing remains stored.
- Q: What should happen when an uploaded image has no declared media type? → A: Accept it and treat it as JPEG.
- Q: Is a receipt incomplete when the tax summary has no entries? → A: At least one tax summary entry is required.

### Session 2026-10-06

- Q: Should one request both transcribe the photos and store the receipt? → A: No. `POST /api/transcription` accepts one to five images and returns the structured receipt without an id and without storing it. `POST /api/receipt` accepts that same object as JSON and only persists it, assigning the id.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Transcribe receipt photos (Priority: P1)

An authorized caller submits one to five photos of one Polish fiscal receipt, in page order, to `POST /api/transcription`. The service transcribes those photos into one structured receipt and returns that receipt. Nothing is stored, and the response has no id.

**Why this priority**: Transcription is what turns a photo into a receipt the caller can keep. Storage is a separate step.

**Independent Test**: Submit one clear photo of a complete Polish fiscal receipt, and submit two to five ordered photos of one complete receipt. Each response is the transcribed receipt, including seller, receipt identifiers, at least one line item, at least one tax summary entry, totals, and at least one payment. No receipt row is created.

**Acceptance Scenarios**:

1. **Given** an authorized caller and one readable photo of one complete Polish fiscal receipt, **When** the caller submits that photo to transcription, **Then** the service returns that structured receipt and stores nothing.
2. **Given** an authorized caller and two to five ordered photos of one complete Polish fiscal receipt, **When** the caller submits those photos to transcription, **Then** the service returns one structured receipt and stores nothing.
3. **Given** a transcription response, **When** the caller inspects it, **Then** it contains the transcribed seller, receipt header, at least one line item, at least one tax summary entry, totals, and at least one payment, and it has no id.
4. **Given** a receipt photo set whose transcription includes optional details such as addresses, fiscal device data, or lines that could not be placed in a field, **When** all required receipt parts are present, **Then** the response includes those optional details and stores nothing.

---

### User Story 2 - Store a structured receipt (Priority: P1)

An authorized caller sends a structured receipt to `POST /api/receipt`. The service stores that receipt and returns the same receipt with an id assigned by the service. This endpoint does not accept photos and does not call transcription.

**Why this priority**: A transcribed receipt becomes an expense record only when it is stored.

**Independent Test**: Submit the JSON body returned by transcription. The response is HTTP 201 with the same receipt fields plus an id. A second identical submission stores a separate receipt.

**Acceptance Scenarios**:

1. **Given** an authorized caller and a complete structured receipt, **When** the caller submits that object, **Then** the service stores one receipt and returns it with an id.
2. **Given** a stored receipt returned to the caller, **When** the caller inspects it, **Then** it matches the submitted seller, receipt header, line items, tax summary, totals, payments, and optional details, plus the assigned id.
3. **Given** a complete structured receipt and a failure while saving it, **When** the save does not finish, **Then** the service returns the reason "storage failed" and a short explanation, and no receipt remains stored.
4. **Given** an authorized caller and a structured receipt that is missing a required part, has no tax summary entry, or has a malformed required value, **When** the caller submits that object, **Then** the service returns the reason "incomplete" and a short explanation, and no receipt is stored.

---

### User Story 3 - Reject a photo that cannot become a receipt (Priority: P1)

An authorized caller submits a photo that cannot be turned into a complete receipt. Transcription returns a fixed reason and a short explanation, and no receipt is stored.

**Why this priority**: A failed transcription must not be presented as a receipt the caller can store.

**Independent Test**: Submit an unreadable image, a receipt missing a required part or tax summary entry, and a transcription that does not finish. The reasons are "unreadable", "incomplete", and "transcription unavailable", and no receipt is stored.

**Acceptance Scenarios**:

1. **Given** an authorized caller and a photo that is unreadable, blank, or not a receipt, **When** the caller submits that photo to transcription, **Then** the service returns the reason "unreadable" and a short explanation, and no receipt is stored.
2. **Given** an authorized caller and a photo of a receipt that is missing one or more required parts, has no tax summary entry, or has a malformed required value, **When** the caller submits that photo to transcription, **Then** the service returns the reason "incomplete" and a short explanation, and no receipt is stored.
3. **Given** transcription is unavailable or does not finish in time, **When** the caller submits a photo, **Then** the service returns the reason "transcription unavailable" and a short explanation, and no receipt is stored.

---

### User Story 4 - Reject an invalid or unauthorized submission (Priority: P2)

A caller submits something transcription should not attempt, sends a receipt body that cannot be read, or calls either endpoint without authorization. The service refuses the request before creating a receipt.

**Why this priority**: Invalid files, unreadable JSON, and unauthorized use must be stopped before any receipt is stored.

**Independent Test**: Submit an empty file, an unsupported file, an oversized image, a receipt request without a JSON body, or a request from an unauthorized caller. Each is refused with the matching reason and no receipt is stored.

**Acceptance Scenarios**:

1. **Given** an authorized caller and no image, an empty image, or a file that is not an accepted image, **When** the caller submits it to transcription, **Then** the service returns the reason "invalid submission" and a short explanation, does not attempt transcription, and does not store a receipt.
2. **Given** an authorized caller and an image larger than the accepted size, **When** the caller submits it to transcription, **Then** the service returns the reason "invalid submission" and a short explanation, does not attempt transcription, and does not store a receipt.
3. **Given** a caller who is not authorized, **When** that caller calls transcription or receipt storage, **Then** the service returns the reason "not authorized" and a short explanation, does not attempt transcription, and does not store a receipt.
4. **Given** an authorized caller and six or more images, **When** the caller submits them to transcription, **Then** the service returns the reason "invalid submission" and a short explanation, does not attempt transcription, and does not store a receipt.
5. **Given** an authorized caller and a receipt request with no JSON body, **When** the caller submits it, **Then** the service returns the reason "invalid submission" and a short explanation, and does not store a receipt.

### Edge Cases

- A photo contains more than one receipt. Transcription returns the reason "unreadable" and nothing is stored, because one submission captures exactly one receipt.
- One image in a two-to-five image submission is blank or unreadable. Transcription returns the reason "unreadable" and nothing is stored.
- A photo is a receipt from outside the supported Polish fiscal receipt format. Transcription returns the reason "unreadable" and nothing is stored.
- Line totals, tax amounts, and the amount due do not reconcile. If all required parts are present and amounts are valid currency amounts, transcription returns the receipt and storage keeps it as submitted. Correcting arithmetic is outside this capability.
- The same structured receipt is stored twice. Each successful store creates a separate receipt. Detecting duplicates is outside this capability.
- The photo file name is missing. Transcription still returns the receipt; the file name is omitted. Storage keeps the file name only when the submitted object includes one.
- Optional fields that are absent on the paper receipt are omitted rather than filled with placeholders.
- Amounts that are not valid currency amounts, or a seller tax identifier that is not a 10-digit Polish tax identifier, produce the reason "incomplete". Nothing is stored.
- A transcription submission contains six or more images. The caller receives the reason "invalid submission" and nothing is stored.
- An image has no declared media type. Transcription accepts it and treats it as a JPEG. A declared type other than JPEG, PNG, or WebP is "invalid submission".
- The tax summary is missing or has no entries. The caller receives the reason "incomplete" and nothing is stored.
- Saving a valid structured receipt fails. The caller receives the reason "storage failed", and no partial receipt remains stored.
- An id sent to receipt storage is ignored. The service assigns the stored id.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `POST /api/transcription` MUST accept one to five receipt photos of one receipt from an authorized caller and return the structured receipt when transcription succeeds. It MUST NOT store a receipt.
- **FR-002**: Transcription MUST read the photos, in submission order, into one Polish fiscal receipt. The returned object MUST omit the id.
- **FR-003**: `POST /api/receipt` MUST accept a structured receipt as JSON and MUST store it only when all required parts are present: seller trade name and tax identifier, receipt number, issue date and time, currency, at least one line item, at least one tax summary entry, totals, and at least one payment. This endpoint MUST NOT accept photos and MUST NOT call transcription.
- **FR-004**: Receipt storage MUST return the same receipt that was stored, including any optional details from the submitted object, plus the assigned id.
- **FR-005**: Each stored receipt MUST have a unique identifier assigned by the service. An id in the storage request MUST be ignored.
- **FR-006**: The service MUST retain the stored receipt after storage completes. The original photo MUST NOT be retained. Transcription MUST return the structured receipt only.
- **FR-007**: When any submitted photo is blank or unreadable, or the photos are not exactly one Polish fiscal receipt, transcription MUST return the reason "unreadable" and a short explanation, and MUST NOT store a receipt.
- **FR-008**: When a required part is missing or a required value is malformed, transcription and receipt storage MUST each return the reason "incomplete" and a short explanation, and MUST NOT store a receipt.
- **FR-009**: When transcription is unavailable or does not complete within the allowed time, transcription MUST return the reason "transcription unavailable" and a short explanation, and MUST NOT store a receipt.
- **FR-010**: Transcription MUST refuse a submission that has no image, an empty image, a declared type other than JPEG, PNG, or WebP, an image above 10 MB, or more than five images. The reason MUST be "invalid submission". Transcription MUST NOT be attempted and a receipt MUST NOT be stored. Receipt storage MUST refuse a request that has no JSON receipt body with the reason "invalid submission".
- **FR-011**: Transcription MUST accept JPEG, PNG, and WebP images. An image with no declared media type MUST be accepted and treated as JPEG. Each image MUST be at most 10 MB.
- **FR-012**: Callers who are not authorized MUST be refused on both endpoints with the reason "not authorized" and a short explanation. Transcription MUST NOT be attempted, and no receipt MUST be stored.
- **FR-013**: Every failure MUST include exactly one fixed reason and a short explanation. The reason MUST be one of: invalid submission, not authorized, unreadable, incomplete, transcription unavailable, or storage failed.
- **FR-014**: A stored line item MUST include description, quantity greater than zero, unit price, line total, and tax category.
- **FR-015**: A stored payment MUST include a payment method and an amount. Allowed methods are cash, card, transfer, voucher, mobile, and other.
- **FR-016**: Currency amounts MUST be expressed as values with two fractional digits. Currency MUST be a three-letter currency code.
- **FR-017**: The seller tax identifier MUST be a 10-digit Polish tax identifier when the receipt is stored or returned from transcription.
- **FR-018**: When a valid structured receipt cannot be saved, receipt storage MUST return the reason "storage failed" and MUST leave no stored receipt.

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

- **SC-001**: An authorized caller can turn one to five clear photos of one complete receipt into one structured receipt in a single transcription request, without typing receipt fields, and can store that object in a separate request.
- **SC-002**: The caller receives either the transcribed receipt or a fixed failure reason with a short explanation within 60 seconds of submitting one to five photos. Storage returns the stored receipt or a fixed failure reason without calling transcription.
- **SC-003**: 100% of refused or failed transcription and storage requests leave no stored receipt. A successful transcription also leaves no stored receipt.
- **SC-004**: 100% of requests from unauthorized callers are refused on both endpoints and leave no stored receipt.
- **SC-005**: Every transcribed or stored receipt includes seller, receipt header, at least one line item, at least one tax summary entry, totals, and at least one payment. Only the stored receipt includes an id.

## Assumptions

- This capability covers transcribing one receipt from one to five photos, and storing one structured receipt. The caller performs those as two requests. Listing, viewing, editing, deleting, categorizing, and exporting expenses are separate capabilities.
- Supported documents are Polish fiscal receipts. Invoices, foreign receipts, and multi-receipt photos are refused.
- Transcription is performed by an external language-model capability. The provider is not chosen by this specification. The service depends on that capability being available.
- The original photo is used only to produce the transcription and is then discarded. Only a receipt submitted to storage is retained.
- Callers must already be authorized. How a caller signs in is outside this capability. This submission capability is not available to anonymous callers.
- Accepted declared image types are JPEG, PNG, and WebP. An image with no declared type is treated as JPEG. Each image is at most 10 MB. A submission contains one to five images.
- Transcription is allowed up to 60 seconds. If it does not finish, transcription fails and nothing is stored.
- Duplicate receipts are not detected. Storing the same structured receipt again stores another receipt.
- Arithmetic reconciliation between line items, tax, and totals is not required for acceptance.
- Acceptance of this capability is the scenario tests in this specification. A percentage accuracy target is not a release gate.
- All stored receipts belong to one shared ledger. A receipt does not record who submitted it. Separate personal ledgers and sharing are outside this capability.
- No screen or page is part of this capability. A separate client submits the photo to transcription, then submits the returned object to storage, and receives either the receipt or the failure explanation.
