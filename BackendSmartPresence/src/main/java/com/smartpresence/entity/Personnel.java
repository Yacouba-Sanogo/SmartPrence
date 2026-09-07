package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import com.smartpresence.constants.TypePersonnel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Membre du personnel universitaire : enseignant, administratif, technique ou sécurité.
 *
 * <h2>Correspondance biométrique</h2>
 * <p>Le champ {@code biometricId} est une <b>référence logique</b> reliant l'agent au
 * modèle d'empreinte conservé dans la mémoire interne du capteur AS608. Il ne contient
 * <b>jamais</b> d'empreinte ni de gabarit biométrique : le backend n'en reçoit aucun
 * (cf. {@code SmartPresence_CONTEXT.md} §3.6 et §13).</p>
 *
 * <p>Ce champ est <b>nullable</b> : un agent est d'abord créé dans le référentiel, puis
 * <b>enrôlé</b> dans un second temps lorsque son doigt est présenté au capteur. Un
 * personnel non enrôlé ne peut simplement pas pointer. La contrainte d'unicité empêche
 * qu'un même identifiant biométrique soit attribué à deux agents — condition
 * indispensable à la fiabilité de l'identification.</p>
 *
 * @since 0.0.1
 */
@Entity
@Table(
        name = TableNames.PERSONNELS,
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_personnels_matricule", columnNames = TableNames.COL_MATRICULE),
                @UniqueConstraint(name = "uk_personnels_biometric_id", columnNames = TableNames.COL_BIOMETRIC_ID)
        },
        indexes = @Index(name = "idx_personnels_biometric_id", columnList = TableNames.COL_BIOMETRIC_ID)
)
@Getter
@Setter
@ToString(exclude = "utilisateur")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Personnel extends BaseAuditableEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(length = 36)
    private UUID id;

    @Column(name = TableNames.COL_MATRICULE, nullable = false, length = 50)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(name = TableNames.COL_EMAIL, length = 150)
    private String email;

    @Column(length = 20)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypePersonnel type;

    @Column(length = 120)
    private String service;

    /**
     * Référence logique de correspondance biométrique — <b>jamais une empreinte</b>.
     *
     * <p>{@code null} tant que l'agent n'a pas été enrôlé sur un capteur. Unique dès
     * qu'il est renseigné.</p>
     */
    @Column(name = TableNames.COL_BIOMETRIC_ID, length = 100)
    private String biometricId;

    @Column(nullable = false)
    private boolean actif = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", unique = true,
            foreignKey = @ForeignKey(name = "fk_personnels_utilisateur"))
    private Utilisateur utilisateur;

    /**
     * Indique si l'agent dispose d'une empreinte enrôlée et peut donc pointer.
     *
     * @return {@code true} si un {@code biometricId} est associé
     */
    public boolean isEnrole() {
        return biometricId != null && !biometricId.isBlank();
    }

    @PrePersist
    void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
