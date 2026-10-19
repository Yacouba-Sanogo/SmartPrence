package com.smartpresence.entity;

import com.smartpresence.constants.StatutDemande;
import com.smartpresence.constants.TableNames;
import com.smartpresence.constants.TypeConge;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Demande de congé ou d'absence d'un enseignant, soumise à la décision de l'administration.
 *
 * <p>La demande ne modifie pas l'emploi du temps : replanifier les séances touchées reste
 * un acte de l'administration, informée par la décision.</p>
 */
@Entity
@Table(name = TableNames.DEMANDES_CONGE,
        indexes = @Index(name = "idx_conges_enseignant", columnList = "enseignant_id,debut"))
@Getter @Setter @ToString(exclude = "enseignant")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class DemandeConge extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "enseignant_id", nullable = false) private Personnel enseignant;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TypeConge type = TypeConge.CONGE;
    @Column(nullable = false) private LocalDate debut;
    @Column(nullable = false) private LocalDate fin;
    @Column(nullable = false, length = 500) private String motif;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutDemande statut = StatutDemande.EN_ATTENTE;
    @Column(length = 500) private String reponse;
    @Column(name = "traitee_le") private Instant traiteeLe;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
