package com.smartpresence.entity;

import com.smartpresence.constants.StatutReclamation;
import com.smartpresence.constants.TableNames;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Contestation d'une note par l'étudiant, traitée par l'enseignant qui l'a saisie.
 *
 * <p>L'ancienne et la nouvelle valeur sont conservées : une note publiée qui change doit
 * laisser une trace de qui l'a changée, pourquoi, et de combien.</p>
 */
@Entity
@Table(name = TableNames.RECLAMATIONS,
        indexes = {
                @Index(name = "idx_reclamations_enseignant", columnList = "enseignant_id,statut"),
                @Index(name = "idx_reclamations_etudiant", columnList = "etudiant_id")
        })
@Getter @Setter @ToString(exclude = {"note", "etudiant", "enseignant"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Reclamation extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "note_id", nullable = false) private Note note;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "etudiant_id", nullable = false) private Etudiant etudiant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "enseignant_id", nullable = false) private Personnel enseignant;
    @Column(nullable = false, length = 1000) private String motif;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutReclamation statut = StatutReclamation.OUVERTE;
    @Column(length = 1000) private String reponse;
    @Column(name = "valeur_avant", precision = 4, scale = 2) private BigDecimal valeurAvant;
    @Column(name = "valeur_apres", precision = 4, scale = 2) private BigDecimal valeurApres;
    @Column(name = "traitee_le") private Instant traiteeLe;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
