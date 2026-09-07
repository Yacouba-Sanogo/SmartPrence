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
