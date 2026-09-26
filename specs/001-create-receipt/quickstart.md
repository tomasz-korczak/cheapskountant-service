# Quickstart: Create Receipt from Photo

Validates the create-receipt flow described in [plan.md](./plan.md). Contracts and tables are not copied here.

- HTTP contract: [contracts/create-receipt.openapi.yaml](./contracts/create-receipt.openapi.yaml)
- Tables and validation: [data-model.md](./data-model.md)
- Receipt field schema: `.external-resources/receipt-schema.json`

## Prerequisites

- JDK 23 at `C:\tools\jdk-23.0.2`
- Maven 3.6.3 or newer
- A MariaDB database the configured user can migrate
- An OpenRouter API key

Set the JDK for the shell:

```powershell
$env:JAVA_HOME = "C:\tools\jdk-23.0.2"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
```

## Configuration

The process reads these environment variables. Do not put the secret values in source or in this guide.

| Variable | Purpose |
|----------|---------|
| `OPENROUTER_API_KEY` | OpenRouter credential |
| `DB_CONNECTION` | MariaDB JDBC URL, for example `jdbc:mariadb://localhost:3306/cheapskountant` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `API_KEY` | Bearer token required by `POST /api/receipt` |

`app.transcription.model` defaults to `google/gemini-2.5-flash`. Change it in `application.yaml` to point transcription at another OpenRouter model. The system prompt file is `src/main/resources/prompts/receipt-transcription-system.st`.

## Automated checks

From the repository root, with a running MariaDB available to Testcontainers or the configured test database:

```powershell
mvn test
```

Expected coverage:

- Submitting no image, an empty image, a non-image type, or a file over 10 MB returns `invalid submission` and does not call the model.
- A missing or wrong bearer token returns `not authorized` and does not call the model.
- An unreadable upload or a response that is not one fiscal receipt returns `unreadable` and leaves no row.
- A receipt missing a required part, or with a malformed tax id or money value, returns `incomplete` and leaves no row.
- A provider failure returns `transcription unavailable` with HTTP 503. A call that exceeds 60 seconds returns the same reason with HTTP 504.
- A valid transcription that the database rejects returns `storage failed`, and the transaction leaves no partial rows.
- A valid one-image submission and a valid multi-image submission each return HTTP 201 with `id` and the required receipt groups.
- The log file `logs/cheapskountant-service.log` contains the system prompt, an empty tool-definition list, the user instruction, image count, media type, and size, and the model response. It does not contain the API key, the database password, or image bytes.

Default `mvn test` uses a fake transcription client. It does not spend OpenRouter credit.

## Manual end-to-end check

Start MariaDB, export the four connection and credential variables plus `OPENROUTER_API_KEY` and `API_KEY`, then build the jar and image:

```powershell
mvn -DskipTests package
docker build -t cheapskountant-service .
docker run --rm -p 8080:8080 `
  -e OPENROUTER_API_KEY `
  -e DB_CONNECTION `
  -e DB_USERNAME `
  -e DB_PASSWORD `
  -e API_KEY `
  cheapskountant-service
```

When MariaDB runs on the host, `DB_CONNECTION` must use a host name the container can reach, such as `host.docker.internal`. The image runs as a non-root user on Eclipse Temurin JRE 23.0.2. `docker inspect` should show a healthy container after startup.

Startup must apply `db/changelog/db.changelog-master.sql` before the server accepts receipt requests. `GET /actuator/health` needs no token and returns success.

Submit one JPEG page:

```powershell
curl.exe -sS -X POST "http://localhost:8080/api/receipt" `
  -H "Authorization: Bearer $env:API_KEY" `
  -F "images=@.external-resources/mtciung-receipt.jpg;type=image/jpeg"
```

A clear complete receipt returns HTTP 201 and a body with `id`, seller, receipt header, at least one item, tax summary, totals, and at least one payment. Query MariaDB and confirm one root `receipt` row, the required child rows, equal `created_at` and `updated_at`, and no image blob.

Submit the same photo again. A second receipt is stored.

For a long receipt, repeat the `images` field in page order, up to 5 parts. The response is still one receipt. A sixth part is `invalid submission`. An image part with no media type is accepted as JPEG.

Submit the same request without the `Authorization` header. The response reason is `not authorized`, and the receipt row count does not change.

Stop the process and confirm `logs/cheapskountant-service.log` has the model exchange and no image payload.
