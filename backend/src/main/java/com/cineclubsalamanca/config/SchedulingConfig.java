package com.cineclubsalamanca.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas.
 *
 * @see com.cineclubsalamanca.maintenance.TareasMantenimiento
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
