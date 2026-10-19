package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Absence d'un étudiant à une évaluation.
 *
 * <p>Distincte d'un zéro : un absent n'a pas été noté, et une moyenne ne doit pas
 * l'apprendre à ses dépens sans décision de l'administration.</p>
 */
@Entity
@Table(name = TableNames.ABSENCES_EVALUATION,
        uniqueConstraints = @UniqueConstraint(name = "uk_absence_evaluation", columnNames = {"evaluation_id", "etudiant_id"}))
@Getter @Setter @ToString(exclude = {"evaluation", "etudiant"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class AbsenceEvaluation extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "evaluation_id", nullable = false) private Evaluation evaluation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "etudiant_id", nullable = false) private Etudiant etudiant;
    @Column(length = 300) private String motif;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
