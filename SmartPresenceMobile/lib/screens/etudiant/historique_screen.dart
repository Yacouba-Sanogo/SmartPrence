import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/presence.dart';
import '../../services/etudiant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';

/// Historique complet des relevés, chargé au fil du défilement.
class HistoriqueScreen extends StatefulWidget {
  const HistoriqueScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<HistoriqueScreen> createState() => _HistoriqueScreenState();
}

class _HistoriqueScreenState extends State<HistoriqueScreen> {
  late final EtudiantService _service = EtudiantService(widget.auth.client);
  final _defilement = ScrollController();

  final List<Presence> _releves = [];
  StatutPresence? _filtre;
  int _page = 0;
  bool _derniere = false;
  bool _chargement = true;
  bool _chargementSuite = false;
  String? _erreur;

  @override
  void initState() {
    super.initState();
    _defilement.addListener(_surDefilement);
    _charger(remiseAZero: true);
  }

  @override
  void dispose() {
    _defilement.removeListener(_surDefilement);
    _defilement.dispose();
    super.dispose();
  }

  /// Charge la page suivante avant d'atteindre le bas, pour éviter l'à-coup.
  void _surDefilement() {
    if (_chargementSuite || _derniere) return;
    if (_defilement.position.pixels >=
        _defilement.position.maxScrollExtent - 320) {
      _charger();
    }
  }

  Future<void> _charger({bool remiseAZero = false}) async {
    if (remiseAZero) {
      setState(() {
        _chargement = true;
        _erreur = null;
        _page = 0;
        _derniere = false;
        _releves.clear();
      });
    } else {
      setState(() => _chargementSuite = true);
    }

    try {
      final page = await _service.mesPresences(page: _page, taille: 20);
      if (!mounted) return;
      setState(() {
        _releves.addAll(page.elements);
        _derniere = page.derniere;
        _page++;
        _chargement = false;
        _chargementSuite = false;
      });
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _chargement = false;
        _chargementSuite = false;
      });
    }
  }

  List<Presence> get _affiches =>
      _filtre == null ? _releves : _releves.where((p) => p.statut == _filtre).toList();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      body: Column(
        children: [
          EnteteDegrade(
            debordement: 30,
            padding: const EdgeInsets.fromLTRB(
                Espaces.lg, Espaces.md, Espaces.lg, Espaces.md),
            enfant: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Mon historique',
                    style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 21)),
                const SizedBox(height: 3),
                Text('Tous vos relevés de présence',
                    style: Typo.legende.copyWith(
                        color: Colors.white.withValues(alpha: 0.72), fontSize: 12)),
              ],
            ),
          ),
          Transform.translate(
            offset: const Offset(0, -22),
            child: _filtres(),
          ),
          Expanded(
            child: Transform.translate(
              offset: const Offset(0, -14),
              child: _corps(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _filtres() {
    const options = <(String, StatutPresence?)>[
      ('Tous', null),
      ('Présent', StatutPresence.present),
      ('Retard', StatutPresence.retard),
      ('Absent', StatutPresence.absent),
      ('Justifié', StatutPresence.justifie),
    ];

    return SizedBox(
      height: 56,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: Espaces.lg, vertical: Espaces.sm),
        itemCount: options.length,
        separatorBuilder: (_, __) => const SizedBox(width: Espaces.sm),
        itemBuilder: (context, index) {
          final (libelle, statut) = options[index];
          final actif = _filtre == statut;
          return ChoiceChip(
            label: Text(libelle),
            selected: actif,
            onSelected: (_) => setState(() => _filtre = statut),
            showCheckmark: false,
            backgroundColor: Couleurs.carte,
            selectedColor: Couleurs.indigo600,
            labelStyle: Typo.legende.copyWith(
              fontSize: 12.5,
              fontWeight: actif ? FontWeight.w600 : FontWeight.w400,
              color: actif ? Colors.white : Couleurs.encreAttenuee,
            ),
            side: BorderSide(color: actif ? Couleurs.indigo600 : Couleurs.trait),
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(Rayons.pilule)),
          );
        },
      ),
    );
  }

  Widget _corps() {
    if (_chargement) {
      return const Center(child: CircularProgressIndicator(color: Couleurs.indigo600));
    }
    if (_erreur != null) {
      return EtatVide(
        enErreur: true,
        icone: Icons.cloud_off_rounded,
        titre: 'Chargement impossible',
        detail: _erreur,
        libelleAction: 'Réessayer',
        surAction: () => _charger(remiseAZero: true),
      );
    }
    if (_releves.isEmpty) {
      return const EtatVide(
        icone: Icons.event_note_outlined,
        titre: 'Aucun relevé',
        detail: "Vos présences apparaîtront ici dès votre premier passage "
            'devant un lecteur.',
      );
    }

    final affiches = _affiches;
    if (affiches.isEmpty) {
      return EtatVide(
        icone: Icons.filter_alt_off_outlined,
        titre: 'Aucun relevé pour ce filtre',
        detail: 'Choisissez « Tous » pour revoir la liste complète.',
        libelleAction: 'Tout afficher',
        surAction: () => setState(() => _filtre = null),
      );
    }

    return RefreshIndicator(
      onRefresh: () => _charger(remiseAZero: true),
      color: Couleurs.indigo600,
      child: ListView.separated(
        controller: _defilement,
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(
            Espaces.lg, Espaces.sm, Espaces.lg, Espaces.xxxl),
        itemCount: affiches.length + (_chargementSuite ? 1 : 0),
        separatorBuilder: (_, __) => const SizedBox(height: Espaces.sm),
        itemBuilder: (context, index) {
          if (index >= affiches.length) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: Espaces.lg),
              child: Center(
                child: SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(
                      strokeWidth: 2.2, color: Couleurs.indigo600),
                ),
              ),
            );
          }
          return _ligne(affiches[index]);
        },
      ),
    );
  }

  Widget _ligne(Presence presence) {
    return Carte(
      padding: const EdgeInsets.symmetric(
          horizontal: Espaces.lg, vertical: Espaces.md + 2),
      enfant: Row(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(presence.heure,
                  style: Typo.mono(14, couleur: presence.statut.teinte)),
              const SizedBox(height: 2),
              Text(_dateCourte(presence.date), style: Typo.legendePale),
            ],
          ),
          Container(
            width: 1,
            height: 36,
            color: Couleurs.traitLeger,
            margin: const EdgeInsets.symmetric(horizontal: Espaces.md),
          ),
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
                Row(
                  children: [
                    if (presence.salle != null)
                      Flexible(
                        child: Text(presence.salle!,
                            style: Typo.legendePale,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis),
                      ),
                    if (presence.estRegularise) ...[
                      if (presence.salle != null)
                        Text(' · ', style: Typo.legendePale),
                      Text('régularisé',
                          style: Typo.legendePale
                              .copyWith(fontStyle: FontStyle.italic)),
                    ],
                  ],
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

  static const List<String> _mois = [
    'janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin',
    'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.',
  ];

  String _dateCourte(DateTime date) => '${date.day} ${_mois[date.month - 1]}';
}
