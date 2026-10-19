package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Page du cahier de textes : ce qui a été enseigné pendant une séance.
 *
 * <p>Une séance a au plus une page ; la contrainte d'unicité empêche deux écritures
 * concurrentes de produire deux pages pour le même cours.</p>
 */
@Entity
@Table(name = TableNames.CAHIERS_TEXTES,
        uniqueConstraints = @UniqueConstraint(name = "uk_cahier_seance", columnNames = "seance_id"))
@Getter @Setter @ToString(exclude = {"seance", "chapitre"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CahierTexte extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seance_id", nullable = false) private Seance seance;
    @Column(nullable = false, length = 2000) private String contenu;
    @Column(name = "travail_a_faire", length = 500) private String travailAFaire;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chapitre_id") private Chapitre chapitre;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
