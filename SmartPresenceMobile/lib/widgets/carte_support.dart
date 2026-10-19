import 'package:flutter/material.dart';

import '../core/dates.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../models/gestion.dart';
import 'communs.dart';

/// Un support de cours : pictogramme du format, titre, matière, taille.
///
/// Partagée par l'enseignant (qui gère ses supports) et l'étudiant (qui les télécharge) :
/// un même document doit se reconnaître d'un côté comme de l'autre.
class CarteSupport extends StatelessWidget {
  const CarteSupport({
    super.key,
    required this.support,
    required this.surTap,
    this.enCours = false,
    this.action,
    this.afficherAuteur = false,
  });

  final SupportCours support;
  final VoidCallback? surTap;

  /// Téléchargement en cours : l'icône cède la place à un indicateur.
  final bool enCours;

  /// Bouton secondaire, par exemple la suppression côté enseignant.
  final Widget? action;
  final bool afficherAuteur;

  @override
  Widget build(BuildContext context) {
    final (icone, teinte) = support.apparence;

    return Carte(
      surTap: enCours ? null : surTap,
      padding: const EdgeInsets.fromLTRB(Espaces.md + 2, Espaces.md + 2, Espaces.sm, Espaces.md + 2),
      enfant: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              color: teinte.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(Rayons.md),
            ),
            child: enCours
                ? Padding(
                    padding: const EdgeInsets.all(13),
                    child: CircularProgressIndicator(strokeWidth: 2.2, color: teinte),
                  )
                : Icon(icone, size: 24, color: teinte),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(support.titre,
                    style: Typo.libelle.copyWith(fontSize: 14),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
                const SizedBox(height: 3),
                Text(
                  [
                    support.matiere,
                    if (afficherAuteur) support.enseignantNom,
                  ].where((t) => t.isNotEmpty).join(' · '),
                  style: Typo.legendePale,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  '${support.extension.toUpperCase()} · ${support.tailleLisible} · ${Dates.courte(support.publieLe)}',
                  style: Typo.mono(10.5, couleur: Couleurs.encrePale),
                ),
                if (support.description != null) ...[
                  const SizedBox(height: 4),
                  Text(support.description!,
                      style: Typo.corpsAttenue.copyWith(fontSize: 12.5),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis),
                ],
              ],
            ),
          ),
          if (action != null)
            action!
          else
            const Padding(
              padding: EdgeInsets.all(Espaces.sm),
              child: Icon(Icons.download_rounded, size: 21, color: Couleurs.indigo600),
            ),
        ],
      ),
    );
  }
}
