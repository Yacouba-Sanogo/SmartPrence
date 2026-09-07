#pragma once

#include <Arduino.h>

/**
 * Retour a l'utilisateur : afficheur, diodes et buzzer.
 *
 * <p>Le lecteur est utilise debout, dans un couloir, par quelqu'un qui ne s'arrete pas.
 * Le retour doit donc etre lisible en une fraction de seconde et audible sans regarder :
 * d'ou un signal sonore distinct par issue, et une diode qui tranche a distance.</p>
 */
namespace ihm {

/// Prepare l'afficheur et les broches de signalisation.
bool demarrer();

/// Ecran d'attente : etablissement, heure, etat reseau, file en attente.
void veille(const String& heure, bool enLigne, size_t enAttente);

/// Passage accepte.
void succes(const String& reference, uint16_t score);

/// Doigt lu mais inconnu du capteur.
void nonReconnu();

/// Message d'erreur ou d'information, affiche quelques secondes.
void message(const String& titre, const String& detail = "", bool erreur = false);

/// Progression d'un enrolement.
void etapeEnrolement(const String& etape, uint16_t slot);

/// Reference produite a l'issue d'un enrolement, a recopier dans l'administration.
void referenceEnrolee(const String& reference);

}  // namespace ihm
