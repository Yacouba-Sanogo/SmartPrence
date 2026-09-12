import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/seance.dart';
import '../../services/etudiant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';

/// Emploi du temps de l'étudiant.
///
/// <p>Une semaine à la fois, jour par jour. La semaine est la bonne unité : la
/// journée seule oblige à feuilleter pour préparer son sac du lendemain, et le mois
/// entier ne tient pas sur un téléphone.</p>
///
/// <p>Les séances viennent de la classe de l'étudiant, déduite de son jeton :
/// l'application n'a pas les moyens de demander l'emploi du temps d'une autre.</p>
class EmploiDuTempsScreen extends StatefulWidget {
  const EmploiDuTempsScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<EmploiDuTempsScreen> createState() => _EmploiDuTempsScreenState();
}

class _EmploiDuTempsScreenState extends State<EmploiDuTempsScreen> {
  late final EtudiantService _service = EtudiantService(widget.auth.client);

  /// Lundi de la semaine affichée.
  late DateTime _lundi = _lundiDe(DateTime.now());

  List<Seance> _seances = const [];
  bool _chargement = true;
  String? _erreur;

  /// Vrai tant qu'aucune semaine n'a été obtenue.
  ///
  /// En changeant de semaine, la précédente reste affichée jusqu'à l'arrivée de
  /// la suivante : vider l'écran à chaque flèche le ferait clignoter.
  bool _vierge = true;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  static DateTime _lundiDe(DateTime jour) {
    final minuit = DateTime(jour.year, jour.month, jour.day);
    return minuit.subtract(Duration(days: minuit.weekday - 1));
  }

  DateTime get _dimanche => _lundi.add(const Duration(days: 6));

  Future<void> _charger() async {
    setState(() {
      _chargement = true;
      _erreur = null;
    });
    try {
      final seances = await _service.monEmploiDuTemps(debut: _lundi, fin: _dimanche);
      if (!mounted) return;
      setState(() {
        _seances = seances;
        _chargement = false;
        _vierge = false;
      });
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _chargement = false;
      });
    }
  }

  void _decaler(int semaines) {
    setState(() => _lundi = _lundi.add(Duration(days: 7 * semaines)));
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

  Widget _entete() {
    final semaineCourante = _lundiDe(DateTime.now()) == _lundi;

    return EnteteDegrade(
      debordement: 32,
      padding: const EdgeInsets.fromLTRB(
          Espaces.lg, Espaces.md, Espaces.lg, Espaces.md),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('Emploi du temps',
              style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 21)),
          const SizedBox(height: 3),
          Text(
            semaineCourante ? 'Cette semaine' : 'Semaine du ${Dates.courte(_lundi)}',
            style: Typo.legende.copyWith(
                color: Colors.white.withValues(alpha: 0.72), fontSize: 12),
          ),
          const SizedBox(height: Espaces.lg),
          _navigationSemaine(semaineCourante),
        ],
      ),
    );
  }

  Widget _navigationSemaine(bool semaineCourante) {
    return Row(
      children: [
        _fleche(Icons.chevron_left_rounded, () => _decaler(-1)),
        Expanded(
          child: Center(
            child: Text(
              '${Dates.courte(_lundi)} — ${Dates.courte(_dimanche)}',
              style: Typo.libelle.copyWith(
                  fontSize: 13.5, fontWeight: FontWeight.w600, color: Colors.white),
            ),
          ),
        ),
        _fleche(Icons.chevron_right_rounded, () => _decaler(1)),
        if (!semaineCourante) ...[
          const SizedBox(width: Espaces.sm - 2),
          GestureDetector(
            onTap: () {
              setState(() => _lundi = _lundiDe(DateTime.now()));
              _charger();
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 6),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(Rayons.pilule),
              ),
              child: Text('Auj.',
                  style: Typo.legende.copyWith(
                      fontSize: 11.5,
                      fontWeight: FontWeight.w700,
                      color: Couleurs.indigo700)),
            ),
          ),
        ],
      ],
    );
  }

  Widget _fleche(IconData icone, VoidCallback surTap) {
    return GestureDetector(
      onTap: surTap,
      child: Container(
        width: 32,
        height: 32,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.14),
          borderRadius: BorderRadius.circular(Rayons.pilule),
        ),
        child: Icon(icone, size: 20, color: Colors.white),
      ),
    );
  }

  Widget _corps() {
    return TransitionContenu(enfant: _contenu());
  }

  Widget _contenu() {
    if (_chargement && _vierge) {
      return const Center(
        key: ValueKey('chargement'),
        child: CircularProgressIndicator(color: Couleurs.royal600),
      );
    }

    if (_erreur != null) {
      return ListView(
        key: const ValueKey('erreur'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Emploi du temps indisponible',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }

    if (_seances.isEmpty) {
      return ListView(
        key: ValueKey('vide-${_lundi.toIso8601String()}'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          const EtatVide(
            icone: Icons.event_available_outlined,
            titre: 'Aucun cours cette semaine',
            detail: 'Les séances apparaîtront ici dès que la scolarité les aura '
                'planifiées.',
          ),
        ],
      );
    }

    // Regroupées par jour : un emploi du temps se lit par journées, pas en liste
    // continue où le passage de mardi à mercredi ne se voit plus.
    final jours = <DateTime, List<Seance>>{};
    for (final seance in _seances) {
      final jour = DateTime(seance.debut.year, seance.debut.month, seance.debut.day);
      jours.putIfAbsent(jour, () => []).add(seance);
    }
    final cles = jours.keys.toList()..sort();

    return ListView.builder(
      // La clé porte la semaine : le fondu et la cascade rejouent à chaque flèche.
      key: ValueKey('semaine-${_lundi.toIso8601String()}'),
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      itemCount: cles.length,
      itemBuilder: (_, index) => ApparitionEnCascade(
        rang: index,
        enfant: _journee(cles[index], jours[cles[index]]!),
      ),
    );
  }

  Widget _journee(DateTime jour, List<Seance> seances) {
    final aujourdhui = Dates.estAujourdhui(jour);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(
              left: Espaces.sm, top: Espaces.sm, bottom: Espaces.sm),
          child: Row(
            children: [
              Text(
                Dates.jourEtDate(jour),
                style: Typo.suretitre.copyWith(
                    color: aujourdhui ? Couleurs.indigo700 : Couleurs.encreAttenuee),
              ),
              if (aujourdhui) ...[
                const SizedBox(width: Espaces.sm),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                  decoration: BoxDecoration(
                    color: Couleurs.indigo50,
                    borderRadius: BorderRadius.circular(Rayons.pilule),
                  ),
                  child: Text("Aujourd'hui",
                      style: Typo.legende.copyWith(
                          fontSize: 9.5,
                          fontWeight: FontWeight.w700,
                          color: Couleurs.indigo600)),
                ),
              ],
            ],
          ),
        ),
        ...seances.map(_carteSeance),
        const SizedBox(height: Espaces.sm),
      ],
    );
  }

  Widget _carteSeance(Seance seance) {
    final enCours = seance.seDerouleMaintenant;

    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm),
      child: Carte(
        rayon: Rayons.tuile + 2,
        padding: const EdgeInsets.symmetric(
            horizontal: Espaces.md + 1, vertical: Espaces.md),
        enfant: Row(
          children: [
            // Colonne horaire : l'heure est la première chose qu'on cherche.
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(Dates.heure(seance.debut),
                    style: Typo.mono(14,
                        graisse: FontWeight.w700,
                        couleur: enCours ? Couleurs.indigo600 : Couleurs.encre)),
                const SizedBox(height: 2),
                Text(Dates.heure(seance.fin),
                    style: Typo.mono(11, couleur: Couleurs.encrePale)),
              ],
            ),
            const SizedBox(width: Espaces.md),
            Container(
              width: 3,
              height: 38,
              decoration: BoxDecoration(
                color: enCours ? Couleurs.indigo500 : seance.statut.teinte,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(width: Espaces.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(seance.matiere,
                      style: Typo.libelle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 3),
                  Text(
                    [
                      if (seance.salle != null) seance.salle!,
                      seance.creneau,
                    ].join(' · '),
                    style: Typo.legendePale,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            if (enCours)
              Pastille(
                compact: true,
                texte: 'En cours',
                teinte: Couleurs.indigo600,
                fond: Couleurs.indigo50,
              )
            else if (seance.statut == StatutSeance.annulee)
              Pastille(
                compact: true,
                texte: 'Annulée',
                teinte: Couleurs.danger,
                fond: Couleurs.dangerFond,
              ),
          ],
        ),
      ),
    );
  }
}
