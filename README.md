# Readscape

Event-driven book catalog, reviews and lending platform built with Java 25, Spring Boot 4 and Kafka.
This is v2 of the earlier "Book Management System" microservices project; the plan, the v1 audit and the
roadmap are in [docs/v2-plan.md](docs/v2-plan.md).

## Services

| Service | Port | Responsibility |
|---|---|---|
| gateway | 8080 | Spring Cloud Gateway: routing, JWT validation, aggregated Swagger UI |
| identity-service | 8001 | Users, login, RS256 JWT issuing, JWKS |
| catalog-service | 8002 | Authors, books, cover images (MinIO) |
| review-service | 8003 | Reviews and ratings |
| notification-service | 8004 | Notifications (event consumer, from M2) |

Shared code lives in `libs/` (`platform`, `common-web`, `security`, `test-fixtures`).

## Run locally

Requirements: JDK 25, Docker, `jq` and `openssl`.

```bash
make dev-keys     # local JWT signing key in .local/keys (git-ignored)
make apps-build   # build jars and images
make up           # infra + services via docker compose
make smoke        # end-to-end checks through the gateway
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- kafka-ui: http://localhost:8090
- Dev admin login: `admin` / `admin` (local only)

For development, `make infra-up` starts only Postgres, Kafka, Schema Registry and MinIO, and the services
run from the IDE or `./gradlew bootRun`. `make help` lists all targets.

## Tests

```bash
make build        # ./gradlew build with JDK 25, Testcontainers-based tests
```

## Roadmap

M0 foundation and M1 identity, CRUD and security are done. Next: M2 event backbone (outbox, Avro,
retry/DLT), M3 observability, M4 CQRS read model, M5 borrow saga, M6 hardening. See the plan for details.
