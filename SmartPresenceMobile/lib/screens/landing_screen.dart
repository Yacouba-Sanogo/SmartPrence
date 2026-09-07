import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../widgets/logo_enetp.dart';

/// Écran d'accueil affiché avant la connexion.
///
/// <p>Il ne sert à rien de fonctionnel — et c'est précisément son rôle : dire de quel
/// établissement il s'agit et ce que fait l'application, avant de demander quoi que ce
/// soit. Un formulaire de connexion nu ne dit ni l'un ni l'autre.</p>
///
/// <p>Il n'apparaît qu'une fois : le voir à chaque ouverture deviendrait un péage.</p>
class LandingScreen extends StatelessWidget {
  const LandingScreen({super.key, required this.surCommencer});

  final VoidCallback surCommencer;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.indigo900,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, contraintes) {
            // La carte occupe une part fixe de la hauteur : sur un petit écran elle
            // rétrécit avec le reste, plutôt que de repousser le bouton hors du champ.
            final hauteurCarte = math.max(280.0, contraintes.maxHeight * 0.52);

            return Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(
                      Espaces.lg, Espaces.md, Espaces.lg, Espaces.xl),
                  child: SizedBox(
                    height: hauteurCarte,
                    width: double.infinity,
                    child: const _CarteIllustration(),
                  ),
                ),
                Expanded(child: _texteEtAction(context)),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _texteEtAction(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
          Espaces.xxl, 0, Espaces.xxl, Espaces.xl),
      // Le bloc est centré dans l'espace restant plutôt que réparti aux extrémités :
      // « spaceBetween » collait le bouton au bord bas et creusait un vide au milieu.
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Column(
            children: [
              Text(
                'ENETP',
                textAlign: TextAlign.center,
                style: Typo.suretitre.copyWith(
                  color: CouleursEnetp.plume,
                  fontSize: 12,
                  letterSpacing: 2.4,
                ),
              ),
              const SizedBox(height: Espaces.md),
              Text(
                'La présence,\nétablie par l’empreinte.',
                textAlign: TextAlign.center,
                style: Typo.titreEcran.copyWith(
                  color: Colors.white,
                  fontSize: 25,
                  height: 1.28,
                ),
              ),
              const SizedBox(height: Espaces.md),
              Text(
                'Votre passage est relevé par le capteur, à l’entrée. '
                'Aucune empreinte ne quitte l’appareil.',
                textAlign: TextAlign.center,
                style: Typo.corpsAttenue.copyWith(
                  color: Colors.white.withValues(alpha: 0.62),
                  fontSize: 13,
                  height: 1.55,
                ),
              ),
            ],
          ),
          const SizedBox(height: Espaces.xxxl),
          _bouton(),
        ],
      ),
    );
  }

  /// Bouton d'entrée, en or.
  ///
  /// L'or est repris de la plume du logo : sur l'indigo profond, c'est la seule teinte
  /// de l'identité de l'école qui ressorte franchement, et elle rattache l'écran à
  /// l'établissement plutôt qu'au produit.
  Widget _bouton() {
    return Container(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [CouleursEnetp.plume, Color(0xFFEFA714)],
        ),
        borderRadius: BorderRadius.circular(Rayons.pilule),
        boxShadow: const [
          BoxShadow(
            color: Color(0x59F0B429),
            blurRadius: 22,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(Rayons.pilule),
        child: InkWell(
          onTap: surCommencer,
          borderRadius: BorderRadius.circular(Rayons.pilule),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: Espaces.lg),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Commencer',
                  style: Typo.bouton.copyWith(
                    color: Couleurs.indigo900,
                    fontSize: 14.5,
                  ),
                ),
                const SizedBox(width: Espaces.sm),
                const Icon(Icons.arrow_forward_rounded,
                    size: 18, color: Couleurs.indigo900),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Carte blanche portant le logo et l'illustration.
class _CarteIllustration extends StatelessWidget {
  const _CarteIllustration();

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(Rayons.bloc + 8),
        boxShadow: const [
          BoxShadow(
            color: Color(0x33070B25),
            blurRadius: 30,
            offset: Offset(0, 12),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(Rayons.bloc + 8),
        child: Stack(
          alignment: Alignment.center,
          children: [
            // Halo diffus derrière l'empreinte : il donne du volume sans motif.
            Positioned(
              child: Container(
                width: 220,
                height: 220,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Couleurs.indigo50,
                ),
              ),
            ),
            const _AccentsFlottants(),
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const LogoEnetp(taille: 104),
                const SizedBox(height: Espaces.lg),
                Container(
                  width: 96,
                  height: 96,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    gradient: Couleurs.degradeEntete,
                    borderRadius: BorderRadius.circular(30),
                    boxShadow: Couleurs.ombreIndigo,
                  ),
                  child: const Icon(Icons.fingerprint_rounded,
                      size: 52, color: Colors.white),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// Formes décoratives reprenant les couleurs de l'établissement.
class _AccentsFlottants extends StatelessWidget {
  const _AccentsFlottants();

  @override
  Widget build(BuildContext context) {
    return Positioned.fill(
      child: Stack(
        children: [
          Positioned(
            top: 26,
            left: 26,
            child: _pastille(CouleursEnetp.vert, 14, ronde: true),
          ),
          Positioned(
            top: 58,
            right: 34,
            child: _pastille(CouleursEnetp.rouge, 20),
          ),
          Positioned(
            bottom: 46,
            left: 34,
            child: _pastille(CouleursEnetp.plume, 24),
          ),
          Positioned(
            bottom: 30,
            right: 28,
            child: _pastille(Couleurs.indigo300, 12, ronde: true),
          ),
        ],
      ),
    );
  }

  Widget _pastille(Color couleur, double taille, {bool ronde = false}) {
    return Container(
      width: taille,
      height: taille,
      decoration: BoxDecoration(
        color: couleur.withValues(alpha: 0.16),
        shape: ronde ? BoxShape.circle : BoxShape.rectangle,
        borderRadius: ronde ? null : BorderRadius.circular(taille * 0.34),
      ),
    );
  }
}
