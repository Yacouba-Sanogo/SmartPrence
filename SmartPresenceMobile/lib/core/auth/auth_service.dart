import 'dart:async';

import 'package:flutter/foundation.dart';

import '../api/client_api.dart';
import '../api/erreurs.dart';
import '../../models/profil.dart';
import 'session.dart';

/// État d'authentification de l'application.
enum EtatAuth { demarrage, deconnecte, connecte }

/// Authentification réelle contre le backend.
///
/// Remplace la version simulée, qui se contentait d'attendre 800 ms avant de retourner
/// un utilisateur factice choisi selon que l'email contenait « admin ».
///
/// La connexion se fait en deux temps : `POST /auth/login` fournit le jeton, puis
/// `GET /moi/profil` résout le rattachement métier — le compte seul ne dit pas si la
/// personne est étudiante ou enseignante, et donc quels écrans lui ouvrir.
class AuthService extends ChangeNotifier {
  AuthService({ClientApi? client, DepotSession? depot})
      : _depot = depot ?? DepotSession() {
    _client = client ?? ClientApi(surSessionExpiree: _surSessionExpiree);
  }

  late final ClientApi _client;
  final DepotSession _depot;

  ClientApi get client => _client;

  EtatAuth _etat = EtatAuth.demarrage;
  EtatAuth get etat => _etat;

  Profil? _profil;
  Profil? get profil => _profil;

  bool _enCours = false;
  bool get enCours => _enCours;

  /// Restaure une session existante au lancement.
  ///
  /// Un jeton présent ne suffit pas : il est confronté au serveur via le profil. Un
  /// compte supprimé ou désactivé entre deux ouvertures doit renvoyer à la connexion,
  /// pas ouvrir une application vide.
  Future<void> demarrer() async {
    final session = await _depot.lire();
    if (session == null) {
      _passerA(EtatAuth.deconnecte);
      return;
    }

    _client.definirJeton(session.jeton);
    try {
      _profil = await _chargerProfil();
      _passerA(EtatAuth.connecte);
    } on ErreurApi {
      await _fermerSession();
    }
  }

  Future<void> connecter(String email, String motDePasse) async {
    _enCours = true;
    notifyListeners();

    try {
      final reponse = await _client.post('/auth/login', corps: {
        'email': email.trim(),
        'motDePasse': motDePasse,
      });

      final session = Session.depuisReponseAuth(reponse as Map<String, dynamic>);
      if (session.jeton.isEmpty) {
        throw const ErreurApi(MessagesErreur.inattendue);
      }

      _client.definirJeton(session.jeton);
      await _depot.ecrire(session);
      _profil = await _chargerProfil();
      _passerA(EtatAuth.connecte);
    } finally {
      _enCours = false;
      notifyListeners();
    }
  }

  Future<void> deconnecter() => _fermerSession();

  Future<Profil> _chargerProfil() async {
    final donnees = await _client.get('/moi/profil');
    return Profil.depuisJson(donnees as Map<String, dynamic>);
  }

  /// Appelé par le client HTTP sur toute réponse 401.
  void _surSessionExpiree() {
    if (_etat == EtatAuth.connecte) {
      unawaited(_fermerSession());
    }
  }

  Future<void> _fermerSession() async {
    _client.definirJeton(null);
    _profil = null;
    await _depot.effacer();
    _passerA(EtatAuth.deconnecte);
  }

  void _passerA(EtatAuth etat) {
    _etat = etat;
    notifyListeners();
  }
}
