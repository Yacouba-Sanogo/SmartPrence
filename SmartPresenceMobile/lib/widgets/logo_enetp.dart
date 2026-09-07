import 'package:flutter/material.dart';

/// Logo officiel de l'ENETP.
///
/// C'est le fichier fourni par l'établissement, affiché tel quel. Une version
/// redessinée au [CustomPainter] l'a précédé ici : nette à toute taille, mais ce
/// n'était pas le logo de l'école, et une identité ne se réinterprète pas.
///
/// Le disque argenté fait partie du dessin et n'a pas été détouré — le lettrage
/// porte un liseré blanc conçu pour ressortir dessus, et l'isoler laisserait des
/// lettres cernées de blanc flottant sur le fond. Sur un fond sombre ou coloré, on
/// pose donc le logo sur une [plaque] claire plutôt que de retoucher l'original.
class LogoEnetp extends StatelessWidget {
  const LogoEnetp({super.key, this.taille = 104, this.plaque = false});

  /// Côté du logo en pixels logiques. Le fichier est carré.
  final double taille;

  /// Pose le logo sur un disque blanc, indispensable dès que le fond n'est pas clair.
  final bool plaque;

  @override
  Widget build(BuildContext context) {
    final image = Image.asset(
      'assets/images/logo-enetp.png',
      width: taille,
      height: taille,
      fit: BoxFit.contain,
      filterQuality: FilterQuality.medium,
      semanticLabel: "Logo de l'ENETP — Qui maîtrise enseigne",
    );

    if (!plaque) return image;

    return Container(
      width: taille * 1.18,
      height: taille * 1.18,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: Colors.white,
        boxShadow: const [
          BoxShadow(
            color: Color(0x1F1B1464),
            blurRadius: 18,
            offset: Offset(0, 6),
          ),
        ],
      ),
      child: image,
    );
  }
}
