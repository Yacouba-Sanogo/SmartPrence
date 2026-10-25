import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/annonce.dart';
import '../../models/presence.dart';
import '../../models/seance.dart';
import '../../models/statistiques.dart';
import '../../services/etudiant_service.dart';
import '../../widgets/accueil_etudiant.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import '../../widgets/squelette.dart';
import '../bibliotheque_screen.dart';
import 'shell_etudiant.dart';
import 'supports_screen.dart';

/// Accueil de l'étudiant.
///
/// <p>L'écran répond à une seule question, posée en trois secondes dans un couloir :
/// « où j'en suis ». D'où l'anneau en tête, la semaine en un coup d'œil juste à côté,
/// puis les trois chiffres, les accès rapides et, tout en bas, le détail.</p>
class AccueilEtudiantScreen extends StatefulWidget {
  const AccueilEtudiantScreen({super.key, required this.auth, this.surNaviguer});

  final AuthService auth;

  /// Ouvre un autre onglet du shell depuis un raccourci.
  final ValueChanged<int>? surNaviguer;

  @override
  State<AccueilEtudiantScreen> createState() => _AccueilEtudiantScreenState();
}

class _AccueilEtudiantScreenState extends State<AccueilEtudiantScreen> {
  late final EtudiantService _service = EtudiantService(widget.auth.client);

  Assiduite _assiduite = Assiduite.vide;
  List<Presence> _derniers = const [];
  Seance? _prochaine;
  List<Annonce> _notifications = const [];
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
      // Les deux appels partent ensemble : l'un ne dépend pas de l'autre.
      // Séance suivante et notifications sont un plus : leur échec ne doit pas vider l'écran.
      final resultats = await Future.wait([
        _service.monAssiduite(),
        _service.mesPresences(taille: 20),
        _service.monEmploiDuTemps().then<List<Seance>>((s) => s).catchError((_) => <Seance>[]),
        _service.notifications().then<List<Annonce>>((n) => n).catchError((_) => <Annonce>[]),
      ]);
      if (!mounted) return;
      final maintenant = DateTime.now();
      final seances = (resultats[2] as List<Seance>)
          .where((s) => s.statut != StatutSeance.annulee && s.fin.isAfter(maintenant))
          .toList()
        ..sort((a, b) => a.debut.compareTo(b.debut));
      setState(() {
        _assiduite = resultats[0] as Assiduite;
        _derniers = (resultats[1] as PagePresences).elements;
        _prochaine = seances.isEmpty ? null : seances.first;
        _notifications = resultats[3] as List<Annonce>;
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

  /// Dernier relevé du jour, s'il existe — le fait le plus frais de l'écran.
  Presence? get _passageDuJour {
    for (final releve in _derniers) {
      if (Dates.estAujourdhui(releve.date)) return releve;
    }
    return null;
  }

  /// État de chaque jour ouvré de la semaine courante.
  List<({String lettre, EtatJour etat})> get _semaine {
    const lettres = ['L', 'M', 'M', 'J', 'V'];
    final aujourdhui = DateTime.now();
    final lundi = aujourdhui.subtract(Duration(days: aujourdhui.weekday - 1));

    return List.generate(5, (i) {
      final jour = lundi.add(Duration(days: i));
      if (jour.isAfter(aujourdhui) && !Dates.memeJour(jour, aujourdhui)) {
        return (lettre: lettres[i], etat: EtatJour.aVenir);
      }
      final releves = _derniers.where((r) => Dates.memeJour(r.date, jour));
      if (releves.isEmpty) {
        return (lettre: lettres[i], etat: EtatJour.absent);
      }
      final enRetard = releves.any((r) => r.statut == StatutPresence.retard);
      return (lettre: lettres[i], etat: enRetard ? EtatJour.retard : EtatJour.present);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      body: RefreshIndicator(
        onRefresh: _charger,
        color: Couleurs.royal600,
        child: CustomScrollView(
          // Toujours défilable : sans quoi le geste de rafraîchissement ne
          // fonctionnerait pas sur un écran dont le contenu tient en entier.
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(child: _entete()),
            if (_chargement)
              const SliverPadding(
                padding: EdgeInsets.fromLTRB(Espaces.lg, Espaces.lg, Espaces.lg, 130),
                sliver: SliverToBoxAdapter(child: SqueletteAccueil()),
              )
            else if (_erreur != null)
              SliverFillRemaining(
                hasScrollBody: false,
                child: EtatVide(
                  enErreur: true,
                  icone: Icons.cloud_off_rounded,
                  titre: 'Chargement impossible',
                  detail: _erreur,
                  libelleAction: 'Réessayer',
                  surAction: _charger,
                ),
              )
            else
              SliverPadding(
                // Marge basse : la barre de navigation flotte au-dessus du contenu.
                padding: const EdgeInsets.fromLTRB(Espaces.lg, Espaces.lg, Espaces.lg, 130),
                sliver: SliverList.list(children: _contenu()),
              ),
          ],
        ),
      ),
    );
  }

  Widget _entete() {
    final passage = _passageDuJour;

    return EnteteAccueil(
      profil: widget.auth.profil,
      nonLues: _notifications.where((n) => !n.lue).length,
      surNotifications: _ouvrirNotifications,
      surProfil: () => widget.surNaviguer?.call(OngletsEtudiant.profil),
      passage: BandeauEntete(
        icone: Icons.fingerprint_rounded,
        texte: passage == null ? 'Aucun passage aujourd’hui' : 'Dernier passage · aujourd’hui ${passage.heure}',
        pastille: passage == null
            ? null
            : Pastille(
                compact: true,
                texte: passage.statut == StatutPresence.retard ? 'En retard' : 'À l’heure',
                teinte: passage.statut == StatutPresence.retard ? Couleurs.alerte : Couleurs.succes,
                fond: passage.statut == StatutPresence.retard ? Couleurs.alerteFond : Couleurs.succesFond,
              ),
      ),
    );
  }

  Future<void> _ouvrirNotifications() async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Couleurs.fond,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(Rayons.bloc + 4)),
      ),
      builder: (feuille) => FeuilleNotifications(
        notifications: _notifications,
        surToutLire: () async {
          Navigator.pop(feuille);
          try {
            await _service.toutMarquerLu();
          } on ErreurApi {
            // Les compteurs se corrigent au prochain rafraîchissement.
          }
          if (mounted) {
            setState(() => _notifications = [for (final n in _notifications) n.commeLue()]);
          }
        },
      ),
    );
  }

  List<Widget> _contenu() {
    return [
      CarteAssiduite(assiduite: _assiduite, semaine: _semaine),
      if (!_assiduite.sansHistorique) ...[
        const SizedBox(height: Espaces.sm + 2),
        _statistiques(),
      ],
      const SizedBox(height: Espaces.md + 2),
      BanniereSeance(
        seance: _prochaine,
        surTap: () => widget.surNaviguer?.call(OngletsEtudiant.emploiDuTemps),
      ),
      const SizedBox(height: Espaces.xl + 4),
      const TitreSection('Accès rapide'),
      _raccourcis(),
      const SizedBox(height: Espaces.xl + 4),
      TitreSection(
        'Activité récente',
        icone: Icons.history_rounded,
        action: _notifications.isEmpty ? null : 'Voir tout',
        surAction: _ouvrirNotifications,
      ),
      ActiviteRecente(notifications: _notifications, surTap: _ouvrirNotifications),
      const SizedBox(height: Espaces.xl + 4),
      _derniersReleves(),
    ];
  }

  /// Relevés de la semaine en cours pour un statut : le mouvement que montre chaque carte.
  int _cetteSemaine(StatutPresence statut) {
    final aujourdhui = DateTime.now();
    final lundi = DateTime(aujourdhui.year, aujourdhui.month, aujourdhui.day)
        .subtract(Duration(days: aujourdhui.weekday - 1));
    return _derniers.where((r) => r.statut == statut && !r.date.isBefore(lundi)).length;
  }

  Widget _statistiques() {
    return Row(
      children: [
        Expanded(
          child: CarteStatistique(
            icone: Icons.person_rounded,
            libelle: 'Présences',
            valeur: _assiduite.presents,
            total: _assiduite.total,
            cetteSemaine: _cetteSemaine(StatutPresence.present),
            teinte: Couleurs.succes,
            fond: Couleurs.succesFond,
          ),
        ),
        const SizedBox(width: Espaces.sm),
        Expanded(
          child: CarteStatistique(
            icone: Icons.schedule_rounded,
            libelle: 'Retards',
            valeur: _assiduite.retards,
            total: _assiduite.total,
            cetteSemaine: _cetteSemaine(StatutPresence.retard),
            teinte: Couleurs.alerte,
            fond: Couleurs.alerteFond,
          ),
        ),
        const SizedBox(width: Espaces.sm),
        Expanded(
          child: CarteStatistique(
            icone: Icons.close_rounded,
            libelle: 'Absences',
            valeur: _assiduite.absents,
            total: _assiduite.total,
            cetteSemaine: _cetteSemaine(StatutPresence.absent),
            teinte: Couleurs.danger,
            fond: Couleurs.dangerFond,
          ),
        ),
      ],
    );
  }

  Widget _raccourcis() {
    return Column(
      children: [
        RangeeRaccourcis(
          gauche: RaccourciAccueil(
            icone: Icons.calendar_month_rounded,
            libelle: 'Emploi du temps',
            detail: 'Consultez votre planning',
            surTap: () => widget.surNaviguer?.call(OngletsEtudiant.emploiDuTemps),
          ),
          droite: RaccourciAccueil(
            icone: Icons.school_rounded,
            libelle: 'Mes notes',
            detail: 'Consultez vos résultats',
            surTap: () => widget.surNaviguer?.call(OngletsEtudiant.notes),
          ),
        ),
        const SizedBox(height: Espaces.sm + 2),
        RangeeRaccourcis(
          gauche: RaccourciAccueil(
            icone: Icons.folder_copy_rounded,
            libelle: 'Supports de cours',
            detail: 'Téléchargez vos ressources',
            surTap: () => Navigator.of(context).push<void>(
              routeDouce<void>((_) => SupportsEtudiantScreen(auth: widget.auth)),
            ),
          ),
          droite: RaccourciAccueil(
            icone: Icons.local_library_rounded,
            libelle: 'Bibliothèque',
            detail: 'Explorez la bibliothèque',
            surTap: () => Navigator.of(context).push<void>(
              routeDouce<void>((_) => BibliothequeScreen(auth: widget.auth)),
            ),
          ),
        ),
      ],
    );
  }

  Widget _derniersReleves() {
    final recents = _derniers.take(5).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TitreSection(
          'Derniers relevés',
          action: recents.isEmpty ? null : 'Tout voir',
          surAction: () => widget.surNaviguer?.call(OngletsEtudiant.releves),
        ),
        if (recents.isEmpty)
          const SectionVide(
            icone: Icons.event_note_outlined,
            titre: 'Aucun relevé pour le moment',
            detail: 'Vos passages devant les lecteurs apparaîtront ici, avec la matière et la salle.',
          )
        else
          Carte(
            padding: EdgeInsets.zero,
            enfant: Column(
              children: [
                for (var i = 0; i < recents.length; i++) ...[
                  if (i > 0)
                    const Divider(height: 1, thickness: 1, indent: Espaces.lg, endIndent: Espaces.lg, color: Couleurs.traitLeger),
                  _ligneReleve(recents[i]),
                ],
              ],
            ),
          ),
      ],
    );
  }

  Widget _ligneReleve(Presence presence) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Espaces.lg, vertical: Espaces.md),
      child: Row(
        children: [
          Container(
            width: 46,
            height: 40,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: presence.statut.fond,
              borderRadius: BorderRadius.circular(Rayons.md - 2),
            ),
            child: Text(presence.heure,
                style: Typo.mono(12, graisse: FontWeight.w600, couleur: presence.statut.teinte)),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  presence.matiere ?? 'Séance non identifiée',
                  style: Typo.libelle,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  [Dates.courte(presence.date), presence.salle].whereType<String>().join(' · '),
                  style: Typo.legendePale,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          const SizedBox(width: Espaces.sm),
          Pastille(
            texte: presence.statut.libelle,
            teinte: presence.statut.teinte,
            fond: presence.statut.fond,
          ),
        ],
      ),
    );
  }
}
