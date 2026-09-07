import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';

/// Anneau de progression.
///
/// Un pourcentage seul est un nombre ; le même nombre dans un anneau se lit sans être
/// lu — on voit en un instant si la part est petite ou grande. C'est ce qui fait de cet
/// élément le point d'entrée naturel des écrans d'assiduité.
class Anneau extends StatelessWidget {
  const Anneau({
    super.key,
    required this.valeur,
    this.taille = 86,
    this.epaisseur = 11,
    this.couleur,
    this.couleurFond,
    this.centre,
    this.suffixe = '%',
  });

  /// Part remplie, de 0 à 100.
  final double valeur;

  final double taille;
  final double epaisseur;

  /// Teinte de l'arc. Par défaut, elle suit la valeur : verte au-dessus de 75,
  /// ambre au-dessus de 50, rouge en dessous.
  final Color? couleur;

  final Color? couleurFond;

  /// Contenu du centre. À défaut, le pourcentage.
  final Widget? centre;

  final String suffixe;

  Color get _teinte {
    if (couleur != null) return couleur!;
    if (valeur >= 75) return Couleurs.succesVif;
    if (valeur >= 50) return Couleurs.alerteVif;
    return Couleurs.dangerVif;
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: taille,
      height: taille,
      child: Stack(
        alignment: Alignment.center,
        children: [
          // TweenAnimationBuilder plutôt qu'une valeur figée : l'anneau se remplit à
          // l'arrivée des données, ce qui signale que l'écran a fini de charger.
          TweenAnimationBuilder<double>(
            tween: Tween(begin: 0, end: valeur.clamp(0, 100)),
            duration: const Duration(milliseconds: 750),
            curve: Curves.easeOutCubic,
            builder: (context, valeurAnimee, _) => CustomPaint(
              size: Size.square(taille),
              painter: _PeintreAnneau(
                valeur: valeurAnimee,
                epaisseur: epaisseur,
                teinte: _teinte,
                fond: couleurFond ?? Couleurs.traitLeger,
              ),
            ),
          ),
          centre ??
              Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    '${valeur.round()}',
                    style: Typo.mono(taille * 0.245, graisse: FontWeight.w600),
                  ),
                  if (suffixe.isNotEmpty)
                    Text(suffixe,
                        style: Typo.legendePale.copyWith(fontSize: taille * 0.12)),
                ],
              ),
        ],
      ),
    );
  }
}

class _PeintreAnneau extends CustomPainter {
  const _PeintreAnneau({
    required this.valeur,
    required this.epaisseur,
    required this.teinte,
    required this.fond,
  });

  final double valeur;
  final double epaisseur;
  final Color teinte;
  final Color fond;

  @override
  void paint(Canvas canvas, Size size) {
    final centre = Offset(size.width / 2, size.height / 2);
    final rayon = (size.width - epaisseur) / 2;

    final piste = Paint()
      ..color = fond
      ..style = PaintingStyle.stroke
      ..strokeWidth = epaisseur;
    canvas.drawCircle(centre, rayon, piste);

    if (valeur <= 0) return;

    final arc = Paint()
      ..color = teinte
      ..style = PaintingStyle.stroke
      ..strokeWidth = epaisseur
      // Extrémités arrondies : sans elles, l'arc se termine en biseau et paraît cassé.
      ..strokeCap = StrokeCap.round;

    canvas.drawArc(
      Rect.fromCircle(center: centre, radius: rayon),
      -math.pi / 2,
      2 * math.pi * (valeur / 100),
      false,
      arc,
    );
  }

  @override
  bool shouldRepaint(_PeintreAnneau ancien) =>
      ancien.valeur != valeur ||
      ancien.teinte != teinte ||
      ancien.epaisseur != epaisseur;
}
