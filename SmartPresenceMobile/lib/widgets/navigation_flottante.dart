import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';

/// Un onglet de la barre flottante.
class Onglet {
  const Onglet({required this.icone, required this.iconeActive, required this.libelle});

  final IconData icone;
  final IconData iconeActive;
  final String libelle;
}

/// Barre de navigation flottante.
///
/// Elle ne colle pas au bas de l'écran : posée en retrait, sur un dégradé et une ombre
/// colorée, elle appartient à l'application plutôt qu'au système. C'est ce détail qui
/// sépare une interface soignée d'une interface par défaut.
class NavigationFlottante extends StatelessWidget {
  const NavigationFlottante({
    super.key,
    required this.onglets,
    required this.indexActif,
    required this.surSelection,
  });

  final List<Onglet> onglets;
  final int indexActif;
  final ValueChanged<int> surSelection;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        Espaces.xl,
        0,
        Espaces.xl,
        // Marge sous la barre système, sans jamais coller au bord.
        MediaQuery.of(context).padding.bottom + Espaces.md,
      ),
      child: Container(
        padding: const EdgeInsets.symmetric(
            horizontal: Espaces.sm, vertical: Espaces.md - 1),
        decoration: BoxDecoration(
          gradient: Couleurs.degradeNavigation,
          borderRadius: BorderRadius.circular(Rayons.bloc),
          boxShadow: Couleurs.ombreIndigo,
        ),
        child: Row(
          children: [
            for (var i = 0; i < onglets.length; i++)
              Expanded(child: _bouton(onglets[i], i == indexActif, i)),
          ],
        ),
      ),
    );
  }

  Widget _bouton(Onglet onglet, bool actif, int index) {
    final teinte = actif ? Colors.white : Colors.white.withValues(alpha: 0.52);

    return GestureDetector(
      onTap: () => surSelection(index),
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
        padding: const EdgeInsets.symmetric(vertical: Espaces.xs + 1),
        decoration: BoxDecoration(
          // Le fond de l'onglet actif remplace le point ou le trait habituels :
          // il tient sur une barre étroite sans encombrer.
          color: actif ? Colors.white.withValues(alpha: 0.16) : Colors.transparent,
          borderRadius: BorderRadius.circular(Rayons.tuile),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(actif ? onglet.iconeActive : onglet.icone, size: 21, color: teinte),
            const SizedBox(height: 3),
            Text(
              onglet.libelle,
              style: Typo.legende.copyWith(
                fontSize: 9.5,
                height: 1.1,
                color: teinte,
                fontWeight: actif ? FontWeight.w600 : FontWeight.w400,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}

/// Barre de navigation à bouton central surélevé.
///
/// Cinq emplacements de même largeur : deux onglets, le bouton d'empreinte, deux onglets.
/// La symétrie vient de là, pas d'un espace vide ajusté à l'œil. Tous les libellés
/// partagent la même ligne de base, celui du bouton central compris, et l'onglet actif
/// se reconnaît à sa pastille bleue et à son libellé en gras.
class NavigationCentrale extends StatelessWidget {
  const NavigationCentrale({
    super.key,
    required this.gauche,
    required this.droite,
    required this.centre,
    required this.indexActif,
    required this.indexCentre,
    required this.surSelection,
  });

  /// Onglets à gauche et à droite du bouton ; chacun porte l'index qu'il active.
  final List<(int, Onglet)> gauche;
  final List<(int, Onglet)> droite;
  final Onglet centre;
  final int indexActif;
  final int indexCentre;
  final ValueChanged<int> surSelection;

  static const double _hauteurBarre = 68;
  static const double _tailleBouton = 60;
  static const double _saillie = 22;

  @override
  Widget build(BuildContext context) {
    final centreActif = indexActif == indexCentre;
    final basSysteme = MediaQuery.of(context).padding.bottom;

    return Padding(
      padding: EdgeInsets.fromLTRB(Espaces.lg, 0, Espaces.lg, basSysteme + Espaces.sm),
      child: SizedBox(
        height: _hauteurBarre + _saillie,
        child: Stack(
          alignment: Alignment.bottomCenter,
          clipBehavior: Clip.none,
          children: [
            Container(
              height: _hauteurBarre,
              decoration: BoxDecoration(
                color: Couleurs.carte,
                borderRadius: BorderRadius.circular(Rayons.bloc + 4),
                border: Border.all(color: Couleurs.traitLeger),
                boxShadow: Couleurs.ombreFlottante,
              ),
              child: Row(
                children: [
                  for (final (index, onglet) in gauche) Expanded(child: _bouton(onglet, index == indexActif, index)),
                  Expanded(child: _libelleCentre(centreActif)),
                  for (final (index, onglet) in droite) Expanded(child: _bouton(onglet, index == indexActif, index)),
                ],
              ),
            ),
            Positioned(
              top: 0,
              child: Semantics(
                button: true,
                label: centre.libelle,
                child: GestureDetector(
                  onTap: () => surSelection(indexCentre),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 220),
                    width: _tailleBouton,
                    height: _tailleBouton,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: Couleurs.degradeEntete,
                      border: Border.all(color: Couleurs.fond, width: 4),
                      boxShadow: centreActif ? Couleurs.ombreRoyal : Couleurs.ombreCarte,
                    ),
                    child: Icon(
                      centreActif ? centre.iconeActive : centre.icone,
                      color: Colors.white,
                      size: 29,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Libellé du bouton central, posé sur la même ligne de base que ceux des onglets.
  Widget _libelleCentre(bool actif) {
    return GestureDetector(
      onTap: () => surSelection(indexCentre),
      behavior: HitTestBehavior.opaque,
      child: Align(
        alignment: Alignment.bottomCenter,
        child: Padding(
          padding: const EdgeInsets.only(bottom: 9),
          child: Text(
            centre.libelle,
            style: Typo.legende.copyWith(
              fontSize: 10,
              height: 1.1,
              fontWeight: actif ? FontWeight.w700 : FontWeight.w500,
              color: actif ? Couleurs.royal600 : Couleurs.encreDiscrete,
            ),
            maxLines: 1,
          ),
        ),
      ),
    );
  }

  Widget _bouton(Onglet onglet, bool actif, int index) {
    final teinte = actif ? Couleurs.royal600 : Couleurs.encreDiscrete;

    return Semantics(
      button: true,
      selected: actif,
      label: onglet.libelle,
      child: GestureDetector(
        onTap: () => surSelection(index),
        behavior: HitTestBehavior.opaque,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 220),
              curve: Curves.easeOut,
              width: 46,
              height: 28,
              decoration: BoxDecoration(
                color: actif ? Couleurs.royal50 : Colors.transparent,
                borderRadius: BorderRadius.circular(Rayons.pilule),
              ),
              child: Icon(actif ? onglet.iconeActive : onglet.icone, size: 22, color: teinte),
            ),
            const SizedBox(height: 3),
            Text(
              onglet.libelle,
              style: Typo.legende.copyWith(
                fontSize: 10,
                height: 1.1,
                color: teinte,
                fontWeight: actif ? FontWeight.w700 : FontWeight.w500,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}
