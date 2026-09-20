# RetroFlow

Backend API for StackCraft's internal retrospective tool. Replaces the mix of sticky notes,
Miro boards, and shared Google Docs the 12 dev teams currently use to run retros.

## Tech stack

- Java 21
- Spring Boot 3
- Gradle
- PostgreSQL

## Getting started

1. Make sure you have PostgreSQL running locally on port 5432. Create a database called
   `retroflow`.
2. Update `application.yml` with your local database credentials if they differ from the
   defaults.
3. Run the app:

   ```
   ./gradlew bootRun
   ```

4. The API will be available at http://localhost:8080.

## Running tests

```
./gradlew test
```

## Project structure

Standard Spring Boot layered structure — controllers, services, repositories, entities.

## Current status

Core domain model (Team, Retrospective, FeedbackItem, ActionItem) and CRUD endpoints are in
progress. See `RetroflowController` for the current endpoint list.

## Contact

The previous team lead has left the company, so there is no forwarding contact. Ping #retroflow-support on
Slack if you get stuck, though response times have been slow lately.
