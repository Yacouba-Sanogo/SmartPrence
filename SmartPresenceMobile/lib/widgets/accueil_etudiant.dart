import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../core/dates.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../core/preferences_app.dart';
import '../models/annonce.dart';
import '../models/profil.dart';
import '../models/seance.dart';
import '../models/statistiques.dart';
import 'anneau.dart';
import 'fond_campus.dart';
import 'communs.dart';
import 'logo_enetp.dart';

/// En-tête de l'accueil étudiant.
///
/// La photo du bâtiment de l'école, teintée de bleu royal, occupe le fond : à gauche elle
/// s'efface sous le dégradé pour que le texte reste lisible, à droite elle reste visible
/// avec la devise. Trois étages : l'établissement et les notifications, la personne, puis
/// le dernier passage. Les coins bas arrondis ferment le bloc sans chevaucher le contenu.
class EnteteAccueil extends StatelessWidget {
  const EnteteAccueil({
    super.key,
    required this.profil,
    required this.passage,
    this.nonLues = 0,
    this.surProfil,
    this.surNotifications,
  });

  final Profil? profil;

  /// Bandeau « dernier passage », fourni par l'écran qui connaît les relevés.
  final Widget passage;
  final int nonLues;
  final VoidCallback? surProfil;
  final VoidCallback? surNotifications;

  @override
  Widget build(BuildContext context) {
    final haut = MediaQuery.of(context).padding.top;
    final classe = profil?.classeLibelle ?? profil?.classeCode;
    // Texte agrandi : la devise ferait déborder l'identité, on la retire plutôt que de tout comprimer.
    final texteGrand = MediaQuery.textScalerOf(context).scale(10) > 11.5;

    return ProtegeCouleurs(
      child: Container(
      width: double.infinity,
      decoration: const BoxDecoration(
        gradient: Couleurs.degradeEntete,
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(30)),
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          const Positioned.fill(child: FondPhotoCampus()),
          Padding(
            padding: EdgeInsets.fromLTRB(Espaces.xl, haut + Espaces.md, Espaces.xl, Espaces.lg + 2),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    const LogoEnetp(taille: 30, plaque: true),
                    const SizedBox(width: Espaces.md),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('ENETP',
                              style: Typo.libelle.copyWith(
                                  color: Colors.white, fontSize: 16, letterSpacing: 1.2, fontWeight: FontWeight.w700, height: 1.1)),
                          Text('École Nationale d’Économie\net de Technique de Planification',
                              style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.85), fontSize: 8.5, height: 1.25)),
                        ],
                      ),
                    ),
                    _BoutonCloche(nonLues: nonLues, surTap: surNotifications),
                  ],
                ),
                const SizedBox(height: Espaces.lg),
                Stack(
                  children: [
                    Row(
                      children: [
                        GestureDetector(
                          onTap: surProfil,
                          child: ListenableBuilder(
                            listenable: ContexteApp.of(context).photo,
                            builder: (context, _) => AvatarProfil(
                              initiales: profil?.initiales ?? '?',
                              octets: ContexteApp.of(context).photo.octets,
                              taille: 64,
                            ),
                          ),
                        ),
                        const SizedBox(width: Espaces.md + 2),
                        Expanded(
                          child: Padding(
                            // Place à la devise, à droite, sur la photo.
                            padding: EdgeInsets.only(right: texteGrand ? 0 : 104),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text('Bonjour,',
                                    style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.8), fontSize: 13)),
                                Text(
                                  profil?.nomComplet.isNotEmpty == true ? profil!.nomComplet : 'Étudiant',
                                  style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 21, height: 1.18),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                if (classe != null)
                                  Padding(
                                    padding: const EdgeInsets.only(top: 5),
                                    child: Row(
                                      children: [
                                        Icon(Icons.school_rounded, size: 15, color: Colors.white.withValues(alpha: 0.9)),
                                        const SizedBox(width: 6),
                                        Flexible(
                                          child: Text(classe,
                                              style: Typo.legende.copyWith(color: Colors.white, fontSize: 12),
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis),
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
                    if (!texteGrand) Positioned(right: 0, bottom: 0, child: _Devise()),
                  ],
                ),
                const SizedBox(height: Espaces.lg),
                passage,
              ],
            ),
          ),
        ],
      ),
    ));
  }
}

class _Devise extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'Apprendre\naujourd’hui,\nconstruire demain',
          style: GoogleFonts.caveat(color: Colors.white, fontSize: 16, height: 1.05, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 3),
        Container(
          width: 92,
          height: 2.5,
          decoration: BoxDecoration(color: Couleurs.royal300, borderRadius: BorderRadius.circular(2)),
        ),
      ],
    );
  }
}

/// Avatar rond : la photo de la personne si elle en a une, sinon ses initiales.
class AvatarProfil extends StatelessWidget {
  const AvatarProfil({super.key, required this.initiales, this.octets, this.taille = 64});

  final String initiales;
  final Uint8List? octets;
  final double taille;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: taille,
      height: taille,
      alignment: Alignment.center,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Couleurs.royal400, Couleurs.royal600],
        ),
        border: Border.all(color: Colors.white.withValues(alpha: 0.65), width: 2),
      ),
      child: octets != null
          ? Image.memory(octets!, width: taille, height: taille, fit: BoxFit.cover, gaplessPlayback: true)
          : Text(initiales,
              style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: taille * 0.34, fontWeight: FontWeight.w700)),
    );
  }
}

class _BoutonCloche extends StatelessWidget {
  const _BoutonCloche({required this.nonLues, this.surTap});

  final int nonLues;
  final VoidCallback? surTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: nonLues > 0 ? 'Notifications, $nonLues non lues' : 'Notifications',
      child: GestureDetector(
        onTap: surTap,
        behavior: HitTestBehavior.opaque,
        child: SizedBox(
          width: 44,
          height: 44,
          child: Stack(
            alignment: Alignment.center,
            clipBehavior: Clip.none,
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.16), shape: BoxShape.circle),
                child: const Icon(Icons.notifications_none_rounded, size: 22, color: Colors.white),
              ),
              if (nonLues > 0)
                Positioned(
                  top: 0,
                  right: 0,
                  child: Container(
                    constraints: const BoxConstraints(minWidth: 18),
                    padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                    decoration: BoxDecoration(
                      color: Couleurs.dangerVif,
                      borderRadius: BorderRadius.circular(Rayons.pilule),
                      border: Border.all(color: Couleurs.royal800, width: 1.5),
                    ),
                    child: Text(nonLues > 9 ? '9+' : '$nonLues',
                        textAlign: TextAlign.center,
                        style: Typo.legende.copyWith(color: Colors.white, fontSize: 9.5, fontWeight: FontWeight.w700)),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Carte d'assiduité : l'anneau, l'appréciation en une phrase, la semaine en cinq points.
class CarteAssiduite extends StatelessWidget {
  const CarteAssiduite({super.key, required this.assiduite, required this.semaine});

  final Assiduite assiduite;
  final List<({String lettre, EtatJour etat})> semaine;

  String get _appreciation {
    final t = assiduite.taux;
    if (t >= 90) return 'Excellente assiduité';
    if (t >= 75) return 'Assiduité correcte';
    if (t >= 50) return 'À surveiller';
    return 'Assiduité insuffisante';
  }

  @override
  Widget build(BuildContext context) {
    if (assiduite.sansHistorique) {
      return const SectionVide(
        icone: Icons.fingerprint_rounded,
        titre: 'Aucune présence enregistrée',
        detail: 'Votre assiduité s’affichera ici dès votre premier passage devant un lecteur.',
      );
    }

    return Carte(
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Row(
        children: [
          Anneau(valeur: assiduite.taux, taille: 88, epaisseur: 10, couleur: Couleurs.royal600),
          const SizedBox(width: Espaces.lg + 2),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    Expanded(child: Text(_appreciation, style: Typo.libelle.copyWith(fontSize: 15.5))),
                    if (assiduite.taux >= 90)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(color: Couleurs.royal50, borderRadius: BorderRadius.circular(Rayons.pilule)),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.star_rounded, size: 13, color: Couleurs.royal600),
                            const SizedBox(width: 3),
                            Text('Bravo !', style: Typo.legende.copyWith(color: Couleurs.royal600, fontWeight: FontWeight.w700, fontSize: 11)),
                          ],
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 3),
                Text('Vous êtes présent(e) à ${assiduite.taux.round()} % des séances.',
                    style: Typo.corpsAttenue.copyWith(fontSize: 12.5)),
                const SizedBox(height: Espaces.md),
                Row(
                  children: [
                    for (final jour in semaine) ...[
                      PointJour(lettre: jour.lettre, etat: jour.etat),
                      const SizedBox(width: 7),
                    ],
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Une statistique d'assiduité : icône, libellé, valeur, volume total et mouvement de la semaine.
class CarteStatistique extends StatelessWidget {
  const CarteStatistique({
    super.key,
    required this.icone,
    required this.libelle,
    required this.valeur,
    required this.total,
    required this.cetteSemaine,
    required this.teinte,
    required this.fond,
  });

  final IconData icone;
  final String libelle;
  final int valeur;
  final int total;

  /// Relevés de la semaine en cours pour ce statut.
  final int cetteSemaine;
  final Color teinte;
  final Color fond;

  @override
  Widget build(BuildContext context) {
    final calme = cetteSemaine == 0;
    return Carte(
      rayon: Rayons.carte - 2,
      padding: const EdgeInsets.fromLTRB(Espaces.md, Espaces.md + 2, Espaces.md, Espaces.md),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              Container(
                width: 30,
                height: 30,
                decoration: BoxDecoration(color: fond, shape: BoxShape.circle),
                child: Icon(icone, size: 16, color: teinte),
              ),
              const SizedBox(width: 7),
              Expanded(
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(libelle, style: Typo.libelle.copyWith(fontSize: 12.5), maxLines: 1),
                ),
              ),
            ],
          ),
          const SizedBox(height: Espaces.md),
          Text('$valeur', style: Typo.mono(26, graisse: FontWeight.w600, couleur: Couleurs.encre)),
          Text('sur $total séances', style: Typo.legendePale.copyWith(fontSize: 10.5)),
          const SizedBox(height: Espaces.sm + 2),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 5),
            decoration: BoxDecoration(
              color: calme ? Couleurs.traitPale : fond,
              borderRadius: BorderRadius.circular(Rayons.sm - 2),
            ),
            child: Row(
              children: [
                Icon(calme ? Icons.remove_rounded : Icons.arrow_upward_rounded, size: 13, color: calme ? Couleurs.encreDiscrete : teinte),
                const SizedBox(width: 3),
                Expanded(
                  child: FittedBox(
                    fit: BoxFit.scaleDown,
                    alignment: Alignment.centerLeft,
                    child: Text(
                      calme ? 'aucun cette semaine' : '+$cetteSemaine cette semaine',
                      style: Typo.legende.copyWith(fontSize: 10.5, fontWeight: FontWeight.w600, color: calme ? Couleurs.encreDiscrete : teinte),
                      maxLines: 1,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Bannière de la prochaine séance : l'information la plus utile de la journée.
class BanniereSeance extends StatelessWidget {
  const BanniereSeance({super.key, required this.seance, this.surTap});

  final Seance? seance;
  final VoidCallback? surTap;

  /// « En cours », « Dans 2 h 30 », « Demain »… ce qu'on dirait à voix haute.
  String _echeance(Seance s) {
    if (s.seDerouleMaintenant) return 'En cours';
    final reste = s.debut.difference(DateTime.now());
    if (reste.inMinutes < 60) return 'Dans ${reste.inMinutes.clamp(1, 59)} min';
    if (Dates.memeJour(s.debut, DateTime.now())) {
      final h = reste.inHours;
      final m = reste.inMinutes % 60;
      return m == 0 ? 'Dans $h h' : 'Dans $h h ${m.toString().padLeft(2, '0')}';
    }
    return Dates.relatif(s.debut);
  }

  @override
  Widget build(BuildContext context) {
    final s = seance;
    if (s == null) {
      return Carte(
        padding: const EdgeInsets.all(Espaces.lg),
        enfant: Row(
          children: [
            Container(
              width: 46,
              height: 46,
              decoration: BoxDecoration(color: Couleurs.royal50, borderRadius: BorderRadius.circular(Rayons.md)),
              child: const Icon(Icons.event_available_rounded, size: 24, color: Couleurs.royal500),
            ),
            const SizedBox(width: Espaces.md + 2),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Aucune séance à venir', style: Typo.libelle.copyWith(fontSize: 14)),
                  const SizedBox(height: 2),
                  Text('Votre emploi du temps est libre sur les sept prochains jours.',
                      style: Typo.corpsAttenue.copyWith(fontSize: 12.5)),
                ],
              ),
            ),
          ],
        ),
      );
    }

    return GestureDetector(
      onTap: surTap,
      child: ProtegeCouleurs(child: Container(
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          gradient: Couleurs.degradeEntete,
          borderRadius: BorderRadius.circular(Rayons.carte),
          boxShadow: Couleurs.ombreRoyal,
        ),
        child: Stack(
          children: [
            Positioned(
              right: -30,
              top: -40,
              child: Container(
                width: 150,
                height: 150,
                decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.white.withValues(alpha: 0.06)),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(Espaces.lg + 2),
              child: IntrinsicHeight(
                child: Row(
                  children: [
                    Container(
                      width: 52,
                      height: 52,
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(Rayons.md),
                      ),
                      child: const Icon(Icons.calendar_month_rounded, size: 26, color: Colors.white),
                    ),
                    const SizedBox(width: Espaces.md),
                    Container(width: 1, color: Colors.white.withValues(alpha: 0.22)),
                    const SizedBox(width: Espaces.md),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Row(
                            children: [
                              Text('PROCHAINE SÉANCE',
                                  style: Typo.legende.copyWith(
                                      color: Colors.white.withValues(alpha: 0.7),
                                      fontSize: 10,
                                      letterSpacing: 1.0,
                                      fontWeight: FontWeight.w600)),
                              const SizedBox(width: Espaces.sm),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                                decoration: BoxDecoration(
                                  color: s.seDerouleMaintenant ? Couleurs.succesVif : Colors.white.withValues(alpha: 0.18),
                                  borderRadius: BorderRadius.circular(Rayons.pilule),
                                ),
                                child: Text(_echeance(s),
                                    style: Typo.legende.copyWith(color: Colors.white, fontSize: 10, fontWeight: FontWeight.w700)),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(s.matiere,
                              style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 17, height: 1.2),
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis),
                          const SizedBox(height: 6),
                          Row(
                            children: [
                              Icon(Icons.schedule_rounded, size: 13, color: Colors.white.withValues(alpha: 0.8)),
                              const SizedBox(width: 4),
                              Text('${Dates.relatif(s.debut)} · ${s.creneau}',
                                  style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.92), fontSize: 11.5)),
                            ],
                          ),
                          if (s.salle != null)
                            Padding(
                              padding: const EdgeInsets.only(top: 3),
                              child: Row(
                                children: [
                                  Icon(Icons.place_rounded, size: 13, color: Colors.white.withValues(alpha: 0.8)),
                                  const SizedBox(width: 4),
                                  Flexible(
                                    child: Text(s.salle!,
                                        style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.92), fontSize: 11.5),
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis),
                                  ),
                                ],
                              ),
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(width: Espaces.sm),
                    Container(
                      width: 38,
                      height: 38,
                      decoration: const BoxDecoration(color: Colors.white, shape: BoxShape.circle),
                      child: const Icon(Icons.arrow_forward_rounded, size: 19, color: Couleurs.royal700),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      )),
    );
  }
}

/// Raccourci de l'accueil : icône à gauche, titre et consigne, flèche. Les quatre ont le même gabarit.
class RaccourciAccueil extends StatelessWidget {
  const RaccourciAccueil({
    super.key,
    required this.icone,
    required this.libelle,
    required this.detail,
    required this.surTap,
  });

  final IconData icone;
  final String libelle;
  final String detail;
  final VoidCallback surTap;

  @override
  Widget build(BuildContext context) {
    return Carte(
      surTap: surTap,
      rayon: Rayons.carte - 4,
      padding: const EdgeInsets.fromLTRB(Espaces.md, Espaces.md + 2, Espaces.sm, Espaces.md + 2),
      enfant: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(color: Couleurs.royal50, borderRadius: BorderRadius.circular(14)),
            child: Icon(icone, size: 22, color: Couleurs.royal600),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(libelle, style: Typo.libelle.copyWith(fontSize: 12), maxLines: 1),
                ),
                const SizedBox(height: 2),
                Text(detail, style: Typo.legendePale.copyWith(fontSize: 10.5), maxLines: 2, overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
          const Icon(Icons.arrow_forward_rounded, size: 16, color: Couleurs.royal500),
        ],
      ),
    );
  }
}

/// Rangée de deux raccourcis de même largeur, donc de même hauteur.
class RangeeRaccourcis extends StatelessWidget {
  const RangeeRaccourcis({super.key, required this.gauche, required this.droite});

  final Widget gauche;
  final Widget droite;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(child: gauche),
        const SizedBox(width: Espaces.sm + 2),
        Expanded(child: droite),
      ],
    );
  }
}

/// Titre de section, avec un lien d'action facultatif à droite.
class TitreSection extends StatelessWidget {
  const TitreSection(this.titre, {super.key, this.icone, this.action, this.surAction});

  final String titre;
  final IconData? icone;
  final String? action;
  final VoidCallback? surAction;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(Espaces.xs, 0, Espaces.xs, Espaces.md - 2),
      child: Row(
        children: [
          if (icone != null) ...[
            Icon(icone, size: 18, color: Couleurs.royal600),
            const SizedBox(width: 7),
          ],
          Expanded(child: Text(titre, style: Typo.titreSection)),
          if (action != null)
            GestureDetector(
              onTap: surAction,
              behavior: HitTestBehavior.opaque,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  children: [
                    Text(action!,
                        style: Typo.legende.copyWith(color: Couleurs.royal600, fontWeight: FontWeight.w600, fontSize: 12.5)),
                    const SizedBox(width: 3),
                    const Icon(Icons.arrow_forward_rounded, size: 14, color: Couleurs.royal600),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// Activité récente : les dernières notifications, avec leur ancienneté.
class ActiviteRecente extends StatelessWidget {
  const ActiviteRecente({super.key, required this.notifications, this.surTap});

  final List<Annonce> notifications;
  final VoidCallback? surTap;

  /// « Il y a 2 heures », « Hier », « 23 oct. ».
  static String ancienne(DateTime d) {
    final ecart = DateTime.now().difference(d);
    if (ecart.inMinutes < 1) return 'À l’instant';
    if (ecart.inMinutes < 60) return 'Il y a ${ecart.inMinutes} min';
    if (ecart.inHours < 24 && Dates.memeJour(d, DateTime.now())) {
      return 'Il y a ${ecart.inHours} heure${ecart.inHours > 1 ? 's' : ''}';
    }
    return Dates.relatif(d);
  }

  @override
  Widget build(BuildContext context) {
    final recentes = notifications.take(3).toList();
    if (recentes.isEmpty) {
      return const SectionVide(
        icone: Icons.notifications_none_rounded,
        titre: 'Rien de nouveau',
        detail: 'Les annonces, rappels et nouveaux supports de cours apparaîtront ici.',
      );
    }

    return Carte(
      padding: EdgeInsets.zero,
      enfant: Column(
        children: [
          for (var i = 0; i < recentes.length; i++) ...[
            if (i > 0) const Divider(height: 1, thickness: 1, indent: Espaces.lg, endIndent: Espaces.lg, color: Couleurs.traitLeger),
            InkWell(
              onTap: surTap,
              borderRadius: BorderRadius.circular(Rayons.carte),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: Espaces.lg, vertical: Espaces.md),
                child: Row(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(color: recentes[i].type.fond, borderRadius: BorderRadius.circular(12)),
                      child: Icon(recentes[i].type.icone, size: 20, color: recentes[i].type.teinte),
                    ),
                    const SizedBox(width: Espaces.md),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(recentes[i].titre,
                              style: Typo.libelle.copyWith(fontWeight: recentes[i].lue ? FontWeight.w500 : FontWeight.w700),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis),
                          if (recentes[i].message.isNotEmpty)
                            Text(recentes[i].message, style: Typo.corpsAttenue.copyWith(fontSize: 12), maxLines: 1, overflow: TextOverflow.ellipsis),
                          const SizedBox(height: 2),
                          Text(ancienne(recentes[i].recueLe), style: Typo.legendePale.copyWith(fontSize: 10.5)),
                        ],
                      ),
                    ),
                    if (!recentes[i].lue)
                      Container(width: 8, height: 8, decoration: const BoxDecoration(color: Couleurs.royal500, shape: BoxShape.circle)),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

/// État vide d'une section : une icône, une phrase, une consigne. Jamais une carte muette.
class SectionVide extends StatelessWidget {
  const SectionVide({super.key, required this.icone, required this.titre, required this.detail});

  final IconData icone;
  final String titre;
  final String detail;

  @override
  Widget build(BuildContext context) {
    return Carte(
      padding: const EdgeInsets.symmetric(vertical: Espaces.xxl, horizontal: Espaces.xl),
      enfant: SizedBox(
        width: double.infinity,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 54,
              height: 54,
              decoration: const BoxDecoration(color: Couleurs.royal50, shape: BoxShape.circle),
              child: Icon(icone, size: 26, color: Couleurs.royal500),
            ),
            const SizedBox(height: Espaces.md + 2),
            Text(titre, style: Typo.libelle.copyWith(fontSize: 14), textAlign: TextAlign.center),
            const SizedBox(height: 4),
            Text(detail, style: Typo.corpsAttenue.copyWith(fontSize: 12.5), textAlign: TextAlign.center),
          ],
        ),
      ),
    );
  }
}

/// Feuille des notifications de l'étudiant : annonces, rappels de la bibliothèque, nouveaux supports.
class FeuilleNotifications extends StatelessWidget {
  const FeuilleNotifications({super.key, required this.notifications, this.surToutLire});

  final List<Annonce> notifications;
  final VoidCallback? surToutLire;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxHeight: MediaQuery.of(context).size.height * 0.78),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: Espaces.md),
            Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(color: Couleurs.trait, borderRadius: BorderRadius.circular(Rayons.pilule)),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(Espaces.xl, Espaces.lg, Espaces.md, Espaces.sm),
              child: Row(
                children: [
                  Expanded(child: Text('Notifications', style: Typo.titreEcran.copyWith(fontSize: 19))),
                  if (notifications.any((n) => !n.lue))
                    TextButton(onPressed: surToutLire, child: const Text('Tout marquer comme lu')),
                ],
              ),
            ),
            Flexible(
              child: notifications.isEmpty
                  ? const Padding(
                      padding: EdgeInsets.fromLTRB(Espaces.xl, Espaces.md, Espaces.xl, Espaces.xxl),
                      child: SectionVide(
                        icone: Icons.notifications_none_rounded,
                        titre: 'Rien de nouveau',
                        detail: 'Les annonces, rappels et nouveaux supports de cours apparaîtront ici.',
                      ),
                    )
                  : ListView.separated(
                      shrinkWrap: true,
                      padding: const EdgeInsets.fromLTRB(Espaces.lg, 0, Espaces.lg, Espaces.xl),
                      itemCount: notifications.length,
                      separatorBuilder: (_, __) => const SizedBox(height: Espaces.sm),
                      itemBuilder: (_, i) => _LigneNotification(notifications[i]),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LigneNotification extends StatelessWidget {
  const _LigneNotification(this.n);

  final Annonce n;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(Espaces.md + 2),
      decoration: BoxDecoration(
        color: n.lue ? Couleurs.carte : Couleurs.royal50,
        borderRadius: BorderRadius.circular(Rayons.tuile),
        border: Border.all(color: n.lue ? Couleurs.traitLeger : Couleurs.royal200),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(color: n.type.fond, borderRadius: BorderRadius.circular(12)),
            child: Icon(n.type.icone, size: 19, color: n.type.teinte),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(n.titre,
                    style: Typo.libelle.copyWith(fontWeight: n.lue ? FontWeight.w500 : FontWeight.w700),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
                if (n.message.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(n.message, style: Typo.corpsAttenue.copyWith(fontSize: 12.5), maxLines: 3, overflow: TextOverflow.ellipsis),
                ],
                const SizedBox(height: 4),
                Text('${Dates.relatif(n.recueLe)} · ${Dates.heure(n.recueLe)}', style: Typo.legendePale.copyWith(fontSize: 11)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
