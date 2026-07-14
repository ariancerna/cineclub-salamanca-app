# Cineclub Salamanca — Online Booking System

Web application for managing seat reservations and capacity control for Cineclub Salamanca (Ate, Lima). It lets moviegoers reserve seats for free and pre-order minibar items, while administrators manage screenings and validate check-ins in real time.

Universidad Tecnológica del Perú — Integrating Course I: Software Systems

## Technologies

Backend: Java 21 + Spring Boot 3.4.5. Security: Spring Security + JWT (JJWT 0.12.6) + BCrypt. Database: PostgreSQL 16 (Docker). ORM: Spring Data JPA + Hibernate. Validation: Jakarta Validation. Documentation: Swagger UI / OpenAPI 3.1. Frontend: HTML5 + Tailwind CSS v4 + vanilla JS. Testing: JUnit 5 + Mockito (22 unit tests).

## Architecture

The project follows the MVC pattern with layered separation and applies SOLID principles.

```
cineclub-salamanca-app/
├── backend/                # REST API — Spring Boot
│   └── src/main/java/com/cineclubsalamanca/
│       ├── controller/     # REST controllers
│       ├── service/        # Business logic
│       ├── repository/     # DAO layer (Spring Data JPA)
│       ├── entity/         # JPA entities
│       ├── dto/            # Data transfer objects
│       ├── security/       # JWT + Spring Security
│       └── config/         # General configuration
├── frontend/                # Static web interface
│   ├── index.html          # Movie listing
│   ├── reserva.html        # Seat selection + minibar
│   ├── mis-entradas.html   # User history
│   ├── admin.html          # Admin panel
│   └── src/js/             # Modular JavaScript logic
└── docker-compose.yml       # PostgreSQL in Docker
```

## Requirements

Java 21, Docker Desktop, and Python 3 (to serve the frontend).

## How to run it

### 0. Set up environment variables

Copy `.env.example` as `.env`:

```bash
cp .env.example .env
```

This file holds all sensitive variables (DB credentials, JWT secret, etc.) and is not tracked in git for security reasons.

### 1. Start the database

```bash
docker compose up -d
```

This starts a PostgreSQL container on port 5432 with the `cineclub` database. Credentials are loaded automatically from `.env`.

### 2. Start the backend

Environment variables are loaded automatically from `.env` before startup.

Option A (Windows - Cmd):

```bash
cd backend
iniciar.cmd
```

Option B (Git Bash / Linux / macOS):

```bash
cd backend
chmod +x iniciar.sh
./iniciar.sh
```

Option C (PowerShell):

```bash
cd backend
.\iniciar.ps1
```

Option D (manual — load variables and run Maven):

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The server will be available at `http://localhost:8080`.

### 3. Start the frontend

```bash
cd frontend
python -m http.server 3000
```

Open `http://localhost:3000`.

## Seed data

On first startup, the system automatically creates an administrator account (`admin@cineclub.com` / `admin1234`), 8 classic catalog movies, 8 scheduled screenings, and 6 minibar products.

## Main endpoints

Public endpoints include registration and login (`POST /api/auth/register`, `POST /api/auth/login`) and read-only access to movies, screenings and products (`GET /api/peliculas`, `GET /api/funciones`, `GET /api/productos`). Authenticated users can create and view their own reservations (`POST /api/reservas`, `GET /api/reservas/mis-reservas`). Admin-only endpoints include viewing reservations per screening, confirming check-ins, and managing movies and screenings (`GET /api/reservas/funcion/{id}`, `PATCH /api/reservas/{codigo}/confirmar-ingreso`, `POST /api/peliculas`, `POST /api/funciones`).

Full interactive documentation: `http://localhost:8080/swagger-ui/index.html`.

## Tests

```bash
cd backend
./mvnw test
```

22 unit tests covering the service layer (JUnit 5 + Mockito): ReservaServiceTest with 9 tests for business rules (capacity, duplicates, seats), AuthServiceTest with 5 tests for registration, login and BCrypt encryption, and FuncionServiceTest with 8 tests for screening CRUD operations.

## Team members

Joan Pelayo Soto (Backend Developer), Arian Cerna Martinez (Frontend Developer), Fabián Morocho Rosales (Project Manager), Fabrizio Santillán Valdiviezo (Functional Analyst), and Gianmarco Chávez Mejía (Testing and Documentation).
