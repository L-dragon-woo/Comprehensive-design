<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-31 | Updated: 2026-05-31 -->

# backend

## Purpose
Spring Boot 4.0.6 API server (port 8080, management port 8081). Responsibilities: JWT authentication backed by MariaDB user records and Redis refresh-token store; proxying and persisting AI analysis/chat requests to the FastAPI AI server; Kakao Local API hospital search; Prometheus metrics via Spring Actuator.

## Key Files
| File | Description |
|------|-------------|
| `build.gradle` | Gradle build script — Spring Boot 4.0.6, Spring Data JPA/MongoDB/Redis, WebMVC + WebFlux, Spring Security + OAuth2 Resource Server, Auth0 java-jwt 4.4.0, Lombok, MariaDB JDBC, Micrometer Prometheus |
| `settings.gradle` | Sets `rootProject.name = 'backend'` |
| `Dockerfile` | Two-stage build: `gradle:9.2-jdk17` builds the fat jar via `gradle bootJar --no-daemon`; `eclipse-temurin:17-jre` runs it; exposes port 8080 |
| `gradle/wrapper/gradle-wrapper.properties` | Pins Gradle 9.4.1 binary distribution |
| `gradlew` / `gradlew.bat` | Gradle wrapper scripts (Unix / Windows) |
| `src/main/resources/application.properties` | All runtime config — datasource (MariaDB), Redis, MongoDB, JWT (secret/issuer/TTLs), AI service base URL (`skinai.ai.base-url`), Kakao REST key, multipart limits (10 MB), Actuator endpoints |

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `src/main/` | Java application sources and resources |
| `src/test/` | JUnit 5 tests (Spring Boot test slice) |
| `src/` | See code-map in `src/main/java/com/ohgiraffers/backend/AGENTS.md` |
| `gradle/wrapper/` | Gradle wrapper binaries and properties |
| `build/` | Compiled output (git-ignored) |

## For AI Agents

### Working In This Directory
- All config is injected via environment variables with defaults defined in `application.properties`. Override via env vars in `docker-compose.yml` or a local `.env`.
- The AI service URL is configured as `skinai.ai.base-url` (env: `AI_SERVICE_URL`, default `http://localhost:8000`). All AI calls go through `AiProxyController`, never directly from the frontend.
- Multipart and codec limits are set to 10 MB both here and in `docker-compose.yml`. Changing them requires both files.
- MariaDB DDL auto is `update` by default; in production override with `validate` or `none`.
- Management endpoints (health probes, prometheus, info) are exposed on port 8081 and the `GET /actuator/prometheus` endpoint is only permitted from the internal management port (enforced in `SecurityConfig`).

### Testing Requirements
- Compile check: `.\gradlew.bat compileJava`
- Run tests: `.\gradlew.bat test`
- Tests use Spring Boot test slices; Redis reactive test support is on the test classpath (`spring-boot-starter-data-redis-reactive-test`).
- Integration tests require running Redis, MariaDB, and MongoDB; use `docker compose up -d redis mariadb mongo` before running `.\gradlew.bat test` locally.

### Common Patterns
- Constructor injection throughout — no field `@Autowired`.
- `ResponseStatusException` is the standard way to signal HTTP errors; `ApiExceptionHandler` serializes them to `{"status": N, "message": "..."}`.
- JWT tokens carry a `type` claim (`"access"` or `"refresh"`); `JwtService.verify()` validates both signature and type.
- MongoDB documents use `Instant` for timestamps; auto-index creation is enabled.
- Redis keys for refresh tokens: `auth:refresh:{username}:{hex-hash-of-token}`.

## Dependencies

### Internal
- Reads AI server at `AI_SERVICE_URL` (default `http://ai:8000`) — the FastAPI service in `../AI/`
- Shares API surface with `../frontend/` — route contracts defined in `../frontend/apiList.md`

### External
| Dependency | Version / Coordinates | Role |
|-----------|----------------------|------|
| Spring Boot | 4.0.6 | Framework BOM (manages all spring-* versions) |
| Spring Data JPA | managed by Boot | ORM over MariaDB (`users` table) |
| Spring Data MongoDB | managed by Boot | Document store for analysis results and chat history |
| Spring Data Redis / Redis Reactive | managed by Boot | Refresh-token TTL store |
| Spring Security | managed by Boot | Stateless JWT filter chain, BCrypt password encoding |
| Spring WebMVC | managed by Boot | REST controllers |
| Spring WebFlux (WebClient) | managed by Boot | Non-blocking HTTP proxy to AI server and Kakao API |
| Spring Actuator | managed by Boot | `/actuator/health`, `/actuator/prometheus`, `/actuator/info` |
| `com.auth0:java-jwt` | 4.4.0 | JWT creation and verification (HMAC256) |
| Lombok | managed by Boot | `@Data`, `@Builder`, etc. (compile-only) |
| `org.mariadb.jdbc:mariadb-java-client` | managed by Boot | MariaDB JDBC driver (runtime) |
| `io.micrometer:micrometer-registry-prometheus` | managed by Boot | Prometheus metrics export (runtime) |
| Kakao Local API | external HTTP | Hospital place search (`/v2/local/search/keyword.json`) |

<!-- MANUAL: Any manually added notes below this line are preserved on regeneration -->
