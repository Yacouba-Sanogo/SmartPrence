import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

/// Session persistée entre deux lancements de l'application.
///
/// Conservée sur l'appareil pour éviter de redemander les identifiants à chaque
/// ouverture. L'expiration est vérifiée à la restauration : un jeton périmé est écarté
/// plutôt que présenté au serveur pour se faire rejeter.
class Session {
  const Session({
    required this.jeton,
    required this.jetonRafraichissement,
    required this.expireLe,
    required this.email,
  });

  final String jeton;
  final String jetonRafraichissement;
  final DateTime expireLe;
  final String email;

  bool get valide => DateTime.now().isBefore(expireLe);

  Map<String, dynamic> versJson() => {
        'jeton': jeton,
        'jetonRafraichissement': jetonRafraichissement,
        'expireLe': expireLe.toIso8601String(),
        'email': email,
      };

  factory Session.depuisJson(Map<String, dynamic> json) => Session(
        jeton: json['jeton'] as String? ?? '',
        jetonRafraichissement: json['jetonRafraichissement'] as String? ?? '',
        expireLe: DateTime.tryParse(json['expireLe'] as String? ?? '') ??
            DateTime.fromMillisecondsSinceEpoch(0),
        email: json['email'] as String? ?? '',
      );

  /// Construit la session à partir de la réponse d'authentification du backend.
  ///
  /// La réponse est **plate** : ni objet `user` imbriqué, ni date d'expiration —
  /// seulement une durée en millisecondes, qu'il faut convertir en instant.
  factory Session.depuisReponseAuth(Map<String, dynamic> json) {
    final dureeMs = (json['expiresInMs'] as num?)?.toInt() ?? 0;
    return Session(
      jeton: json['accessToken'] as String? ?? '',
      jetonRafraichissement: json['refreshToken'] as String? ?? '',
      expireLe: DateTime.now().add(Duration(milliseconds: dureeMs)),
      email: json['email'] as String? ?? '',
    );
  }
}

/// Stockage local de la session.
class DepotSession {
  static const String _cle = 'smartpresence.session';

  Future<Session?> lire() async {
    final prefs = await SharedPreferences.getInstance();
    final brut = prefs.getString(_cle);
    if (brut == null) return null;
    try {
      final session = Session.depuisJson(jsonDecode(brut) as Map<String, dynamic>);
      if (!session.valide || session.jeton.isEmpty) {
        await effacer();
        return null;
      }
      return session;
    } catch (_) {
      await effacer();
      return null;
    }
  }

  Future<void> ecrire(Session session) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_cle, jsonEncode(session.versJson()));
  }

  Future<void> effacer() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_cle);
  }
}
