import 'package:flutter/material.dart';

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

/// Programme d'une classe : chapitres par matière avec leur avancement, et cahier de
/// textes.
///
/// Les deux vont ensemble : le cahier raconte ce qui a été fait séance après séance, le
/// programme dit où l'on en est sur l'ensemble de la matière.
class ProgrammeScreen extends StatefulWidget {
  const ProgrammeScreen({super.key, required this.auth, required this.classe});

  final AuthService auth;
  final ClasseEnseignee classe;

  @override
  State<ProgrammeScreen> createState() => _ProgrammeScreenState();
}

class _ProgrammeScreenState extends State<ProgrammeScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<Syllabus> _syllabus = const [];
  List<PageCahier> _cahier = const [];
  List<MatiereBreve> _matieres = const [];
  bool _onCahier = false;
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
        _service.syllabus(widget.classe.id),
        _service.cahierDeClasse(widget.classe.id),
        _service.matieres(),
      ]);
      if (!mounted) return;
      setState(() {
        _syllabus = r[0] as List<Syllabus>;
        _cahier = r[1] as List<PageCahier>;
        _matieres = r[2] as List<MatiereBreve>;
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

  Future<void> _ajouter() async {
    if (_matieres.isEmpty) {
      setState(() => _erreurAction =
          'Aucune matière active. La scolarité doit en créer avant tout programme.');
      return;
    }
    final ajoute = await ouvrirFormulaire<bool>(
      context,
      (_) => _FormulaireChapitre(
          service: _service, classeId: widget.classe.id, matieres: _matieres),
    );
    if (ajoute == true && mounted) {
      setState(() => _erreurAction = null);
      _charger();
    }
  }

  Future<void> _basculer(Chapitre chapitre) async {
    try {
      await _service.modifierChapitre(chapitre.id,
          titre: chapitre.titre, objectifs: chapitre.objectifs, termine: !chapitre.termine);
      if (mounted) _charger();
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreurAction = e.message);
    }
  }

  Future<void> _supprimer(Chapitre chapitre) async {
    final confirme = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Supprimer ce chapitre', style: Typo.titreSection),
        content: Text('« ${chapitre.titre} » sera retiré du programme.',
            style: Typo.corpsAttenue),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Rayons.carte)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
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
      await _service.supprimerChapitre(chapitre.id);
      if (mounted) _charger();
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreurAction = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      floatingActionButton: _chargement || _erreur != null || _onCahier
          ? null
          : FloatingActionButton.extended(
              onPressed: _ajouter,
              backgroundColor: Couleurs.indigo600,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.add_rounded, size: 20),
              label: const Text('Chapitre'),
            ),
      body: Column(
        children: [
          EnteteDegrade(
            debordement: 32,
            padding: const EdgeInsets.fromLTRB(Espaces.sm, Espaces.sm, Espaces.lg, Espaces.md),
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
                          Text('Programme',
                              style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19)),
                          const SizedBox(height: 2),
                          Text(widget.classe.code,
                              style: Typo.legende.copyWith(
                                  color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5)),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: Espaces.md),
                Row(
                  children: [
                    _onglet('Chapitres', !_onCahier, () => setState(() => _onCahier = false)),
                    const SizedBox(width: Espaces.sm - 2),
                    _onglet('Cahier de textes', _onCahier, () => setState(() => _onCahier = true)),
                  ],
                ),
              ],
            ),
          ),
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

  Widget _onglet(String libelle, bool actif, VoidCallback surTap) {
    return GestureDetector(
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
            titre: 'Programme indisponible',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
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
        // La clé relance la cascade à chaque bascule d'onglet.
        KeyedSubtree(
          key: ValueKey(_onCahier),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: _onCahier ? _pagesCahier() : _chapitres(),
          ),
        ),
      ],
    );
  }

  List<Widget> _chapitres() {
    if (_syllabus.isEmpty) {
      return const [
        SizedBox(height: Espaces.xl),
        EtatVide(
          icone: Icons.checklist_rounded,
          titre: 'Aucun chapitre',
          detail: 'Découpez votre cours en chapitres pour suivre son avancement.',
        ),
      ];
    }
    return _syllabus
        .asMap()
        .entries
        .map((e) => ApparitionEnCascade(rang: e.key, enfant: _carteMatiere(e.value)))
        .toList();
  }

  Widget _carteMatiere(Syllabus s) {
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.md),
      child: Carte(
        padding: const EdgeInsets.fromLTRB(Espaces.lg, Espaces.lg, Espaces.sm, Espaces.sm),
        enfant: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(right: Espaces.md),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(s.matiereLibelle,
                            style: Typo.libelle.copyWith(fontSize: 14),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis),
                      ),
                      Text('${s.termines}/${s.chapitres.length}',
                          style: Typo.mono(13, graisse: FontWeight.w600)),
                    ],
                  ),
                  const SizedBox(height: Espaces.sm),
                  BarreAnimee(
                    valeur: s.avancement,
                    couleur: s.avancement >= 1 ? Couleurs.succesVif : Couleurs.indigo600,
                  ),
                ],
              ),
            ),
            const SizedBox(height: Espaces.xs),
            ...s.chapitres.map(_ligneChapitre),
          ],
        ),
      ),
    );
  }

  Widget _ligneChapitre(Chapitre c) {
    return InkWell(
      onTap: () => _basculer(c),
      borderRadius: BorderRadius.circular(Rayons.sm),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          children: [
            // La coche « tombe » en ressort : le geste de cocher se sent avant de se lire.
            AnimatedSwitcher(
              duration: Durees.moyenne,
              switchInCurve: Curves.elasticOut,
              transitionBuilder: (enfant, animation) =>
                  ScaleTransition(scale: animation, child: enfant),
              child: Icon(
                c.termine ? Icons.check_circle_rounded : Icons.radio_button_unchecked_rounded,
                key: ValueKey(c.termine),
                size: 22,
                color: c.termine ? Couleurs.succesVif : Couleurs.encrePale,
              ),
            ),
            const SizedBox(width: Espaces.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  AnimatedDefaultTextStyle(
                    duration: Durees.moyenne,
                    style: Typo.corps.copyWith(
                      fontSize: 13.5,
                      color: c.termine ? Couleurs.encreAttenuee : Couleurs.encre,
                      decoration: c.termine ? TextDecoration.lineThrough : null,
                    ),
                    child: Text('${c.ordre}. ${c.titre}'),
                  ),
                  if (c.termine && c.termineLe != null)
                    Text('Terminé le ${Dates.courte(c.termineLe!)}', style: Typo.legendePale),
                ],
              ),
            ),
            IconButton(
              onPressed: () => _supprimer(c),
              icon: const Icon(Icons.delete_outline_rounded, size: 17),
              color: Couleurs.encreDiscrete,
              visualDensity: VisualDensity.compact,
              tooltip: 'Supprimer',
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _pagesCahier() {
    if (_cahier.isEmpty) {
      return const [
        SizedBox(height: Espaces.xl),
        EtatVide(
          icone: Icons.menu_book_outlined,
          titre: 'Cahier de textes vide',
          detail: 'Ouvrez une séance depuis « Journée » pour y consigner ce que vous avez enseigné.',
        ),
      ];
    }
    return _cahier
        .map((p) => Padding(
              padding: const EdgeInsets.only(bottom: Espaces.md),
              child: Carte(
                padding: const EdgeInsets.all(Espaces.lg),
                enfant: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(p.matiereLibelle,
                              style: Typo.libelle.copyWith(fontSize: 13.5),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis),
                        ),
                        Text(Dates.courte(p.debutSeance), style: Typo.legendePale),
                      ],
                    ),
                    if (p.chapitreTitre != null) ...[
                      const SizedBox(height: 4),
                      Pastille(
                        texte: p.chapitreTitre!,
                        teinte: Couleurs.indigo600,
                        fond: Couleurs.royal50,
                        compact: true,
                      ),
                    ],
                    const SizedBox(height: Espaces.sm),
                    Text(p.contenu, style: Typo.corps.copyWith(fontSize: 13.5)),
                    if (p.travailAFaire != null) ...[
                      const SizedBox(height: Espaces.sm),
                      Text('À faire : ${p.travailAFaire}', style: Typo.corpsAttenue),
                    ],
                  ],
                ),
              ),
            ))
        .toList();
  }
}

class _FormulaireChapitre extends StatefulWidget {
  const _FormulaireChapitre(
      {required this.service, required this.classeId, required this.matieres});

  final EnseignantService service;
  final int classeId;
  final List<MatiereBreve> matieres;

  @override
  State<_FormulaireChapitre> createState() => _FormulaireChapitreState();
}

class _FormulaireChapitreState extends State<_FormulaireChapitre> {
  final _titre = TextEditingController();
  final _objectifs = TextEditingController();
  late int? _matiereId = widget.matieres.isEmpty ? null : widget.matieres.first.id;
  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() {
    _titre.dispose();
    _objectifs.dispose();
    super.dispose();
  }

  bool get _valide => _matiereId != null && _titre.text.trim().isNotEmpty;

  Future<void> _enregistrer() async {
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      await widget.service.ajouterChapitre(
        classeId: widget.classeId,
        matiereId: _matiereId!,
        titre: _titre.text,
        objectifs: _objectifs.text,
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
    return CadreFormulaire(
      titre: 'Nouveau chapitre',
      sousTitre: 'Il rejoint la fin du programme de la matière choisie.',
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
        const EtiquetteChamp('Titre'),
        TextField(
          controller: _titre,
          textCapitalization: TextCapitalization.sentences,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(hintText: 'Chapitre 1 — Introduction'),
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Objectifs', facultatif: true),
        TextField(
          controller: _objectifs,
          maxLines: 2,
          maxLength: 500,
          textCapitalization: TextCapitalization.sentences,
          decoration: const InputDecoration(hintText: 'Ce que l’étudiant saura faire à la fin'),
        ),
        const SizedBox(height: Espaces.sm),
        ActionsFormulaire(
          libelle: 'Ajouter',
          envoi: _envoi,
          surValidation: _valide ? _enregistrer : null,
        ),
      ],
    );
  }
}
