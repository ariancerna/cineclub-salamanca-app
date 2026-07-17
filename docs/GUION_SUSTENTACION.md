# Guion de sustentación

Proyecto CineClub Salamanca — UTP, Curso Integrador I: Sistemas Software.
Duración: 10 minutos.

## Cómo repartir el tiempo

La rúbrica reparte 18 puntos entre las secciones del proyecto. El tiempo debe seguir ese
reparto, no lo que sea más entretenido de contar.

| Sección | Puntos | Peso | Minutos |
|---|---:|---:|---:|
| Monitoreo | 6 | 33% | 3.3 |
| Mantenimiento | 5 | 28% | 2.8 |
| Producto final | 3 | 17% | 1.7 |
| Pruebas y seguridad | 2 | 11% | 1.1 |
| Despliegue | 2 | 11% | 1.1 |

**Monitoreo y mantenimiento son el 61% de la nota.** El error clásico es dedicar ocho
minutos a la demo de la aplicación y dos a decir "además tenemos logs". Eso regala once
puntos.

La sustentación oral (2 puntos más) no es una sección: se evalúa en cómo hablas. El criterio
que la distingue del resto es este: *"demuestra dominio de la sección que sustenta, pues
vincula sus ideas con los conceptos abordados en el curso"*. No basta con mostrar que
funciona: hay que explicar por qué se decidió así.

## Estructura, minuto a minuto

### 1. Contexto (0:00 – 0:45)

El Cineclub de Salamanca, en Ate, proyecta cine clásico con entrada gratuita. El problema no
es cobrar, es **controlar el aforo**: sin reservas, la sala se llena o se vacía sin
previsión, y no hay forma de saber cuánta gente va a venir.

La reserva gratuita trae un problema propio: reservar y no asistir no le cuesta nada al
espectador, pero deja butacas vacías. Ese dato, el ausentismo, es lo que el sistema mide.

> Menciona el ausentismo desde el inicio. Justifica una métrica de negocio que aparecerá
> luego en monitoreo, y muestra que entendiste el dominio, no solo el código.

### 2. Arquitectura (0:45 – 2:30)

Muestra el diagrama de capas.

- **MVC en capas**: controlador, servicio, repositorio (DAO), entidad. Cada capa depende
  solo de la siguiente.
- **DTO en la frontera HTTP**, no entidades. Dos motivos: seguridad, porque `JwtResponse`
  no puede filtrar el `passwordHash` aunque alguien se distraiga; y técnico, porque
  serializar entidades con relaciones `LAZY` revienta con `LazyInitializationException`.
- **API sin estado** (`STATELESS`): la identidad viaja en el JWT, el servidor no guarda
  sesión. Permite reiniciar o replicar el backend sin echar a nadie.

SOLID con un ejemplo concreto, no recitando las cinco letras:

> "Las métricas de negocio se publican con un `MeterBinder` en vez de meter contadores
> dentro de `ReservaService`. Así, si mañana cambia el monitoreo, no se toca la lógica de
> negocio. Eso es responsabilidad única aplicada, no teoría."

Y una decisión de diseño con su precio:

> "`aforoDisponible` está denormalizado: podría calcularse contando reservas, pero se guarda
> para no repetir esa cuenta en cada consulta de la cartelera. El precio es que puede
> desincronizarse. Por eso existe la auditoría semanal que voy a mostrar en mantenimiento."

Eso encadena arquitectura con mantenimiento y demuestra que el diseño fue pensado, no
improvisado.

### 3. Pruebas y seguridad (2:30 – 3:40)

**El argumento central, y es el más fuerte que tienes:**

> "Empezamos con 22 pruebas y todas pasaban. Al medir con JaCoCo, la cobertura de la capa de
> servicios era 49%: la mitad de la lógica de negocio no se ejecutaba nunca. Hoy son 80
> pruebas y 100%."

Niveles: unitarias con Mockito (aisladas, sin tocar disco ni red) e integración con
`@SpringBootTest` contra H2, donde la cadena de filtros de seguridad es real.

Seguridad, con las observaciones levantadas:

- **OBS-01**: la API devolvía 403 donde correspondía 401. Sin `AuthenticationEntryPoint`,
  Spring Security responde 403 hasta al usuario anónimo, y el frontend no podía distinguir
  "tu sesión venció" de "no te alcanza el rol". Cita el RFC 9110 §15.5.2.
- **BCrypt**: hash adaptativo y con sal. Lento a propósito para encarecer el diccionario, y
  la sal va dentro del hash, así que no sirven las tablas arcoíris.
- **Inyección SQL**: no protege filtrar caracteres, protege que JPA parametriza. El payload
  viaja como valor, nunca como sentencia.

### 4. Despliegue (3:40 – 4:50)

- **Maven**: el ciclo de vida por fases. `verify` **falla la build** si la cobertura baja del
  70%. No es decorativo: bloquea el empaquetado.
- **Artefacto**: JAR ejecutable con Tomcat embebido. No hace falta servidor de aplicaciones.
- **Docker multietapa**: Maven compila en la primera etapa, la imagen final solo lleva el
  JAR sobre un JRE. Sin código fuente, sin JDK, usuario sin privilegios.
- **Perfiles**: `dev`, `test`, `prod`. En producción `ddl-auto=validate`, que comprueba el
  esquema pero no lo toca. Con `update`, un descuido en una entidad alteraría la base real.

### 5. Monitoreo (4:50 – 8:00) — la sección más pesada

Los tres pilares, como preguntas:

| Pregunta | Herramienta |
|---|---|
| ¿Está viva y puede atender? | Health tools (Actuator) |
| ¿Responde con la rapidez esperada? | Performance tools (Micrometer) |
| ¿Qué pasó y cuándo? | Logs (Logback con rotación) |

**El momento fuerte de esta sección: la sonda de cartelera.**

> "Escribí una sonda propia que avisa si no hay funciones programadas. Al principio
> reportaba `DOWN`, y las pruebas fallaron con un 503. Ahí está lo interesante: Actuator
> traduce `DOWN` a un 503, el balanceador habría sacado la instancia de rotación y Docker
> habría reiniciado el contenedor. Todo eso con la aplicación perfectamente sana. Estaba
> tratando una condición de negocio como si fuera una avería. Ahora usa un estado propio,
> `SIN_CARTELERA`, que se sirve con 200: el aviso queda visible sin degradar la salud
> global."

Eso es exactamente "dominio vinculado a conceptos del curso".

Rendimiento, con un detalle que demuestra criterio:

> "Las métricas de latencia están configuradas por percentiles, no por promedio. Un promedio
> de 200 ms puede esconder que uno de cada veinte usuarios espera tres segundos. El p95
> refleja lo que la gente experimenta."

Métricas de negocio: reservas, asistencias, aforo, usuarios. La relación asistencias/totales
es el ausentismo del que hablaste al inicio.

Logs: rotación diaria, 10 MB por archivo, 30 días, y tope total de 200 MB.

> "El tope total es la protección que más importa. Sin él, un error en bucle llena el disco y
> se lleva puestas la aplicación y la base de datos."

Muestra la captura del log con las líneas `[MANTENIMIENTO]`.

### 6. Mantenimiento (8:00 – 9:30)

Empieza por el criterio de diseño, que es lo que se evalúa:

> "Las tareas están repartidas entre la aplicación y el sistema operativo. Si necesita
> entender el negocio, va en la aplicación con `@Scheduled`. Si tiene que funcionar cuando la
> aplicación no funciona, va en cron. Un respaldo programado dentro de la aplicación sería
> inútil justo cuando más se lo necesita."

Las tres tareas: reporte diario, auditoría de aforo, purga por retención.

**El momento fuerte: la auditoría detectó algo real.** Muestra la captura:

```
[MANTENIMIENTO] Aforo inconsistente en función 6 — registrado: 31, real: 32. Corrigiendo.
```

> "Esto no es un ejemplo inventado. El contador se desincronizó porque se borró una reserva
> directamente en la base, saltándose la aplicación. La tarea lo detectó y lo corrigió sola.
> Y cada línea de estas es además una señal: si aparecen seguido, significa que el problema
> de concurrencia se está materializando y toca implementar bloqueo optimista con
> `@Version`."

Respaldos: `pg_dump` comprimido, retención de 30 días, y la prueba mensual de restauración.

> "El script comprueba que el volcado no quede vacío. Sin ese control, un fallo de `pg_dump`
> dejaría un archivo de cero bytes con pinta de respaldo válido, y la retención iría borrando
> los buenos hasta dejar solo basura. Es la forma más traicionera en que falla un sistema de
> respaldos."

RPO de 24 horas y RTO de 1 hora, y por qué son razonables aquí: funciones semanales y
reserva gratuita. Un sistema con pagos necesitaría otros números.

### 7. Cierre (9:30 – 10:00)

**No cierres con "todo funciona". Cierra con esto:**

> "Las 79 pruebas pasaban en verde con tres defectos dentro. Los encontré al desplegar y usar
> la aplicación: `open-in-view=false` rompía la cartelera entera con
> `LazyInitializationException`; el contenedor corría en UTC, así que una función de las 19:30
> se daba por pasada a las 14:30; y se podía reservar una función ya proyectada, porque el
> servicio validaba aforo y butaca pero no la fecha. Ninguno lo detectaba la suite, porque los
> perfiles de desarrollo y prueba no reproducen la configuración de producción. La conclusión
> que me llevo es que una suite en verde solo garantiza lo que la suite ejercita, y que la
> configuración de producción también es código que hay que probar ejecutándolo."

Eso demuestra criterio propio y honestidad técnica, que es lo que separa una nota alta de
una media.

## Preguntas probables

**¿Por qué `SIN_CARTELERA` y no `DOWN`?**
Porque `DOWN` se traduce a 503 y el balanceador retiraría una instancia sana. Una cartelera
vacía es una condición de negocio, no una avería. Un fallo real al consultar la base sí
reporta `DOWN`.

**¿Por qué el umbral de cobertura es 70% si están al 100%?**
Es un piso que impide retroceder, no una meta. Si lo pusiera en 100%, cada línea nueva
exigiría su prueba antes de compilar, y eso empuja a escribir pruebas de relleno. La
cobertura mide qué código se ejecutó, no si las aserciones comprueban algo útil.

**¿Por qué no desactivaron `open-in-view`, que es la práctica recomendada?**
Porque los DTO se construyen fuera de la transacción y recorren relaciones `LAZY`.
Desactivarlo exige `@EntityGraph` en las consultas de dos repositorios. Está documentado como
trabajo futuro: a este tamaño, el riesgo de introducirlo sin pruebas de integración con
configuración de producción supera el beneficio.

**¿Qué pasa si dos personas reservan la misma butaca a la vez?**
Es una limitación conocida y está declarada en el informe. El servicio consulta y luego
escribe sin bloqueo, así que cabe otra transacción en medio. Lo contienen la constraint de
base de datos y la auditoría semanal. La solución correcta es bloqueo optimista con
`@Version` en `Funcion`.

**¿Por qué no está en la nube?**
Porque el backend es un proceso de larga vida con tareas programadas, sondas de salud y logs
con rotación. En serverless las tareas `@Scheduled` no correrían, las métricas se
reiniciarían en cada invocación y los logs se perderían. La rúbrica pide configuración de
servidores, que es lo opuesto a serverless. Docker Compose es coherente con lo que documenté.

**¿Por qué guardan el aforo si se puede calcular?**
Para no recalcularlo en cada consulta de la cartelera. El precio es que puede
desincronizarse, y por eso existe la auditoría semanal.

**¿El proyecto está listo para producción?**
No del todo, y está documentado. Falta HTTPS, el `JWT_SECRET` debe generarse por entorno
(OBS-03), no hay límite de intentos de login (OBS-04, riesgo aceptado) y faltan migraciones
versionadas con Flyway.

## Sobre las diapositivas

Unas diez, una por minuto. Los diagramas de `docs/ARQUITECTURA.md` están en Mermaid y se
exportan como imagen. Las capturas de `docs/capturas/` sirven para monitoreo y mantenimiento.

Poco texto: la diapositiva acompaña, no se lee. Si una lámina tiene párrafos, el evaluador
los lee y deja de escucharte.

## Los tres errores que hay que evitar

1. **Demorar ocho minutos en la demo.** La aplicación funcionando vale poco por sí sola; lo
   que se evalúa es el criterio detrás.
2. **Recitar SOLID.** Un ejemplo concreto del código vale más que las cinco definiciones.
3. **Decir que todo está perfecto.** Las limitaciones declaradas demuestran criterio. Un
   evaluador que encuentra un hueco que tú no mencionaste piensa que no lo viste.
