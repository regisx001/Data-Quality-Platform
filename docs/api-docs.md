# dQul — API Reference

> Base URL: `http://localhost:7000`  
> Authentication: JWT Bearer token via `Authorization: Bearer <token>` header

---

## Table of Contents

- [Authentication](#authentication) — register, login, verify, current user
- [Users](#users) — CRUD + activate/deactivate
- [Common Error Responses](#common-error-responses)
- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)

---

## Authentication

All auth endpoints are **public** (`/api/v1/auth/**`). No token required.

### POST `/api/v1/auth/register`

Create a new user account.

**Request body:**

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "strongPass123",
  "fullName": "John Doe",
  "role": "USER"
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `username` | string | yes | — | Unique username (3–50 chars) |
| `email` | string | yes | — | Unique email address |
| `password` | string | yes | — | Password (min 8 chars) |
| `fullName` | string | yes | — | Display name |
| `role` | string | no | `"USER"` | Role (`USER`, `ADMIN`, etc.) |

**Response `201 Created`:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400000,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER"
}
```

**Response `400 Bad Request`:**

```json
{
  "status": 400,
  "message": "Username 'johndoe' is already taken"
}
```

| Scenario | Status | Message |
|----------|--------|---------|
| Username already exists | 400 | `Username '<name>' is already taken` |
| Email already in use | 400 | `Email '<email>' is already in use` |

---

### POST `/api/v1/auth/login`

Authenticate with username or email.

**Request body:**

```json
{
  "login": "johndoe",
  "password": "strongPass123"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `login` | string | yes | Username **or** email (if it contains `@`, treated as email) |
| `password` | string | yes | Raw password |

**Response `200 OK`:** Same shape as register response.

**Error responses:**

| Scenario | Status | Message |
|----------|--------|---------|
| Wrong password | 401 | `Invalid credentials` |
| Account deactivated | 401 | `User account is deactivated` |
| User not found | 401 | `Invalid credentials` |

> **Note:** On successful login, the `lastLoginAt` timestamp is updated automatically.

---

### POST `/api/v1/auth/verify`

Check whether a JWT token is still valid (not expired).

**Request body:**  
Raw plain-text token string.

```
eyJhbGciOiJIUzI1NiJ9...
```

**Response `200 OK`:** Empty body — token is valid.

**Response `401 Unauthorized`:**

```json
{
  "status": 401,
  "message": "Token is invalid or expired"
}
```

---

### GET `/api/v1/auth/me`

Return the currently authenticated user's profile.

**Headers:** `Authorization: Bearer <token>`

**Response `200 OK`:**

```json
{
  "token": null,
  "expiresIn": 0,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER"
}
```

**Response `401 Unauthorized`:** Returned if no valid token is provided.

---

## Users

All user endpoints require **authentication** (`Authorization: Bearer <token>`).  
Unverified accounts will receive a `403 Forbidden` response.

### GET `/api/v1/users`

List all registered users.

**Response `200 OK`:**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "johndoe",
    "email": "john@example.com",
    "passwordHash": "{bcrypt}$2a$10$...",
    "fullName": "John Doe",
    "role": "USER",
    "active": true,
    "verified": false,
    "createdAt": "2026-07-27T12:00:00",
    "lastLoginAt": null,
    "notifications": [],
    "enabled": true,
    "accountNonExpired": true,
    "accountNonLocked": true,
    "credentialsNonExpired": true,
    "authorities": [{ "authority": "ROLE_USER" }]
  }
]
```

> **Note:** The response includes `UserDetails` fields (`enabled`, `authorities`, etc.) inherited from Spring Security's `UserDetails` interface.

---

### GET `/api/v1/users/{id}`

Get a single user by UUID.

**Path parameter:** `id` — UUID of the user

**Response `200 OK`:** Single user object.

**Response `404 Not Found`:**

```json
{
  "status": 404,
  "message": "User not found with id: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### GET `/api/v1/users/by-username/{username}`

Get a single user by username.

**Path parameter:** `username` — String

**Response `200 OK`:** Single user object.

**Response `404 Not Found`:** User not found.

---

### GET `/api/v1/users/by-email/{email}`

Get a single user by email address.

**Path parameter:** `email` — String

**Response `200 OK`:** Single user object.

**Response `404 Not Found`:** User not found.

---

### PUT `/api/v1/users/{id}`

Update user profile fields. Only non-null fields are updated.

**Path parameter:** `id` — UUID of the user

**Request body:**

```json
{
  "email": "newemail@example.com",
  "fullName": "New Full Name",
  "role": "ADMIN"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string or null | no | New email (checked for uniqueness) |
| `fullName` | string or null | no | New display name |
| `role` | string or null | no | New role |

**Response `200 OK`:** Updated user object.

**Error responses:**

| Scenario | Status | Message |
|----------|--------|---------|
| User not found | 404 | `User not found with id: ...` |
| Email already in use | 400 | `Email '...' is already in use` |

---

### DELETE `/api/v1/users/{id}`

Delete a user by ID.

**Path parameter:** `id` — UUID of the user

**Response `204 No Content`:** Success — no body.

**Response `404 Not Found`:** User not found.

---

### PATCH `/api/v1/users/{id}/activate`

Re-activate a deactivated user account.

**Response `200 OK`:** Updated user object with `"active": true`.

**Response `404 Not Found`:** User not found.

---

### PATCH `/api/v1/users/{id}/deactivate`

Deactivate (lock) a user account. Deactivated users cannot authenticate.

**Response `200 OK`:** Updated user object with `"active": false`.

**Response `404 Not Found`:** User not found.

---

## Common Error Responses

All error responses follow a consistent shape:

```json
{
  "status": 401,
  "message": "Description of the error"
}
```

### HTTP Status Codes Used

| Code | Meaning | Used When |
|------|---------|-----------|
| `200` | OK | Success |
| `201` | Created | Resource created (register) |
| `204` | No Content | Resource deleted |
| `400` | Bad Request | Duplicate username/email |
| `401` | Unauthorized | Missing/invalid token, wrong credentials |
| `403` | Forbidden | Account not verified |
| `404` | Not Found | User not found |

---

## Architecture Overview

```
┌──────────────┐      ┌─────────────────┐      ┌──────────────────┐
│              │      │                 │      │                  │
│  Controller  │─────▶│   Service       │─────▶│   Repository     │
│  (REST)      │      │   (Business     │      │   (Data Access)  │
│              │      │    Logic)        │      │                  │
└──────────────┘      └─────────────────┘      └──────────────────┘
                              │
                              ▼
                      ┌─────────────────┐
                      │   JwtService     │
                      │   (Token Mgmt)   │
                      └─────────────────┘
```

### Security Filter Chain

```
Request
  │
  ├── JwtAuthenticationFilter      — Extract & validate JWT, set SecurityContext
  ├── AccountVerificationFilter    — Block unverified accounts (403)
  ├── UsernamePasswordAuth Filter  — Standard Spring Security filter
  │
  ▼
Controller
```

### Dependency Separation

| Service | Responsibility |
|---------|---------------|
| `AuthenticationService` | Register, login, token verification |
| `UserService` | Profile CRUD, activate/deactivate, existence checks |
| `JwtService` | Generate, parse, validate JWT tokens |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 (Spring Boot 3.x lineage) |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL (prod) / H2 (tests) |
| Migrations | Flyway |
| Build | Maven |
| Auth | JWT Bearer tokens, `{bcrypt}` password encoding |

---

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | JDBC URL | `jdbc:postgresql://localhost:3452/dqul` |
| `DATABASE_USERNAME` | DB user | `postgres` |
| `DATABASE_PASSWORD` | DB password | `postgres` |
| `DATABASE_DRIVER` | JDBC driver class | `org.postgresql.Driver` |
| `JWT_SECRET_KEY` | Base64-encoded HMAC-SHA key | `TXlTdXBlclNlY3JldEtleUZv...` |
| `JWT_EXPIRATION_TIME` | Token lifetime (ms) | `86400000` (24h) |
