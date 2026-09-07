#include "api.h"

#include <ArduinoJson.h>
#include <HTTPClient.h>
#include <WiFi.h>

#include "config.h"

namespace {

/// Chemin d'ingestion, selon le public relevé par cet appareil.
const char* cheminIngestion() {
  return USAGE == UsageLecteur::Etudiants ? "/api/esp32/presences/synchronize"
                                          : "/api/esp32/personnels/pointages";
}

/// Nom du tableau attendu par le serveur — il diffère d'un flux à l'autre.
const char* nomDuTableau() {
  return USAGE == UsageLecteur::Etudiants ? "presences" : "pointages";
}

}  // namespace

namespace api {

bool connecterWifi() {
  if (WiFi.status() == WL_CONNECTED) return true;

  WiFi.mode(WIFI_STA);
  // La mise en veille du Wi-Fi économise du courant mais retarde les envois de
  // plusieurs secondes : sur un lecteur alimenté au secteur, elle ne se justifie pas.
  WiFi.setSleep(false);
  WiFi.begin(WIFI_SSID, WIFI_MOT_DE_PASSE);

  const uint32_t limite = millis() + DELAI_CONNEXION_WIFI_MS;
  while (WiFi.status() != WL_CONNECTED && millis() < limite) {
    delay(250);
  }

  const bool connecte = WiFi.status() == WL_CONNECTED;
  if (connecte) {
    Serial.print(F("[api] Wi-Fi connecte, adresse "));
    Serial.println(WiFi.localIP());
  } else {
    Serial.println(F("[api] Wi-Fi injoignable, les releves restent en file"));
  }
  return connecte;
}

bool wifiConnecte() {
  return WiFi.status() == WL_CONNECTED;
}

Resultat envoyer(const file_attente::Evenement* evenements, size_t nombre,
                 int tentatives) {
  Resultat issue;

  if (!wifiConnecte()) {
    issue.message = F("hors ligne");
    return issue;
  }
  if (nombre == 0) {
    issue.succes = true;
    return issue;
  }

  JsonDocument corps;
  corps["deviceId"] = DEVICE_ID;
  corps["nombreTentatives"] = tentatives;

  JsonArray tableau = corps[nomDuTableau()].to<JsonArray>();
  for (size_t i = 0; i < nombre; i++) {
    JsonObject item = tableau.add<JsonObject>();
    item["deviceId"] = DEVICE_ID;
    item["biometricId"] = evenements[i].reference;
    item["date"] = evenements[i].date;
    item["heure"] = evenements[i].heure;
    item["createdAt"] = evenements[i].creeLe;

    if (USAGE == UsageLecteur::Etudiants) {
      // Le lecteur ignore l'emploi du temps : il constate une présence, et c'est
      // au serveur de la rattacher à une séance et de qualifier un éventuel retard.
      item["statut"] = "PRESENT";
    }
    // Flux personnel : le sens (entrée ou sortie) est volontairement omis. Le
    // serveur le déduit des pointages déjà enregistrés dans la journée, ce qui
    // reste juste même après un rejeu dans le désordre.
  }

  String charge;
  serializeJson(corps, charge);

  HTTPClient http;
  http.setTimeout(DELAI_REQUETE_HTTP_MS);
  http.begin(String(URL_SERVEUR) + cheminIngestion());
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-API-KEY", CLE_API);

  issue.codeHttp = http.POST(charge);

  if (issue.codeHttp == 200) {
    JsonDocument reponse;
    if (deserializeJson(reponse, http.getString()) == DeserializationError::Ok) {
      JsonObject donnees = reponse["data"];
      issue.inseres = donnees["totalInseres"] | 0;
      issue.doublons = donnees["totalIgnoresDoublons"] | 0;
    }
    issue.succes = true;
    Serial.printf("[api] lot accepte : %d insere(s), %d doublon(s)\n", issue.inseres,
                  issue.doublons);
  } else {
    issue.message = http.errorToString(issue.codeHttp);
    Serial.printf("[api] echec HTTP %d — le lot reste en file\n", issue.codeHttp);

    // 401 et 403 ne se résolvent pas en réessayant : la clé est fausse, révoquée,
    // ou l'appareil n'est pas habilité à ce flux. Le dire explicitement évite de
    // chercher une panne réseau qui n'existe pas.
    if (issue.codeHttp == 401 || issue.codeHttp == 403) {
      Serial.println(F("[api] cle d'API refusee ou usage de l'appareil non autorise"));
    }
  }

  http.end();
  return issue;
}

}  // namespace api
