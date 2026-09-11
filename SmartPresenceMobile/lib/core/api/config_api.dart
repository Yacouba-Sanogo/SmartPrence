import 'package:flutter/foundation.dart';

/// Adresse du backend selon la plateforme d'exécution.
///
/// Trois situations, résolues dans cet ordre :
///
/// 1. Une adresse fournie au lancement l'emporte toujours —
///    `flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8080/api`
///    reste la façon de viser un serveur du réseau local depuis un appareil
///    physique, sans recompiler.
/// 2. En version distribuée (release), le serveur déployé sur Render. Une
///    application installée sur le téléphone d'un étudiant n'a évidemment aucun
///    moyen d'atteindre le poste de développement.
/// 3. En développement, le backend local. L'émulateur Android ne voit pas
///    `localhost` : la machine hôte y est exposée sous `10.0.2.2`. Le simulateur
///    iOS et le bureau, eux, atteignent `localhost` directement. Cette
///    distinction est la cause classique d'un « serveur injoignable » sur un
///    backend pourtant démarré.
class ConfigApi {
  ConfigApi._();

  static const String _surcharge = String.fromEnvironment('API_BASE_URL');

  /// Backend déployé (Render). Le contexte `/api` fait partie de l'adresse.
  static const String _production = 'https://smartprence.onrender.com/api';

  static String get baseUrl {
    if (_surcharge.isNotEmpty) return _surcharge;
    if (kReleaseMode) return _production;
    if (kIsWeb) return 'http://localhost:8080/api';
    // `defaultTargetPlatform` plutôt que `Platform.isAndroid` : ce dernier vient
    // de `dart:io`, absent du web. Son simple import y faisait échouer la
    // compilation — et l'échec se voyait à l'écran blanc, pas dans un message.
    if (defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080/api';
    }
    return 'http://localhost:8080/api';
  }

  /// Au-delà, on considère le serveur injoignable plutôt que lent.
  ///
  /// Quinze secondes suffisent face à un backend local, mais pas face à
  /// l'hébergement distant : l'offre gratuite de Render endort l'instance après
  /// quinze minutes sans trafic, et son réveil a été mesuré à 27 secondes. Avec
  /// le délai du développement, le tout premier appel de la journée expirerait
  /// systématiquement et l'application annoncerait « serveur injoignable » à
  /// propos d'un serveur parfaitement sain.
  ///
  /// Le critère est l'adresse elle-même : seul le serveur distant est en HTTPS.
  static Duration get delaiMax => baseUrl.startsWith('https://')
      ? const Duration(seconds: 60)
      : const Duration(seconds: 15);
}
