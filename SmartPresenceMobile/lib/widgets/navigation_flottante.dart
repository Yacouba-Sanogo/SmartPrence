import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../core/preferences_app.dart';

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

/// Barre de navigation collée au bas de l'écran, avec un bouton central surélevé.
///
/// Elle occupe toute la largeur, coins hauts arrondis, comme sur la maquette : quatre onglets
/// de même largeur autour du bouton d'empreinte, qui dépasse de la barre. L'onglet actif se
/// reconnaît à sa couleur, à son libellé en gras et au trait bleu sous lui. Le bouton central
/// n'a pas de libellé : son icône suffit, et un libellé de plus alourdirait la barre.
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

  static const double _hauteurBarre = 60;
  static const double _tailleBouton = 58;
  static const double _saillie = 24;

  @override
  Widget build(BuildContext context) {
    final centreActif = indexActif == indexCentre;
    final basSysteme = MediaQuery.of(context).padding.bottom;
    final sombre = ContexteApp.maybeOf(context)?.sombre == true;

    // La barre garde sa taille : un texte très agrandi ne doit pas faire chevaucher les libellés.
    return MediaQuery(
      data: MediaQuery.of(context).copyWith(textScaler: MediaQuery.textScalerOf(context).clamp(maxScaleFactor: 1.1)),
      child: SizedBox(
        height: _hauteurBarre + _saillie + basSysteme,
        child: Stack(
          alignment: Alignment.bottomCenter,
          clipBehavior: Clip.none,
          children: [
            Container(
              height: _hauteurBarre + basSysteme,
              padding: EdgeInsets.only(bottom: basSysteme),
              decoration: BoxDecoration(
                color: Couleurs.carte,
                borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
                boxShadow: const [BoxShadow(color: Color(0x1A0F1729), blurRadius: 18, offset: Offset(0, -4))],
              ),
              child: Row(
                children: [
                  for (final (index, onglet) in gauche) Expanded(child: _bouton(context, onglet, index == indexActif, index)),
                  const Expanded(child: SizedBox()),
                  for (final (index, onglet) in droite) Expanded(child: _bouton(context, onglet, index == indexActif, index)),
                ],
              ),
            ),
            Positioned(
              top: 0,
              child: Semantics(
                button: true,
                selected: centreActif,
                label: centre.libelle,
                child: GestureDetector(
                  onTap: () {
                    ContexteApp.maybeOf(context)?.preferences.vibrer();
                    surSelection(indexCentre);
                  },
                  child: ProtegeCouleurs(
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 220),
                      width: _tailleBouton + 10,
                      height: _tailleBouton + 10,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: sombre ? const Color(0xFF05070C) : Couleurs.carte,
                        boxShadow: const [BoxShadow(color: Color(0x1F0F1729), blurRadius: 10, offset: Offset(0, -2))],
                      ),
                      alignment: Alignment.center,
                      child: Container(
                        width: _tailleBouton,
                        height: _tailleBouton,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          gradient: Couleurs.degradeEntete,
                          boxShadow: centreActif ? Couleurs.ombreRoyal : Couleurs.ombreCarte,
                        ),
                        child: Icon(
                          centreActif ? centre.iconeActive : centre.icone,
                          color: Colors.white,
                          size: 30,
                        ),
                      ),
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

  Widget _bouton(BuildContext context, Onglet onglet, bool actif, int index) {
    final teinte = actif ? Couleurs.royal600 : Couleurs.encreDiscrete;

    return Semantics(
      button: true,
      selected: actif,
      label: onglet.libelle,
      child: GestureDetector(
        onTap: () {
          ContexteApp.maybeOf(context)?.preferences.vibrer();
          surSelection(index);
        },
        behavior: HitTestBehavior.opaque,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(actif ? onglet.iconeActive : onglet.icone, size: 24, color: teinte),
            const SizedBox(height: 2),
            Text(
              onglet.libelle,
              style: Typo.legende.copyWith(
                fontSize: 10.5,
                height: 1.1,
                color: teinte,
                fontWeight: actif ? FontWeight.w700 : FontWeight.w500,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 4),
            AnimatedContainer(
              duration: const Duration(milliseconds: 220),
              curve: Curves.easeOut,
              width: actif ? 26 : 0,
              height: 3,
              decoration: BoxDecoration(color: Couleurs.royal600, borderRadius: BorderRadius.circular(2)),
            ),
          ],
        ),
      ),
    );
  }
}
