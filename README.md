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
- JWT token generation
- User roles
    - `CUSTOMER`
    - `ADMIN`

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