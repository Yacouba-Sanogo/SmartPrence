import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/enseignement.dart';
import '../../models/note.dart';
import '../../models/seance.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import '../../widgets/formulaire.dart';
import 'resultats_screen.dart';

/// Évaluations d'une classe : planifier une épreuve, fixer son barème, puis en saisir
/// les résultats.
class EvaluationsScreen extends StatefulWidget {
  const EvaluationsScreen({super.key, required this.auth, required this.classe});

  final AuthService auth;
  final ClasseEnseignee classe;

  @override
  State<EvaluationsScreen> createState() => _EvaluationsScreenState();
}

class _EvaluationsScreenState extends State<EvaluationsScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<Evaluation> _evaluations = const [];
  List<MatiereBreve> _matieres = const [];
  bool _chargement = true;
  String? _erreur;
  String? _erreurAction;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  Future<void> _charger() async {
    setState(() {
      _chargement = true;
      _erreur = null;
    });
    try {
      final r = await Future.wait([
        _service.evaluations(widget.classe.id),
        _service.matieres(),
      ]);
      if (!mounted) return;
      setState(() {
        _evaluations = r[0] as List<Evaluation>;
        _matieres = r[1] as List<MatiereBreve>;
        _chargement = false;
      });
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _chargement = false;
      });
    }
  }

  Future<void> _planifier({Evaluation? existante}) async {
    if (_matieres.isEmpty) {
      setState(() => _erreurAction =
          'Aucune matière active. La scolarité doit en créer avant toute évaluation.');
      return;
    }
    final enregistre = await ouvrirFormulaire<bool>(
      context,
      (_) => _FormulaireEvaluation(
        service: _service,
        classeId: widget.classe.id,
        matieres: _matieres,
        existante: existante,
      ),
    );
    if (enregistre == true && mounted) {
      setState(() => _erreurAction = null);
      _charger();
    }
  }

  Future<void> _ouvrir(Evaluation evaluation) async {
    await Navigator.of(context).push<void>(routeDouce<void>(
      (_) => ResultatsScreen(auth: widget.auth, evaluationId: evaluation.id),
    ));
    if (mounted) _charger();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      floatingActionButton: _chargement || _erreur != null
          ? null
          : FloatingActionButton.extended(
              onPressed: () => _planifier(),
              backgroundColor: Couleurs.indigo600,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.add_rounded, size: 20),
              label: const Text('Planifier'),
            ),
      body: Column(
        children: [
          EnteteDegrade(
            debordement: 30,
            padding: const EdgeInsets.fromLTRB(Espaces.sm, Espaces.sm, Espaces.lg, Espaces.md),
            enfant: Row(
              children: [
                ActionEntete(
                  icone: Icons.arrow_back_rounded,
                  surTap: () => Navigator.of(context).maybePop(),
                ),
                const SizedBox(width: Espaces.xs),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text('Évaluations',
                          style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19)),
                      const SizedBox(height: 2),
                      Text(
                          '${widget.classe.code} · ${_evaluations.length} épreuve'
                          '${_evaluations.length > 1 ? 's' : ''}',
                          style: Typo.legende.copyWith(
                              color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5)),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: Transform.translate(
              offset: const Offset(0, -24),
              child: RefreshIndicator(
                onRefresh: _charger,
                color: Couleurs.indigo600,
                child: _corps(),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _corps() {
    if (_chargement) {
      return const Center(child: CircularProgressIndicator(color: Couleurs.indigo600));
    }
    if (_erreur != null) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Évaluations indisponibles',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }
    if (_evaluations.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.quiz_outlined,
            titre: 'Aucune évaluation',
            detail: 'Planifiez un devoir ou un examen : vous y saisirez ensuite les résultats.',
          ),
        ],
      );
    }

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      children: [
        if (_erreurAction != null) ...[
          Bandeau.erreur(
              message: _erreurAction!, surFermeture: () => setState(() => _erreurAction = null)),
          const SizedBox(height: Espaces.md),
        ],
        ..._evaluations.asMap().entries.map(
            (e) => ApparitionEnCascade(rang: e.key, enfant: _carte(e.value))),
      ],
    );
  }

  Widget _carte(Evaluation e) {
    final saisis = e.notes + e.absents;
    final complet = e.effectif > 0 && saisis >= e.effectif;

    final Pastille etat;
    if (e.validees > 0 && e.brouillons == 0 && e.transmises == 0) {
      etat = const Pastille(
          texte: 'Validée', teinte: Couleurs.succes, fond: Couleurs.succesFond, compact: true);
    } else if (e.transmises > 0 && e.brouillons == 0) {
      etat = const Pastille(
          texte: 'Transmise', teinte: Couleurs.alerte, fond: Couleurs.alerteFond, compact: true);
    } else if (e.brouillons > 0) {
      etat = const Pastille(
          texte: 'Brouillon',
          teinte: Couleurs.encreAttenuee,
          fond: Couleurs.traitPale,
          compact: true);
    } else {
      etat = const Pastille(
          texte: 'À noter', teinte: Couleurs.indigo600, fond: Couleurs.royal50, compact: true);
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.md),
      child: Carte(
        surTap: () => _ouvrir(e),
        padding: const EdgeInsets.fromLTRB(Espaces.lg, Espaces.md + 2, Espaces.sm, Espaces.md + 2),
        enfant: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: Couleurs.royal50,
                borderRadius: BorderRadius.circular(Rayons.md),
              ),
              child: Icon(e.type.icone, size: 21, color: Couleurs.indigo600),
            ),
            const SizedBox(width: Espaces.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Flexible(
                        child: Text(e.libelle,
                            style: Typo.libelle.copyWith(fontSize: 14),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis),
                      ),
                      const SizedBox(width: Espaces.sm),
                      etat,
                    ],
                  ),
                  const SizedBox(height: 3),
                  Text(
                    '${e.matiereLibelle} · ${Dates.courte(e.date)} · sur ${e.bareme} · coef. ${e.coefficient}',
                    style: Typo.legendePale,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 3),
                  Text(
                    complet
                        ? '${e.notes} noté${e.notes > 1 ? 's' : ''}'
                            '${e.absents > 0 ? ' · ${e.absents} absent${e.absents > 1 ? 's' : ''}' : ''}'
                        : '$saisis / ${e.effectif} saisis',
                    style: Typo.legende.copyWith(
                        fontSize: 11.5,
                        color: complet ? Couleurs.succes : Couleurs.encreAttenuee),
                  ),
                  const SizedBox(height: 6),
                  BarreAnimee(
                    valeur: e.effectif == 0 ? 0 : saisis / e.effectif,
                    couleur: complet ? Couleurs.succesVif : Couleurs.royal500,
                    hauteur: 4,
                  ),
                ],
              ),
            ),
            IconButton(
              onPressed: () => _planifier(existante: e),
              icon: const Icon(Icons.edit_outlined, size: 17),
              color: Couleurs.encreDiscrete,
              visualDensity: VisualDensity.compact,
              tooltip: 'Modifier',
            ),
          ],
        ),
      ),
    );
  }
}

class _FormulaireEvaluation extends StatefulWidget {
  const _FormulaireEvaluation({
    required this.service,
    required this.classeId,
    required this.matieres,
    this.existante,
  });

  final EnseignantService service;
  final int classeId;
  final List<MatiereBreve> matieres;
  final Evaluation? existante;

  @override
  State<_FormulaireEvaluation> createState() => _FormulaireEvaluationState();
}

class _FormulaireEvaluationState extends State<_FormulaireEvaluation> {
  late final TextEditingController _libelle;
  late final TextEditingController _sujet;
  late final TextEditingController _bareme;

  int? _matiereId;
  TypeEvaluation _type = TypeEvaluation.devoir;
  PeriodeScolaire _periode = PeriodeScolaire.semestre1;
  int _coefficient = 1;
  DateTime _date = DateTime.now();

  bool _envoi = false;
  bool _suppression = false;
  String? _erreur;

  bool get _modification => widget.existante != null;

  @override
  void initState() {
    super.initState();
    final e = widget.existante;
    _libelle = TextEditingController(text: e?.libelle ?? '');
    _sujet = TextEditingController(text: e?.sujet ?? '');
    _bareme = TextEditingController(text: '${e?.bareme ?? 20}');
    _matiereId = e?.matiereId ?? (widget.matieres.isEmpty ? null : widget.matieres.first.id);
    _type = e?.type ?? TypeEvaluation.devoir;
    _periode = e?.periode ?? PeriodeScolaire.semestre1;
    _coefficient = e?.coefficient ?? 1;
    _date = e?.date ?? DateTime.now();
  }

  @override
  void dispose() {
    _libelle.dispose();
    _sujet.dispose();
    _bareme.dispose();
    super.dispose();
  }

  int? get _baremeNumerique {
    final v = int.tryParse(_bareme.text.trim());
    return v == null || v < 1 || v > 100 ? null : v;
  }

  bool get _valide =>
      _matiereId != null && _libelle.text.trim().isNotEmpty && _baremeNumerique != null;

  Future<void> _enregistrer() async {
    if (!_valide || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      if (_modification) {
        await widget.service.modifierEvaluation(
          widget.existante!.id,
          matiereId: _matiereId!,
          type: _type,
          libelle: _libelle.text,
          date: _date,
          bareme: _baremeNumerique!,
          coefficient: _coefficient,
          periode: _periode,
          sujet: _sujet.text,
        );
      } else {
        await widget.service.planifierEvaluation(
          classeId: widget.classeId,
          matiereId: _matiereId!,
          type: _type,
          libelle: _libelle.text,
          date: _date,
          bareme: _baremeNumerique!,
          coefficient: _coefficient,
          periode: _periode,
          sujet: _sujet.text,
        );
      }
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ErreurApi catch (e) {
      // Le serveur fige le barème dès qu'une note est saisie : son message l'explique.
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _envoi = false;
      });
    }
  }

  Future<void> _supprimer() async {
    setState(() {
      _suppression = true;
      _erreur = null;
    });
    try {
      await widget.service.supprimerEvaluation(widget.existante!.id);
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _suppression = false;
      });
    }
  }

  Future<void> _choisirDate() async {
    final choisie = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(DateTime.now().year - 1),
      lastDate: DateTime(DateTime.now().year + 1, 12, 31),
      locale: const Locale('fr'),
    );
    if (choisie != null) setState(() => _date = choisie);
  }

  @override
  Widget build(BuildContext context) {
    return CadreFormulaire(
      titre: _modification ? "Modifier l'évaluation" : 'Planifier une évaluation',
      sousTitre: 'Le barème fixe le total de points : les notes se saisissent dessus.',
      enfants: [
        if (_erreur != null) ...[
          Bandeau.erreur(message: _erreur!),
          const SizedBox(height: Espaces.md),
        ],
        const EtiquetteChamp('Matière'),
        DropdownButtonFormField<int>(
          initialValue: _matiereId,
          isExpanded: true,
          onChanged: _envoi ? null : (id) => setState(() => _matiereId = id),
          items: widget.matieres
              .map((m) => DropdownMenuItem(
                    value: m.id,
                    child: Text('${m.code} — ${m.libelle}',
                        style: Typo.corps, maxLines: 1, overflow: TextOverflow.ellipsis),
                  ))
              .toList(),
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Intitulé'),
        TextField(
          controller: _libelle,
          textCapitalization: TextCapitalization.sentences,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(hintText: 'Devoir n°1'),
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Nature'),
        Wrap(
          spacing: Espaces.sm - 2,
          runSpacing: Espaces.sm - 2,
          children: TypeEvaluation.values.map(_puceType).toList(),
        ),
        const SizedBox(height: Espaces.lg),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const EtiquetteChamp('Barème (sur)'),
                  TextField(
                    controller: _bareme,
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    onChanged: (_) => setState(() {}),
                    decoration: const InputDecoration(hintText: '20'),
                  ),
                  if (_bareme.text.trim().isNotEmpty && _baremeNumerique == null)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Text('Entre 1 et 100',
                          style: Typo.legende.copyWith(color: Couleurs.danger)),
                    ),
                ],
              ),
            ),
            const SizedBox(width: Espaces.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [const EtiquetteChamp('Coefficient'), _selecteurCoefficient()],
              ),
            ),
          ],
        ),
        const SizedBox(height: Espaces.lg),
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const EtiquetteChamp('Date'),
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
                  const EtiquetteChamp('Période'),
                  DropdownButtonFormField<PeriodeScolaire>(
                    initialValue: _periode,
                    isExpanded: true,
                    onChanged: _envoi ? null : (p) => setState(() => _periode = p ?? _periode),
                    items: PeriodeScolaire.values
                        .map((p) => DropdownMenuItem(value: p, child: Text(p.libelle)))
                        .toList(),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Sujet / consignes', facultatif: true),
        TextField(
          controller: _sujet,
          maxLines: 3,
          maxLength: 2000,
          textCapitalization: TextCapitalization.sentences,
          decoration: const InputDecoration(hintText: 'Énoncé, durée, documents autorisés…'),
        ),
        const SizedBox(height: Espaces.sm),
        ActionsFormulaire(
          libelle: _modification ? 'Enregistrer' : 'Planifier',
          envoi: _envoi,
          surValidation: _valide && !_suppression ? _enregistrer : null,
        ),
        if (_modification) ...[
          const SizedBox(height: Espaces.sm),
          Center(
            child: TextButton.icon(
              onPressed: _envoi || _suppression ? null : _supprimer,
              style: TextButton.styleFrom(foregroundColor: Couleurs.danger),
              icon: const Icon(Icons.delete_outline_rounded, size: 18),
              label: const Text("Supprimer l'évaluation"),
            ),
          ),
        ],
      ],
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
            onPressed: _coefficient > 1 ? () => setState(() => _coefficient--) : null,
            icon: const Icon(Icons.remove_rounded, size: 17),
            visualDensity: VisualDensity.compact,
          ),
          Expanded(
            child: Text('$_coefficient',
                textAlign: TextAlign.center, style: Typo.mono(14, graisse: FontWeight.w600)),
          ),
          IconButton(
            onPressed: _coefficient < 20 ? () => setState(() => _coefficient++) : null,
            icon: const Icon(Icons.add_rounded, size: 17),
            visualDensity: VisualDensity.compact,
          ),
        ],
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
            Icon(type.icone, size: 13, color: actif ? Colors.white : Couleurs.encreDiscrete),
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
