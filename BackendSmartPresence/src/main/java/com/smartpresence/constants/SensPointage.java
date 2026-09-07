package com.smartpresence.constants;

/**
 * Sens d'un pointage de personnel : entrée dans l'établissement ou sortie.
 *
 * <p>Remplace l'ancien indicateur booléen {@code arrivee}, non extensible. Le sens est
 * déterminé côté backend à l'ingestion : le <b>premier</b> pointage du jour pour un
 * personnel donné vaut {@link #ENTREE}, les suivants alternent. Ce choix évite de faire
 * porter la logique métier au firmware ESP32, qui reste volontairement simple
 * (cf. {@code SmartPresence_CONTEXT.md} §8.1).</p>
 *
 * @since 0.0.1
 */
public enum SensPointage {

    /** Arrivée du personnel — première identification de la journée. */
    ENTREE,

    /** Départ du personnel — identification postérieure à une entrée. */
    SORTIE

}
