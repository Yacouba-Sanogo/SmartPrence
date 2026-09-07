import 'package:flutter/material.dart';

import '../core/auth/auth_service.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../models/profil.dart';
import '../widgets/communs.dart';
import '../widgets/entete.dart';

/// Profil de l'utilisateur connecté et déconnexion.
///
/// Écran unique pour l'étudiant et l'enseignant : seul le bloc de rattachement change.
/// Les dupliquer aurait fait diverger deux pages identiques à 90 %.
class ProfilScreen extends StatelessWidget {
  const ProfilScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  Widget build(BuildContext context) {
    final profil = auth.profil;

    return Scaffold(
      backgroundColor: Couleurs.fond,
      body: ListView(
        padding: EdgeInsets.zero,
        children: [
          _enteteIdentite(profil),
          Transform.translate(
            offset: const Offset(0, -30),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(
                  Espaces.md + 2, 0, Espaces.md + 2, Espaces.xxxl * 3),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
            Text(profil?.estEnseignant == true ? 'Rattachement' : 'Scolarité',
                style: Typo.suretitre),
            const SizedBox(height: Espaces.sm),
            _rattachement(profil),
            const SizedBox(height: Espaces.lg),
            _noteBiometrie(),
            const SizedBox(height: Espaces.xl),
            OutlinedButton.icon(
              onPressed: () => _confirmerDeconnexion(context),
              icon: const Icon(Icons.logout_rounded, size: 19),
              label: const Text('Se déconnecter'),
              style: OutlinedButton.styleFrom(
                foregroundColor: Couleurs.danger,
                side: const BorderSide(color: Couleurs.dangerTrait),
                minimumSize: const Size(0, 48),
              ),
            ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Identité posée dans l'en-tête dégradé plutôt que dans une carte blanche.
  ///
  /// C'est la seule page où la personne est le sujet : lui donner le dégradé entier
  /// vaut mieux qu'un bandeau générique suivi d'un portrait en vignette.
  Widget _enteteIdentite(Profil? profil) {
    return EnteteDegrade(
      debordement: 40,
      padding: const EdgeInsets.fromLTRB(
          Espaces.lg, Espaces.xl, Espaces.lg, Espaces.lg),
      enfant: Column(
        children: [
          Container(
            width: 74,
            height: 74,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.18),
              borderRadius: BorderRadius.circular(Rayons.bloc),
            ),
            child: Text(
              profil?.initiales ?? '?',
              style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 26),
            ),
          ),
          const SizedBox(height: Espaces.md),
          Text(profil?.nomComplet ?? '—',
              style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19),
              textAlign: TextAlign.center),
          if (profil?.matricule != null) ...[
            const SizedBox(height: 3),
            Text(profil!.matricule!,
                style: Typo.mono(12,
                    couleur: Colors.white.withValues(alpha: 0.72))),
          ],
          if (profil?.estEnseignant == true) ...[
            const SizedBox(height: Espaces.sm + 2),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.18),
                borderRadius: BorderRadius.circular(Rayons.pilule),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.cast_for_education_rounded,
                      size: 13, color: Colors.white),
                  const SizedBox(width: 5),
                  Text('Enseignant',
                      style: Typo.legende.copyWith(
                          color: Colors.white,
                          fontSize: 10.5,
                          fontWeight: FontWeight.w600)),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _rattachement(Profil? profil) {
    final lignes = profil?.estEnseignant == true
        ? <Widget>[
            _ligne('Service', profil?.service),
            _separateur(),
            _ligne('Email', profil?.email),
          ]
        : <Widget>[
            _ligne('Classe', profil?.classeLibelle),
            _separateur(),
            _ligne('Promotion', profil?.promotionLibelle),
            _separateur(),
            _ligne('Email', profil?.email),
          ];

    return Carte(padding: EdgeInsets.zero, enfant: Column(children: lignes));
  }

  Widget _noteBiometrie() {
    return Carte(
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: Couleurs.succesFond,
              borderRadius: BorderRadius.circular(Rayons.md),
            ),
            child: const Icon(Icons.shield_outlined, size: 19, color: Couleurs.succes),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Vos empreintes ne quittent pas le capteur', style: Typo.libelle),
                const SizedBox(height: 4),
                Text(
                  "Le serveur ne conserve qu'une référence logique. Aucune donnée "
                  'biométrique ne circule sur le réseau ni ne figure en base.',
                  style: Typo.legende,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _ligne(String libelle, String? valeur) {
    return Padding(
      padding: const EdgeInsets.symmetric(
          horizontal: Espaces.lg, vertical: Espaces.md + 2),
      child: Row(
        children: [
          Text(libelle, style: Typo.corpsAttenue),
          const Spacer(),
          Flexible(
            child: Text(
              valeur?.isNotEmpty == true ? valeur! : '—',
              style: Typo.libelle,
              textAlign: TextAlign.right,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _separateur() =>
      const Divider(height: 1, indent: Espaces.lg, endIndent: Espaces.lg);

  Future<void> _confirmerDeconnexion(BuildContext context) async {
    final confirme = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Se déconnecter', style: Typo.titreSection),
        content: Text(
          'Vous devrez saisir à nouveau vos identifiants à la prochaine ouverture.',
          style: Typo.corpsAttenue,
        ),
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(Rayons.carte)),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Couleurs.danger),
            child: const Text('Se déconnecter'),
          ),
        ],
      ),
    );

    if (confirme == true) {
      await auth.deconnecter();
    }
  }
}
