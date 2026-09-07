import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/note.dart';
import '../../services/etudiant_service.dart';
import '../../widgets/anneau.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';

/// Bulletin de l'étudiant.
///
/// <p>Une matière ne montre d'abord que sa moyenne : c'est ce qu'on vient chercher.
/// Le détail des notes se déplie à la demande, sinon un bulletin de huit matières
/// deviendrait une liste de quarante lignes où plus rien ne ressort.</p>
class NotesScreen extends StatefulWidget {
  const NotesScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<NotesScreen> createState() => _NotesScreenState();
}

class _NotesScreenState extends State<NotesScreen> {
  late final EtudiantService _service = EtudiantService(widget.auth.client);

  Bulletin _bulletin = Bulletin.vide;
  PeriodeScolaire? _periode;
  bool _chargement = true;
  String? _erreur;

  /// Matières dépliées, par identifiant.
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
      final bulletin = await _service.monBulletin(periode: _periode);
      if (!mounted) return;
      setState(() {
        _bulletin = bulletin;
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

  void _changerPeriode(PeriodeScolaire? periode) {
    setState(() {
      _periode = periode;
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
            _bulletin.classeLibelle ?? 'Bulletin scolaire',
            style: Typo.legende.copyWith(
                color: Colors.white.withValues(alpha: 0.72), fontSize: 12),
          ),
          if (!_chargement && _erreur == null) ...[
            const SizedBox(height: Espaces.lg),
            _syntheseEntete(),
          ],
          const SizedBox(height: Espaces.md),
          _filtresPeriode(),
        ],
      ),
    );
  }

  Widget _syntheseEntete() {
    final endifficulte = _bulletin.matieresEnDifficulte;

    return Row(
      children: [
        Anneau(
          valeur: _bulletin.progression,
          taille: 76,
          epaisseur: 12,
          couleurFond: Colors.white.withValues(alpha: 0.2),
          couleur: _bulletin.moyenneGenerale == null
              ? Colors.white.withValues(alpha: 0.35)
              : teinteNote(_bulletin.moyenneGenerale!),
          centre: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_bulletin.moyenneFormatee,
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
          child: Row(
            children: [
              Expanded(
                child: Compteur(
                  surFondColore: true,
                  valeur: '${_bulletin.matieres.length}',
                  libelle: 'Matières',
                  teinte: Colors.white,
                ),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: Compteur(
                  surFondColore: true,
                  valeur: '${_bulletin.nombreNotes}',
                  libelle: 'Notes',
                  teinte: Colors.white,
                ),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: Compteur(
                  surFondColore: true,
                  valeur: '$endifficulte',
                  libelle: 'Sous 10',
                  teinte: endifficulte > 0
                      ? const Color(0xFFFF9AA8)
                      : Couleurs.succesVif,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _filtresPeriode() {
    final choix = <({PeriodeScolaire? valeur, String libelle})>[
      (valeur: null, libelle: 'Année'),
      (valeur: PeriodeScolaire.semestre1, libelle: 'Semestre 1'),
      (valeur: PeriodeScolaire.semestre2, libelle: 'Semestre 2'),
    ];

    return Row(
      children: choix.map((c) {
        final actif = _periode == c.valeur;
        return Padding(
          padding: const EdgeInsets.only(right: Espaces.sm - 2),
          child: GestureDetector(
            onTap: () => _changerPeriode(c.valeur),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 6),
              decoration: BoxDecoration(
                color: actif ? Colors.white : Colors.white.withValues(alpha: 0.14),
                borderRadius: BorderRadius.circular(Rayons.pilule),
              ),
              child: Text(
                c.libelle,
                style: Typo.legende.copyWith(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w600,
                  color: actif
                      ? Couleurs.indigo700
                      : Colors.white.withValues(alpha: 0.82),
                ),
              ),
            ),
          ),
        );
      }).toList(),
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
            titre: 'Bulletin indisponible',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }

    if (_bulletin.sansNote) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.school_outlined,
            titre: 'Aucune note pour l’instant',
            detail: _periode == null
                ? 'Vos notes apparaîtront ici dès que vos enseignants les auront saisies.'
                : 'Aucune note sur ${_periode!.libelle.toLowerCase()}.',
          ),
        ],
      );
    }

    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      itemCount: _bulletin.matieres.length,
      separatorBuilder: (_, index) => const SizedBox(height: Espaces.sm + 2),
      itemBuilder: (_, index) => _carteMatiere(_bulletin.matieres[index]),
    );
  }

  Widget _carteMatiere(LigneMatiere matiere) {
    final depliee = _depliees.contains(matiere.matiereId);
    final moyenne = matiere.moyenne;
    final teinte = moyenne == null ? Couleurs.encreDiscrete : teinteNote(moyenne);
    final fond = moyenne == null ? Couleurs.traitPale : fondNote(moyenne);

    return Carte(
      rayon: Rayons.tuile + 2,
      padding: EdgeInsets.zero,
      surTap: matiere.notes.isEmpty
          ? null
          : () => setState(() {
                if (depliee) {
                  _depliees.remove(matiere.matiereId);
                } else {
                  _depliees.add(matiere.matiereId);
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
                  child: Text(matiere.moyenneFormatee,
                      style: Typo.mono(14, graisse: FontWeight.w700, couleur: teinte)),
                ),
                const SizedBox(width: Espaces.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(matiere.libelle,
                          style: Typo.libelle,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis),
                      const SizedBox(height: 3),
                      Text(
                        '${matiere.code} · coefficient ${matiere.coefficient} · '
                        '${matiere.notes.length} note${matiere.notes.length > 1 ? 's' : ''}',
                        style: Typo.legendePale,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                if (matiere.notes.isNotEmpty)
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
            ...matiere.notes.map(_ligneNote),
            const SizedBox(height: Espaces.sm),
          ],
        ],
      ),
    );
  }

  Widget _ligneNote(Note note) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 1, Espaces.md - 2, Espaces.md + 1, 0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 38,
            padding: const EdgeInsets.symmetric(vertical: 4),
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: fondNote(note.valeur),
              borderRadius: BorderRadius.circular(Rayons.sm),
            ),
            child: Text(note.valeurFormatee,
                style: Typo.mono(12,
                    graisse: FontWeight.w600, couleur: teinteNote(note.valeur))),
          ),
          const SizedBox(width: Espaces.md - 2),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(note.type.icone, size: 13, color: Couleurs.encrePale),
                    const SizedBox(width: 5),
                    Expanded(
                      child: Text(note.libelle,
                          style: Typo.legende.copyWith(
                              fontSize: 12, color: Couleurs.encreAttenuee),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis),
                    ),
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  '${Dates.courte(note.dateEvaluation)} · coef. ${note.coefficient}'
                  '${note.enseignantNom == null ? '' : ' · ${note.enseignantNom}'}',
                  style: Typo.legendePale.copyWith(fontSize: 10.5),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                if (note.appreciation != null && note.appreciation!.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(
                        horizontal: Espaces.sm + 1, vertical: 6),
                    decoration: BoxDecoration(
                      color: Couleurs.traitPale,
                      borderRadius: BorderRadius.circular(Rayons.sm),
                    ),
                    child: Text(note.appreciation!,
                        style: Typo.legende.copyWith(fontSize: 11, height: 1.4)),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
