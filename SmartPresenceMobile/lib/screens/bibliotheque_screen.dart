import 'package:flutter/material.dart';

import '../core/api/erreurs.dart';
import '../core/auth/auth_service.dart';
import '../core/dates.dart';
import '../core/design/animations.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../models/bibliotheque.dart';
import '../services/bibliotheque_service.dart';
import '../services/fichier/choix_fichier.dart';
import '../widgets/communs.dart';
import '../widgets/entete.dart';
import '../widgets/formulaire.dart';

enum _Onglet { documents, livres, emprunts, depots }

/// Bibliothèque : mémoires et anciens sujets, livres du fonds, emprunts et dépôts de l'étudiant.
///
/// Les listes sont chargées en entier puis filtrées sur l'appareil : la recherche fonctionne
/// donc aussi sans réseau, à partir de la dernière version connue du catalogue.
class BibliothequeScreen extends StatefulWidget {
  const BibliothequeScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<BibliothequeScreen> createState() => _BibliothequeScreenState();
}

class _BibliothequeScreenState extends State<BibliothequeScreen> {
  late final BibliothequeService _service = BibliothequeService(widget.auth.client);
  late final bool _etudiant = widget.auth.profil?.estEtudiant == true;

  final _recherche = TextEditingController();
  _Onglet _onglet = _Onglet.documents;
  String? _type;
  String? _filiere;

  List<DocumentBiblio> _documents = const [];
  List<LivreBiblio> _livres = const [];
  List<EmpruntBiblio> _emprunts = const [];
  List<DocumentBiblio> _depots = const [];
  bool _chargement = true;
  String? _erreur;
  String? _erreurAction;
  String? _ouvert;

  List<_Onglet> get _onglets =>
      [_Onglet.documents, _Onglet.livres, if (_etudiant) ...[_Onglet.emprunts, _Onglet.depots]];

  @override
  void initState() {
    super.initState();
    _charger();
  }

  @override
  void dispose() {
    _recherche.dispose();
    super.dispose();
  }

  Future<void> _charger() async {
    setState(() {
      _chargement = _documents.isEmpty && _livres.isEmpty;
      _erreur = null;
    });
    try {
      final r = await Future.wait([
        _service.documents(),
        _service.livres(),
        if (_etudiant) _service.mesEmprunts(),
        if (_etudiant) _service.mesDepots(),
      ]);
      if (!mounted) return;
      setState(() {
        _documents = r[0] as List<DocumentBiblio>;
        _livres = r[1] as List<LivreBiblio>;
        if (_etudiant) {
          _emprunts = r[2] as List<EmpruntBiblio>;
          _depots = r[3] as List<DocumentBiblio>;
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

  String _normaliser(String s) => s
      .toLowerCase()
      .replaceAll(RegExp('[àâä]'), 'a')
      .replaceAll(RegExp('[éèêë]'), 'e')
      .replaceAll(RegExp('[îï]'), 'i')
      .replaceAll(RegExp('[ôö]'), 'o')
      .replaceAll(RegExp('[ùûü]'), 'u')
      .replaceAll('ç', 'c');

  bool _correspond(String texte) {
    final mot = _normaliser(_recherche.text.trim());
    return mot.isEmpty || _normaliser(texte).contains(mot);
  }

  List<DocumentBiblio> get _documentsFiltres => _documents.where((d) {
        if (_type == 'MEMOIRE' && !d.estMemoire) return false;
        if (_type == 'SUJET' && d.estMemoire) return false;
        if (_filiere != null && d.filiere != _filiere) return false;
        return _correspond('${d.titre} ${d.auteur ?? ''} ${d.matiere ?? ''} ${d.description ?? ''}');
      }).toList();

  List<LivreBiblio> get _livresFiltres =>
      _livres.where((l) => _correspond('${l.titre} ${l.auteur} ${l.categorie ?? ''}')).toList();

  Future<void> _ouvrir(DocumentBiblio d) async {
    setState(() {
      _ouvert = d.id;
      _erreurAction = null;
    });
    try {
      await _service.ouvrir(d);
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreurAction = e.message);
    } finally {
      if (mounted) setState(() => _ouvert = null);
    }
  }

  Future<void> _deposer() async {
    final filieres = await _service.filieres().catchError((_) => <FiliereBiblio>[]);
    if (!mounted) return;
    final envoye = await ouvrirFormulaire<bool>(
      context,
      (_) => _FormulaireDepot(service: _service, filieres: filieres),
    );
    if (envoye == true) {
      await _charger();
      if (mounted) setState(() => _onglet = _Onglet.depots);
    }
  }

  Future<void> _retirer(DocumentBiblio d) async {
    try {
      await _service.retirerDepot(d.id);
      if (mounted) setState(() => _depots = _depots.where((x) => x.id != d.id).toList());
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreurAction = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
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
                      Text('Bibliothèque', style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 19)),
                      const SizedBox(height: 2),
                      Text('${_documents.length} documents · ${_livres.length} livres',
                          style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.72), fontSize: 11.5)),
                    ],
                  ),
                ),
                if (_etudiant) ActionEntete(icone: Icons.upload_file_rounded, surTap: _deposer),
              ],
            ),
          ),
          Expanded(
            child: Transform.translate(
              offset: const Offset(0, -24),
              child: Column(
                children: [
                  _selecteur(),
                  Expanded(
                    child: RefreshIndicator(
                      onRefresh: _charger,
                      color: Couleurs.indigo600,
                      child: TransitionContenu(enfant: _corps()),
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

  static const _libelles = {
    _Onglet.documents: 'Documents',
    _Onglet.livres: 'Livres',
    _Onglet.emprunts: 'Emprunts',
    _Onglet.depots: 'Dépôts',
  };

  Widget _selecteur() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(Espaces.md + 2, 0, Espaces.md + 2, Espaces.xs),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            for (final o in _onglets)
              Padding(
                padding: const EdgeInsets.only(right: Espaces.sm),
                child: ChoiceChip(
                  label: Text(_libelles[o]!),
                  selected: _onglet == o,
                  onSelected: (_) => setState(() {
                    _onglet = o;
                    _recherche.clear();
                  }),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _barreRecherche({bool filtres = false}) {
    final filieres = {for (final d in _documents) if (d.filiere != null) d.filiere!}.toList()..sort();
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm),
      child: Column(
        children: [
          TextField(
            controller: _recherche,
            onChanged: (_) => setState(() {}),
            decoration: const InputDecoration(
              hintText: 'Rechercher…',
              prefixIcon: Icon(Icons.search_rounded),
            ),
          ),
          if (filtres) ...[
            const SizedBox(height: Espaces.sm),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  for (final (valeur, texte) in [(null, 'Tout'), ('MEMOIRE', 'Mémoires'), ('SUJET', 'Sujets')])
                    Padding(
                      padding: const EdgeInsets.only(right: Espaces.xs + 2),
                      child: FilterChip(
                        label: Text(texte),
                        selected: _type == valeur,
                        onSelected: (_) => setState(() => _type = valeur),
                      ),
                    ),
                  for (final f in filieres)
                    Padding(
                      padding: const EdgeInsets.only(right: Espaces.xs + 2),
                      child: FilterChip(
                        label: Text(f),
                        selected: _filiere == f,
                        onSelected: (s) => setState(() => _filiere = s ? f : null),
                      ),
                    ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _corps() {
    if (_chargement) {
      return const Center(key: ValueKey('c'), child: CircularProgressIndicator(color: Couleurs.indigo600));
    }
    if (_erreur != null && _documents.isEmpty && _livres.isEmpty) {
      return ListView(
        key: const ValueKey('e'),
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: Espaces.xxl),
          EtatVide(
            enErreur: true,
            icone: Icons.cloud_off_rounded,
            titre: 'Bibliothèque indisponible',
            detail: _erreur,
            libelleAction: 'Réessayer',
            surAction: _charger,
          ),
        ],
      );
    }
    final contenu = switch (_onglet) {
      _Onglet.documents => _listeDocuments(),
      _Onglet.livres => _listeLivres(),
      _Onglet.emprunts => _listeEmprunts(),
      _Onglet.depots => _listeDepots(),
    };
    return ListView(
      key: ValueKey(_onglet),
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(Espaces.md + 2, Espaces.sm, Espaces.md + 2, Espaces.xxxl * 3),
      children: [
        if (_erreurAction != null) ...[
          Bandeau.erreur(message: _erreurAction!, surFermeture: () => setState(() => _erreurAction = null)),
          const SizedBox(height: Espaces.md),
        ],
        ...contenu,
      ],
    );
  }

  Widget _vide(IconData icone, String titre, String detail) => Padding(
        padding: const EdgeInsets.only(top: Espaces.xl),
        child: EtatVide(icone: icone, titre: titre, detail: detail),
      );

  List<Widget> _listeDocuments() {
    final liste = _documentsFiltres;
    return [
      _barreRecherche(filtres: true),
      if (liste.isEmpty)
        _vide(Icons.menu_book_outlined, 'Aucun document', 'Aucun mémoire ni sujet ne correspond à votre recherche.')
      else
        for (var i = 0; i < liste.length; i++)
          ApparitionEnCascade(
            rang: i,
            enfant: Padding(
              padding: const EdgeInsets.only(bottom: Espaces.sm + 2),
              child: _CarteDocument(doc: liste[i], enCours: _ouvert == liste[i].id, surTap: () => _ouvrir(liste[i])),
            ),
          ),
    ];
  }

  List<Widget> _listeLivres() {
    final liste = _livresFiltres;
    return [
      _barreRecherche(),
      if (liste.isEmpty)
        _vide(Icons.auto_stories_outlined, 'Aucun livre', 'Aucun livre du fonds ne correspond à votre recherche.')
      else
        for (var i = 0; i < liste.length; i++)
          ApparitionEnCascade(
            rang: i,
            enfant: Padding(padding: const EdgeInsets.only(bottom: Espaces.sm + 2), child: _CarteLivre(livre: liste[i])),
          ),
    ];
  }

  List<Widget> _listeEmprunts() {
    if (_emprunts.isEmpty) {
      return [_vide(Icons.bookmark_border_rounded, 'Aucun emprunt', 'Les livres que vous empruntez à la bibliothèque apparaissent ici avec leur date de retour.')];
    }
    return [
      for (var i = 0; i < _emprunts.length; i++)
        ApparitionEnCascade(
          rang: i,
          enfant: Padding(padding: const EdgeInsets.only(bottom: Espaces.sm + 2), child: _CarteEmprunt(emprunt: _emprunts[i])),
        ),
    ];
  }

  List<Widget> _listeDepots() {
    if (_depots.isEmpty) {
      return [_vide(Icons.upload_file_outlined, 'Aucun dépôt', 'Déposez un mémoire ou un ancien sujet avec le bouton en haut à droite. L’administration le publie après vérification.')];
    }
    return [
      for (var i = 0; i < _depots.length; i++)
        ApparitionEnCascade(
          rang: i,
          enfant: Padding(
            padding: const EdgeInsets.only(bottom: Espaces.sm + 2),
            child: _CarteDocument(
              doc: _depots[i],
              afficherStatut: true,
              enCours: _ouvert == _depots[i].id,
              surTap: () => _ouvrir(_depots[i]),
              action: _depots[i].statut == 'PUBLIE'
                  ? null
                  : IconButton(
                      tooltip: 'Retirer',
                      onPressed: () => _retirer(_depots[i]),
                      icon: const Icon(Icons.delete_outline_rounded, color: Couleurs.dangerVif),
                    ),
            ),
          ),
        ),
    ];
  }
}

class _CarteDocument extends StatelessWidget {
  const _CarteDocument({required this.doc, required this.surTap, this.enCours = false, this.afficherStatut = false, this.action});

  final DocumentBiblio doc;
  final VoidCallback surTap;
  final bool enCours;
  final bool afficherStatut;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final teinte = doc.estMemoire ? Couleurs.indigo600 : Couleurs.alerteVif;
    final (texteStatut, couleurStatut) = switch (doc.statut) {
      'PUBLIE' => ('Publié', Couleurs.succesVif),
      'REFUSE' => ('Refusé', Couleurs.dangerVif),
      _ => ('En attente de validation', Couleurs.alerteVif),
    };
    return Carte(
      surTap: enCours ? null : surTap,
      padding: const EdgeInsets.fromLTRB(Espaces.md + 2, Espaces.md + 2, Espaces.sm, Espaces.md + 2),
      enfant: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(color: teinte.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(Rayons.md)),
            child: enCours
                ? Padding(padding: const EdgeInsets.all(13), child: CircularProgressIndicator(strokeWidth: 2.2, color: teinte))
                : Icon(doc.estMemoire ? Icons.school_rounded : Icons.quiz_rounded, size: 24, color: teinte),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(doc.titre, style: Typo.libelle, maxLines: 2, overflow: TextOverflow.ellipsis),
                const SizedBox(height: 2),
                Text(
                  [
                    doc.estMemoire ? 'Mémoire' : 'Sujet',
                    if (doc.sousTitre.isNotEmpty) doc.sousTitre,
                    if (doc.filiere != null) doc.filiere!,
                    if (doc.annee != null) '${doc.annee}',
                  ].join(' · '),
                  style: Typo.legendePale,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                if (afficherStatut) ...[
                  const SizedBox(height: 4),
                  Text(texteStatut, style: Typo.legende.copyWith(color: couleurStatut, fontWeight: FontWeight.w600)),
                  if (doc.statut == 'REFUSE' && doc.motifRefus != null)
                    Text('Motif : ${doc.motifRefus}', style: Typo.legendePale),
                ] else
                  Text(doc.tailleLisible, style: Typo.legendePale),
              ],
            ),
          ),
          if (action != null) action!,
        ],
      ),
    );
  }
}

class _CarteLivre extends StatelessWidget {
  const _CarteLivre({required this.livre});

  final LivreBiblio livre;

  @override
  Widget build(BuildContext context) {
    final dispo = livre.disponibles > 0;
    return Carte(
      padding: const EdgeInsets.all(Espaces.md + 2),
      enfant: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(color: Couleurs.royal50, borderRadius: BorderRadius.circular(Rayons.md)),
            child: const Icon(Icons.menu_book_rounded, size: 24, color: Couleurs.indigo600),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(livre.titre, style: Typo.libelle, maxLines: 2, overflow: TextOverflow.ellipsis),
                Text(
                  [livre.auteur, if (livre.categorie != null) livre.categorie!].join(' · '),
                  style: Typo.legendePale,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                if (livre.emplacement != null) Text('Rayon : ${livre.emplacement}', style: Typo.legendePale),
              ],
            ),
          ),
          const SizedBox(width: Espaces.sm),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(
              color: dispo ? Couleurs.succesFond : Couleurs.dangerFond,
              borderRadius: BorderRadius.circular(Rayons.pilule),
            ),
            child: Text(
              dispo ? '${livre.disponibles} dispo' : 'Sorti',
              style: Typo.legende.copyWith(
                  color: dispo ? Couleurs.succes : Couleurs.danger, fontWeight: FontWeight.w600, fontSize: 11.5),
            ),
          ),
        ],
      ),
    );
  }
}

class _CarteEmprunt extends StatelessWidget {
  const _CarteEmprunt({required this.emprunt});

  final EmpruntBiblio emprunt;

  @override
  Widget build(BuildContext context) {
    final e = emprunt;
    final reste = e.joursRestants(DateTime.now());
    final (texte, fond, couleur) = e.rendu
        ? ('Rendu le ${Dates.courte(e.retourneLe!)}', Couleurs.royal50, Couleurs.encreAttenuee)
        : e.enRetard
            ? ('En retard de ${e.joursDeRetard} jour${e.joursDeRetard > 1 ? 's' : ''}', Couleurs.dangerFond, Couleurs.danger)
            : reste <= 2
                ? (reste <= 0 ? 'À rendre aujourd’hui' : 'À rendre dans $reste jour${reste > 1 ? 's' : ''}', Couleurs.alerteFond, Couleurs.alerte)
                : ('À rendre dans $reste jours', Couleurs.succesFond, Couleurs.succes);
    return Carte(
      padding: const EdgeInsets.all(Espaces.md + 2),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(e.livreTitre, style: Typo.libelle, maxLines: 2, overflow: TextOverflow.ellipsis),
          Text(e.livreAuteur, style: Typo.legendePale),
          const SizedBox(height: Espaces.sm),
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(color: fond, borderRadius: BorderRadius.circular(Rayons.pilule)),
                child: Text(texte, style: Typo.legende.copyWith(color: couleur, fontWeight: FontWeight.w600, fontSize: 11.5)),
              ),
              const Spacer(),
              Flexible(
                child: Text(
                  e.rendu
                      ? 'Emprunté le ${Dates.courte(e.emprunteLe)}'
                      : 'Retour avant le ${Dates.courte(e.retourPrevu)}',
                  style: Typo.legendePale,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _FormulaireDepot extends StatefulWidget {
  const _FormulaireDepot({required this.service, required this.filieres});

  final BibliothequeService service;
  final List<FiliereBiblio> filieres;

  @override
  State<_FormulaireDepot> createState() => _FormulaireDepotState();
}

class _FormulaireDepotState extends State<_FormulaireDepot> {
  static const _tailleMax = 25 * 1024 * 1024;
  static const _extensions = ['pdf', 'doc', 'docx', 'odt', 'ppt', 'pptx', 'zip', 'png', 'jpg', 'jpeg'];

  final _titre = TextEditingController();
  final _auteur = TextEditingController();
  final _matiere = TextEditingController();
  final _annee = TextEditingController();
  bool _memoire = true;
  int? _filiereId;
  FichierChoisi? _fichier;
  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() {
    _titre.dispose();
    _auteur.dispose();
    _matiere.dispose();
    _annee.dispose();
    super.dispose();
  }

  bool get _valide =>
      _fichier != null &&
      _titre.text.trim().isNotEmpty &&
      (_memoire ? _auteur.text.trim().isNotEmpty : _matiere.text.trim().isNotEmpty);

  static String _taille(int o) => o < 1024 * 1024 ? '${(o / 1024).round()} Ko' : '${(o / 1024 / 1024).toStringAsFixed(1).replaceAll('.', ',')} Mo';

  Future<void> _choisir() async {
    try {
      final choisi = await choisirFichier(_extensions);
      if (choisi == null) return;
      if (choisi.octets.length > _tailleMax) {
        if (mounted) setState(() => _erreur = 'Ce fichier dépasse 25 Mo.');
        return;
      }
      if (!mounted) return;
      setState(() {
        _fichier = choisi;
        _erreur = null;
        if (_titre.text.trim().isEmpty) {
          final i = choisi.nom.lastIndexOf('.');
          _titre.text = i > 0 ? choisi.nom.substring(0, i) : choisi.nom;
        }
      });
    } catch (_) {
      if (mounted) setState(() => _erreur = 'Impossible d’ouvrir le sélecteur de fichiers sur cet appareil.');
    }
  }

  Future<void> _envoyer() async {
    final f = _fichier;
    if (!_valide || f == null || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      await widget.service.deposer(
        memoire: _memoire,
        titre: _titre.text,
        auteur: _auteur.text,
        matiere: _matiere.text,
        filiereId: _filiereId,
        annee: int.tryParse(_annee.text.trim()),
        nomFichier: f.nom,
        octets: f.octets,
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
      titre: 'Déposer un document',
      sousTitre: 'Il sera visible de tous après vérification par l’administration.',
      enfants: [
        if (_erreur != null) ...[
          Bandeau.erreur(message: _erreur!),
          const SizedBox(height: Espaces.md),
        ],
        SegmentedButton<bool>(
          segments: const [
            ButtonSegment(value: true, label: Text('Mémoire')),
            ButtonSegment(value: false, label: Text('Ancien sujet')),
          ],
          selected: {_memoire},
          onSelectionChanged: _envoi ? null : (s) => setState(() => _memoire = s.first),
        ),
        const SizedBox(height: Espaces.lg),
        GestureDetector(
          onTap: _envoi ? null : _choisir,
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: Espaces.lg, horizontal: Espaces.lg),
            decoration: BoxDecoration(
              color: f == null ? Couleurs.royal50 : Couleurs.succesFond,
              borderRadius: BorderRadius.circular(Rayons.tuile),
              border: Border.all(color: f == null ? Couleurs.royal200 : Couleurs.succesTrait),
            ),
            child: Column(
              children: [
                Icon(f == null ? Icons.cloud_upload_outlined : Icons.check_circle_rounded,
                    size: 28, color: f == null ? Couleurs.indigo600 : Couleurs.succesVif),
                const SizedBox(height: Espaces.xs),
                Text(f == null ? 'Choisir un fichier (25 Mo max)' : f.nom,
                    style: Typo.libelle.copyWith(fontSize: 13.5), textAlign: TextAlign.center, maxLines: 2, overflow: TextOverflow.ellipsis),
                if (f != null) Text('${_taille(f.octets.length)} · toucher pour changer', style: Typo.legendePale),
              ],
            ),
          ),
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Titre'),
        TextField(
          controller: _titre,
          maxLength: 160,
          textCapitalization: TextCapitalization.sentences,
          onChanged: (_) => setState(() {}),
        ),
        EtiquetteChamp(_memoire ? 'Auteur' : 'Matière'),
        TextField(
          controller: _memoire ? _auteur : _matiere,
          maxLength: 150,
          textCapitalization: TextCapitalization.words,
          onChanged: (_) => setState(() {}),
        ),
        const EtiquetteChamp('Filière', facultatif: true),
        DropdownButtonFormField<int?>(
          initialValue: _filiereId,
          isExpanded: true,
          onChanged: _envoi ? null : (id) => setState(() => _filiereId = id),
          items: [
            const DropdownMenuItem<int?>(value: null, child: Text('Non précisée')),
            for (final fl in widget.filieres)
              DropdownMenuItem<int?>(value: fl.id, child: Text(fl.libelle, maxLines: 1, overflow: TextOverflow.ellipsis)),
          ],
        ),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Année', facultatif: true),
        TextField(controller: _annee, keyboardType: TextInputType.number, maxLength: 4),
        const SizedBox(height: Espaces.sm),
        ActionsFormulaire(libelle: 'Déposer', envoi: _envoi, surValidation: _valide ? _envoyer : null),
      ],
    );
  }
}
