# Cineclub Salamanca — Online Booking System

Web application for managing seat reservations and capacity control for Cineclub Salamanca (Ate, Lima). It lets moviegoers reserve seats for free and pre-order minibar items, while administrators manage screenings and validate check-ins in real time.

Universidad Tecnológica del Perú — Integrating Course I: Software Systems

## Technologies

Backend: Java 21 + Spring Boot 3.4.5. Security: Spring Security + JWT (JJWT 0.12.6) + BCrypt. Database: PostgreSQL 15 (Docker), H2 for tests. ORM: Spring Data JPA + Hibernate. Validation: Jakarta Validation. Documentation: Swagger UI / OpenAPI 3.1 and Javadoc. Frontend: HTML5 + Tailwind CSS v4 + vanilla JS. Testing: JUnit 5 + Mockito + AssertJ (79 tests). Coverage: JaCoCo. Dependency scanning: OWASP Dependency-Check. Monitoring: Spring Boot Actuator + Micrometer. Deployment: Maven + multi-stage Docker.

## Documentation

The project report and annexes live in `docs/`, written in Spanish since that is the language of the UTP submission: [architecture and diagrams](docs/ARQUITECTURA.md), [software testing report](docs/INFORME_PRUEBAS.md), [security testing report](docs/INFORME_SEGURIDAD.md), [deployment plan](docs/PLAN_DESPLIEGUE.md), [monitoring plan](docs/PLAN_MONITOREO.md), [maintenance plan](docs/PLAN_MANTENIMIENTO.md), and [references](docs/REFERENCIAS.md).

## Architecture

The project follows the MVC pattern with layered separation and applies SOLID principles.

```
cineclub-salamanca-app/
├── backend/                # REST API — Spring Boot
│   ├── Dockerfile          # Multi-stage image (Maven build → JRE)
│   └── src/
│       ├── main/java/com/cineclubsalamanca/
│       │   ├── controller/ # REST controllers
│       │   ├── service/    # Business logic
│       │   ├── repository/ # DAO layer (Spring Data JPA)
│       │   ├── entity/     # JPA entities
│       │   ├── dto/        # Data transfer objects
│       │   ├── security/   # JWT + Spring Security
│       │   ├── monitoring/ # Health probes and business metrics
│       │   ├── maintenance/# Scheduled tasks (cron jobs)
│       │   └── config/     # General configuration
│       └── test/java/com/cineclubsalamanca/
│           ├── service/    # Unit tests (63)
│           └── integration/# End-to-end security tests (16)
├── frontend/                # Static web interface
│   ├── index.html          # Movie listing
│   ├── reserva.html        # Seat selection + minibar
│   ├── mis-entradas.html   # User history
│   ├── admin.html          # Admin panel
│   └── src/js/             # Modular JavaScript logic
├── scripts/                 # Operations: backup, restore, healthcheck, crontab
├── docs/                    # Architecture, reports and plans
└── docker-compose.yml       # PostgreSQL + backend + frontend
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

Before any public deployment you must generate your own `JWT_SECRET` with `openssl rand -base64 48`. With the sample value anyone can sign administrator tokens (see OBS-03 in the [security report](docs/INFORME_SEGURIDAD.md)).

### 1. Start the database

```bash
docker compose up -d db
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
./mvnw test                  # 79 tests
./mvnw verify                # tests + coverage + 70% threshold
./mvnw verify -Pseguridad    # dependency vulnerability scan
```

79 tests in total: 63 unit tests (JUnit 5 + Mockito) and 16 integration tests. ReservaServiceTest has 17 cases for business rules (capacity, duplicates, seats, minibar subtotals), SeguridadIntegrationTest has 16 for JWT, roles, validation and SQL injection, FuncionServiceTest has 13 for screening CRUD and rescheduling, TareasMantenimientoTest has 10 for the capacity audit and purge jobs, PeliculaServiceTest and ProductoServiceTest have 9 each, and AuthServiceTest has 5 for registration, login and BCrypt encryption.

Service layer coverage is 100% of lines and branches. The report is generated at `backend/target/site/jacoco/index.html`. Details in the [testing report](docs/INFORME_PRUEBAS.md).

Javadoc is generated with `./mvnw javadoc:javadoc` into `backend/target/reports/apidocs/index.html`.

## Monitoring

`/actuator/health` and `/actuator/info` are public, though health details are only shown to administrators. `/actuator/metrics` and `/actuator/prometheus` require the admin role. Besides the standard JVM and HTTP metrics, the application publishes business metrics: total reservations, check-ins, upcoming screenings, available seats and registered users.

Logs are written to `logs/cineclub.log` with daily rotation, gzip compression and 30 days of history. See the [monitoring plan](docs/PLAN_MONITOREO.md).

## Maintenance

```bash
./scripts/backup.sh          # backup with retention (Windows: backup.ps1)
./scripts/restore.sh --ultimo
./scripts/healthcheck.sh
```

Scheduled tasks run inside the application: a daily operations report, a weekly audit that recalculates screening capacity and fixes drift, and a monthly purge of old reservations. Infrastructure jobs (backup, health check, restore drill, log cleanup) run from cron, see `scripts/crontab.example`. Details in the [maintenance plan](docs/PLAN_MANTENIMIENTO.md).

## Deployment

```bash
# Development: database only
docker compose up -d db

# Full deployment: database + backend + frontend
docker compose --profile completo up -d --build
```

The backend image is built in two stages: Maven compiles it and the final image keeps only the JAR on a JRE, running as an unprivileged user. See the [deployment plan](docs/PLAN_DESPLIEGUE.md).

## Team members

Joan Pelayo Soto (Backend Developer), Arian Cerna Martinez (Frontend Developer), Fabián Morocho Rosales (Project Manager), Fabrizio Santillán Valdiviezo (Functional Analyst), and Gianmarco Chávez Mejía (Testing and Documentation).
