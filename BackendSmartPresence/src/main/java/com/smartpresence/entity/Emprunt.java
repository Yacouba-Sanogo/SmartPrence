package com.smartpresence.entity;

import com.smartpresence.constants.StatutEmprunt;
import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Prêt d'un exemplaire à un étudiant. Le retard est calculé à partir de la date de retour prévue. */
@Entity
@Table(name = TableNames.BIBLIOTHEQUE_EMPRUNTS,
        indexes = @Index(name = "idx_emprunts_ouverts", columnList = "retourne_le,retour_prevu"))
@Getter @Setter @ToString(exclude = {"livre", "etudiant"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Emprunt extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "livre_id", nullable = false) private Livre livre;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "etudiant_id", nullable = false) private Etudiant etudiant;
    @Column(name = "emprunte_le", nullable = false) private LocalDate emprunteLe;
    @Column(name = "retour_prevu", nullable = false) private LocalDate retourPrevu;
    @Column(name = "retourne_le") private LocalDate retourneLe;
    @Column(nullable = false) private int prolongations;
    /** Dernier jour où un rappel a été envoyé : évite d'en envoyer un par jour. */
    @Column(name = "dernier_rappel_le") private LocalDate dernierRappelLe;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }

    @Transient
    public StatutEmprunt statutAu(LocalDate jour) {
        if (retourneLe != null) return StatutEmprunt.RETOURNE;
        return retourPrevu.isBefore(jour) ? StatutEmprunt.EN_RETARD : StatutEmprunt.EN_COURS;
    }

    /** Jours écoulés depuis la date prévue ; 0 si le livre n'est pas en retard. */
    @Transient
    public long joursDeRetardAu(LocalDate jour) {
        LocalDate fin = retourneLe != null ? retourneLe : jour;
        return Math.max(0, ChronoUnit.DAYS.between(retourPrevu, fin));
    }
}
