package com.smartpresence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Super-classe commune à toutes les entités persistées, apportant les champs d'audit
 * technique {@code createdAt} et {@code updatedAt}.
 *
 * <p>Cette classe est une {@link MappedSuperclass} : elle n'est pas mappée à une table
 * propre, mais ses colonnes sont <b>intégrées</b> au schéma de chaque entité qui en hérite.
 * Le renseignement automatique des horodatages est assuré par
 * {@link AuditingEntityListener} et l'activation de {@code @EnableJpaAuditing} côté
 * configuration.</p>
 *
 * <h2>Évolution prévue (non implémentée à ce stade)</h2>
 * <p>L'audit applicatif {@code createdBy} / {@code updatedBy} (traçabilité de l'auteur des
 * modifications) est <b>anticipé</b> mais volontairement non activé. Son activation
 * ultérieure nécessitera :</p>
 * <ol>
 *   <li>l'ajout des champs {@code @CreatedBy} / {@code @LastModifiedBy} (Spring Data) ;</li>
 *   <li>la fourniture d'un bean {@code AuditorAware<UUID>} basé sur le contexte
 *       Spring Security ;</li>
 *   <li>la surcharge éventuelle du type ({@code UUID} pour les utilisateurs).</li>
 * </ol>
 * <p>Voir {@code SmartPresence_CONTEXT.md} §12 (Évolutions futures : Audit).</p>
 *
 * <h2>Conventions</h2>
 * <ul>
 *   <li>Horodatages en {@link Instant} (UTC), stockés en {@code TIMESTAMP}.</li>
 *   <li>{@code createdAt} est immuable après création ({@code updatable = false}).</li>
 *   <li>{@code nullable = false} pour garantir l'intégrité d'audit.</li>
 * </ul>
 *
 * @since 0.0.1
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditableEntity {

    /**
     * Instant de création de la ligne (insertion en base).
     * <p>Renseigné automatiquement, non modifiable par la suite.</p>
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Instant de dernière modification de la ligne.
     * <p>Renseigné automatiquement à chaque mise à jour.</p>
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
