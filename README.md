# GitHub Access Report

## 1. Overview

GitHub Access Report is a Spring Boot application that connects to the GitHub API and generates an access report for repositories within a GitHub organization.

The application retrieves repositories and their collaborators, determines each user's permission level, and aggregates the information into a user-centric access report.

The project is designed to handle organizations with a large number of repositories and users by processing repository collaborator requests concurrently using a bounded thread pool.

---

## 2. Problem Statement

Organizations often need visibility into who has access to which repositories in GitHub.

This application provides an API that generates a structured report showing:

* GitHub organization
* Users with repository access
* Repositories accessible by each user
* Permission level for each repository

---

## 3. Functional Requirements

The application implements the following requirements:

1. Authenticate with GitHub using a personal access token.
2. Retrieve repositories belonging to a GitHub organization.
3. Retrieve collaborators for each repository.
4. Determine the user's permission level.
5. Aggregate repository access by username.
6. Expose the access report through a REST API.

Supported permission levels include:

* `ADMIN`
* `WRITE`
* `READ`
* `NONE`

---

## 4. Technology Stack

* Java 21
* Spring Boot 4.1.1
* Spring Web MVC
* Spring WebFlux / WebClient
* Maven
* JUnit 5
* Mockito
* Jackson
* GitHub REST API

---

## 5. Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/githubAccess/githubAccessReport/
│   │       ├── client/
│   │       │   └── GitHubClient.java
│   │       ├── controller/
│   │       │   └── AccessReportController.java
│   │       ├── dto/
│   │       │   ├── AccessReportResponse.java
│   │       │   ├── GitHubCollaboratorResponse.java
│   │       │   ├── GitHubRepositoryResponse.java
│   │       │   ├── RepositoryAccess.java
│   │       │   └── UserAccessReport.java
│   │       ├── exception/
│   │       │   ├── GitHubApiException.java
│   │       │   └── GlobalExceptionHandler.java
│   │       ├── service/
│   │       │   └── AccessReportService.java
│   │       └── GithubAccessReportApplication.java
│   │
│   └── resources/
│       └── application.properties
│
└── test/
    └── java/
        └── com/githubAccess/githubAccessReport/
            ├── client/
            │   └── GitHubClientTest.java
            ├── controller/
            │   └── AccessReportControllerTest.java
            └── service/
                └── AccessReportServiceTest.java
```

---

## 6. Authentication Configuration

The application authenticates with GitHub using a Personal Access Token.

The GitHub token should **not be committed to the repository**.

Configure the application using an environment variable.

### application.properties

```properties
server.port=8082

spring.application.name=githubAccessReport

github.api-url=https://api.github.com

github.token=${GITHUB_TOKEN}
```

Before starting the application, set the environment variable.

### Windows PowerShell

```powershell
$env:GITHUB_TOKEN="your-github-token"
```

Then run the application.

### Important

Never commit the actual GitHub token to GitHub.

If a token is accidentally exposed, revoke it and create a new token.

---

## 7. How to Run the Application

### Prerequisites

Make sure the following are installed:

* Java 21
* Maven
* A GitHub Personal Access Token with sufficient permissions to read the organization's repositories and collaborators

### Clone the repository

```bash
git clone <your-public-github-repository-url>
```

Navigate into the project:

```bash
cd githubAccessReport
```

### Configure the GitHub token

Windows PowerShell:

```powershell
$env:GITHUB_TOKEN="your-github-token"
```

### Run tests

```bash
mvn clean test
```

### Start the application

```bash
mvn spring-boot:run
```

The application runs on:

```text
http://localhost:8082
```

---

## 8. API Endpoint

The application exposes an endpoint for generating the access report.

### Generate Access Report

```http
GET /api/github/{organization}/access-report
```

Example:

```http
GET http://localhost:8082/api/github/cloud-eagle-java-test/access-report
```

Replace `cloud-eagle-java-test` with the GitHub organization you want to inspect.

---

## 9. Example Response

The response is aggregated by user.

Example:

```json
{
  "organization": "cloud-eagle-java-test",
  "users": [
    {
      "username": "pratha27m",
      "repositories": [
        {
          "repository": "access-report-test",
          "permission": "ADMIN"
        }
      ]
    },
    {
      "username": "prathmeshlashkare27",
      "repositories": [
        {
          "repository": "access-report-test",
          "permission": "WRITE"
        }
      ]
    },
    {
      "username": "lashkare27",
      "repositories": [
        {
          "repository": "access-report-test",
          "permission": "READ"
        }
      ]
    }
  ]
}
```

---

## 10. Scalability and Performance

The assignment requires support for organizations with 100+ repositories and 1000+ users with repository access.

The implementation avoids processing repositories sequentially.

For each repository, a task is submitted to a bounded `ExecutorService` thread pool.

```java
Executors.newFixedThreadPool(10);
```

This allows multiple repository collaborator requests to execute concurrently while limiting the number of simultaneous tasks.

A fixed-size pool is used instead of creating one thread per repository to avoid excessive thread creation.

The collaborator results are aggregated using a `ConcurrentHashMap`.

Conceptually:

```text
Organization
     |
     v
Get repositories
     |
     +-------- Repository 1
     |             |
     |             v
     |       Get collaborators
     |
     +-------- Repository 2
     |             |
     |             v
     |       Get collaborators
     |
     +-------- Repository 3
                   |
                   v
             Get collaborators

              ↓

      Concurrent aggregation

              ↓

        User Access Report
```

This design reduces unnecessary sequential waiting when an organization contains many repositories.

---

## 11. Error Handling

The application contains centralized exception handling using `@RestControllerAdvice`.

GitHub API failures are represented by:

```java
GitHubApiException
```

The `GlobalExceptionHandler` converts exceptions into HTTP responses.

This keeps error-handling logic separate from controller and service logic.

---

## 12. Testing

The project contains unit and integration-style tests for the main components.

### GitHub Client Tests

`GitHubClientTest`

Tests GitHub API interaction logic, including:

* Repository retrieval
* Collaborator retrieval
* Pagination
* JSON response mapping
* API error scenarios

### Service Tests

`AccessReportServiceTest`

Tests:

* Access report generation
* User aggregation
* `ADMIN` permission mapping
* `WRITE` permission mapping
* `READ` permission mapping
* Empty repository scenarios

### Controller Tests

`AccessReportControllerTest`

Tests:

* Successful API requests
* Invalid requests / validation behavior
* Controller response handling

Run all tests with:

```bash
mvn clean test
```

Expected result:

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

---

## 13. Design Decisions

### Bounded concurrency

A fixed thread pool of 10 threads is used to process repository collaborator requests concurrently.

This provides better performance than making every GitHub API call sequentially while preventing uncontrolled thread creation.

### Concurrent aggregation

`ConcurrentHashMap` is used because multiple repository-processing tasks can update the access report concurrently.

### Separation of responsibilities

The application is separated into:

* `Controller` — handles HTTP requests
* `Service` — generates and aggregates the access report
* `Client` — communicates with GitHub
* `DTO` — represents request/response data
* `Exception` — handles application/API errors

This makes the application easier to test and maintain.

### Pagination

GitHub API responses are paginated. The GitHub client handles pagination when retrieving repositories and collaborators so that the implementation is not limited to the first page of results.

---

## 14. Assumptions

* The provided GitHub token has permission to access the required organization repositories and collaborator information.
* The organization name supplied to the API exists on GitHub.
* GitHub API rate limits are respected.
* Repository access is represented using the permission levels returned by GitHub.
* The application currently uses a fixed thread pool of 10 concurrent tasks as a bounded concurrency strategy.
* The access report is generated synchronously; the API waits until repository processing has completed before returning the response.

---

## 15. Security

The GitHub Personal Access Token is supplied through configuration/environment variables rather than being hardcoded into the application source code.

The token must not be committed to Git.

If a token is accidentally exposed:

1. Revoke the exposed token in GitHub.
2. Generate a new token.
3. Store the new token in the `GITHUB_TOKEN` environment variable.

---

## 16. Future Improvements

Possible improvements for a production-scale implementation include:

* GitHub API rate-limit handling and retry/backoff
* Configurable thread-pool size
* Caching repository/collaborator information
* Asynchronous report generation for very large organizations
* Structured JSON error responses
* More detailed API documentation using OpenAPI/Swagger
* Metrics for GitHub API latency and failures
* Integration tests using a mock GitHub API

---

## 17. Conclusion

The GitHub Access Report service provides an aggregated view of repository access within a GitHub organization.

It satisfies the required functionality by authenticating with GitHub, retrieving repositories and collaborators, determining permissions, aggregating access by user, and exposing the result through a REST API.

The implementation also addresses the scale requirement through bounded concurrent processing and thread-safe aggregation.