import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/presence.dart';
import '../../models/seance.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import 'cahier_sheet.dart';
import 'signalement_sheet.dart';

/// Filtres proposés sur la liste des étudiants.
enum _Filtre { tous, presents, absents, nonEnroles }

extension on _Filtre {
  String get libelle => switch (this) {
        _Filtre.tous => 'Tous',
        _Filtre.presents => 'Présents',
        _Filtre.absents => 'Absents',
        _Filtre.nonEnroles => 'Non enrôlés',
      };
}

/// Feuille de présence d'une séance.
///
/// Elle est **constatée, pas saisie** : l'enseignant ne coche personne. Il lit ce que le
/// lecteur a relevé et signale l'écart s'il y en a un. C'est ce qui sépare ce système
/// d'un appel manuel informatisé, et ce qui donne au relevé sa valeur probante.
class FeuilleScreen extends StatefulWidget {
  const FeuilleScreen({super.key, required this.auth, required this.seance});

  final AuthService auth;
  final Seance seance;

  @override
  State<FeuilleScreen> createState() => _FeuilleScreenState();
}

class _FeuilleScreenState extends State<FeuilleScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  late Seance _seance = widget.seance;
  bool _declaration = false;

  FeuilleSeance? _feuille;
  _Filtre _filtre = _Filtre.tous;
  bool _chargement = true;
  String? _erreur;

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
      final feuille = await _service.feuille(widget.seance.id);
      if (!mounted) return;
      setState(() {
        _feuille = feuille;
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

  /// Lignes affichées, triées : les situations à vérifier remontent en tête.
  List<LigneFeuille> get _lignesFiltrees {
    final lignes = _feuille?.lignes ?? const <LigneFeuille>[];
    final retenues = lignes.where((ligne) => switch (_filtre) {
          _Filtre.tous => true,
          _Filtre.presents => ligne.statut == StatutPresence.present ||
              ligne.statut == StatutPresence.retard,
          _Filtre.absents => ligne.statut == StatutPresence.absent,
          _Filtre.nonEnroles => !ligne.enrole,
        });

    return retenues.toList()
      ..sort((a, b) {
        // Non enrôlés d'abord : ce sont les seuls dont le relevé ne prouve rien.
        if (a.enrole != b.enrole) return a.enrole ? 1 : -1;
        final rang = _rang(a.statut).compareTo(_rang(b.statut));
        if (rang != 0) return rang;
        return a.nom.toLowerCase().compareTo(b.nom.toLowerCase());
      });
  }

  static int _rang(StatutPresence statut) => switch (statut) {
        StatutPresence.absent => 0,
        StatutPresence.retard => 1,
        StatutPresence.present => 2,
        _ => 3,
      };

  /// Déclare la séance effectuée. Le serveur refuse une séance annulée ou pas encore
  /// commencée ; son message est affiché tel quel.
  Future<void> _declarer() async {
    if (_declaration) return;
    setState(() => _declaration = true);
    try {
      final mise = await _service.declarerSeance(_seance.id);
      if (!mounted) return;
      setState(() {
        _seance = mise;
        _declaration = false;
      });
      _message('Séance déclarée effectuée.');
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() => _declaration = false);
      _message(e.message);
    }
  }

  Future<void> _ouvrirCahier() async {
    final enregistre = await ouvrirCahier(context, service: _service, seance: _seance);
    if (enregistre && mounted) _message('Cahier de textes enregistré.');
  }

  void _message(String texte) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(texte), behavior: SnackBarBehavior.floating),
    );
  }

  Future<void> _signaler({LigneFeuille? etudiant}) async {
    final feuille = _feuille;
    if (feuille == null) return;

    final depose = await ouvrirSignalement(
      context,
      service: _service,
      seanceId: widget.seance.id,
      // Seuls les absents sont proposés : accepter un signalement pour quelqu'un qui a
      // déjà un relevé est refusé côté serveur. Le laisser choisir mènerait à un
      // signalement voué à être écarté, après plusieurs jours d'attente.
      etudiants: feuille.lignes
          .where((ligne) => ligne.statut == StatutPresence.absent)
          .toList(),
      etudiantPreselectionne: etudiant,
    );

    if (!depose || !mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Signalement transmis à la scolarité.'),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final feuille = _feuille;

    return Scaffold(
      backgroundColor: Couleurs.fond,
      floatingActionButton: feuille == null ? null : _boutonSignaler(),
      body: RefreshIndicator(
        onRefresh: _charger,
        color: Couleurs.indigo600,
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(child: _entete(feuille)),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(
                  Espaces.md + 2, 0, Espaces.md + 2, Espaces.xxxl * 2),
              sliver: SliverToBoxAdapter(
                child: Transform.translate(
                  offset: const Offset(0, -34),
                  child: _corps(),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Bouton de signalement — la seule teinte chaude de l'application.
  ///
  /// Signaler interrompt le cours normal des choses : lui donner la couleur de tous
  /// les autres boutons le noierait parmi eux.
  Widget _boutonSignaler() {
    return Container(
      decoration: BoxDecoration(
        gradient: Couleurs.degradeSignalement,
        borderRadius: BorderRadius.circular(Rayons.bloc),
        boxShadow: Couleurs.ombreDanger,
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(Rayons.bloc),
        child: InkWell(
          onTap: () => _signaler(),
          borderRadius: BorderRadius.circular(Rayons.bloc),
          child: Padding(
            padding: const EdgeInsets.symmetric(
                horizontal: Espaces.xl - 1, vertical: Espaces.md),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.flag_rounded, size: 17, color: Colors.white),
                const SizedBox(width: 7),
                Text('Signaler',
                    style: Typo.bouton.copyWith(fontSize: 12.5)),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _entete(FeuilleSeance? feuille) {
    return EnteteDegrade(
      debordement: 40,
      padding: const EdgeInsets.fromLTRB(
          Espaces.sm, Espaces.sm, Espaces.lg, Espaces.lg),
      enfant: Column(
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
                    Text(widget.seance.matiere,
                        style: Typo.titreSection
                            .copyWith(color: Colors.white, fontSize: 15),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 2),
                    Text(
                      [
                        widget.seance.classeCode,
                        widget.seance.creneau,
                        feuille?.salle,
                      ].whereType<String>().where((t) => t.isNotEmpty).join(' · '),
                      style: Typo.mono(11,
                          couleur: Colors.white.withValues(alpha: 0.7)),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (feuille != null) ...[
            const SizedBox(height: Espaces.lg),
            _infos(feuille),
            const SizedBox(height: Espaces.md + 2),
            Container(
              padding: const EdgeInsets.symmetric(vertical: Espaces.md),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(Rayons.tuile),
              ),
              child: Row(
                children: [
                  _chiffre(_deuxChiffres(feuille.presents), 'Présents',
                      const Color(0xFF7DD3FC)),
                  _chiffre(_deuxChiffres(feuille.retards), 'Retards',
                      Couleurs.alerteVif),
                  _chiffre(_deuxChiffres(feuille.absents), 'Absences',
                      const Color(0xFFFF9AA8)),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  /// Salle, classe, horaire et effectif, en une ligne soulignée.
  Widget _infos(FeuilleSeance feuille) {
    Widget bloc(String titre, String valeur) => Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(titre,
                  style: Typo.legende.copyWith(
                      fontSize: 10, color: Colors.white.withValues(alpha: 0.6))),
              const SizedBox(height: 3),
              Text(valeur,
                  style: Typo.libelle.copyWith(
                      color: Colors.white,
                      fontSize: 13.5,
                      fontWeight: FontWeight.w700),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis),
            ],
          ),
        );

    return Container(
      padding: const EdgeInsets.only(bottom: Espaces.md),
      decoration: BoxDecoration(
        border: Border(
            bottom: BorderSide(color: Colors.white.withValues(alpha: 0.25))),
      ),
      child: Row(
        children: [
          bloc('Salle', feuille.salle ?? '—'),
          bloc('Classe', widget.seance.classeCode),
          bloc('Horaire', widget.seance.creneau),
          bloc('Total', '${feuille.effectif}'),
        ],
      ),
    );
  }

  Widget _chiffre(String valeur, String libelle, Color teinte) {
    return Expanded(
      child: Column(
        children: [
          Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(color: teinte, shape: BoxShape.circle),
          ),
          const SizedBox(height: 6),
          Text(valeur,
              style: Typo.mono(20, graisse: FontWeight.w700, couleur: Colors.white)),
          const SizedBox(height: 2),
          Text(libelle,
              style: Typo.legende.copyWith(
                  fontSize: 10.5, color: Colors.white.withValues(alpha: 0.65))),
        ],
      ),
    );
  }

  /// Deux chiffres minimum : une colonne de nombres alignés se lit mieux.
  static String _deuxChiffres(int valeur) =>
      valeur < 10 ? '0$valeur' : '$valeur';

  Widget _corps() {
    if (_chargement) {
      return const Padding(
        padding: EdgeInsets.only(top: Espaces.xxxl * 2),
        child: Center(child: CircularProgressIndicator(color: Couleurs.indigo600)),
      );
    }

    final feuille = _feuille;
    if (_erreur != null || feuille == null) {
      return EtatVide(
        enErreur: true,
        icone: Icons.cloud_off_rounded,
        titre: 'Feuille indisponible',
        detail: _erreur,
        libelleAction: 'Réessayer',
        surAction: _charger,
      );
    }

    final lignes = _lignesFiltrees;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Le contenu est remonté de 34 px sur l'en-tête : sans cette marge, la première carte
        // passait sous le dégradé et son bouton était à moitié masqué.
        const SizedBox(height: Espaces.xl),
        _actionsSeance(),
        const SizedBox(height: Espaces.md - 1),
        if (feuille.absentsNonEnroles > 0) ...[
          _avertissementNonEnroles(feuille.absentsNonEnroles),
          const SizedBox(height: Espaces.md - 1),
        ],
        _filtres(feuille),
        const SizedBox(height: Espaces.md - 1),
        if (lignes.isEmpty)
          Carte(
            padding: const EdgeInsets.symmetric(vertical: Espaces.xxl),
            enfant: Text(
              feuille.effectif == 0
                  ? 'Aucun étudiant inscrit dans cette classe.'
                  : 'Aucun étudiant dans cette catégorie.',
              textAlign: TextAlign.center,
              style: Typo.corpsAttenue,
            ),
          )
        else
          _tableau(lignes),
      ],
    );
  }

  /// Ce que l'enseignant fait de sa séance : la déclarer tenue, puis consigner le cours.
  Widget _actionsSeance() {
    final declaree = _seance.declaree;
    final annulee = _seance.statut == StatutSeance.annulee;

    return Carte(
      padding: const EdgeInsets.all(Espaces.md),
      enfant: Row(
        children: [
          Expanded(
            child: declaree
                ? const Align(
                    alignment: Alignment.centerLeft,
                    child: Pastille(
                      texte: 'Séance effectuée',
                      teinte: Couleurs.succes,
                      fond: Couleurs.succesFond,
                      icone: Icons.check_circle_rounded,
                    ),
                  )
                : FilledButton.icon(
                    onPressed: annulee || !_seance.commencee || _declaration ? null : _declarer,
                    style: FilledButton.styleFrom(minimumSize: const Size(0, 44)),
                    icon: _declaration
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Icon(Icons.task_alt_rounded, size: 18),
                    label: Text(annulee
                        ? 'Séance annulée'
                        : !_seance.commencee
                            ? 'Pas encore commencée'
                            : 'Déclarer effectuée'),
                  ),
          ),
          const SizedBox(width: Espaces.md),
          OutlinedButton.icon(
            onPressed: _seance.commencee && !annulee ? _ouvrirCahier : null,
            style: OutlinedButton.styleFrom(minimumSize: const Size(0, 44)),
            icon: const Icon(Icons.menu_book_rounded, size: 18),
            label: const Text('Cahier'),
          ),
        ],
      ),
    );
  }

  /// Le point le plus important de l'écran.
  ///
  /// Sans cet avertissement, l'enseignant lirait un absent non enrôlé comme un absent
  /// ordinaire — alors que le lecteur, faute d'empreinte associée, était incapable de
  /// l'identifier. Le relevé ne dit rien de son assiduité.
  Widget _avertissementNonEnroles(int nombre) {
    return Bandeau.attention(
      message: nombre == 1
          ? "1 absent n'est pas enrôlé : le lecteur ne pouvait pas l’identifier. "
              'Son absence de relevé ne dit rien de sa présence.'
          : "$nombre absents ne sont pas enrôlés : le lecteur ne pouvait pas les "
              'identifier. Leur absence de relevé ne dit rien de leur présence.',
    );
  }

  Widget _filtres(FeuilleSeance feuille) {
    int compte(_Filtre filtre) => switch (filtre) {
          _Filtre.tous => feuille.lignes.length,
          _Filtre.presents => feuille.presents + feuille.retards,
          _Filtre.absents => feuille.absents,
          _Filtre.nonEnroles => feuille.lignes.where((l) => !l.enrole).length,
        };

    return SizedBox(
      height: 32,
      child: ListView(
        scrollDirection: Axis.horizontal,
        physics: const BouncingScrollPhysics(),
        children: _Filtre.values.map((filtre) {
          final nombre = compte(filtre);
          final actif = _filtre == filtre;
          // Une catégorie vide reste visible mais inerte : sa présence
          // renseigne autant que son contenu.
          final inerte = nombre == 0;

          return Padding(
            padding: const EdgeInsets.only(right: Espaces.sm - 2),
            child: GestureDetector(
              onTap: inerte ? null : () => setState(() => _filtre = filtre),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 6),
                decoration: BoxDecoration(
                  gradient: actif ? Couleurs.degradeNavigation : null,
                  color: actif ? null : Couleurs.carte,
                  borderRadius: BorderRadius.circular(Rayons.pilule),
                  boxShadow: actif ? Couleurs.ombreIndigo : Couleurs.ombreCarte,
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      filtre.libelle,
                      style: Typo.legende.copyWith(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w500,
                        color: actif
                            ? Colors.white
                            : (inerte ? Couleurs.encrePale : Couleurs.encreAttenuee),
                      ),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      '$nombre',
                      style: Typo.mono(11.5,
                          graisse: FontWeight.w600,
                          couleur: actif
                              ? Colors.white
                              : (inerte ? Couleurs.encrePale : Couleurs.indigo600)),
                    ),
                  ],
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  /// Tableau unique N° · Nom · Matricule · Statut, dans une carte blanche.
  Widget _tableau(List<LigneFeuille> lignes) {
    final entete = Typo.suretitre.copyWith(fontSize: 10);

    return Carte(
      rayon: Rayons.bloc,
      ombre: Couleurs.ombreFlottante,
      padding: const EdgeInsets.fromLTRB(
          Espaces.lg, Espaces.lg, Espaces.lg, Espaces.sm),
      enfant: Column(
        children: [
          Row(children: [
            SizedBox(width: 30, child: Text('N°', style: entete)),
            Expanded(flex: 5, child: Text('Nom', style: entete)),
            Expanded(flex: 4, child: Text('Matricule', style: entete)),
            SizedBox(
                width: 76,
                child: Text('Statut', style: entete, textAlign: TextAlign.end)),
          ]),
          Divider(height: Espaces.lg, color: Couleurs.traitLeger),
          for (var i = 0; i < lignes.length; i++) _ligne(i + 1, lignes[i]),
        ],
      ),
    );
  }

  Widget _ligne(int rang, LigneFeuille ligne) {
    final aSignaler = ligne.statut == StatutPresence.absent;
    final style = Typo.libelle.copyWith(
        fontSize: 12.5, color: Couleurs.royal800, fontWeight: FontWeight.w600);

    return InkWell(
      onTap: aSignaler ? () => _signaler(etudiant: ligne) : null,
      borderRadius: BorderRadius.circular(Rayons.sm),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 9),
        child: Row(
          children: [
            SizedBox(
                width: 30,
                child: Text(rang.toString().padLeft(2, '0'), style: style)),
            Expanded(
              flex: 5,
              child: Row(
                children: [
                  Flexible(
                    child: Text(ligne.nomComplet,
                        style: style, maxLines: 1, overflow: TextOverflow.ellipsis),
                  ),
                  if (!ligne.enrole) ...[
                    const SizedBox(width: 4),
                    const Tooltip(
                      message: 'Non enrôlé',
                      child: Icon(Icons.fingerprint_rounded,
                          size: 15, color: Couleurs.alerteVif),
                    ),
                  ],
                  if (ligne.estRegularise) ...[
                    const SizedBox(width: 4),
                    const Tooltip(
                      message: 'Régularisé',
                      child: Icon(Icons.edit_note_rounded,
                          size: 16, color: Couleurs.royal500),
                    ),
                  ],
                ],
              ),
            ),
            Expanded(
              flex: 4,
              child: Text(ligne.matricule,
                  style: Typo.mono(11.5, couleur: Couleurs.royal600),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis),
            ),
            SizedBox(
              width: 76,
              child: Align(
                alignment: Alignment.centerRight,
                child: Pastille(
                  compact: true,
                  texte: ligne.statut.libelle,
                  teinte: ligne.statut.teinte,
                  fond: ligne.statut.fond,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
