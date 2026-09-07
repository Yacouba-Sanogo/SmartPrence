package com.smartpresence.entity;

import com.smartpresence.constants.StatutSignalement;
import com.smartpresence.constants.TableNames;
import com.smartpresence.constants.TypeSignalement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Anomalie constatée par un enseignant sur une séance.
 *
 * <h2>Pourquoi une entité distincte plutôt qu'une correction directe</h2>
 * <p>L'enseignant ne modifie jamais un relevé : il <b>témoigne</b>, et la scolarité
 * <b>arbitre</b>. Cette séparation est ce qui préserve la valeur probante du système —
 * si chaque enseignant pouvait réécrire les présences de son cours, le relevé
 * biométrique ne vaudrait pas mieux qu'une feuille d'émargement.</p>
 *
 * <p>Le signalement laisse donc une trace propre : qui a signalé, quoi, quand, qui a
 * tranché et sur quel motif. La correction éventuelle est un {@link Presence} de source
 * {@code MANUEL} produit à l'acceptation, distinct et daté.</p>
 *
 * @since 0.0.1
 */
@Entity
@Table(
        name = TableNames.SIGNALEMENTS_PRESENCE,
        indexes = {
                @Index(name = "idx_signalements_seance", columnList = "seance_id"),
                @Index(name = "idx_signalements_statut", columnList = "statut")
        }
)
@Getter
@Setter
@ToString(exclude = {"seance", "enseignant", "etudiant", "traitePar"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SignalementPresence extends BaseAuditableEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id", length = 36)
    private UUID id;

    /** Séance concernée. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seance_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_signalements_seance"))
    private Seance seance;

    /** Enseignant à l'origine du signalement. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enseignant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_signalements_enseignant"))
    private Personnel enseignant;

    /**
     * Étudiant concerné.
     *
     * <p>{@code null} lorsque l'anomalie touche la séance entière — un lecteur en panne
     * ne vise personne en particulier.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id",
            foreignKey = @ForeignKey(name = "fk_signalements_etudiant"))
    private Etudiant etudiant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TypeSignalement type;

    /** Description libre, obligatoire : c'est le témoignage de l'enseignant. */
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutSignalement statut = StatutSignalement.EN_ATTENTE;

    /** Agent de la scolarité ayant arbitré, {@code null} tant que le signalement est en attente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_id",
            foreignKey = @ForeignKey(name = "fk_signalements_traite_par"))
    private Utilisateur traitePar;

    /** Motif de la décision, consigné à l'arbitrage. */
    @Column(name = "commentaire_traitement", length = 500)
    private String commentaireTraitement;

    @Column(name = "traite_le")
    private Instant traiteLe;

    /**
     * Relevé correctif produit à l'acceptation, s'il y a lieu.
     *
     * <p>Rend le lien explicite entre un témoignage et la présence qui en découle :
     * un contrôle ultérieur peut remonter de la ligne corrigée jusqu'à sa justification.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presence_corrective_id",
            foreignKey = @ForeignKey(name = "fk_signalements_presence"))
    private Presence presenceCorrective;

    public boolean estEnAttente() {
        return statut == StatutSignalement.EN_ATTENTE;
    }

    @PrePersist
    void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
