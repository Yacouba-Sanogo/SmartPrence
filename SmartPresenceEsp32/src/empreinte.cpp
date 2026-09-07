#include "empreinte.h"

#include <Adafruit_Fingerprint.h>

#include "config.h"

namespace {

HardwareSerial liaison(2);
Adafruit_Fingerprint capteur(&liaison);
bool disponible = false;

/// Attend qu'une image exploitable soit capturée, ou abandonne au bout du délai.
bool attendreImage(uint32_t delaiMs) {
  const uint32_t limite = millis() + delaiMs;
  while (millis() < limite) {
    const uint8_t code = capteur.getImage();
    if (code == FINGERPRINT_OK) return true;
    if (code == FINGERPRINT_NOFINGER) {
      delay(60);
      continue;
    }
    // Image floue ou doigt mal posé : on laisse une nouvelle chance dans le délai.
    delay(120);
  }
  return false;
}

/// Attend le retrait du doigt, pour ne pas enchaîner deux lectures identiques.
void attendreRetrait(uint32_t delaiMs) {
  const uint32_t limite = millis() + delaiMs;
  while (millis() < limite) {
    if (capteur.getImage() == FINGERPRINT_NOFINGER) return;
    delay(80);
  }
}

}  // namespace

namespace empreinte {

bool demarrer() {
  liaison.begin(DEBIT_EMPREINTE, SERIAL_8N1, BROCHE_EMPREINTE_RX, BROCHE_EMPREINTE_TX);
  delay(120);

  if (DETECTION_PAR_BROCHE) {
    // Résistance de tirage au bas : la broche de détection du module est active
    // à l'état haut, et flotterait sans cela.
    pinMode(BROCHE_EMPREINTE_TOUCHE, INPUT_PULLDOWN);
  }

  capteur.begin(DEBIT_EMPREINTE);
  disponible = capteur.verifyPassword();

  if (!disponible) {
    Serial.println(F("[empreinte] AS608 muet : verifiez le croisement TX/RX et "
                     "l'alimentation 3,3 V"));
    return false;
  }

  capteur.getTemplateCount();
  Serial.printf("[empreinte] AS608 pret, %u empreinte(s) en memoire\n",
                capteur.templateCount);
  return true;
}

uint16_t nombreEnregistrees() {
  if (!disponible) return 0;
  capteur.getTemplateCount();
  return capteur.templateCount;
}

bool doigtPose() {
  if (!disponible) return false;
  if (DETECTION_PAR_BROCHE) {
    return digitalRead(BROCHE_EMPREINTE_TOUCHE) == HIGH;
  }
  return capteur.getImage() == FINGERPRINT_OK;
}

Identification identifier() {
  Identification issue;
  if (!disponible) {
    issue.resultat = Resultat::Erreur;
    return issue;
  }

  if (capteur.getImage() != FINGERPRINT_OK) {
    issue.resultat = Resultat::PasDeDoigt;
    return issue;
  }

  if (capteur.image2Tz() != FINGERPRINT_OK) {
    issue.resultat = Resultat::LectureRatee;
    return issue;
  }

  if (capteur.fingerFastSearch() != FINGERPRINT_OK) {
    issue.resultat = Resultat::NonReconnu;
    return issue;
  }

  issue.resultat = Resultat::Identifie;
  issue.slot = capteur.fingerID;
  issue.score = capteur.confidence;
  return issue;
}

String referenceDe(uint16_t slot) {
  char tampon[48];
  snprintf(tampon, sizeof(tampon), "%s-%03u", PREFIXE_BIOMETRIQUE, slot);
  return String(tampon);
}

uint16_t premierSlotLibre() {
  if (!disponible) return 0;

  // L'AS608 numérote ses emplacements à partir de 1. La capacité dépend du module :
  // 127, 162 ou 300 selon les versions ; loadModel échoue au-delà, ce qui borne
  // naturellement la recherche.
  for (uint16_t slot = 1; slot <= 300; slot++) {
    if (capteur.loadModel(slot) != FINGERPRINT_OK) {
      return slot;
    }
  }
  return 0;
}

bool enroler(uint16_t slot, void (*progression)(const String&)) {
  if (!disponible || slot == 0) return false;

  progression(F("Posez le doigt"));
  if (!attendreImage(12000)) {
    progression(F("Aucune lecture"));
    return false;
  }
  if (capteur.image2Tz(1) != FINGERPRINT_OK) {
    progression(F("Image floue"));
    return false;
  }

  progression(F("Retirez le doigt"));
  attendreRetrait(6000);
  delay(400);

  progression(F("Reposez le doigt"));
  if (!attendreImage(12000)) {
    progression(F("Aucune lecture"));
    return false;
  }
  if (capteur.image2Tz(2) != FINGERPRINT_OK) {
    progression(F("Image floue"));
    return false;
  }

  // Les deux images doivent produire le même gabarit : c'est ce contrôle qui
  // empêche d'enregistrer un doigt posé de travers.
  if (capteur.createModel() != FINGERPRINT_OK) {
    progression(F("Doigts differents"));
    return false;
  }

  if (capteur.storeModel(slot) != FINGERPRINT_OK) {
    progression(F("Memoire pleine"));
    return false;
  }

  progression(F("Enregistre"));
  return true;
}

bool supprimer(uint16_t slot) {
  if (!disponible) return false;
  return capteur.deleteModel(slot) == FINGERPRINT_OK;
}

}  // namespace empreinte
