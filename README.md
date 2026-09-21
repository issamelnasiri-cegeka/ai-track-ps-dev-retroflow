# RetroFlow

Backend API under development for StackCraft's internal retrospective tool. Intended to
replace the mix of sticky notes, Miro boards, and shared Google Docs used to run retros.

## Tech stack

- Java 21
- Spring Boot 3.3.4 with Spring MVC
- Maven
- Spring Data JPA / Hibernate
- In-memory H2 database
- Bean Validation dependency (request validation is not implemented yet)

## Getting started

1. Install JDK 21 and Maven. No Maven wrapper is included in this repository.
2. Configuration is in `src/main/resources/application.properties`. No external database
   or PostgreSQL setup is required.
3. From the repository root, the standard startup command is:

   ```sh
   mvn spring-boot:run
   ```

   **Current blocker:** the existing test source references `TeamService` and
   `RetrospectiveService`, which do not exist yet. This command includes test compilation
   and is blocked until those services and the methods required by the tests are implemented.

4. After startup, the server uses http://localhost:8080 by default. No business REST
   endpoints are implemented yet. Actuator exposes `/actuator/health` without health
   details or component information; other Actuator endpoints are not exposed over HTTP.

## Docker

Build and run with Docker using BuildKit:

```sh
docker build -t retroflow:local .
docker run --rm --name retroflow -p 127.0.0.1:8080:8080 retroflow:local
```

The multi-stage build uses Maven and a Java 21 JDK to run
`mvn package -DskipTests --batch-mode --no-transfer-progress`, then copies only the
executable JAR into an Alpine-based Java 21 JRE image. The application runs as
non-root UID/GID `10001`, and the JAR is read-only.

Only `pom.xml` and `src/main` are copied as build inputs. This intentionally avoids
the current test-compilation blocker: `-DskipTests` skips execution, not compilation.
The existing tests are unchanged and must be fixed and run separately in CI; a
successful image build is not evidence that the business rules are implemented.
`.dockerignore` excludes unrelated files, including Git history, IDE files, local
environment files, and host build output.

The container disables the H2 console and enables graceful shutdown. Allow more
than Spring's default 30-second shutdown-phase timeout when stopping it:

```sh
docker stop --timeout 40 retroflow
```

The image health check calls `/actuator/health` every 30 seconds, with a 60-second
startup grace period and three retries before marking the container unhealthy.
It includes database health, not just whether the port is listening. Docker records
health status but does not automatically restart an unhealthy container.

The image still uses in-memory H2 unless deployment configuration overrides it.
Containerization does not make the application production-ready: persistent storage,
schema migrations, authentication, and the unfinished business API remain separate
work. A PostgreSQL deployment also requires adding its JDBC driver. Supply production
configuration and secrets at deployment time, not in the image. Pin approved base
images by digest and regularly rebuild them for security updates before deployment.

## Local database

The checked-in configuration uses H2 with these connection settings:

| Setting | Value |
| --- | --- |
| JDBC URL | `jdbc:h2:mem:retroflow;DB_CLOSE_DELAY=-1` |
| Driver | `org.h2.Driver` |
| Username | `sa` |
| Password | Empty |

Data is held in memory and is lost when the application stops. `DB_CLOSE_DELAY=-1`
keeps the database alive when connections close within the same JVM; it does not
preserve data across restarts.

Hibernate manages the schema with `spring.jpa.hibernate.ddl-auto=update`.
Direct Hibernate SQL printing is disabled with `spring.jpa.show-sql=false`.

The H2 web console is enabled at http://localhost:8080/h2-console after startup.
Use the connection settings above to log in.

These are local development settings, not a production-ready configuration.

## Running tests

```sh
mvn test
```

`RetroflowBusinessRulesTest` describes three intended business rules: closed
retrospectives reject new feedback, a team cannot have two open retrospectives,
and completed action items cannot be uncompleted.

The test source currently cannot compile because the required services are missing.
These are specifications for unfinished functionality, not a passing test suite.

## Continuous integration and coverage

`.github/workflows/ci.yml` runs on pushes to every branch and pull requests targeting
`main`. It uses an Ubuntu runner, Temurin Java 21, and a Maven dependency cache keyed
by `pom.xml`. It runs:

```sh
mvn verify --batch-mode --no-transfer-progress
```

JaCoCo instruments tests, writes HTML and XML reports to `target/site/jacoco`, and
fails `verify` if aggregate line coverage is below 80%. This is a project-wide line
coverage requirement, not branch coverage or an 80% requirement for each class.
CI also fails if a successful Maven run produces no coverage report.

PR runs upload the XML report even when the coverage gate fails. A separate
`coverage-comment.yml` workflow posts the percentage on the matching PR, or states
that coverage is unavailable if compilation failed before a report could be generated.
It runs from the default branch, never checks out PR code or restores build caches,
and has only artifact-read and PR-comment permissions. This separation supports fork
PRs without giving their build jobs write credentials or repository secrets.
The comment workflow must exist on the repository's default branch before it can run.
Repository and organization policies must permit Actions to comment on PRs.

Comments are informational, not a security boundary: PR code can alter its own
coverage output. Require the `Verify and enforce coverage` check through branch
protection, and require trusted review of workflow, build, and test changes.
Actions are pinned to commit SHAs; keep those pins and Maven dependencies updated.

The existing missing-service compilation blocker also affects CI. Neither tests nor
the coverage requirement are bypassed by this workflow.

## Project structure

Source code is under `src/main/java/com/stackcraft/retroflow`:

- `RetroflowApplication.java`: Spring Boot entry point.
- `controller/`: empty `RetroflowController`; no business request mappings.
- `entity/`: `Team`, `Retrospective`, `FeedbackItem`, and `ActionItem` entity stubs.
- `repository/`: Spring Data JPA repositories for teams, retrospectives, and feedback items.
- `exception/`: `RetroflowException`; no global exception handler.

The service layer is not implemented. Application configuration is under
`src/main/resources`, and the business-rule test source is under `src/test/java`.

## Current status

This project is a scaffold, not a completed REST API. Each entity currently contains
only an ID. Domain fields, relationships, transactional services, business-rule
enforcement, request validation, business REST endpoints, and consistent HTTP error handling
remain to be implemented. Application-level authentication and authorization are
not configured.

## Contact

For project questions, use #retroflow-support on Slack.
