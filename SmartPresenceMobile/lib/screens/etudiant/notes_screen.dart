import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/note.dart';
import '../../models/releve.dart';
import '../../services/etudiant_service.dart';
import '../../widgets/anneau.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';

/// Relevé de notes de l'étudiant, au format LMD.
///
/// <p>Trois niveaux de lecture, du plus synthétique au plus détaillé, parce que
/// c'est dans cet ordre qu'on lit un relevé : la décision et les crédits d'abord —
/// « ai-je validé ? » —, puis les UE, et enfin, à la demande, le détail des ECUE
/// avec la note de devoir et celle d'examen.</p>
///
/// <p>Aucune moyenne n'est calculée ici. Elles viennent toutes du serveur : ce qui
/// s'affiche sur le téléphone est, au centième près, ce que dit le relevé de
/// l'administration.</p>
class NotesScreen extends StatefulWidget {
  const NotesScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<NotesScreen> createState() => _NotesScreenState();
}

class _NotesScreenState extends State<NotesScreen> {
  late final EtudiantService _service = EtudiantService(widget.auth.client);

  ReleveSemestre _releve = ReleveSemestre.vide;
  PeriodeScolaire? _semestre;
  bool _chargement = true;
  String? _erreur;

  /// UE dépliées, par identifiant.
  final Set<int> _depliees = {};

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
      final releve = await _service.monReleve(semestre: _semestre);
      if (!mounted) return;
      setState(() {
        _releve = releve;
        // Le serveur tranche quand aucun semestre n'est demandé : on s'aligne sur
        // sa réponse, sinon l'onglet actif ne correspondrait pas au contenu affiché.
        _semestre ??= releve.semestre;
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

  void _changerSemestre(PeriodeScolaire semestre) {
    if (semestre == _semestre) return;
    setState(() {
      _semestre = semestre;
      _depliees.clear();
    });
    _charger();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
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

  // ------------------------------------------------------------------ En-tête

  Widget _entete() {
    return EnteteDegrade(
      debordement: 32,
      padding: const EdgeInsets.fromLTRB(
          Espaces.lg, Espaces.md, Espaces.lg, Espaces.md),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('Mes notes',
              style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 21)),
          const SizedBox(height: 3),
          Text(
            _releve.classeLibelle ?? _releve.promotionLibelle ?? 'Relevé de notes',
            style: Typo.legende.copyWith(
                color: Colors.white.withValues(alpha: 0.72), fontSize: 12),
          ),
          if (!_chargement && _erreur == null) ...[
            const SizedBox(height: Espaces.lg),
            _synthese(),
          ],
          const SizedBox(height: Espaces.md),
          _ongletsSemestres(),
        ],
      ),
    );
  }

  Widget _synthese() {
    return Row(
      children: [
        Anneau(
          valeur: _releve.progression,
          taille: 76,
          epaisseur: 12,
          couleurFond: Colors.white.withValues(alpha: 0.2),
          couleur: _releve.moyenneGenerale == null
              ? Colors.white.withValues(alpha: 0.35)
              : teinteNote(_releve.moyenneGenerale!),
          centre: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_releve.moyenneFormatee,
                  style: Typo.mono(16,
                      graisse: FontWeight.w600, couleur: Colors.white)),
              Text('/ 20',
                  style: Typo.legende.copyWith(
                      fontSize: 9, color: Colors.white.withValues(alpha: 0.62))),
            ],
          ),
        ),
        const SizedBox(width: Espaces.lg - 2),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('Moyenne générale',
                  style: Typo.legende.copyWith(
                      fontSize: 11, color: Colors.white.withValues(alpha: 0.70))),
              const SizedBox(height: Espaces.sm),
              Row(
                children: [
                  Expanded(
                    child: Compteur(
                      surFondColore: true,
                      valeur: '${_releve.creditsAcquis}/${_releve.creditsRequis}',
                      libelle: 'Crédits',
                      teinte: _releve.creditsRequis > 0 &&
                              _releve.creditsAcquis >= _releve.creditsRequis
                          ? Couleurs.succesVif
                          : Colors.white,
                    ),
                  ),
                  const SizedBox(width: 6),
                  Expanded(flex: 2, child: _badgeDecision()),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  /// La décision, en pastille pleine.
  ///
  /// Une couleur posée sur un voile blanc translucide, elle-même sur un dégradé
  /// indigo, ne ressort pas : c'est pourtant la première ligne que l'étudiant
  /// cherche. Le fond plein lui rend son évidence.
  Widget _badgeDecision() {
    final (Color fond, Color teinte, IconData icone) = switch (_releve.decision) {
      DecisionSemestre.valide =>
        (Couleurs.succesVif, Colors.white, Icons.verified_rounded),
      DecisionSemestre.nonValide =>
        (const Color(0xFFF2506A), Colors.white, Icons.error_outline_rounded),
      DecisionSemestre.enAttente => (
          Colors.white.withValues(alpha: 0.16),
          Colors.white,
          Icons.hourglass_empty_rounded
        ),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: fond,
        borderRadius: BorderRadius.circular(Rayons.md - 2),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              Icon(icone, size: 13, color: teinte),
              const SizedBox(width: 5),
              Flexible(
                child: Text(
                  _releve.decision.libelle,
                  style: Typo.libelle.copyWith(
                      fontSize: 13, fontWeight: FontWeight.w700, color: teinte),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 1),
          Text('Décision',
              style: Typo.legende.copyWith(
                  fontSize: 9.5, color: teinte.withValues(alpha: 0.72))),
        ],
      ),
    );
  }

  /// Onglets S1 … S6 — seulement ceux où la promotion a une maquette.
  Widget _ongletsSemestres() {
    final semestres = _releve.semestresDisponibles;

    return SizedBox(
      height: 30,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: semestres.length,
        separatorBuilder: (_, index) => const SizedBox(width: Espaces.sm - 2),
        itemBuilder: (_, index) {
          final semestre = semestres[index];
          final actif = _semestre == semestre;
          return GestureDetector(
            onTap: () => _changerSemestre(semestre),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              decoration: BoxDecoration(
                color: actif ? Colors.white : Colors.white.withValues(alpha: 0.14),
                borderRadius: BorderRadius.circular(Rayons.pilule),
              ),
              child: Text(
                semestre.libelleCourt,
                style: Typo.legende.copyWith(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w700,
                  color: actif
                      ? Couleurs.indigo700
                      : Colors.white.withValues(alpha: 0.82),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  // -------------------------------------------------------------------- Corps

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
            titre: 'Relevé indisponible',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }

    if (_releve.unites.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          const EtatVide(
            icone: Icons.account_tree_outlined,
            titre: 'Maquette non renseignée',
            detail: 'Les unités d’enseignement de ce semestre n’ont pas encore été '
                'saisies par la scolarité.',
          ),
        ],
      );
    }

    if (_releve.sansNote) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(
            Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
        children: [
          const SizedBox(height: Espaces.lg),
          EtatVide(
            icone: Icons.school_outlined,
            titre: 'Aucune note pour l’instant',
            detail: 'Vos notes du ${_releve.semestre.libelle.toLowerCase()} '
                'apparaîtront ici dès que vos enseignants les auront saisies.',
          ),
          const SizedBox(height: Espaces.lg),
          // La maquette reste visible : savoir ce qui sera évalué a son intérêt,
          // même avant la première note.
          ..._releve.unites.map(_carteUnite),
        ],
      );
    }

    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      itemCount: _releve.unites.length + (_releve.compensationAppliquee ? 1 : 0),
      separatorBuilder: (_, index) => const SizedBox(height: Espaces.sm + 2),
      itemBuilder: (_, index) {
        if (_releve.compensationAppliquee && index == _releve.unites.length) {
          return _noteSurLaCompensation();
        }
        return _carteUnite(_releve.unites[index]);
      },
    );
  }

  /// Une UE à 9 marquée « acquise » sans un mot d'explication est incompréhensible.
  Widget _noteSurLaCompensation() {
    return Container(
      padding: const EdgeInsets.all(Espaces.md),
      decoration: BoxDecoration(
        color: Couleurs.succesFond,
        borderRadius: BorderRadius.circular(Rayons.tuile),
        border: Border.all(color: Couleurs.succesTrait),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.info_outline_rounded, size: 16, color: Couleurs.succes),
          const SizedBox(width: Espaces.sm),
          Expanded(
            child: Text(
              'Une ou plusieurs UE sous la moyenne ont été acquises par compensation : '
              'la moyenne générale du semestre atteint la barre de 10.',
              style: Typo.legende.copyWith(
                  fontSize: 11.5, height: 1.45, color: Couleurs.succes),
            ),
          ),
        ],
      ),
    );
  }

  // ------------------------------------------------------------------ Une UE

  Widget _carteUnite(UniteReleve unite) {
    final depliee = _depliees.contains(unite.id);
    final moyenne = unite.moyenne;
    final teinte = moyenne == null ? Couleurs.encreDiscrete : teinteNote(moyenne);
    final fond = moyenne == null ? Couleurs.traitPale : fondNote(moyenne);

    return Carte(
      rayon: Rayons.tuile + 2,
      padding: EdgeInsets.zero,
      surTap: unite.ecues.isEmpty
          ? null
          : () => setState(() {
                if (depliee) {
                  _depliees.remove(unite.id);
                } else {
                  _depliees.add(unite.id);
                }
              }),
      enfant: Column(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(
                horizontal: Espaces.md + 1, vertical: Espaces.md),
            child: Row(
              children: [
                Container(
                  width: 46,
                  height: 46,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: fond,
                    borderRadius: BorderRadius.circular(Rayons.md - 1),
                  ),
                  child: Text(unite.moyenneFormatee,
                      style: Typo.mono(14, graisse: FontWeight.w700, couleur: teinte)),
                ),
                const SizedBox(width: Espaces.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(unite.libelle,
                          style: Typo.libelle,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis),
                      const SizedBox(height: 3),
                      Row(
                        children: [
                          Flexible(
                            child: Text(unite.code,
                                style: Typo.legendePale,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis),
                          ),
                          const SizedBox(width: 6),
                          _pastilleCredits(unite),
                        ],
                      ),
                    ],
                  ),
                ),
                if (unite.ecues.isNotEmpty)
                  AnimatedRotation(
                    turns: depliee ? 0.5 : 0,
                    duration: const Duration(milliseconds: 200),
                    child: const Icon(Icons.keyboard_arrow_down_rounded,
                        size: 22, color: Couleurs.encrePale),
                  ),
              ],
            ),
          ),
          if (depliee) ...[
            const Divider(height: 1, indent: Espaces.md + 1, endIndent: Espaces.md + 1),
            _tableauEcues(unite),
          ],
        ],
      ),
    );
  }

  Widget _pastilleCredits(UniteReleve unite) {
    final acquise = unite.acquise;
    return Pastille(
      compact: true,
      texte: '${unite.creditsFormates} cr.',
      icone: acquise
          ? (unite.acquiseParCompensation
              ? Icons.compare_arrows_rounded
              : Icons.check_circle_rounded)
          : Icons.radio_button_unchecked_rounded,
      teinte: acquise ? Couleurs.succes : Couleurs.encreDiscrete,
      fond: acquise ? Couleurs.succesFond : Couleurs.traitPale,
    );
  }

  // ------------------------------------------------- Le tableau des ECUE

  /// Tableau des ECUE : devoir, examen, moyenne de l'ECUE et moyenne de l'UE.
  ///
  /// La moyenne de l'UE ne figure que sur la première ligne, comme une cellule
  /// fusionnée sur un relevé imprimé : la répéter à chaque ligne laisserait croire
  /// qu'elle change d'un ECUE à l'autre.
  Widget _tableauEcues(UniteReleve unite) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 1, Espaces.sm + 2, Espaces.md + 1, Espaces.md),
      child: Column(
        children: [
          _enteteTableau(),
          const SizedBox(height: 2),
          ...unite.ecues.asMap().entries.map(
                (entree) => _ligneEcue(
                  entree.value,
                  moyenneUnite: entree.key == 0 ? unite.moyenneFormatee : null,
                ),
              ),
        ],
      ),
    );
  }

  Widget _enteteTableau() {
    TextStyle style() => Typo.legendePale.copyWith(
        fontSize: 9.5, fontWeight: FontWeight.w700, letterSpacing: 0.3);

    return Container(
      padding: const EdgeInsets.symmetric(vertical: 6, horizontal: Espaces.sm),
      decoration: BoxDecoration(
        color: Couleurs.traitPale,
        borderRadius: BorderRadius.circular(Rayons.sm),
      ),
      child: Row(
        children: [
          Expanded(child: Text('ECUE', style: style())),
          SizedBox(width: 36, child: Text('DEV', style: style(), textAlign: TextAlign.center)),
          SizedBox(width: 36, child: Text('EXM', style: style(), textAlign: TextAlign.center)),
          SizedBox(width: 42, child: Text('MOY', style: style(), textAlign: TextAlign.center)),
          SizedBox(width: 44, child: Text('MOY UE', style: style(), textAlign: TextAlign.center)),
        ],
      ),
    );
  }

  Widget _ligneEcue(EcueReleve ecue, {String? moyenneUnite}) {
    final moyenne = ecue.moyenne;
    final teinte = moyenne == null ? Couleurs.encreDiscrete : teinteNote(moyenne);

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7, horizontal: Espaces.sm),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Deux lignes plutôt qu'une : « Architecture des ord… » ne dit pas
                // de quelle matière il s'agit, et la colonne ne peut pas s'élargir
                // sans écraser les chiffres.
                Text(ecue.libelle,
                    style: Typo.legende.copyWith(
                        fontSize: 12, height: 1.25, color: Couleurs.encreAttenuee),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
                const SizedBox(height: 1),
                Text('${ecue.code} · ${ecue.credits} cr.',
                    style: Typo.legendePale.copyWith(fontSize: 10),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
          _cellule(ecue.devoirFormate, largeur: 36),
          _cellule(ecue.examenFormate, largeur: 36),
          _cellule(ecue.moyenneFormatee, largeur: 42, teinte: teinte, gras: true),
          SizedBox(
            width: 44,
            child: moyenneUnite == null
                ? const SizedBox.shrink()
                : Text(moyenneUnite,
                    textAlign: TextAlign.center,
                    style: Typo.mono(12.5,
                        graisse: FontWeight.w700, couleur: Couleurs.indigo700)),
          ),
        ],
      ),
    );
  }

  Widget _cellule(String valeur, {required double largeur, Color? teinte, bool gras = false}) {
    return SizedBox(
      width: largeur,
      child: Text(
        valeur,
        textAlign: TextAlign.center,
        style: Typo.mono(12,
            graisse: gras ? FontWeight.w700 : FontWeight.w500,
            couleur: teinte ?? Couleurs.encreAttenuee),
      ),
    );
  }
}
