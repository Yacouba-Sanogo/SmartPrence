package com.smartpresence.dto.response;

import com.smartpresence.constants.TypePersonnel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Acces fraichement cree pour un membre du personnel.
 *
 * <p>Comme pour un etudiant, le mot de passe ne circule qu'a cette occasion : il est
 * tire au hasard, hache avant enregistrement, et le serveur ne peut plus le restituer.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComptePersonnelResponse {

    private UUID personnelId;
    private String matricule;
    private String nomComplet;
    private TypePersonnel type;

    private UUID utilisateurId;

    /** Identifiant de connexion, derive de l'email de l'agent ou de son matricule. */
    private String email;

    /** Mot de passe initial, affiche une seule fois. */
    private String motDePasseInitial;

    /** Role attribue : ENSEIGNANT pour un professeur, PERSONNEL sinon. */
    private String role;
}
