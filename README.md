# RentVideo Advanced — Video Rental System

RESTful Spring Boot API for an online video rental service with **stateless JWT authentication**, **HTTP Basic Auth login**, **role-based authorization** (`CUSTOMER` / `ADMIN`), and **MySQL** (or H2 for local demos).

## Features

- User registration with BCrypt password hashing (default role: `CUSTOMER`)
- Login via JSON body **or** HTTP Basic Auth → returns a JWT
- Video catalog browse (authenticated users)
- Video CRUD (ADMIN only)
- Rent / return videos (max **2** active rentals per user)
- Global exception handling with meaningful HTTP status codes

## Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 3.4 |
| Security | Spring Security + JWT (JJWT) + BCrypt |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8 (prod) / H2 (default local) |
| Build | Maven |
| Java | 21+ |

## Project Structure

```
src/main/java/com/crio/rentvideo/
├── config/          # Security configuration
├── controller/      # REST controllers
├── dto/             # Request / response payloads
├── entity/          # JPA entities (User, Video, Rental)
├── exception/       # Custom exceptions + global handler
├── repository/      # Spring Data repositories
├── security/        # JWT filter, JwtService, UserDetails
└── service/         # Business logic
```

## Database Schema (Design Choices)

```
users (id, email UNIQUE, password, first_name, last_name, role)
videos (id, title, director, genre, available)
rentals (id, user_id FK, video_id FK, rented_at, returned_at, active)
```

- **`users.role`**: enum `CUSTOMER` | `ADMIN` stored as string.
- **`videos.available`**: flipped to `false` on rent and back to `true` on return.
- **`rentals`**: tracks active vs returned rentals; enforces max 2 active per user in the service layer.
- Foreign keys ensure rentals always reference a valid user and video.

**Who can rent?** Both `CUSTOMER` and `ADMIN` may rent/return. Catalog management remains ADMIN-only. This keeps ops/testing simple while still enforcing the business rental limit for every authenticated renter.

## Getting Started

### Prerequisites

- JDK 21+
- Maven 3.9+ (or use the included `./mvnw`)
- MySQL 8+ (only if using the `mysql` profile)

### Run with H2 (default — no MySQL required)

```bash
./mvnw spring-boot:run
```

H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)  
JDBC URL: `jdbc:h2:mem:rentvideo`

### Run with MySQL

1. Create DB user/password (defaults in `application-mysql.properties`: `root` / `root`).
2. Start the app:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

Schema is auto-created via `spring.jpa.hibernate.ddl-auto=update`.

### Run tests

```bash
./mvnw test
```

## Authentication Flow

1. **Register** (public): `POST /api/auth/register`
2. **Login** (public):
   - JSON: `POST /api/auth/login` with `{ "email", "password" }`
   - Basic Auth: `POST /api/auth/login/basic` with `Authorization: Basic base64(email:password)`
3. Use the returned JWT on private endpoints:

```http
Authorization: Bearer <jwt>
```

## API Endpoints

### Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register user |
| POST | `/api/auth/login` | Login (JSON) → JWT |
| POST | `/api/auth/login/basic` | Login (Basic Auth) → JWT |

### Videos (authenticated)

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| GET | `/api/videos` | Any authenticated | List available videos (`?availableOnly=false` for all) |
| GET | `/api/videos/{id}` | Any authenticated | Get video by id |
| POST | `/api/videos` | ADMIN | Create video |
| PUT | `/api/videos/{id}` | ADMIN | Update video |
| DELETE | `/api/videos/{id}` | ADMIN | Delete video |

### Rentals

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| POST | `/api/videos/{videoId}/rent` | CUSTOMER, ADMIN | Rent a video |
| POST | `/api/videos/{videoId}/return` | CUSTOMER, ADMIN | Return a video |
| POST | `/api/books/{videoId}/return` | CUSTOMER, ADMIN | Alias (matches assignment wording) |
| GET | `/api/rentals/me` | CUSTOMER, ADMIN | List my active rentals |

## Sample Requests

### Register ADMIN

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@rentvideo.com","password":"admin123","firstName":"Ada","lastName":"Admin","role":"ADMIN"}'
```

### Register CUSTOMER

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@rentvideo.com","password":"cust123","firstName":"Chris","lastName":"Customer"}'
```

### Login (JSON) and capture token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@rentvideo.com","password":"admin123"}'
```

### Login (Basic Auth)

```bash
curl -X POST http://localhost:8080/api/auth/login/basic \
  -u admin@rentvideo.com:admin123
```

### Create a video (ADMIN + JWT)

```bash
curl -X POST http://localhost:8080/api/videos \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Inception","director":"Christopher Nolan","genre":"Sci-Fi"}'
```

### Rent / return

```bash
curl -X POST http://localhost:8080/api/videos/1/rent \
  -H "Authorization: Bearer <TOKEN>"

curl -X POST http://localhost:8080/api/videos/1/return \
  -H "Authorization: Bearer <TOKEN>"
```

## Error Handling

| Scenario | HTTP Status |
|----------|-------------|
| Entity not found | 404 |
| Validation / business rule (max rentals, unavailable video) | 400 |
| Missing / invalid credentials | 401 |
| Authenticated but wrong role | 403 |

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `app.jwt.secret` | HMAC signing secret (≥ 256-bit) | set in `application.properties` |
| `app.jwt.expiration-ms` | Token lifetime | `86400000` (24h) |
| `spring.profiles.active` | `h2` or `mysql` | `h2` |

## Notes

- Passwords are never stored in plain text (BCrypt).
- Sessions are **stateless**; authorization relies on JWT claims + DB-backed `UserDetails`.
- The assignment lists `POST /books/{videoId}/return`; the canonical path is `/api/videos/{videoId}/return`, with `/api/books/{videoId}/return` provided as a compatibility alias.
