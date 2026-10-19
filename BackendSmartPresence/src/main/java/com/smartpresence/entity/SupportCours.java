package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Support de cours publié par un enseignant pour une classe : PDF, présentation, énoncé
 * d'exercices.
 *
 * <p>Les métadonnées vivent ici, le contenu dans {@link SupportContenu} : une liste de
 * supports ne doit pas charger des mégaoctets pour afficher des titres.</p>
 */
@Entity
@Table(name = TableNames.SUPPORTS_COURS,
        indexes = @Index(name = "idx_supports_classe", columnList = "classe_id,created_at"))
@Getter @Setter @ToString(exclude = {"classe", "matiere", "enseignant"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SupportCours extends BaseAuditableEntity {
    @Id @EqualsAndHashCode.Include @Column(length = 36) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "classe_id", nullable = false) private Classe classe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "matiere_id", nullable = false) private Matiere matiere;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "enseignant_id", nullable = false) private Personnel enseignant;
    @Column(nullable = false, length = 160) private String titre;
    @Column(length = 500) private String description;
    @Column(name = "nom_fichier", nullable = false, length = 255) private String nomFichier;
    @Column(name = "type_mime", nullable = false, length = 120) private String typeMime;
    @Column(nullable = false) private long taille;
    @PrePersist void generateId() { if (id == null) id = UUID.randomUUID(); }
}
