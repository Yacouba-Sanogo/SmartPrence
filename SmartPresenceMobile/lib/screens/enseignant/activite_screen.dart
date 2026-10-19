import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/enseignement.dart';
import '../../services/enseignant_service.dart';
import '../../models/gestion.dart';
import '../../widgets/anneau.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import 'conges_screen.dart';
import 'reclamations_screen.dart';

/// Activité de l'enseignant : sa propre présence et le bilan de ses cours.
///
/// Deux questions qu'il se pose en fin de journée : « le lecteur m'a-t-il bien relevé ? »
/// et « combien de séances me reste-t-il à déclarer ? ». Les pointages viennent du
/// lecteur d'entrée, au même titre que ceux du reste du personnel — l'enseignant les
/// consulte, il ne les saisit pas.
class ActiviteScreen extends StatefulWidget {
  const ActiviteScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<ActiviteScreen> createState() => _ActiviteScreenState();
}

class _ActiviteScreenState extends State<ActiviteScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<JourneePointage> _journees = const [];
  BilanEnseignement? _bilan;
  int _reclamationsOuvertes = 0;
  int _congesEnAttente = 0;
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
      final maintenant = DateTime.now();
      final resultats = await Future.wait([
        _service.mesPointages(),
        _service.bilan(debut: DateTime(maintenant.year, maintenant.month, 1), fin: maintenant),
        _service.reclamationsRecues(),
        _service.mesConges(),
      ]);
      if (!mounted) return;
      setState(() {
        _journees = resultats[0] as List<JourneePointage>;
        _bilan = resultats[1] as BilanEnseignement;
        _reclamationsOuvertes = (resultats[2] as List<Reclamation>).where((r) => r.ouverte).length;
        _congesEnAttente =
            (resultats[3] as List<DemandeConge>).where((d) => d.statut == StatutDemande.enAttente).length;
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

  JourneePointage? get _aujourdhui {
    for (final j in _journees) {
      if (Dates.estAujourdhui(j.date)) return j;
    }
    return null;
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
                Text('Mon activité',
                    style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 21)),
                const SizedBox(height: 3),
                Text(
                    '${Dates.moisLongs[DateTime.now().month - 1]} ${DateTime.now().year}',
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
    if (_erreur != null || _bilan == null) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Activité indisponible',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }

    final bilan = _bilan!;
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      children: [
        ApparitionEnCascade(rang: 0, enfant: _carteAujourdhui()),
        const SizedBox(height: Espaces.md),
        ApparitionEnCascade(rang: 1, enfant: _raccourcis()),
        const SizedBox(height: Espaces.md),
        if (bilan.aDeclarer > 0) ...[
          Bandeau(
            icone: Icons.pending_actions_rounded,
            teinte: Couleurs.alerte,
            fond: Couleurs.alerteFond,
            bordure: Couleurs.alerteTrait,
            message: bilan.aDeclarer == 1
                ? '1 séance passée attend votre déclaration. Ouvrez-la depuis '
                    '« Journée » pour la déclarer effectuée.'
                : '${bilan.aDeclarer} séances passées attendent votre déclaration. '
                    'Ouvrez-les depuis « Journée » pour les déclarer effectuées.',
          ),
          const SizedBox(height: Espaces.md),
        ],
        ApparitionEnCascade(rang: 1, enfant: _carteBilan(bilan)),
        if (bilan.parMatiere.isNotEmpty) ...[
          const SizedBox(height: Espaces.md),
          ApparitionEnCascade(rang: 2, enfant: _carteMatieres(bilan)),
        ],
        const SizedBox(height: Espaces.lg),
        Text('Derniers pointages', style: Typo.suretitre),
        const SizedBox(height: Espaces.sm),
        if (_journees.isEmpty)
          Carte(
            padding: const EdgeInsets.symmetric(vertical: Espaces.xl, horizontal: Espaces.lg),
            enfant: Text(
              'Aucun pointage relevé sur les trente derniers jours.',
              textAlign: TextAlign.center,
              style: Typo.corpsAttenue,
            ),
          )
        else
          ..._journees.take(10).toList().asMap().entries.map(
              (e) => ApparitionEnCascade(rang: e.key + 3, enfant: _ligneJournee(e.value))),
      ],
    );
  }

  /// Congés et réclamations : deux démarches rares mais qui ne doivent pas attendre,
  /// d'où le compteur sur chaque tuile.
  Widget _raccourcis() {
    Widget tuile(IconData icone, String libelle, int compteur, String sous, Widget Function() ecran) {
      return Expanded(
        child: Carte(
          surTap: () async {
            await Navigator.of(context).push<void>(routeDouce<void>((_) => ecran()));
            if (mounted) _charger();
          },
          padding: const EdgeInsets.all(Espaces.md + 2),
          enfant: Row(
            children: [
              Stack(
                clipBehavior: Clip.none,
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: Couleurs.royal50,
                      borderRadius: BorderRadius.circular(Rayons.md),
                    ),
                    child: Icon(icone, size: 20, color: Couleurs.indigo600),
                  ),
                  if (compteur > 0)
                    Positioned(
                      top: -5,
                      right: -5,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                        decoration: BoxDecoration(
                          color: Couleurs.alerteVif,
                          borderRadius: BorderRadius.circular(Rayons.pilule),
                        ),
                        child: Text('$compteur',
                            style: Typo.mono(10, graisse: FontWeight.w700, couleur: Colors.white)),
                      ),
                    ),
                ],
              ),
              const SizedBox(width: Espaces.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(libelle,
                        style: Typo.libelle.copyWith(fontSize: 13.5),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 2),
                    Text(sous, style: Typo.legendePale, maxLines: 1, overflow: TextOverflow.ellipsis),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    }

    return Row(
      children: [
        tuile(Icons.beach_access_rounded, 'Congés', _congesEnAttente,
            _congesEnAttente > 0 ? 'en attente' : 'Mes demandes', () => CongesScreen(auth: widget.auth)),
        const SizedBox(width: Espaces.sm + 2),
        tuile(Icons.rate_review_rounded, 'Réclamations', _reclamationsOuvertes,
            _reclamationsOuvertes > 0 ? 'à traiter' : 'Notes contestées',
            () => ReclamationsScreen(auth: widget.auth)),
      ],
    );
  }

  Widget _carteAujourdhui() {
    final jour = _aujourdhui;
    final String titre;
    final String detail;
    final IconData icone;
    final Color teinte;
    final Color fond;

    if (jour == null) {
      titre = "Pas encore pointé aujourd'hui";
      detail = 'Présentez votre doigt au lecteur d’entrée à votre arrivée.';
      icone = Icons.fingerprint_rounded;
      teinte = Couleurs.alerte;
      fond = Couleurs.alerteFond;
    } else {
      titre = jour.retard
          ? 'Arrivé à ${jour.entree} · ${jour.minutesRetard} min de retard'
          : 'Arrivé à ${jour.entree ?? '—'}';
      detail = jour.surPlace
          ? 'Vous êtes sur site.'
          : jour.sortie == null
              ? 'Aucune sortie relevée.'
              : 'Sorti à ${jour.sortie}.';
      icone = Icons.verified_rounded;
      teinte = jour.retard ? Couleurs.alerte : Couleurs.succes;
      fond = jour.retard ? Couleurs.alerteFond : Couleurs.succesFond;
    }

    return Carte(
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(color: fond, borderRadius: BorderRadius.circular(Rayons.md)),
            child: Icon(icone, color: teinte, size: 24),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(titre, style: Typo.libelle.copyWith(fontSize: 14)),
                const SizedBox(height: 3),
                Text(detail, style: Typo.legendePale),
              ],
            ),
          ),
          // Le lecteur vous a relevé et vous n'êtes pas encore ressorti : sur site.
          if (jour != null && jour.surPlace) PointVivant(couleur: teinte),
        ],
      ),
    );
  }

  Widget _carteBilan(BilanEnseignement bilan) {
    // Part des séances échues qui sont réglées : ni annulées, ni encore à déclarer.
    final echues = bilan.effectuees + bilan.aDeclarer;
    final taux = echues == 0 ? 100.0 : bilan.effectuees / echues * 100;

    return Carte(
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Séances du mois', style: Typo.suretitre),
          const SizedBox(height: Espaces.md),
          Row(
            children: [
              Anneau(
                valeur: taux,
                taille: 96,
                epaisseur: 10,
                couleur: bilan.aDeclarer == 0 ? Couleurs.succesVif : Couleurs.indigo600,
                centre: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CompteurAnime(
                      valeur: bilan.effectuees,
                      style: Typo.mono(24, graisse: FontWeight.w700, couleur: Couleurs.encre),
                    ),
                    Text('déclarées', style: Typo.legendePale.copyWith(fontSize: 10.5)),
                  ],
                ),
              ),
              const SizedBox(width: Espaces.lg),
              Expanded(
                child: Column(
                  children: [
                    _ligneChiffre(Icons.pending_actions_rounded, 'À déclarer', bilan.aDeclarer,
                        bilan.aDeclarer > 0 ? Couleurs.alerte : Couleurs.encrePale),
                    const SizedBox(height: Espaces.sm),
                    _ligneChiffre(Icons.event_busy_rounded, 'Annulées', bilan.annulees,
                        bilan.annulees > 0 ? Couleurs.danger : Couleurs.encrePale),
                    const SizedBox(height: Espaces.sm),
                    _ligneChiffre(Icons.fingerprint_rounded, 'Jours pointés', bilan.joursPointes,
                        Couleurs.indigo600),
                    const SizedBox(height: Espaces.sm),
                    _ligneChiffre(Icons.schedule_rounded, 'En retard', bilan.joursEnRetard,
                        bilan.joursEnRetard > 0 ? Couleurs.alerte : Couleurs.encrePale),
                  ],
                ),
              ),
            ],
          ),
          const Divider(height: Espaces.xl),
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: Couleurs.royal50,
                  borderRadius: BorderRadius.circular(Rayons.sm),
                ),
                child: const Icon(Icons.timer_outlined, size: 18, color: Couleurs.indigo600),
              ),
              const SizedBox(width: Espaces.md),
              Expanded(child: Text('Heures enseignées', style: Typo.corps)),
              Text(bilan.heures,
                  style: Typo.mono(18, graisse: FontWeight.w700, couleur: Couleurs.indigo600)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _ligneChiffre(IconData icone, String libelle, int valeur, Color teinte) {
    return Row(
      children: [
        Icon(icone, size: 16, color: teinte),
        const SizedBox(width: Espaces.sm),
        Expanded(child: Text(libelle, style: Typo.legende.copyWith(fontSize: 12.5))),
        CompteurAnime(
            valeur: valeur, style: Typo.mono(15, graisse: FontWeight.w600, couleur: teinte)),
      ],
    );
  }

  Widget _carteMatieres(BilanEnseignement bilan) {
    return Carte(
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Heures par matière', style: Typo.suretitre),
          const SizedBox(height: Espaces.sm),
          ...bilan.parMatiere.map((m) {
            final h = m.minutes ~/ 60;
            final min = m.minutes % 60;
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 6),
              child: Column(
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(m.libelle,
                            style: Typo.corps, maxLines: 1, overflow: TextOverflow.ellipsis),
                      ),
                      Text('${m.effectuees} séance${m.effectuees > 1 ? 's' : ''}',
                          style: Typo.legendePale),
                      const SizedBox(width: Espaces.md),
                      Text(min == 0 ? '$h h' : '$h h ${min.toString().padLeft(2, '0')}',
                          style: Typo.mono(13, graisse: FontWeight.w600)),
                    ],
                  ),
                  const SizedBox(height: 5),
                  BarreAnimee(
                    valeur: bilan.minutes == 0 ? 0 : m.minutes / bilan.minutes,
                    couleur: Couleurs.royal500,
                    hauteur: 4,
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _ligneJournee(JourneePointage j) {
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm),
      child: Carte(
        padding: const EdgeInsets.symmetric(horizontal: Espaces.lg, vertical: Espaces.md),
        enfant: Row(
          children: [
            Expanded(
              child: Text(Dates.jourEtDate(j.date), style: Typo.libelle.copyWith(fontSize: 13)),
            ),
            Text('${j.entree ?? '—'} → ${j.sortie ?? (j.surPlace ? 'sur site' : '—')}',
                style: Typo.mono(12.5, couleur: Couleurs.encreAttenuee)),
            if (j.retard) ...[
              const SizedBox(width: Espaces.sm),
              const Pastille(
                  texte: 'Retard', teinte: Couleurs.alerte, fond: Couleurs.alerteFond, compact: true),
            ],
          ],
        ),
      ),
    );
  }
}
