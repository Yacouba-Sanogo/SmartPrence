#pragma once

/**
 * Identifiants propres a votre installation.
 *
 * Copiez ce fichier en `identifiants.h` et renseignez les valeurs. `identifiants.h`
 * est ignore par git : une cle d'API dans un depot est une cle compromise.
 */

// ----- Reseau -------------------------------------------------------------
static constexpr const char* WIFI_SSID = "NOM_DU_RESEAU";
static constexpr const char* WIFI_MOT_DE_PASSE = "MOT_DE_PASSE";

// ----- Serveur ------------------------------------------------------------
//
// Adresse du backend, sans barre oblique finale. Le contexte /api est ajoute par
// le firmware.
static constexpr const char* URL_SERVEUR = "http://192.168.1.10:8080";

/**
 * Cle d'API de cet appareil.
 *
 * Elle est definie a la creation du lecteur dans l'administration web
 * (ecran Appareils). Le serveur n'en conserve qu'une empreinte SHA-256 : elle ne
 * peut pas etre relue, seulement remplacee.
 */
static constexpr const char* CLE_API = "REMPLACER_PAR_LA_CLE";

/**
 * Identifiant du lecteur, tel qu'affiche dans l'administration web.
 *
 * Le serveur verifie qu'il correspond a la cle d'API presentee : un lot signe par
 * une cle mais annonce par un autre appareil est refuse.
 */
static constexpr const char* DEVICE_ID = "00000000-0000-0000-0000-000000000000";

/**
 * Prefixe des references logiques produites a l'enrolement.
 *
 * La reference vaut `<PREFIXE>-<slot>`, par exemple `ENETP-B12-007`. Elle doit etre
 * unique dans l'etablissement : deux lecteurs partageant le meme prefixe
 * attribueraient la meme reference a deux personnes differentes.
 */
static constexpr const char* PREFIXE_BIOMETRIQUE = "ENETP-B12";
