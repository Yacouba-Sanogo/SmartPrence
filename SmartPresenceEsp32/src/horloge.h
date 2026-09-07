#pragma once

#include <Arduino.h>

/**
 * Horloge du lecteur.
 *
 * Le DS3231 conserve l'heure **UTC** sur sa pile, y compris hors tension. C'est ce qui
 * permet d'horodater un relevé pendant une coupure réseau : sans lui, l'ESP32 repartirait
 * de 1970 à chaque redémarrage et tous les événements accumulés seraient inexploitables.
 *
 * Le NTP ne sert qu'à recaler la dérive, quand le réseau est disponible.
 */
namespace horloge {

/// Initialise le bus I2C et le DS3231. Retourne false si le module ne répond pas.
bool demarrer();

/// `true` si l'horloge a perdu l'heure (pile absente ou vide).
bool heurePerdue();

/**
 * Recale le DS3231 sur le NTP si l'écart dépasse la tolérance.
 *
 * @return true si l'heure a été corrigée
 */
bool recalerSurNtp();

/// Instant courant en secondes depuis l'époque Unix, UTC.
uint32_t maintenantUtc();

/// `2026-08-26` — date **locale**, telle que l'attend le serveur.
String dateLocale(uint32_t instantUtc);

/// `08:05:00` — heure **locale**.
String heureLocale(uint32_t instantUtc);

/// `2026-08-26T08:05:00Z` — instant UTC au format ISO 8601, pour `createdAt`.
String isoUtc(uint32_t instantUtc);

/// `26/08 08:05` — forme courte pour l'afficheur.
String pourAffichage(uint32_t instantUtc);

}  // namespace horloge
