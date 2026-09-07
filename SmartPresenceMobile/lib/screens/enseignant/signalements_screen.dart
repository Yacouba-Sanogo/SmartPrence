import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/signalement.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';

/// Suivi des anomalies signalées par l'enseignant.
///
/// L'écran existe pour une raison simple : sans retour visible, un enseignant à qui on
/// demande de signaler cesse de le faire. Il doit voir que la scolarité a tranché, et
/// dans quel sens.
class SignalementsScreen extends StatefulWidget {
  const SignalementsScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<SignalementsScreen> createState() => _SignalementsScreenState();
}

class _SignalementsScreenState extends State<SignalementsScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<Signalement> _signalements = const [];
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
      final signalements = await _service.mesSignalements();
      if (!mounted) return;
      // Les plus récents d'abord : c'est l'ordre dans lequel on les cherche.
      signalements.sort((a, b) => b.depose.compareTo(a.depose));
      setState(() {
        _signalements = signalements;
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
    final enAttente = _signalements.where((s) => s.enAttente).length;

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
                Text('Mes signalements',
                    style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 21)),
                const SizedBox(height: 3),
                Text('Ce que vous avez signalé, et la suite qui y a été donnée',
                    style: Typo.legende.copyWith(
                        color: Colors.white.withValues(alpha: 0.72), fontSize: 12)),
                if (enAttente > 0) ...[
                  const SizedBox(height: Espaces.md),
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: Espaces.md, vertical: Espaces.sm),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.14),
                      borderRadius: BorderRadius.circular(Rayons.md),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.hourglass_empty_rounded,
                            size: 14, color: Couleurs.alerteVif),
                        const SizedBox(width: 6),
                        Text(
                          '$enAttente en attente d’arbitrage',
                          style: Typo.legende.copyWith(
                              color: Colors.white, fontSize: 11.5),
                        ),
                      ],
                    ),
                  ),
                ],
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

    if (_signalements.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.flag_outlined,
            titre: 'Aucun signalement',
            detail: 'Ouvrez la feuille d’une séance pour signaler une anomalie de '
                'relevé — un étudiant non reconnu, un lecteur en panne.',
          ),
        ],
      );
    }

    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      itemCount: _signalements.length,
      separatorBuilder: (_, index) => const SizedBox(height: Espaces.md),
      itemBuilder: (_, index) => _carte(_signalements[index]),
    );
  }

  Widget _carte(Signalement signalement) {
    return Carte(
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(signalement.type.icone, size: 17, color: Couleurs.encreAttenuee),
              const SizedBox(width: Espaces.sm),
              Expanded(
                child: Text(signalement.type.libelle,
                    style: Typo.libelle, maxLines: 1, overflow: TextOverflow.ellipsis),
              ),
              const SizedBox(width: Espaces.sm),
              Pastille(
                texte: signalement.statut.libelle,
                icone: signalement.statut.icone,
                teinte: signalement.statut.teinte,
                fond: signalement.statut.fond,
              ),
            ],
          ),
          const SizedBox(height: Espaces.sm),
          Text(
            '${signalement.matiere} · ${signalement.classeCode} · '
            '${Dates.courte(signalement.seanceDebut)}',
            style: Typo.legendePale,
          ),
          if (signalement.etudiantNom != null) ...[
            const SizedBox(height: Espaces.sm),
            Row(
              children: [
                const Icon(Icons.person_outline_rounded,
                    size: 14, color: Couleurs.encrePale),
                const SizedBox(width: 5),
                Flexible(
                  child: Text(signalement.etudiantNom!,
                      style: Typo.corpsAttenue,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis),
                ),
                if (signalement.etudiantMatricule != null) ...[
                  const SizedBox(width: Espaces.sm),
                  Text(signalement.etudiantMatricule!,
                      style: Typo.mono(11.5, couleur: Couleurs.encrePale)),
                ],
              ],
            ),
          ],
          const SizedBox(height: Espaces.md),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(Espaces.md),
            decoration: BoxDecoration(
              color: Couleurs.traitPale,
              borderRadius: BorderRadius.circular(Rayons.md),
            ),
            child: Text(signalement.description, style: Typo.corpsAttenue),
          ),
          if (signalement.commentaireTraitement != null) ...[
            const SizedBox(height: Espaces.md),
            Text('Réponse de la scolarité', style: Typo.suretitre),
            const SizedBox(height: Espaces.xs),
            Text(signalement.commentaireTraitement!, style: Typo.corpsAttenue),
            if (signalement.traiteLe != null) ...[
              const SizedBox(height: Espaces.xs),
              Text('le ${Dates.jourEtDate(signalement.traiteLe!)}',
                  style: Typo.legendePale),
            ],
          ],
          if (signalement.corrige) ...[
            const SizedBox(height: Espaces.md),
            Row(
              children: [
                const Icon(Icons.edit_note_rounded, size: 16, color: Couleurs.indigo600),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Un relevé de présence a été ajouté à la feuille, marqué comme '
                    'régularisation manuelle.',
                    style: Typo.legende.copyWith(color: Couleurs.royal700),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
