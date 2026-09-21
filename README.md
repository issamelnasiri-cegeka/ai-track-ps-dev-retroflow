# RetroFlow

Spring Boot backend for StackCraft's retrospective tool. The intended business
rules are in the [project brief](docs/retroflow-brief.md).

**Current status:** the application starts, H2 and health reporting work, and
controllers validate requests. However, `TeamService` and `RetrospectiveService`
still throw `UnsupportedOperationException`. Valid business requests return HTTP
500, and the three business-rule tests error. This is an unfinished API, not a
production-ready service.

Stack: Java 21, Spring Boot 3.3.4, Maven, Spring MVC, Spring Data JPA/Hibernate,
Bean Validation, Actuator, and in-memory H2. Tests use JUnit 5 and AssertJ.

## Getting Started

### From a fresh clone to a running application in under five minutes

The quickest setup uses Docker: no local Java, Maven, PostgreSQL, database creation,
or database credentials are required.

Prerequisites:

- Git and access to this repository.
- A running Docker Desktop or Docker Engine with BuildKit.
- `curl` 7.71 or newer for the startup retry command.
- Port 8080 available locally.

Examples use a POSIX-compatible shell: macOS/Linux, or Git Bash/WSL on Windows.
Install prerequisites first. The five-minute target assumes a working internet
connection; initial image and dependency downloads depend on network speed.

1. Clone the repository and enter it:

   ```sh
   git clone https://github.com/issamelnasiri-cegeka/ai-track-ps-dev-retroflow.git
   cd ai-track-ps-dev-retroflow
   ```

2. Build and start the application:

   ```sh
   docker build --tag retroflow:local .
   docker run --detach --rm --name retroflow \
     --publish 127.0.0.1:8080:8080 retroflow:local
   ```

3. Wait for startup and check the health endpoint:

   ```sh
   curl --fail --silent --show-error \
     --retry 30 --retry-all-errors --retry-delay 1 \
     --retry-max-time 60 --max-time 3 \
     http://localhost:8080/actuator/health
   ```

   Expected response: `{"status":"UP"}`. Brief connection errors while Java starts
   are retried. This confirms application/database health, not that business
   operations are implemented. There is no frontend at `/`; HTTP 404 there is normal.

The application is now listening at `http://localhost:8080`. Try the requests in
[Manual API testing](#manual-api-testing).

Useful container commands:

```sh
docker logs --tail 50 retroflow
docker inspect --format '{{.State.Health.Status}}' retroflow
docker stop --timeout 40 retroflow
```

The health status can remain `starting` until the first scheduled probe. The
40-second stop timeout allows graceful shutdown; `--rm` removes the stopped
container. Stopping the application discards its in-memory data.

## Local development without Docker

Install **JDK 21** (Temurin recommended) and **Maven 3.9.x**. No Maven wrapper is
currently checked in. Confirm both the compiler/runtime selection and Maven's JVM:

```sh
java -version
mvn --version
```

Both should report Java 21. If they disagree, correct `JAVA_HOME`, `PATH`, and the
IDE's Maven JVM selection. Do not rely on whichever JDK happens to be installed.

From the repository root:

```sh
mvn spring-boot:run
```

Wait for the `Started RetroflowApplication` log, then use the same health and API
requests as for Docker. Stop with Ctrl+C. This command compiles test sources but
does not run tests, so the current business-rule test errors do not block startup.
No test-skip option is needed.

If Docker or another application already uses port 8080:

```sh
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Use `http://localhost:8081` for that instance. Each application process has its own
in-memory database.

### IDE setup and the edit/run loop

| IDE | Configuration |
| --- | --- |
| IntelliJ IDEA | Open `pom.xml` as a Maven project. Set Project SDK, language level, and Maven runner/importer JVM to Java 21. |
| VS Code | Open the repository folder, install Extension Pack for Java, and select JDK 21 as the project runtime. The Spring Boot Extension Pack is optional. |

Run or debug `com.stackcraft.retroflow.RetroflowApplication` with the repository
root as the working directory. In an IDE application configuration, `--server.port=8081`
is a **program argument**, not a JVM option. Put breakpoints in the controllers or
services to inspect a request.

For the fastest loop, run directly through Maven or the IDE instead of rebuilding
the Docker image after every edit. Restart the application after source changes;
Spring Boot DevTools and automatic restart are **not** configured. IDE HotSwap can
handle some method-body changes while debugging, but structural changes may need
a restart.

Keep `.idea/`, `.vscode/`, and machine-specific run settings local. Docker's
allowlist excludes root-level IDE files. Git already ignores IntelliJ files; do
not assume it ignores every editor's settings.

## Manual API testing

No authentication is configured. Keep the application on a trusted development
machine; the Docker quickstart publishes only to localhost.

### Routes that exist today

| Method | Path | Current behavior |
| --- | --- | --- |
| GET | `/actuator/health` | HTTP 200 with `{"status":"UP"}` when healthy |
| POST | `/api/teams` | Validates input; valid requests reach an unimplemented service and return HTTP 500 |
| GET | `/api/teams/{id}` | Requires a positive ID; otherwise reaches the service stub |
| POST | `/api/teams/{teamId}/retrospectives` | Validates ID/title; otherwise reaches the service stub |
| GET | `/api/teams/{teamId}/retrospectives` | Requires a positive team ID; otherwise reaches the service stub |
| PUT | `/api/retrospectives/{id}/close` | Requires a positive ID; otherwise reaches the service stub |

There are no feedback/action-item HTTP routes, Swagger UI, or OpenAPI endpoint yet.
`RetroflowController` is a leftover empty placeholder; the actual routes are in
`TeamController` and `RetrospectiveController`.

### Check request validation

This intentionally invalid request should return **HTTP 400**, with messages
explaining that a name and at least one member are required:

```sh
curl --include --request POST http://localhost:8080/api/teams \
  --header 'Content-Type: application/json' \
  --data '{"name":"","members":[]}'
```

A representative response body is below; the timestamp and message ordering vary:

```json
{
  "timestamp": "2026-09-21T09:22:25Z",
  "status": 400,
  "error": "Bad Request",
  "message": "name: name must not be blank; members: members must contain at least one name"
}
```

Path parameters are validated too:

```sh
curl --include http://localhost:8080/api/teams/0
```

Expected: **HTTP 400**, with an `id must be positive` message.

### Exercise the current implementation boundary

This payload satisfies request validation:

```sh
curl --include --request POST http://localhost:8080/api/teams \
  --header 'Content-Type: application/json' \
  --data '{"name":"Platform","members":["Alice","Bob"]}'
```

**Current result: HTTP 500**, because `TeamService.createTeam` is not implemented.
Once implemented, the controller is designed to return HTTP 201 and a team response
with `id`, `name`, and `members`. Do not assume a team was created or that ID 1 exists.

Team/member names must be nonblank and at most 100 characters; a team needs at
least one member. Retrospective creation accepts `{"title":"Sprint 1"}` with a
nonblank title of at most 200 characters.

The global handler maps validation errors to 400, missing-resource exceptions to
404, and business-rule exceptions to 409. The current unimplemented-service 500s
use Spring Boot's default error response instead of that custom error format.
You can import the curl commands into Postman or run equivalent requests in an IDE
HTTP client; no account, API key, or seed data is needed.

## Tests and coverage

Run the same lifecycle used by CI:

```sh
mvn verify --batch-mode --no-transfer-progress
```

For a shorter feedback loop focused on the existing test class:

```sh
mvn -Dtest=RetroflowBusinessRulesTest test
```

**Both commands currently exit unsuccessfully:** all three tests error with
`UnsupportedOperationException` from `TeamService.createTeam`. They compile and
run; the problem is unfinished business logic, not missing service classes.
Do not skip tests or weaken their assertions to make CI green.

The tests describe three required rules: closed retrospectives reject new
feedback, a team cannot have two open retrospectives, and completed action items
cannot be uncompleted.

Surefire results are in `target/surefire-reports/`. JaCoCo attaches its agent
during tests and normally generates reports and enforces **80% aggregate line
coverage** during `verify`. This is not branch coverage or a per-class threshold.
Test errors currently stop Maven before that reporting/check phase.

After a test run has produced `target/jacoco.exec`, generate a diagnostic report:

```sh
mvn jacoco:report
```

Open `target/site/jacoco/index.html` in a browser; machine-readable coverage is in
`target/site/jacoco/jacoco.xml`. Generating a report does not make failing tests pass
or run the coverage gate. After implementing the services, rerun `mvn verify`.

## Configuration and database

The configuration file is
[`src/main/resources/application.properties`](src/main/resources/application.properties),
not `application.yml`.

| Setting | Checked-in behavior |
| --- | --- |
| Application / default port | `retroflow` / `8080` |
| Database | In-memory H2; no external database |
| JDBC URL | `jdbc:h2:mem:retroflow;DB_CLOSE_DELAY=-1` |
| Driver / username / password | `org.h2.Driver` / `sa` / empty |
| Schema | Hibernate `ddl-auto=update`; no migration tool |
| SQL printing | `spring.jpa.show-sql=false` |
| H2 console | Enabled for direct Maven/IDE runs; disabled in the Docker image |
| Actuator HTTP exposure | Health only, without diagnostic details or component names |

For a direct Maven/IDE run, open `http://localhost:8080/h2-console/` and use the
connection settings above. The console must connect to the same application;
another process using that JDBC URL gets a different in-memory database.
`DB_CLOSE_DELAY=-1` keeps the database alive between connections in the same JVM,
not across application restarts. There is no seed-data script.

Spring Boot accepts environment overrides such as `SERVER_PORT` and
`SPRING_DATASOURCE_URL`; it does **not** automatically load a local `.env` file.
Do not commit credentials. PostgreSQL is not configured and its JDBC driver is
not included.

## Docker behavior

The multi-stage image builds with Maven and a Java 21 JDK, then runs the executable
JAR on an Alpine-based Java 21 JRE as UID/GID `10001`. The JAR is read-only. The
container disables the H2 console and enables graceful shutdown.

The build copies only `pom.xml` and `src/main` and runs
`mvn package -DskipTests --batch-mode --no-transfer-progress`. It deliberately does
not include or run the tests. A successful image build is not a successful
verification of the application. In a full checkout, `-DskipTests` skips execution,
not test compilation.

The image probes `/actuator/health` every 30 seconds, with a 60-second startup
grace period and three retries. Docker records an unhealthy status but does not
automatically restart an unhealthy container. To use a different host port, change
the publish mapping to `127.0.0.1:8081:8080`; keep the container port at 8080 so its
health check still works. After changing code, stop, rebuild, and rerun the
quickstart container.

This is not a production deployment recipe. Authentication, durable storage,
schema migrations, TLS termination, and secret management are not configured.

## CI

[Maven CI](.github/workflows/ci.yml) runs on every branch push and on PRs targeting
`master`, using Ubuntu, Temurin Java 21, Maven caching, and the verification command
above. It also rejects a successful Maven run with no coverage report.

The [coverage commenter](.github/workflows/coverage-comment.yml) runs separately
with artifact-read and PR-write permissions. It does not check out PR code or
restore its cache. It posts coverage when available, including after failed tests
that produced execution data; otherwise it states that coverage is unavailable.
The commenter must be present on the default branch and repository policy must
allow PR comments.

The PR triggers and coverage comment filters target `master`, the repository's
default branch. If the default branch is renamed, update both workflows together.

The current service stubs keep CI red. Require trusted review of workflow/build
changes and a passing CI job before merging completed implementation work.
Coverage comments are informational: PR-controlled code can alter its report.
Actions use commit-SHA pins; keep them and Maven dependencies updated.

## Repository map

| Path | Purpose |
| --- | --- |
| `pom.xml` | Maven dependencies, Java version, packaging, JaCoCo |
| `src/main/java/com/stackcraft/retroflow/RetroflowApplication.java` | Application entry point |
| `src/main/java/com/stackcraft/retroflow/controller/` | HTTP routes |
| `src/main/java/com/stackcraft/retroflow/dto/` | Request validation and response records |
| `src/main/java/com/stackcraft/retroflow/service/` | Unimplemented business operations |
| `src/main/java/com/stackcraft/retroflow/entity/` | JPA entities, relationships, and enums |
| `src/main/java/com/stackcraft/retroflow/repository/` | Spring Data repositories |
| `src/main/java/com/stackcraft/retroflow/web/` | Global exception handling |
| `src/test/java/com/stackcraft/retroflow/` | Business-rule tests |
| `docs/retroflow-brief.md` | Intended product behavior |
| `Dockerfile`, `.dockerignore` | Container build and build-context allowlist |
| `.github/workflows/` | Verification and PR coverage commenting |

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Docker cannot connect to its daemon | Start Docker Desktop/Engine before building. |
| Git clone is denied | Authenticate with GitHub and confirm repository access; never put a token in the clone URL. |
| Port 8080 is occupied | Stop your own existing instance or use the alternate-port configuration above. |
| Container name `retroflow` already exists | Inspect it before stopping it; do not delete someone else's container. |
| Native build uses the wrong Java release | Check both `java -version` and `mvn --version`, plus the IDE Maven JVM. |
| HTTP 500 on a valid business request | Service methods are still unimplemented; inspect application logs. |
| HTTP 404 at `/` or `/swagger-ui` | No frontend or Swagger UI is configured. |
| H2 console is missing in Docker | It is intentionally disabled; use a local Maven/IDE run for database inspection. |
| Data disappears on restart | Expected for in-memory H2, even when the container is restarted. |
| No JaCoCo HTML report after failed tests | Run the diagnostic reporting command after execution data has been written. |

## Useful next tooling additions

These are recommendations, **not installed features**: a Maven Wrapper to pin the
build tool, an `.editorconfig` for consistent whitespace, a shared `.http` request
collection, and a development-only DevTools setup for automatic restart. Add
controller/integration tests and OpenAPI documentation alongside implemented
endpoints rather than documenting unimplemented happy paths as working.

For project questions, use the documented Slack channel, #retroflow-support.
