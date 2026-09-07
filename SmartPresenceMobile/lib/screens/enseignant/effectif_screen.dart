import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/etudiant_inscrit.dart';
import '../../models/seance.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import 'notes_classe_screen.dart';

/// Effectif nominatif d'une classe.
///
/// L'intérêt principal n'est pas la liste elle-même mais la colonne d'enrôlement : elle
/// dit à l'enseignant, avant le cours, quels étudiants le lecteur sera de toute façon
/// incapable d'identifier. Sans cette information, il interpréterait leurs absences de
/// relevé comme des absences réelles.
class EffectifScreen extends StatefulWidget {
  const EffectifScreen({super.key, required this.auth, required this.classe});

  final AuthService auth;
  final ClasseEnseignee classe;

  @override
  State<EffectifScreen> createState() => _EffectifScreenState();
}

class _EffectifScreenState extends State<EffectifScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<EtudiantInscrit> _etudiants = const [];
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
      final etudiants = await _service.etudiantsDeLaClasse(widget.classe.id);
      if (!mounted) return;
      setState(() {
        _etudiants = etudiants;
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

  int get _nonEnroles => _etudiants.where((e) => !e.enrole).length;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => Navigator.of(context).push<void>(
          MaterialPageRoute(
            builder: (_) => NotesClasseScreen(auth: widget.auth, classe: widget.classe),
          ),
        ),
        backgroundColor: Couleurs.indigo600,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.school_rounded, size: 19),
        label: const Text('Notes'),
      ),
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.classe.libelle.isEmpty ? widget.classe.code : widget.classe.libelle,
              style: Typo.titreSection,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            Text(widget.classe.promotion ?? widget.classe.code, style: Typo.legende),
          ],
        ),
      ),
      body: SafeArea(
        top: false,
        child: RefreshIndicator(
          onRefresh: _charger,
          color: Couleurs.indigo600,
          child: _corps(),
        ),
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
            titre: 'Effectif indisponible',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }

    if (_etudiants.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.person_off_outlined,
            titre: 'Aucun étudiant inscrit',
            detail: 'Cette classe ne compte encore aucun inscrit.',
          ),
        ],
      );
    }

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.lg, Espaces.md, Espaces.lg, Espaces.xxxl),
      children: [
        if (_nonEnroles > 0)
          Padding(
            padding: const EdgeInsets.only(bottom: Espaces.md),
            child: Bandeau(
              icone: Icons.fingerprint_rounded,
              teinte: Couleurs.alerte,
              fond: Couleurs.alerteFond,
              bordure: Couleurs.alerteTrait,
              message: _nonEnroles == 1
                  ? "1 étudiant n'a pas d'empreinte enrôlée : aucun lecteur ne peut "
                      'relever sa présence. Signalez-le à la scolarité.'
                  : "$_nonEnroles étudiants n'ont pas d'empreinte enrôlée : aucun "
                      'lecteur ne peut relever leur présence. Signalez-le à la scolarité.',
            ),
          ),
        Text(
          '${_etudiants.length} inscrit${_etudiants.length > 1 ? 's' : ''}',
          style: Typo.suretitre,
        ),
        const SizedBox(height: Espaces.sm),
        ..._etudiants.map(_ligne),
      ],
    );
  }

  Widget _ligne(EtudiantInscrit etudiant) {
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm),
      child: Carte(
        padding: const EdgeInsets.symmetric(
            horizontal: Espaces.lg, vertical: Espaces.md),
        enfant: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: etudiant.enrole ? Couleurs.royal50 : Couleurs.alerteFond,
                shape: BoxShape.circle,
              ),
              child: Text(
                etudiant.initiales,
                style: Typo.legende.copyWith(
                  fontWeight: FontWeight.w600,
                  color: etudiant.enrole ? Couleurs.indigo600 : Couleurs.alerte,
                ),
              ),
            ),
            const SizedBox(width: Espaces.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(etudiant.nomComplet,
                      style: Typo.libelle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 3),
                  Text(etudiant.matricule,
                      style: Typo.mono(11.5, couleur: Couleurs.encrePale)),
                ],
              ),
            ),
            const SizedBox(width: Espaces.sm),
            if (!etudiant.actif)
              const Pastille(
                texte: 'Inactif',
                teinte: Couleurs.encreDiscrete,
                fond: Couleurs.traitPale,
              )
            else if (etudiant.enrole)
              const Pastille(
                texte: 'Enrôlé',
                icone: Icons.fingerprint_rounded,
                teinte: Couleurs.succes,
                fond: Couleurs.succesFond,
              )
            else
              const Pastille(
                texte: 'Non enrôlé',
                icone: Icons.fingerprint_rounded,
                teinte: Couleurs.alerte,
                fond: Couleurs.alerteFond,
              ),
          ],
        ),
      ),
    );
  }
}
