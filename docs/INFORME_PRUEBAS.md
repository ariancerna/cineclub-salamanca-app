# Informe de Pruebas de Software — CineClub Salamanca

**Universidad Tecnológica del Perú — Curso Integrador I: Sistemas Software**

---

## 1. Conceptos de testing aplicados

El *testing* es el proceso de ejecutar un programa con la intención de encontrar defectos.
Una prueba que pasa no demuestra que el software sea correcto; solo que no falla en el caso
probado. Por eso el criterio de diseño de esta suite no fue "confirmar que funciona", sino
**buscar los casos donde podría romperse**.

### Niveles de prueba en el proyecto

| Nivel | Qué verifica | Implementación | Cantidad |
|---|---|---|---|
| **Unitario** | Una clase aislada, con sus dependencias sustituidas por dobles | JUnit 5 + Mockito | 63 |
| **Integración** | Varios componentes reales colaborando, incluida la cadena de filtros y la base de datos | `@SpringBootTest` + MockMvc + H2 | 16 |
| **Total** | | | **79** |

### Tipos de prueba

- **Caja blanca:** el diseño de los casos parte del código. La cobertura JaCoCo mide qué
  líneas y ramas quedaron sin ejercitar.
- **Caja negra:** los casos de `SeguridadIntegrationTest` se escriben contra el contrato de
  la API (rutas, códigos HTTP), sin mirar la implementación interna.
- **Regresión:** la suite completa se ejecuta en cada `mvnw verify`; el umbral de cobertura
  hace fallar la build si una clase de servicio nueva llega sin pruebas.

### Estructura de cada caso: Arrange–Act–Assert

Los tres bloques son visibles en cada método. Ejemplo real de `ReservaServiceTest`:

```java
@Test
@DisplayName("No se puede reservar si el aforo está agotado")
void crear_debeLanzarExcepcion_cuandoAforoAgotado() {
    // Arrange — se prepara el escenario
    Usuario usuario = usuarioBase();
    Funcion funcion = funcionBase();
    funcion.setAforoDisponible(0);
    when(usuarioRepository.findByEmail("joan@test.com")).thenReturn(Optional.of(usuario));
    when(funcionService.buscarPorId(1L)).thenReturn(funcion);

    // Act + Assert — se ejecuta y se comprueba el resultado esperado
    assertThatThrownBy(() -> reservaService.crear("joan@test.com", req))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No hay butacas disponibles");

    verify(reservaRepository, never()).save(any());  // no debe persistir nada
}
```

La última línea es la que da valor a la prueba: no basta con que lance excepción, hay que
comprobar que **no dejó datos a medias**.

### Dobles de prueba

Se usan **mocks** (Mockito) para aislar la unidad bajo prueba. `ReservaServiceTest` sustituye
los cinco colaboradores de `ReservaService`, de modo que un fallo en el test señala a
`ReservaService` y no a la base de datos ni a otro servicio. Las pruebas unitarias no tocan
disco ni red: la suite completa corre en menos de 2 segundos.

---

## 2. Herramientas

| Herramienta | Versión | Uso |
|---|---|---|
| JUnit 5 (Jupiter) | 5.11 (vía Spring Boot) | Motor de pruebas |
| Mockito | 5.14 | Dobles de prueba |
| AssertJ | 3.26 | Aserciones legibles (`assertThat(...).isEqualTo(...)`) |
| Spring Boot Test | 3.4.5 | Contexto de aplicación en integración |
| Spring Security Test | 6.4 | Utilidades de seguridad en pruebas |
| MockMvc | — | Peticiones HTTP simuladas sin abrir puerto |
| H2 | 2.3 | Base en memoria para integración |
| JaCoCo | 0.8.12 | Cobertura de código |

---

## 3. Cobertura

### Resultado por clase (capa de servicios)

| Clase | Cobertura de líneas | Cobertura de ramas | Ramas |
|---|---:|---:|---:|
| `AuthService` | 100% | 100% | 2 |
| `ReservaService` | 100% | 100% | 10 |
| `FuncionService` | 100% | — | 0 |
| `PeliculaService` | 100% | — | 0 |
| `ProductoService` | 100% | — | 0 |
| **Total paquete `service`** | **100%** (131/131 líneas) | **100%** | 12 |

> Las clases sin ramas condicionales no reportan cobertura de ramas.

### Evolución

La instrumentación con JaCoCo reveló que la suite original, pese a tener 22 pruebas, dejaba
**la mitad de la lógica de negocio sin ejercitar**. El dato no era evidente sin medirlo:
las 22 pruebas existentes pasaban todas.

| Momento | Pruebas | Cobertura de servicios |
|---|---:|---:|
| Estado inicial | 22 | 49% |
| Tras ampliar la suite | 79 | 100% |

Los huecos detectados y cerrados:

| Hallazgo | Acción |
|---|---|
| `PeliculaService` sin ninguna prueba (0%) | `PeliculaServiceTest` — 9 casos |
| `ProductoService` sin ninguna prueba (0%) | `ProductoServiceTest` — 9 casos |
| `ReservaService` no ejercitaba el cálculo de subtotales del minibar | 4 casos nuevos, incluido el de precio × cantidad |
| `FuncionService` no cubría `listarTodas`, `obtenerPorId` ni `actualizar` | 5 casos nuevos |
| `TareasMantenimiento` sin pruebas pese a corregir datos en producción | `TareasMantenimientoTest` — 10 casos |

### Umbral automatizado

`pom.xml` configura JaCoCo para **fallar la build** si la capa de servicios baja del 70% de
líneas:

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

El umbral se aplica solo a `service` porque es donde vive la lógica de negocio. Exigir el
mismo porcentaje a las entidades o los DTO mediría sobre todo *getters* generados por
Lombok, lo que inflaría la cifra sin mejorar la calidad de las pruebas.

El umbral es del 70% aunque la cobertura actual sea del 100%: es un **suelo** que impide
retroceder, no una meta. Fijarlo en 100% obligaría a escribir una prueba por cada línea
nueva antes de poder compilar, lo que en la práctica empuja a redactar pruebas de relleno
para satisfacer al contador. La cobertura mide qué código se ejecutó, no si las aserciones
comprueban algo útil: es un indicador de huecos, no de calidad.

---

## 4. Casos de prueba

### 4.1 `ReservaServiceTest` — 17 casos

Concentra las reglas de negocio críticas del sistema.

| Caso | Regla verificada | Resultado |
|---|---|---|
| Reserva exitosa retorna código y datos completos | Camino feliz | ✅ |
| No se puede reservar si el aforo está agotado | `aforoDisponible <= 0` → 409 | ✅ |
| Un usuario no puede reservar dos veces la misma función | Unicidad usuario–función | ✅ |
| No se puede reservar una butaca ya ocupada | Unicidad butaca–función | ✅ |
| Email inexistente lanza `EntityNotFoundException` | Validación de usuario | ✅ |
| El admin confirma ingreso y `asistioIngreso` pasa a `true` | Control de acceso a sala | ✅ |
| Código de reserva inválido lanza `EntityNotFoundException` | Validación de código | ✅ |
| Retorna los códigos de butaca ocupados | Mapa de asientos | ✅ |
| Lista vacía cuando la función no tiene reservas | Caso borde | ✅ |
| El subtotal del minibar es precio × cantidad | Cálculo monetario | ✅ |
| Una reserva sin minibar no consulta el catálogo | Eficiencia | ✅ |
| Crear una reserva descuenta una butaca del aforo | Consistencia del contador | ✅ |
| El código generado usa el prefijo `SLM-` | Formato (12 caracteres) | ✅ |
| `listarMisReservas` devuelve solo las del usuario | Aislamiento entre usuarios | ✅ |
| `listarMisReservas` con email inexistente | Validación | ✅ |
| `obtenerPorCodigo` devuelve la reserva | Consulta por código | ✅ |
| `listarPorFuncion` para el panel admin | Consulta administrativa | ✅ |

### 4.2 `TareasMantenimientoTest` — 10 casos

Las tareas programadas **modifican datos en producción sin supervisión humana**, así que
merecen pruebas tanto como la lógica que sirve peticiones. La auditoría de aforo es la más
delicada: corrige registros automáticamente, y un error de signo dejaría el aforo peor de lo
que estaba.

| Caso | Regla verificada |
|---|---|
| Corrige el aforo cuando el contador se desvió | `aforoMaximo − reservas` |
| No escribe si el aforo ya es consistente | Evita escrituras inútiles |
| Trata un aforo nulo como inconsistente | Caso borde |
| Revisa varias funciones y corrige solo las desviadas | Aislamiento |
| Soporta una cartelera vacía | Caso borde |
| Purga las reservas anteriores a la retención | Política de retención |
| No llama a `deleteAll` si no hay nada que purgar | Evita operaciones vacías |
| El límite de purga respeta los meses configurados | Configurabilidad |
| El reporte diario consulta reservas y aforo | Reporte de operación |
| El reporte no falla con aforo nulo | Robustez |

### 4.3 Casos borde considerados

Además del camino feliz, se probaron:

- **Aforo exactamente en 0** — límite inferior del contador.
- **Precio 0.00** en un producto de cortesía — `@DecimalMin("0.00")` lo admite.
- **Lista de minibar `null`** frente a lista vacía — dos entradas distintas al mismo camino.
- **Contraseña de 3 caracteres** — justo bajo el mínimo de 8.
- **Reserva de función inexistente** — identificador que no resuelve.

### 4.3 Precisión monetaria

Los importes usan `BigDecimal` y no `double`, porque la aritmética binaria de coma flotante
no representa exactamente los decimales del sistema decimal (`0.1 + 0.2 != 0.3`). Las
aserciones usan `isEqualByComparingTo` en lugar de `isEqualTo`, ya que `BigDecimal.equals`
distingue `17.0` de `17.00` — misma cantidad, distinta escala:

```java
assertThat(detalles.get(0).getSubtotal()).isEqualByComparingTo("17.00");  // 8.50 × 2
```

---

## 5. Pruebas de integración

`SeguridadIntegrationTest` levanta el contexto completo de Spring contra H2 en memoria. A
diferencia de las unitarias, aquí **la cadena de filtros de seguridad es real**: los tokens
se firman y validan de verdad.

Estas pruebas son las que detectaron el defecto de códigos 401/403 descrito en el
[informe de seguridad](INFORME_SEGURIDAD.md) — un fallo que ninguna prueba unitaria de
servicio podía encontrar, porque ocurre en los filtros, antes de llegar al servicio.

El perfil `test` usa H2 en memoria en lugar de PostgreSQL para que la suite corra sin Docker
y con una base limpia por ejecución. La contrapartida es que H2 no es PostgreSQL: se activa
`MODE=PostgreSQL` para acercar el dialecto, pero un defecto específico del motor real no se
detectaría aquí. Es un compromiso consciente entre fidelidad y velocidad.

---

## 6. Ejecución

```bash
cd backend

# Solo pruebas
./mvnw test

# Pruebas + cobertura + verificación del umbral
./mvnw verify
```

El reporte HTML de cobertura queda en `backend/target/site/jacoco/index.html`.

### Última ejecución

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

### Verificación en ejecución real

Además de la suite, se arrancó el JAR empaquetado y se comprobó el sistema de extremo a
extremo:

| Comprobación | Resultado |
|---|---|
| `/actuator/health` anónimo | `{"status":"UP"}`, sin exponer componentes |
| `/actuator/metrics` sin token | HTTP 401 (corrección de OBS-01 confirmada) |
| `/actuator/health` como admin | `cartelera: UP`, `funcionesFuturas: 8` |
| Reserva real con minibar | Código `SLM-6C566857`, aforo 32 → 31 |
| Métricas tras la reserva | `reservas.totales` 0 → 1, `aforo.disponible` 256 → 255 |
| Archivo de log | `logs/cineclub.log` escrito con el formato configurado |

---

## 7. Limitaciones conocidas

Declararlas es parte del informe: una suite verde no equivale a un sistema sin defectos.

1. **Concurrencia sin probar.** La suite no cubre dos reservas simultáneas sobre la misma
   butaca. `ReservaService` comprueba disponibilidad y luego escribe, sin bloqueo: entre
   ambas operaciones cabe otra transacción. En la práctica el riesgo es bajo (una sala de
   20 butacas, tráfico modesto) y la constraint de base de datos junto con la
   [auditoría semanal de aforo](PLAN_MANTENIMIENTO.md) lo contienen. La solución correcta
   sería bloqueo optimista con `@Version` en `Funcion`.
2. **Sin pruebas de carga.** No se ha medido el comportamiento bajo tráfico sostenido. Las
   métricas de latencia por percentiles ya están expuestas (ver
   [plan de monitoreo](PLAN_MONITOREO.md)), pero falta el ensayo con JMeter o k6.
3. **Frontend sin pruebas automatizadas.** La verificación de la interfaz es manual. Una
   suite con Playwright cubriría los flujos de reserva de extremo a extremo.
4. **Cobertura de `controller` no medida por el umbral.** Los controladores se ejercitan
   indirectamente desde las pruebas de integración, pero no tienen una meta propia.

---

## 8. Documentos relacionados

- [Arquitectura](ARQUITECTURA.md)
- [Informe de pruebas de seguridad](INFORME_SEGURIDAD.md)
- [Referencias bibliográficas](REFERENCIAS.md)
