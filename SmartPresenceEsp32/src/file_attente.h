#pragma once

#include <Arduino.h>

/**
 * File d'attente des relevés non encore acquittés.
 *
 * <p>Elle est le cœur de la tolérance aux coupures. Un relevé n'est effacé qu'après
 * confirmation du serveur : coupure Wi-Fi, redémarrage, panne de courant — l'événement
 * survit, parce qu'il est écrit en NVS avant toute tentative d'envoi.</p>
 *
 * <p>Le rejeu est sans danger : le serveur écarte les doublons sur la clé
 * {@code (appareil, personne, date, heure)}. Mieux vaut donc renvoyer deux fois que
 * risquer de perdre un passage.</p>
 *
 * <p>Implémentée en tampon circulaire sur des clés {@code e000..e199} plutôt qu'en un
 * seul bloc JSON : réécrire tout le tableau à chaque relevé userait la flash bien plus
 * vite, et une coupure en pleine écriture perdrait la file entière au lieu d'un
 * enregistrement.</p>
 */
namespace file_attente {

/// Un passage relevé par le capteur.
struct Evenement {
  String reference;   ///< Référence logique de l'empreinte
  String date;        ///< `2026-08-26`, heure locale
  String heure;       ///< `08:05:00`, heure locale
  String creeLe;      ///< `2026-08-26T08:05:00Z`, horodatage RTC en UTC
};

/// Ouvre l'espace NVS et restaure les index.
bool demarrer();

/// Ajoute un événement. Retourne false si la file est pleine.
bool empiler(const Evenement& evenement);

/// Nombre d'événements en attente.
size_t taille();

bool estVide();

/**
 * Lit jusqu'à `maximum` événements les plus anciens, sans les retirer.
 *
 * Ils ne sont supprimés qu'après acquittement, par {@link acquitter}.
 */
size_t lirePremiers(Evenement* destination, size_t maximum);

/// Retire les `nombre` événements les plus anciens, une fois le serveur confirmé.
void acquitter(size_t nombre);

/// Vide la file — réservé à la maintenance.
void vider();

}  // namespace file_attente
