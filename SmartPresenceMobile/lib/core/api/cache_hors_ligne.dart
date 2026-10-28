import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

/// Dernières réponses de lecture du serveur, gardées sur l'appareil.
///
/// Elles permettent de continuer à consulter l'emploi du temps, les classes, les notes ou
/// les présences quand le réseau manque. Seules les lectures sont conservées : une écriture
/// (présence, note, annonce…) ne se rejoue jamais en différé, parce qu'elle pourrait arriver
/// après un changement fait entre-temps par quelqu'un d'autre.
///
/// Chaque panne de stockage est absorbée : le cache est un confort, jamais une condition.
class CacheHorsLigne {
  CacheHorsLigne();

  static const String _prefixe = 'cache_api:';

  /// Écritures et effacement passent l'un après l'autre : une écriture lancée juste avant
  /// la déconnexion ne doit pas réapparaître après l'effacement.
  Future<void> _file = Future<void>.value();

  Future<void> _enfiler(Future<void> Function() tache) =>
      _file = _file.then((_) => tache());

  Future<void> ecrire(String cle, Object? donnees) => _enfiler(() async {
        try {
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString(
              '$_prefixe$cle',
              jsonEncode(
                  {'t': DateTime.now().toIso8601String(), 'd': donnees}));
        } catch (_) {}
      });

  /// Dernière réponse enregistrée pour [cle], ou `null` si aucune.
  Future<EntreeCache?> lire(String cle) async {
    await _file;
    try {
      final prefs = await SharedPreferences.getInstance();
      final brut = prefs.getString('$_prefixe$cle');
      if (brut == null) return null;
      final json = jsonDecode(brut) as Map<String, dynamic>;
      return EntreeCache(json['d'], DateTime.parse(json['t'] as String));
    } catch (_) {
      return null;
    }
  }

  /// Efface tout : appelé à la déconnexion, pour que le compte suivant ne voie rien du précédent.
  Future<void> vider() => _enfiler(() async {
        try {
          final prefs = await SharedPreferences.getInstance();
          for (final cle in prefs
              .getKeys()
              .where((c) => c.startsWith(_prefixe))
              .toList()) {
            await prefs.remove(cle);
          }
        } catch (_) {}
      });
}

class EntreeCache {
  const EntreeCache(this.donnees, this.enregistreLe);

  final Object? donnees;
  final DateTime enregistreLe;
}
