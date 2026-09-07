import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/api/erreurs.dart';
import '../../core/dates.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/etudiant_inscrit.dart';
import '../../models/note.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';

/// Ouvre la saisie d'une note et renvoie `true` si elle a été enregistrée.
Future<bool> ouvrirSaisieNote(
  BuildContext context, {
  required EnseignantService service,
  required List<EtudiantInscrit> etudiants,
  required List<MatiereBreve> matieres,
  EtudiantInscrit? etudiantPreselectionne,
  Note? noteExistante,
}) async {
  final enregistre = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Couleurs.carte,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(Rayons.bloc)),
    ),
    builder: (_) => _FormulaireNote(
      service: service,
      etudiants: etudiants,
      matieres: matieres,
      etudiantPreselectionne: etudiantPreselectionne,
      noteExistante: noteExistante,
    ),
  );
  return enregistre ?? false;
}

class _FormulaireNote extends StatefulWidget {
  const _FormulaireNote({
    required this.service,
    required this.etudiants,
    required this.matieres,
    this.etudiantPreselectionne,
    this.noteExistante,
  });

  final EnseignantService service;
  final List<EtudiantInscrit> etudiants;
  final List<MatiereBreve> matieres;
  final EtudiantInscrit? etudiantPreselectionne;
  final Note? noteExistante;

  @override
  State<_FormulaireNote> createState() => _FormulaireNoteState();
}

class _FormulaireNoteState extends State<_FormulaireNote> {
  late final TextEditingController _valeur;
  late final TextEditingController _libelle;
  late final TextEditingController _appreciation;

  String? _etudiantId;
  int? _matiereId;
  int _coefficient = 1;
  TypeEvaluation _type = TypeEvaluation.devoir;
  PeriodeScolaire _periode = PeriodeScolaire.semestre1;
  DateTime _date = DateTime.now();

  bool _envoi = false;
  String? _erreur;

  bool get _modification => widget.noteExistante != null;

  @override
  void initState() {
    super.initState();
    final note = widget.noteExistante;

    _valeur = TextEditingController(
        text: note == null ? '' : note.valeurFormatee.replaceAll(',', '.'));
    _libelle = TextEditingController(text: note?.libelle ?? '');
    _appreciation = TextEditingController(text: note?.appreciation ?? '');

    _etudiantId = note?.etudiantId ?? widget.etudiantPreselectionne?.id;
    _matiereId = note?.matiereId ??
        (widget.matieres.isEmpty ? null : widget.matieres.first.id);
    _coefficient = note?.coefficient ?? 1;
    _type = note?.type ?? TypeEvaluation.devoir;
    _date = note?.dateEvaluation ?? DateTime.now();
  }

  @override
  void dispose() {
    _valeur.dispose();
    _libelle.dispose();
    _appreciation.dispose();
    super.dispose();
  }

  /// Valeur saisie, ou `null` si elle n'est pas une note recevable.
  double? get _valeurNumerique {
    final brut = _valeur.text.trim().replaceAll(',', '.');
    final valeur = double.tryParse(brut);
    if (valeur == null || valeur < 0 || valeur > 20) return null;
    return valeur;
  }

  bool get _valide =>
      _etudiantId != null &&
      _matiereId != null &&
      _valeurNumerique != null &&
      _libelle.text.trim().isNotEmpty;

  Future<void> _enregistrer() async {
    if (!_valide || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });

    final saisie = SaisieNote(
      etudiantId: _etudiantId!,
      matiereId: _matiereId!,
      valeur: _valeurNumerique!,
      coefficient: _coefficient,
      type: _type,
      periode: _periode,
      libelle: _libelle.text,
      dateEvaluation: _date,
      appreciation: _appreciation.text,
    );

    try {
      if (_modification) {
        await widget.service.modifierNote(widget.noteExistante!.id, saisie);
      } else {
        await widget.service.saisirNote(saisie);
      }
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ErreurApi catch (e) {
      // Le serveur refuse notamment une classe qui n'est pas la sienne, ou la
      // note d'un collègue : son message est plus précis que tout contrôle local.
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _envoi = false;
      });
    }
  }

  Future<void> _choisirDate() async {
    final choisie = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(DateTime.now().year - 1),
      lastDate: DateTime.now().add(const Duration(days: 1)),
      locale: const Locale('fr'),
    );
    if (choisie != null) setState(() => _date = choisie);
  }

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

              Text(_modification ? 'Corriger la note' : 'Saisir une note',
                  style: Typo.titreEcran.copyWith(fontSize: 19)),
              const SizedBox(height: Espaces.xs),
              Text(
                'La note sera visible par l’étudiant dans son bulletin.',
                style: Typo.corpsAttenue,
              ),
              const SizedBox(height: Espaces.xl),

              if (_erreur != null) ...[
                Bandeau.erreur(message: _erreur!),
                const SizedBox(height: Espaces.md),
              ],

              _etiquette('Étudiant'),
              DropdownButtonFormField<String>(
                initialValue: _etudiantId,
                isExpanded: true,
                // Changer d'étudiant reviendrait à déplacer un résultat : le serveur
                // le refuse, autant ne pas le proposer.
                onChanged: _modification || _envoi
                    ? null
                    : (id) => setState(() => _etudiantId = id),
                hint: const Text('Choisir un étudiant'),
                items: widget.etudiants
                    .map((e) => DropdownMenuItem(
                          value: e.id,
                          child: Text(e.nomComplet,
                              style: Typo.corps,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis),
                        ))
                    .toList(),
              ),

              const SizedBox(height: Espaces.lg),
              _etiquette('Matière'),
              DropdownButtonFormField<int>(
                initialValue: _matiereId,
                isExpanded: true,
                onChanged: _envoi ? null : (id) => setState(() => _matiereId = id),
                items: widget.matieres
                    .map((m) => DropdownMenuItem(
                          value: m.id,
                          child: Text('${m.code} — ${m.libelle}',
                              style: Typo.corps,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis),
                        ))
                    .toList(),
              ),

              const SizedBox(height: Espaces.lg),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    flex: 3,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _etiquette('Note sur 20'),
                        TextField(
                          controller: _valeur,
                          keyboardType:
                              const TextInputType.numberWithOptions(decimal: true),
                          inputFormatters: [
                            FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]')),
                          ],
                          onChanged: (_) => setState(() {}),
                          decoration: const InputDecoration(hintText: '12,5'),
                        ),
                        if (_valeur.text.trim().isNotEmpty && _valeurNumerique == null)
                          Padding(
                            padding: const EdgeInsets.only(top: 4),
                            child: Text('Entre 0 et 20',
                                style: Typo.legende.copyWith(color: Couleurs.danger)),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(width: Espaces.md),
                  Expanded(
                    flex: 2,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _etiquette('Coefficient'),
                        _selecteurCoefficient(),
                      ],
                    ),
                  ),
                ],
              ),

              const SizedBox(height: Espaces.lg),
              _etiquette('Intitulé'),
              TextField(
                controller: _libelle,
                textCapitalization: TextCapitalization.sentences,
                onChanged: (_) => setState(() {}),
                decoration: const InputDecoration(hintText: 'Devoir n°1'),
              ),

              const SizedBox(height: Espaces.lg),
              _etiquette('Nature'),
              Wrap(
                spacing: Espaces.sm - 2,
                runSpacing: Espaces.sm - 2,
                children: TypeEvaluation.values.map(_puceType).toList(),
              ),

              const SizedBox(height: Espaces.lg),
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _etiquette('Date'),
                        GestureDetector(
                          onTap: _envoi ? null : _choisirDate,
                          child: Container(
                            height: 48,
                            padding: const EdgeInsets.symmetric(horizontal: Espaces.lg),
                            decoration: BoxDecoration(
                              border: Border.all(color: Couleurs.trait),
                              borderRadius: BorderRadius.circular(Rayons.tuile),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.calendar_today_rounded,
                                    size: 16, color: Couleurs.encreDiscrete),
                                const SizedBox(width: Espaces.sm),
                                Text(Dates.courte(_date), style: Typo.corps),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: Espaces.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _etiquette('Période'),
                        _selecteurPeriode(),
                      ],
                    ),
                  ),
                ],
              ),

              const SizedBox(height: Espaces.lg),
              _etiquette('Appréciation', facultatif: true),
              TextField(
                controller: _appreciation,
                maxLines: 2,
                maxLength: 500,
                textCapitalization: TextCapitalization.sentences,
                decoration: const InputDecoration(
                    hintText: 'Bon travail, raisonnement à détailler.'),
              ),

              const SizedBox(height: Espaces.sm),
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
                      onPressed: _valide && !_envoi ? _enregistrer : null,
                      style: FilledButton.styleFrom(minimumSize: const Size(0, 48)),
                      child: _envoi
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2, color: Colors.white),
                            )
                          : Text(_modification ? 'Enregistrer' : 'Ajouter la note'),
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

  Widget _etiquette(String texte, {bool facultatif = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.baseline,
        textBaseline: TextBaseline.alphabetic,
        children: [
          Text(texte, style: Typo.suretitre),
          if (facultatif) ...[
            const SizedBox(width: 6),
            Text('facultatif', style: Typo.legendePale),
          ],
        ],
      ),
    );
  }

  Widget _selecteurCoefficient() {
    return Container(
      height: 48,
      decoration: BoxDecoration(
        border: Border.all(color: Couleurs.trait),
        borderRadius: BorderRadius.circular(Rayons.tuile),
      ),
      child: Row(
        children: [
          IconButton(
            onPressed: _coefficient > 1
                ? () => setState(() => _coefficient--)
                : null,
            icon: const Icon(Icons.remove_rounded, size: 17),
            visualDensity: VisualDensity.compact,
          ),
          Expanded(
            child: Text('$_coefficient',
                textAlign: TextAlign.center,
                style: Typo.mono(14, graisse: FontWeight.w600)),
          ),
          IconButton(
            onPressed: _coefficient < 20
                ? () => setState(() => _coefficient++)
                : null,
            icon: const Icon(Icons.add_rounded, size: 17),
            visualDensity: VisualDensity.compact,
          ),
        ],
      ),
    );
  }

  Widget _selecteurPeriode() {
    return Container(
      height: 48,
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: Couleurs.traitPale,
        borderRadius: BorderRadius.circular(Rayons.tuile),
      ),
      child: Row(
        children: PeriodeScolaire.values.map((p) {
          final actif = _periode == p;
          return Expanded(
            child: GestureDetector(
              onTap: () => setState(() => _periode = p),
              child: Container(
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: actif ? Couleurs.carte : Colors.transparent,
                  borderRadius: BorderRadius.circular(Rayons.md - 2),
                  boxShadow: actif ? Couleurs.ombreCarte : null,
                ),
                child: Text(
                  p == PeriodeScolaire.semestre1 ? 'S1' : 'S2',
                  style: Typo.legende.copyWith(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: actif ? Couleurs.indigo700 : Couleurs.encreDiscrete,
                  ),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _puceType(TypeEvaluation type) {
    final actif = _type == type;
    return GestureDetector(
      onTap: () => setState(() => _type = type),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 7),
        decoration: BoxDecoration(
          color: actif ? Couleurs.indigo600 : Couleurs.carte,
          border: Border.all(color: actif ? Couleurs.indigo600 : Couleurs.trait),
          borderRadius: BorderRadius.circular(Rayons.pilule),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(type.icone,
                size: 13, color: actif ? Colors.white : Couleurs.encreDiscrete),
            const SizedBox(width: 5),
            Text(
              type.libelle,
              style: Typo.legende.copyWith(
                fontSize: 11.5,
                fontWeight: FontWeight.w500,
                color: actif ? Colors.white : Couleurs.encreAttenuee,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
