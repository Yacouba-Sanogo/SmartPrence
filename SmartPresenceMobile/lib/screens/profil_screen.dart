import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';

import '../core/auth/auth_service.dart';
import '../core/design/animations.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../core/preferences_app.dart';
import '../models/profil.dart';
import '../widgets/accueil_etudiant.dart';
import '../widgets/choix_option.dart';
import '../widgets/communs.dart';
import '../widgets/fond_campus.dart';
import '../widgets/mot_de_passe_sheet.dart';
import 'bibliotheque_screen.dart';

/// Profil de l'utilisateur connecté, ses réglages et la déconnexion.
///
/// Écran unique pour l'étudiant et l'enseignant : seuls le bloc de scolarité et quelques
/// raccourcis changent. Les réglages d'apparence agissent sur toute l'application et sont
/// conservés sur l'appareil ; la photo aussi — le serveur n'a pas de champ photo.
class ProfilScreen extends StatelessWidget {
  const ProfilScreen({super.key, required this.auth});

  final AuthService auth;

  static const String version = '1.0.0';

  @override
  Widget build(BuildContext context) {
    final profil = auth.profil;
    final contexte = ContexteApp.of(context);
    final prefs = contexte.preferences;

    return Scaffold(
      backgroundColor: Couleurs.fond,
      body: ListView(
        padding: EdgeInsets.zero,
        children: [
          _Entete(profil: profil, contexte: contexte),
          Padding(
            padding: const EdgeInsets.fromLTRB(Espaces.lg, Espaces.lg, Espaces.lg, 130),
            child: ListenableBuilder(
              listenable: prefs,
              builder: (context, _) => Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (profil?.estEtudiant == true) _CarteNiveau(profil: profil!),
                  if (profil?.estEtudiant == true) const SizedBox(height: Espaces.xl),
                  const _Titre('Informations'),
                  _Informations(profil: profil),
                  if (profil?.estEtudiant == true) ...[
                    const SizedBox(height: Espaces.xl),
                    ChoixOption(client: auth.client),
                  ],
                  const SizedBox(height: Espaces.xl),
                  const _Titre('Apparence'),
                  _Apparence(prefs: prefs),
                  const SizedBox(height: Espaces.xl),
                  const _Titre('Préférences'),
                  _Preferences(prefs: prefs, auth: auth),
                  const SizedBox(height: Espaces.xl),
                  const _Titre('Raccourcis'),
                  Carte(
                    padding: EdgeInsets.zero,
                    enfant: Column(
                      children: [
                        _Action(
                          icone: Icons.local_library_rounded,
                          libelle: 'Bibliothèque',
                          detail: 'Mémoires, anciens sujets, livres',
                          surTap: () => Navigator.of(context).push<void>(routeDouce<void>((_) => BibliothequeScreen(auth: auth))),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: Espaces.xl),
                  const _Titre('Sécurité'),
                  Carte(
                    padding: EdgeInsets.zero,
                    enfant: Column(
                      children: [
                        _Action(
                          icone: Icons.lock_reset_rounded,
                          libelle: 'Changer mon mot de passe',
                          detail: 'Choisissez un mot de passe que vous seul connaissez',
                          surTap: () => _changerMotDePasse(context),
                        ),
                        const Divider(height: 1, indent: 64, color: Couleurs.traitLeger),
                        const _NoteBiometrie(),
                      ],
                    ),
                  ),
                  const SizedBox(height: Espaces.xl),
                  const _Titre('À propos'),
                  const _APropos(),
                  const SizedBox(height: Espaces.xl),
                  OutlinedButton.icon(
                    onPressed: () => _confirmerDeconnexion(context),
                    icon: const Icon(Icons.logout_rounded, size: 19),
                    label: const Text('Se déconnecter'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Couleurs.danger,
                      side: const BorderSide(color: Couleurs.dangerTrait),
                      minimumSize: const Size(0, 50),
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

  Future<void> _changerMotDePasse(BuildContext context) async {
    final change = await ouvrirChangementMotDePasse(context, auth.client);
    if (change && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Mot de passe modifié.'), behavior: SnackBarBehavior.floating),
      );
    }
  }

  Future<void> _confirmerDeconnexion(BuildContext context) async {
    final confirme = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Se déconnecter', style: Typo.titreSection),
        content: Text(
          'Vous devrez saisir à nouveau vos identifiants à la prochaine ouverture.',
          style: Typo.corpsAttenue,
        ),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Rayons.carte)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Couleurs.danger),
            child: const Text('Se déconnecter'),
          ),
        ],
      ),
    );
    if (confirme == true) await auth.deconnecter();
  }
}

// ---------------------------------------------------------------------------
// En-tête : photo, nom, rôle
// ---------------------------------------------------------------------------

class _Entete extends StatelessWidget {
  const _Entete({required this.profil, required this.contexte});

  final Profil? profil;
  final ContexteApp contexte;

  @override
  Widget build(BuildContext context) {
    final haut = MediaQuery.of(context).padding.top;
    final enseignant = profil?.estEnseignant == true;

    return ProtegeCouleurs(
      child: Container(
        width: double.infinity,
        clipBehavior: Clip.antiAlias,
        decoration: const BoxDecoration(
          gradient: Couleurs.degradeEntete,
          borderRadius: BorderRadius.vertical(bottom: Radius.circular(30)),
        ),
        child: Stack(
          children: [
            const Positioned.fill(child: FondPhotoCampus()),
            Padding(
              padding: EdgeInsets.fromLTRB(Espaces.xl, haut + Espaces.xl, Espaces.xl, Espaces.xl + 4),
              child: Column(
                children: [
                  ListenableBuilder(
                    listenable: contexte.photo,
                    builder: (context, _) => _AvatarModifiable(
                      initiales: profil?.initiales ?? '?',
                      octets: contexte.photo.octets,
                      surTap: () => _ouvrirChoixPhoto(context, contexte.photo),
                    ),
                  ),
                  const SizedBox(height: Espaces.md + 2),
                  Text(
                    profil?.nomComplet.isNotEmpty == true ? profil!.nomComplet : '—',
                    style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 22),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: Espaces.sm),
                  Wrap(
                    alignment: WrapAlignment.center,
                    spacing: 8,
                    runSpacing: 6,
                    children: [
                      _Puce(
                        icone: enseignant ? Icons.cast_for_education_rounded : Icons.school_rounded,
                        texte: enseignant ? 'Enseignant' : 'Étudiant',
                      ),
                      if (profil?.matricule != null) _Puce(icone: Icons.badge_outlined, texte: profil!.matricule!),
                    ],
                  ),
                  if (profil?.classeLibelle != null) ...[
                    const SizedBox(height: Espaces.sm),
                    Text(profil!.classeLibelle!,
                        style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.85), fontSize: 13)),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _ouvrirChoixPhoto(BuildContext context, PhotoProfil photo) async {
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Couleurs.fond,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(Rayons.bloc + 4))),
      builder: (feuille) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(Espaces.lg, Espaces.md, Espaces.lg, Espaces.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 36,
                  height: 4,
                  decoration: BoxDecoration(color: Couleurs.trait, borderRadius: BorderRadius.circular(Rayons.pilule)),
                ),
              ),
              const SizedBox(height: Espaces.lg),
              Text('Photo de profil', style: Typo.titreEcran.copyWith(fontSize: 19)),
              const SizedBox(height: Espaces.xs),
              Text('Elle reste sur votre téléphone : personne d’autre ne la voit.', style: Typo.corpsAttenue),
              const SizedBox(height: Espaces.lg),
              Carte(
                padding: EdgeInsets.zero,
                enfant: Column(
                  children: [
                    _Action(
                      icone: Icons.photo_camera_rounded,
                      libelle: 'Prendre une photo',
                      surTap: () {
                        Navigator.pop(feuille);
                        _choisir(context, photo, ImageSource.camera);
                      },
                    ),
                    const Divider(height: 1, indent: 64, color: Couleurs.traitLeger),
                    _Action(
                      icone: Icons.photo_library_rounded,
                      libelle: 'Choisir dans la galerie',
                      surTap: () {
                        Navigator.pop(feuille);
                        _choisir(context, photo, ImageSource.gallery);
                      },
                    ),
                    if (photo.octets != null) ...[
                      const Divider(height: 1, indent: 64, color: Couleurs.traitLeger),
                      _Action(
                        icone: Icons.delete_outline_rounded,
                        libelle: 'Retirer la photo',
                        danger: true,
                        surTap: () {
                          Navigator.pop(feuille);
                          photo.retirer();
                        },
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _choisir(BuildContext context, PhotoProfil photo, ImageSource source) async {
    final messager = ScaffoldMessenger.of(context);
    try {
      final fichier = await ImagePicker().pickImage(source: source, maxWidth: 720, maxHeight: 720, imageQuality: 85);
      if (fichier == null) return;
      await photo.definir(await fichier.readAsBytes());
      messager.showSnackBar(const SnackBar(content: Text('Photo mise à jour.'), behavior: SnackBarBehavior.floating));
    } catch (_) {
      messager.showSnackBar(const SnackBar(
        content: Text('Impossible d’ouvrir la photo. Vérifiez l’autorisation de l’appareil photo ou de la galerie.'),
        behavior: SnackBarBehavior.floating,
      ));
    }
  }
}

class _AvatarModifiable extends StatelessWidget {
  const _AvatarModifiable({required this.initiales, required this.octets, required this.surTap});

  final String initiales;
  final Uint8List? octets;
  final VoidCallback surTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Modifier la photo de profil',
      child: GestureDetector(
        onTap: surTap,
        child: SizedBox(
          width: 108,
          height: 108,
          child: Stack(
            clipBehavior: Clip.none,
            children: [
              Container(
                padding: const EdgeInsets.all(3),
                decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.white.withValues(alpha: 0.22)),
                child: AvatarProfil(initiales: initiales, octets: octets, taille: 102),
              ),
              Positioned(
                right: 0,
                bottom: 2,
                child: Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    shape: BoxShape.circle,
                    boxShadow: Couleurs.ombreFlottante,
                  ),
                  child: const Icon(Icons.photo_camera_rounded, size: 18, color: Couleurs.royal600),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Puce extends StatelessWidget {
  const _Puce({required this.icone, required this.texte});

  final IconData icone;
  final String texte;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.16), borderRadius: BorderRadius.circular(Rayons.pilule)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icone, size: 13, color: Colors.white.withValues(alpha: 0.9)),
          const SizedBox(width: 5),
          Text(texte, style: Typo.legende.copyWith(color: Colors.white, fontSize: 11.5, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Niveau et informations
// ---------------------------------------------------------------------------

/// Niveau d'études et filière, lus dans le libellé de la classe (« Licence 1 Informatique »).
class _CarteNiveau extends StatelessWidget {
  const _CarteNiveau({required this.profil});

  final Profil profil;

  @override
  Widget build(BuildContext context) {
    final libelle = profil.classeLibelle ?? '';
    final lu = RegExp(r'^(Licence|Master|Doctorat)\s*(\d)?\s*(.*)$', caseSensitive: false).firstMatch(libelle);
    final niveau = lu == null ? (profil.classeCode ?? '—') : '${lu.group(1)}${lu.group(2) == null ? '' : ' ${lu.group(2)}'}';
    final filiere = lu == null || (lu.group(3) ?? '').isEmpty ? (profil.promotionLibelle ?? '—') : lu.group(3)!;

    return Row(
      children: [
        Expanded(child: _Bloc(icone: Icons.stairs_rounded, libelle: 'Niveau', valeur: niveau)),
        const SizedBox(width: Espaces.sm + 2),
        Expanded(child: _Bloc(icone: Icons.account_tree_rounded, libelle: 'Filière', valeur: filiere)),
      ],
    );
  }
}

class _Bloc extends StatelessWidget {
  const _Bloc({required this.icone, required this.libelle, required this.valeur});

  final IconData icone;
  final String libelle;
  final String valeur;

  @override
  Widget build(BuildContext context) {
    return Carte(
      rayon: Rayons.carte - 4,
      padding: const EdgeInsets.all(Espaces.md + 2),
      enfant: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(color: Couleurs.royal50, borderRadius: BorderRadius.circular(13)),
            child: Icon(icone, size: 20, color: Couleurs.royal600),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(libelle, style: Typo.legendePale.copyWith(fontSize: 11)),
                FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(valeur, style: Typo.libelle.copyWith(fontSize: 14), maxLines: 1),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Informations extends StatelessWidget {
  const _Informations({required this.profil});

  final Profil? profil;

  @override
  Widget build(BuildContext context) {
    final lignes = <(IconData, String, String?, bool)>[
      if (profil?.estEnseignant == true) (Icons.apartment_rounded, 'Service', profil?.service, false),
      (Icons.mail_outline_rounded, 'Email', profil?.email, true),
      if (profil?.matricule != null) (Icons.badge_outlined, 'Matricule', profil?.matricule, true),
      if (profil?.estEtudiant == true) (Icons.groups_rounded, 'Classe', profil?.classeLibelle ?? profil?.classeCode, false),
      if (profil?.estEtudiant == true) (Icons.calendar_today_rounded, 'Promotion', profil?.promotionLibelle, false),
    ];

    return Carte(
      padding: EdgeInsets.zero,
      enfant: Column(
        children: [
          for (var i = 0; i < lignes.length; i++) ...[
            if (i > 0) const Divider(height: 1, indent: 64, color: Couleurs.traitLeger),
            _LigneInfo(icone: lignes[i].$1, libelle: lignes[i].$2, valeur: lignes[i].$3, copiable: lignes[i].$4),
          ],
        ],
      ),
    );
  }
}

class _LigneInfo extends StatelessWidget {
  const _LigneInfo({required this.icone, required this.libelle, required this.valeur, required this.copiable});

  final IconData icone;
  final String libelle;
  final String? valeur;
  final bool copiable;

  @override
  Widget build(BuildContext context) {
    final present = valeur?.isNotEmpty == true;
    return InkWell(
      borderRadius: BorderRadius.circular(Rayons.carte),
      onTap: copiable && present
          ? () async {
              await Clipboard.setData(ClipboardData(text: valeur!));
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('$libelle copié.'), behavior: SnackBarBehavior.floating, duration: const Duration(seconds: 2)),
                );
              }
            }
          : null,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: Espaces.lg, vertical: Espaces.md + 1),
        child: Row(
          children: [
            Icon(icone, size: 20, color: Couleurs.royal500),
            const SizedBox(width: Espaces.md + 4),
            Text(libelle, style: Typo.corpsAttenue),
            const SizedBox(width: Espaces.md),
            Expanded(
              child: Text(
                present ? valeur! : '—',
                style: Typo.libelle,
                textAlign: TextAlign.right,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            if (copiable && present) ...[
              const SizedBox(width: 6),
              const Icon(Icons.copy_rounded, size: 14, color: Couleurs.encrePale),
            ],
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Réglages
// ---------------------------------------------------------------------------

class _Apparence extends StatelessWidget {
  const _Apparence({required this.prefs});

  final PreferencesApp prefs;

  @override
  Widget build(BuildContext context) {
    const modes = [
      (ModeTheme.clair, Icons.light_mode_rounded, 'Clair'),
      (ModeTheme.sombre, Icons.dark_mode_rounded, 'Sombre'),
      (ModeTheme.systeme, Icons.brightness_auto_rounded, 'Auto'),
    ];

    return Carte(
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Thème', style: Typo.libelle),
          const SizedBox(height: Espaces.md),
          Row(
            children: [
              for (final (mode, icone, texte) in modes) ...[
                Expanded(
                  child: GestureDetector(
                    onTap: () {
                      prefs.vibrer();
                      prefs.definirTheme(mode);
                    },
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      padding: const EdgeInsets.symmetric(vertical: Espaces.md),
                      decoration: BoxDecoration(
                        color: prefs.theme == mode ? Couleurs.royal600 : Couleurs.royal50,
                        borderRadius: BorderRadius.circular(Rayons.tuile),
                      ),
                      child: Column(
                        children: [
                          Icon(icone, size: 22, color: prefs.theme == mode ? Colors.white : Couleurs.royal600),
                          const SizedBox(height: 4),
                          Text(texte,
                              style: Typo.legende.copyWith(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w700,
                                  color: prefs.theme == mode ? Colors.white : Couleurs.royal700)),
                        ],
                      ),
                    ),
                  ),
                ),
                if (mode != modes.last.$1) const SizedBox(width: Espaces.sm),
              ],
            ],
          ),
          const SizedBox(height: Espaces.xl),
          Text('Taille du texte', style: Typo.libelle),
          const SizedBox(height: Espaces.md),
          Row(
            children: [
              for (final (nom, echelle) in PreferencesApp.taillesTexte) ...[
                Expanded(
                  child: GestureDetector(
                    onTap: () {
                      prefs.vibrer();
                      prefs.definirEchelleTexte(echelle);
                    },
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      alignment: Alignment.center,
                      padding: const EdgeInsets.symmetric(vertical: Espaces.sm + 2),
                      decoration: BoxDecoration(
                        color: prefs.echelleTexte == echelle ? Couleurs.royal600 : Couleurs.carte,
                        borderRadius: BorderRadius.circular(Rayons.pilule),
                        border: Border.all(color: prefs.echelleTexte == echelle ? Couleurs.royal600 : Couleurs.trait),
                      ),
                      child: Text(nom,
                          maxLines: 1,
                          style: Typo.legende.copyWith(
                              fontSize: 11.5,
                              fontWeight: FontWeight.w600,
                              color: prefs.echelleTexte == echelle ? Colors.white : Couleurs.encreAttenuee)),
                    ),
                  ),
                ),
                if (echelle != PreferencesApp.taillesTexte.last.$2) const SizedBox(width: 6),
              ],
            ],
          ),
          const SizedBox(height: Espaces.md),
          Text('Voici à quoi ressemblera le texte dans toute l’application.', style: Typo.corpsAttenue),
        ],
      ),
    );
  }
}

class _Preferences extends StatelessWidget {
  const _Preferences({required this.prefs, required this.auth});

  final PreferencesApp prefs;
  final AuthService auth;

  @override
  Widget build(BuildContext context) {
    return Carte(
      padding: EdgeInsets.zero,
      enfant: Column(
        children: [
          _Interrupteur(
            icone: Icons.vibration_rounded,
            libelle: 'Vibrations',
            detail: 'Un léger retour au toucher',
            valeur: prefs.vibrations,
            surChange: prefs.definirVibrations,
          ),
          const Divider(height: 1, indent: 64, color: Couleurs.traitLeger),
          _Interrupteur(
            icone: Icons.animation_rounded,
            libelle: 'Animations réduites',
            detail: 'Des transitions plus sobres',
            valeur: prefs.animationsReduites,
            surChange: prefs.definirAnimationsReduites,
          ),
          const Divider(height: 1, indent: 64, color: Couleurs.traitLeger),
          _Action(
            icone: Icons.cleaning_services_rounded,
            libelle: 'Vider les données hors ligne',
            detail: 'Efface ce que l’application garde pour fonctionner sans réseau',
            surTap: () async {
              await auth.client.viderCache();
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Données hors ligne effacées.'), behavior: SnackBarBehavior.floating),
                );
              }
            },
          ),
        ],
      ),
    );
  }
}

class _Interrupteur extends StatelessWidget {
  const _Interrupteur({
    required this.icone,
    required this.libelle,
    required this.detail,
    required this.valeur,
    required this.surChange,
  });

  final IconData icone;
  final String libelle;
  final String detail;
  final bool valeur;
  final ValueChanged<bool> surChange;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(Espaces.lg, Espaces.sm, Espaces.md, Espaces.sm),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(color: Couleurs.royal50, borderRadius: BorderRadius.circular(11)),
            child: Icon(icone, size: 19, color: Couleurs.royal600),
          ),
          const SizedBox(width: Espaces.md + 2),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(libelle, style: Typo.libelle),
                Text(detail, style: Typo.legendePale.copyWith(fontSize: 11)),
              ],
            ),
          ),
          Switch.adaptive(value: valeur, onChanged: surChange, activeThumbColor: Couleurs.royal600),
        ],
      ),
    );
  }
}

class _Action extends StatelessWidget {
  const _Action({required this.icone, required this.libelle, this.detail, required this.surTap, this.danger = false});

  final IconData icone;
  final String libelle;
  final String? detail;
  final VoidCallback surTap;
  final bool danger;

  @override
  Widget build(BuildContext context) {
    final teinte = danger ? Couleurs.danger : Couleurs.royal600;
    return InkWell(
      borderRadius: BorderRadius.circular(Rayons.carte),
      onTap: surTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: Espaces.lg, vertical: Espaces.md),
        child: Row(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(color: danger ? Couleurs.dangerFond : Couleurs.royal50, borderRadius: BorderRadius.circular(11)),
              child: Icon(icone, size: 19, color: teinte),
            ),
            const SizedBox(width: Espaces.md + 2),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(libelle, style: Typo.libelle.copyWith(color: danger ? Couleurs.danger : null)),
                  if (detail != null) Text(detail!, style: Typo.legendePale.copyWith(fontSize: 11)),
                ],
              ),
            ),
            const Icon(Icons.chevron_right_rounded, size: 22, color: Couleurs.encrePale),
          ],
        ),
      ),
    );
  }
}

class _NoteBiometrie extends StatelessWidget {
  const _NoteBiometrie();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(Espaces.lg),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(color: Couleurs.succesFond, borderRadius: BorderRadius.circular(11)),
            child: const Icon(Icons.shield_outlined, size: 19, color: Couleurs.succes),
          ),
          const SizedBox(width: Espaces.md + 2),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Vos empreintes ne quittent pas le capteur', style: Typo.libelle),
                const SizedBox(height: 3),
                Text(
                  'Le serveur ne conserve qu’une référence logique. Aucune donnée biométrique ne circule sur le réseau ni ne figure en base.',
                  style: Typo.legendePale.copyWith(fontSize: 11.5, height: 1.4),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _APropos extends StatelessWidget {
  const _APropos();

  @override
  Widget build(BuildContext context) {
    return Carte(
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text('SmartPresence', style: Typo.libelle.copyWith(fontSize: 15)),
              const Spacer(),
              Text('Version ${ProfilScreen.version}', style: Typo.mono(12, couleur: Couleurs.encreDiscrete)),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            'Gestion de présence par empreinte digitale — École Nationale d’Économie et de Technique de Planification (ENETP).',
            style: Typo.corpsAttenue.copyWith(fontSize: 12.5),
          ),
        ],
      ),
    );
  }
}

class _Titre extends StatelessWidget {
  const _Titre(this.texte);

  final String texte;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(Espaces.xs, 0, 0, Espaces.sm + 2),
      child: Text(texte.toUpperCase(), style: Typo.suretitre),
    );
  }
}
