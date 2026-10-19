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

  /// Jour retenu dans la bande de la semaine ; `null` montre toute la semaine.
  DateTime? _jourRetenu;

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
    setState(() {
      _lundi = _lundi.add(Duration(days: 7 * semaines));
      _jourRetenu = null;
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
          const SizedBox(height: Espaces.lg),
          _bandeSemaine(),
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

  /// Bande des sept jours : numéro du jour et un point par cours prévu.
  ///
  /// Toucher un jour ne montre que lui ; le toucher de nouveau rend la semaine.
  Widget _bandeSemaine() {
    const noms = ['LUN', 'MAR', 'MER', 'JEU', 'VEN', 'SAM', 'DIM'];

    return Row(
      children: [
        for (var i = 0; i < 7; i++)
          Expanded(child: _jourDeBande(noms[i], _lundi.add(Duration(days: i)))),
      ],
    );
  }

  Widget _jourDeBande(String nom, DateTime jour) {
    final cours = _seances.where((s) => Dates.memeJour(s.debut, jour)).length;
    final retenu = _jourRetenu != null && Dates.memeJour(_jourRetenu!, jour);
    final aujourdhui = Dates.estAujourdhui(jour);

    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () => setState(() => _jourRetenu = retenu ? null : jour),
      child: Column(
        children: [
          Text(nom,
              style: Typo.legende.copyWith(
                  fontSize: 9.5,
                  letterSpacing: 0.6,
                  color: Colors.white.withValues(alpha: 0.6))),
          const SizedBox(height: 6),
          AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            width: 32,
            height: 32,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: retenu ? Colors.white : Colors.transparent,
              border: aujourdhui && !retenu
                  ? Border.all(color: Colors.white.withValues(alpha: 0.7))
                  : null,
            ),
            child: Text('${jour.day}',
                style: Typo.mono(13,
                    graisse: FontWeight.w700,
                    couleur: retenu ? Couleurs.royal700 : Colors.white)),
          ),
          const SizedBox(height: 5),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              for (var k = 0; k < cours.clamp(0, 3); k++)
                Container(
                  width: 4,
                  height: 4,
                  margin: const EdgeInsets.symmetric(horizontal: 1.5),
                  decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: Colors.white.withValues(alpha: 0.75)),
                ),
              if (cours == 0) const SizedBox(height: 4),
            ],
          ),
        ],
      ),
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
      if (_jourRetenu != null && !Dates.memeJour(seance.debut, _jourRetenu!)) continue;
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

  /// Une journée : son nom à la verticale, puis un tableau Séance · Salle · Matière · Horaire.
  Widget _journee(DateTime jour, List<Seance> seances) {
    final aujourdhui = Dates.estAujourdhui(jour);
    final nom = Dates.jours[jour.weekday - 1];

    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.md),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 22,
            child: RotatedBox(
              quarterTurns: 3,
              child: Text(
                nom.toUpperCase(),
                textAlign: TextAlign.center,
                style: Typo.suretitre.copyWith(
                    letterSpacing: 1.4,
                    color: aujourdhui ? Couleurs.royal600 : Couleurs.encreDiscrete),
              ),
            ),
          ),
          const SizedBox(width: Espaces.xs),
          Expanded(
            child: Carte(
              rayon: Rayons.carte,
              ombre: Couleurs.ombreFlottante,
              padding: const EdgeInsets.fromLTRB(
                  Espaces.lg, Espaces.md, Espaces.lg, Espaces.sm),
              enfant: Column(
                children: [
                  _ligneTableau(
                    ['Séance', 'Salle', 'Matière', 'Horaire'],
                    entete: true,
                  ),
                  Divider(height: Espaces.lg, color: Couleurs.traitLeger),
                  for (var i = 0; i < seances.length; i++)
                    _ligneSeance(i + 1, seances[i]),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _ligneSeance(int rang, Seance s) {
    final annulee = s.statut == StatutSeance.annulee;
    final enCours = s.seDerouleMaintenant;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: _ligneTableau(
        [
          rang == 1 ? '1re' : '${rang}e',
          s.salle ?? '—',
          s.matiere,
          '${Dates.heure(s.debut)}–${Dates.heure(s.fin)}',
        ],
        barre: annulee,
        accent: enCours,
      ),
    );
  }

  Widget _ligneTableau(List<String> c,
      {bool entete = false, bool barre = false, bool accent = false}) {
    final style = entete
        ? Typo.suretitre.copyWith(fontSize: 10)
        : Typo.libelle.copyWith(
            fontSize: 12.5,
            color: accent ? Couleurs.royal600 : Couleurs.royal800,
            fontWeight: accent ? FontWeight.w700 : FontWeight.w600,
            decoration: barre ? TextDecoration.lineThrough : null);

    Widget cellule(String t, int flex) => Expanded(
          flex: flex,
          child: Text(t, style: style, maxLines: 2, overflow: TextOverflow.ellipsis),
        );

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [cellule(c[0], 2), cellule(c[1], 3), cellule(c[2], 5), cellule(c[3], 5)],
    );
  }
}
