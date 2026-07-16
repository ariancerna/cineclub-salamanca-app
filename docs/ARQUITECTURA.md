# Arquitectura del sistema

Proyecto CineClub Salamanca — UTP, Curso Integrador I: Sistemas Software.

Este documento describe la estructura MVC, la capa DAO y cómo se aplican los principios
SOLID en el código.

## 1. Visión general

La aplicación tiene tres partes: un frontend estático, una API REST y una base de datos.

```mermaid
flowchart LR
    Nav["Navegador<br/>HTML + JS"]
    Api["API REST<br/>Spring Boot 3.4.5"]
    Db[("PostgreSQL 15")]
    Mon["Actuator<br/>health · metrics"]

    Nav -->|"JSON + JWT"| Api
    Api -->|JDBC| Db
    Api --- Mon

    style Nav fill:#e8f0fe,stroke:#5b8def,color:#1a1a1a
    style Api fill:#e6f4ea,stroke:#34a853,color:#1a1a1a
    style Db fill:#fef7e0,stroke:#f9ab00,color:#1a1a1a
    style Mon fill:#f3e8fd,stroke:#a142f4,color:#1a1a1a
```

La API no guarda sesión (`SessionCreationPolicy.STATELESS`). La identidad viaja en un token
JWT en cada petición, así que el backend se puede reiniciar sin que nadie pierda la sesión.

## 2. Capas

El backend está separado en capas, y cada una solo depende de la siguiente. Entre el
controlador y la vista se usan DTO, no entidades JPA.

```mermaid
flowchart TD
    subgraph Vista
        V["Frontend estático<br/>index · reserva · admin · mis-entradas"]
    end

    subgraph Controlador["Controlador — @RestController"]
        C1[AuthController]
        C2[PeliculaController]
        C3[FuncionController]
        C4[ReservaController]
        C5[ProductoController]
    end

    subgraph Servicio["Lógica de negocio — @Service"]
        S1[AuthService]
        S2[PeliculaService]
        S3[FuncionService]
        S4[ReservaService]
        S5[ProductoService]
    end

    subgraph DAO["Capa DAO — Spring Data JPA"]
        R1[UsuarioRepository]
        R2[PeliculaRepository]
        R3[FuncionRepository]
        R4[ReservaRepository]
        R5[ProductoRepository]
    end

    subgraph Modelo["Modelo — @Entity"]
        M["Usuario · Pelicula · Funcion<br/>Reserva · Producto · DetalleMinibar"]
    end

    V -->|JSON| Controlador
    Controlador -->|DTO| Servicio
    Servicio --> DAO
    DAO --> Modelo
    Modelo --> BD[("PostgreSQL")]
```

Usamos DTO en la frontera HTTP por dos motivos. El primero es de seguridad: los `record` de
`dto/` definen exactamente qué sale, y así `JwtResponse` no puede filtrar el `passwordHash`.
El segundo es práctico: si serializáramos las entidades directamente, Jackson intentaría
recorrer las relaciones `LAZY` de JPA y saltaría `LazyInitializationException`. La
conversión está en los métodos `from(...)` de cada DTO de respuesta.

### Paquetes

| Capa MVC | Paquete | Responsabilidad |
|---|---|---|
| Vista | `frontend/` | Interfaz; consume la API con `fetch` |
| Controlador | `controller/` | Rutas HTTP, códigos de estado, autorización |
| — | `dto/` | Entrada y salida de la API |
| Modelo (negocio) | `service/` | Reglas del dominio y transacciones |
| Modelo (datos) | `repository/` | Capa DAO |
| Modelo (dominio) | `entity/` | Entidades JPA |
| Transversal | `security/`, `config/` | JWT, filtros, CORS, errores |
| Transversal | `monitoring/`, `maintenance/` | Sondas, métricas y tareas programadas |

## 3. Modelo de datos

```mermaid
erDiagram
    USUARIO ||--o{ RESERVA : "realiza"
    FUNCION ||--o{ RESERVA : "recibe"
    PELICULA ||--o{ FUNCION : "se proyecta en"
    RESERVA ||--o{ DETALLE_MINIBAR : "incluye"
    PRODUCTO ||--o{ DETALLE_MINIBAR : "aparece en"

    USUARIO {
        bigint id PK
        string nombre
        string email UK
        string password_hash
        enum rol
    }
    PELICULA {
        bigint id PK
        string titulo
        string director
        int duracion_minutos
    }
    FUNCION {
        bigint id PK
        bigint pelicula_id FK
        datetime fecha_hora
        int aforo_maximo
        int aforo_disponible
        string sala
    }
    RESERVA {
        string codigo_reserva PK
        bigint usuario_id FK
        bigint funcion_id FK
        string numero_butaca
        datetime fecha_emision
        boolean asistio_ingreso
    }
    PRODUCTO {
        bigint id PK
        string nombre
        decimal precio
    }
    DETALLE_MINIBAR {
        bigint id PK
        string reserva_id FK
        bigint producto_id FK
        int cantidad
        decimal subtotal
    }
```

Hay dos cosas del modelo que conviene aclarar:

La clave primaria de `RESERVA` es el `codigo_reserva` (`SLM-` más 8 caracteres) y no un id
autonumérico. Como ese código es el que el espectador muestra en la puerta, lo usamos
directamente como identificador en vez de arrastrar una columna extra.

`aforo_disponible` está denormalizado. Se podría calcular contando reservas, pero lo
guardamos para no repetir esa cuenta cada vez que alguien abre la cartelera. A cambio, el
contador se puede desincronizar, y por eso existe la auditoría semanal que describe el
[plan de mantenimiento](PLAN_MANTENIMIENTO.md).

## 4. Flujo de una reserva

```mermaid
sequenceDiagram
    participant N as Navegador
    participant F as JwtAuthFilter
    participant C as ReservaController
    participant S as ReservaService
    participant R as Repositories
    participant D as PostgreSQL

    N->>F: POST /api/reservas + Bearer JWT
    F->>F: Validar firma y vigencia
    alt Token ausente o inválido
        F-->>N: 401 Credenciales ausentes o inválidas
    end
    F->>C: Petición autenticada
    C->>S: crear(email, request)

    S->>R: findByEmail(email)
    S->>R: buscarPorId(funcionId)
    S->>S: ¿Hay aforo disponible?
    S->>R: ¿El usuario ya reservó?
    S->>R: ¿La butaca está ocupada?

    alt Alguna regla falla
        S-->>N: 409 Conflict
    end

    S->>S: Generar código SLM-XXXXXXXX
    S->>S: Calcular subtotales del minibar
    S->>R: save(reserva) · aforo - 1
    R->>D: INSERT / UPDATE (una transacción)
    S-->>N: 201 Created + ReservaResponse
```

El método `crear` es `@Transactional`, así que si falla el descuento del aforo tampoco se
guarda la reserva.

## 5. Principios SOLID

| Principio | Dónde se ve |
|---|---|
| Responsabilidad única | Cada capa cambia por un solo motivo. `MetricasNegocio` publica la telemetría con un `MeterBinder` en lugar de meter contadores dentro de `ReservaService`, para que las reglas de negocio no se toquen al cambiar el monitoreo. |
| Abierto/cerrado | Para añadir una sonda basta crear un `HealthIndicator`: Actuator la recoge sin modificar nada existente. Igual con un `MeterBinder` o un `@ExceptionHandler`. |
| Sustitución de Liskov | `UserDetailsServiceImpl` cumple el contrato de `UserDetailsService` y Spring Security lo usa sin conocer la implementación. Lo mismo con `EntradaNoAutenticada` y `SinPermisos`, que reemplazan a los manejadores por defecto. |
| Segregación de interfaces | Los repositorios declaran solo los métodos que hacen falta. Los DTO son interfaces estrechas: `PeliculaRequest` no trae campos que el cliente no deba mandar. |
| Inversión de dependencias | Los servicios dependen de interfaces `Repository`, no de `EntityManager`. La inyección es por constructor con campos `final`, lo que además permite instanciarlos con mocks en las pruebas. |

## 6. Seguridad

```mermaid
flowchart TD
    P["Petición HTTP"] --> J["JwtAuthFilter"]
    J -->|"Sin token"| Anon["Contexto anónimo"]
    J -->|"Token válido"| Auth["SecurityContext<br/>con roles"]
    J -->|"Token inválido"| E1

    Anon --> FC["SecurityFilterChain"]
    Auth --> FC

    FC -->|"Ruta pública"| OK["Controlador"]
    FC -->|"Requiere sesión"| E1["EntradaNoAutenticada<br/>401"]
    FC -->|"Requiere rol"| E2["SinPermisos<br/>403"]
    FC -->|"Autorizado"| OK

    OK --> PA["@PreAuthorize"]
    PA --> SV["Servicio"]

    style E1 fill:#fce8e6,stroke:#d93025,color:#1a1a1a
    style E2 fill:#fef7e0,stroke:#f9ab00,color:#1a1a1a
    style OK fill:#e6f4ea,stroke:#34a853,color:#1a1a1a
```

La autorización está en dos sitios: por ruta en `SecurityConfig` y por método con
`@PreAuthorize`. Es redundante a propósito. Si mañana alguien agrega una ruta y se olvida de
cubrirla en la cadena de filtros, la anotación del método todavía protege la operación.

Para que la API distinga 401 de 403 hubo que configurar un `AuthenticationEntryPoint`
propio, porque Spring Security responde 403 en los dos casos. Está explicado en el
[informe de seguridad](INFORME_SEGURIDAD.md).

## 7. Despliegue

```mermaid
flowchart TB
    subgraph Host["Servidor — Docker Compose"]
        subgraph N["cineclub-frontend"]
            NG["nginx:1.27-alpine<br/>:3000"]
        end
        subgraph B["cineclub-backend"]
            SB["JRE 21 · app.jar<br/>perfil prod · :8080"]
        end
        subgraph DB["cineclub-db"]
            PG["postgres:15-alpine<br/>:5432"]
        end
        VOL[("postgres_data")]
        LOGS[("./logs")]
    end

    U["Espectador"] --> NG
    NG -.->|"El navegador llama<br/>directamente a :8080"| SB
    SB -->|"depends_on: service_healthy"| PG
    PG --- VOL
    SB --- LOGS

    style PG fill:#fef7e0,stroke:#f9ab00,color:#1a1a1a
    style SB fill:#e6f4ea,stroke:#34a853,color:#1a1a1a
    style NG fill:#e8f0fe,stroke:#5b8def,color:#1a1a1a
```

nginx solo sirve los archivos estáticos, no hace de proxy inverso. El navegador arma la URL
de la API con el host desde el que cargó la página (ver `api.js`), así que el backend
necesita tener publicado el puerto 8080. El procedimiento completo está en el
[plan de despliegue](PLAN_DESPLIEGUE.md).

## Documentos relacionados

- [Informe de pruebas de software](INFORME_PRUEBAS.md)
- [Informe de pruebas de seguridad](INFORME_SEGURIDAD.md)
- [Plan de despliegue](PLAN_DESPLIEGUE.md)
- [Plan de monitoreo](PLAN_MONITOREO.md)
- [Plan de mantenimiento](PLAN_MANTENIMIENTO.md)
- [Referencias](REFERENCIAS.md)
