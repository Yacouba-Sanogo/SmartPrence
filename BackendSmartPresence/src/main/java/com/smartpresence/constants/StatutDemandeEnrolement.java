package com.smartpresence.constants;

/**
 * Cycle de vie d'une demande d'enrôlement biométrique.
 *
 * <p>Une demande naît dans l'application mobile, vit quelques minutes, et meurt de
 * l'une de quatre façons : servie par un lecteur, abandonnée par l'étudiant,
 * périmée faute de passage devant le capteur, ou — jamais — oubliée.</p>
 *
 * @since 0.0.1
 */
public enum StatutDemandeEnrolement {

    /** Demande ouverte : l'étudiant est attendu devant un lecteur. */
    EN_ATTENTE,

    /** Empreinte capturée et référence associée à l'étudiant. */
    TERMINEE,

    /** Abandonnée par l'étudiant depuis l'application. */
    ANNULEE,

    /** Délai écoulé sans passage devant un lecteur. */
    EXPIREE
}
