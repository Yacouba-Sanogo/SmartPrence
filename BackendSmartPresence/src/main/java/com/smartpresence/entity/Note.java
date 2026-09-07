package com.smartpresence.entity;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.TableNames;
import com.smartpresence.constants.TypeEvaluation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Note obtenue par un étudiant dans une matière.
 *
 * <h2>Pourquoi un {@link BigDecimal} et non un {@code double}</h2>
 * <p>Une moyenne se calcule à partir de ces valeurs et s'affiche sur un bulletin. Le
 * flottant binaire ne représente pas exactement 12,5 ni 0,1 : une somme de vingt notes
 * dériverait, et un étudiant à 9,995 pourrait basculer du mauvais côté d'un seuil. La
 * précision décimale est ici une exigence, pas une préférence.</p>
 *
 * <h2>Le coefficient porte le poids, pas le type</h2>
 * <p>{@link TypeEvaluation} sert à l'affichage et au tri. Deux devoirs de même type
 * peuvent peser différemment ; c'est {@code coefficient} qui l'exprime, et lui seul
 * entre dans le calcul.</p>
 *
 * <h2>Qui a noté</h2>
 * <p>L'enseignant est conservé : une note est un jugement, et l'auteur d'un jugement
 * doit rester identifiable. C'est aussi ce qui permet de vérifier qu'un enseignant ne
 * note que dans les classes où il intervient.</p>
 *
 * @since 0.0.1
 */
@Entity
@Table(
        name = TableNames.NOTES,
        indexes = {
                @Index(name = "idx_notes_etudiant_periode",
                        columnList = TableNames.COL_ETUDIANT_ID + "," + TableNames.COL_PERIODE),
                @Index(name = "idx_notes_matiere",
                        columnList = TableNames.COL_MATIERE_ID)
        }
)
@Getter
@Setter
@ToString(exclude = {"etudiant", "matiere", "enseignant"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Note extends BaseAuditableEntity {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "id", length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = TableNames.COL_ETUDIANT_ID, nullable = false,
            foreignKey = @ForeignKey(name = "fk_notes_etudiant"))
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = TableNames.COL_MATIERE_ID, nullable = false,
            foreignKey = @ForeignKey(name = "fk_notes_matiere"))
    private Matiere matiere;

    /** Enseignant auteur de la note. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = TableNames.COL_PERSONNEL_ID, nullable = false,
            foreignKey = @ForeignKey(name = "fk_notes_enseignant"))
    private Personnel enseignant;

    /**
     * Valeur sur 20.
     *
     * <p>{@code precision = 4, scale = 2} : de 0,00 à 20,00 au quart de point près,
     * ce qui couvre toutes les pratiques de notation en vigueur.</p>
     */
    @Column(name = "valeur", nullable = false, precision = 4, scale = 2)
    private BigDecimal valeur;

    /** Poids de la note dans la moyenne de la matière. */
    @Column(name = "coefficient", nullable = false)
    private int coefficient = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_evaluation", nullable = false, length = 20)
    private TypeEvaluation type = TypeEvaluation.DEVOIR;

    @Enumerated(EnumType.STRING)
    @Column(name = TableNames.COL_PERIODE, nullable = false, length = 20)
    private PeriodeScolaire periode = PeriodeScolaire.SEMESTRE_1;

    /** Intitulé libre — « Devoir n°2 », « Contrôle continu »… */
    @Column(name = "libelle", nullable = false, length = 120)
    private String libelle;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;

    /** Commentaire de l'enseignant, visible par l'étudiant. */
    @Column(name = "appreciation", length = 500)
    private String appreciation;

    @PrePersist
    private void genererIdentifiant() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /**
     * Contribution pondérée de la note, pour le calcul d'une moyenne.
     *
     * <p>Exposée ici plutôt que recalculée à chaque endroit qui en a besoin : la
     * pondération est une propriété de la note, pas du bulletin.</p>
     */
    public BigDecimal contributionPonderee() {
        return valeur.multiply(BigDecimal.valueOf(coefficient));
    }
}
