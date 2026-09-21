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

4. After startup, the server uses http://localhost:8080 by default. No REST endpoints
   are implemented yet, so this is not currently a usable API.

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

## Project structure

Source code is under `src/main/java/com/stackcraft/retroflow`:

- `RetroflowApplication.java`: Spring Boot entry point.
- `controller/`: empty `RetroflowController`; no request mappings.
- `entity/`: `Team`, `Retrospective`, `FeedbackItem`, and `ActionItem` entity stubs.
- `repository/`: Spring Data JPA repositories for teams, retrospectives, and feedback items.
- `exception/`: `RetroflowException`; no global exception handler.

The service layer is not implemented. Application configuration is under
`src/main/resources`, and the business-rule test source is under `src/test/java`.

## Current status

This project is a scaffold, not a completed REST API. Each entity currently contains
only an ID. Domain fields, relationships, transactional services, business-rule
enforcement, request validation, REST endpoints, and consistent HTTP error handling
remain to be implemented. Application-level authentication and authorization are
not configured.

## Contact

For project questions, use #retroflow-support on Slack.
