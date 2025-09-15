# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/java/com/sivalabs/bookstore/{common,catalog,orders,inventory,notifications,config}`
- Tests: `src/test/java/com/sivalabs/bookstore/**`
- Resources: `src/main/resources` (Liquibase changelogs in `src/main/resources/db`)
- Modular monolith using Spring Modulith; communicate across modules via published APIs and domain events, not direct internals.

## Build, Test, and Development Commands
- Build & test: `./mvnw -ntp clean verify`
- Run locally: `./mvnw spring-boot:run` (requires PostgreSQL and RabbitMQ or Testcontainers in tests)
- Format code: `./mvnw spotless:apply`
- Task runner shortcuts: `task test`, `task format`, `task build_image`, `task start`, `task stop`
- Apply DB changes (local): `./mvnw liquibase:update` (uses config in `pom.xml`)

## Coding Style & Naming Conventions
- Java 21+. Formatting enforced by Spotless + Palantir Java Format. CI fails on unformatted code.
- Package-by-feature under `com.sivalabs.bookstore.<module>`; avoid cyclic dependencies.
- Class/files: descriptive PascalCase; constants UPPER_SNAKE_CASE.
- Spring beans: prefer constructor injection; keep config in `config` module.

## Testing Guidelines
- Frameworks: JUnit 5, Spring Boot Test, Spring Modulith tests, Testcontainers.
- Name tests with `*Tests` (e.g., `OrderRestControllerTests`).
- Prefer slice or module-scoped tests; use `@SpringBootTest` only when needed.
- Run all tests: `./mvnw -ntp verify` or `task test` (also used by CI).

## Commit & Pull Request Guidelines
- Use Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `chore:` (optionally scope: `feat(orders): ...`).
- Keep messages imperative and concise; link issues (e.g., `#123`).
- PRs: follow `.github/pull_request_template.md`; include description, affected modules, test steps, and screenshots when UI changes.
- Before opening: run `./mvnw spotless:apply` and `./mvnw -ntp verify`; respect Modulith boundaries.

## Security & Configuration Tips
- Do not commit secrets. Use env vars to override defaults:
  - `SPRING_DATASOURCE_URL`, `SPRING_RABBITMQ_HOST` (see `compose.yml`).
- Observability: Actuator at `/actuator`; Modulith info at `/actuator/modulith`.
