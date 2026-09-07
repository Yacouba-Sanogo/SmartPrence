package com.smartpresence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activation de l'audit JPA pour le renseignement automatique des champs
 * {@code createdAt} et {@code updatedAt} portés par {@link com.smartpresence.entity.BaseAuditableEntity}.
 *
 * <p>L'audit applicatif {@code createdBy} / {@code updatedBy} n'est pas activé à ce stade
 * (évolution future prévue, cf. {@code SmartPresence_CONTEXT.md} §12). Il sera introduit
 * via un bean {@code AuditorAware} s'appuyant sur le contexte Spring Security.</p>
 *
 * @since 0.0.1
 * @see com.smartpresence.entity.BaseAuditableEntity
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
