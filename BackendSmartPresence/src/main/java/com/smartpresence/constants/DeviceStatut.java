package com.smartpresence.constants;

/**
 * État opérationnel d'un appareil ESP32.
 *
 * <p>Permet le suivi et la supervision des équipements IoT déployés
 * (cf. {@code SmartPresence_CONTEXT.md} §4.7).</p>
 *
 * @since 0.0.1
 */
public enum DeviceStatut {

    /** Appareil actif et opérationnel. */
    ACTIF,

    /** Appareil désactivé administrativement (ne doit plus accepter de requêtes). */
    INACTIF,

    /** Appareil en mode hors ligne (perte de connectivité détectée). */
    HORS_LIGNE,

    /** Appareil en panne (défaillance matérielle ou logicielle signalée). */
    EN_PANNE

}
