package com.smartpresence.entity;

import com.smartpresence.constants.StatutSeance;
import com.smartpresence.constants.TableNames;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.Instant;
import java.util.UUID;

/** Séance planifiée : le contexte académique d'une présence étudiante. */
@Entity @Table(name = TableNames.SEANCES, indexes = @Index(name = "idx_seances_debut", columnList = "debut"))
@Getter @Setter @ToString(exclude = {"classe", "matiere", "enseignant", "salle"}) @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Seance extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "classe_id", nullable = false) private Classe classe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "matiere_id", nullable = false) private Matiere matiere;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "enseignant_id", nullable = false) private Personnel enseignant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "salle_id") private Salle salle;
    @Column(nullable = false) private Instant debut;
    @Column(nullable = false) private Instant fin;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutSeance statut = StatutSeance.PLANIFIEE;
    @Column(length = 500) private String note;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
