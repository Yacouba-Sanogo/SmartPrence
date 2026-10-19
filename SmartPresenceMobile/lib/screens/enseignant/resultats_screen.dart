import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/enseignement.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';

/// Résultats d'une évaluation : une ligne par étudiant, une note sur le barème ou une
/// absence.
///
/// La saisie se fait sur le barème de l'épreuve (7 sur 10), jamais sur 20 : c'est le
/// serveur qui convertit, pour que l'enseignant corrige avec les chiffres de ses copies.
/// Tant que la note est en brouillon elle reste modifiable ; transmise à l'administration,
/// elle se verrouille jusqu'à sa validation ou son renvoi.
class ResultatsScreen extends StatefulWidget {
  const ResultatsScreen({super.key, required this.auth, required this.evaluationId});

  final AuthService auth;
  final String evaluationId;

  @override
  State<ResultatsScreen> createState() => _ResultatsScreenState();
}

class _ResultatsScreenState extends State<ResultatsScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  ResultatsEvaluation? _donnees;
  final Map<String, TextEditingController> _champs = {};
  final Map<String, bool> _absents = {};

  bool _chargement = true;
  bool _envoi = false;
  bool _modifie = false;
  String? _erreur;
  String? _erreurAction;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  @override
  void dispose() {
    for (final c in _champs.values) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _charger() async {
    setState(() {
      _chargement = true;
      _erreur = null;
    });
    try {
      _appliquer(await _service.resultats(widget.evaluationId));
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _chargement = false;
      });
    }
  }

  void _appliquer(ResultatsEvaluation donnees) {
    if (!mounted) return;
    for (final c in _champs.values) {
      c.dispose();
    }
    _champs.clear();
    _absents.clear();
    for (final l in donnees.lignes) {
      _champs[l.etudiantId] = TextEditingController(
          text: l.valeurSurBareme == null ? '' : _texte(l.valeurSurBareme!));
      _absents[l.etudiantId] = l.absent;
    }
    setState(() {
      _donnees = donnees;
      _modifie = false;
      _chargement = false;
    });
  }

  static String _texte(double v) =>
      (v == v.roundToDouble() ? v.toStringAsFixed(0) : v.toStringAsFixed(2))
          .replaceAll(RegExp(r'0+$'), '')
          .replaceAll(RegExp(r'\.$'), '');

  double? _valeur(String etudiantId) {
    final brut = _champs[etudiantId]?.text.trim().replaceAll(',', '.') ?? '';
    return brut.isEmpty ? null : double.tryParse(brut);
  }

  bool _horsBareme(String etudiantId, int bareme) {
    final v = _valeur(etudiantId);
    final brut = _champs[etudiantId]?.text.trim() ?? '';
    if (brut.isEmpty) return false;
    return v == null || v < 0 || v > bareme;
  }

  Future<void> _enregistrer() async {
    final donnees = _donnees;
    if (donnees == null || _envoi) return;
    final bareme = donnees.evaluation.bareme;

    final saisies = <SaisieResultat>[];
    for (final l in donnees.lignes) {
      if (l.verrouille) continue;
      if (_absents[l.etudiantId] == true) {
        saisies.add(SaisieResultat(etudiantId: l.etudiantId, absent: true));
        continue;
      }
      if (_horsBareme(l.etudiantId, bareme)) {
        setState(() => _erreurAction =
            'La note de ${l.nomComplet} doit être comprise entre 0 et $bareme.');
        return;
      }
      final v = _valeur(l.etudiantId);
      if (v != null) saisies.add(SaisieResultat(etudiantId: l.etudiantId, valeur: v));
    }
    if (saisies.isEmpty) {
      setState(() => _erreurAction = 'Rien à enregistrer.');
      return;
    }

    setState(() {
      _envoi = true;
      _erreurAction = null;
    });
    try {
      final maj = await _service.enregistrerResultats(widget.evaluationId, saisies);
      if (!mounted) return;
      _appliquer(maj);
      setState(() => _envoi = false);
      _message('Résultats enregistrés.');
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _erreurAction = e.message;
        _envoi = false;
      });
    }
  }

  Future<void> _transmettre() async {
    final donnees = _donnees;
    if (donnees == null) return;
    final n = donnees.evaluation.brouillons;
    final confirme = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Transmettre à l’administration', style: Typo.titreSection),
        content: Text(
          '$n note${n > 1 ? 's seront envoyées' : ' sera envoyée'} pour validation. '
          'Elles ne seront plus modifiables, et n’apparaîtront dans les bulletins '
          'qu’une fois validées.',
          style: Typo.corpsAttenue,
        ),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Rayons.carte)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Transmettre')),
        ],
      ),
    );
    if (confirme != true) return;

    setState(() {
      _envoi = true;
      _erreurAction = null;
    });
    try {
      await _service.transmettreEvaluation(widget.evaluationId);
      if (!mounted) return;
      setState(() => _envoi = false);
      await _charger();
      _message('Notes transmises à l’administration.');
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _erreurAction = e.message;
        _envoi = false;
      });
    }
  }

  void _message(String texte) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(texte), behavior: SnackBarBehavior.floating),
    );
  }

  @override
  Widget build(BuildContext context) {
    final donnees = _donnees;
    return Scaffold(
      backgroundColor: Couleurs.fond,
      body: Column(
        children: [
          _entete(donnees),
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
      bottomNavigationBar: donnees == null ? null : _barreActions(donnees),
    );
  }

  Widget _entete(ResultatsEvaluation? donnees) {
    final e = donnees?.evaluation;
    return EnteteDegrade(
      debordement: 30,
      padding: const EdgeInsets.fromLTRB(Espaces.sm, Espaces.sm, Espaces.lg, Espaces.md),
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
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(e?.libelle ?? 'Résultats',
                    style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis),
                const SizedBox(height: 2),
                Text(
                  e == null
                      ? ''
                      : '${e.matiereLibelle} · ${Dates.courte(e.date)} · sur ${e.bareme}',
                  style: Typo.legende
                      .copyWith(color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
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
    final donnees = _donnees;
    if (_erreur != null || donnees == null) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Résultats indisponibles',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }
    if (donnees.lignes.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.person_off_outlined,
            titre: 'Aucun étudiant inscrit',
            detail: 'Cette classe ne compte encore aucun inscrit à noter.',
          ),
        ],
      );
    }

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 2),
      children: [
        if (_erreurAction != null) ...[
          Bandeau.erreur(
              message: _erreurAction!, surFermeture: () => setState(() => _erreurAction = null)),
          const SizedBox(height: Espaces.md),
        ],
        if (donnees.evaluation.sujet != null) ...[
          Carte(
            padding: const EdgeInsets.all(Espaces.lg),
            enfant: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Sujet', style: Typo.suretitre),
                const SizedBox(height: 4),
                Text(donnees.evaluation.sujet!, style: Typo.corps.copyWith(fontSize: 13.5)),
              ],
            ),
          ),
          const SizedBox(height: Espaces.md),
        ],
        _progression(donnees),
        const SizedBox(height: Espaces.md),
        ...donnees.lignes.asMap().entries.map((e) => ApparitionEnCascade(
            rang: e.key, enfant: _ligne(e.value, donnees.evaluation.bareme))),
      ],
    );
  }

  /// Combien de copies sont traitées, en direct pendant la saisie.
  Widget _progression(ResultatsEvaluation donnees) {
    final total = donnees.lignes.length;
    var traites = 0;
    for (final l in donnees.lignes) {
      if (l.verrouille || _absents[l.etudiantId] == true || _valeur(l.etudiantId) != null) {
        traites++;
      }
    }
    final complet = total > 0 && traites == total;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text('Copies traitées', style: Typo.suretitre),
            const Spacer(),
            Text('$traites / $total',
                style: Typo.mono(13,
                    graisse: FontWeight.w600,
                    couleur: complet ? Couleurs.succes : Couleurs.encreAttenuee)),
          ],
        ),
        const SizedBox(height: 6),
        BarreAnimee(
          valeur: total == 0 ? 0 : traites / total,
          couleur: complet ? Couleurs.succesVif : Couleurs.royal500,
        ),
      ],
    );
  }

  Widget _ligne(ResultatEtudiant l, int bareme) {
    final absent = _absents[l.etudiantId] == true;
    final verrouille = l.verrouille;
    final erreur = !verrouille && !absent && _horsBareme(l.etudiantId, bareme);

    // La carte change de teinte en douceur : absent en rose, note hors barème en ambre.
    final fond = absent
        ? Couleurs.dangerFond
        : erreur
            ? Couleurs.alerteFond
            : Couleurs.carte;

    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm),
      child: TweenAnimationBuilder<Color?>(
        tween: ColorTween(end: fond),
        duration: Durees.moyenne,
        builder: (_, couleur, enfant) => Carte(
          couleur: couleur,
          padding: const EdgeInsets.symmetric(horizontal: Espaces.lg, vertical: Espaces.md),
          enfant: enfant!,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(l.nomComplet,
                          style: Typo.libelle, maxLines: 1, overflow: TextOverflow.ellipsis),
                      const SizedBox(height: 2),
                      Text(l.matricule, style: Typo.mono(11, couleur: Couleurs.encrePale)),
                    ],
                  ),
                ),
                if (verrouille)
                  Pastille(
                      texte: l.statut!.libelle,
                      teinte: l.statut!.teinte,
                      fond: l.statut!.fond,
                      compact: true)
                else ...[
                  Text('Absent', style: Typo.legendePale),
                  Switch(
                    value: absent,
                    activeThumbColor: Couleurs.danger,
                    onChanged: _envoi
                        ? null
                        : (v) => setState(() {
                              _absents[l.etudiantId] = v;
                              _modifie = true;
                            }),
                  ),
                ],
                const SizedBox(width: Espaces.sm),
                SizedBox(
                  width: 74,
                  child: verrouille || absent
                      ? Text(
                          absent ? 'ABS' : '${_champs[l.etudiantId]?.text ?? ''} / $bareme',
                          textAlign: TextAlign.right,
                          style: Typo.mono(14,
                              graisse: FontWeight.w600,
                              couleur: absent ? Couleurs.danger : Couleurs.encre),
                        )
                      : TextField(
                          controller: _champs[l.etudiantId],
                          enabled: !_envoi,
                          textAlign: TextAlign.center,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))],
                          onChanged: (_) => setState(() => _modifie = true),
                          decoration: InputDecoration(
                            hintText: '/ $bareme',
                            isDense: true,
                            errorText: erreur ? ' ' : null,
                            errorStyle: const TextStyle(height: 0, fontSize: 0),
                          ),
                        ),
                ),
              ],
            ),
            if (l.motifRejet != null && l.statut == StatutNote.brouillon)
              Padding(
                padding: const EdgeInsets.only(top: Espaces.sm),
                child: Bandeau.attention(message: 'Renvoyée par l’administration : ${l.motifRejet}'),
              ),
          ],
        ),
      ),
    );
  }

  Widget _barreActions(ResultatsEvaluation donnees) {
    final e = donnees.evaluation;
    final aTransmettre = e.brouillons > 0 && !_modifie;
    final toutVerrouille = donnees.lignes.every((l) => l.verrouille);

    // La barre monte du bas à l'arrivée des données, au lieu de surgir avec l'écran.
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 1, end: 0),
      duration: Durees.ample,
      curve: Courbes.sortie,
      builder: (_, t, enfant) => Transform.translate(offset: Offset(0, 60 * t), child: enfant),
      child: _barre(e, aTransmettre, toutVerrouille, donnees),
    );
  }

  Widget _barre(Evaluation e, bool aTransmettre, bool toutVerrouille, ResultatsEvaluation donnees) {
    return Container(
      decoration: const BoxDecoration(
        color: Couleurs.carte,
        border: Border(top: BorderSide(color: Couleurs.traitLeger)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(Espaces.lg, Espaces.md, Espaces.lg, Espaces.md),
          child: toutVerrouille
              ? Text(
                  e.transmises > 0
                      ? 'Notes transmises, en attente de validation.'
                      : 'Notes validées par l’administration.',
                  textAlign: TextAlign.center,
                  style: Typo.corpsAttenue,
                )
              : Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: aTransmettre && !_envoi ? _transmettre : null,
                        style: OutlinedButton.styleFrom(minimumSize: const Size(0, 48)),
                        child: const Text('Transmettre'),
                      ),
                    ),
                    const SizedBox(width: Espaces.md),
                    Expanded(
                      flex: 2,
                      child: FilledButton(
                        onPressed: _modifie && !_envoi ? _enregistrer : null,
                        style: FilledButton.styleFrom(minimumSize: const Size(0, 48)),
                        child: _envoi
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(
                                    strokeWidth: 2, color: Colors.white),
                              )
                            : const Text('Enregistrer'),
                      ),
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}
