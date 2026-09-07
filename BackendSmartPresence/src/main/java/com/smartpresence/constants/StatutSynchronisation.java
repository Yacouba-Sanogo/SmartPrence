package com.smartpresence.constants;

/**
 * Résultat d'une opération de synchronisation entre un appareil ESP32 et le backend.
 *
 * <p>Journalisé dans {@code HistoriqueSynchronisation} à des fins d'audit et d'évaluation
 * scientifique (fiabilité réseau, performances de synchronisation), cf.
 * {@code SmartPresence_CONTEXT.md} §4.9.</p>
 *
 * @since 0.0.1
 */
public enum StatutSynchronisation {

    /** Synchronisation entièrement réussie : tous les événements ont été traités. */
    SUCCES,

    /** Synchronisation échouée : aucun événement n'a pu être traité. */
    ECHEC,

    /** Synchronisation partielle : une partie seulement des événements a été traitée. */
    PARTIEL

}
