/**
 * SmartPresence — lecteur biométrique ESP32.
 *
 * <h2>Ce que fait cet appareil</h2>
 * <p>Il identifie un doigt sur le capteur AS608, horodate le passage avec son horloge
 * DS3231, l'écrit en mémoire non volatile, puis tente de le transmettre au serveur.</p>
 *
 * <h2>Ce qu'il ne fait pas</h2>
 * <ul>
 *   <li><b>Il ne transmet aucune donnée biométrique.</b> Le gabarit reste dans la mémoire
 *       du capteur ; seul un numéro d'emplacement en sort, transformé en référence
 *       logique. Cette référence est inutilisable sans la table d'enrôlement du serveur,
 *       et ne permet pas de reconstituer une empreinte.</li>
 *   <li><b>Il ne connaît pas l'emploi du temps.</b> Rattacher un passage à une séance
 *       demande de savoir quel cours a lieu, où et pour quelle classe : c'est au serveur
 *       de le faire.</li>
 *   <li><b>Il ne décide pas d'un retard.</b> Sans l'heure de début du cours, il ne
 *       pourrait le faire qu'au jugé.</li>
 * </ul>
 *
 * <h2>Coupure réseau</h2>
 * <p>Le relevé est écrit en NVS <b>avant</b> toute tentative d'envoi, et n'en est retiré
 * qu'une fois le serveur confirmé. Coupure Wi-Fi, redémarrage, panne de courant : le
 * passage survit. Un lot rejoué ne crée pas de doublon, le serveur les écartant sur la
 * clé (appareil, personne, date, heure).</p>
 */

#include <Arduino.h>

#include "api.h"
#include "config.h"
#include "empreinte.h"
#include "file_attente.h"
#include "horloge.h"
#include "ihm.h"

namespace {

uint32_t prochainEnvoi = 0;
uint32_t prochainRafraichissement = 0;
uint32_t derniereReconnexion = 0;
int tentatives = 0;

/// Dernier emplacement identifié, pour ignorer un doigt resté posé.
uint16_t dernierSlot = 0;
uint32_t dernierPassage = 0;

/// Deux passages du même doigt rapprochés sont un rebond, pas deux présences.
constexpr uint32_t DELAI_ANTI_REBOND_MS = 5000;

void afficherVeille() {
  ihm::veille(horloge::pourAffichage(horloge::maintenantUtc()), api::wifiConnecte(),
              file_attente::taille());
}

/// Enregistre le passage, puis tente de vider la file dans la foulée.
void releverPassage(const empreinte::Identification& lecture) {
  const uint32_t maintenant = horloge::maintenantUtc();

  file_attente::Evenement evenement;
  evenement.reference = empreinte::referenceDe(lecture.slot);
  evenement.date = horloge::dateLocale(maintenant);
  evenement.heure = horloge::heureLocale(maintenant);
  evenement.creeLe = horloge::isoUtc(maintenant);

  if (!file_attente::empiler(evenement)) {
    ihm::message(F("File pleine"), F("Prevenez l'administration."), true);
    return;
  }

  Serial.printf("[passage] %s a %s\n", evenement.reference.c_str(),
                evenement.heure.c_str());
  ihm::succes(evenement.reference, lecture.score);

  // Envoi immédiat : l'utilisateur est encore devant l'appareil, et voir la file
  // se vider tout de suite rassure sur le bon fonctionnement.
  prochainEnvoi = 0;
}

/// Transmet un lot si la file n'est pas vide.
void tenterEnvoi() {
  if (file_attente::estVide()) {
    tentatives = 0;
    return;
  }
  if (!api::wifiConnecte()) return;

  file_attente::Evenement lot[TAILLE_LOT];
  const size_t nombre = file_attente::lirePremiers(lot, TAILLE_LOT);
  if (nombre == 0) return;

  tentatives++;
  const api::Resultat issue = api::envoyer(lot, nombre, tentatives);

  if (issue.succes) {
    // Les doublons comptent comme acquittés : le serveur les connaît déjà, les
    // garder en file les ferait renvoyer indéfiniment.
    file_attente::acquitter(nombre);
    tentatives = 0;
  }
}

/// Mode enrôlement, déclenché par un appui long sur le bouton.
void enroler() {
  const uint16_t slot = empreinte::premierSlotLibre();
  if (slot == 0) {
    ihm::message(F("Memoire pleine"), F("Capteur sature."), true);
    return;
  }

  ihm::etapeEnrolement(F("Preparation..."), slot);

  const bool reussi = empreinte::enroler(slot, [](const String& etape) {
    ihm::etapeEnrolement(etape, empreinte::premierSlotLibre());
  });

  if (!reussi) {
    ihm::message(F("Enrolement echoue"), F("Recommencez."), true);
    return;
  }

  // La référence est affichée longuement : l'opérateur doit la recopier dans
  // l'administration web pour l'associer à une personne. Tant que ce n'est pas
  // fait, le capteur reconnaît le doigt mais le serveur ignore à qui il appartient.
  ihm::referenceEnrolee(empreinte::referenceDe(slot));
}

/// Détecte un appui long sur le bouton d'enrôlement.
bool appuiLong() {
  if (digitalRead(BROCHE_BOUTON) == HIGH) return false;

  const uint32_t debut = millis();
  while (digitalRead(BROCHE_BOUTON) == LOW) {
    if (millis() - debut >= DUREE_APPUI_LONG_MS) return true;
    delay(20);
  }
  return false;
}

}  // namespace

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println(F("\n=== SmartPresence — lecteur ESP32 ==="));

  pinMode(BROCHE_BOUTON, INPUT_PULLUP);

  ihm::demarrer();
  delay(1200);

  if (!horloge::demarrer()) {
    ihm::message(F("Horloge absente"), F("Verifiez le DS3231."), true);
  } else if (horloge::heurePerdue()) {
    ihm::message(F("Heure perdue"), F("Pile DS3231 a changer."), true);
  }

  if (!empreinte::demarrer()) {
    ihm::message(F("Capteur absent"), F("Verifiez TX/RX et 3,3V."), true);
  }

  file_attente::demarrer();

  if (api::connecterWifi()) {
    horloge::recalerSurNtp();
  }

  Serial.printf("[demarrage] %u empreinte(s) au capteur, %u en file\n",
                empreinte::nombreEnregistrees(),
                static_cast<unsigned>(file_attente::taille()));
  afficherVeille();
}

void loop() {
  const uint32_t maintenant = millis();

  if (appuiLong()) {
    enroler();
    afficherVeille();
    return;
  }

  if (empreinte::doigtPose()) {
    const empreinte::Identification lecture = empreinte::identifier();

    switch (lecture.resultat) {
      case empreinte::Resultat::Identifie: {
        const bool rebond = lecture.slot == dernierSlot &&
                            maintenant - dernierPassage < DELAI_ANTI_REBOND_MS;
        if (!rebond) {
          dernierSlot = lecture.slot;
          dernierPassage = maintenant;
          releverPassage(lecture);
        }
        break;
      }
      case empreinte::Resultat::NonReconnu:
        ihm::nonReconnu();
        break;
      case empreinte::Resultat::LectureRatee:
        ihm::message(F("Lecture ratee"), F("Reposez le doigt bien a plat."));
        break;
      default:
        break;
    }
    afficherVeille();
  }

  if (maintenant >= prochainEnvoi) {
    tenterEnvoi();
    prochainEnvoi = maintenant + PERIODE_ENVOI_MS;
  }

  // Reconnexion espacée : insister toutes les secondes bloquerait la lecture
  // du capteur pendant que la pile Wi-Fi tente de s'associer.
  if (!api::wifiConnecte() && maintenant - derniereReconnexion > 30000) {
    derniereReconnexion = maintenant;
    if (api::connecterWifi()) {
      horloge::recalerSurNtp();
    }
  }

  if (maintenant >= prochainRafraichissement) {
    afficherVeille();
    prochainRafraichissement = maintenant + 10000;
  }

  delay(40);
}
