import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';

/// Carte de base : blanche, très arrondie, posée sur le fond par une ombre douce.
///
/// L'ombre remplace la bordure de la version précédente. Une bordure grise cerne et
/// aplatit ; une ombre détache, et c'est ce détachement qui donne à l'écran son relief.
class Carte extends StatelessWidget {
  const Carte({
    super.key,
    required this.enfant,
    this.padding,
    this.surTap,
    this.rayon = Rayons.carte,
    this.ombre,
    this.couleur,
  });

  final Widget enfant;
  final EdgeInsetsGeometry? padding;
  final VoidCallback? surTap;
  final double rayon;
  final List<BoxShadow>? ombre;
  final Color? couleur;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: couleur ?? Couleurs.carte,
        borderRadius: BorderRadius.circular(rayon),
        boxShadow: ombre ?? Couleurs.ombreCarte,
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(rayon),
        child: InkWell(
          onTap: surTap,
          borderRadius: BorderRadius.circular(rayon),
          child: Padding(
            padding: padding ?? const EdgeInsets.all(Espaces.lg),
            child: enfant,
          ),
        ),
      ),
    );
  }
}

/// Pastille de statut, en pilule.
class Pastille extends StatelessWidget {
  const Pastille({
    super.key,
    required this.texte,
    required this.teinte,
    required this.fond,
    this.icone,
    this.compact = false,
  });

  final String texte;
  final Color teinte;
  final Color fond;
  final IconData? icone;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(
          horizontal: compact ? 7 : 9, vertical: compact ? 2.5 : 4),
      decoration: BoxDecoration(
        color: fond,
        borderRadius: BorderRadius.circular(Rayons.pilule),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icone != null) ...[
            Icon(icone, size: compact ? 11 : 12.5, color: teinte),
            const SizedBox(width: 4),
          ],
          Text(
            texte,
            style: Typo.legende.copyWith(
              color: teinte,
              fontWeight: FontWeight.w600,
              fontSize: compact ? 9.5 : 10.5,
              height: 1.3,
            ),
          ),
        ],
      ),
    );
  }
}

/// Tuile d'accès rapide : icône teintée dans un carré arrondi, libellé dessous.
class Tuile extends StatelessWidget {
  const Tuile({
    super.key,
    required this.icone,
    required this.libelle,
    required this.teinte,
    required this.fondIcone,
    this.surTap,
    this.badge,
  });

  final IconData icone;
  final String libelle;
  final Color teinte;
  final Color fondIcone;
  final VoidCallback? surTap;

  /// Nombre affiché en coin — les signalements en attente, par exemple.
  final int? badge;

  @override
  Widget build(BuildContext context) {
    return Carte(
      surTap: surTap,
      rayon: Rayons.tuile,
      padding: const EdgeInsets.symmetric(vertical: Espaces.md + 1, horizontal: 6),
      enfant: Stack(
        clipBehavior: Clip.none,
        children: [
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 36,
                height: 36,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: fondIcone,
                  borderRadius: BorderRadius.circular(Rayons.md - 2),
                ),
                child: Icon(icone, size: 18, color: teinte),
              ),
              const SizedBox(height: Espaces.sm - 1),
              Text(
                libelle,
                style: Typo.legende.copyWith(
                    fontSize: 10.5, color: Couleurs.encreAttenuee, height: 1.2),
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
          if (badge != null && badge! > 0)
            Positioned(
              top: -4,
              right: 2,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                constraints: const BoxConstraints(minWidth: 17),
                decoration: BoxDecoration(
                  color: Couleurs.dangerVif,
                  borderRadius: BorderRadius.circular(Rayons.pilule),
                ),
                child: Text(
                  '${badge!}',
                  textAlign: TextAlign.center,
                  style: Typo.legende.copyWith(
                      color: Colors.white, fontSize: 9.5, fontWeight: FontWeight.w700),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// Compteur compact : un nombre coloré et son libellé, dans une carte étroite.
class Compteur extends StatelessWidget {
  const Compteur({
    super.key,
    required this.valeur,
    required this.libelle,
    required this.teinte,
    this.surFondColore = false,
  });

  final String valeur;
  final String libelle;
  final Color teinte;

  /// Variante posée dans un en-tête dégradé : fond translucide au lieu de blanc.
  final bool surFondColore;

  @override
  Widget build(BuildContext context) {
    final contenu = Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(valeur, style: Typo.mono(17, graisse: FontWeight.w600, couleur: teinte)),
        const SizedBox(height: 1),
        Text(
          libelle,
          style: Typo.legende.copyWith(
            fontSize: 9.5,
            color: surFondColore
                ? Colors.white.withValues(alpha: 0.68)
                : Couleurs.encrePale,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ],
    );

    if (surFondColore) {
      return Container(
        padding: const EdgeInsets.symmetric(vertical: Espaces.sm, horizontal: Espaces.xs),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.14),
          borderRadius: BorderRadius.circular(Rayons.md - 1),
        ),
        child: contenu,
      );
    }

    return Carte(
      rayon: Rayons.md + 1,
      padding: const EdgeInsets.symmetric(vertical: Espaces.md - 1, horizontal: 6),
      enfant: contenu,
    );
  }
}

/// Écran d'état : chargement, absence de données, ou erreur.
class EtatVide extends StatelessWidget {
  const EtatVide({
    super.key,
    required this.titre,
    this.detail,
    this.icone = Icons.info_outline_rounded,
    this.enErreur = false,
    this.libelleAction,
    this.surAction,
  });

  final String titre;
  final String? detail;
  final IconData icone;
  final bool enErreur;
  final String? libelleAction;
  final VoidCallback? surAction;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(
            horizontal: Espaces.xxl, vertical: Espaces.xxxl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 62,
              height: 62,
              decoration: BoxDecoration(
                color: enErreur ? Couleurs.dangerFond : Couleurs.indigo50,
                borderRadius: BorderRadius.circular(Rayons.carte),
              ),
              child: Icon(icone,
                  size: 27,
                  color: enErreur ? Couleurs.dangerVif : Couleurs.indigo400),
            ),
            const SizedBox(height: Espaces.lg),
            Text(titre, style: Typo.titreSection, textAlign: TextAlign.center),
            if (detail != null) ...[
              const SizedBox(height: Espaces.sm),
              Text(detail!, style: Typo.corpsAttenue, textAlign: TextAlign.center),
            ],
            if (libelleAction != null && surAction != null) ...[
              const SizedBox(height: Espaces.xl),
              FilledButton(
                onPressed: surAction,
                style: FilledButton.styleFrom(
                  minimumSize: const Size(170, 46),
                  backgroundColor: Couleurs.indigo600,
                ),
                child: Text(libelleAction!),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Bandeau d'information ou d'alerte.
class Bandeau extends StatelessWidget {
  const Bandeau({
    super.key,
    required this.message,
    required this.teinte,
    required this.fond,
    required this.bordure,
    this.icone = Icons.info_outline_rounded,
    this.surFermeture,
  });

  const Bandeau.erreur({super.key, required this.message, this.surFermeture})
      : teinte = Couleurs.danger,
        fond = Couleurs.dangerFond,
        bordure = Couleurs.dangerTrait,
        icone = Icons.error_outline_rounded;

  const Bandeau.attention({super.key, required this.message, this.surFermeture})
      : teinte = Couleurs.alerte,
        fond = Couleurs.alerteFond,
        bordure = Couleurs.alerteTrait,
        icone = Icons.fingerprint_rounded;

  final String message;
  final Color teinte;
  final Color fond;
  final Color bordure;
  final IconData icone;
  final VoidCallback? surFermeture;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(Espaces.md - 1),
      decoration: BoxDecoration(
        color: fond,
        border: Border.all(color: bordure),
        borderRadius: BorderRadius.circular(Rayons.tuile),
        boxShadow: Couleurs.ombreCarte,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icone, size: 18, color: teinte),
          const SizedBox(width: Espaces.sm + 1),
          Expanded(
            child: Text(message,
                style: Typo.legende.copyWith(
                    color: teinte, fontSize: 11.5, height: 1.5)),
          ),
          if (surFermeture != null)
            GestureDetector(
              onTap: surFermeture,
              child: Icon(Icons.close_rounded,
                  size: 17, color: teinte.withValues(alpha: 0.7)),
            ),
        ],
      ),
    );
  }
}

/// Indicateur chiffré large, pour un tableau de bord.
class Indicateur extends StatelessWidget {
  const Indicateur({
    super.key,
    required this.libelle,
    required this.valeur,
    required this.teinte,
    this.detail,
    this.icone,
  });

  final String libelle;
  final String valeur;
  final Color teinte;
  final String? detail;
  final IconData? icone;

  @override
  Widget build(BuildContext context) {
    return Carte(
      padding: const EdgeInsets.symmetric(
          horizontal: Espaces.lg, vertical: Espaces.md + 2),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              if (icone != null) ...[
                Icon(icone, size: 14, color: teinte),
                const SizedBox(width: 5),
              ],
              Expanded(
                child: Text(libelle,
                    style: Typo.legende, maxLines: 1, overflow: TextOverflow.ellipsis),
              ),
            ],
          ),
          const SizedBox(height: Espaces.sm),
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(valeur, style: Typo.chiffreCle.copyWith(color: teinte)),
              if (detail != null) ...[
                const SizedBox(width: 6),
                Expanded(
                  child: Text(detail!,
                      style: Typo.legendePale,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}

/// Point de la semaine : un jour, et l'état qui lui correspond.
class PointJour extends StatelessWidget {
  const PointJour({
    super.key,
    required this.lettre,
    required this.etat,
  });

  final String lettre;
  final EtatJour etat;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(lettre, style: Typo.legendePale.copyWith(fontSize: 10)),
        const SizedBox(height: 4),
        Container(
          width: 24,
          height: 24,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: etat.fond,
            border: etat.trait == null ? null : Border.all(color: etat.trait!, width: 1.5),
          ),
          child: etat.icone == null
              ? null
              : Icon(etat.icone, size: 12, color: Colors.white),
        ),
      ],
    );
  }
}

/// État possible d'une journée dans la bande hebdomadaire.
enum EtatJour {
  present,
  retard,
  absent,
  aVenir;

  Color get fond => switch (this) {
        EtatJour.present => Couleurs.succesVif,
        EtatJour.retard => Couleurs.alerteVif,
        EtatJour.absent => Couleurs.dangerVif,
        EtatJour.aVenir => Colors.transparent,
      };

  Color? get trait => this == EtatJour.aVenir ? Couleurs.trait : null;

  IconData? get icone => switch (this) {
        EtatJour.present => Icons.check_rounded,
        EtatJour.retard => Icons.schedule_rounded,
        EtatJour.absent => Icons.close_rounded,
        EtatJour.aVenir => null,
      };
}
