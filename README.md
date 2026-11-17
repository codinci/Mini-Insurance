# Mini Insurance Claims Management System

A backend-first portfolio project built with Spring Boot and PostgreSQL.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT
- Lombok
- Swagger / OpenAPI

## Running the project

1. Create PostgreSQL database `mini_insurance`
2. Configure credentials in `application.properties`
3. Run `MiniInsuranceClaimsApplication`

The application runs on:

`http://localhost:8082`

## Swagger

Swagger UI:

`http://localhost:8082/swagger-ui.html`

OpenAPI specification:

`http://localhost:8082/v3/api-docs`

## Current Features

- Project setup
- PostgreSQL connection
- Swagger / OpenAPI documentation
- Health endpoint
- User registration
- User authentication
- BCrypt password hashing
- JWT token generation and validation
- User roles
  - `CUSTOMER`
  - `ADMIN`
- Role-based authorization
- Protected customer endpoints
- Protected admin endpoints
- Documented `401 Unauthorized` and `403 Forbidden` responses
- Premium quotes for MOTOR, HEALTH, HOME, TRAVEL and LIFE
- Quote-then-purchase policy flow
- Encrypted storage of rating inputs (AES-GCM)

## Premium Quotes

Premiums are calculated on the server from risk details. Clients cannot supply a premium.

1. `POST /api/customer/quotes`: submit type-specific rating inputs. Returns a quote with an annual premium (or `DECLINED` with a reason), an itemised breakdown and an expiry 30 days out.
2. `POST /api/customer/policies`: purchase with `{"quoteId": 1, "startDate": "2027-01-01"}`. Type, coverage and premium come from the quote. Each quote can be purchased once.
3. `GET /api/customer/quotes` and `GET /api/customer/quotes/{id}`: view your own quotes.
4. `GET /api/admin/quotes` and `GET /api/admin/quotes/{id}`: admin view, filterable by `status` and `policyType`.

The `policyType` field in the quote request selects the required fields. Unknown fields are rejected with `400`.

Rating inputs are stored encrypted. Set `DATA_ENCRYPTION_KEY` to a Base64-encoded 16, 24 or 32-byte key before creating quotes, for example:

```bash
openssl rand -base64 32
```

Inputs of unpurchased quotes are purged 90 days after they expire.

## Authentication

### User Registration

New users can register through:

`POST /api/auth/register`

Newly registered users are automatically assigned the `CUSTOMER` role.

Example request:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "password123"
}