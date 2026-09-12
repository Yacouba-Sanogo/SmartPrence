package com.smartpresence.constants;

/**
 * Verdict d'un semestre, tel qu'il figure en tête du relevé.
 *
 * @since 0.0.1
 */
public enum DecisionSemestre {

    /** Moyenne générale à la barre : le semestre est acquis, crédits compris. */
    VALIDE,

    /** Sous la barre : seules les UE acquises apportent leurs crédits. */
    NON_VALIDE,

    /**
     * Aucune note saisie : rien à décider.
     *
     * <p>Distingué de {@link #NON_VALIDE} à dessein — annoncer un échec à un étudiant
     * dont les enseignants n'ont simplement pas encore saisi les notes serait faux, et
     * il n'y a pas de cas où l'inverse se justifie.</p>
     */
    EN_ATTENTE
}
