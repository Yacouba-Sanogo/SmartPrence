import 'dart:io' show Platform;

import 'package:flutter/foundation.dart';

/// Adresse du backend selon la plateforme d'exécution.
///
/// L'émulateur Android ne voit pas `localhost` : la machine hôte y est exposée sous
/// `10.0.2.2`. Le simulateur iOS et le bureau, eux, atteignent `localhost` directement.
/// Cette distinction est la cause classique d'un « serveur injoignable » sur un backend
/// pourtant démarré.
///
/// Sur un appareil physique, il faut l'adresse IP de la machine sur le réseau local :
/// la fournir au lancement, sans recompiler —
/// `flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8080/api`
class ConfigApi {
  ConfigApi._();

  static const String _surcharge = String.fromEnvironment('API_BASE_URL');

  static String get baseUrl {
    if (_surcharge.isNotEmpty) return _surcharge;
    if (kIsWeb) return 'http://localhost:8080/api';
    if (Platform.isAndroid) return 'http://10.0.2.2:8080/api';
    return 'http://localhost:8080/api';
  }

  /// Au-delà, on considère le serveur injoignable plutôt que lent.
  static const Duration delaiMax = Duration(seconds: 15);
}
