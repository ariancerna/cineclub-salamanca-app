package com.cineclubsalamanca.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita la ejecución de tareas programadas (cron jobs) de la aplicación.
 *
 * <p>Se aísla en su propia clase para poder desactivar la programación en las pruebas
 * de integración sin tener que levantar los planificadores.</p>
 *
 * @see com.cineclubsalamanca.maintenance.TareasMantenimiento
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
