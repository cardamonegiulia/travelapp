package com.unical.travelapp.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Abilita i job schedulati dell'applicazione (oggi: l'invito a recensire i viaggi conclusi).
 *
 * <p>E' condizionata su una property cosi' i test possono spegnere lo scheduler e invocare
 * il job direttamente, con una data scelta da loro: un job che parte da solo mentre gira la
 * suite renderebbe i test dipendenti dall'ora in cui vengono eseguiti.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
