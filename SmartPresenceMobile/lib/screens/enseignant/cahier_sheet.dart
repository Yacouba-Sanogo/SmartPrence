import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/enseignement.dart';
import '../../models/seance.dart';
import '../../services/enseignant_service.dart';
import '../../widgets/communs.dart';
import '../../widgets/formulaire.dart';

/// Ouvre la page du cahier de textes d'une séance et renvoie `true` si elle a été
/// enregistrée.
Future<bool> ouvrirCahier(
  BuildContext context, {
  required EnseignantService service,
  required Seance seance,
}) async {
  final enregistre = await ouvrirFormulaire<bool>(
    context,
    (_) => _FormulaireCahier(service: service, seance: seance),
  );
  return enregistre ?? false;
}

class _FormulaireCahier extends StatefulWidget {
  const _FormulaireCahier({required this.service, required this.seance});

  final EnseignantService service;
  final Seance seance;

  @override
  State<_FormulaireCahier> createState() => _FormulaireCahierState();
}

class _FormulaireCahierState extends State<_FormulaireCahier> {
  final _contenu = TextEditingController();
  final _travail = TextEditingController();

  List<Chapitre> _chapitres = const [];
  String? _chapitreId;
  bool _chapitreTermine = false;

  bool _chargement = true;
  bool _envoi = false;
  String? _erreur;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  @override
  void dispose() {
    _contenu.dispose();
    _travail.dispose();
    super.dispose();
  }

  Future<void> _charger() async {
    try {
      final page = await widget.service.cahierDeSeance(widget.seance.id);
      final classeId = widget.seance.classeId;
      final matiereId = widget.seance.matiereId;
      var chapitres = <Chapitre>[];
      if (classeId != null && matiereId != null) {
        final syllabus = await widget.service.syllabus(classeId, matiereId: matiereId);
        chapitres = syllabus.expand((s) => s.chapitres).toList();
      }
      if (!mounted) return;
      setState(() {
        _chapitres = chapitres;
        if (page != null) {
          _contenu.text = page.contenu;
          _travail.text = page.travailAFaire ?? '';
          _chapitreId = page.chapitreId;
        }
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

  bool get _valide => _contenu.text.trim().isNotEmpty;

  Future<void> _enregistrer() async {
    if (!_valide || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      await widget.service.ecrireCahier(
        widget.seance.id,
        contenu: _contenu.text,
        travailAFaire: _travail.text,
        chapitreId: _chapitreId,
        chapitreTermine: _chapitreTermine,
      );
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ErreurApi catch (e) {
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
      titre: 'Cahier de textes',
      sousTitre: '${widget.seance.matiere} · ${widget.seance.classeCode}',
      enfants: [
        if (_chargement)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: Espaces.xxl),
            child: Center(child: CircularProgressIndicator(color: Couleurs.indigo600)),
          )
        else ...[
          if (_erreur != null) ...[
            Bandeau.erreur(message: _erreur!),
            const SizedBox(height: Espaces.md),
          ],
          const EtiquetteChamp('Contenu enseigné'),
          TextField(
            controller: _contenu,
            maxLines: 4,
            maxLength: 2000,
            textCapitalization: TextCapitalization.sentences,
            onChanged: (_) => setState(() {}),
            decoration: const InputDecoration(
                hintText: 'Notions abordées, exemples, exercices corrigés…'),
          ),
          const SizedBox(height: Espaces.sm),
          const EtiquetteChamp('Travail à faire', facultatif: true),
          TextField(
            controller: _travail,
            maxLines: 2,
            maxLength: 500,
            textCapitalization: TextCapitalization.sentences,
            decoration: const InputDecoration(hintText: 'Exercices 1 à 3 pour la prochaine séance'),
          ),
          if (_chapitres.isNotEmpty) ...[
            const SizedBox(height: Espaces.sm),
            const EtiquetteChamp('Chapitre', facultatif: true),
            DropdownButtonFormField<String?>(
              initialValue: _chapitreId,
              isExpanded: true,
              onChanged: _envoi
                  ? null
                  : (id) => setState(() {
                        _chapitreId = id;
                        _chapitreTermine = false;
                      }),
              items: [
                const DropdownMenuItem<String?>(value: null, child: Text('Aucun chapitre')),
                ..._chapitres.map((c) => DropdownMenuItem<String?>(
                      value: c.id,
                      child: Text('${c.ordre}. ${c.titre}',
                          style: Typo.corps, maxLines: 1, overflow: TextOverflow.ellipsis),
                    )),
              ],
            ),
            if (_chapitreId != null)
              CheckboxListTile(
                value: _chapitreTermine,
                onChanged: _envoi ? null : (v) => setState(() => _chapitreTermine = v ?? false),
                contentPadding: EdgeInsets.zero,
                controlAffinity: ListTileControlAffinity.leading,
                activeColor: Couleurs.indigo600,
                title: Text('Ce chapitre est terminé', style: Typo.corps),
              ),
          ],
          const SizedBox(height: Espaces.lg),
          ActionsFormulaire(
            libelle: 'Enregistrer',
            envoi: _envoi,
            surValidation: _valide ? _enregistrer : null,
          ),
        ],
      ],
    );
  }
}
