import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../models/gestion.dart';
import '../../models/note.dart';
import '../../models/seance.dart';
import '../../services/enseignant_service.dart';
import '../../services/fichiers.dart';
import '../../widgets/carte_support.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';
import '../../widgets/formulaire.dart';

/// Supports de cours qu'un enseignant publie pour une classe.
///
/// Publier prévient les étudiants de la classe : l'enseignant n'a rien d'autre à faire.
class SupportsScreen extends StatefulWidget {
  const SupportsScreen({super.key, required this.auth, required this.classe});

  final AuthService auth;
  final ClasseEnseignee classe;

  @override
  State<SupportsScreen> createState() => _SupportsScreenState();
}

class _SupportsScreenState extends State<SupportsScreen> {
  late final EnseignantService _service = EnseignantService(widget.auth.client);

  List<SupportCours> _supports = const [];
  List<MatiereBreve> _matieres = const [];
  String? _ouvert;
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
      _chargement = _supports.isEmpty;
      _erreur = null;
    });
    try {
      final r = await Future.wait([_service.supports(widget.classe.id), _service.matieres()]);
      if (!mounted) return;
      setState(() {
        _supports = r[0] as List<SupportCours>;
        _matieres = r[1] as List<MatiereBreve>;
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

  Future<void> _publier() async {
    if (_matieres.isEmpty) {
      setState(() => _erreurAction = 'Aucune matière active. La scolarité doit en créer avant toute publication.');
      return;
    }
    final publie = await ouvrirFormulaire<bool>(
      context,
      (_) => _FormulairePublication(service: _service, classeId: widget.classe.id, matieres: _matieres),
    );
    if (publie == true && mounted) {
      setState(() => _erreurAction = null);
      _charger();
    }
  }

  Future<void> _ouvrirFichier(SupportCours s) async {
    setState(() {
      _ouvert = s.id;
      _erreurAction = null;
    });
    try {
      await ouvrirSupport(widget.auth.client, s);
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreurAction = e.message);
    } finally {
      if (mounted) setState(() => _ouvert = null);
    }
  }

  Future<void> _retirer(SupportCours s) async {
    final confirme = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Retirer ce support', style: Typo.titreSection),
        content: Text('« ${s.titre} » ne sera plus accessible aux étudiants.', style: Typo.corpsAttenue),
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
      await _service.supprimerSupport(s.id);
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
              onPressed: _publier,
              backgroundColor: Couleurs.indigo600,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.upload_file_rounded, size: 20),
              label: const Text('Publier'),
            ),
      body: Column(
        children: [
          EnteteDegrade(
            debordement: 30,
            padding: const EdgeInsets.fromLTRB(Espaces.sm, Espaces.sm, Espaces.lg, Espaces.md),
            enfant: Row(
              children: [
                ActionEntete(icone: Icons.arrow_back_rounded, surTap: () => Navigator.of(context).maybePop()),
                const SizedBox(width: Espaces.xs),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text('Supports de cours',
                          style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19)),
                      const SizedBox(height: 2),
                      Text(
                          '${widget.classe.code} · ${_supports.length} document${_supports.length > 1 ? 's' : ''}',
                          style: Typo.legende.copyWith(
                              color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5)),
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
    if (_erreur != null && _supports.isEmpty) {
      return ListView(
        key: const ValueKey('e'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Supports indisponibles',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }
    if (_supports.isEmpty) {
      return ListView(
        key: const ValueKey('v'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          if (_erreurAction != null)
            Padding(
              padding: const EdgeInsets.all(Espaces.lg),
              child: Bandeau.erreur(message: _erreurAction!),
            ),
          const SizedBox(height: Espaces.xxl),
          const EtatVide(
            icone: Icons.folder_open_rounded,
            titre: 'Aucun support publié',
            detail: 'Déposez un PDF, une présentation ou un énoncé : les étudiants de la classe '
                'sont prévenus et le téléchargent depuis leur téléphone.',
          ),
        ],
      );
    }

    return ListView(
      key: const ValueKey('l'),
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(Espaces.md + 2, Espaces.md, Espaces.md + 2, Espaces.xxxl * 3),
      children: [
        if (_erreurAction != null) ...[
          Bandeau.erreur(message: _erreurAction!, surFermeture: () => setState(() => _erreurAction = null)),
          const SizedBox(height: Espaces.md),
        ],
        ..._supports.asMap().entries.map((e) => ApparitionEnCascade(
              rang: e.key,
              enfant: Padding(
                padding: const EdgeInsets.only(bottom: Espaces.sm + 2),
                child: CarteSupport(
                  support: e.value,
                  enCours: _ouvert == e.value.id,
                  surTap: () => _ouvrirFichier(e.value),
                  action: IconButton(
                    onPressed: () => _retirer(e.value),
                    icon: const Icon(Icons.delete_outline_rounded, size: 20),
                    color: Couleurs.encreDiscrete,
                    tooltip: 'Retirer',
                  ),
                ),
              ),
            )),
      ],
    );
  }
}

class _FormulairePublication extends StatefulWidget {
  const _FormulairePublication({required this.service, required this.classeId, required this.matieres});

  final EnseignantService service;
  final int classeId;
  final List<MatiereBreve> matieres;

  @override
  State<_FormulairePublication> createState() => _FormulairePublicationState();
}

class _FormulairePublicationState extends State<_FormulairePublication> {
  static const _taillemax = 10 * 1024 * 1024;
  static const _extensions = [
    'pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'odt', 'odp', 'ods', 'txt', 'csv', 'png', 'jpg',
    'jpeg', 'zip',
  ];

  final _titre = TextEditingController();
  final _description = TextEditingController();
  late int? _matiereId = widget.matieres.isEmpty ? null : widget.matieres.first.id;
  PlatformFile? _fichier;
  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() {
    _titre.dispose();
    _description.dispose();
    super.dispose();
  }

  bool get _valide => _fichier != null && _matiereId != null && _titre.text.trim().isNotEmpty;

  Future<void> _choisir() async {
    final r = await FilePicker.platform.pickFiles(type: FileType.custom, allowedExtensions: _extensions, withData: true);
    final f = r?.files.single;
    if (f == null) return;
    if (f.size > _taillemax) {
      setState(() => _erreur = 'Ce fichier dépasse 10 Mo. Réduisez-le avant de le publier.');
      return;
    }
    setState(() {
      _fichier = f;
      _erreur = null;
      if (_titre.text.trim().isEmpty) {
        // Le nom du fichier sans son extension : un point de départ, pas une obligation.
        final i = f.name.lastIndexOf('.');
        _titre.text = i > 0 ? f.name.substring(0, i) : f.name;
      }
    });
  }

  Future<void> _envoyer() async {
    final f = _fichier;
    if (!_valide || f == null || f.bytes == null || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      await widget.service.publierSupport(
        classeId: widget.classeId,
        matiereId: _matiereId!,
        titre: _titre.text,
        description: _description.text,
        nomFichier: f.name,
        octets: f.bytes!,
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
    final f = _fichier;
    return CadreFormulaire(
      titre: 'Publier un support',
      sousTitre: 'PDF, présentation, document ou tableur jusqu’à 10 Mo.',
      enfants: [
        if (_erreur != null) ...[
          Bandeau.erreur(message: _erreur!),
          const SizedBox(height: Espaces.md),
        ],
        GestureDetector(
          onTap: _envoi ? null : _choisir,
          child: AnimatedContainer(
            duration: Durees.moyenne,
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: Espaces.xl, horizontal: Espaces.lg),
            decoration: BoxDecoration(
              color: f == null ? Couleurs.royal50 : Couleurs.succesFond,
              borderRadius: BorderRadius.circular(Rayons.tuile),
              border: Border.all(color: f == null ? Couleurs.royal200 : Couleurs.succesTrait),
            ),
            child: Column(
              children: [
                Icon(f == null ? Icons.cloud_upload_outlined : Icons.check_circle_rounded,
                    size: 30, color: f == null ? Couleurs.indigo600 : Couleurs.succesVif),
                const SizedBox(height: Espaces.sm),
                Text(f == null ? 'Choisir un fichier' : f.name,
                    style: Typo.libelle.copyWith(fontSize: 13.5),
                    textAlign: TextAlign.center,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis),
                if (f != null)
                  Text('${(f.size / 1024).round()} Ko · toucher pour changer', style: Typo.legendePale),
              ],
            ),
          ),
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Matière'),
        DropdownButtonFormField<int>(
          initialValue: _matiereId,
          isExpanded: true,
          onChanged: _envoi ? null : (id) => setState(() => _matiereId = id),
          items: widget.matieres
              .map((m) => DropdownMenuItem(
                    value: m.id,
                    child: Text('${m.code} — ${m.libelle}',
                        style: Typo.corps, maxLines: 1, overflow: TextOverflow.ellipsis),
                  ))
              .toList(),
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Titre'),
        TextField(
          controller: _titre,
          maxLength: 160,
          textCapitalization: TextCapitalization.sentences,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(hintText: 'Chapitre 1 — Introduction'),
        ),
        const EtiquetteChamp('Description', facultatif: true),
        TextField(
          controller: _description,
          maxLines: 2,
          maxLength: 500,
          textCapitalization: TextCapitalization.sentences,
          decoration: const InputDecoration(hintText: 'À lire avant la prochaine séance'),
        ),
        const SizedBox(height: Espaces.sm),
        ActionsFormulaire(
          libelle: 'Publier',
          envoi: _envoi,
          surValidation: _valide ? _envoyer : null,
        ),
      ],
    );
  }
}
