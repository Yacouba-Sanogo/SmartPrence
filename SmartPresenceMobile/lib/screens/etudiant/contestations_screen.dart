import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/gestion.dart';
import '../../models/note.dart';
import '../../services/etudiant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import '../../widgets/formulaire.dart';

/// Contestations de l'étudiant : celles qu'il a déposées, et la réponse de l'enseignant.
///
/// Contester est un recours, pas un geste courant : l'écran l'offre depuis les notes, et
/// n'accepte qu'une contestation à la fois par note — le serveur y veille.
class ContestationsScreen extends StatefulWidget {
  const ContestationsScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<ContestationsScreen> createState() => _ContestationsScreenState();
}

class _ContestationsScreenState extends State<ContestationsScreen> {
  late final EtudiantService _service = EtudiantService(widget.auth.client);

  List<Reclamation> _contestations = const [];
  bool _chargement = true;
  String? _erreur;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  Future<void> _charger() async {
    setState(() {
      _chargement = _contestations.isEmpty;
      _erreur = null;
    });
    try {
      final r = await _service.mesContestations();
      if (!mounted) return;
      setState(() {
        _contestations = r;
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

  Future<void> _nouvelle() async {
    final envoyee = await ouvrirFormulaire<bool>(context, (_) => _FormulaireContestation(service: _service));
    if (envoyee == true && mounted) _charger();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      floatingActionButton: _chargement || _erreur != null
          ? null
          : FloatingActionButton.extended(
              onPressed: _nouvelle,
              backgroundColor: Couleurs.indigo600,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.rate_review_rounded, size: 19),
              label: const Text('Contester une note'),
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
                      Text('Mes contestations',
                          style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19)),
                      const SizedBox(height: 2),
                      Text('Réponses de vos enseignants',
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
    if (_erreur != null && _contestations.isEmpty) {
      return ListView(
        key: const ValueKey('e'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Contestations indisponibles',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }
    if (_contestations.isEmpty) {
      return ListView(
        key: const ValueKey('v'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.rate_review_outlined,
            titre: 'Aucune contestation',
            detail: 'Une note vous paraît injuste ? Contestez-la : l’enseignant qui l’a '
                'saisie vous répond ici.',
          ),
        ],
      );
    }

    return ListView(
      key: const ValueKey('l'),
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      children: _contestations
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
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(r.matiere,
                    style: Typo.libelle.copyWith(fontSize: 14),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis),
              ),
              Pastille(texte: r.statut.libelle, teinte: r.statut.teinte, fond: r.statut.fond, compact: true),
            ],
          ),
          const SizedBox(height: 2),
          Text('${r.libelleNote} · ${Dates.courte(r.creeLe)}', style: Typo.legendePale),
          const SizedBox(height: Espaces.md),
          Text(r.motif, style: Typo.corpsAttenue.copyWith(fontSize: 13.5)),
          if (r.reponse != null) ...[
            const SizedBox(height: Espaces.md),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(Espaces.md),
              decoration: BoxDecoration(color: r.statut.fond, borderRadius: BorderRadius.circular(Rayons.md)),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(r.reponse!,
                      style: Typo.legende.copyWith(fontSize: 12.5, color: r.statut.teinte)),
                  if (r.corrigee) ...[
                    const SizedBox(height: 6),
                    Text('Note corrigée : ${formaterNote(r.valeurAvant!)} → ${formaterNote(r.valeurApres!)} / 20',
                        style: Typo.mono(12, graisse: FontWeight.w600, couleur: r.statut.teinte)),
                  ],
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _FormulaireContestation extends StatefulWidget {
  const _FormulaireContestation({required this.service});

  final EtudiantService service;

  @override
  State<_FormulaireContestation> createState() => _FormulaireContestationState();
}

class _FormulaireContestationState extends State<_FormulaireContestation> {
  final _motif = TextEditingController();
  List<Note> _notes = const [];
  String? _noteId;
  bool _chargement = true;
  bool _envoi = false;
  String? _erreur;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  @override
  void dispose() {
    _motif.dispose();
    super.dispose();
  }

  Future<void> _charger() async {
    try {
      final notes = await widget.service.notesContestables();
      if (!mounted) return;
      setState(() {
        _notes = notes;
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

  bool get _valide => _noteId != null && _motif.text.trim().isNotEmpty;

  Future<void> _envoyer() async {
    if (!_valide || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      await widget.service.contester(_noteId!, _motif.text);
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ErreurApi catch (e) {
      // Le serveur refuse notamment une note déjà en cours de contestation.
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _envoi = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return CadreFormulaire(
      titre: 'Contester une note',
      sousTitre: 'Expliquez précisément ce que vous contestez : l’enseignant y répondra.',
      enfants: [
        if (_chargement)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: Espaces.xxl),
            child: Center(child: CircularProgressIndicator(color: Couleurs.indigo600)),
          )
        else ...[
          if (_erreur != null) ...[
            Bandeau.erreur(message: _erreur!),
            const SizedBox(height: Espaces.md),
          ],
          if (_notes.isEmpty)
            Text('Aucune note publiée à contester pour le moment.', style: Typo.corpsAttenue)
          else ...[
            const EtiquetteChamp('Note concernée'),
            DropdownButtonFormField<String>(
              initialValue: _noteId,
              isExpanded: true,
              hint: const Text('Choisir une note'),
              onChanged: _envoi ? null : (id) => setState(() => _noteId = id),
              items: _notes
                  .map((n) => DropdownMenuItem(
                        value: n.id,
                        child: Text('${n.matiereLibelle ?? ''} — ${n.libelle} · ${n.valeurFormatee}/20',
                            style: Typo.corps, maxLines: 1, overflow: TextOverflow.ellipsis),
                      ))
                  .toList(),
            ),
            const SizedBox(height: Espaces.lg),
            const EtiquetteChamp('Motif'),
            TextField(
              controller: _motif,
              maxLines: 4,
              maxLength: 1000,
              textCapitalization: TextCapitalization.sentences,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(hintText: 'La question 3 n’a pas été comptée…'),
            ),
            const SizedBox(height: Espaces.sm),
            ActionsFormulaire(
              libelle: 'Envoyer',
              envoi: _envoi,
              surValidation: _valide ? _envoyer : null,
            ),
          ],
        ],
      ],
    );
  }
}
