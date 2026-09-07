package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Promotion universitaire (filière + niveau + année universitaire).
 *
 * <p>Entité de <b>référentiel organisationnel</b>, pivot des statistiques agrégées
 * par promotion. Regroupe des {@code Classe}s (relation 1-N, ajoutée au sous-groupe 2B).</p>
 *
 * <h2>Choix de conception</h2>
 * <ul>
 *   <li><b>Clé technique {@code Long} auto-incrémentée</b>.</li>
 *   <li><b>{@code anneeUniversitaire} en {@link String}</b> (ex. {@code "2025-2026"}) :
 *       ce n'est pas une date ponctuelle mais une période académique à format libre
 *       selon les établissements ; un {@code LocalDate} serait sémantiquement faux.</li>
 *   <li><b>{@code filiere} / {@code niveau} textuels</b> : flexibilité métier (les
 *       intitulés varient d'un établissement à l'autre).</li>
 * </ul>
 *
 * @since 0.0.1
 */
@Entity
@Table(
        name = TableNames.PROMOTIONS,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_promotions_code",
                columnNames = TableNames.COL_CODE
        ),
        indexes = @Index(name = "idx_promotions_code", columnList = TableNames.COL_CODE)
)
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Promotion extends BaseAuditableEntity {

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
     * Code métier unique de la promotion (ex. {@code L3-INFO-2026}).
     */
    @Column(name = TableNames.COL_CODE, nullable = false, length = 50)
    private String code;

    /**
     * Libellé lisible de la promotion.
     */
    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    /**
     * Filière (ex. « Informatique »).
     */
    @Column(name = "filiere", nullable = false, length = 100)
    private String filiere;

    /**
     * Niveau d'étude (ex. « Licence 3 », « Master 1 »).
     */
    @Column(name = "niveau", nullable = false, length = 50)
    private String niveau;

    /**
     * Année universitaire au format libre (ex. {@code "2025-2026"}).
     * <p>Stockée en {@link String} car il s'agit d'une période académique,
     * non d'une date ponctuelle.</p>
     */
    @Column(name = "annee_universitaire", nullable = false, length = 20)
    private String anneeUniversitaire;

}
