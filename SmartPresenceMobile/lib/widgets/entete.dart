import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';

/// En-tête dégradé qui ouvre chaque écran.
///
/// La zone colorée descend plus bas que son contenu : le premier bloc de la page vient
/// la chevaucher. Ce recouvrement crée la profondeur qu'un empilement de cartes blanches
/// sur fond gris ne donne jamais.
class EnteteDegrade extends StatelessWidget {
  const EnteteDegrade({
    super.key,
    required this.enfant,
    this.debordement = 44,
    this.padding,
    this.degrade,
  });

  final Widget enfant;

  /// Hauteur de dégradé ajoutée sous le contenu, destinée à être recouverte.
  final double debordement;

  final EdgeInsetsGeometry? padding;
  final LinearGradient? degrade;

  @override
  Widget build(BuildContext context) {
    final hautStatut = MediaQuery.of(context).padding.top;

    return Container(
      decoration: BoxDecoration(gradient: degrade ?? Couleurs.degradeEntete),
      child: Stack(
        clipBehavior: Clip.hardEdge,
        children: [
          // Disques translucides : ils cassent l'aplat sans introduire de motif, et
          // restent lisibles sur toutes les tailles d'écran.
          Positioned(
            top: -56,
            right: -38,
            child: _disque(150, 0.07),
          ),
          Positioned(
            top: hautStatut + 44,
            right: 52,
            child: _disque(92, 0.05),
          ),
          Padding(
            padding: (padding ??
                    const EdgeInsets.fromLTRB(
                        Espaces.lg, Espaces.lg, Espaces.lg, Espaces.lg))
                .add(EdgeInsets.only(top: hautStatut, bottom: debordement)),
            child: enfant,
          ),
        ],
      ),
    );
  }

  Widget _disque(double taille, double opacite) => Container(
        width: taille,
        height: taille,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: Colors.white.withValues(alpha: opacite),
        ),
      );
}

/// Ligne d'identité d'un en-tête : avatar, salutation, action à droite.
class LigneIdentite extends StatelessWidget {
  const LigneIdentite({
    super.key,
    required this.initiales,
    required this.titre,
    this.surtitre,
    this.action,
    this.surTapAvatar,
  });

  final String initiales;
  final String titre;
  final String? surtitre;
  final Widget? action;
  final VoidCallback? surTapAvatar;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        GestureDetector(
          onTap: surTapAvatar,
          child: Container(
            width: 44,
            height: 44,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.18),
              borderRadius: BorderRadius.circular(Rayons.md),
            ),
            child: Text(
              initiales,
              style: Typo.libelle.copyWith(
                  color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14),
            ),
          ),
        ),
        const SizedBox(width: Espaces.md),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              if (surtitre != null)
                Text(surtitre!,
                    style: Typo.legende.copyWith(
                        color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5)),
              Text(
                titre,
                style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 18),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
        if (action != null) action!,
      ],
    );
  }
}

/// Bandeau translucide posé dans un en-tête dégradé.
class BandeauEntete extends StatelessWidget {
  const BandeauEntete({
    super.key,
    required this.icone,
    required this.texte,
    this.pastille,
  });

  final IconData icone;
  final String texte;
  final Widget? pastille;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
          horizontal: Espaces.md + 1, vertical: Espaces.sm + 2),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(Rayons.md),
      ),
      child: Row(
        children: [
          Icon(icone, size: 16, color: Colors.white),
          const SizedBox(width: Espaces.sm + 1),
          Expanded(
            child: Text(
              texte,
              style: Typo.legende.copyWith(color: Colors.white, fontSize: 12),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          if (pastille != null) pastille!,
        ],
      ),
    );
  }
}

/// Bouton d'icône sur fond dégradé.
class ActionEntete extends StatelessWidget {
  const ActionEntete({super.key, required this.icone, this.surTap, this.pastille = false});

  final IconData icone;
  final VoidCallback? surTap;

  /// Point d'attention affiché en haut à droite.
  final bool pastille;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: surTap,
      behavior: HitTestBehavior.opaque,
      child: SizedBox(
        width: 38,
        height: 38,
        child: Stack(
          alignment: Alignment.center,
          children: [
            Icon(icone, size: 21, color: Colors.white.withValues(alpha: 0.92)),
            if (pastille)
              Positioned(
                top: 6,
                right: 6,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: Couleurs.dangerVif,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
