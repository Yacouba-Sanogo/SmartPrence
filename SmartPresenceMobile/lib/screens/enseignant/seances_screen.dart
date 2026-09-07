import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/seance.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import 'feuille_screen.dart';

/// Journée de l'enseignant : ses séances, dans l'ordre, avec accès à chaque feuille.
///
/// C'est l'écran d'entrée du volet enseignant parce que c'est la seule question qu'il se
/// pose en arrivant : qu'est-ce que j'assure aujourd'hui, et où en est le relevé.
class SeancesScreen extends StatefulWidget {
  const SeancesScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<SeancesScreen> createState() => _SeancesScreenState();
}

class _SeancesScreenState extends State<SeancesScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  DateTime _jour = DateTime.now();
  List<Seance> _seances = const [];
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
      final seances = await _service.mesSeances(_jour);
      if (!mounted) return;
      setState(() {
        _seances = seances;
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

  void _allerAu(DateTime jour) {
    setState(() => _jour = jour);
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
              offset: const Offset(0, -28),
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

  /// En-tête dégradé portant la navigation par jour.
  ///
  /// Les flèches encadrent la date plutôt que de figurer dans une barre séparée :
  /// changer de jour est le seul geste récurrent de cet écran, il doit tomber sous
  /// le pouce sans chercher.
  Widget _entete() {
    final estAujourdhui = Dates.estAujourdhui(_jour);

    return EnteteDegrade(
      debordement: 34,
      padding: const EdgeInsets.fromLTRB(
          Espaces.sm, Espaces.md, Espaces.sm, Espaces.md),
      enfant: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              ActionEntete(
                icone: Icons.chevron_left_rounded,
                surTap: () => _allerAu(_jour.subtract(const Duration(days: 1))),
              ),
              Expanded(
                child: Column(
                  children: [
                    Text(
                      estAujourdhui
                          ? Dates.relatif(_jour)
                          : Dates.jours[_jour.weekday - 1],
                      style: Typo.titreEcran
                          .copyWith(color: Colors.white, fontSize: 19),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '${_jour.day} ${Dates.moisLongs[_jour.month - 1]} ${_jour.year}',
                      style: Typo.legende.copyWith(
                          color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5),
                    ),
                  ],
                ),
              ),
              ActionEntete(
                icone: Icons.chevron_right_rounded,
                surTap: () => _allerAu(_jour.add(const Duration(days: 1))),
              ),
            ],
          ),
          if (!_chargement && _erreur == null && _seances.isNotEmpty) ...[
            const SizedBox(height: Espaces.md),
            _resumeDuJour(),
          ],
        ],
      ),
    );
  }

  /// Résumé chiffré de la journée, posé dans l'en-tête.
  Widget _resumeDuJour() {
    final enCours = _seances.where((s) => s.seDerouleMaintenant).length;
    final passees = _seances
        .where((s) => s.fin.isBefore(DateTime.now()))
        .length;

    return Row(
      children: [
        Expanded(
          child: Compteur(
            surFondColore: true,
            valeur: '${_seances.length}',
            libelle: 'Séances',
            teinte: Colors.white,
          ),
        ),
        const SizedBox(width: 6),
        Expanded(
          child: Compteur(
            surFondColore: true,
            valeur: '$enCours',
            libelle: 'En cours',
            teinte: enCours > 0 ? Couleurs.succesVif : Colors.white,
          ),
        ),
        const SizedBox(width: 6),
        Expanded(
          child: Compteur(
            surFondColore: true,
            valeur: '$passees',
            libelle: 'Terminées',
            teinte: Colors.white,
          ),
        ),
      ],
    );
  }

  Widget _corps() {
    if (_chargement) {
      return const Center(child: CircularProgressIndicator(color: Couleurs.royal600));
    }

    if (_erreur != null) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Chargement impossible',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }

    if (_seances.isEmpty) {
      final estAujourdhui = Dates.estAujourdhui(_jour);
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.event_available_outlined,
            titre: 'Aucune séance ce jour',
            detail: estAujourdhui
                ? "Rien ne vous est planifié aujourd'hui."
                : 'Rien de planifié le ${Dates.jourEtDate(_jour)}.',
            libelleAction: estAujourdhui ? null : "Revenir à aujourd'hui",
            surAction: estAujourdhui ? null : () => _allerAu(DateTime.now()),
          ),
        ],
      );
    }

    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.sm, Espaces.md + 2, Espaces.xxxl * 3),
      itemCount: _seances.length,
      separatorBuilder: (_, index) => const SizedBox(height: Espaces.md),
      itemBuilder: (_, index) => _carteSeance(_seances[index]),
    );
  }

  Widget _carteSeance(Seance seance) {
    final enCours = seance.seDerouleMaintenant;
    final sousTitre = [seance.classeCode, seance.salle]
        .whereType<String>()
        .where((texte) => texte.isNotEmpty)
        .join(' · ');

    return Carte(
      surTap: () => _ouvrirFeuille(seance),
      padding: const EdgeInsets.fromLTRB(
          Espaces.lg, Espaces.md + 2, Espaces.md, Espaces.md + 2),
      enfant: Row(
        children: [
          // Colonne horaire en monospace : les créneaux s'alignent d'une ligne à
          // l'autre, et la journée se lit d'un seul coup d'œil.
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                Dates.heure(seance.debut),
                style: Typo.mono(15,
                    graisse: FontWeight.w600,
                    couleur: enCours ? Couleurs.royal600 : Couleurs.encre),
              ),
              const SizedBox(height: 2),
              Text(Dates.heure(seance.fin),
                  style: Typo.mono(13, couleur: Couleurs.encrePale)),
            ],
          ),
          Container(
            width: enCours ? 3 : 1,
            height: 42,
            margin: const EdgeInsets.symmetric(horizontal: Espaces.md),
            decoration: BoxDecoration(
              color: enCours ? Couleurs.royal500 : Couleurs.traitLeger,
              borderRadius: BorderRadius.circular(Rayons.pilule),
            ),
          ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(
                      child: Text(seance.matiere,
                          style: Typo.libelle.copyWith(fontSize: 14),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis),
                    ),
                    if (enCours) ...[
                      const SizedBox(width: Espaces.sm),
                      const Pastille(
                        texte: 'En cours',
                        teinte: Couleurs.royal600,
                        fond: Couleurs.royal50,
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 3),
                Text(sousTitre.isEmpty ? '—' : sousTitre,
                    style: Typo.legendePale,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
          const SizedBox(width: Espaces.xs),
          const Icon(Icons.chevron_right_rounded, size: 20, color: Couleurs.encrePale),
        ],
      ),
    );
  }

  Future<void> _ouvrirFeuille(Seance seance) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => FeuilleScreen(auth: widget.auth, seance: seance),
      ),
    );
  }
}
