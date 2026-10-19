import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/dates.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/gestion.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import '../../widgets/formulaire.dart';

/// Demandes de congé et d'absence de l'enseignant.
///
/// La décision revient à l'administration : l'écran montre où en est chaque demande, et
/// ce qu'elle a répondu. Une demande en attente peut être retirée, pas une demande traitée.
class CongesScreen extends StatefulWidget {
  const CongesScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<CongesScreen> createState() => _CongesScreenState();
}

class _CongesScreenState extends State<CongesScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<DemandeConge> _demandes = const [];
  bool _chargement = true;
  String? _erreur;
  String? _erreurAction;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  Future<void> _charger() async {
    setState(() {
      _chargement = _demandes.isEmpty;
      _erreur = null;
    });
    try {
      final demandes = await _service.mesConges();
      if (!mounted) return;
      setState(() {
        _demandes = demandes;
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

  Future<void> _nouvelle() async {
    final envoyee = await ouvrirFormulaire<bool>(context, (_) => _FormulaireConge(service: _service));
    if (envoyee == true && mounted) {
      setState(() => _erreurAction = null);
      _charger();
    }
  }

  Future<void> _retirer(DemandeConge d) async {
    final confirme = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Retirer cette demande', style: Typo.titreSection),
        content: Text('Votre demande du ${Dates.courte(d.debut)} au ${Dates.courte(d.fin)} '
            'ne sera plus examinée.', style: Typo.corpsAttenue),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Rayons.carte)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Garder')),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Couleurs.danger),
            child: const Text('Retirer'),
          ),
        ],
      ),
    );
    if (confirme != true) return;
    try {
      await _service.annulerConge(d.id);
      if (mounted) _charger();
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreurAction = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      floatingActionButton: _chargement || _erreur != null
          ? null
          : FloatingActionButton.extended(
              onPressed: _nouvelle,
              backgroundColor: Couleurs.indigo600,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.add_rounded, size: 20),
              label: const Text('Nouvelle demande'),
            ),
      body: Column(
        children: [
          EnteteDegrade(
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
                      Text('Congés et absences',
                          style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19)),
                      const SizedBox(height: 2),
                      Text(
                        _demandes.where((d) => d.statut == StatutDemande.enAttente).isEmpty
                            ? 'Aucune demande en attente'
                            : '${_demandes.where((d) => d.statut == StatutDemande.enAttente).length} en attente',
                        style: Typo.legende.copyWith(
                            color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: Transform.translate(
              offset: const Offset(0, -24),
              child: RefreshIndicator(
                onRefresh: _charger,
                color: Couleurs.indigo600,
                child: TransitionContenu(enfant: _corps()),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _corps() {
    if (_chargement) {
      return const Center(key: ValueKey('c'), child: CircularProgressIndicator(color: Couleurs.indigo600));
    }
    if (_erreur != null && _demandes.isEmpty) {
      return ListView(
        key: const ValueKey('e'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Demandes indisponibles',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }
    if (_demandes.isEmpty) {
      return ListView(
        key: const ValueKey('v'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: Espaces.xxl),
          EtatVide(
            icone: Icons.beach_access_outlined,
            titre: 'Aucune demande',
            detail: 'Un congé, une absence ou une formation : déposez votre demande, '
                'l’administration vous répond ici.',
          ),
        ],
      );
    }

    return ListView(
      key: const ValueKey('l'),
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(
          Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      children: [
        if (_erreurAction != null) ...[
          Bandeau.erreur(
              message: _erreurAction!, surFermeture: () => setState(() => _erreurAction = null)),
          const SizedBox(height: Espaces.md),
        ],
        ..._demandes.asMap().entries.map((e) => ApparitionEnCascade(
              rang: e.key,
              enfant: Padding(
                padding: const EdgeInsets.only(bottom: Espaces.md),
                child: _carte(e.value),
              ),
            )),
      ],
    );
  }

  Widget _carte(DemandeConge d) {
    final periode = d.debut == d.fin
        ? Dates.jourEtDate(d.debut)
        : '${Dates.courte(d.debut)} → ${Dates.courte(d.fin)}';

    return Carte(
      padding: const EdgeInsets.all(Espaces.lg),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: d.statut.fond,
                  borderRadius: BorderRadius.circular(Rayons.md),
                ),
                child: Icon(d.type.icone, size: 20, color: d.statut.teinte),
              ),
              const SizedBox(width: Espaces.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('${d.type.libelle} · ${d.jours} jour${d.jours > 1 ? 's' : ''}',
                        style: Typo.libelle.copyWith(fontSize: 14)),
                    const SizedBox(height: 2),
                    Text(periode, style: Typo.legendePale),
                  ],
                ),
              ),
              Pastille(texte: d.statut.libelle, teinte: d.statut.teinte, fond: d.statut.fond, compact: true),
            ],
          ),
          const SizedBox(height: Espaces.md),
          Text(d.motif, style: Typo.corpsAttenue.copyWith(fontSize: 13.5)),
          if (d.seancesConcernees > 0) ...[
            const SizedBox(height: Espaces.sm),
            Row(
              children: [
                const Icon(Icons.event_note_rounded, size: 15, color: Couleurs.encreDiscrete),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    '${d.seancesConcernees} séance${d.seancesConcernees > 1 ? 's' : ''} sur cette période',
                    style: Typo.legendePale,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ],
          if (d.reponse != null) ...[
            const SizedBox(height: Espaces.md),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(Espaces.md),
              decoration: BoxDecoration(
                color: d.statut.fond,
                borderRadius: BorderRadius.circular(Rayons.md),
              ),
              child: Text('Administration : ${d.reponse}',
                  style: Typo.legende.copyWith(fontSize: 12.5, color: d.statut.teinte)),
            ),
          ],
          if (d.statut == StatutDemande.enAttente) ...[
            const SizedBox(height: Espaces.sm),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: () => _retirer(d),
                style: TextButton.styleFrom(foregroundColor: Couleurs.encreAttenuee),
                icon: const Icon(Icons.undo_rounded, size: 16),
                label: const Text('Retirer'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _FormulaireConge extends StatefulWidget {
  const _FormulaireConge({required this.service});

  final EnseignantService service;

  @override
  State<_FormulaireConge> createState() => _FormulaireCongeState();
}

class _FormulaireCongeState extends State<_FormulaireConge> {
  final _motif = TextEditingController();
  TypeConge _type = TypeConge.conge;
  DateTime _debut = DateTime.now();
  DateTime _fin = DateTime.now();
  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() {
    _motif.dispose();
    super.dispose();
  }

  bool get _valide => _motif.text.trim().isNotEmpty && !_fin.isBefore(_debut);

  Future<void> _choisir({required bool debut}) async {
    final choisie = await showDatePicker(
      context: context,
      initialDate: debut ? _debut : _fin,
      firstDate: DateTime.now().subtract(const Duration(days: 60)),
      lastDate: DateTime.now().add(const Duration(days: 400)),
      locale: const Locale('fr'),
    );
    if (choisie == null) return;
    setState(() {
      if (debut) {
        _debut = choisie;
        if (_fin.isBefore(_debut)) _fin = _debut;
      } else {
        _fin = choisie;
      }
    });
  }

  Future<void> _envoyer() async {
    if (!_valide || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      await widget.service.demanderConge(type: _type, debut: _debut, fin: _fin, motif: _motif.text);
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ErreurApi catch (e) {
      // Le serveur refuse notamment une période qui en recouvre une autre.
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _envoi = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return CadreFormulaire(
      titre: 'Nouvelle demande',
      sousTitre: 'L’administration vous répondra ici, et vous serez notifié.',
      enfants: [
        if (_erreur != null) ...[
          Bandeau.erreur(message: _erreur!),
          const SizedBox(height: Espaces.md),
        ],
        const EtiquetteChamp('Nature'),
        Wrap(
          spacing: Espaces.sm - 2,
          runSpacing: Espaces.sm - 2,
          children: TypeConge.values.map(_puce).toList(),
        ),
        const SizedBox(height: Espaces.lg),
        Row(
          children: [
            Expanded(child: _date('Du', _debut, () => _choisir(debut: true))),
            const SizedBox(width: Espaces.md),
            Expanded(child: _date('Au', _fin, () => _choisir(debut: false))),
          ],
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Motif'),
        TextField(
          controller: _motif,
          maxLines: 3,
          maxLength: 500,
          textCapitalization: TextCapitalization.sentences,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(hintText: 'Congé annuel, formation à Bamako…'),
        ),
        const SizedBox(height: Espaces.sm),
        ActionsFormulaire(
          libelle: 'Envoyer la demande',
          envoi: _envoi,
          surValidation: _valide ? _envoyer : null,
        ),
      ],
    );
  }

  Widget _date(String etiquette, DateTime valeur, VoidCallback surTap) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        EtiquetteChamp(etiquette),
        GestureDetector(
          onTap: _envoi ? null : surTap,
          child: Container(
            height: 48,
            padding: const EdgeInsets.symmetric(horizontal: Espaces.lg),
            decoration: BoxDecoration(
              border: Border.all(color: Couleurs.trait),
              borderRadius: BorderRadius.circular(Rayons.tuile),
            ),
            child: Row(
              children: [
                const Icon(Icons.calendar_today_rounded, size: 16, color: Couleurs.encreDiscrete),
                const SizedBox(width: Espaces.sm),
                Text(Dates.courte(valeur), style: Typo.corps),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _puce(TypeConge type) {
    final actif = _type == type;
    return GestureDetector(
      onTap: () => setState(() => _type = type),
      child: AnimatedContainer(
        duration: Durees.rapide,
        padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 7),
        decoration: BoxDecoration(
          color: actif ? Couleurs.indigo600 : Couleurs.carte,
          border: Border.all(color: actif ? Couleurs.indigo600 : Couleurs.trait),
          borderRadius: BorderRadius.circular(Rayons.pilule),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(type.icone, size: 13, color: actif ? Colors.white : Couleurs.encreDiscrete),
            const SizedBox(width: 5),
            Text(type.libelle,
                style: Typo.legende.copyWith(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w500,
                  color: actif ? Colors.white : Couleurs.encreAttenuee,
                )),
          ],
        ),
      ),
    );
  }
}
