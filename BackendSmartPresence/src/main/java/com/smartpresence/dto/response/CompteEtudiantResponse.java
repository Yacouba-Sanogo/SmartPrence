package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Accès mobile fraîchement créé pour un étudiant.
 *
 * <p>C'est la <b>seule et unique</b> occasion où le mot de passe circule en clair :
 * il est tiré au hasard, haché avant enregistrement, et le serveur ne peut plus le
 * restituer ensuite. Il revient à l'administration de le transmettre à l'étudiant.</p>
 *
 * <p>Aucun mot de passe par défaut dérivé du matricule ou de la date de naissance :
 * il serait devinable pour toute une promotion à la fois.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompteEtudiantResponse {

    private UUID etudiantId;
    private String matricule;
    private String nomComplet;

    private UUID utilisateurId;

    /** Identifiant de connexion, dérivé de l'email de l'étudiant ou de son matricule. */
    private String email;

    /** Mot de passe initial, affiché une seule fois. */
    private String motDePasseInitial;
}
