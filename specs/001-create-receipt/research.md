# Research: Create Receipt from Photo

## Spring Boot and Java

**Decision**: Spring Boot 4.1.1, Java 23, compiler `--release 23`, JDK `C:\tools\jdk-23.0.2`.

**Rationale**: 4.1.1 is the latest stable Spring Boot release as of 2026-09-26 (20 August 2026). Spring Boot 4.1 requires Java 17 or newer and is compatible through Java 26, so JDK 23 is a supported runtime. Spring Boot 4.2.0-M1 is a milestone and is not used.

**Alternatives considered**: Spring Boot 4.0.8 is stable but older. Spring Boot 3.5.x would force an older Spring AI line.

## Spring AI and OpenRouter

**Decision**: Spring AI 2.0.1 with `spring-ai-starter-model-openai`. Base URL `https://openrouter.ai/api/v1`. API key from `OPENROUTER_API_KEY` mapped to `spring.ai.openai.api-key`. Default model `google/gemini-2.5-flash`, property `app.transcription.model`.

**Rationale**: Spring AI 2.0.1 is the latest stable release and already tracks Spring Boot 4.1. OpenRouter is OpenAI-compatible. Spring AI 2.0 chat calls succeed against `https://openrouter.ai/api/v1`. The OpenAI starter is the supported way to reach that API without a custom HTTP client. The model id stays in application configuration so it can change without a code edit. Spring relaxed binding also allows an environment override of that property.

**Alternatives considered**: A hand-written OpenRouter client would bypass Spring AI annotations and `ChatClient`. The native Gemini SDK would ignore the OpenRouter requirement. Spring AI 1.1.x targets the Spring Boot 3 line.

## Transcription call

**Decision**: `ReceiptTranscriptionService` builds one `ChatClient` user turn that contains a short instruction plus one `Media` part per image, in upload order. The system text comes from `src/main/resources/prompts/receipt-transcription-system.st`. The response is constrained with `useProviderStructuredOutput()` using the JSON Schema document `.external-resources/receipt-schema.json`, copied onto the classpath as `schemas/receipt-schema.json` for runtime. The service deserializes into a receipt draft that matches that schema. Local schema validation runs before anything is saved. Automatic multi-attempt schema repair is not enabled, so retries cannot consume the 60 second budget silently.

**Rationale**: The schema file, not a generated guess, is the response contract. Provider-side structured output asks OpenRouter to enforce it. A local check still guards incomplete JSON. The draft type has no database id; the id is assigned on insert.

**Alternatives considered**: Prompt-only JSON mode is the fallback if this model rejects `response_format` json_schema through OpenRouter. In that fallback the same schema text is included in the prompt and the local validator still decides. Three silent retries were rejected because a late failure must surface as `incomplete` or `unreadable` inside 60 seconds.

Images are sent as base64-encoded data inside the OpenAI-compatible image part that Spring AI produces from `Media`. The phrase "base64 decoded" is treated as base64-encoded bytes. Raw decoded bytes are not placed in the JSON chat body.

Each part is an image. The media type is taken from the upload when it is `image/jpeg`, `image/png`, or `image/webp`. When the upload has no media type, the part is sent as `image/jpeg`. The OpenAI-compatible wire type for that part is `image_url` with a `data:` URL. Spring AI owns that mapping. The service does not invent a custom part type named `image`, because OpenRouter's OpenAI-compatible API expects `image_url`.

## Several images, one receipt

**Decision**: `POST /api/receipt` accepts multipart field `images`, from one to five file parts, each at most 10 MB. Parts are pages of one Polish fiscal receipt, in order. Zero parts, an empty part, a non-image declared type, a part over 10 MB, or a sixth image is `invalid submission` and the model is not called. A part with no media type is sent as `image/jpeg`.

**Rationale**: A long receipt may need more than one photo, and the accepted limit is five. If the transcription cannot represent the upload as exactly one fiscal receipt, the reason is `unreadable` and nothing is stored.

**Alternatives considered**: Stitching images into one JPEG before the call loses page boundaries and adds an image library. Rejecting every multi-image request contradicts the planning input.

## System prompt file

**Decision**: The system prompt is only the template file `prompts/receipt-transcription-system.st`, loaded with Spring's resource abstraction and passed as the `ChatClient` system text. It is not hardcoded in Java.

The template must tell the model to:

- Treat the images as ordered pages of one Polish fiscal receipt.
- Set `documentType` to `fiscal_receipt`.
- Copy only what is visible. Leave optional fields out instead of inventing them.
- Put unrecognized lines into `unparsedLines`.
- Use two-fraction money strings, a 10-digit seller tax id, and a three-letter currency code.
- Use only the payment methods and item types allowed by the schema.
- Return one object. Do not return a second receipt.

**Rationale**: The prompt is required configuration, and the rules above are what make `incomplete` and `unreadable` distinguishable from a successful save.

**Alternatives considered**: A prompt embedded in Java cannot be changed without a rebuild.

## Persistence

**Decision**: MariaDB, Spring Data JPA, Liquibase formatted SQL at `src/main/resources/db/changelog/db.changelog-master.sql`. Liquibase runs automatically on startup before JPA validation. `spring.jpa.hibernate.ddl-auto=validate`. Money is `DECIMAL(14,2)` in the database and serialized back to a two-fraction string in the API. Child rows use their own autoincrement primary keys and foreign keys to the parent. Every table has `created_at` and `updated_at`, set to the same timestamp on insert.

**Rationale**: Liquibase SQL is the constitution rule. JPA maps the aggregate without becoming the schema owner. Decimal columns avoid binary floating point. Separate child tables match the nested schema and still give every table its own key and timestamps. This feature never updates a receipt, so `updated_at` stays equal to `created_at` after insert.

**Alternatives considered**: Hibernate `ddl-auto=update` would bypass Liquibase. Storing money as `VARCHAR` would match the schema pattern literally and weaken numeric queries. One wide table would bury repeating line items.

Photos are not inserted. Original file names, when present, are joined in order into `source.fileName`. Missing names are skipped. `source.rawText` stores only text the model returned.

## Logging

**Decision**: Logback writes application logs to `logs/cheapskountant-service.log`. A `ChatClient` advisor logs every model call: system prompt, tool definitions (empty for this endpoint), user instruction text, image count, media type, and byte size, then the model response text. If a tool call occurs, the advisor logs the tool name, parameters, and result. API keys, database passwords, and image bytes or base64 are never logged.

**Rationale**: The operational request is to see prompts, tool definitions, and responses. Writing the photo into the log would retain it, which the spec and constitution forbid. This endpoint registers no tools; the advisor still records an empty tool list and any unexpected tool call.

**Alternatives considered**: `DEBUG` logging on the OpenAI client often prints the raw body, including base64 and sometimes headers. That is rejected.

## Authorization

**Decision**: `/api/receipt` requires header `Authorization: Bearer <API_KEY>`, where `API_KEY` is an environment variable. A missing or wrong key returns `not authorized` and does not call the model or the database. There is no user table. `/actuator/health` is public.

**Rationale**: The constitution requires authentication unless an endpoint is explicitly public. The spec refuses anonymous receipt creation and does not define accounts. A shared key matches the single shared ledger.

**Alternatives considered**: Leaving the endpoint open fails the constitution. A user-account design contradicts the shared-ledger clarification.

## Failure mapping

**Decision**:

| Condition | Reason | HTTP |
|-----------|--------|------|
| No image, empty image, unsupported declared type, image over 10 MB, more than five images | invalid submission | 400 |
| Missing or wrong API key | not authorized | 401 |
| Blank, unreadable, not one Polish fiscal receipt, or a response that is not the receipt object | unreadable | 422 |
| Receipt object missing required parts, including a tax summary with no entries, or containing a malformed required value | incomplete | 422 |
| OpenRouter error or the 60 second budget expires | transcription unavailable | 503, or 504 on timeout |
| Database rollback after a valid transcription | storage failed | 500 |

Validation and authorization happen before the model call. A failure does not insert rows.

**Rationale**: The six spec reasons stay stable for clients. Storage failure is separate so an outage is not reported as a bad photo. HTTP codes distinguish bad input, refusal, and dependency failure.

**Alternatives considered**: Mapping storage failure to `transcription unavailable` would be false. Using only HTTP 200 with an error body would hide the failure from generic clients.

## Container runtime

**Decision**: Run the service from `Dockerfile` based on `eclipse-temurin:23.0.2_7-jre`. The jar name is `target/cheapskountant-service.jar`. The process listens on port 8080, runs as a non-root user, writes logs under `/app/logs`, and uses `GET /actuator/health` as the container health check. Credentials stay in the container environment: `OPENROUTER_API_KEY`, `DB_CONNECTION`, `DB_USERNAME`, `DB_PASSWORD`, and `API_KEY`.

**Rationale**: Eclipse Temurin is the official OpenJDK image family, and its JRE tag is the standard runtime base. `23.0.2_7` matches the local JDK 23.0.2. The default Ubuntu JRE variant is the one to use when a smaller musl image is not required. Curl is installed only so the health check can call the public health endpoint.

**Alternatives considered**: `eclipse-temurin:23-jre-alpine` is smaller and uses musl. A custom `jlink` runtime would shrink the image further and would add a build stage this service does not need. Baking secrets into the image was rejected.

## What this release does not decide

Listing, editing, deleting, arithmetic reconciliation, duplicate detection, and a statistical accuracy sample stay out of scope. The language-model provider is OpenRouter; the spec left the provider unnamed, and this plan names it.
