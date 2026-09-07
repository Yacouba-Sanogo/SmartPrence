package com.smartpresence.constants;

/**
 * Vocation fonctionnelle d'un appareil ESP32 déployé.
 *
 * <p>Le parc d'appareils n'est pas homogène : un lecteur posté à l'entrée du bâtiment
 * sert au pointage du <b>personnel</b>, tandis que les lecteurs installés en salle
 * relèvent la présence des <b>étudiants</b>. Cette distinction est portée par le modèle
 * afin que la couche métier puisse <b>rejeter</b> un appareil sollicitant un endpoint
 * qui ne relève pas de sa vocation.</p>
 *
 * <p>Un appareil dont l'usage n'est pas renseigné est traité comme {@link #MIXTE}
 * (compatibilité ascendante avec le parc déjà enregistré).</p>
 *
 * @since 0.0.1
 */
public enum UsageDevice {

    /** Lecteur de salle : relève uniquement les présences académiques des étudiants. */
    ETUDIANT,

    /** Lecteur d'entrée : relève uniquement les pointages d'arrivée/départ du personnel. */
    PERSONNEL,

    /** Lecteur polyvalent : accepte les deux flux (étudiants et personnel). */
    MIXTE

}
