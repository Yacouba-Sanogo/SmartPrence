package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Salle physique équipée d'un appareil ESP32.
 *
 * <p>Entité de <b>référentiel d'infrastructure</b>. Reliée à un {@link Device} par une
 * relation <b>1-1 bidirectionnelle</b> : {@code Device} est propriétaire (porte la FK
 * {@code salle_id}), {@code Salle} en est la face inverse ({@code mappedBy}).</p>
 *
 * <h2>Choix de conception</h2>
 * <ul>
 *   <li><b>Clé technique {@code Long} auto-incrémentée</b>.</li>
 *   <li><b>{@code capacite} en {@link Integer}</b> (et non {@code int} primitif) :
     *   une salle peut avoir une capacité inconnue, donc <b>nullable</b>.</li>
 *   <li><b>Relation {@code device} en LAZY</b> : l'appareil n'est chargé qu'à la demande.</li>
 * </ul>
 *
 * @since 0.0.1
 * @see Device
 */
@Entity
@Table(
        name = TableNames.SALLES,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_salles_code",
                columnNames = TableNames.COL_CODE
        ),
        indexes = @Index(name = "idx_salles_code", columnList = TableNames.COL_CODE)
)
@Getter
@Setter
@ToString(exclude = {"device"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Salle extends BaseAuditableEntity {

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
     * Code métier unique de la salle (ex. {@code B12-101}).
     */
    @Column(name = TableNames.COL_CODE, nullable = false, length = 50)
    private String code;

    /**
     * Libellé lisible de la salle.
     */
    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    /**
     * Bâtiment dans lequel se trouve la salle (optionnel).
     */
    @Column(name = "batiment", length = 100)
    private String batiment;

    /**
     * Capacité d'accueil en nombre de places.
     * <p>Type {@link Integer} (et non {@code int} primitif) pour permettre une valeur
     * {@code null} lorsque la capacité est inconnue.</p>
     */
    @Column(name = "capacite")
    private Integer capacite;

    /**
     * Appareil ESP32 installé dans la salle.
     *
     * <p><b>1-1 bidirectionnelle, face inverse</b> ({@code mappedBy = "salle"} côté
     * {@link Device}). {@link FetchType#LAZY} : l'appareil n'est chargé qu'à la demande.
     * <b>Aucune cascade</b>.</p>
     */
    @OneToOne(mappedBy = "salle", fetch = FetchType.LAZY)
    private Device device;

}
