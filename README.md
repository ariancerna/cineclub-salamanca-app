# Cineclub Salamanca — Sistema de Reservas Online

Aplicación web para la gestión de reservas y control de aforo del Cineclub de Salamanca (Ate, Lima). Permite a los espectadores reservar butacas de forma gratuita y preordenar productos del minibar, mientras el administrador gestiona funciones y valida ingresos en tiempo real.

**Universidad Tecnológica del Perú — Curso Integrador I: Sistemas Software**

---

## Tecnologías

| Capa | Tecnología |
|------|-----------|
| Backend | Java 21 + Spring Boot 3.4.5 |
| Seguridad | Spring Security + JWT (JJWT 0.12.6) + BCrypt |
| Base de datos | PostgreSQL 16 (Docker) |
| ORM | Spring Data JPA + Hibernate |
| Validación | Jakarta Validation |
| Documentación | Swagger UI / OpenAPI 3.1 |
| Frontend | HTML5 + Tailwind CSS v4 + Vanilla JS |
| Pruebas | JUnit 5 + Mockito (22 tests unitarios) |

---

## Arquitectura

El proyecto sigue el patrón **MVC** con separación en capas y aplica principios **SOLID**:

```
cineclub-salamanca-app/
├── backend/                        # API REST — Spring Boot
│   └── src/main/java/com/cineclubsalamanca/
│       ├── controller/             # Controladores REST
│       ├── service/                # Lógica de negocio
│       ├── repository/             # Capa DAO (Spring Data JPA)
│       ├── entity/                 # Entidades JPA
│       ├── dto/                    # Objetos de transferencia
│       ├── security/               # JWT + Spring Security
│       └── config/                 # Configuración general
├── frontend/                       # Interfaz web estática
│   ├── index.html                  # Cartelera
│   ├── reserva.html                # Selección de asiento + minibar
│   ├── mis-entradas.html           # Historial del usuario
│   ├── admin.html                  # Panel de administración
│   └── src/js/                     # Lógica JavaScript modular
└── docker-compose.yml              # PostgreSQL en Docker
```

---

## Requisitos

- [Java 21](https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.msi)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Python 3 (para servir el frontend)

---

## Cómo ejecutar

### 1. Levantar la base de datos

```bash
docker compose up -d
```

Esto inicia un contenedor PostgreSQL en el puerto `5432` con la base de datos `cineclub`.

### 2. Iniciar el backend

```bash
cd backend
./mvnw spring-boot:run
```

El servidor queda disponible en `http://localhost:8080`

### 3. Iniciar el frontend

```bash
cd frontend
python -m http.server 3000
```

Abrir `http://localhost:3000`

---

## Datos iniciales

Al arrancar por primera vez, el sistema crea automáticamente:

- **Usuario administrador:** `admin@cineclub.com` / `admin1234`
- 8 películas del catálogo clásico
- 8 funciones programadas
- 6 productos del minibar

---

## Endpoints principales

| Método | Endpoint | Acceso |
|--------|----------|--------|
| `POST` | `/api/auth/register` | Público |
| `POST` | `/api/auth/login` | Público |
| `GET` | `/api/peliculas` | Público |
| `GET` | `/api/funciones` | Público |
| `GET` | `/api/productos` | Público |
| `POST` | `/api/reservas` | Autenticado |
| `GET` | `/api/reservas/mis-reservas` | Autenticado |
| `GET` | `/api/reservas/funcion/{id}` | Admin |
| `PATCH` | `/api/reservas/{codigo}/confirmar-ingreso` | Admin |
| `POST` | `/api/peliculas` | Admin |
| `POST` | `/api/funciones` | Admin |

Documentación interactiva completa: [`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html)

---

## Pruebas

```bash
cd backend
./mvnw test
```

**22 pruebas unitarias** sobre la capa de servicios (JUnit 5 + Mockito):
- `ReservaServiceTest` — 9 tests (reglas de negocio: aforo, duplicados, butacas)
- `AuthServiceTest` — 5 tests (registro, login, encriptación BCrypt)
- `FuncionServiceTest` — 8 tests (CRUD de funciones)

---

## Integrantes

| Nombre | Código | Rol |
|--------|--------|-----|
| Joan Pelayo Soto | U23311319 | Backend Developer |
| Arian Cerna Martinez | U23200256 | Frontend Developer |
| Fabián Morocho Rosales | U22323551 | Project Manager |
| Fabrizio Santillán Valdiviezo | U20229814 | Analista Funcional |
| Gianmarco Chávez Mejía | U23246322 | Testing y Documentación |
