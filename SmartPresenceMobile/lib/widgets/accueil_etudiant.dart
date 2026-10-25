import 'package:flutter/material.dart';

import '../core/dates.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../models/annonce.dart';
import '../models/profil.dart';
import '../models/seance.dart';
import '../models/statistiques.dart';
import 'anneau.dart';
import 'communs.dart';
import 'logo_enetp.dart';

/// En-tête de l'accueil étudiant.
///
/// Trois étages : l'établissement et les notifications, la personne, puis le dernier
/// passage. Le bloc bleu a les coins bas arrondis et le contenu suit à plat : rien ne
/// chevauche, donc rien n'est rogné, et le dégradé s'arrête où l'information s'arrête.
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

    return Container(
      width: double.infinity,
      decoration: const BoxDecoration(
        gradient: Couleurs.degradeEntete,
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(30)),
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          // Un seul disque, très discret : il casse l'aplat sans devenir un motif.
          Positioned(
            right: -60,
            top: haut + 20,
            child: Container(
              width: 210,
              height: 210,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.white.withValues(alpha: 0.05),
              ),
            ),
          ),
          Padding(
            padding: EdgeInsets.fromLTRB(Espaces.xl, haut + Espaces.md, Espaces.xl, Espaces.xl),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    const LogoEnetp(taille: 30, plaque: true),
                    const SizedBox(width: Espaces.md),
                    Expanded(
                      child: Text('ENETP',
                          style: Typo.libelle.copyWith(
                              color: Colors.white, fontSize: 15, letterSpacing: 1.2, fontWeight: FontWeight.w700)),
                    ),
                    _BoutonCloche(nonLues: nonLues, surTap: surNotifications),
                  ],
                ),
                const SizedBox(height: Espaces.xl),
                Row(
                  children: [
                    GestureDetector(
                      onTap: surProfil,
                      child: Container(
                        width: 68,
                        height: 68,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                            colors: [Colors.white.withValues(alpha: 0.30), Colors.white.withValues(alpha: 0.10)],
                          ),
                          borderRadius: BorderRadius.circular(22),
                          border: Border.all(color: Colors.white.withValues(alpha: 0.55), width: 1.5),
                        ),
                        child: Text(
                          profil?.initiales ?? '?',
                          style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w700),
                        ),
                      ),
                    ),
                    const SizedBox(width: Espaces.lg),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text('Bonjour,',
                              style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.75), fontSize: 13)),
                          Text(
                            profil?.nomComplet.isNotEmpty == true ? profil!.nomComplet : 'Étudiant',
                            style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 22, height: 1.2),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: Espaces.sm),
                          Wrap(
                            spacing: 6,
                            runSpacing: 6,
                            children: [
                              if (classe != null) _Puce(icone: Icons.school_rounded, texte: classe),
                              if (profil?.matricule != null) _Puce(icone: Icons.badge_outlined, texte: profil!.matricule!),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: Espaces.lg + 2),
                passage,
              ],
            ),
          ),
        ],
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
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(Rayons.pilule),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icone, size: 13, color: Colors.white.withValues(alpha: 0.9)),
          const SizedBox(width: 5),
          Flexible(
            child: Text(texte,
                style: Typo.legende.copyWith(color: Colors.white, fontSize: 11.5, fontWeight: FontWeight.w500),
                maxLines: 1,
                overflow: TextOverflow.ellipsis),
          ),
        ],
      ),
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
                decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.14), shape: BoxShape.circle),
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

/// Panneau d'assiduité : l'anneau, l'appréciation, la semaine, puis les trois chiffres.
class PanneauAssiduite extends StatelessWidget {
  const PanneauAssiduite({super.key, required this.assiduite, required this.semaine});

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
      enfant: Column(
        children: [
          Row(
            children: [
              Anneau(valeur: assiduite.taux, taille: 92, epaisseur: 10),
              const SizedBox(width: Espaces.lg + 2),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(_appreciation, style: Typo.libelle.copyWith(fontSize: 15)),
                    const SizedBox(height: 2),
                    Text('Cette semaine', style: Typo.legendePale),
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
          const SizedBox(height: Espaces.lg),
          const Divider(height: 1, thickness: 1, color: Couleurs.traitLeger),
          const SizedBox(height: Espaces.lg),
          IntrinsicHeight(
            child: Row(
              children: [
                _Chiffre(
                    icone: Icons.check_circle_rounded,
                    valeur: assiduite.presents,
                    libelle: 'Présences',
                    teinte: Couleurs.succes,
                    fond: Couleurs.succesFond),
                const VerticalDivider(width: 1, thickness: 1, color: Couleurs.traitLeger),
                _Chiffre(
                    icone: Icons.schedule_rounded,
                    valeur: assiduite.retards,
                    libelle: 'Retards',
                    teinte: Couleurs.alerte,
                    fond: Couleurs.alerteFond),
                const VerticalDivider(width: 1, thickness: 1, color: Couleurs.traitLeger),
                _Chiffre(
                    icone: Icons.cancel_rounded,
                    valeur: assiduite.absents,
                    libelle: 'Absences',
                    teinte: Couleurs.danger,
                    fond: Couleurs.dangerFond),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Chiffre extends StatelessWidget {
  const _Chiffre({
    required this.icone,
    required this.valeur,
    required this.libelle,
    required this.teinte,
    required this.fond,
  });

  final IconData icone;
  final int valeur;
  final String libelle;
  final Color teinte;
  final Color fond;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: [
          Container(
            width: 34,
            height: 34,
            decoration: BoxDecoration(color: fond, borderRadius: BorderRadius.circular(11)),
            child: Icon(icone, size: 18, color: teinte),
          ),
          const SizedBox(height: Espaces.sm),
          Text('$valeur', style: Typo.mono(22, graisse: FontWeight.w600, couleur: Couleurs.encre)),
          Text(libelle, style: Typo.legende.copyWith(fontSize: 11.5, color: Couleurs.encreAttenuee)),
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
      child: Container(
        padding: const EdgeInsets.all(Espaces.lg + 2),
        decoration: BoxDecoration(
          gradient: Couleurs.degradeEntete,
          borderRadius: BorderRadius.circular(Rayons.carte),
          boxShadow: Couleurs.ombreRoyal,
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text('PROCHAINE SÉANCE',
                          style: Typo.legende.copyWith(
                              color: Colors.white.withValues(alpha: 0.7),
                              fontSize: 10.5,
                              letterSpacing: 1.1,
                              fontWeight: FontWeight.w600)),
                      const SizedBox(width: Espaces.sm),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(
                          color: s.seDerouleMaintenant ? Couleurs.succesVif : Colors.white.withValues(alpha: 0.18),
                          borderRadius: BorderRadius.circular(Rayons.pilule),
                        ),
                        child: Text(_echeance(s),
                            style: Typo.legende.copyWith(color: Colors.white, fontSize: 10.5, fontWeight: FontWeight.w700)),
                      ),
                    ],
                  ),
                  const SizedBox(height: Espaces.sm),
                  Text(s.matiere,
                      style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19, height: 1.25),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis),
                  const SizedBox(height: Espaces.sm),
                  Row(
                    children: [
                      Icon(Icons.schedule_rounded, size: 14, color: Colors.white.withValues(alpha: 0.8)),
                      const SizedBox(width: 5),
                      Text('${Dates.relatif(s.debut)} · ${s.creneau}',
                          style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.9), fontSize: 12)),
                      if (s.salle != null) ...[
                        const SizedBox(width: Espaces.md),
                        Icon(Icons.meeting_room_outlined, size: 14, color: Colors.white.withValues(alpha: 0.8)),
                        const SizedBox(width: 5),
                        Flexible(
                          child: Text(s.salle!,
                              style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.9), fontSize: 12),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: Espaces.md),
            Container(
              width: 40,
              height: 40,
              decoration: const BoxDecoration(color: Colors.white, shape: BoxShape.circle),
              child: const Icon(Icons.arrow_forward_rounded, size: 20, color: Couleurs.royal700),
            ),
          ],
        ),
      ),
    );
  }
}

/// Raccourci de l'accueil : même gabarit pour les quatre, deux par ligne.
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
      rayon: Rayons.carte - 2,
      padding: const EdgeInsets.all(Espaces.md + 2),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [Couleurs.royal500, Couleurs.royal700],
                  ),
                  borderRadius: BorderRadius.circular(13),
                ),
                child: Icon(icone, size: 21, color: Colors.white),
              ),
              const Spacer(),
              const Icon(Icons.chevron_right_rounded, size: 22, color: Couleurs.encrePale),
            ],
          ),
          const SizedBox(height: Espaces.md),
          Text(libelle, style: Typo.libelle.copyWith(fontSize: 13.5), maxLines: 1, overflow: TextOverflow.ellipsis),
          const SizedBox(height: 2),
          Text(detail, style: Typo.legendePale.copyWith(fontSize: 11), maxLines: 1, overflow: TextOverflow.ellipsis),
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
  const TitreSection(this.titre, {super.key, this.action, this.surAction});

  final String titre;
  final String? action;
  final VoidCallback? surAction;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(Espaces.xs, 0, Espaces.xs, Espaces.md - 2),
      child: Row(
        children: [
          Expanded(child: Text(titre, style: Typo.titreSection)),
          if (action != null)
            GestureDetector(
              onTap: surAction,
              behavior: HitTestBehavior.opaque,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Text(action!,
                    style: Typo.legende.copyWith(color: Couleurs.royal600, fontWeight: FontWeight.w600, fontSize: 12.5)),
              ),
            ),
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
