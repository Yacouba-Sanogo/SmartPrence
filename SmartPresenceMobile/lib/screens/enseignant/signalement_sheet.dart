import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/seance.dart';
import '../../models/signalement.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';

/// Longueur maximale acceptée par le serveur — rappelée ici pour que la coupure se
/// voie pendant la saisie plutôt qu'au refus de l'envoi.
const int _maxDescription = 500;

/// Ouvre le formulaire de signalement et renvoie `true` si une anomalie a été déposée.
Future<bool> ouvrirSignalement(
  BuildContext context, {
  required EnseignantService service,
  required String seanceId,
  required List<LigneFeuille> etudiants,
  LigneFeuille? etudiantPreselectionne,
}) async {
  final depose = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Couleurs.carte,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(Rayons.carte + 4)),
    ),
    builder: (_) => _FormulaireSignalement(
      service: service,
      seanceId: seanceId,
      etudiants: etudiants,
      etudiantPreselectionne: etudiantPreselectionne,
    ),
  );
  return depose ?? false;
}

class _FormulaireSignalement extends StatefulWidget {
  const _FormulaireSignalement({
    required this.service,
    required this.seanceId,
    required this.etudiants,
    this.etudiantPreselectionne,
  });

  final EnseignantService service;
  final String seanceId;
  final List<LigneFeuille> etudiants;
  final LigneFeuille? etudiantPreselectionne;

  @override
  State<_FormulaireSignalement> createState() => _FormulaireSignalementState();
}

class _FormulaireSignalementState extends State<_FormulaireSignalement> {
  final _description = TextEditingController();

  late TypeSignalement _type = widget.etudiantPreselectionne != null
      ? TypeSignalement.etudiantNonReconnu
      : TypeSignalement.lecteurDefaillant;

  late LigneFeuille? _etudiant = widget.etudiantPreselectionne;

  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() {
    _description.dispose();
    super.dispose();
  }

  bool get _valide =>
      _description.text.trim().isNotEmpty &&
      (!_type.viseUnEtudiant || _etudiant != null);

  Future<void> _envoyer() async {
    if (!_valide || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });

    try {
      await widget.service.signaler(
        seanceId: widget.seanceId,
        type: _type,
        description: _description.text,
        etudiantId: _type.viseUnEtudiant ? _etudiant!.etudiantId : null,
      );
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _envoi = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    // Le clavier recouvrirait le bouton d'envoi sans ce décalage.
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

              Text('Signaler une anomalie', style: Typo.titreEcran.copyWith(fontSize: 19)),
              const SizedBox(height: Espaces.xs),
              Text(
                'Le relevé ne sera pas modifié : votre signalement part à la scolarité, '
                'qui décidera de la suite.',
                style: Typo.corpsAttenue,
              ),
              const SizedBox(height: Espaces.xl),

              Text('Nature', style: Typo.suretitre),
              const SizedBox(height: Espaces.sm),
              ...TypeSignalement.values.map(_choixType),

              if (_type.viseUnEtudiant) ...[
                const SizedBox(height: Espaces.lg),
                Text('Étudiant concerné', style: Typo.suretitre),
                const SizedBox(height: Espaces.sm),
                _selecteurEtudiant(),
              ],

              const SizedBox(height: Espaces.lg),
              Text('Ce que vous avez constaté', style: Typo.suretitre),
              const SizedBox(height: Espaces.sm),
              TextField(
                controller: _description,
                maxLines: 4,
                maxLength: _maxDescription,
                textCapitalization: TextCapitalization.sentences,
                onChanged: (_) => setState(() {}),
                decoration: const InputDecoration(
                  hintText: 'Exemple : présent au premier rang, le lecteur a refusé '
                      'son doigt après trois essais.',
                ),
              ),

              if (_erreur != null) ...[
                const SizedBox(height: Espaces.sm),
                Bandeau.erreur(message: _erreur!),
              ],

              const SizedBox(height: Espaces.md),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: _envoi ? null : () => Navigator.pop(context, false),
                      style: OutlinedButton.styleFrom(minimumSize: const Size(0, 48)),
                      child: const Text('Annuler'),
                    ),
                  ),
                  const SizedBox(width: Espaces.md),
                  Expanded(
                    flex: 2,
                    child: FilledButton(
                      onPressed: _valide && !_envoi ? _envoyer : null,
                      style: FilledButton.styleFrom(minimumSize: const Size(0, 48)),
                      child: _envoi
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2, color: Colors.white),
                            )
                          : const Text('Transmettre'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _choixType(TypeSignalement type) {
    final choisi = _type == type;
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm),
      child: Material(
        color: choisi ? Couleurs.royal50 : Couleurs.carte,
        borderRadius: BorderRadius.circular(Rayons.md),
        child: InkWell(
          borderRadius: BorderRadius.circular(Rayons.md),
          onTap: _envoi
              ? null
              : () => setState(() {
                    _type = type;
                    // Changer de nature ne doit pas laisser traîner un étudiant
                    // désigné sur un signalement qui ne vise plus personne.
                    if (!type.viseUnEtudiant) _etudiant = null;
                  }),
          child: Ink(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(Rayons.md),
              border: Border.all(
                color: choisi ? Couleurs.royal400 : Couleurs.trait,
                width: choisi ? 1.5 : 1,
              ),
            ),
            padding: const EdgeInsets.all(Espaces.md),
            child: Row(
              children: [
                Icon(type.icone,
                    size: 19,
                    color: choisi ? Couleurs.royal600 : Couleurs.encreDiscrete),
                const SizedBox(width: Espaces.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(type.libelle,
                          style: Typo.libelle.copyWith(
                              color: choisi ? Couleurs.royal700 : Couleurs.encre)),
                      const SizedBox(height: 2),
                      Text(type.explication, style: Typo.legende),
                    ],
                  ),
                ),
                if (choisi)
                  const Icon(Icons.check_circle_rounded,
                      size: 19, color: Couleurs.royal600),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _selecteurEtudiant() {
    if (widget.etudiants.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(Espaces.md),
        decoration: BoxDecoration(
          color: Couleurs.traitPale,
          borderRadius: BorderRadius.circular(Rayons.md),
        ),
        child: Text(
          'Tous les étudiants de la séance ont déjà un relevé : il n’y a personne à '
          'signaler comme non reconnu.',
          style: Typo.legende,
        ),
      );
    }

    return DropdownButtonFormField<String>(
      initialValue: _etudiant?.etudiantId,
      isExpanded: true,
      hint: const Text('Choisir un étudiant'),
      items: widget.etudiants
          .map((ligne) => DropdownMenuItem(
                value: ligne.etudiantId,
                child: Row(
                  children: [
                    Expanded(
                      child: Text(ligne.nomComplet,
                          style: Typo.corps,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis),
                    ),
                    const SizedBox(width: Espaces.sm),
                    Text(ligne.matricule,
                        style: Typo.mono(12, couleur: Couleurs.encrePale)),
                  ],
                ),
              ))
          .toList(),
      onChanged: _envoi
          ? null
          : (id) => setState(() => _etudiant =
              widget.etudiants.where((l) => l.etudiantId == id).firstOrNull),
    );
  }
}
