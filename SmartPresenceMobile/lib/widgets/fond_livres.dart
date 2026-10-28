import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

/// Fond de la bannière « prochaine séance » : une pile de livres et un ordinateur portable.
///
/// Dessiné en code, dans les bleus de la charte, à droite de la bannière ; le texte reste sur
/// la partie sombre, à gauche.
class FondLivresOrdinateur extends StatelessWidget {
  const FondLivresOrdinateur({super.key});

  @override
  Widget build(BuildContext context) {
    return const CustomPaint(painter: _PeintreLivres());
  }
}

class _PeintreLivres extends CustomPainter {
  const _PeintreLivres();

  @override
  void paint(Canvas canvas, Size taille) {
    final l = taille.width;
    final h = taille.height;

    // Halo bleu clair derrière la scène.
    final halo = Rect.fromCircle(center: Offset(l * 0.82, h * 0.62), radius: h * 1.1);
    canvas.drawRect(
      Offset.zero & taille,
      Paint()
        ..shader = RadialGradient(colors: [Couleurs.royal400.withValues(alpha: 0.42), Colors.transparent]).createShader(halo),
    );

    // Ordinateur portable : écran incliné et socle.
    final ecran = Path()
      ..moveTo(l * 0.80, h * 0.10)
      ..lineTo(l * 0.97, h * 0.04)
      ..lineTo(l * 1.01, h * 0.62)
      ..lineTo(l * 0.84, h * 0.70)
      ..close();
    canvas.drawPath(
      ecran,
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Couleurs.royal300.withValues(alpha: 0.38), Couleurs.royal500.withValues(alpha: 0.22)],
        ).createShader(Rect.fromLTWH(l * 0.78, 0, l * 0.25, h)),
    );
    canvas.drawPath(
      ecran,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4
        ..color = Colors.white.withValues(alpha: 0.28),
    );
    final socle = Path()
      ..moveTo(l * 0.62, h * 0.82)
      ..lineTo(l * 0.84, h * 0.70)
      ..lineTo(l * 1.01, h * 0.62)
      ..lineTo(l * 1.01, h * 0.74)
      ..lineTo(l * 0.80, h * 0.92)
      ..close();
    canvas.drawPath(socle, Paint()..color = Couleurs.royal200.withValues(alpha: 0.22));

    // Pile de livres devant l'ordinateur.
    const couleurs = [Color(0xFF8FA8D8), Color(0xFF5C81DD), Color(0xFFC9D8F7), Color(0xFF3F6BE0)];
    final base = h * 0.98;
    final x0 = l * 0.70;
    for (var i = 0; i < 4; i++) {
      final epaisseur = h * 0.15;
      final haut = base - (i + 1) * epaisseur;
      final largeur = l * (0.25 - (i % 2) * 0.03);
      final decalage = (i % 2 == 0 ? 0.0 : l * 0.015) + (i == 3 ? l * 0.02 : 0);
      final r = RRect.fromRectAndRadius(Rect.fromLTWH(x0 + decalage, haut, largeur, epaisseur - 1.5), const Radius.circular(2));
      canvas.drawRRect(
        r,
        Paint()
          ..shader = LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color.lerp(couleurs[i], Colors.white, 0.25)!.withValues(alpha: 0.85), couleurs[i].withValues(alpha: 0.85)],
          ).createShader(r.outerRect),
      );
      // Tranche des pages, côté droit.
      canvas.drawRect(
        Rect.fromLTWH(r.right - largeur * 0.78, haut + 2, largeur * 0.74, epaisseur - 5),
        Paint()..color = Colors.white.withValues(alpha: 0.18),
      );
    }

    // Voile de lisibilité : opaque sous le texte, ouvert à droite.
    canvas.drawRect(
      Offset.zero & taille,
      Paint()
        ..shader = LinearGradient(
          colors: [Couleurs.royal900.withValues(alpha: 0.80), Colors.transparent],
          stops: const [0.55, 0.88],
        ).createShader(Offset.zero & taille),
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter ancien) => false;
}
