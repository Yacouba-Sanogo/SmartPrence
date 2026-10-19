import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/gestion.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import '../../widgets/formulaire.dart';

/// Contestations de notes adressées à l'enseignant.
///
/// Les ouvertes sont en tête et mises en avant : une contestation qui traîne est la
/// première source de tension avec les étudiants. Répondre peut corriger la note ; la
/// contestation garde l'ancienne valeur, pour qu'une note publiée ne change jamais sans trace.
class ReclamationsScreen extends StatefulWidget {
  const ReclamationsScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<ReclamationsScreen> createState() => _ReclamationsScreenState();
}

class _ReclamationsScreenState extends State<ReclamationsScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<Reclamation> _reclamations = const [];
  bool _chargement = true;
  String? _erreur;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  Future<void> _charger() async {
    setState(() {
      _chargement = _reclamations.isEmpty;
      _erreur = null;
    });
    try {
      final r = await _service.reclamationsRecues();
      if (!mounted) return;
      setState(() {
        _reclamations = r;
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

  int get _ouvertes => _reclamations.where((r) => r.ouverte).length;

  Future<void> _repondre(Reclamation r) async {
    final traitee = await ouvrirFormulaire<bool>(
      context,
      (_) => _FormulaireReponse(service: _service, reclamation: r),
    );
    if (traitee == true && mounted) _charger();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
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
                      Text('Réclamations',
                          style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19)),
                      const SizedBox(height: 2),
                      Text(
                        _ouvertes == 0
                            ? 'Aucune en attente de réponse'
                            : '$_ouvertes à traiter',
                        style: Typo.legende.copyWith(
                            color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5),
                      ),
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
                child: TransitionContenu(enfant: _corps()),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _corps() {
    if (_chargement) {
      return const Center(key: ValueKey('c'), child: CircularProgressIndicator(color: Couleurs.indigo600));
    }
    if (_erreur != null && _reclamations.isEmpty) {
      return ListView(
        key: const ValueKey('e'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Réclamations indisponibles',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }
    if (_reclamations.isEmpty) {
      return ListView(
        key: const ValueKey('v'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.mark_email_read_outlined,
            titre: 'Aucune réclamation',
            detail: 'Quand un étudiant conteste l’une de vos notes, sa demande arrive ici.',
          ),
        ],
      );
    }

    return ListView(
      key: const ValueKey('l'),
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      children: _reclamations
          .asMap()
          .entries
          .map((e) => ApparitionEnCascade(
                rang: e.key,
                enfant: Padding(
                  padding: const EdgeInsets.only(bottom: Espaces.md),
                  child: _carte(e.value),
                ),
              ))
          .toList(),
    );
  }

  Widget _carte(Reclamation r) {
    return Carte(
      surTap: r.ouverte ? () => _repondre(r) : null,
      padding: EdgeInsets.zero,
      enfant: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              width: r.ouverte ? 4 : 0,
              decoration: const BoxDecoration(
                color: Couleurs.alerteVif,
                borderRadius: BorderRadius.horizontal(left: Radius.circular(Rayons.carte)),
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(Espaces.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(r.etudiantNom,
                              style: Typo.libelle.copyWith(fontSize: 14),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis),
                        ),
                        Pastille(
                            texte: r.statut.libelle,
                            teinte: r.statut.teinte,
                            fond: r.statut.fond,
                            compact: true),
                      ],
                    ),
                    const SizedBox(height: 2),
                    Text('${r.matiere} · ${r.libelleNote} · ${Dates.courte(r.creeLe)}',
                        style: Typo.legendePale, maxLines: 1, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: Espaces.md),
                    Row(
                      children: [
                        _note(r),
                        const SizedBox(width: Espaces.md),
                        Expanded(child: Text(r.motif, style: Typo.corpsAttenue.copyWith(fontSize: 13.5))),
                      ],
                    ),
                    if (r.reponse != null) ...[
                      const SizedBox(height: Espaces.md),
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(Espaces.md),
                        decoration: BoxDecoration(
                          color: r.statut.fond,
                          borderRadius: BorderRadius.circular(Rayons.md),
                        ),
                        child: Text('Votre réponse : ${r.reponse}',
                            style: Typo.legende.copyWith(fontSize: 12.5, color: r.statut.teinte)),
                      ),
                    ],
                    if (r.ouverte) ...[
                      const SizedBox(height: Espaces.sm),
                      Align(
                        alignment: Alignment.centerRight,
                        child: Text('Toucher pour répondre',
                            style: Typo.legende.copyWith(color: Couleurs.indigo600, fontWeight: FontWeight.w600)),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// La note, avec son ancienne valeur barrée quand elle a été corrigée.
  Widget _note(Reclamation r) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: fondNoteReclamation(r.valeurActuelle),
        borderRadius: BorderRadius.circular(Rayons.md - 2),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (r.corrigee)
            Text(formaterNote(r.valeurAvant!),
                style: Typo.mono(11, couleur: Couleurs.encrePale)
                    .copyWith(decoration: TextDecoration.lineThrough)),
          Text(formaterNote(r.valeurActuelle),
              style: Typo.mono(16, graisse: FontWeight.w700, couleur: Couleurs.encre)),
          Text('/20', style: Typo.legendePale.copyWith(fontSize: 10)),
        ],
      ),
    );
  }
}

Color fondNoteReclamation(double v) =>
    v >= 14 ? Couleurs.succesFond : (v >= 10 ? Couleurs.alerteFond : Couleurs.dangerFond);

class _FormulaireReponse extends StatefulWidget {
  const _FormulaireReponse({required this.service, required this.reclamation});

  final EnseignantService service;
  final Reclamation reclamation;

  @override
  State<_FormulaireReponse> createState() => _FormulaireReponseState();
}

class _FormulaireReponseState extends State<_FormulaireReponse> {
  final _reponse = TextEditingController();
  late final TextEditingController _valeur =
      TextEditingController(text: formaterNote(widget.reclamation.valeurActuelle).replaceAll(',', '.'));
  bool _accepter = true;
  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() {
    _reponse.dispose();
    _valeur.dispose();
    super.dispose();
  }

  double? get _valeurNumerique {
    final v = double.tryParse(_valeur.text.trim().replaceAll(',', '.'));
    return v == null || v < 0 || v > 20 ? null : v;
  }

  bool get _valide =>
      _reponse.text.trim().isNotEmpty && (!_accepter || _valeur.text.trim().isEmpty || _valeurNumerique != null);

  Future<void> _envoyer() async {
    if (!_valide || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      await widget.service.repondreReclamation(
        widget.reclamation.id,
        accepter: _accepter,
        reponse: _reponse.text,
        valeurCorrigee: _accepter ? _valeurNumerique : null,
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
    final r = widget.reclamation;
    return CadreFormulaire(
      titre: 'Répondre à ${r.etudiantNom}',
      sousTitre: '${r.matiere} · ${r.libelleNote}',
      enfants: [
        if (_erreur != null) ...[
          Bandeau.erreur(message: _erreur!),
          const SizedBox(height: Espaces.md),
        ],
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(Espaces.md),
          decoration: BoxDecoration(
            color: Couleurs.traitPale,
            borderRadius: BorderRadius.circular(Rayons.md),
          ),
          child: Text('« ${r.motif} »', style: Typo.corpsAttenue.copyWith(fontStyle: FontStyle.italic)),
        ),
        const SizedBox(height: Espaces.lg),
        Container(
          padding: const EdgeInsets.all(3),
          decoration: BoxDecoration(
            color: Couleurs.traitPale,
            borderRadius: BorderRadius.circular(Rayons.tuile),
          ),
          child: Row(
            children: [
              _choix('Accepter', true, Couleurs.succes),
              _choix('Rejeter', false, Couleurs.danger),
            ],
          ),
        ),
        AnimatedSize(
          duration: Durees.moyenne,
          curve: Courbes.sortie,
          alignment: Alignment.topCenter,
          child: _accepter
              ? Padding(
                  padding: const EdgeInsets.only(top: Espaces.lg),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const EtiquetteChamp('Note corrigée (sur 20)', facultatif: true),
                      TextField(
                        controller: _valeur,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))],
                        onChanged: (_) => setState(() {}),
                        decoration: InputDecoration(
                          hintText: 'Laisser tel quel : ${formaterNote(r.valeurActuelle)}',
                        ),
                      ),
                      if (_valeur.text.trim().isNotEmpty && _valeurNumerique == null)
                        Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text('Entre 0 et 20',
                              style: Typo.legende.copyWith(color: Couleurs.danger)),
                        ),
                    ],
                  ),
                )
              : const SizedBox(width: double.infinity),
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Réponse à l’étudiant'),
        TextField(
          controller: _reponse,
          maxLines: 3,
          maxLength: 1000,
          textCapitalization: TextCapitalization.sentences,
          onChanged: (_) => setState(() {}),
          decoration: InputDecoration(
            hintText: _accepter
                ? 'La question 3 a bien été omise, je corrige.'
                : 'Le barème a été appliqué comme annoncé.',
          ),
        ),
        const SizedBox(height: Espaces.sm),
        ActionsFormulaire(
          libelle: _accepter ? 'Accepter et répondre' : 'Rejeter et répondre',
          envoi: _envoi,
          surValidation: _valide ? _envoyer : null,
        ),
      ],
    );
  }

  Widget _choix(String libelle, bool valeur, Color teinte) {
    final actif = _accepter == valeur;
    return Expanded(
      child: GestureDetector(
        onTap: () => setState(() => _accepter = valeur),
        child: AnimatedContainer(
          duration: Durees.rapide,
          height: 42,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: actif ? Couleurs.carte : Colors.transparent,
            borderRadius: BorderRadius.circular(Rayons.md - 2),
            boxShadow: actif ? Couleurs.ombreCarte : null,
          ),
          child: Text(libelle,
              style: Typo.legende.copyWith(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: actif ? teinte : Couleurs.encreDiscrete,
              )),
        ),
      ),
    );
  }
}
