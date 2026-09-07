package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Compte d'authentification humain (administrateurs, enseignants, responsables
 * de scolarité, superviseurs).
 *
 * <p>Authentification <b>JWT</b> côté Flutter. L'identifiant applicatif de connexion
 * est l'{@code email}. Le mot de passe est stocké sous forme <b>hachée (BCrypt)</b> et
 * n'est jamais exposé en dehors de la couche service.</p>
 *
 * <h2>RBAC multi-rôles</h2>
 * <p>Un utilisateur possède un <b>ensemble de rôles</b> ({@link Set} de {@link Role}),
 * ce qui permet de cumuler plusieurs responsabilités (par ex. {@code ENSEIGNANT} +
 * {@code SUPERVISEUR}). Cette relation est <b>unidirectionnelle</b> et <b>LAZY</b> :
 * {@code Utilisateur} en est le propriétaire, {@code Role} ignore la liste de ses
 * utilisateurs.</p>
 *
 * <h2>Choix techniques</h2>
 * <ul>
 *   <li><b>Clé primaire {@link UUID}</b> : anti-énumération, générée côté application
 *       via {@link PrePersist} (aucune dépendance à un mécanisme de génération base).</li>
 *   <li><b>{@link ManyToMany} sans cascade</b> : la suppression d'un utilisateur ne doit
 *       jamais affecter le référentiel des rôles (partagé et stable).</li>
 * </ul>
 *
 * @since 0.0.1
 * @see Role
 */
@Entity
@Table(
        name = TableNames.UTILISATEURS,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_utilisateurs_email",
                columnNames = TableNames.COL_EMAIL
        ),
        indexes = @Index(name = "idx_utilisateurs_email", columnList = TableNames.COL_EMAIL)
)
@Getter
@Setter
@ToString(exclude = {"roles"}) // évite chargement LAZY involontaire du Set<Role>
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Utilisateur extends BaseAuditableEntity {

    /**
     * Identifiant technique de l'utilisateur, généré côté application avant insertion.
     * <p>Seul champ retenu pour {@link #equals(Object)} / {@link #hashCode()},
     * conformément aux recommandations Hibernate.</p>
     */
    @EqualsAndHashCode.Include
    @Id
    @Column(name = "id", length = 36)
    private UUID id;

    /**
     * Email unique servant d'identifiant de connexion.
     */
    @Column(name = TableNames.COL_EMAIL, nullable = false, length = 150)
    private String email;

    /**
     * Mot de passe <b>haché (BCrypt)</b>.
     * <p>Jamais stocké en clair, jamais exposé via les DTO de sortie.</p>
     */
    @Column(name = "mot_de_passe", nullable = false, length = 100)
    private String motDePasse;

    /**
     * Nom de famille de l'utilisateur.
     */
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    /**
     * Prénom de l'utilisateur.
     */
    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    /**
     * Indique si le compte est actif.
     * <p>Permet une <b>désactivation sans suppression</b> (traçabilité, audit).</p>
     */
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /**
     * Ensemble des rôles attribués à l'utilisateur (RBAC multi-rôles).
     *
     * <p><b>Relation N-N unidirectionnelle</b>, propriétaire côté {@code Utilisateur},
     * via la table de jointure {@code utilisateurs_roles}.</p>
     *
     * <p>{@link FetchType#LAZY} : les rôles ne sont chargés qu'à la demande
     * (typiquement lors de l'autorisation Spring Security).</p>
     *
     * <p><b>Aucune cascade</b> : la gestion des rôles reste explicite, la suppression
     * d'un utilisateur n'affecte jamais le référentiel des rôles.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = TableNames.JOIN_UTILISATEURS_ROLES,
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Génère l'identifiant {@link UUID} avant l'insertion en base si nécessaire.
     */
    @PrePersist
    void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

}
