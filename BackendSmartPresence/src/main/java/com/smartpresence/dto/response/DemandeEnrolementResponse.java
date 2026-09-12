package com.smartpresence.dto.response;

import com.smartpresence.constants.StatutDemandeEnrolement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * État de la demande d'enrôlement, tel que l'application le montre à l'étudiant.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeEnrolementResponse {

    /**
     * Code à retrouver sur l'écran du lecteur avant de poser le doigt.
     *
     * <p>Absent une fois la demande close : il n'a plus rien à confirmer.</p>
     */
    private String code;

    private StatutDemandeEnrolement statut;

    /** Instant au-delà duquel il faudra redemander. */
    private Instant expireLe;

    /** Secondes restantes, pour afficher un compte à rebours sans horloge partagée. */
    private long secondesRestantes;

    /** Nom du lecteur qui a pris la demande en charge, lorsqu'il y en a un. */
    private String lecteur;

    /** Vrai lorsque l'étudiant est désormais reconnu par le capteur. */
    private boolean enrole;
}
