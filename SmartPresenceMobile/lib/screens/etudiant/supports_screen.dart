import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/gestion.dart';
import '../../services/etudiant_service.dart';
import '../../services/fichiers.dart';
import '../../widgets/carte_support.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';

/// Supports de cours de la classe, regroupés par matière.
///
/// Un étudiant cherche d'abord « le cours de telle matière » : le regroupement vient avant
/// l'ordre chronologique, qui ne sert qu'à l'intérieur de chaque groupe.
class SupportsEtudiantScreen extends StatefulWidget {
  const SupportsEtudiantScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<SupportsEtudiantScreen> createState() => _SupportsEtudiantScreenState();
}

class _SupportsEtudiantScreenState extends State<SupportsEtudiantScreen> {
  late final EtudiantService _service = EtudiantService(widget.auth.client);

  List<SupportCours> _supports = const [];
  String? _ouvert;
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
      _chargement = _supports.isEmpty;
      _erreur = null;
    });
    try {
      final r = await _service.supportsDeCours();
      if (!mounted) return;
      setState(() {
        _supports = r;
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

  Future<void> _ouvrir(SupportCours s) async {
    setState(() {
      _ouvert = s.id;
      _erreurAction = null;
    });
    try {
      await ouvrirSupport(widget.auth.client, s);
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreurAction = e.message);
    } finally {
      if (mounted) setState(() => _ouvert = null);
    }
  }

  Map<String, List<SupportCours>> get _parMatiere {
    final groupes = <String, List<SupportCours>>{};
    for (final s in _supports) {
      groupes.putIfAbsent(s.matiere, () => []).add(s);
    }
    return groupes;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      body: Column(
        children: [
          EnteteDegrade(
            debordement: 30,
            padding: const EdgeInsets.fromLTRB(Espaces.sm, Espaces.sm, Espaces.lg, Espaces.md),
            enfant: Row(
              children: [
                ActionEntete(icone: Icons.arrow_back_rounded, surTap: () => Navigator.of(context).maybePop()),
                const SizedBox(width: Espaces.xs),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text('Supports de cours',
                          style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19)),
                      const SizedBox(height: 2),
                      Text(
                          '${_supports.length} document${_supports.length > 1 ? 's' : ''} pour votre classe',
                          style: Typo.legende.copyWith(
                              color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5)),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: Transform.translate(
              offset: const Offset(0, -24),
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

  Widget _corps() {
    if (_chargement) {
      return const Center(key: ValueKey('c'), child: CircularProgressIndicator(color: Couleurs.indigo600));
    }
    if (_erreur != null && _supports.isEmpty) {
      return ListView(
        key: const ValueKey('e'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Supports indisponibles',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }
    if (_supports.isEmpty) {
      return ListView(
        key: const ValueKey('v'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.folder_open_rounded,
            titre: 'Aucun support pour le moment',
            detail: 'Vos enseignants y publient leurs cours, présentations et énoncés. '
                'Vous êtes prévenu à chaque nouveau document.',
          ),
        ],
      );
    }

    var rang = 0;
    return ListView(
      key: const ValueKey('l'),
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      children: [
        if (_erreurAction != null) ...[
          Bandeau.erreur(message: _erreurAction!, surFermeture: () => setState(() => _erreurAction = null)),
          const SizedBox(height: Espaces.md),
        ],
        for (final groupe in _parMatiere.entries) ...[
          Padding(
            padding: const EdgeInsets.fromLTRB(Espaces.xs, Espaces.md, 0, Espaces.sm),
            child: Row(
              children: [
                Expanded(child: Text(groupe.key.toUpperCase(), style: Typo.suretitre)),
                Text('${groupe.value.length}', style: Typo.mono(12, couleur: Couleurs.encrePale)),
              ],
            ),
          ),
          for (final s in groupe.value)
            ApparitionEnCascade(
              rang: rang++,
              enfant: Padding(
                padding: const EdgeInsets.only(bottom: Espaces.sm + 2),
                child: CarteSupport(
                  support: s,
                  afficherAuteur: true,
                  enCours: _ouvert == s.id,
                  surTap: () => _ouvrir(s),
                ),
              ),
            ),
        ],
      ],
    );
  }
}
