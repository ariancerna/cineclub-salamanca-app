# Qué herramienta usé para cada cosa

Proyecto CineClub Salamanca — UTP, Curso Integrador I: Sistemas Software.

Respuestas para cuando el docente pregunte "¿qué usaste para...?". La regla de oro de la
rúbrica: no basta nombrar la herramienta, hay que **vincularla con el concepto del curso**.
Nombrar sin explicar el porqué cae en "En Proceso". Por eso cada respuesta trae la
herramienta y el concepto detrás.

---

## Pruebas de software

**¿Qué usaste para probar?**
JUnit 5 como motor de pruebas, Mockito para los dobles de prueba y AssertJ para las
aserciones. Son las tres que trae `spring-boot-starter-test`.

**¿Por qué Mockito? / ¿Qué es un mock?**
Un mock es un doble que reemplaza una dependencia real. En `ReservaServiceTest` reemplazo los
cinco colaboradores de `ReservaService`, así que si el test falla sé que el problema está en
`ReservaService` y no en la base o en otro servicio. Eso es una prueba **unitaria**: aísla una
sola unidad. Corren en menos de dos segundos porque no tocan disco ni red.

**¿Qué niveles de prueba tienes?**
Dos. Unitarias con Mockito, aisladas. Y de integración con `@SpringBootTest` y MockMvc contra
una base H2 en memoria, donde la cadena de filtros de seguridad es real y los tokens se firman
y validan de verdad. Es la pirámide de pruebas: muchas unitarias, rápidas, y menos de
integración, más lentas pero más completas.

**¿Qué es caja blanca y caja negra en tu proyecto?**
Caja blanca: los casos se diseñan mirando el código, y JaCoCo me dice qué líneas quedaron sin
ejecutar. Caja negra: las pruebas de integración se escriben contra el contrato de la API
—rutas y códigos HTTP— sin mirar la implementación.

**¿Cómo mides la cobertura?**
JaCoCo 0.8.12. Genera un reporte HTML y, más importante, está configurado en el `pom.xml` para
**fallar la build** si la capa de servicios baja del 70% de líneas. Empezamos en 49% de
cobertura con 22 pruebas que pasaban todas; hoy son 80 pruebas y 100%. El dato clave: que las
pruebas pasen no significa que cubran; hubo que medirlo para verlo.

**¿Por qué 70% si estás al 100%?**
Es un piso que impide retroceder, no una meta. Si lo pusiera en 100%, cada línea nueva
exigiría su prueba antes de compilar, y eso empuja a escribir pruebas de relleno. La cobertura
mide qué código se ejecutó, no si las aserciones comprueban algo útil.

**¿Qué patrón siguen tus pruebas?**
Arrange-Act-Assert: preparo el escenario, ejecuto, y compruebo. En la comprobación no solo
verifico el resultado, también que no queden efectos colaterales; por ejemplo, que una reserva
rechazada no haya guardado nada con `verify(repo, never()).save(...)`.

---

## Pruebas de seguridad

**¿Qué herramientas de seguridad usaste?**
Tres enfoques. SAST, revisión estática de la configuración de seguridad. SCA con OWASP
Dependency-Check 10.0.4, que busca vulnerabilidades conocidas en las librerías contra la base
NVD. Y DAST con `SeguridadIntegrationTest`, que ataca la API en ejecución con peticiones reales:
tokens falsos, roles insuficientes, inyección SQL.

**¿Cómo pruebas contra inyección SQL?**
Mando el payload `' OR '1'='1'; DROP TABLE usuario; --` en el login y verifico que la tabla
siga intacta contando los usuarios antes y después. Lo importante: no protege filtrar
caracteres, protege que JPA parametriza las consultas. El texto viaja como valor de un
parámetro, nunca como parte de la sentencia SQL.

**¿Cómo proteges las contraseñas?**
BCrypt, que viene con Spring Security. Es un hash adaptativo y con sal: lento a propósito para
encarecer los ataques de diccionario, y la sal va dentro del hash, así que no sirven las tablas
precomputadas. No es reversible; para verificar, `passwordEncoder.matches()` vuelve a hashear
el intento y compara.

**¿Cómo funciona la autenticación?**
JWT firmado con HS256, usando la librería JJWT 0.12.6. La API es sin estado: la identidad viaja
en el token en cada petición, el servidor no guarda sesión. La autorización está en dos niveles:
por ruta en `SecurityConfig` y por método con `@PreAuthorize`. Es defensa en profundidad.

**¿Qué observaciones levantaste?**
Seis, documentadas en el informe de seguridad. Las más importantes:
- OBS-01: la API devolvía 403 donde correspondía 401. Sin un `AuthenticationEntryPoint`, Spring
  Security responde 403 hasta al anónimo, y el cliente no puede distinguir "vuelve a iniciar
  sesión" de "no te alcanza el rol". Lo detectaron las pruebas de integración.
- OBS-03: el `JWT_SECRET` de ejemplo permite firmar tokens de administrador. En producción se
  genera por variable de entorno con `openssl rand -base64 48`.

**¿Cómo corres el análisis de dependencias?**
`./mvnw verify -Pseguridad`. Va en un perfil aparte porque descarga el catálogo NVD y tarda
varios minutos; obligarlo en cada build haría el desarrollo inviable. Está configurado para
fallar ante cualquier vulnerabilidad de severidad alta o crítica (CVSS >= 7).

---

## Despliegue

**¿Cómo despliegas la aplicación?**
Con Maven y Docker. Maven empaqueta un JAR ejecutable con Tomcat embebido, así que no hace
falta instalar un servidor de aplicaciones aparte; alcanza con `java -jar`. Docker toma ese JAR
en una imagen multietapa.

**¿Qué es el ciclo de vida de Maven?**
Fases que corren en orden: compile, test, package, verify, install. Invocar una ejecuta las
anteriores. En mi proyecto, `verify` genera el reporte de cobertura y falla si baja del 70%,
así que una build que no cumple no llega a empaquetarse.

**¿Qué es la construcción multietapa de Docker?**
El Dockerfile tiene dos etapas. La primera usa una imagen con Maven para compilar. La segunda
solo copia el JAR sobre un JRE. La imagen final no lleva Maven, ni el JDK, ni el código fuente:
pesa menos y expone menos superficie de ataque. Además corre con un usuario sin privilegios.

**¿Cómo configuras los servidores? / ¿Qué es un perfil?**
Tres perfiles de Spring: dev, test y prod. La misma imagen sirve para todos; lo que cambia es
el perfil activo. En producción uso `ddl-auto=validate`: Hibernate comprueba que las tablas
coincidan con las entidades pero no las modifica. Con `update`, un descuido en una entidad
alteraría el esquema real. Las credenciales entran por variables de entorno, nunca en el código.

**¿Cómo orquestas los servicios?**
Docker Compose. Levanta PostgreSQL, el backend y nginx para el frontend. El backend espera con
`condition: service_healthy` a que la base acepte conexiones, no solo a que el contenedor
arranque.

**¿Por qué no lo subiste a la nube?**
Porque el backend es un proceso de larga vida con tareas programadas, sondas de salud y logs
con rotación. En serverless las tareas `@Scheduled` no correrían y los logs se perderían. La
rúbrica pide configuración de servidores, que es lo opuesto a serverless. Docker Compose es
coherente con lo que documenté.

---

## Monitoreo

**¿Qué usaste para monitorear?**
Spring Boot Actuator para las sondas de salud y Micrometer para las métricas, con un registro
Prometheus. Actuator expone la telemetría por HTTP sin código adicional.

**¿Cuáles son las health tools?**
El endpoint `/actuator/health`. Trae sondas automáticas de base de datos, disco y proceso, y
una propia que escribí, `cartelera`, que avisa si no hay funciones programadas. El detalle solo
lo ven los administradores; el anónimo ve únicamente el estado, porque decirle a cualquiera que
la base está caída es información útil para un atacante.

**La pregunta estrella: ¿por qué tu sonda no reporta DOWN cuando no hay cartelera?**
Porque Actuator traduce `DOWN` a un HTTP 503, y eso haría que el balanceador sacara la instancia
de rotación y Docker reiniciara el contenedor. Todo con la aplicación perfectamente sana. Una
cartelera vacía es una condición de negocio, no una avería. Uso un estado propio,
`SIN_CARTELERA`, que se sirve con 200: el aviso queda visible sin degradar la salud global. Un
fallo real al consultar la base sí reporta `DOWN`.

**¿Cuáles son las performance tools?**
Las métricas de `/actuator/metrics` y `/actuator/prometheus`: latencia HTTP, memoria, GC, pool
de conexiones. La latencia está configurada por percentiles, no por promedio. Un promedio de
200 ms puede esconder que uno de cada veinte usuarios espera tres segundos; el p95 refleja lo
que la gente experimenta.

**¿Qué son las métricas de negocio?**
Indicadores que Actuator no puede conocer: reservas totales, asistencias, aforo disponible,
usuarios. La relación asistencias sobre totales es el ausentismo, que en un cine con reserva
gratuita importa: reservar y no ir deja butacas vacías. Las publico con un `MeterBinder` para
no acoplar la lógica de negocio a la telemetría.

**¿Cómo manejas los logs?**
Logback, que viene con Spring Boot. Configurado con rotación diaria, 10 MB por archivo, 30 días
de historial y un tope total de 200 MB. El tope es lo que más importa: sin él, un error en
bucle llena el disco y se lleva la aplicación y la base.

---

## Mantenimiento

**¿Qué usaste para los cron jobs?**
La anotación `@Scheduled` de Spring, habilitada con `@EnableScheduling`. Tengo tres tareas:
reporte diario de operación, auditoría semanal de aforo y purga mensual de reservas. Las
expresiones cron están en la configuración, así que se ajustan por entorno sin recompilar.

**¿Por qué unas tareas están en la aplicación y otras en cron del sistema?**
Es el criterio de diseño clave. Si la tarea necesita entender el negocio, va en la aplicación
con `@Scheduled`, porque necesita acceso al modelo y a las transacciones de JPA. Si tiene que
funcionar aunque la aplicación esté caída —como un respaldo—, va en cron del sistema operativo.
Un respaldo programado dentro de la app sería inútil justo cuando más se necesita.

**¿Qué hace la auditoría de aforo?**
El aforo disponible es un contador denormalizado que se descuenta al reservar. Si dos reservas
concurrentes se solapan, el contador se desvía del número real. La tarea recalcula el valor
correcto y corrige. Y no es teoría: en las pruebas detectó el contador en 31 cuando debía ser
32, y lo corrigió sola. Cada corrección es además una señal: si aparecen seguido, hay que
implementar bloqueo optimista con `@Version`.

**¿Cómo haces los backups?**
Un script `backup.sh` que usa `pg_dump` dentro del contenedor, comprime con gzip y aplica una
retención de 30 días. Hay versión para Windows en PowerShell. El script comprueba que el volcado
no quede vacío: sin ese control, un fallo de `pg_dump` dejaría un archivo de cero bytes con
pinta de respaldo válido, y la retención acabaría borrando los buenos.

**¿Probaste que los backups sirvan?**
Sí. Un respaldo que nunca se restauró no ofrece garantía. `restore.sh` lo restaura sobre una
base de ensayo, sin tocar producción, y verifiqué que las tablas y los datos vuelven. Está
programado para hacerse el día 15 de cada mes.

**¿Qué otros scripts tienes?**
`healthcheck.sh`, que consulta la sonda de salud y devuelve un código de salida para cron o
alertas; y `crontab.example`, la programación de referencia del servidor. Todos cargan la
configuración desde `.env` y ninguno recibe credenciales por línea de comandos, donde quedarían
en el historial del shell.

**¿Cuál es tu política de recuperación?**
RPO de 24 horas, que sale de respaldar una vez al día, y RTO de 1 hora en el peor caso. Son
razonables para un cine con funciones semanales y reserva gratuita; un sistema con pagos
necesitaría otros números.

---

## Producto final y buenas prácticas

**¿Qué patrones de diseño usaste?**
MVC en capas: controlador, servicio, repositorio (DAO), entidad. El patrón DAO lo implementa
Spring Data JPA: los repositorios son interfaces y Spring genera la implementación. Uso DTO en
la frontera HTTP para no exponer entidades, y aplico SOLID; por ejemplo, inyección de
dependencias por constructor, que es inversión de dependencias y además permite las pruebas con
mocks.

**¿Qué control de versiones usaste?**
Git, con el repositorio en GitHub. El historial tiene commits temáticos por criterio de la
rúbrica: uno para monitoreo, otro para mantenimiento, otro para pruebas.

**¿Cómo generas la documentación del código?**
Javadoc, con el `maven-javadoc-plugin`. `./mvnw javadoc:javadoc` genera el HTML, y el
empaquetado produce además un JAR de Javadoc.

**¿La documentación está alineada con el código? (criterio "coherente")**
Sí, y lo cuidé activamente: cuando corregí un bug o cambié un número de pruebas, actualicé los
informes en la misma tanda. Por ejemplo, cuando el README decía "22 pruebas" y ya eran 80, lo
corregí.

---

## Si te preguntan algo que no sabes

No inventes. Di "esa parte la implementé de tal forma, pero no recuerdo el detalle exacto; lo
puedo revisar en el código". Es mejor que una respuesta inventada que el docente detecte. La
rúbrica de autoría dice "el código fue hecho por el estudiante **o lo domina**": dominar
incluye saber dónde está la respuesta, no memorizar todo.
