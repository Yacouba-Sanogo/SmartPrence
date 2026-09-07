#include "horloge.h"

#include <RTClib.h>
#include <WiFi.h>
#include <Wire.h>
#include <time.h>

#include "config.h"

namespace {

RTC_DS3231 rtc;
bool disponible = false;

/// Formate un instant UTC décalé du fuseau configuré.
String formater(uint32_t instantUtc, const char* motif, bool appliquerDecalage) {
  const time_t brut =
      static_cast<time_t>(instantUtc) +
      (appliquerDecalage ? DECALAGE_UTC_MINUTES * 60 : 0);

  // gmtime_r et non localtime_r : le décalage est appliqué à la main, la
  // bibliothèque C n'ayant pas de base de fuseaux embarquée.
  struct tm eclate;
  gmtime_r(&brut, &eclate);

  char tampon[32];
  strftime(tampon, sizeof(tampon), motif, &eclate);
  return String(tampon);
}

}  // namespace

namespace horloge {

bool demarrer() {
  Wire.begin(BROCHE_I2C_SDA, BROCHE_I2C_SCL);
  disponible = rtc.begin();
  if (!disponible) {
    Serial.println(F("[horloge] DS3231 introuvable sur le bus I2C"));
  }
  return disponible;
}

bool heurePerdue() {
  return !disponible || rtc.lostPower();
}

bool recalerSurNtp() {
  if (WiFi.status() != WL_CONNECTED) return false;

  // Le serveur NTP fournit de l'UTC ; le décalage local est appliqué à l'affichage,
  // jamais au stockage.
  configTime(0, 0, SERVEUR_NTP);

  struct tm obtenu;
  if (!getLocalTime(&obtenu, 8000)) {
    Serial.println(F("[horloge] NTP injoignable, on garde l'heure du DS3231"));
    return false;
  }

  const uint32_t reference = static_cast<uint32_t>(mktime(&obtenu));
  if (!disponible) return false;

  const uint32_t courant = rtc.now().unixtime();
  const int32_t derive = static_cast<int32_t>(reference) - static_cast<int32_t>(courant);

  if (abs(derive) <= DERIVE_TOLEREE_SECONDES && !rtc.lostPower()) {
    return false;
  }

  rtc.adjust(DateTime(reference));
  Serial.printf("[horloge] DS3231 recale, derive corrigee : %ld s\n",
                static_cast<long>(derive));
  return true;
}

uint32_t maintenantUtc() {
  if (disponible) return rtc.now().unixtime();

  // Repli sur l'horloge interne : elle dérive et repart à zéro au redémarrage,
  // mais vaut mieux qu'un horodatage absent.
  return static_cast<uint32_t>(time(nullptr));
}

String dateLocale(uint32_t instantUtc) {
  return formater(instantUtc, "%Y-%m-%d", true);
}

String heureLocale(uint32_t instantUtc) {
  return formater(instantUtc, "%H:%M:%S", true);
}

String isoUtc(uint32_t instantUtc) {
  return formater(instantUtc, "%Y-%m-%dT%H:%M:%SZ", false);
}

String pourAffichage(uint32_t instantUtc) {
  return formater(instantUtc, "%d/%m %H:%M", true);
}

}  // namespace horloge
