package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Étudiant tel qu'un enseignant a besoin de le connaître.
 *
 * <h2>Pourquoi une réponse distincte de {@link EtudiantResponse}</h2>
 * <p>{@code EtudiantResponse} porte l'email, le téléphone, la date de naissance et la
 * référence biométrique. Rien de tout cela n'est nécessaire pour faire cours, et
 * l'exposer élargirait la surface de fuite sans contrepartie.</p>
 *
 * <p>Le champ {@code enrole} est en revanche indispensable : sans lui, l'enseignant ne
 * peut pas distinguer un étudiant réellement absent d'un étudiant que le lecteur était
 * de toute façon incapable d'identifier.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectifEtudiantResponse {

    private UUID id;
    private String matricule;
    private String nom;
    private String prenom;

    /** {@code false} : aucun lecteur ne peut relever la présence de cet étudiant. */
    private boolean enrole;

    private boolean actif;
}
