package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Groupe d'étudiants appartenant à une {@link Promotion}.
 *
 * <p>Entité <b>organisationnelle</b> : elle agrège des étudiants (relation 1-N) et est
 * animée par plusieurs enseignants (relation N-N).</p>
 *
 * <h2>Relations</h2>
 * <ul>
 *   <li>{@link Promotion} : <b>N-1 unidirectionnelle, EAGER</b> — la promotion est
 *       toujours nécessaire à l'affichage d'une classe (information légère et systématique).</li>
 *   <li>{@link Enseignant} : <b>N-N unidirectionnelle, LAZY</b> — la liste des enseignants
 *       d'une classe n'est chargée qu'à la demande.</li>
 *   <li>{@link Etudiant} : <b>1-N bidirectionnelle, LAZY</b> — propriétaire côté
 *       {@code Etudiant} ({@code mappedBy = "classe"}). La bidirectionnalité est justifiée
 *       par la consultation fréquente des étudiants d'une classe.</li>
 * </ul>
 *
 * <p><b>Aucune cascade</b>, <b>orphanRemoval = false</b> : la suppression d'une classe ne
 * supprime jamais ses étudiants (changement de classe ≠ suppression de l'étudiant).</p>
 *
 * @since 0.0.1
 */
@Entity
@Table(
        name = TableNames.CLASSES,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_classes_code",
                columnNames = TableNames.COL_CODE
        ),
        indexes = @Index(name = "idx_classes_code", columnList = TableNames.COL_CODE)
)
@Getter
@Setter
@ToString(exclude = {"etudiants", "enseignants", "promotion"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Classe extends BaseAuditableEntity {

    /**
     * Identifiant technique auto-généré.
     * <p>Seul champ retenu pour {@link #equals(Object)} / {@link #hashCode()},
     * conformément aux recommandations Hibernate.</p>
     */
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Code métier unique de la classe (ex. {@code L3-INFO-A}).
     */
    @Column(name = TableNames.COL_CODE, nullable = false, length = 50)
    private String code;

    /**
     * Libellé lisible de la classe.
     */
    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    /**
     * Promotion à laquelle appartient la classe.
     *
     * <p><b>N-1 unidirectionnelle, EAGER</b> (information toujours nécessaire à l'affichage
     * d'une classe). Aucune cascade.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_classes_promotion"))
    private Promotion promotion;

    /**
     * Étudiants appartenant à la classe.
     *
     * <p><b>1-N bidirectionnelle, LAZY</b>, propriétaire côté {@code Etudiant}
     * ({@code mappedBy = "classe"}). <b>Aucune cascade</b>, <b>orphanRemoval = false</b>.</p>
     *
     * <p>Initialisé en {@link ArrayList} mutable pour permettre l'ajout depuis JPA
     * tout en contrôlant l'accès côté service via les DTO.</p>
     */
    @OneToMany(mappedBy = "classe", fetch = FetchType.LAZY)
    private List<Etudiant> etudiants = new ArrayList<>();

    /**
     * Enseignants intervenant dans la classe.
     *
     * <p><b>N-N unidirectionnelle, LAZY</b>, propriétaire côté {@code Classe}, via la
     * table de jointure {@code classes_enseignants}. <b>Aucune cascade</b> :
     * la suppression d'une classe ne supprime jamais les enseignants (entités partagées).</p>
     *
     * <p>Le type cible est {@link Personnel} — de catégorie
     * {@link com.smartpresence.constants.TypePersonnel#ENSEIGNANT} — et non une entité
     * {@code Enseignant} distincte. Le modèle en comportait deux, sans lien entre elles :
     * les séances pointaient vers {@code Personnel}, le rattachement aux classes vers
     * {@code Enseignant}. Un même professeur aurait donc eu deux fiches, et « mes classes »
     * et « mes séances » n'auraient jamais pu désigner la même personne.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = TableNames.JOIN_CLASSES_ENSEIGNANTS,
            joinColumns = @JoinColumn(name = "classe_id"),
            inverseJoinColumns = @JoinColumn(name = "enseignant_id")
    )
    private Set<Personnel> enseignants = new HashSet<>();

}
