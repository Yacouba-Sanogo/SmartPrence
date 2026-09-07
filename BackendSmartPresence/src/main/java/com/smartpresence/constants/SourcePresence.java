package com.smartpresence.constants;

/**
 * Origine d'un événement de présence.
 *
 * <p>Permet de différencier :</p>
 * <ul>
 *   <li>{@link #ESP32} : présence capturée automatiquement par le dispositif embarqué
 *       (capteur AS608). Dans ce cas, l'association à un {@code Device} est <b>obligatoire</b>.</li>
 *   <li>{@link #MANUEL} : présence saisie ou corrigée manuellement par un administrateur
 *       (correction administrative, rattrapage). Dans ce cas, le {@code Device} est
 *       <b>optionnel</b> (peut être {@code null}).</li>
 * </ul>
 *
 * <p>Cette règle est contrôlée par la couche service et la validation métier,
 * cf. {@code SmartPresence_CONTEXT.md} §9.3.</p>
 *
 * @since 0.0.1
 */
public enum SourcePresence {

    /** Présence issue du dispositif embarqué ESP32 (identification biométrique). Device obligatoire. */
    ESP32,

    /** Présence saisie ou corrigée manuellement (administration). Device optionnel. */
    MANUEL

}
