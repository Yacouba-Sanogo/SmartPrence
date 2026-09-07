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
 * Rôle applicatif dans le cadre du contrôle d'accès basé sur les rôles (RBAC multi-rôles).
 *
 * <p>Entité de <b>référentiel système</b> : faible volume, forte stabilité. Référencée
 * par {@code Utilisateur} via une relation N-N (table {@code utilisateurs_roles}).</p>
 *
 * <h2>Choix de conception</h2>
 * <ul>
 *   <li><b>Clé technique {@code Long} auto-incrémentée</b> : entité de référentiel non
 *       exposée directement à l'externe, jointures simples et performantes.</li>
 *   <li><b>{@code code} stocké en {@link String}</b> (et non mappé depuis l'énumération
 *       {@code RoleCode}) : découple le référentiel de persistance de l'énumération
 *       applicative. Permet d'ajouter ou modérer des rôles en base sans recompiler.
 *       {@code RoleCode} reste utilisé pour le <i>seeding</i> et la logique d'autorisation.</li>
 * </ul>
 *
 * @since 0.0.1
 * @see com.smartpresence.constants.RoleCode
 */
@Entity
@Table(
        name = TableNames.ROLES,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roles_code",
                columnNames = TableNames.COL_CODE
        ),
        indexes = @Index(name = "idx_roles_code", columnList = TableNames.COL_CODE)
)
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Role extends BaseAuditableEntity {

    /**
     * Identifiant technique auto-généré.
     * <p>Seul champ retenu pour {@link #equals(Object)} / {@link #hashCode()},
     * conformément aux recommandations Hibernate (pas de comparaison des propriétés métier).</p>
     */
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Code métier unique du rôle (ex. {@code ADMIN}, {@code ENSEIGNANT}).
     * <p>Correspond à l'une des valeurs de {@code RoleCode}, mais stocké en
     * {@link String} pour découpler persistance et énumération applicative.</p>
     */
    @Column(name = TableNames.COL_CODE, nullable = false, length = 50)
    private String code;

    /**
     * Libellé lisible du rôle (ex. « Administrateur »).
     */
    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    /**
     * Description optionnelle du rôle et de son périmètre.
     */
    @Column(name = "description", length = 255)
    private String description;

}
