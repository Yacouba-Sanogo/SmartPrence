#pragma once

#include <Arduino.h>

/**
 * Capteur d'empreintes AS608.
 *
 * <b>Le gabarit ne quitte jamais le module.</b> L'AS608 stocke les empreintes dans sa
 * propre mémoire et ne renvoie qu'un numéro d'emplacement. Le firmware n'a donc accès à
 * aucune donnée biométrique, et n'en transmet aucune : c'est ce qui rend défendable
 * l'ensemble du dispositif.
 *
 * La référence transmise au serveur est construite à partir de cet emplacement —
 * `<PREFIXE_BIOMETRIQUE>-<slot>` — et n'a de sens que rapprochée de la table
 * d'enrôlement du backend.
 */
namespace empreinte {

/// Issue d'une identification.
enum class Resultat {
  Identifie,     ///< Doigt reconnu, `slot` et `score` renseignés
  NonReconnu,    ///< Doigt lu mais absent de la base du capteur
  LectureRatee,  ///< Image inexploitable — doigt mal posé, vitre sale
  PasDeDoigt,    ///< Aucun contact
  Erreur         ///< Capteur muet ou en défaut
};

struct Identification {
  Resultat resultat = Resultat::PasDeDoigt;
  uint16_t slot = 0;
  uint16_t score = 0;
};

/// Ouvre la liaison série et vérifie le mot de passe du module.
bool demarrer();

/// Nombre d'empreintes enregistrées dans le capteur.
uint16_t nombreEnregistrees();

/// `true` si un doigt est posé — lecture de la broche de détection, ou du capteur.
bool doigtPose();

/// Tente d'identifier le doigt posé.
Identification identifier();

/**
 * Construit la référence logique d'un emplacement.
 *
 * C'est cette chaîne, et elle seule, qui circule sur le réseau.
 */
String referenceDe(uint16_t slot);

/// Premier emplacement libre, ou 0 si la mémoire du capteur est pleine.
uint16_t premierSlotLibre();

/**
 * Enrôle une empreinte à l'emplacement indiqué.
 *
 * Deux captures sont demandées, comme l'exige l'AS608 : la seconde valide la première
 * et écarte une image prise de travers.
 *
 * @param progression appelé à chaque étape, pour l'afficheur
 * @return true si le gabarit a été enregistré
 */
bool enroler(uint16_t slot, void (*progression)(const String&));

/// Efface une empreinte du capteur.
bool supprimer(uint16_t slot);

}  // namespace empreinte
