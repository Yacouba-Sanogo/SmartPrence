package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/** Livre physique du fonds de la bibliothèque. Le nombre disponible se déduit des emprunts en cours. */
@Entity
@Table(name = TableNames.BIBLIOTHEQUE_LIVRES)
@Getter @Setter @ToString(exclude = "filiere")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Livre extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @Column(nullable = false, length = 200) private String titre;
    @Column(nullable = false, length = 150) private String auteur;
    @Column(length = 30) private String isbn;
    @Column(length = 80) private String categorie;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "filiere_id") private Filiere filiere;
    @Column(length = 500) private String resume;
    /** Rayon ou étagère, pour retrouver le livre dans la salle. */
    @Column(length = 80) private String emplacement;
    @Column(nullable = false) private int exemplaires = 1;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
