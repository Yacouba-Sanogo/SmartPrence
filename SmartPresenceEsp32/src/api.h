#pragma once

#include <Arduino.h>

#include "file_attente.h"

/**
 * Dialogue avec le serveur SmartPresence.
 *
 * <p>L'appareil s'authentifie par un en-tête `X-API-KEY`, jamais par un jeton JWT : il
 * n'est pas un utilisateur, et n'a accès qu'aux deux routes d'ingestion.</p>
 *
 * <p>Le lecteur ne transmet que des <b>références logiques</b>. Il ignore les
 * identifiants internes du serveur, et aucune donnée biométrique ne circule.</p>
 */
namespace api {

/// Issue d'une tentative d'envoi.
struct Resultat {
  bool succes = false;
  int codeHttp = 0;
  int inseres = 0;
  int doublons = 0;
  String message;
};

/// Établit la connexion Wi-Fi. Retourne false au bout du délai configuré.
bool connecterWifi();

bool wifiConnecte();

/**
 * Transmet un lot de relevés.
 *
 * <p>Un lot rejoué après une coupure ne crée pas de doublon : le serveur écarte les
 * événements déjà connus sur la clé (appareil, personne, date, heure). C'est ce qui
 * autorise à renvoyer sans crainte tant qu'aucun acquittement n'est reçu.</p>
 *
 * @param evenements    lot à transmettre
 * @param nombre        taille du lot
 * @param tentatives    numéro de tentative, journalisé par le serveur
 */
Resultat envoyer(const file_attente::Evenement* evenements, size_t nombre,
                 int tentatives);

/// Étudiant attendu devant le lecteur, tel que le serveur le désigne.
struct DemandeEnrolement {
  String code;       ///< Code à six chiffres, à comparer à celui du téléphone
  String nom;        ///< Nom affiché : l'étudiant vérifie que c'est bien le sien
  String matricule;
  uint32_t secondesRestantes = 0;
};

/// Issue de la question « qui est attendu ? ».
enum class IssueDemande {
  Aucune,  ///< Personne n'attend (204)
  Demande, ///< `demande` est renseignée
  Erreur   ///< Réseau ou serveur : on réessaiera au prochain tour
};

/**
 * Demande au serveur qui est attendu devant ce lecteur.
 *
 * <p>Le serveur réserve la demande à ce lecteur quelques minutes, pour que deux capteurs
 * voisins n'appellent pas le même étudiant.</p>
 */
IssueDemande prochainEnrolement(DemandeEnrolement& demande);

/**
 * Annonce la référence produite par la capture, en citant le code de la demande.
 *
 * <p>Aucune donnée biométrique ne circule : la référence n'est que l'emplacement occupé
 * dans la mémoire du capteur.</p>
 */
bool annoncerEnrolement(const String& code, const String& reference);

}  // namespace api
