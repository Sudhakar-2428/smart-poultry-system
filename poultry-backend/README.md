# Smart Poultry Management System - Backend Service

Enterprise Spring Boot backend REST API powering the Smart Poultry Management System. Handles flock lifecycle, egg collection, hatching incubation, brooder temperature cohorts, feed inventory, double-entry financial ledger accounting, reverse geocoding, ultra-efficient weather engine caching, and multi-farm role-based access control.

---

## Technical Stack & Requirements

* **Java Version**: OpenJDK / Oracle JDK 21+
* **Framework**: Spring Boot 3.3.4
* **Database**: MySQL 8.0+
* **Database Migration**: Flyway
* **Security & Auth**: Spring Security 6, Stateless JWT Authentication (HS512)
* **Build Tool**: Apache Maven (Wrapper included `./mvnw`)
* **Caching Engine**: Caffeine In-Memory Cache (15-minute SWR Weather Engine)
* **API Documentation**: OpenAPI 3.0 / Springdoc Swagger UI

---

## Environment Variables Configuration

The application uses environment variables with default local fallbacks. Copy `src/main/resources/application-example.properties` to set up custom local properties or define the environment variables below:

| Environment Variable | Description | Default Local Value |
|---|---|---|
| `PORT` | HTTP Server Port | `8080` |
| `DB_HOST` | MySQL Database Hostname | `localhost` |
| `DB_PORT` | MySQL Database Port | `3306` |
| `DB_NAME` | Database Schema Name | `poultry_management` |
| `DB_USERNAME` | Database User | `root` |
| `DB_PASSWORD` | Database Password | *(Set locally)* |
| `JWT_SECRET` | Base64-encoded Secret Key for JWT (Min 64 chars) | *(Base64 Secret Key)* |
| `JWT_EXPIRATION_MS` | Token Expiration in milliseconds | `86400000` (24 Hours) |
| `CORS_ALLOWED_ORIGINS` | Permitted Frontend Origins | `http://localhost:3000,http://localhost:5173` |

---

## Build Instructions

1. **Verify Prerequisites**:
   Ensure Java 21+ and MySQL 8+ are installed and running locally.

2. **Compile the Project**:
   ```bash
   ./mvnw clean compile
   ```

3. **Run Unit & Integration Tests**:
   ```bash
   ./mvnw test
   ```

---

## Running the Application

1. **Create MySQL Database**:
   ```sql
   CREATE DATABASE IF NOT EXISTS poultry_management;
   ```

2. **Start Backend Server**:
   ```bash
   ./mvnw spring-boot:run
   ```
   *The server runs at `http://localhost:8080/api/v1`.*

---

## Interactive API Documentation (Swagger)

Once the application is running, access Swagger UI to explore and test all API endpoints interactively:

* **Swagger UI**: [http://localhost:8080/api/v1/swagger-ui/index.html](http://localhost:8080/api/v1/swagger-ui/index.html)
* **OpenAPI Spec**: [http://localhost:8080/api/v1/v3/api-docs](http://localhost:8080/api/v1/v3/api-docs)

---

## Database Migrations (Flyway)

Database schema migrations are automatically managed by Flyway during application startup under `src/main/resources/db/migration/`:
* `V2__create_farm_and_farm_member.sql`: Farm & Membership schemas.
* `V3__add_email_verification.sql`: Owner registration & email verification schema.
* `V4__add_farm_location_coordinates.sql`: Farm location coordinates & reverse geocoding schema.
