import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

/// Photo du bâtiment de l'école, teintée de bleu royal, sous un voile qui s'ouvre vers la droite.
///
/// Recadrée sur la façade : le bas de l'original montre des voitures, qu'on ne veut pas voir.
/// [intensite] règle la présence de la photo : forte sur l'accueil, discrète derrière un titre.
class FondPhotoCampus extends StatelessWidget {
  const FondPhotoCampus({super.key, this.intensite = 1});

  final double intensite;

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        Opacity(
          opacity: 0.62 * intensite,
          child: ColorFiltered(
            // Niveaux de gris, un peu éclaircis : la façade ocre devient bleutée sous le voile.
            colorFilter: const ColorFilter.matrix(<double>[
              0.33, 0.33, 0.33, 0, 25, //
              0.33, 0.33, 0.33, 0, 25, //
              0.33, 0.33, 0.33, 0, 25, //
              0, 0, 0, 1, 0, //
            ]),
            child: Image.asset(
              'assets/images/campus.jpg',
              fit: BoxFit.cover,
              alignment: Alignment.centerRight,
              filterQuality: FilterQuality.medium,
              errorBuilder: (_, __, ___) => const SizedBox.shrink(),
            ),
          ),
        ),
        // Voile bleu : opaque à gauche pour le texte, plus transparent à droite pour la photo.
        DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.centerLeft,
              end: Alignment.centerRight,
              colors: [
                Couleurs.royal900.withValues(alpha: 0.96),
                Couleurs.royal800.withValues(alpha: 0.82),
                Couleurs.royal600.withValues(alpha: 0.38 + (1 - intensite) * 0.4),
              ],
              stops: const [0.0, 0.5, 1.0],
            ),
          ),
        ),
      ],
    );
  }
}
