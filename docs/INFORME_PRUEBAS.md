# Informe de pruebas de software

Proyecto CineClub Salamanca — UTP, Curso Integrador I: Sistemas Software.

## 1. Conceptos de testing

Probar software es ejecutarlo buscando defectos. Una prueba que pasa no demuestra que el
programa sea correcto, solo que no falló en ese caso concreto. Con ese criterio armamos la
suite: los casos buscan dónde se puede romper, no confirmar que funciona.

### Niveles

| Nivel | Qué verifica | Cómo | Cantidad |
|---|---|---|---|
| Unitario | Una clase aislada, con sus dependencias reemplazadas por mocks | JUnit 5 + Mockito | 63 |
| Integración | Varios componentes reales juntos, incluyendo filtros y base de datos | `@SpringBootTest` + MockMvc + H2 | 16 |
| Total | | | 79 |

### Tipos

- **Caja blanca**: los casos se diseñan mirando el código, y JaCoCo indica qué líneas y
  ramas quedaron sin ejecutar.
- **Caja negra**: los casos de `SeguridadIntegrationTest` se escriben contra el contrato de
  la API (rutas y códigos HTTP), sin mirar cómo está implementada por dentro.
- **Regresión**: la suite corre entera en cada `mvnw verify`, y el umbral de cobertura hace
  fallar la build si aparece un servicio sin pruebas.

### Estructura de los casos

Cada prueba sigue Arrange–Act–Assert. Un ejemplo real de `ReservaServiceTest`:

```java
@Test
@DisplayName("No se puede reservar si el aforo está agotado")
void crear_debeLanzarExcepcion_cuandoAforoAgotado() {
    // Arrange
    Usuario usuario = usuarioBase();
    Funcion funcion = funcionBase();
    funcion.setAforoDisponible(0);
    when(usuarioRepository.findByEmail("joan@test.com")).thenReturn(Optional.of(usuario));
    when(funcionService.buscarPorId(1L)).thenReturn(funcion);

    // Act + Assert
    assertThatThrownBy(() -> reservaService.crear("joan@test.com", req))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No hay butacas disponibles");

    verify(reservaRepository, never()).save(any());
}
```

La última línea es la que le da valor al caso. No alcanza con que lance la excepción:
también hay que comprobar que no dejó datos a medio escribir.

### Mocks

Usamos mocks de Mockito para aislar la clase que se está probando. `ReservaServiceTest`
reemplaza los cinco colaboradores de `ReservaService`, de modo que cuando el test falla
sabemos que el problema está en `ReservaService` y no en la base o en otro servicio. Las
pruebas unitarias no tocan disco ni red y corren en menos de 2 segundos.

## 2. Herramientas

| Herramienta | Versión | Para qué |
|---|---|---|
| JUnit 5 | 5.11 (vía Spring Boot) | Motor de pruebas |
| Mockito | 5.14 | Mocks |
| AssertJ | 3.26 | Aserciones legibles |
| Spring Boot Test | 3.4.5 | Contexto de aplicación en integración |
| Spring Security Test | 6.4 | Utilidades de seguridad |
| MockMvc | — | Peticiones HTTP simuladas sin abrir puerto |
| H2 | 2.3 | Base en memoria para integración |
| JaCoCo | 0.8.12 | Cobertura |

## 3. Cobertura

### Por clase (capa de servicios)

| Clase | Líneas | Ramas |
|---|---:|---:|
| `AuthService` | 100% | 100% |
| `ReservaService` | 100% | 100% |
| `FuncionService` | 100% | sin ramas |
| `PeliculaService` | 100% | sin ramas |
| `ProductoService` | 100% | sin ramas |
| Total `service` | 100% (131/131) | 100% |

### Evolución

Al instrumentar con JaCoCo apareció un dato que no se veía a simple vista: la suite original
tenía 22 pruebas y todas pasaban, pero dejaba la mitad de la lógica de negocio sin ejecutar.

| Momento | Pruebas | Cobertura de servicios |
|---|---:|---:|
| Inicial | 22 | 49% |
| Actual | 79 | 100% |

Huecos que encontramos y cerramos:

| Hueco | Qué se hizo |
|---|---|
| `PeliculaService` sin ninguna prueba (0%) | `PeliculaServiceTest`, 9 casos |
| `ProductoService` sin ninguna prueba (0%) | `ProductoServiceTest`, 9 casos |
| `ReservaService` no probaba el cálculo de subtotales del minibar | 4 casos nuevos |
| `FuncionService` no cubría `listarTodas`, `obtenerPorId` ni `actualizar` | 5 casos nuevos |
| `TareasMantenimiento` sin pruebas, aunque corrige datos en producción | `TareasMantenimientoTest`, 10 casos |

### Umbral

En el `pom.xml`, JaCoCo hace fallar la build si la capa de servicios baja del 70% de líneas:

```xml
<rule>
    <element>PACKAGE</element>
    <includes><include>com.cineclubsalamanca.service</include></includes>
    <limits>
        <limit>
            <counter>LINE</counter>
            <value>COVEREDRATIO</value>
            <minimum>0.70</minimum>
        </limit>
    </limits>
</rule>
```

El umbral aplica solo a `service` porque ahí está la lógica de negocio. Pedirle lo mismo a
las entidades o los DTO mediría sobre todo getters generados por Lombok, con lo que el
número subiría sin que las pruebas mejoraran.

Lo dejamos en 70% aunque hoy estemos en 100%, porque la idea es que sea un piso que impida
retroceder y no una meta. Si lo pusiéramos en 100%, cada línea nueva exigiría su prueba
antes de poder compilar, y eso termina generando pruebas de relleno para contentar al
contador. La cobertura dice qué código se ejecutó, no si las aserciones comprueban algo
útil: sirve para encontrar huecos, no para medir calidad.

## 4. Casos de prueba

### 4.1 `ReservaServiceTest` (17 casos)

Concentra las reglas de negocio más importantes.

| Caso | Qué verifica |
|---|---|
| Reserva exitosa retorna código y datos completos | Camino feliz |
| No se puede reservar si el aforo está agotado | `aforoDisponible <= 0` da 409 |
| Un usuario no puede reservar dos veces la misma función | Unicidad usuario-función |
| No se puede reservar una butaca ya ocupada | Unicidad butaca-función |
| Email inexistente lanza `EntityNotFoundException` | Validación de usuario |
| El admin confirma ingreso y `asistioIngreso` pasa a true | Control de ingreso |
| Código de reserva inválido lanza `EntityNotFoundException` | Validación de código |
| Retorna los códigos de butaca ocupados | Mapa de asientos |
| Lista vacía cuando la función no tiene reservas | Caso borde |
| El subtotal del minibar es precio por cantidad | Cálculo monetario |
| Una reserva sin minibar no consulta el catálogo | Eficiencia |
| Crear una reserva descuenta una butaca del aforo | Consistencia del contador |
| El código generado usa el prefijo `SLM-` | Formato de 12 caracteres |
| `listarMisReservas` devuelve solo las del usuario | Aislamiento entre usuarios |
| `listarMisReservas` con email inexistente | Validación |
| `obtenerPorCodigo` devuelve la reserva | Consulta por código |
| `listarPorFuncion` para el panel admin | Consulta administrativa |

### 4.2 `TareasMantenimientoTest` (10 casos)

Las tareas programadas modifican datos en producción sin que nadie las supervise, así que
merecen pruebas igual que el código que atiende peticiones. La auditoría de aforo es la más
delicada porque corrige registros sola: un error de signo dejaría el aforo peor que antes.

| Caso | Qué verifica |
|---|---|
| Corrige el aforo cuando el contador se desvió | `aforoMaximo − reservas` |
| No escribe si el aforo ya es correcto | Evita escrituras inútiles |
| Trata un aforo nulo como inconsistente | Caso borde |
| Revisa varias funciones y corrige solo las desviadas | Aislamiento |
| Soporta una cartelera vacía | Caso borde |
| Purga las reservas anteriores a la retención | Política de retención |
| No llama a `deleteAll` si no hay nada que purgar | Evita operaciones vacías |
| El límite de purga respeta los meses configurados | Configurabilidad |
| El reporte diario consulta reservas y aforo | Reporte de operación |
| El reporte no falla con aforo nulo | Robustez |

### 4.3 Casos borde

Además del camino feliz probamos:

- Aforo exactamente en 0, el límite inferior del contador.
- Precio 0.00 en un producto de cortesía, que `@DecimalMin("0.00")` sí acepta.
- Lista de minibar en `null` frente a lista vacía, que son dos entradas distintas al mismo
  camino.
- Contraseña de 3 caracteres, justo por debajo del mínimo de 8.
- Reserva de una función que no existe.

### 4.4 Precisión monetaria

Los importes usan `BigDecimal` y no `double`, porque la coma flotante binaria no representa
exacto los decimales (`0.1 + 0.2 != 0.3`). En las aserciones usamos `isEqualByComparingTo` y
no `isEqualTo`, ya que `BigDecimal.equals` considera distintos `17.0` y `17.00`, que son la
misma cantidad con distinta escala:

```java
assertThat(detalles.get(0).getSubtotal()).isEqualByComparingTo("17.00");  // 8.50 × 2
```

## 5. Pruebas de integración

`SeguridadIntegrationTest` levanta todo el contexto de Spring contra H2 en memoria. Acá la
cadena de filtros de seguridad es real y los tokens se firman y validan de verdad.

Estas pruebas fueron las que encontraron el problema de los códigos 401/403 que está en el
[informe de seguridad](INFORME_SEGURIDAD.md). Ninguna prueba unitaria de servicio lo podía
detectar, porque ocurre en los filtros, antes de que la petición llegue al servicio.

El perfil `test` usa H2 y no PostgreSQL para que la suite corra sin Docker y con una base
limpia cada vez. La contra es que H2 no es PostgreSQL: activamos `MODE=PostgreSQL` para
acercar el dialecto, pero un defecto propio del motor real no aparecería acá. Es un
intercambio entre fidelidad y velocidad que asumimos a conciencia.

## 6. Ejecución

```bash
cd backend

# Solo pruebas
./mvnw test

# Pruebas + cobertura + umbral
./mvnw verify
```

El reporte HTML queda en `backend/target/site/jacoco/index.html`.

### Última corrida

```
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- SeguridadIntegrationTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- TareasMantenimientoTest
[INFO] Tests run: 5,  Failures: 0, Errors: 0, Skipped: 0 -- AuthServiceTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- FuncionServiceTest
[INFO] Tests run: 9,  Failures: 0, Errors: 0, Skipped: 0 -- PeliculaServiceTest
[INFO] Tests run: 9,  Failures: 0, Errors: 0, Skipped: 0 -- ProductoServiceTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0 -- ReservaServiceTest
[INFO] Tests run: 79, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Verificación con el despliegue real

Además de la suite, levantamos el stack completo con `docker compose --profile completo up
-d --build`: PostgreSQL 15, el backend con perfil `prod` y nginx sirviendo el frontend.

| Comprobación | Resultado |
|---|---|
| Arranque del backend con perfil `prod` | `Started CineclubSalamancaApplication in 11.32 seconds` |
| `ddl-auto=validate` contra el esquema existente | Pasa, sin errores de validación |
| `/actuator/health` sin autenticar | `{"status":"UP"}`, sin exponer componentes |
| `/actuator/metrics` sin token | HTTP 401 |
| `/actuator/health` como admin | `cartelera: UP`, leyendo PostgreSQL real |
| Frontend en nginx | HTTP 200 |
| Reserva real con minibar | Código `SLM-77DC0E7F`, aforo 32 a 31, subtotal 2 × 35.00 = 70.00 |
| `healthcheck.sh` | `OK: la aplicación responde UP`, salida 0 |
| `backup.sh` | Volcado de 4.0K generado, retención aplicada |
| Restauración sobre base de ensayo | Las 6 tablas y la reserva `SLM-77DC0E7F` se recuperan |
| `restore.sh --ultimo` | Selecciona el último respaldo, pide confirmación y restaura |
| Datos tras restaurar | 1 reserva, 8 películas, 8 funciones, 3 usuarios. La aplicación sigue UP |

Este despliegue es lo que destapó el defecto que se describe a continuación, y que ninguna
prueba de la suite podía encontrar.

### Defecto encontrado solo al desplegar: LazyInitializationException

Al abrir la cartelera contra el stack real, `GET /api/funciones` fallaba con
`LazyInitializationException: Could not initialize proxy [Pelicula#5] - no session`.

**Causa.** El perfil `prod` traía `spring.jpa.open-in-view=false`. Sin *open session in
view*, la sesión de Hibernate se cierra al salir del repositorio, y los DTO se construyen
después: `FuncionResponse.from()` accede a `funcion.getPelicula()`, que es `LAZY`. Lo mismo
ocurría en `ReservaResponse.from()`, que recorre `getUsuario()`, `getFuncion()` y
`getDetallesMinibar()`. En la práctica, toda la API de reservas quedaba rota en producción.

**Por qué la suite no lo detectó.** Los perfiles `dev` y `test` no desactivan
`open-in-view`, así que mantienen la sesión abierta durante toda la petición y el código
funciona. Las 79 pruebas pasaban en verde con el defecto presente. Es un recordatorio de que
una suite verde solo garantiza lo que la suite ejercita, y de que la configuración de
producción también es código.

**Corrección.** Se restauró `open-in-view` a su valor por defecto. Desactivarlo es la
práctica recomendada, porque mantiene la conexión ocupada durante toda la petición, pero
exige que las consultas traigan las relaciones necesarias con `@EntityGraph` o `JOIN FETCH`
en `FuncionRepository` y `ReservaRepository`. Queda documentado como trabajo futuro: es la
mejora correcta, pero introducirla sin pruebas de integración que corran con la
configuración de producción tendría más riesgo que beneficio a este tamaño.

**Mejora pendiente.** Ejecutar `SeguridadIntegrationTest` también con el perfil `prod`
habría detectado esto. Hoy las pruebas de integración solo usan `test`.

## 7. Limitaciones

Vale la pena dejarlas escritas, porque una suite en verde no significa que no haya defectos.

1. **No probamos concurrencia.** Falta el caso de dos reservas simultáneas sobre la misma
   butaca. `ReservaService` consulta la disponibilidad y después escribe, sin bloqueo, así
   que entre esas dos operaciones cabe otra transacción. En la práctica el riesgo es bajo
   (una sala de 20 butacas y poco tráfico) y lo contienen la constraint de la base más la
   [auditoría semanal](PLAN_MANTENIMIENTO.md). La solución correcta sería bloqueo optimista
   con `@Version` en `Funcion`.
2. **No hay pruebas de carga.** No medimos el comportamiento con tráfico sostenido. Las
   métricas de latencia ya están expuestas (ver [plan de monitoreo](PLAN_MONITOREO.md)),
   pero falta la prueba con JMeter o k6.
3. **El frontend no tiene pruebas automatizadas.** Se verifica a mano. Playwright cubriría
   los flujos de reserva completos.
4. **La cobertura de `controller` no tiene umbral propio.** Los controladores se ejercitan
   desde las pruebas de integración, pero sin meta definida.

## Documentos relacionados

- [Arquitectura](ARQUITECTURA.md)
- [Informe de pruebas de seguridad](INFORME_SEGURIDAD.md)
- [Referencias](REFERENCIAS.md)
