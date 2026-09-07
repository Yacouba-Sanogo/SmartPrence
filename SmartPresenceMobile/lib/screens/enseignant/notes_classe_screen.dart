import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/etudiant_inscrit.dart';
import '../../models/note.dart';
import '../../models/seance.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import 'saisie_note_sheet.dart';

/// Notes d'une classe, vues et saisies par l'enseignant.
///
/// <p>Les notes sont groupées par étudiant, pas par évaluation : la question que se pose
/// un enseignant devant sa classe est « où en est celui-là », rarement « qui a eu quoi
/// au devoir n°2 ».</p>
class NotesClasseScreen extends StatefulWidget {
  const NotesClasseScreen({super.key, required this.auth, required this.classe});

  final AuthService auth;
  final ClasseEnseignee classe;

  @override
  State<NotesClasseScreen> createState() => _NotesClasseScreenState();
}

class _NotesClasseScreenState extends State<NotesClasseScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<Note> _notes = const [];
  List<EtudiantInscrit> _etudiants = const [];
  List<MatiereBreve> _matieres = const [];

  int? _filtreMatiere;
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
      final resultats = await Future.wait([
        _service.notesDeLaClasse(widget.classe.id, matiereId: _filtreMatiere),
        _service.etudiantsDeLaClasse(widget.classe.id),
        _service.matieres(),
      ]);
      if (!mounted) return;
      setState(() {
        _notes = resultats[0] as List<Note>;
        _etudiants = resultats[1] as List<EtudiantInscrit>;
        _matieres = resultats[2] as List<MatiereBreve>;
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

  /// Notes regroupées par étudiant, dans l'ordre de l'effectif.
  Map<EtudiantInscrit, List<Note>> get _parEtudiant {
    final groupes = <EtudiantInscrit, List<Note>>{};
    for (final etudiant in _etudiants) {
      groupes[etudiant] =
          _notes.where((n) => n.etudiantId == etudiant.id).toList();
    }
    return groupes;
  }

  Future<void> _ouvrirSaisie({EtudiantInscrit? etudiant, Note? note}) async {
    if (_matieres.isEmpty) {
      setState(() => _erreurAction =
          'Aucune matière active. La scolarité doit en créer avant toute notation.');
      return;
    }

    final enregistre = await ouvrirSaisieNote(
      context,
      service: _service,
      etudiants: _etudiants,
      matieres: _matieres,
      etudiantPreselectionne: etudiant,
      noteExistante: note,
    );

    if (enregistre && mounted) {
      setState(() => _erreurAction = null);
      _charger();
    }
  }

  Future<void> _supprimer(Note note) async {
    final confirme = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Supprimer cette note', style: Typo.titreSection),
        content: Text(
          '${note.libelle} — ${note.valeurFormatee}/20 pour ${note.etudiantComplet}. '
          'Cette suppression est définitive.',
          style: Typo.corpsAttenue,
        ),
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(Rayons.carte)),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Couleurs.danger),
            child: const Text('Supprimer'),
          ),
        ],
      ),
    );

    if (confirme != true) return;
    try {
      await _service.supprimerNote(note.id);
      if (mounted) _charger();
    } on ErreurApi catch (e) {
      // Le serveur refuse notamment la note d'un collègue : son message le dit.
      if (mounted) setState(() => _erreurAction = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      floatingActionButton: _chargement || _erreur != null
          ? null
          : FloatingActionButton.extended(
              onPressed: () => _ouvrirSaisie(),
              backgroundColor: Couleurs.indigo600,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.add_rounded, size: 20),
              label: const Text('Noter'),
            ),
      body: Column(
        children: [
          _entete(),
          Expanded(
            child: Transform.translate(
              offset: const Offset(0, -26),
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

  Widget _entete() {
    return EnteteDegrade(
      debordement: 32,
      padding: const EdgeInsets.fromLTRB(
          Espaces.sm, Espaces.sm, Espaces.lg, Espaces.md),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
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
                    Text('Notes',
                        style: Typo.titreEcran
                            .copyWith(color: Colors.white, fontSize: 19)),
                    const SizedBox(height: 2),
                    Text(
                      '${widget.classe.code} · ${_notes.length} note'
                      '${_notes.length > 1 ? 's' : ''}',
                      style: Typo.legende.copyWith(
                          color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5),
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (_matieres.isNotEmpty) ...[
            const SizedBox(height: Espaces.md),
            _filtresMatiere(),
          ],
        ],
      ),
    );
  }

  Widget _filtresMatiere() {
    return SizedBox(
      height: 30,
      child: ListView(
        scrollDirection: Axis.horizontal,
        physics: const BouncingScrollPhysics(),
        children: [
          _puce('Toutes', _filtreMatiere == null, () {
            setState(() => _filtreMatiere = null);
            _charger();
          }),
          ..._matieres.map((m) => _puce(m.code, _filtreMatiere == m.id, () {
                setState(() => _filtreMatiere = m.id);
                _charger();
              })),
        ],
      ),
    );
  }

  Widget _puce(String libelle, bool actif, VoidCallback surTap) {
    return Padding(
      padding: const EdgeInsets.only(right: Espaces.sm - 2),
      child: GestureDetector(
        onTap: surTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 5),
          decoration: BoxDecoration(
            color: actif ? Colors.white : Colors.white.withValues(alpha: 0.14),
            borderRadius: BorderRadius.circular(Rayons.pilule),
          ),
          child: Text(
            libelle,
            style: Typo.legende.copyWith(
              fontSize: 11.5,
              fontWeight: FontWeight.w600,
              color: actif ? Couleurs.indigo700 : Colors.white.withValues(alpha: 0.82),
            ),
          ),
        ),
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
            titre: 'Notes indisponibles',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }

    if (_etudiants.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.person_off_outlined,
            titre: 'Aucun étudiant inscrit',
            detail: 'Cette classe ne compte encore aucun inscrit à noter.',
          ),
        ],
      );
    }

    final groupes = _parEtudiant;

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 2),
      children: [
        if (_erreurAction != null) ...[
          Bandeau.erreur(
            message: _erreurAction!,
            surFermeture: () => setState(() => _erreurAction = null),
          ),
          const SizedBox(height: Espaces.md),
        ],
        ...groupes.entries.map((e) => _carteEtudiant(e.key, e.value)),
      ],
    );
  }

  Widget _carteEtudiant(EtudiantInscrit etudiant, List<Note> notes) {
    // Moyenne locale, purement indicative : celle qui fait foi est calculée par le
    // serveur sur l'ensemble des matières et figure au bulletin.
    final moyenne = notes.isEmpty
        ? null
        : notes.fold<double>(0, (t, n) => t + n.valeur * n.coefficient) /
            notes.fold<int>(0, (t, n) => t + n.coefficient);

    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm + 2),
      child: Carte(
        rayon: Rayons.tuile + 2,
        padding: EdgeInsets.zero,
        enfant: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(
                  horizontal: Espaces.md + 1, vertical: Espaces.md),
              child: Row(
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: moyenne == null
                          ? Couleurs.traitPale
                          : fondNote(moyenne),
                      borderRadius: BorderRadius.circular(Rayons.md - 2),
                    ),
                    child: Text(
                      moyenne == null
                          ? etudiant.initiales
                          : moyenne.toStringAsFixed(1).replaceAll('.', ','),
                      style: Typo.mono(12,
                          graisse: FontWeight.w700,
                          couleur: moyenne == null
                              ? Couleurs.encreDiscrete
                              : teinteNote(moyenne)),
                    ),
                  ),
                  const SizedBox(width: Espaces.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(etudiant.nomComplet,
                            style: Typo.libelle,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis),
                        const SizedBox(height: 2),
                        Text(
                          notes.isEmpty
                              ? 'Aucune note'
                              : '${notes.length} note${notes.length > 1 ? 's' : ''}',
                          style: Typo.legendePale,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: () => _ouvrirSaisie(etudiant: etudiant),
                    icon: const Icon(Icons.add_circle_outline_rounded, size: 21),
                    color: Couleurs.indigo600,
                    tooltip: 'Ajouter une note',
                  ),
                ],
              ),
            ),
            if (notes.isNotEmpty) ...[
              const Divider(height: 1, indent: Espaces.md + 1, endIndent: Espaces.md + 1),
              ...notes.map((n) => _ligneNote(etudiant, n)),
              const SizedBox(height: Espaces.sm),
            ],
          ],
        ),
      ),
    );
  }

  Widget _ligneNote(EtudiantInscrit etudiant, Note note) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 1, Espaces.sm + 2, Espaces.sm, 0),
      child: Row(
        children: [
          Container(
            width: 36,
            padding: const EdgeInsets.symmetric(vertical: 3),
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: fondNote(note.valeur),
              borderRadius: BorderRadius.circular(Rayons.sm),
            ),
            child: Text(note.valeurFormatee,
                style: Typo.mono(11.5,
                    graisse: FontWeight.w600, couleur: teinteNote(note.valeur))),
          ),
          const SizedBox(width: Espaces.md - 2),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(note.libelle,
                    style: Typo.legende.copyWith(
                        fontSize: 12, color: Couleurs.encreAttenuee),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis),
                const SizedBox(height: 1),
                Text(
                  '${note.matiereLibelle ?? ''} · coef. ${note.coefficient} · '
                  '${Dates.courte(note.dateEvaluation)}',
                  style: Typo.legendePale.copyWith(fontSize: 10.5),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: () => _ouvrirSaisie(etudiant: etudiant, note: note),
            icon: const Icon(Icons.edit_outlined, size: 17),
            color: Couleurs.encreDiscrete,
            visualDensity: VisualDensity.compact,
            tooltip: 'Modifier',
          ),
          IconButton(
            onPressed: () => _supprimer(note),
            icon: const Icon(Icons.delete_outline_rounded, size: 17),
            color: Couleurs.encreDiscrete,
            visualDensity: VisualDensity.compact,
            tooltip: 'Supprimer',
          ),
        ],
      ),
    );
  }
}
