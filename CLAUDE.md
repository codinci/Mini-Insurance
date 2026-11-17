# CLAUDE.md

Guidance for working in this repository.

## Project

Mini Insurance Claims Management System: a backend-only portfolio project (no frontend).

- Java 21, Spring Boot 4.1.0 (parent POM), Maven (use `mvnw` / `mvnw.cmd`)
- Spring Web MVC, Spring Security (stateless, JWT), Spring Data JPA, PostgreSQL
- Lombok (annotation processing is configured in `pom.xml`)
- springdoc-openapi 2.8.13 for Swagger UI

Base package: `com.example.miniinsurance`

## Commands

- Build: `./mvnw clean package` (Windows: `mvnw.cmd clean package`)
- Run: `./mvnw spring-boot:run`
- Test: `./mvnw test`

The app needs a running PostgreSQL database and the environment variables below, or it will fail to start.

## Configuration

`src/main/resources/application.properties` reads everything from environment variables:

- `DB_NAME`, `DB_USER`, `DB_PASSWORD`: PostgreSQL database `mini_insurance` on `localhost:5432`
- `PORT`: server port (README documents `8082`)
- `JWT_SECRET`: Base64-encoded signing key
- `JWT_EXPIRATION`: token lifetime in milliseconds
- `DATA_ENCRYPTION_KEY`: Base64 AES key (16/24/32 bytes) for encrypting quote rating inputs. Read by `EncryptedStringConverter` on first use, not from `application.properties`. Quote creation fails closed if it is unset.

Do not hardcode credentials or secrets in source or in properties files. `application.properties` is listed in `.gitignore`.

Schema is managed by Hibernate with `ddl-auto=update` (development setting).

## Architecture

Feature-based packages under `com.example.miniinsurance`:

- `auth/`: `AuthController`, `AuthService`, register/login DTOs, `AuthResponse`. Public endpoints under `/api/auth/**`.
- `user/`: `User`, `Role` (`CUSTOMER`, `ADMIN`), `UserRepository`
- `security/`: `SecurityConfig` (filter chain, authorization rules), `JwtService`, `JwtConfig`, `JwtAuthenticationFilter`, `CustomUserDetailsService`
- `customer/`: `CustomerController` at `/api/customer`, requires `ROLE_CUSTOMER`
- `admin/`: `AdminController` at `/api/admin`, requires `ROLE_ADMIN`
- `policy/`: `Policy`, `PolicyType`, `PolicyStatus`, `PolicyRepository`, `PolicyService`, `PolicyController`, DTOs in `policy/dto/`. Endpoint `POST /api/customer/policies` purchases a policy from a stored quote (`{quoteId, startDate}`).
- `quote/`: `Quote`, `QuoteStatus`, `QuoteRepository`, `QuoteService`, `QuoteController` (`/api/customer/quotes`), `AdminQuoteController` (`/api/admin/quotes`), `QuoteRetentionJob` (scheduled purge). DTOs in `quote/dto/` (polymorphic request discriminated by `policyType`). Pricing lives in `quote/rating/`: one `PremiumCalculator` per type plus shared `RatingMath`. Spec: `docs/specs/premium-quotes.md`.
- `common/`: `HealthController` (`/api/health`), `ApiExceptionHandler` (global `@RestControllerAdvice`, error body `ApiErrorResponse`), `PageResponse`, `exception/` (domain exceptions mapped to 400/404/409/410/422), `crypto/EncryptedStringConverter` (AES-GCM JPA converter)
- `config/`: `OpenApiConfig`, `PasswordConfig` (BCrypt), `ClockConfig` (injectable `Clock`), `JacksonConfig` (rejects unknown JSON properties), `SchedulingConfig`

### Authorization rules

Defined in `SecurityConfig`:

- `/api/auth/**`, `/api/health`, Swagger paths: public
- `/api/admin/**`: `ADMIN`
- `/api/customer/**`: `CUSTOMER`
- Everything else: authenticated

Unauthenticated requests return `401` and access-denied returns `403`. Both are written directly by `SecurityConfig.writeError` as a JSON body. Do not use `response.sendError` there: the error re-dispatch turns a 403 for an authenticated user into a 401.

Registration always assigns `CUSTOMER`. Policies are created by the authenticated customer, identified by `authentication.getName()` (the email).

### Conventions

- Controllers are thin; business logic lives in `*Service` classes.
- Request/response types are records in `dto/` subpackages.
- Controllers document endpoints with springdoc annotations (`@Operation`, `@ApiResponses`, `@SecurityRequirement(name = "bearerAuth")`). Keep these on new endpoints.
- Entities use Lombok (`@Builder` is used on `Policy`).
- Errors: services throw the domain exceptions in `common/exception/`. `ApiExceptionHandler` maps them to HTTP status and a consistent body. Do not return stack traces, SQL or submitted values in error responses.
- Request bodies reject unknown JSON properties (`JacksonConfig`), so extra fields such as a client-supplied premium fail with `400`.

## Known inconsistencies

- README says the main class is `MiniInsuranceClaimsApplication`; the actual class is `MiniInsuranceApplication`.
- `CustomerController.profile` uses `System.out.println` for debugging. Prefer a logger if you touch it.
- `logging.level.org.springframework.security=DEBUG` is enabled in properties.

## Git

- Main branch: `main`
- Recent work is on `policy/` (in progress: staged modifications under `src/main/java/com/example/miniinsurance/policy/`).
