import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

/// Silhouette animée qui tient la place d'un bloc en cours de chargement.
///
/// Une page qui se dessine par blocs gris puis se remplit paraît plus rapide qu'un cercle
/// qui tourne seul au milieu du vide : on voit déjà où les choses vont apparaître.
class Squelette extends StatefulWidget {
  const Squelette({super.key, this.hauteur = 80, this.largeur, this.rayon = Rayons.carte});

  final double hauteur;
  final double? largeur;
  final double rayon;

  @override
  State<Squelette> createState() => _SqueletteState();
}

class _SqueletteState extends State<Squelette> with SingleTickerProviderStateMixin {
  late final AnimationController _anim = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400))..repeat();

  @override
  void dispose() {
    _anim.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _anim,
      builder: (context, _) {
        final p = _anim.value;
        return Container(
          height: widget.hauteur,
          width: widget.largeur ?? double.infinity,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(widget.rayon),
            gradient: LinearGradient(
              begin: Alignment(-1.5 + 3 * p, 0),
              end: Alignment(-0.5 + 3 * p, 0),
              colors: const [Color(0xFFE9EDF6), Color(0xFFF6F8FC), Color(0xFFE9EDF6)],
            ),
          ),
        );
      },
    );
  }
}

/// Squelette de l'accueil : carte d'assiduité, trois statistiques, bannière.
class SqueletteAccueil extends StatelessWidget {
  const SqueletteAccueil({super.key});

  @override
  Widget build(BuildContext context) {
    return const Column(
      children: [
        Squelette(hauteur: 130),
        SizedBox(height: Espaces.sm + 2),
        Row(
          children: [
            Expanded(child: Squelette(hauteur: 130, rayon: Rayons.carte - 2)),
            SizedBox(width: Espaces.sm),
            Expanded(child: Squelette(hauteur: 130, rayon: Rayons.carte - 2)),
            SizedBox(width: Espaces.sm),
            Expanded(child: Squelette(hauteur: 130, rayon: Rayons.carte - 2)),
          ],
        ),
        SizedBox(height: Espaces.md + 2),
        Squelette(hauteur: 110),
        SizedBox(height: Espaces.xl),
        Row(
          children: [
            Expanded(child: Squelette(hauteur: 76, rayon: Rayons.carte - 4)),
            SizedBox(width: Espaces.sm + 2),
            Expanded(child: Squelette(hauteur: 76, rayon: Rayons.carte - 4)),
          ],
        ),
      ],
    );
  }
}
