import 'package:flutter/material.dart';

import '../core/auth/auth_service.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../widgets/logo_enetp.dart';
import 'concours_screen.dart';
import 'inscription_screen.dart';

/// Écran d'accueil affiché avant la connexion.
///
/// <p>Le logo de l'école en haut sur fond clair, et en bas un grand panneau bleu à
/// bord supérieur courbe qui porte le message et les deux entrées de l'application :
/// créer un compte, ou se connecter. Il dit de quel établissement il s'agit et ce que
/// fait l'application avant de demander quoi que ce soit.</p>
///
/// <p>Il n'apparaît qu'une fois : le voir à chaque ouverture deviendrait un péage.</p>
class LandingScreen extends StatelessWidget {
  const LandingScreen({super.key, required this.auth, required this.surCommencer});

  final AuthService auth;

  /// « Se connecter » : marque l'accueil comme vu et ouvre la connexion.
  final VoidCallback surCommencer;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: LayoutBuilder(
        builder: (context, contraintes) {
          // Le panneau prend 46 % de la hauteur, avec un plancher : sur un petit écran
          // il ne doit pas repousser les boutons hors du champ.
          final hauteurPanneau =
              (contraintes.maxHeight * 0.46).clamp(330.0, 460.0).toDouble();

          return Stack(
            children: [
              Positioned(
                left: 0,
                right: 0,
                top: 0,
                bottom: hauteurPanneau - 36,
                child: SafeArea(
                  bottom: false,
                  child: Center(child: _entete()),
                ),
              ),
              Positioned(
                left: 0,
                right: 0,
                bottom: 0,
                height: hauteurPanneau,
                child: _panneau(context),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _entete() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const LogoEnetp(taille: 150),
        const SizedBox(height: Espaces.lg),
        Text(
          'ENETP',
          style: Typo.titreEcran.copyWith(
              color: Couleurs.royal800, fontSize: 24, letterSpacing: 4),
        ),
        const SizedBox(height: 2),
        Text(
          'SmartPresence',
          style: Typo.corpsAttenue.copyWith(letterSpacing: 1.2),
        ),
      ],
    );
  }

  Widget _panneau(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        gradient: Couleurs.degradeEntete,
        // Bord supérieur en grande courbe elliptique, comme dans la maquette.
        borderRadius: BorderRadius.vertical(top: Radius.elliptical(520, 90)),
      ),
      padding: const EdgeInsets.fromLTRB(Espaces.xxl, 54, Espaces.xxl, Espaces.lg),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text.rich(
              TextSpan(
                children: [
                  const TextSpan(text: 'Votre présence,\nétablie par '),
                  TextSpan(
                    text: 'l’empreinte',
                    style: TextStyle(color: CouleursEnetp.plume),
                  ),
                ],
              ),
              textAlign: TextAlign.center,
              style: Typo.titreEcran
                  .copyWith(color: Colors.white, fontSize: 24, height: 1.3),
            ),
            const SizedBox(height: Espaces.md),
            Text(
              'Suivez vos présences, votre emploi du temps et vos notes. '
              'Aucune empreinte ne quitte le lecteur.',
              textAlign: TextAlign.center,
              style: Typo.corpsAttenue.copyWith(
                color: Colors.white.withValues(alpha: 0.72),
                fontSize: 13,
                height: 1.55,
              ),
            ),
            const SizedBox(height: Espaces.xl),
            FilledButton(
              onPressed: () => Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => InscriptionScreen(auth: auth))),
              style: FilledButton.styleFrom(
                backgroundColor: Colors.white,
                foregroundColor: Couleurs.royal800,
                minimumSize: const Size.fromHeight(50),
                shape: const StadiumBorder(),
              ),
              child: const Text('Créer mon compte'),
            ),
            const SizedBox(height: Espaces.md),
            OutlinedButton(
              onPressed: surCommencer,
              style: OutlinedButton.styleFrom(
                foregroundColor: Colors.white,
                side: BorderSide(color: Colors.white.withValues(alpha: 0.7)),
                minimumSize: const Size.fromHeight(50),
                shape: const StadiumBorder(),
              ),
              child: const Text('Se connecter'),
            ),
            const SizedBox(height: Espaces.sm),
            TextButton(
              onPressed: () => Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => ConcoursScreen(client: auth.client))),
              style: TextButton.styleFrom(
                  foregroundColor: Colors.white.withValues(alpha: 0.8)),
              child: const Text("Concours d'entrée à l'ENETP"),
            ),
          ],
        ),
      ),
    );
  }
}
