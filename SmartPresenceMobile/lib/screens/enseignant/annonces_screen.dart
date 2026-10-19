import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/annonce.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';

/// Annonces de l'administration et alertes sur les séances.
///
/// Un changement de salle ou une annulation est l'information qui coûte le plus quand
/// elle arrive trop tard : l'écran met donc les non lues en avant, par une barre d'accent
/// et un point qui bat, et se contente de les replier une fois lues.
class AnnoncesScreen extends StatefulWidget {
  const AnnoncesScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<AnnoncesScreen> createState() => _AnnoncesScreenState();
}

class _AnnoncesScreenState extends State<AnnoncesScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<Annonce> _annonces = const [];
  final Set<String> _ouvertes = {};
  bool _seulementNonLues = false;
  bool _chargement = true;
  String? _erreur;
  String? _erreurAction;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  Future<void> _charger() async {
    setState(() {
      _chargement = _annonces.isEmpty;
      _erreur = null;
    });
    try {
      final annonces = await _service.annonces();
      if (!mounted) return;
      setState(() {
        _annonces = annonces;
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

  int get _nonLues => _annonces.where((a) => !a.lue).length;

  /// Ouvre ou replie une annonce, et la marque lue sans attendre le serveur : le retour
  /// visuel est immédiat, et revient en arrière si la requête échoue.
  Future<void> _basculer(Annonce annonce) async {
    setState(() {
      if (!_ouvertes.remove(annonce.id)) _ouvertes.add(annonce.id);
    });
    if (annonce.lue) return;

    _remplacer(annonce.id, (a) => a.commeLue());
    try {
      await _service.marquerLue(annonce.id);
    } on ErreurApi catch (e) {
      if (!mounted) return;
      _remplacer(annonce.id, (_) => annonce);
      setState(() => _erreurAction = e.message);
    }
  }

  void _remplacer(String id, Annonce Function(Annonce) transformation) {
    setState(() {
      _annonces = _annonces.map((a) => a.id == id ? transformation(a) : a).toList();
    });
  }

  Future<void> _toutLire() async {
    if (_nonLues == 0) return;
    final avant = _annonces;
    setState(() => _annonces = _annonces.map((a) => a.commeLue()).toList());
    try {
      await _service.toutMarquerLu();
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _annonces = avant;
        _erreurAction = e.message;
      });
    }
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
                child: TransitionContenu(enfant: _corps()),
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
      padding: const EdgeInsets.fromLTRB(Espaces.sm, Espaces.sm, Espaces.sm, Espaces.md),
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
                    Text('Annonces',
                        style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19)),
                    const SizedBox(height: 2),
                    AnimatedSwitcher(
                      duration: Durees.moyenne,
                      child: Text(
                        _nonLues == 0
                            ? 'Tout est lu'
                            : _nonLues == 1
                                ? '1 non lue'
                                : '$_nonLues non lues',
                        key: ValueKey(_nonLues),
                        style: Typo.legende.copyWith(
                            color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5),
                      ),
                    ),
                  ],
                ),
              ),
              AnimatedOpacity(
                duration: Durees.moyenne,
                opacity: _nonLues == 0 ? 0.35 : 1,
                child: ActionEntete(icone: Icons.done_all_rounded, surTap: _toutLire),
              ),
            ],
          ),
          const SizedBox(height: Espaces.md),
          Padding(
            padding: const EdgeInsets.only(left: Espaces.sm),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _puce('Toutes', !_seulementNonLues, () => setState(() => _seulementNonLues = false)),
                  const SizedBox(width: Espaces.sm - 2),
                  _puce('Non lues', _seulementNonLues, () => setState(() => _seulementNonLues = true)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _puce(String libelle, bool actif, VoidCallback surTap) {
    return GestureDetector(
      onTap: surTap,
      child: AnimatedContainer(
        duration: Durees.rapide,
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 5),
        decoration: BoxDecoration(
          color: actif ? Colors.white : Colors.white.withValues(alpha: 0.14),
          borderRadius: BorderRadius.circular(Rayons.pilule),
        ),
        child: Text(
          libelle,
          style: Typo.legende.copyWith(
            fontSize: 11.5,
            fontWeight: FontWeight.w600,
            color: actif ? Couleurs.indigo700 : Colors.white.withValues(alpha: 0.82),
          ),
        ),
      ),
    );
  }

  Widget _corps() {
    if (_chargement) {
      return const Center(
        key: ValueKey('chargement'),
        child: CircularProgressIndicator(color: Couleurs.indigo600),
      );
    }
    if (_erreur != null && _annonces.isEmpty) {
      return ListView(
        key: const ValueKey('erreur'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Annonces indisponibles',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }

    final visibles = _seulementNonLues ? _annonces.where((a) => !a.lue).toList() : _annonces;
    if (visibles.isEmpty) {
      return ListView(
        key: ValueKey('vide$_seulementNonLues'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: _seulementNonLues ? Icons.mark_email_read_rounded : Icons.campaign_outlined,
            titre: _seulementNonLues ? 'Tout est lu' : 'Aucune annonce',
            detail: _seulementNonLues
                ? 'Vous êtes à jour : aucune annonce en attente de lecture.'
                : 'Les annonces de l’administration et les changements de vos séances apparaîtront ici.',
          ),
        ],
      );
    }

    // Une rubrique par jour : la liste se lit comme un fil, pas comme un tableau.
    final lignes = <Object>[];
    DateTime? jourCourant;
    for (final a in visibles) {
      if (jourCourant == null || !Dates.memeJour(jourCourant, a.recueLe)) {
        jourCourant = a.recueLe;
        lignes.add(a.recueLe);
      }
      lignes.add(a);
    }

    var rang = 0;
    return ListView(
      key: ValueKey('liste$_seulementNonLues'),
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      children: [
        if (_erreurAction != null) ...[
          Bandeau.erreur(
              message: _erreurAction!, surFermeture: () => setState(() => _erreurAction = null)),
          const SizedBox(height: Espaces.md),
        ],
        for (final ligne in lignes)
          if (ligne is DateTime)
            Padding(
              padding: const EdgeInsets.fromLTRB(Espaces.xs, Espaces.md, 0, Espaces.sm),
              child: Text(Dates.relatif(ligne).toUpperCase(), style: Typo.suretitre),
            )
          else
            ApparitionEnCascade(
              key: ValueKey((ligne as Annonce).id),
              rang: rang++,
              enfant: Padding(
                padding: const EdgeInsets.only(bottom: Espaces.sm + 2),
                child: _carte(ligne),
              ),
            ),
      ],
    );
  }

  Widget _carte(Annonce a) {
    final ouverte = _ouvertes.contains(a.id);

    return Carte(
      surTap: () => _basculer(a),
      padding: EdgeInsets.zero,
      enfant: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Barre d'accent : visible tant que l'annonce n'est pas lue.
            AnimatedContainer(
              duration: Durees.moyenne,
              curve: Courbes.sortie,
              width: a.lue ? 0 : 4,
              decoration: BoxDecoration(
                color: a.type.teinte,
                borderRadius: const BorderRadius.horizontal(left: Radius.circular(Rayons.carte)),
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(Espaces.lg),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    AnimatedContainer(
                      duration: Durees.moyenne,
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: a.lue ? Couleurs.traitPale : a.type.fond,
                        borderRadius: BorderRadius.circular(Rayons.md),
                      ),
                      child: Icon(a.type.icone,
                          size: 20, color: a.lue ? Couleurs.encreDiscrete : a.type.teinte),
                    ),
                    const SizedBox(width: Espaces.md),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: AnimatedDefaultTextStyle(
                                  duration: Durees.moyenne,
                                  style: Typo.libelle.copyWith(
                                    fontSize: 14,
                                    fontWeight: a.lue ? FontWeight.w500 : FontWeight.w700,
                                    color: a.lue ? Couleurs.encreAttenuee : Couleurs.encre,
                                  ),
                                  maxLines: ouverte ? 3 : 1,
                                  overflow: TextOverflow.ellipsis,
                                  child: Text(a.titre),
                                ),
                              ),
                              if (!a.lue) ...[
                                const SizedBox(width: Espaces.sm),
                                PointVivant(couleur: a.type.teinte, taille: 8),
                              ],
                            ],
                          ),
                          const SizedBox(height: 3),
                          Text('${a.type.libelle} · ${_heure(a.recueLe)}', style: Typo.legendePale),
                          AnimatedSize(
                            duration: Durees.moyenne,
                            curve: Courbes.sortie,
                            alignment: Alignment.topCenter,
                            child: Padding(
                              padding: EdgeInsets.only(top: Espaces.sm - 2),
                              child: Text(
                                a.message,
                                style: Typo.corpsAttenue.copyWith(fontSize: 13.5),
                                maxLines: ouverte ? 12 : 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// « à l'instant », « il y a 12 min », « il y a 3 h », puis l'heure exacte.
  static String _heure(DateTime d) {
    final ecart = DateTime.now().difference(d);
    if (ecart.inMinutes < 1) return "à l'instant";
    if (ecart.inMinutes < 60) return 'il y a ${ecart.inMinutes} min';
    if (ecart.inHours < 6 && Dates.estAujourdhui(d)) return 'il y a ${ecart.inHours} h';
    return Dates.heure(d);
  }
}
