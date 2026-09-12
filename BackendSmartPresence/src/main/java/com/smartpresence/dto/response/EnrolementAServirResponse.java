package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Demande d'enrôlement remise à un lecteur, pour affichage sur son écran.
 *
 * <p>Ne contient <b>aucun identifiant interne</b> : le lecteur reçoit de quoi
 * afficher un nom et un code, rien de plus. C'est le code qu'il renverra pour
 * désigner la demande — il ignore jusqu'à l'existence des UUID du serveur
 * (cf. {@code SmartPresence_CONTEXT.md} §13).</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrolementAServirResponse {

    /** Code de rendez-vous, que l'étudiant doit reconnaître sur l'écran. */
    private String code;

    /** « KEITA Salif » — de quoi appeler l'intéressé à voix haute. */
    private String nomAffiche;

    /** Matricule, second repère visuel sur un écran de 128×64 pixels. */
    private String matricule;

    /** Secondes restantes avant péremption. */
    private long secondesRestantes;
}
