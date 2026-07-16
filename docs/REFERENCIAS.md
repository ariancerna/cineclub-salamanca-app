# Referencias

Proyecto CineClub Salamanca — UTP, Curso Integrador I: Sistemas Software.

Formato APA 7.

## Libros

Beyer, B., Jones, C., Petoff, J., & Murphy, N. R. (2016). *Site reliability engineering: How
Google runs production systems*. O'Reilly Media.

Bloch, J. (2018). *Effective Java* (3.ª ed.). Addison-Wesley.

Humble, J., & Farley, D. (2010). *Continuous delivery: Reliable software releases through
build, test, and deployment automation*. Addison-Wesley.

Martin, R. C. (2017). *Clean architecture: A craftsman's guide to software structure and
design*. Prentice Hall.

Myers, G. J., Sandler, C., & Badgett, T. (2011). *The art of software testing* (3.ª ed.).
John Wiley & Sons.

Nygard, M. T. (2018). *Release it! Design and deploy production-ready software* (2.ª ed.).
Pragmatic Bookshelf.

Walls, C. (2022). *Spring in action* (6.ª ed.). Manning Publications.

## Artículos

Provos, N., & Mazières, D. (1999). A future-adaptable password scheme. En *Proceedings of
the 1999 USENIX Annual Technical Conference* (pp. 81–92). USENIX Association.
https://www.usenix.org/legacy/events/usenix99/provos.html

Fowler, M. (2007, 1 de enero). *Mocks aren't stubs*.
https://martinfowler.com/articles/mocksArentStubs.html

Fowler, M. (2011, 12 de mayo). *Test pyramid*.
https://martinfowler.com/bliki/TestPyramid.html

## Estándares

Fielding, R. T., Nottingham, M., & Reschke, J. (Eds.). (2022). *HTTP semantics* (RFC 9110).
Internet Engineering Task Force. https://doi.org/10.17487/RFC9110

Jones, M., Bradley, J., & Sakimura, N. (2015). *JSON Web Token (JWT)* (RFC 7519). Internet
Engineering Task Force. https://doi.org/10.17487/RFC7519

## Seguridad

OWASP Foundation. (2021). *OWASP Top 10:2021 — The ten most critical web application
security risks*. https://owasp.org/Top10/

OWASP Foundation. (2024). *OWASP Dependency-Check documentation*.
https://jeremylong.github.io/DependencyCheck/

OWASP Foundation. (2024). *SQL injection prevention cheat sheet*.
https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html

OWASP Foundation. (2024). *Password storage cheat sheet*.
https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

National Institute of Standards and Technology. (2024). *National Vulnerability Database
(NVD)*. https://nvd.nist.gov/

## Documentación de las tecnologías usadas

Broadcom. (2025). *Spring Boot reference documentation* (Versión 3.4.5).
https://docs.spring.io/spring-boot/docs/3.4.5/reference/html/

Broadcom. (2025). *Spring Boot Actuator: Production-ready features*.
https://docs.spring.io/spring-boot/reference/actuator/index.html

Broadcom. (2025). *Spring Security reference*.
https://docs.spring.io/spring-security/reference/

Broadcom. (2025). *Spring Data JPA reference documentation*.
https://docs.spring.io/spring-data/jpa/reference/

Broadcom. (2025). *Spring Framework: Task execution and scheduling*.
https://docs.spring.io/spring-framework/reference/integration/scheduling.html

Micrometer. (2025). *Micrometer application observability documentation*.
https://docs.micrometer.io/micrometer/reference/

Oracle. (2025). *Java SE 21 documentation*. https://docs.oracle.com/en/java/javase/21/

The Apache Software Foundation. (2025). *Maven: Introduction to the build lifecycle*.
https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html

EclEmma. (2025). *JaCoCo Java code coverage library documentation*.
https://www.jacoco.org/jacoco/trunk/doc/

JUnit Team. (2025). *JUnit 5 user guide*.
https://junit.org/junit5/docs/current/user-guide/

Mockito. (2025). *Mockito framework site*. https://site.mockito.org/

AssertJ. (2025). *AssertJ fluent assertions documentation*. https://assertj.github.io/doc/

QOS.ch. (2025). *Logback documentation: Chapter 4 — Appenders*.
https://logback.qos.ch/manual/appenders.html

Docker Inc. (2025). *Dockerfile reference*. https://docs.docker.com/reference/dockerfile/

Docker Inc. (2025). *Docker Compose specification*.
https://docs.docker.com/reference/compose-file/

PostgreSQL Global Development Group. (2025). *PostgreSQL 15 documentation: pg_dump*.
https://www.postgresql.org/docs/15/app-pgdump.html

PostgreSQL Global Development Group. (2025). *PostgreSQL 15 documentation: Backup and
restore*. https://www.postgresql.org/docs/15/backup.html

Prometheus Authors. (2025). *Prometheus documentation: Exposition formats*.
https://prometheus.io/docs/instrumenting/exposition_formats/

okta. (2025). *JJWT — Java JWT: JSON Web Token for Java and Android*.
https://github.com/jwtk/jjwt

springdoc. (2025). *springdoc-openapi documentation*. https://springdoc.org/

## Dónde se usó cada fuente

Myers et al. y el *Test pyramid* de Fowler están detrás de la estrategia de pruebas: la
separación entre nivel unitario e integración y la idea de que una prueba busca defectos en
lugar de confirmar aciertos ([informe de pruebas](INFORME_PRUEBAS.md)). El artículo de
Fowler sobre mocks es el que justifica usarlos para aislar la clase bajo prueba.

Provos y Mazières es el paper original de BCrypt y explica por qué un hash lento a propósito
protege las contraseñas. El OWASP Top 10 nos sirvió para clasificar las observaciones, y el
RFC 9110 §15.5.2 define la diferencia entre 401 y 403 en la que se basa la corrección de
OBS-01 ([informe de seguridad](INFORME_SEGURIDAD.md)).

Nygard y el libro de SRE de Google fundamentan el enfoque de monitoreo: sondas de salud,
percentiles en vez de promedios y la distinción entre una condición de negocio y una falla
real, que es lo que terminó definiendo el diseño de la sonda de cartelera
([plan de monitoreo](PLAN_MONITOREO.md)).

Martin y Bloch guiaron la separación en capas y la aplicación de SOLID
([arquitectura](ARQUITECTURA.md)). Humble y Farley están detrás del proceso de despliegue:
un solo artefacto que se promueve entre entornos y configuración por variables de entorno
([plan de despliegue](PLAN_DESPLIEGUE.md)).
