# Quickstart: Receipt Discounts

Validates the discount fields added to transcription and storage. Prerequisites and the base receipt flow are in [the create-receipt quickstart](../001-create-receipt/quickstart.md).

## What changed

- A line item may include `discount` with `description` and a negative `total`.
- A receipt may include `discountSummary` with `description` and a negative `total`.
- Schema: `.external-resources/receipt-schema.json` and `src/main/resources/schemas/receipt-schema.json`.
- Prompt: `src/main/resources/prompts/receipt-transcription-system.st`.

The sample receipt has item "ChipsyLay soveB110g" with discount description "OPUST" and total `-8.69`, plus discount summary description "OPUSTY ŁĄCZNIE" and total `-8.69`. The drink line and the bottle deposit have no discount.

## Automated checks

From the repository root, with `JAVA_HOME` set to `C:\tools\jdk-23.0.2`:

```text
mvn test
```

Expected:

- A receipt with no discounts still stores.
- An item discount or discount summary with a blank description, zero, or a positive amount is `incomplete` and is not saved.
- A discounted receipt round-trips through MariaDB: the item keeps its line total, the discount total is negative, and the discount summary is a separate row.
- `POST /api/receipt` accepts and returns `items[0].discount` and `discountSummary`.
- The transcription prompt and the JSON schema sent to the model both mention the discount summary.

Docker is required for the MariaDB persistence test. Other tests do not call OpenRouter.

## Manual transcription

Submit the sample receipt photo to `POST /api/transcription` with header `Authorization: Bearer <API_KEY>`.

Expected in the JSON body:

- The chips item has `discount.description` "OPUST" and `discount.total` "-8.69", and it is not a second item.
- `discountSummary.description` is "OPUSTY ŁĄCZNIE" and `discountSummary.total` is "-8.69".
- "OPUSTY ŁĄCZNIE" is not an entry in `items`.
- The bottle deposit remains an item with `itemType` "packaging".

Store that body with `POST /api/receipt`. The response repeats those discount fields and adds `id`.
