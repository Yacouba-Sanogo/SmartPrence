package com.smartpresence.constants;

/**
 * Nature d'une anomalie signalée par un enseignant sur une séance.
 *
 * @since 0.0.1
 */
public enum TypeSignalement {

    /**
     * Un étudiant présent en cours n'a pas été relevé par le lecteur.
     *
     * <p>Seul type dont l'acceptation produit un relevé correctif : la scolarité, en
     * validant, crée une présence de source {@code MANUEL} rattachée à la séance.</p>
     */
    ETUDIANT_NON_RECONNU,

    /**
     * Le lecteur de la salle n'a pas fonctionné pendant la séance.
     *
     * <p>Concerne le groupe entier, pas un étudiant en particulier : l'arbitrage relève
     * de la scolarité, qui décidera d'annuler le relevé de la séance ou de régulariser
     * les étudiants un à un.</p>
     */
    LECTEUR_DEFAILLANT,

    /** Toute autre situation, décrite en clair par l'enseignant. */
    AUTRE
}
