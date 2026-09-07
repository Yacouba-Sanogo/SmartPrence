#include "file_attente.h"

#include <Preferences.h>

#include "config.h"

namespace {

Preferences nvs;

/// Index du plus ancien événement, et du prochain emplacement libre.
uint32_t tete = 0;
uint32_t queue = 0;

constexpr const char* ESPACE = "sp_file";
constexpr const char* CLE_TETE = "tete";
constexpr const char* CLE_QUEUE = "queue";

/// `e042` — les clés NVS sont limitées à quinze caractères.
String cleDe(uint32_t index) {
  char tampon[8];
  snprintf(tampon, sizeof(tampon), "e%03u",
           static_cast<unsigned>(index % CAPACITE_FILE));
  return String(tampon);
}

/**
 * Sérialise un événement en une seule chaîne.
 *
 * Séparateur `|` : aucun des champs n'en contient — références, dates et heures sont
 * alphanumériques. Un JSON par entrée coûterait trois fois la place pour rien.
 */
String encoder(const file_attente::Evenement& e) {
  return e.reference + "|" + e.date + "|" + e.heure + "|" + e.creeLe;
}

bool decoder(const String& brut, file_attente::Evenement& e) {
  const int p1 = brut.indexOf('|');
  const int p2 = brut.indexOf('|', p1 + 1);
  const int p3 = brut.indexOf('|', p2 + 1);
  if (p1 < 0 || p2 < 0 || p3 < 0) return false;

  e.reference = brut.substring(0, p1);
  e.date = brut.substring(p1 + 1, p2);
  e.heure = brut.substring(p2 + 1, p3);
  e.creeLe = brut.substring(p3 + 1);
  return true;
}

void enregistrerIndex() {
  nvs.putUInt(CLE_TETE, tete);
  nvs.putUInt(CLE_QUEUE, queue);
}

}  // namespace

namespace file_attente {

bool demarrer() {
  if (!nvs.begin(ESPACE, false)) {
    Serial.println(F("[file] espace NVS inaccessible"));
    return false;
  }
  tete = nvs.getUInt(CLE_TETE, 0);
  queue = nvs.getUInt(CLE_QUEUE, 0);
  Serial.printf("[file] %u evenement(s) en attente au demarrage\n",
                static_cast<unsigned>(taille()));
  return true;
}

bool empiler(const Evenement& evenement) {
  if (taille() >= CAPACITE_FILE) {
    // Écraser le plus ancien perdrait un passage réel sans que personne ne le sache.
    // Mieux vaut refuser et le signaler à l'écran.
    Serial.println(F("[file] pleine : evenement refuse"));
    return false;
  }

  nvs.putString(cleDe(queue).c_str(), encoder(evenement));
  queue++;
  enregistrerIndex();
  return true;
}

size_t taille() {
  return static_cast<size_t>(queue - tete);
}

bool estVide() {
  return queue == tete;
}

size_t lirePremiers(Evenement* destination, size_t maximum) {
  size_t lus = 0;
  for (uint32_t index = tete; index < queue && lus < maximum; index++) {
    const String brut = nvs.getString(cleDe(index).c_str(), "");
    if (brut.isEmpty()) continue;
    if (decoder(brut, destination[lus])) lus++;
  }
  return lus;
}

void acquitter(size_t nombre) {
  for (size_t i = 0; i < nombre && tete < queue; i++) {
    nvs.remove(cleDe(tete).c_str());
    tete++;
  }

  // Les index sont ramenés à zéro quand la file se vide : sans cela, ils
  // croîtraient indéfiniment et finiraient par déborder.
  if (tete == queue) {
    tete = 0;
    queue = 0;
  }
  enregistrerIndex();
}

void vider() {
  while (tete < queue) {
    nvs.remove(cleDe(tete).c_str());
    tete++;
  }
  tete = 0;
  queue = 0;
  enregistrerIndex();
}

}  // namespace file_attente
