package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

/**
 * Identité complète de l'utilisateur connecté.
 *
 * <p>Le compte de connexion ne dit pas <b>qui</b> est la personne dans le référentiel :
 * il faut résoudre le profil métier associé — étudiant, enseignant ou agent. C'est cette
 * résolution que l'application mobile appelle au démarrage pour savoir quels écrans
 * ouvrir et quelles données demander.</p>
 *
 * <p>Les trois profils sont mutuellement exclusifs en pratique, mais rien n'interdit
 * qu'aucun ne soit renseigné : un administrateur n'est rattaché à aucun référentiel.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilResponse {

    private UUID utilisateurId;
    private String email;
    private String nom;
    private String prenom;
    private Set<String> roles;

    /** Profil métier résolu : {@code ETUDIANT}, {@code ENSEIGNANT}, {@code PERSONNEL} ou {@code AUCUN}. */
    private String typeProfil;

    /** Identifiant de la fiche métier correspondante, {@code null} si aucun profil. */
    private UUID profilId;

    /** Matricule de la fiche métier, {@code null} si aucun profil. */
    private String matricule;

    // Contexte académique, renseigné pour un étudiant uniquement.
    private Long classeId;
    private String classeCode;
    private String classeLibelle;
    private String promotionLibelle;

    /** Service de rattachement, renseigné pour un agent uniquement. */
    private String service;
}
