import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/presence.dart';
import '../../models/profil.dart';
import '../../models/statistiques.dart';
import '../../services/etudiant_service.dart';
import '../../widgets/anneau.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import 'shell_etudiant.dart';

/// Accueil de l'étudiant.
///
/// <p>L'écran répond à une seule question, posée en trois secondes dans un couloir :
/// « où j'en suis ». D'où l'anneau en tête, la semaine en un coup d'œil juste à côté,
/// et le détail seulement ensuite.</p>
class AccueilEtudiantScreen extends StatefulWidget {
  const AccueilEtudiantScreen({super.key, required this.auth, this.surNaviguer});

  final AuthService auth;

  /// Ouvre un autre onglet du shell depuis une tuile d'accès rapide.
  final ValueChanged<int>? surNaviguer;

  @override
  State<AccueilEtudiantScreen> createState() => _AccueilEtudiantScreenState();
}

class _AccueilEtudiantScreenState extends State<AccueilEtudiantScreen> {
  late final EtudiantService _service = EtudiantService(widget.auth.client);

  Assiduite _assiduite = Assiduite.vide;
  List<Presence> _derniers = const [];
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
      final resultats = await Future.wait([
        _service.monAssiduite(),
        _service.mesPresences(taille: 20),
      ]);
      if (!mounted) return;
      setState(() {
        _assiduite = resultats[0] as Assiduite;
        _derniers = (resultats[1] as PagePresences).elements;
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
    final profil = widget.auth.profil;

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: _charger,
        color: Couleurs.indigo600,
        child: CustomScrollView(
          // Toujours défilable : sans quoi le geste de rafraîchissement ne
          // fonctionnerait pas sur un écran dont le contenu tient en entier.
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(child: _entete(profil)),
            if (_chargement)
              const SliverFillRemaining(
                hasScrollBody: false,
                child: Center(
                  child: CircularProgressIndicator(color: Couleurs.indigo600),
                ),
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
                // Le contenu remonte sur le dégradé : c'est le chevauchement qui
                // donne sa profondeur à l'écran.
                padding: const EdgeInsets.fromLTRB(
                    Espaces.md + 2, 0, Espaces.md + 2, Espaces.xxxl * 3),
                sliver: SliverList.list(children: _contenu()),
              ),
          ],
        ),
      ),
    );
  }

  Widget _entete(Profil? profil) {
    final passage = _passageDuJour;

    return EnteteDegrade(
      padding: const EdgeInsets.fromLTRB(
          Espaces.lg, Espaces.lg, Espaces.lg, Espaces.lg),
      enfant: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          LigneIdentite(
            initiales: profil?.initiales ?? '?',
            surtitre: 'Bonjour,',
            titre: profil?.prenom ?? 'Étudiant',
            action: ActionEntete(
              icone: Icons.person_outline_rounded,
              surTap: () => widget.surNaviguer?.call(OngletsEtudiant.profil),
            ),
          ),
          const SizedBox(height: Espaces.md + 2),
          BandeauEntete(
            icone: Icons.fingerprint_rounded,
            texte: passage == null
                ? 'Aucun passage aujourd’hui'
                : 'Dernier passage · aujourd’hui ${passage.heure}',
            pastille: passage == null
                ? null
                : Pastille(
                    compact: true,
                    texte: passage.statut == StatutPresence.retard
                        ? 'En retard'
                        : 'À l’heure',
                    teinte: passage.statut == StatutPresence.retard
                        ? Couleurs.alerte
                        : Couleurs.succes,
                    fond: passage.statut == StatutPresence.retard
                        ? Couleurs.alerteFond
                        : Couleurs.succesFond,
                  ),
          ),
        ],
      ),
    );
  }

  List<Widget> _contenu() {
    return [
      Transform.translate(
        offset: const Offset(0, -40),
        child: Column(
          children: [
            _carteAssiduite(),
            const SizedBox(height: Espaces.sm + 2),
            _compteurs(),
            const SizedBox(height: Espaces.sm + 2),
            _tuiles(),
            const SizedBox(height: Espaces.xl),
            _derniersReleves(),
          ],
        ),
      ),
    ];
  }

  Widget _carteAssiduite() {
    if (_assiduite.sansHistorique) {
      return Carte(
        ombre: Couleurs.ombreFlottante,
        padding: const EdgeInsets.all(Espaces.lg),
        enfant: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: Couleurs.indigo50,
                borderRadius: BorderRadius.circular(Rayons.md),
              ),
              child: const Icon(Icons.fingerprint_rounded,
                  size: 22, color: Couleurs.indigo500),
            ),
            const SizedBox(width: Espaces.md),
            Expanded(
              child: Text(
                'Vos présences apparaîtront ici dès votre premier passage devant '
                'un lecteur.',
                style: Typo.corpsAttenue.copyWith(fontSize: 12.5),
              ),
            ),
          ],
        ),
      );
    }

    return Carte(
      ombre: Couleurs.ombreFlottante,
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Row(
        children: [
          Anneau(valeur: _assiduite.taux, taille: 86),
          const SizedBox(width: Espaces.lg),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Cette semaine', style: Typo.corpsAttenue.copyWith(fontSize: 12.5)),
                const SizedBox(height: Espaces.sm + 1),
                Row(
                  children: [
                    for (final jour in _semaine) ...[
                      PointJour(lettre: jour.lettre, etat: jour.etat),
                      const SizedBox(width: 7),
                    ],
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _compteurs() {
    return Row(
      children: [
        Expanded(
          child: Compteur(
            valeur: '${_assiduite.presents}',
            libelle: 'Présences',
            teinte: Couleurs.succesVif,
          ),
        ),
        const SizedBox(width: Espaces.sm),
        Expanded(
          child: Compteur(
            valeur: '${_assiduite.retards}',
            libelle: 'Retards',
            teinte: Couleurs.alerteVif,
          ),
        ),
        const SizedBox(width: Espaces.sm),
        Expanded(
          child: Compteur(
            valeur: '${_assiduite.absents}',
            libelle: 'Absences',
            teinte: Couleurs.dangerVif,
          ),
        ),
      ],
    );
  }

  Widget _tuiles() {
    return Row(
      children: [
        Expanded(
          child: Tuile(
            icone: Icons.calendar_month_rounded,
            libelle: 'Emploi du temps',
            teinte: Couleurs.indigo600,
            fondIcone: Couleurs.indigo50,
            surTap: () => widget.surNaviguer?.call(OngletsEtudiant.emploiDuTemps),
          ),
        ),
        const SizedBox(width: Espaces.sm),
        Expanded(
          child: Tuile(
            icone: Icons.school_outlined,
            libelle: 'Mes notes',
            teinte: Couleurs.succes,
            fondIcone: Couleurs.succesFond,
            surTap: () => widget.surNaviguer?.call(OngletsEtudiant.notes),
          ),
        ),
        const SizedBox(width: Espaces.sm),
        Expanded(
          child: Tuile(
            icone: Icons.shield_outlined,
            libelle: 'Mon profil',
            teinte: Couleurs.alerte,
            fondIcone: Couleurs.alerteFond,
            surTap: () => widget.surNaviguer?.call(OngletsEtudiant.profil),
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
        Padding(
          padding: const EdgeInsets.only(left: Espaces.xs, bottom: Espaces.md - 2),
          child: Row(
            children: [
              Expanded(child: Text('Derniers relevés', style: Typo.titreSection)),
              if (recents.isNotEmpty)
                GestureDetector(
                  onTap: () => widget.surNaviguer?.call(OngletsEtudiant.releves),
                  child: Text('Tout voir',
                      style: Typo.legende.copyWith(
                          color: Couleurs.indigo600, fontWeight: FontWeight.w600)),
                ),
            ],
          ),
        ),
        if (recents.isEmpty)
          Carte(
            padding: const EdgeInsets.symmetric(vertical: Espaces.xxl),
            enfant: Text(
              'Aucun relevé enregistré.',
              textAlign: TextAlign.center,
              style: Typo.corpsAttenue,
            ),
          )
        else
          ...recents.map(_ligneReleve),
      ],
    );
  }

  Widget _ligneReleve(Presence presence) {
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm),
      child: Carte(
        rayon: Rayons.tuile + 1,
        padding: const EdgeInsets.symmetric(
            horizontal: Espaces.md + 1, vertical: Espaces.md - 2),
        enfant: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: presence.statut.fond,
                borderRadius: BorderRadius.circular(Rayons.md - 1),
              ),
              child: Text(presence.heure,
                  style: Typo.mono(11.5,
                      graisse: FontWeight.w600, couleur: presence.statut.teinte)),
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
                    [Dates.courte(presence.date), presence.salle]
                        .whereType<String>()
                        .join(' · '),
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
      ),
    );
  }
}
