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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Numéro CENOU autorisé à s'inscrire.
 *
 * <p>L'inscription des étudiants est libre — c'est la seule façon de ne pas faire
 * reposer la saisie de toute une promotion sur l'administration. Elle ne peut pas
 * pour autant être ouverte à n'importe qui : ce référentiel est la liste blanche
 * qui l'encadre. Sans numéro connu ici, aucune inscription n'aboutit.</p>
 *
 * <p>Le numéro CENOU <b>devient le matricule</b> de l'étudiant créé : les deux
 * identifiants n'en font qu'un, ce qui évite d'avoir à les rapprocher plus tard.</p>
 *
 * <p>La liste est alimentée à la main ou par import d'un tableur, et chaque numéro
 * ne sert qu'une fois : {@link #utilise} passe à {@code true} à l'inscription et
 * ferme définitivement la porte derrière elle.</p>
 */
@Entity
@Table(
        name = TableNames.NUMEROS_CENOU,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_numeros_cenou_numero",
                columnNames = TableNames.COL_NUMERO
        ),
        indexes = @Index(name = "idx_numeros_cenou_numero", columnList = TableNames.COL_NUMERO)
)
@Getter
@Setter
@ToString(exclude = "classe")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class NumeroCenou extends BaseAuditableEntity {

    /** Identifiant technique auto-généré. */
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Le numéro lui-même, tel que délivré par le CENOU. Unique. */
    @Column(name = TableNames.COL_NUMERO, nullable = false, length = 50)
    private String numero;

    /**
     * Nom et prénom attendus, lorsque le tableau importé les fournit.
     *
     * <p>Facultatifs : le numéro seul suffit à autoriser l'inscription. Renseignés,
     * ils permettent à l'interface de pré-remplir le formulaire et de repérer une
     * saisie manifestement étrangère au numéro.</p>
     */
    @Column(name = "nom", length = 100)
    private String nom;

    @Column(name = "prenom", length = 100)
    private String prenom;

    /**
     * Classe imposée à l'inscription, lorsqu'elle est connue d'avance.
     *
     * <p>Laissée vide, l'étudiant choisit sa classe lui-même au moment de
     * s'inscrire. Renseignée, elle prime sur ce choix : c'est l'administration qui
     * décide de l'affectation, pas l'intéressé.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_id",
            foreignKey = @ForeignKey(name = "fk_numeros_cenou_classe"))
    private Classe classe;

    /** Vrai dès qu'une inscription a consommé ce numéro. */
    @Column(name = "utilise", nullable = false)
    private boolean utilise = false;

    /** Instant de l'inscription qui a consommé le numéro. */
    @Column(name = "date_utilisation")
    private Instant dateUtilisation;
}
