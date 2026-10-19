package com.smartpresence.entity;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.TableNames;
import com.smartpresence.constants.TypeEvaluation;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Évaluation planifiée par un enseignant : devoir, interrogation, examen…
 *
 * <p>Le {@code bareme} est le total de points de l'épreuve. Les notes se saisissent sur
 * ce barème et le serveur les ramène sur 20 : les moyennes, elles, ne connaissent que le
 * « sur 20 ».</p>
 */
@Entity
@Table(name = TableNames.EVALUATIONS,
        indexes = @Index(name = "idx_evaluations_classe", columnList = "classe_id,date_evaluation"))
@Getter @Setter @ToString(exclude = {"classe", "matiere", "enseignant"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Evaluation extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "classe_id", nullable = false) private Classe classe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "matiere_id", nullable = false) private Matiere matiere;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "enseignant_id", nullable = false) private Personnel enseignant;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TypeEvaluation type = TypeEvaluation.DEVOIR;
    @Column(nullable = false, length = 120) private String libelle;
    @Column(name = "date_evaluation", nullable = false) private LocalDate dateEvaluation;
    @Column(nullable = false) private int bareme = 20;
    @Column(nullable = false) private int coefficient = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PeriodeScolaire periode = PeriodeScolaire.SEMESTRE_1;
    /** Énoncé ou consignes de l'épreuve, tels que l'enseignant les prépare. */
    @Column(length = 2000) private String sujet;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
