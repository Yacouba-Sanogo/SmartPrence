package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Étudiant du référentiel, identifié biométriquement côté ESP32.
 *
 * <p><b>Aucune donnée biométrique n'est stockée</b> dans cette entité. Le champ
 * {@code biometricId} est une <b>référence logique de correspondance</b> reliant
 * l'étudiant à son empreinte dans le système embarqué AS608 — c'est un identifiant,
 * jamais une empreinte (cf. {@code SmartPresence_CONTEXT.md} §13).</p>
 *
 * <h2>Relation vers Classe</h2>
 * <p><b>N-1 unidirectionnelle (côté propriétaire), LAZY</b> : {@code Etudiant} porte la
     * clé étrangère {@code classe_id}. La relation est la face propriétaire de la relation
     * bidirectionnelle déclarée côté {@link Classe}. <b>Aucune cascade</b>.</p>
 *
 * @since 0.0.1
 * @see Classe
 */
@Entity
@Table(
        name = TableNames.ETUDIANTS,
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_etudiants_matricule", columnNames = TableNames.COL_MATRICULE),
                @UniqueConstraint(name = "uk_etudiants_biometric_id", columnNames = TableNames.COL_BIOMETRIC_ID)
        },
        indexes = {
                @Index(name = "idx_etudiants_matricule", columnList = TableNames.COL_MATRICULE),
                @Index(name = "idx_etudiants_biometric_id", columnList = TableNames.COL_BIOMETRIC_ID)
        }
)
@Getter
@Setter
@ToString(exclude = {"classe", "utilisateur"}) // évite un chargement LAZY involontaire
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Etudiant extends BaseAuditableEntity {

    /**
     * Identifiant technique de l'étudiant, généré côté application.
     * <p>Cet UUID correspond au {@code studentId} échangé avec l'ESP32.</p>
     */
    @EqualsAndHashCode.Include
    @Id
    @Column(name = "id", length = 36)
    private UUID id;

    /**
     * Matricule métier unique de l'étudiant (identifiant lisible).
     */
    @Column(name = TableNames.COL_MATRICULE, nullable = false, length = 50)
    private String matricule;

    /**
     * Nom de famille de l'étudiant.
     */
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    /**
     * Prénom de l'étudiant.
     */
    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    /**
     * Email de l'étudiant (optionnel).
     */
    @Column(name = TableNames.COL_EMAIL, length = 150)
    private String email;

    /**
     * Téléphone de l'étudiant (optionnel).
     */
    @Column(name = "telephone", length = 20)
    private String telephone;

    /**
     * Date de naissance de l'étudiant (optionnelle).
     */
    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    /**
     * Référence logique de correspondance biométrique.
     *
     * <p><b>Ce n'est PAS une empreinte digitale.</b> Il s'agit d'un identifiant logique
     * reliant l'étudiant à son empreinte stockée dans le système embarqué AS608.
     * Le backend ne stocke <b>aucune donnée biométrique</b>.</p>
     *
     * <p><b>Nullable</b>, comme pour {@link Personnel} : une scolarité inscrit ses
     * étudiants en début d'année et enrôle leurs empreintes ensuite, au fil des
     * passages devant le capteur. L'exiger à la création forcerait à inventer des
     * valeurs factices, qui pollueraient la contrainte d'unicité.</p>
     */
    @Column(name = TableNames.COL_BIOMETRIC_ID, length = 100)
    private String biometricId;

    /** Indique si l'étudiant dispose d'une empreinte enrôlée et peut donc être identifié. */
    public boolean isEnrole() {
        return biometricId != null && !biometricId.isBlank();
    }

    /**
     * Classe à laquelle appartient l'étudiant.
     *
     * <p><b>N-1 (face propriétaire de la relation bidirectionnelle avec {@link Classe}),
     * LAZY</b>. <b>Aucune cascade</b>.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classe_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_etudiants_classe"))
    private Classe classe;

    /**
     * Compte de connexion de l'étudiant, pour l'application mobile.
     *
     * <h2>Relation 1-1 vers Utilisateur</h2>
     * <p><b>Unidirectionnelle</b>, {@code Etudiant} propriétaire : la clé étrangère
     * {@code utilisateur_id} (unique) vit dans la table {@code etudiants}, comme pour
     * {@link Enseignant} et {@link Personnel}. {@code Utilisateur} ne référence rien en
     * retour, ce qui garde l'authentification indépendante du métier.</p>
     *
     * <p><b>Nullable</b> : un étudiant existe d'abord dans le référentiel, et reçoit un
     * accès dans un second temps. Beaucoup n'en auront jamais — leur présence est
     * relevée par le capteur, pas par l'application. Un compte n'est utile qu'à celui
     * qui veut consulter ses propres relevés.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", unique = true,
            foreignKey = @ForeignKey(name = "fk_etudiants_utilisateur"))
    private Utilisateur utilisateur;

    /**
     * Indique si l'étudiant est actif.
     */
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /**
     * Indique si l'étudiant dispose d'un accès à l'application mobile.
     *
     * <p>Nommé {@code compteOuvert} et non {@code aUnCompte} : un getter
     * {@code isAUnCompte()} produit, selon les conventions JavaBeans, la propriété
     * {@code AUnCompte} — deux majuscules initiales suspendent la décapitalisation.
     * La clé JSON exposée aurait hérité de cette majuscule parasite.</p>
     */
    public boolean isCompteOuvert() {
        return utilisateur != null;
    }

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
