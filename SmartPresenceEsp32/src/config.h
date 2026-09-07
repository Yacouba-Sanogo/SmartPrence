#pragma once

#include <Arduino.h>

/**
 * Brochage et constantes du lecteur SmartPresence.
 *
 * Ce fichier est la référence unique du câblage : le guide de branchement en découle,
 * et toute modification de broche doit s'y faire ici, pas dans le code applicatif.
 */

// ---------------------------------------------------------------------------
// Capteur d'empreintes AS608 — liaison série
// ---------------------------------------------------------------------------
//
// UART2 et non UART0 : UART0 porte la console USB, et un capteur branché dessus
// rendrait le téléversement et le moniteur série inutilisables.
//
// Croisement obligatoire : la sortie du capteur entre dans l'ESP32.
//   AS608 TX  ->  GPIO 16 (RX2 de l'ESP32)
//   AS608 RX  <-  GPIO 17 (TX2 de l'ESP32)
static constexpr int BROCHE_EMPREINTE_RX = 16;
static constexpr int BROCHE_EMPREINTE_TX = 17;

/// Débit d'usine de l'AS608. Ne le changer que si le module a été reconfiguré.
static constexpr uint32_t DEBIT_EMPREINTE = 57600;

/**
 * Broche de détection de doigt du module (marquée WAKEUP, TCH ou IRQ).
 *
 * Elle passe à l'état haut dès qu'un doigt touche la vitre. La lire évite
 * d'interroger le capteur en boucle : sans elle, la liaison série est sollicitée
 * plusieurs fois par seconde pour rien, ce qui chauffe le module et sature les traces.
 *
 * Les modules AS608 dépourvus de cette broche restent utilisables : voir
 * DETECTION_PAR_BROCHE ci-dessous.
 */
static constexpr int BROCHE_EMPREINTE_TOUCHE = 4;

/// Mettre à false pour un module sans broche de détection : le capteur sera scruté.
static constexpr bool DETECTION_PAR_BROCHE = true;

// ---------------------------------------------------------------------------
// Bus I2C — horloge DS3231 et afficheur SSD1306
// ---------------------------------------------------------------------------
//
// Les deux composants partagent le même bus : ils ont des adresses distinctes
// (0x68 pour le DS3231, 0x3C pour l'afficheur) et ne se gênent donc pas.
static constexpr int BROCHE_I2C_SDA = 21;
static constexpr int BROCHE_I2C_SCL = 22;

static constexpr uint8_t ADRESSE_OLED = 0x3C;
static constexpr int LARGEUR_OLED = 128;
static constexpr int HAUTEUR_OLED = 64;

// ---------------------------------------------------------------------------
// Signalisation
// ---------------------------------------------------------------------------
//
// Les broches 25 à 27 et 32 à 33 sont libres de toute fonction au démarrage.
// Sont évitées : 0, 2, 12 et 15 (broches de configuration lues au boot, un niveau
// imposé y empêche le démarrage), 6 à 11 (réservées à la flash) et 34 à 39
// (entrées seules, sans étage de sortie).
static constexpr int BROCHE_LED_VERTE = 25;
static constexpr int BROCHE_LED_ROUGE = 26;
static constexpr int BROCHE_BUZZER = 27;

/// Bouton d'enrôlement, câblé entre la broche et la masse (résistance interne activée).
static constexpr int BROCHE_BOUTON = 32;

/// Appui long qui bascule en mode enrôlement.
static constexpr uint32_t DUREE_APPUI_LONG_MS = 1500;

// ---------------------------------------------------------------------------
// Horaire
// ---------------------------------------------------------------------------
//
// Le DS3231 conserve l'heure **UTC**. L'heure locale s'en déduit par ce décalage,
// ce qui évite d'avoir à reprogrammer l'horloge à chaque changement de fuseau.
// Le Mali est à UTC+0 ; ajuster pour un autre pays.
static constexpr int32_t DECALAGE_UTC_MINUTES = 0;

/// Serveur d'horloge interrogé à chaque connexion pour recaler le DS3231.
static constexpr const char* SERVEUR_NTP = "pool.ntp.org";

/// Écart au-delà duquel le DS3231 est recalé sur le NTP.
static constexpr int32_t DERIVE_TOLEREE_SECONDES = 30;

// ---------------------------------------------------------------------------
// File d'attente hors ligne
// ---------------------------------------------------------------------------
//
// Les relevés sont conservés en NVS tant qu'ils n'ont pas été acquittés par le
// serveur. La capacité est bornée par l'espace NVS disponible : au-delà, le plus
// ancien serait écrasé sans être transmis.
static constexpr size_t CAPACITE_FILE = 200;

/// Intervalle entre deux tentatives d'envoi quand la file n'est pas vide.
static constexpr uint32_t PERIODE_ENVOI_MS = 15000;

/// Nombre d'événements transmis en un seul lot.
static constexpr size_t TAILLE_LOT = 20;

// ---------------------------------------------------------------------------
// Réseau
// ---------------------------------------------------------------------------
static constexpr uint32_t DELAI_CONNEXION_WIFI_MS = 20000;
static constexpr uint32_t DELAI_REQUETE_HTTP_MS = 10000;

// ---------------------------------------------------------------------------
// Usage du lecteur
// ---------------------------------------------------------------------------

/**
 * Public relevé par cet appareil.
 *
 * Le serveur refuse un lot qui ne correspond pas à l'usage déclaré du device :
 * un lecteur d'entrée dédié aux agents n'alimente pas le flux académique.
 */
enum class UsageLecteur {
  Etudiants,  ///< POST /esp32/presences/synchronize
  Personnels  ///< POST /esp32/personnels/pointages
};

static constexpr UsageLecteur USAGE = UsageLecteur::Etudiants;

// ---------------------------------------------------------------------------
// Identifiants — fournis par identifiants.h, hors dépôt
// ---------------------------------------------------------------------------
#include "identifiants.h"
