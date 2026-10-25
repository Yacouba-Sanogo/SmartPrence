import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Apparence demandée par l'utilisateur.
enum ModeTheme { systeme, clair, sombre }

/// Réglages personnels, conservés sur l'appareil.
///
/// Rien ici n'est envoyé au serveur : ce sont des goûts d'affichage, pas des données de
/// scolarité. Chaque réglage agit réellement — le thème et la taille du texte sont appliqués
/// à toute l'application depuis [ApplicationSmartPresence], les vibrations et les animations
/// réduites sont lues là où elles comptent.
class PreferencesApp extends ChangeNotifier {
  static const String _cleTheme = 'pref_theme';
  static const String _cleTexte = 'pref_texte';
  static const String _cleVibrations = 'pref_vibrations';
  static const String _cleAnimations = 'pref_animations_reduites';

  /// Les quatre tailles proposées : le texte courant reste lisible à chacune.
  static const List<(String, double)> taillesTexte = [
    ('Petit', 0.9),
    ('Normal', 1.0),
    ('Grand', 1.15),
    ('Très grand', 1.3),
  ];

  ModeTheme _theme = ModeTheme.clair;
  double _echelleTexte = 1.0;
  bool _vibrations = true;
  bool _animationsReduites = false;

  ModeTheme get theme => _theme;
  double get echelleTexte => _echelleTexte;
  bool get vibrations => _vibrations;
  bool get animationsReduites => _animationsReduites;

  Future<void> charger() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      _theme = ModeTheme.values.byName(prefs.getString(_cleTheme) ?? 'clair');
      _echelleTexte = prefs.getDouble(_cleTexte) ?? 1.0;
      _vibrations = prefs.getBool(_cleVibrations) ?? true;
      _animationsReduites = prefs.getBool(_cleAnimations) ?? false;
      notifyListeners();
    } catch (_) {
      // Réglages illisibles : les valeurs par défaut conviennent.
    }
  }

  Future<void> definirTheme(ModeTheme valeur) => _ecrire(() => _theme = valeur, (p) => p.setString(_cleTheme, valeur.name));

  Future<void> definirEchelleTexte(double valeur) =>
      _ecrire(() => _echelleTexte = valeur, (p) => p.setDouble(_cleTexte, valeur));

  Future<void> definirVibrations(bool valeur) =>
      _ecrire(() => _vibrations = valeur, (p) => p.setBool(_cleVibrations, valeur));

  Future<void> definirAnimationsReduites(bool valeur) =>
      _ecrire(() => _animationsReduites = valeur, (p) => p.setBool(_cleAnimations, valeur));

  Future<void> _ecrire(VoidCallback appliquer, Future<bool> Function(SharedPreferences) persister) async {
    appliquer();
    notifyListeners();
    try {
      await persister(await SharedPreferences.getInstance());
    } catch (_) {}
  }

  /// Retour tactile léger, si l'utilisateur ne l'a pas coupé.
  void vibrer() {
    if (_vibrations) HapticFeedback.selectionClick();
  }
}

/// Photo de profil de la personne connectée, gardée sur l'appareil.
///
/// Le serveur n'a pas de champ photo : elle reste locale, par compte, et disparaît avec la
/// déconnexion si l'on choisit de la retirer. Redimensionnée à la prise (720 px), elle pèse
/// quelques dizaines de kilo-octets : sans conséquence dans les préférences.
class PhotoProfil extends ChangeNotifier {
  static const String _prefixe = 'photo_profil:';

  String? _compte;
  Uint8List? _octets;

  Uint8List? get octets => _octets;

  /// Charge la photo du compte [utilisateurId], ou vide si le compte change.
  Future<void> pour(String? utilisateurId) async {
    if (utilisateurId == _compte) return;
    _compte = utilisateurId;
    _octets = null;
    notifyListeners();
    if (utilisateurId == null) return;
    try {
      final prefs = await SharedPreferences.getInstance();
      final brut = prefs.getString('$_prefixe$utilisateurId');
      if (brut != null && _compte == utilisateurId) {
        _octets = base64Decode(brut);
        notifyListeners();
      }
    } catch (_) {}
  }

  Future<void> definir(Uint8List octets) async {
    _octets = octets;
    notifyListeners();
    final compte = _compte;
    if (compte == null) return;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('$_prefixe$compte', base64Encode(octets));
    } catch (_) {}
  }

  Future<void> retirer() async {
    _octets = null;
    notifyListeners();
    final compte = _compte;
    if (compte == null) return;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('$_prefixe$compte');
    } catch (_) {}
  }
}

/// Matrice du mode sombre : inversion de la luminosité, teintes conservées.
///
/// Inverser puis tourner la teinte de 180° donne un bleu royal qui devient un bleu clair sur
/// fond sombre, au lieu d'un jaune. L'opération est son propre inverse : appliquée une
/// seconde fois à une photo, elle lui rend ses couleurs d'origine ([ProtegeCouleurs]).
const List<double> matriceSombre = <double>[
  0.574, -1.430, -0.144, 0, 255, //
  -0.426, -0.430, -0.144, 0, 255, //
  -0.426, -1.430, 0.856, 0, 255, //
  0, 0, 0, 1, 0, //
];

/// Transmet aux écrans les réglages, la photo et l'état sombre.
class ContexteApp extends InheritedWidget {
  const ContexteApp({
    super.key,
    required this.preferences,
    required this.photo,
    required this.sombre,
    required super.child,
  });

  final PreferencesApp preferences;
  final PhotoProfil photo;
  final bool sombre;

  static ContexteApp? maybeOf(BuildContext context) => context.dependOnInheritedWidgetOfExactType<ContexteApp>();

  static final PreferencesApp _preferencesParDefaut = PreferencesApp();
  static final PhotoProfil _photoParDefaut = PhotoProfil();

  /// Les réglages du contexte ; à défaut (écran affiché seul, test), des réglages neutres.
  static ContexteApp of(BuildContext context) =>
      maybeOf(context) ??
      ContexteApp(preferences: _preferencesParDefaut, photo: _photoParDefaut, sombre: false, child: const SizedBox.shrink());

  @override
  bool updateShouldNotify(ContexteApp ancien) => sombre != ancien.sombre || preferences != ancien.preferences || photo != ancien.photo;
}

/// Rend leurs couleurs aux photos et logos quand le mode sombre inverse l'écran.
class ProtegeCouleurs extends StatelessWidget {
  const ProtegeCouleurs({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    if (ContexteApp.maybeOf(context)?.sombre != true) return child;
    return ColorFiltered(colorFilter: const ColorFilter.matrix(matriceSombre), child: child);
  }
}
