package com.smartpresence.constants;

/**
 * Avancement du traitement d'un signalement.
 *
 * <p>Le cycle est volontairement court et à sens unique : un signalement traité ne
 * revient jamais en attente. Réexaminer une décision passe par un nouveau signalement,
 * de façon à ce que l'historique conserve chaque arbitrage.</p>
 *
 * @since 0.0.1
 */
public enum StatutSignalement {

    /** Déposé par l'enseignant, en attente d'arbitrage de la scolarité. */
    EN_ATTENTE,

    /** Retenu : la correction correspondante a été appliquée. */
    ACCEPTE,

    /** Écarté par la scolarité, avec un motif consigné. */
    REJETE
}
