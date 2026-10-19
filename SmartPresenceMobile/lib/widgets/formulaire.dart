import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';

/// Ouvre un formulaire en feuille modale et renvoie ce qu'il a validé.
///
/// Même habillage que la saisie d'une note : poignée, coins arrondis, clavier pris en
/// compte. Centralisé pour que les formulaires de l'enseignant se ressemblent.
Future<T?> ouvrirFormulaire<T>(BuildContext context, WidgetBuilder construire) {
  return showModalBottomSheet<T>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Couleurs.carte,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(Rayons.bloc)),
    ),
    builder: construire,
  );
}

/// Corps standard d'une feuille de formulaire : poignée, titre, contenu défilant.
class CadreFormulaire extends StatelessWidget {
  const CadreFormulaire({
    super.key,
    required this.titre,
    required this.enfants,
    this.sousTitre,
  });

  final String titre;
  final String? sousTitre;
  final List<Widget> enfants;

  @override
  Widget build(BuildContext context) {
    final clavier = MediaQuery.of(context).viewInsets.bottom;
    return Padding(
      padding: EdgeInsets.only(bottom: clavier),
      child: SafeArea(
        top: false,
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(
              Espaces.xl, Espaces.md, Espaces.xl, Espaces.xl),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Center(
                child: Container(
                  width: 36,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Couleurs.trait,
                    borderRadius: BorderRadius.circular(Rayons.pilule),
                  ),
                ),
              ),
              const SizedBox(height: Espaces.xl),
              Text(titre, style: Typo.titreEcran.copyWith(fontSize: 19)),
              if (sousTitre != null) ...[
                const SizedBox(height: Espaces.xs),
                Text(sousTitre!, style: Typo.corpsAttenue),
              ],
              const SizedBox(height: Espaces.xl),
              ...enfants,
            ],
          ),
        ),
      ),
    );
  }
}

/// Étiquette de champ, avec la mention « facultatif » le cas échéant.
class EtiquetteChamp extends StatelessWidget {
  const EtiquetteChamp(this.texte, {super.key, this.facultatif = false});

  final String texte;
  final bool facultatif;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.baseline,
        textBaseline: TextBaseline.alphabetic,
        children: [
          Flexible(child: Text(texte, style: Typo.suretitre, overflow: TextOverflow.ellipsis)),
          if (facultatif) ...[
            const SizedBox(width: 6),
            Text('facultatif', style: Typo.legendePale),
          ],
        ],
      ),
    );
  }
}

/// Rangée Annuler / valider des formulaires.
class ActionsFormulaire extends StatelessWidget {
  const ActionsFormulaire({
    super.key,
    required this.libelle,
    required this.surValidation,
    required this.envoi,
  });

  final String libelle;

  /// `null` tant que le formulaire est incomplet.
  final VoidCallback? surValidation;
  final bool envoi;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: OutlinedButton(
            onPressed: envoi ? null : () => Navigator.pop(context),
            style: OutlinedButton.styleFrom(minimumSize: const Size(0, 48)),
            child: const Text('Annuler'),
          ),
        ),
        const SizedBox(width: Espaces.md),
        Expanded(
          flex: 2,
          child: FilledButton(
            onPressed: envoi ? null : surValidation,
            style: FilledButton.styleFrom(minimumSize: const Size(0, 48)),
            child: envoi
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : Text(libelle),
          ),
        ),
      ],
    );
  }
}
