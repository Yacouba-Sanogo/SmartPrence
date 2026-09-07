import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/seance.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import 'effectif_screen.dart';

/// Classes dans lesquelles l'enseignant intervient.
class ClassesScreen extends StatefulWidget {
  const ClassesScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<ClassesScreen> createState() => _ClassesScreenState();
}

class _ClassesScreenState extends State<ClassesScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<ClasseEnseignee> _classes = const [];
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
      final classes = await _service.mesClasses();
      if (!mounted) return;
      setState(() {
        _classes = classes;
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
                Text('Mes classes',
                    style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 21)),
                const SizedBox(height: 3),
                Text('Les classes où vous intervenez',
                    style: Typo.legende.copyWith(
                        color: Colors.white.withValues(alpha: 0.72), fontSize: 12)),
              ],
            ),
          ),
          Expanded(
            child: Transform.translate(
              offset: const Offset(0, -24),
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
            titre: 'Chargement impossible',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }

    if (_classes.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.school_outlined,
            titre: 'Aucune classe rattachée',
            detail: "Vous n'êtes rattaché à aucune classe pour le moment. "
                'La scolarité peut le faire depuis l’administration.',
          ),
        ],
      );
    }

    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      itemCount: _classes.length,
      separatorBuilder: (_, index) => const SizedBox(height: Espaces.md),
      itemBuilder: (_, index) => _carteClasse(_classes[index]),
    );
  }

  Widget _carteClasse(ClasseEnseignee classe) {
    final effectif = classe.effectif ?? 0;

    return Carte(
      surTap: () => Navigator.of(context).push<void>(
        MaterialPageRoute(
          builder: (_) => EffectifScreen(auth: widget.auth, classe: classe),
        ),
      ),
      padding: const EdgeInsets.fromLTRB(
          Espaces.lg, Espaces.lg, Espaces.md, Espaces.lg),
      enfant: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: Couleurs.royal50,
              borderRadius: BorderRadius.circular(Rayons.md),
            ),
            child: const Icon(Icons.groups_rounded, size: 21, color: Couleurs.indigo600),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(classe.libelle.isEmpty ? classe.code : classe.libelle,
                    style: Typo.libelle.copyWith(fontSize: 14),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis),
                const SizedBox(height: 3),
                Text(
                  [classe.code, classe.promotion]
                      .whereType<String>()
                      .where((texte) => texte.isNotEmpty)
                      .join(' · '),
                  style: Typo.legendePale,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          const SizedBox(width: Espaces.sm),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text('$effectif', style: Typo.mono(17, graisse: FontWeight.w600)),
              Text(effectif > 1 ? 'inscrits' : 'inscrit', style: Typo.legendePale),
            ],
          ),
          const Icon(Icons.chevron_right_rounded, size: 20, color: Couleurs.encrePale),
        ],
      ),
    );
  }
}
