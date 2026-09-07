#include "ihm.h"

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <Wire.h>

#include "config.h"

namespace {

Adafruit_SSD1306 ecran(LARGEUR_OLED, HAUTEUR_OLED, &Wire, -1);
bool ecranPresent = false;

/// Émet une note. Le buzzer est passif : il faut lui fournir une fréquence.
void note(uint16_t frequence, uint16_t dureeMs) {
  tone(BROCHE_BUZZER, frequence, dureeMs);
  delay(dureeMs);
  noTone(BROCHE_BUZZER);
}

void diodes(bool verte, bool rouge) {
  digitalWrite(BROCHE_LED_VERTE, verte ? HIGH : LOW);
  digitalWrite(BROCHE_LED_ROUGE, rouge ? HIGH : LOW);
}

/// Cadre commun : bandeau titre en inverse vidéo, puis le corps.
void cadre(const String& titre) {
  if (!ecranPresent) return;
  ecran.clearDisplay();
  ecran.setTextColor(SSD1306_BLACK, SSD1306_WHITE);
  ecran.setTextSize(1);
  ecran.setCursor(0, 0);
  ecran.print(' ');
  ecran.print(titre);
  for (int i = titre.length() + 1; i < 21; i++) ecran.print(' ');
  ecran.setTextColor(SSD1306_WHITE);
}

}  // namespace

namespace ihm {

bool demarrer() {
  pinMode(BROCHE_LED_VERTE, OUTPUT);
  pinMode(BROCHE_LED_ROUGE, OUTPUT);
  pinMode(BROCHE_BUZZER, OUTPUT);
  diodes(false, false);

  ecranPresent = ecran.begin(SSD1306_SWITCHCAPVCC, ADRESSE_OLED);
  if (!ecranPresent) {
    // L'afficheur n'est pas indispensable : diodes et buzzer suffisent à un
    // pointage. On le signale sans bloquer le démarrage.
    Serial.println(F("[ihm] SSD1306 introuvable, on continue sans afficheur"));
    return false;
  }

  ecran.clearDisplay();
  ecran.setTextColor(SSD1306_WHITE);
  ecran.setTextSize(1);
  ecran.setCursor(0, 24);
  ecran.println(F("   SmartPresence"));
  ecran.println(F("       ENETP"));
  ecran.display();
  return true;
}

void veille(const String& heure, bool enLigne, size_t enAttente) {
  diodes(false, false);
  if (!ecranPresent) return;

  cadre(F("SmartPresence"));

  ecran.setTextSize(2);
  ecran.setCursor(6, 20);
  ecran.print(heure);

  ecran.setTextSize(1);
  ecran.setCursor(0, 44);
  ecran.print(F("Posez votre doigt"));

  ecran.setCursor(0, 56);
  ecran.print(enLigne ? F("En ligne") : F("Hors ligne"));
  if (enAttente > 0) {
    ecran.setCursor(72, 56);
    ecran.printf("file %u", static_cast<unsigned>(enAttente));
  }
  ecran.display();
}

void succes(const String& reference, uint16_t score) {
  diodes(true, false);
  // Deux notes montantes : reconnaissables sans regarder l'écran.
  note(1400, 90);
  note(1900, 110);

  if (ecranPresent) {
    cadre(F("Presence relevee"));
    ecran.setTextSize(1);
    ecran.setCursor(0, 20);
    ecran.print(F("Reference :"));
    ecran.setCursor(0, 32);
    ecran.print(reference);
    ecran.setCursor(0, 48);
    ecran.printf("Fiabilite %u", score);
    ecran.display();
  }

  delay(1400);
  diodes(false, false);
}

void nonReconnu() {
  diodes(false, true);
  // Une note grave et longue : l'échec ne doit pas sonner comme la réussite.
  note(320, 400);

  if (ecranPresent) {
    cadre(F("Non reconnu"));
    ecran.setTextSize(1);
    ecran.setCursor(0, 22);
    ecran.println(F("Doigt inconnu."));
    ecran.println(F("Reessayez, ou"));
    ecran.println(F("voyez la scolarite."));
    ecran.display();
  }

  delay(1600);
  diodes(false, false);
}

void message(const String& titre, const String& detail, bool erreur) {
  diodes(false, erreur);
  if (erreur) note(320, 250);

  if (ecranPresent) {
    cadre(titre);
    ecran.setTextSize(1);
    ecran.setCursor(0, 24);
    ecran.println(detail);
    ecran.display();
  }

  delay(1500);
  diodes(false, false);
}

void etapeEnrolement(const String& etape, uint16_t slot) {
  note(1100, 70);
  if (!ecranPresent) return;

  cadre(F("Enrolement"));
  ecran.setTextSize(1);
  ecran.setCursor(0, 22);
  ecran.printf("Emplacement %u", slot);
  ecran.setCursor(0, 40);
  ecran.setTextSize(1);
  ecran.println(etape);
  ecran.display();
}

void referenceEnrolee(const String& reference) {
  diodes(true, false);
  note(1400, 90);
  note(1900, 90);
  note(2300, 140);

  if (ecranPresent) {
    cadre(F("Enrolement fini"));
    ecran.setTextSize(1);
    ecran.setCursor(0, 20);
    ecran.println(F("Reference a saisir"));
    ecran.println(F("dans l'administration :"));
    ecran.setCursor(0, 46);
    ecran.setTextSize(1);
    ecran.print(reference);
    ecran.display();
  }

  // Long : l'opérateur doit avoir le temps de recopier la référence.
  delay(9000);
  diodes(false, false);
}

}  // namespace ihm
