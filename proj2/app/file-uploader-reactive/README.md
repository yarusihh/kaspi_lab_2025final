# file-uploader-reactive (skeleton)

Reactive Spring Boot skeleton for project 2.

## Goals covered by architecture
- Multipart upload endpoint (`multipart/form-data`)
- Non-blocking request handling with asynchronous processing pipeline
- Idempotency-aware command flow
- Separation of concerns with DI and clean ports/adapters
- Placeholder contracts for consistent "upload + DB record" workflow

## Important
This is a template-only implementation:
- Infrastructure adapters are stubs
- No concrete external storage integration yet
- DB schema migrations are managed by Flyway (`src/main/resources/db/migration`)

## Run
```bash
mvn spring-boot:run
```

## Package layout
- `api` REST layer (controllers, DTO, exception mapping)
- `application` use-cases and ports
- `domain` core models/value objects
- `infrastructure` configs and adapter stubs
