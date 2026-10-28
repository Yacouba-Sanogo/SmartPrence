import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

/// Rayonnages de bibliothèque dessinés en code : rangées de livres, planches, lumière douce.
///
/// Aucune image à embarquer ni à recadrer : le dessin suit la taille de l'en-tête, reste net
/// à toute densité d'écran et se décline dans les bleus de la charte. Le tirage des livres est
/// fixé (graine constante) : l'étagère ne change pas à chaque affichage.
class FondRayonnages extends StatelessWidget {
  const FondRayonnages({super.key});

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        const CustomPaint(painter: _PeintreRayonnages()),
        // Même voile que la photo de l'école : opaque sous le titre, ouvert à droite.
        DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.centerLeft,
              end: Alignment.centerRight,
              colors: [
                Couleurs.royal900.withValues(alpha: 0.94),
                Couleurs.royal800.withValues(alpha: 0.70),
                Couleurs.royal700.withValues(alpha: 0.12),
              ],
              stops: const [0.0, 0.48, 1.0],
            ),
          ),
        ),
      ],
    );
  }
}

class _PeintreRayonnages extends CustomPainter {
  const _PeintreRayonnages();

  static const List<Color> _dos = [
    Color(0xFF5C81DD),
    Color(0xFF2E5BD8),
    Color(0xFFA8C0F1),
    Color(0xFFE4ECFC),
    Color(0xFF8FA8D8),
    Color(0xFFD9B26A),
    Color(0xFF3F6BE0),
    Color(0xFFC9D8F7),
  ];

  @override
  void paint(Canvas canvas, Size taille) {
    final l = taille.width;
    final h = taille.height;

    // Fond de meuble.
    canvas.drawRect(
      Offset.zero & taille,
      Paint()
        ..shader = const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Couleurs.royal900, Couleurs.royal800],
        ).createShader(Offset.zero & taille),
    );

    final hasard = math.Random(7);
    final rangees = math.max(2, (h / 78).round());
    final hauteurRangee = h / rangees;
    const planche = 6.0;

    for (var r = 0; r < rangees; r++) {
      final bas = (r + 1) * hauteurRangee;
      var x = -4.0 + hasard.nextDouble() * 6;
      while (x < l) {
        // Un livre couché de temps en temps, comme sur une vraie étagère.
        if (hasard.nextDouble() < 0.08 && x > 30) {
          final pile = 2 + hasard.nextInt(2);
          final largeur = 34 + hasard.nextDouble() * 12;
          var y = bas - planche;
          for (var i = 0; i < pile; i++) {
            final epaisseur = 7 + hasard.nextDouble() * 4;
            y -= epaisseur;
            _livre(canvas, Rect.fromLTWH(x, y, largeur, epaisseur), _dos[hasard.nextInt(_dos.length)], couche: true);
          }
          x += largeur + 3;
          continue;
        }

        final largeur = 8 + hasard.nextDouble() * 9;
        final haut = (hauteurRangee - planche - 8) * (0.58 + hasard.nextDouble() * 0.38);
        final couleur = _dos[hasard.nextInt(_dos.length)];
        final penche = hasard.nextDouble() < 0.05;
        final rect = Rect.fromLTWH(x, bas - planche - haut, largeur, haut);
        if (penche) {
          canvas.save();
          canvas.translate(rect.left + rect.width / 2, rect.bottom);
          canvas.rotate(-0.12);
          canvas.translate(-(rect.left + rect.width / 2), -rect.bottom);
          _livre(canvas, rect, couleur);
          canvas.restore();
          x += largeur + 8;
        } else {
          _livre(canvas, rect, couleur);
          x += largeur + 0.6;
        }
        // Un vide de temps en temps : l'étagère respire.
        if (hasard.nextDouble() < 0.025) x += 14 + hasard.nextDouble() * 20;
      }

      // Planche, avec son ombre portée sur la rangée du dessous.
      final rectPlanche = Rect.fromLTWH(0, bas - planche, l, planche);
      canvas.drawRect(
        rectPlanche,
        Paint()
          ..shader = const LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF3A57A8), Color(0xFF14296B)],
          ).createShader(rectPlanche),
      );
      canvas.drawRect(
        Rect.fromLTWH(0, bas, l, 7),
        Paint()
          ..shader = LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Colors.black.withValues(alpha: 0.28), Colors.transparent],
          ).createShader(Rect.fromLTWH(0, bas, l, 7)),
      );
    }

    // Lumière chaude d'une lampe, en haut à droite.
    final halo = Rect.fromCircle(center: Offset(l * 0.86, h * 0.1), radius: l * 0.55);
    canvas.drawRect(
      Offset.zero & taille,
      Paint()
        ..shader = RadialGradient(
          colors: [const Color(0xFFFFE9B8).withValues(alpha: 0.30), Colors.transparent],
        ).createShader(halo),
    );
  }

  /// Un livre : dos coloré, léger dégradé de volume, filets et cartouche de titre.
  void _livre(Canvas canvas, Rect r, Color couleur, {bool couche = false}) {
    final forme = RRect.fromRectAndRadius(r, const Radius.circular(1.2));
    canvas.drawRRect(
      forme,
      Paint()
        ..shader = LinearGradient(
          begin: couche ? Alignment.topCenter : Alignment.centerLeft,
          end: couche ? Alignment.bottomCenter : Alignment.centerRight,
          colors: [
            Color.lerp(couleur, Colors.white, 0.16)!,
            couleur,
            Color.lerp(couleur, Colors.black, 0.28)!,
          ],
        ).createShader(r),
    );
    final filet = Paint()
      ..color = Colors.white.withValues(alpha: 0.35)
      ..strokeWidth = 0.8;
    if (couche) {
      canvas.drawLine(Offset(r.left + 3, r.center.dy), Offset(r.right - 3, r.center.dy), filet);
    } else {
      canvas.drawLine(Offset(r.left + 1.5, r.top + r.height * 0.14), Offset(r.right - 1.5, r.top + r.height * 0.14), filet);
      canvas.drawLine(Offset(r.left + 1.5, r.bottom - r.height * 0.14), Offset(r.right - 1.5, r.bottom - r.height * 0.14), filet);
      if (r.width > 11 && r.height > 34) {
        canvas.drawRect(
          Rect.fromCenter(center: Offset(r.center.dx, r.top + r.height * 0.42), width: r.width * 0.55, height: r.height * 0.22),
          Paint()..color = Colors.white.withValues(alpha: 0.28),
        );
      }
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter ancien) => false;
}
