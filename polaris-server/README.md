# polaris-server


![alt text](https://img.shields.io/badge/language-Kotlin-7F52FF.svg)


![alt text](https://img.shields.io/badge/framework-Quarkus-4695EB.svg)


This repository contains the backend server for the Polaris project, a secure, opportunistic Proof-of-Location (PoL) system. The  server is built with Kotlin and the Quarkus framework, providing a  high-performance, container-native application.

The Polaris server acts as the central authority for the entire system:

1. Registers mobile devices and issues unique API keys for secure communication.
2. Provides mobile clients with the necessary information (public keys, IDs) about known beacons in their vicinity.
3. Receives and validates Proof-of-Location tokens  submitted by clients.
4. Manages a job queue of outbound encrypted commands for beacons and processes inbound data relayed by mobile phones.
5. Offers a simple web interface for managing beacons and creating outbound payload jobs.

## System architecture

The server is designed with a layered architecture to ensure separation of concerns, testability, and maintainability.

- Web Layer (web):
  - Handles all incoming HTTP requests via JAX-RS resources.
  - Separated into a secure /api/v1 for clients and a /admin web interface.
  - Uses Qute templates for server-side HTML rendering of the admin panel.
  - Implements API key-based authentication via a custom JAX-RS filter.
- Service Layer (services):
  - Contains all the business logic of the application.
  - Divided into sub-packages by domain (token, payload, crypto, admin).
  - Follows the Single Responsibility Principle (SRP) with specialized classes like PoLTokenValidator and PoLTokenAssembler.
  - Uses Dependency Injection heavily, managed by Quarkus CDI.
- Data Access Layer (repositories & entities):
  - Uses Hibernate ORM with Panache for simplified and idiomatic data access in Kotlin.
  - Entities define the database schema and relationships.
  - Repositories encapsulate data querying logic.
- Database:
  - Designed for PostgreSQL.
  - Schema is managed and versioned using Flyway migrations.



## Configuration

The main configuration is located in src/main/resources/application.properties.

- Database: Configures the JDBC URL, username, and password for the PostgreSQL connection.
- Flyway: Enabled by default (quarkus.flyway.migrate-at-start=true), it automatically applies pending SQL migrations from src/main/resources/db/migration on startup.
- HTTP: Sets the application port and proxy settings.



## Getting started

Requirements:

- JDK 21 or newer.
- Apache Maven 3.9 or newer.
- PostgreSQL database server.

Clone the Repository:

```bash
git clone https://github.com/DrC0okie/polaris.git
cd polaris/polaris-server
```

Configure Environment Variables:
The server's private key is configured via an environment variable. Create a .env file in the project root:

```bash
# .env
# A 32-byte (256-bit) X25519 private key, Base64 encoded.
# You can generate a new one with: openssl rand -base64 32
POLARIS_SERVER_AEAD_SK_B64=YourGeneratedBase64PrivateKeyHere
```

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.



### Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.



## API endpoints

The API is secured with an API key passed in the x-api-key header.

| Method | Endpoint             | Description                                                  |
| ------ | -------------------- | ------------------------------------------------------------ |
| POST   | /api/v1/register     | Registers a new phone and returns an API key and initial beacon data. |
| GET    | /api/v1/beacons      | (Secured) Fetches the latest list of provisioned beacons.    |
| POST   | /api/v1/tokens       | (Secured) Submits a PoL token for validation and storage.    |
| GET    | /api/v1/payloads     | (Secured) Fetches pending outbound payload jobs for the phone. |
| POST   | /api/v1/payloads     | (Secured) Submits an inbound payload from a beacon for processing. |
| POST   | /api/v1/payloads/ack | (Secured) Submits an acknowledgment blob for a delivered outbound payload. |

## Admin interface

A simple web interface is provided for administrative tasks:

- URL: http://localhost:8080/admin/beacons
- Features:
  - List all registered beacons.
  - Add, edit, and delete beacons.
  - List all outbound payload jobs.
  - Create new outbound payload jobs to send commands to beacons.

*Note: The admin interface is not secured in this version.*

## Database migrations

The database schema is managed by [Flyway](https://www.google.com/url?sa=E&q=https%3A%2F%2Fflywaydb.org%2F). Migration scripts are located in src/main/resources/db/migration. To add a new migration, create a new SQL file following the Flyway naming convention, e.g., V1.0.3__add_new_feature.sql. Quarkus will automatically apply it on the next application start.
