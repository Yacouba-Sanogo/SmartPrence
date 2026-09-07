import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/presence.dart';
import '../../models/seance.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/anneau.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
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
            const SizedBox(height: Espaces.md + 3),
            Row(
              children: [
                Anneau(
                  valeur: feuille.tauxPresence.toDouble(),
                  taille: 66,
                  epaisseur: 12,
                  couleurFond: Colors.white.withValues(alpha: 0.2),
                  centre: Text(
                    '${feuille.tauxPresence}%',
                    style: Typo.mono(15,
                        graisse: FontWeight.w600, couleur: Colors.white),
                  ),
                ),
                const SizedBox(width: Espaces.lg - 2),
                Expanded(
                  child: Row(
                    children: [
                      Expanded(
                        child: Compteur(
                          surFondColore: true,
                          valeur: _deuxChiffres(feuille.presents),
                          libelle: 'Présents',
                          teinte: Colors.white,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Compteur(
                          surFondColore: true,
                          valeur: _deuxChiffres(feuille.retards),
                          libelle: 'Retards',
                          teinte: Couleurs.alerteVif,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Compteur(
                          surFondColore: true,
                          valeur: _deuxChiffres(feuille.absents),
                          libelle: 'Absents',
                          teinte: const Color(0xFFFF9AA8),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ],
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
          ...lignes.map(_ligne),
      ],
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

  Widget _ligne(LigneFeuille ligne) {
    final aSignaler = ligne.statut == StatutPresence.absent;

    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm),
      child: Carte(
        surTap: aSignaler ? () => _signaler(etudiant: ligne) : null,
        rayon: Rayons.tuile + 1,
        padding: const EdgeInsets.symmetric(
            horizontal: Espaces.md + 1, vertical: Espaces.md - 2),
        enfant: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: ligne.statut.fond,
                borderRadius: BorderRadius.circular(Rayons.md - 2),
              ),
              child: Text(
                ligne.initiales.isEmpty ? '?' : ligne.initiales,
                style: Typo.legende.copyWith(
                    color: ligne.statut.teinte, fontWeight: FontWeight.w600),
              ),
            ),
            const SizedBox(width: Espaces.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(ligne.nomComplet,
                      style: Typo.libelle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 3),
                  Row(
                    children: [
                      Text(ligne.matricule,
                          style: Typo.mono(11.5, couleur: Couleurs.encrePale)),
                      if (ligne.heure != null) ...[
                        Text('  ·  ', style: Typo.legendePale),
                        Text(ligne.heure!,
                            style: Typo.mono(11.5, couleur: Couleurs.encreDiscrete)),
                      ],
                    ],
                  ),
                  if (!ligne.enrole || ligne.estRegularise) ...[
                    const SizedBox(height: 5),
                    Wrap(
                      spacing: Espaces.xs,
                      runSpacing: Espaces.xs,
                      children: [
                        if (!ligne.enrole)
                          const Pastille(
                            texte: 'Non enrôlé',
                            icone: Icons.fingerprint_rounded,
                            teinte: Couleurs.alerte,
                            fond: Couleurs.alerteFond,
                          ),
                        if (ligne.estRegularise)
                          const Pastille(
                            texte: 'Régularisé',
                            icone: Icons.edit_note_rounded,
                            teinte: Couleurs.royal600,
                            fond: Couleurs.royal50,
                          ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(width: Espaces.sm),
            Pastille(
              texte: ligne.statut.libelle,
              teinte: ligne.statut.teinte,
              fond: ligne.statut.fond,
            ),
          ],
        ),
      ),
    );
  }
}
