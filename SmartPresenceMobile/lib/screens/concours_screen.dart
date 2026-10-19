import 'package:flutter/material.dart';

import '../core/api/client_api.dart';
import '../core/api/erreurs.dart';
import '../core/dates.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../widgets/communs.dart';
import '../widgets/entete.dart';

/// Une filière avec son département et ses options.
class _Filiere {
  const _Filiere(this.libelle, this.departement, this.options);

  final String libelle;
  final String departement;
  final List<String> options;

  factory _Filiere.depuisJson(Map<String, dynamic> j) => _Filiere(
        j['libelle'] as String,
        (j['departement'] as Map<String, dynamic>)['libelle'] as String,
        [for (final o in (j['options'] as List)) (o as Map<String, dynamic>)['libelle'] as String],
      );
}

class _Verdict {
  const _Verdict(this.eligible, this.message, this.pieces);

  final bool eligible;
  final String message;
  final List<String> pieces;
}

/// Diplômes proposés, avec le code attendu par le serveur.
const _diplomes = <(String, String)>[
  ('BACCALAUREAT', 'Baccalauréat'),
  ('LICENCE', 'Licence'),
  ('AUTRE_DIPLOME', 'DUT, maîtrise, autre'),
  ('PROFESSIONNEL', 'Professionnel'),
];

/// Concours d'entrée à l'ENETP : filières ouvertes, éligibilité et pièces à fournir.
///
/// Écran public : un futur candidat n'a pas de compte. Les limites d'âge viennent du
/// serveur, qui reste seul juge ; l'application ne recalcule rien.
class ConcoursScreen extends StatefulWidget {
  const ConcoursScreen({super.key, required this.client});

  final ClientApi client;

  @override
  State<ConcoursScreen> createState() => _ConcoursScreenState();
}

class _ConcoursScreenState extends State<ConcoursScreen> {
  List<_Filiere> _filieres = const [];
  bool _chargement = true;
  String? _erreur;

  DateTime? _naissance;
  String _diplome = 'BACCALAUREAT';
  bool _enCours = false;
  _Verdict? _verdict;
  String? _erreurVerdict;

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
      final d = await widget.client.get('/referentiel/filieres');
      final liste = [
        for (final f in (d as List)) _Filiere.depuisJson(f as Map<String, dynamic>)
      ];
      if (mounted) setState(() => _filieres = liste);
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _chargement = false);
    }
  }

  Future<void> _choisirDate() async {
    final maintenant = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: _naissance ?? DateTime(maintenant.year - 20),
      firstDate: DateTime(maintenant.year - 70),
      lastDate: maintenant.subtract(const Duration(days: 1)),
      helpText: 'Date de naissance',
    );
    if (date != null) setState(() => _naissance = date);
  }

  static String _iso(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-'
      '${d.day.toString().padLeft(2, '0')}';

  Future<void> _verifier() async {
    final naissance = _naissance;
    if (naissance == null) {
      setState(() => _erreurVerdict = 'Indiquez votre date de naissance.');
      return;
    }
    setState(() {
      _enCours = true;
      _erreurVerdict = null;
      _verdict = null;
    });
    try {
      final d = await widget.client.post('/concours/eligibilite', corps: {
        'dateNaissance': _iso(naissance),
        'diplome': _diplome,
      }) as Map<String, dynamic>;
      if (mounted) {
        setState(() => _verdict = _Verdict(
              d['eligible'] == true,
              d['message'] as String,
              [for (final p in (d['piecesAFournir'] as List)) '$p'],
            ));
      }
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreurVerdict = e.message);
    } finally {
      if (mounted) setState(() => _enCours = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      body: SingleChildScrollView(
        child: Column(
          children: [
            EnteteDegrade(
              debordement: 36,
              padding: const EdgeInsets.fromLTRB(
                  Espaces.sm, Espaces.sm, Espaces.lg, Espaces.md),
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
                      children: [
                        Text("Concours d'entrée",
                            style: Typo.titreEcran
                                .copyWith(color: Colors.white, fontSize: 21)),
                        const SizedBox(height: 3),
                        Text('ENETP · Kabala, Bamako',
                            style: Typo.legende.copyWith(
                                color: Colors.white.withValues(alpha: 0.72),
                                fontSize: 12)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Transform.translate(
              offset: const Offset(0, -28),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(
                    Espaces.md + 2, 0, Espaces.md + 2, Espaces.xxl),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _carteEligibilite(),
                    if (_verdict != null || _erreurVerdict != null) ...[
                      const SizedBox(height: Espaces.lg),
                      _carteVerdict(),
                    ],
                    const SizedBox(height: Espaces.xl),
                    Padding(
                      padding: const EdgeInsets.only(left: Espaces.xs, bottom: Espaces.sm),
                      child: Text('Filières et départements', style: Typo.titreSection),
                    ),
                    _filieresListe(),
                    const SizedBox(height: Espaces.xl),
                    _contact(),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _carteEligibilite() {
    return Carte(
      ombre: Couleurs.ombreFlottante,
      padding: const EdgeInsets.all(Espaces.xl),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Puis-je me présenter ?', style: Typo.titreSection),
          const SizedBox(height: Espaces.xs),
          Text('La limite d’âge dépend du diplôme que vous présentez.',
              style: Typo.corpsAttenue),
          const SizedBox(height: Espaces.lg),
          Text('Date de naissance', style: Typo.suretitre),
          const SizedBox(height: 6),
          InkWell(
            onTap: _choisirDate,
            borderRadius: BorderRadius.circular(Rayons.md),
            child: InputDecorator(
              decoration: const InputDecoration(
                  suffixIcon: Icon(Icons.calendar_today_outlined, size: 18)),
              child: Text(
                _naissance == null
                    ? 'Choisir une date'
                    : '${_naissance!.day} ${Dates.moisLongs[_naissance!.month - 1]} ${_naissance!.year}',
                style: _naissance == null
                    ? Typo.corpsAttenue
                    : Typo.corps,
              ),
            ),
          ),
          const SizedBox(height: Espaces.lg),
          Text('Diplôme présenté', style: Typo.suretitre),
          const SizedBox(height: Espaces.sm),
          Wrap(
            spacing: Espaces.sm,
            runSpacing: Espaces.sm,
            children: [
              for (final (code, libelle) in _diplomes)
                ChoiceChip(
                  label: Text(libelle),
                  selected: _diplome == code,
                  onSelected: (_) => setState(() {
                    _diplome = code;
                    _verdict = null;
                  }),
                ),
            ],
          ),
          const SizedBox(height: Espaces.xl),
          FilledButton(
            onPressed: _enCours ? null : _verifier,
            child: _enCours
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2.2, color: Colors.white))
                : const Text('Vérifier mon éligibilité'),
          ),
        ],
      ),
    );
  }

  Widget _carteVerdict() {
    if (_erreurVerdict != null) return Bandeau.erreur(message: _erreurVerdict!);

    final v = _verdict!;
    final teinte = v.eligible ? Couleurs.succes : Couleurs.danger;

    return Carte(
      ombre: Couleurs.ombreCarte,
      padding: const EdgeInsets.all(Espaces.xl),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(v.eligible ? Icons.check_circle_rounded : Icons.cancel_rounded,
                  color: teinte, size: 26),
              const SizedBox(width: Espaces.md),
              Expanded(
                child: Text(v.message,
                    style: Typo.libelle.copyWith(color: teinte, height: 1.4)),
              ),
            ],
          ),
          if (v.pieces.isNotEmpty) ...[
            const SizedBox(height: Espaces.lg),
            Text('Pièces à fournir', style: Typo.suretitre),
            const SizedBox(height: Espaces.sm),
            for (final p in v.pieces)
              Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Padding(
                      padding: EdgeInsets.only(top: 3),
                      child: Icon(Icons.description_outlined,
                          size: 15, color: Couleurs.royal500),
                    ),
                    const SizedBox(width: Espaces.sm),
                    Expanded(child: Text(p, style: Typo.corps)),
                  ],
                ),
              ),
          ],
        ],
      ),
    );
  }

  Widget _filieresListe() {
    if (_chargement) {
      return const Padding(
        padding: EdgeInsets.all(Espaces.xl),
        child: Center(child: CircularProgressIndicator(color: Couleurs.royal600)),
      );
    }
    if (_erreur != null) {
      return EtatVide(
        enErreur: true,
        icone: Icons.cloud_off_rounded,
        titre: 'Filières indisponibles',
        detail: _erreur,
        libelleAction: 'Réessayer',
        surAction: _charger,
      );
    }

    return Column(
      children: [
        for (final f in _filieres)
          Padding(
            padding: const EdgeInsets.only(bottom: Espaces.sm),
            child: Carte(
              rayon: Rayons.tuile,
              padding: EdgeInsets.zero,
              enfant: Theme(
                data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
                child: ExpansionTile(
                  tilePadding: const EdgeInsets.symmetric(
                      horizontal: Espaces.lg, vertical: 2),
                  childrenPadding: const EdgeInsets.fromLTRB(
                      Espaces.lg, 0, Espaces.lg, Espaces.md),
                  expandedCrossAxisAlignment: CrossAxisAlignment.start,
                  title: Text(f.libelle, style: Typo.libelle),
                  subtitle: Text(f.departement, style: Typo.legendePale),
                  children: [
                    Text('OPTIONS À PARTIR DE LA 3e ANNÉE', style: Typo.suretitre),
                    const SizedBox(height: Espaces.sm),
                    for (final o in f.options)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 5),
                        child: Row(
                          children: [
                            const Icon(Icons.chevron_right_rounded,
                                size: 16, color: Couleurs.royal500),
                            const SizedBox(width: 4),
                            Expanded(child: Text(o, style: Typo.corps)),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }

  Widget _contact() {
    return Carte(
      rayon: Rayons.tuile,
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text("L'école", style: Typo.suretitre),
          const SizedBox(height: Espaces.sm),
          _ligneContact(Icons.location_on_outlined,
              'Bâtiment n°4, campus universitaire de Kabala, Bamako (Mali)'),
          const SizedBox(height: 6),
          _ligneContact(Icons.phone_outlined, '+223 20 70 77 61'),
          const SizedBox(height: 6),
          _ligneContact(Icons.language_rounded, 'www.enetp.ml'),
        ],
      ),
    );
  }

  Widget _ligneContact(IconData icone, String texte) => Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icone, size: 17, color: Couleurs.royal500),
          const SizedBox(width: Espaces.sm),
          Expanded(child: Text(texte, style: Typo.corps)),
        ],
      );
}
