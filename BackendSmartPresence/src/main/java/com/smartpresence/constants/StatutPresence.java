package com.smartpresence.constants;

/**
 * Statut d'un événement de présence.
 *
 * <p>Utilisé par l'entité {@code Presence} pour qualifier la situation d'un étudiant
 * lors d'un relevé. Les valeurs {@code PRESENT}, {@code RETARD} et {@code ABSENT}
 * proviennent typiquement du dispositif ESP32 ; {@code JUSTIFIE} résulte d'un traitement
 * administratif (absence couverte par un justificatif).</p>
 *
 * @since 0.0.1
 */
public enum StatutPresence {

    /** Étudiant présent au moment du relevé. */
    PRESENT,

    /** Étudiant arrivé avec un retard (seuil défini par la règle métier). */
    RETARD,

    /** Étudiant absent (non identifié par le capteur). */
    ABSENT,

    /** Absence justifiée par un justificatif officiel (certificat, convocation, etc.). */
    JUSTIFIE

}
