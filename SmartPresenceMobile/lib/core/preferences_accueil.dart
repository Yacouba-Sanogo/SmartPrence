import 'package:shared_preferences/shared_preferences.dart';

/// Mémorise que l'écran d'accueil a déjà été vu.
///
/// Un écran de présentation qui reparaît à chaque ouverture cesse d'être une
/// présentation pour devenir un péage : il ne s'affiche donc qu'au tout premier
/// lancement.
class PreferencesAccueil {
  const PreferencesAccueil();

  static const String _cle = 'smartpresence.accueil_vu';

  Future<bool> dejaVu() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_cle) ?? false;
  }

  Future<void> marquerVu() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_cle, true);
  }
}
