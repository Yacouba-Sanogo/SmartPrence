package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Chapitre du programme qu'un enseignant donne à une classe dans une matière.
 *
 * <p>L'ensemble des chapitres d'une (classe, matière, enseignant) forme son syllabus ;
 * la part de chapitres terminés en donne l'avancement.</p>
 */
@Entity
@Table(name = TableNames.CHAPITRES,
        indexes = @Index(name = "idx_chapitres_contexte", columnList = "classe_id,matiere_id,enseignant_id"))
@Getter @Setter @ToString(exclude = {"classe", "matiere", "enseignant"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Chapitre extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "classe_id", nullable = false) private Classe classe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "matiere_id", nullable = false) private Matiere matiere;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "enseignant_id", nullable = false) private Personnel enseignant;
    @Column(nullable = false) private int ordre;
    @Column(nullable = false, length = 150) private String titre;
    @Column(length = 500) private String objectifs;
    @Column(nullable = false) private boolean termine;
    @Column(name = "termine_le") private LocalDate termineLe;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
