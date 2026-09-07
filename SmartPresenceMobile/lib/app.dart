import 'package:flutter/material.dart';

import 'core/auth/auth_service.dart';
import 'core/preferences_accueil.dart';
import 'core/design/couleurs.dart';
import 'core/design/theme.dart';
import 'core/design/typographie.dart';
import 'screens/connexion_screen.dart';
import 'screens/landing_screen.dart';
import 'screens/enseignant/shell_enseignant.dart';
import 'screens/etudiant/shell_etudiant.dart';
import 'widgets/communs.dart';

/// Racine de l'application.
class ApplicationSmartPresence extends StatefulWidget {
  const ApplicationSmartPresence({super.key});

  @override
  State<ApplicationSmartPresence> createState() => _ApplicationSmartPresenceState();
}

class _ApplicationSmartPresenceState extends State<ApplicationSmartPresence> {
  final _auth = AuthService();
  final _preferences = const PreferencesAccueil();

  /// `null` tant que la préférence n'a pas été lue : afficher la présentation puis
  /// la faire disparaître aurait produit un clignotement à chaque lancement.
  bool? _accueilVu;

  @override
  void initState() {
    super.initState();
    _auth.demarrer();
    _preferences.dejaVu().then((vu) {
      if (mounted) setState(() => _accueilVu = vu);
    });
  }

  Future<void> _passerLAccueil() async {
    await _preferences.marquerVu();
    if (mounted) setState(() => _accueilVu = true);
  }

  @override
  void dispose() {
    _auth.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SmartPresence',
      debugShowCheckedModeBanner: false,
      theme: themeSmartPresence(),
      home: ListenableBuilder(
        listenable: _auth,
        builder: (context, _) => _ecranCourant(),
      ),
    );
  }

  Widget _ecranCourant() {
    if (_auth.etat == EtatAuth.demarrage || _accueilVu == null) {
      return const _EcranDemarrage();
    }
    if (_auth.etat == EtatAuth.deconnecte) {
      return _accueilVu == false
          ? LandingScreen(surCommencer: _passerLAccueil)
          : ConnexionScreen(auth: _auth);
    }
    return _apresConnexion();
  }

  /// Oriente vers l'espace correspondant au profil résolu.
  ///
  /// Un compte d'administration n'a pas d'écran mobile : plutôt qu'une interface
  /// vide, on le dit franchement et on laisse la déconnexion accessible.
  Widget _apresConnexion() {
    final profil = _auth.profil;

    if (profil != null && profil.estEtudiant) {
      return ShellEtudiant(auth: _auth);
    }
    if (profil != null && profil.estEnseignant) {
      return ShellEnseignant(auth: _auth);
    }

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espaces.xxl),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const EtatVide(
                icone: Icons.desktop_windows_outlined,
                titre: 'Aucun espace mobile pour votre profil',
                detail: "L'application mobile s'adresse aux étudiants et aux "
                    "enseignants. Utilisez l'administration web pour les autres "
                    'profils.',
              ),
              const SizedBox(height: Espaces.lg),
              TextButton(
                onPressed: _auth.deconnecter,
                child: const Text('Se déconnecter'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Écran affiché le temps de restaurer la session.
class _EcranDemarrage extends StatelessWidget {
  const _EcranDemarrage();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.royal800,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 62,
              height: 62,
              decoration: BoxDecoration(
                color: Couleurs.royal600,
                borderRadius: BorderRadius.circular(18),
              ),
              child: const Icon(Icons.fingerprint_rounded,
                  color: Colors.white, size: 32),
            ),
            const SizedBox(height: Espaces.lg),
            Text('SmartPresence',
                style: Typo.titreSection.copyWith(color: Colors.white)),
          ],
        ),
      ),
    );
  }
}
