package com.smartpresence.entity;

import com.smartpresence.constants.StatutDocument;
import com.smartpresence.constants.TableNames;
import com.smartpresence.constants.TypeDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

import java.util.UUID;

/**
 * Mémoire ou ancien sujet d'examen de la bibliothèque numérique.
 *
 * <p>Les métadonnées vivent ici, le contenu dans {@link DocumentContenu}. Un document déposé
 * par un étudiant reste {@code EN_ATTENTE} jusqu'à la décision de l'administration ; celui
 * de l'administration est publié d'emblée.</p>
 */
@Entity
@Table(name = TableNames.BIBLIOTHEQUE_DOCUMENTS,
        indexes = @Index(name = "idx_biblio_doc_statut", columnList = "statut,type"))
@Getter @Setter @ToString(exclude = "filiere")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class DocumentBibliotheque extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TypeDocument type;
    @Column(nullable = false, length = 160) private String titre;
    /** Auteur du mémoire ; vide pour un sujet. */
    @Column(length = 150) private String auteur;
    /** Matière d'un sujet d'examen. */
    @Column(length = 150) private String matiere;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "filiere_id") private Filiere filiere;
    /** Année du mémoire ou de l'examen. */
    @Column private Integer annee;
    @Column(length = 500) private String description;
    @Column(name = "nom_fichier", nullable = false, length = 255) private String nomFichier;
    @Column(name = "type_mime", nullable = false, length = 120) private String typeMime;
    @Column(nullable = false) private long taille;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutDocument statut;
    @Column(name = "motif_refus", length = 500) private String motifRefus;
    /** Compte qui a déposé le document : il peut le retirer tant qu'il n'est pas publié. */
    @Column(name = "depose_par_id", nullable = false, length = 36) private UUID deposeParId;
    @Column(name = "depose_par_nom", nullable = false, length = 200) private String deposeParNom;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
