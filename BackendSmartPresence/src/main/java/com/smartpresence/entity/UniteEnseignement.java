package com.smartpresence.entity;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Unité d'enseignement — le niveau auquel se gagnent les crédits.
 *
 * <h2>Pourquoi deux niveaux et non un seul</h2>
 * <p>Le LMD ne valide pas des matières mais des unités : une UE regroupe plusieurs
 * ECUE (les matières, ici {@link Matiere}), porte un nombre de crédits, et c'est sa
 * moyenne — pondérée par les crédits de ses ECUE — qui décide de leur acquisition.
 * Un modèle à un seul niveau sait dire « 12 en Algorithmique » ; il ne sait pas dire
 * « UE Informatique fondamentale acquise, 6 crédits ».</p>
 *
 * <h2>Rattachement</h2>
 * <p>Une UE appartient à une {@link Promotion} et à un semestre : « Licence 1
 * Informatique, semestre 1 » a ses UE, que partagent toutes les classes de cette
 * promotion. Les rattacher à la classe les aurait dupliquées autant de fois qu'il y
 * a de groupes, avec le risque qu'ils divergent.</p>
 *
 * @since 0.0.1
 * @see Matiere
 */
@Entity
@Table(
        name = TableNames.UNITES_ENSEIGNEMENT,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_unites_enseignement_code",
                columnNames = TableNames.COL_CODE
        ),
        indexes = {
                @Index(name = "idx_unites_enseignement_promotion", columnList = "promotion_id"),
                @Index(name = "idx_unites_enseignement_semestre", columnList = "semestre")
        }
)
@Getter
@Setter
@ToString(exclude = "promotion")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class UniteEnseignement extends BaseAuditableEntity {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** « UE-INFO-11 » — repère court, unique dans l'établissement. */
    @Column(name = TableNames.COL_CODE, nullable = false, length = 50)
    private String code;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    /**
     * Crédits que l'UE rapporte lorsqu'elle est acquise.
     *
     * <p>Somme attendue de 30 par semestre — mais la contrainte n'est pas posée ici :
     * une maquette pédagogique en cours de saisie passe forcément par des états
     * incomplets, et refuser d'enregistrer la quatrième UE parce que le total n'est
     * pas encore atteint rendrait la saisie impossible.</p>
     */
    @Column(name = "credits", nullable = false)
    private int credits;

    @Enumerated(EnumType.STRING)
    @Column(name = TableNames.COL_SEMESTRE, nullable = false, length = 20)
    private PeriodeScolaire semestre = PeriodeScolaire.SEMESTRE_1;

    /** Promotion dont l'UE fait partie de la maquette. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_unites_enseignement_promotion"))
    private Promotion promotion;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
