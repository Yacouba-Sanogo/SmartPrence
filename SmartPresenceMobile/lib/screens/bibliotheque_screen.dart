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
import '../widgets/accueil_etudiant.dart';
import '../widgets/rayonnages.dart';
import '../core/preferences_app.dart';
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
          ProtegeCouleurs(child: Container(
            width: double.infinity,
            clipBehavior: Clip.antiAlias,
            decoration: const BoxDecoration(
              gradient: Couleurs.degradeEntete,
              borderRadius: BorderRadius.vertical(bottom: Radius.circular(28)),
            ),
            child: Stack(
              children: [
                const Positioned.fill(child: FondRayonnages()),
                Padding(
                  padding: EdgeInsets.fromLTRB(
                      Espaces.sm, MediaQuery.of(context).padding.top + Espaces.sm, Espaces.lg, Espaces.xxl + 14),
                  child: Row(
                    children: [
                      ActionEntete(icone: Icons.arrow_back_rounded, surTap: () => Navigator.of(context).maybePop()),
                      const SizedBox(width: Espaces.xs),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text('Bibliothèque', style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 22)),
                            const SizedBox(height: 2),
                            Text('Vos documents, toujours à portée de main',
                                style: Typo.legende.copyWith(color: Colors.white.withValues(alpha: 0.82), fontSize: 12)),
                          ],
                        ),
                      ),
                      if (_etudiant) ActionEntete(icone: Icons.upload_file_rounded, surTap: _deposer),
                    ],
                  ),
                ),
              ],
            ),
          )),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(top: Espaces.md),
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

  static const _icones = {
    _Onglet.documents: Icons.description_outlined,
    _Onglet.livres: Icons.menu_book_outlined,
    _Onglet.emprunts: Icons.bookmark_border_rounded,
    _Onglet.depots: Icons.upload_file_outlined,
  };

  int _nombre(_Onglet o) => switch (o) {
        _Onglet.documents => _documents.length,
        _Onglet.livres => _livres.length,
        _Onglet.emprunts => _emprunts.length,
        _Onglet.depots => _depots.length,
      };

  Widget _selecteur() {
    return SizedBox(
      height: 84,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: Espaces.lg),
        itemCount: _onglets.length,
        separatorBuilder: (_, __) => const SizedBox(width: Espaces.sm + 2),
        itemBuilder: (_, i) {
          final o = _onglets[i];
          final actif = _onglet == o;
          return GestureDetector(
            onTap: () => setState(() {
              _onglet = o;
              _recherche.clear();
            }),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              width: 104,
              padding: const EdgeInsets.fromLTRB(Espaces.md, Espaces.md - 2, Espaces.md, Espaces.md - 2),
              decoration: BoxDecoration(
                gradient: actif ? Couleurs.degradeEntete : null,
                color: actif ? null : Couleurs.carte,
                borderRadius: BorderRadius.circular(Rayons.tuile),
                boxShadow: actif ? Couleurs.ombreRoyal : Couleurs.ombreCarte,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Icon(_icones[o], size: 22, color: actif ? Colors.white : Couleurs.royal600),
                      const Spacer(),
                      Text('${_nombre(o)}',
                          style: Typo.mono(15, graisse: FontWeight.w700, couleur: actif ? Colors.white : Couleurs.royal600)),
                    ],
                  ),
                  Text(_libelles[o]!,
                      style: Typo.legende.copyWith(
                          fontSize: 12, fontWeight: FontWeight.w700, color: actif ? Colors.white : Couleurs.encre),
                      maxLines: 1),
                ],
              ),
            ),
          );
        },
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
                      child: _PuceFiltre(texte, actif: _type == valeur, surTap: () => setState(() => _type = valeur)),
                    ),
                  for (final f in filieres)
                    Padding(
                      padding: const EdgeInsets.only(right: Espaces.xs + 2),
                      child: _PuceFiltre(f, actif: _filiere == f, surTap: () => setState(() => _filiere = _filiere == f ? null : f)),
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
      const TitreSection('Documents récents', icone: Icons.history_rounded),
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

/// Puce de filtre : pleine et bleue quand elle est active, contour gris discret sinon.
class _PuceFiltre extends StatelessWidget {
  const _PuceFiltre(this.libelle, {required this.actif, required this.surTap});

  final String libelle;
  final bool actif;
  final VoidCallback surTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: surTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: actif ? Couleurs.royal600 : Couleurs.carte,
          borderRadius: BorderRadius.circular(Rayons.pilule),
          border: Border.all(color: actif ? Couleurs.royal600 : Couleurs.trait),
        ),
        child: Text(libelle,
            style: Typo.legende.copyWith(
                fontSize: 12.5, fontWeight: FontWeight.w600, color: actif ? Colors.white : Couleurs.encreAttenuee)),
      ),
    );
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
    final teinte = doc.estMemoire ? Couleurs.royal600 : Couleurs.alerte;
    final fond = doc.estMemoire ? Couleurs.royal50 : Couleurs.alerteFond;
    final (texteStatut, couleurStatut) = switch (doc.statut) {
      'PUBLIE' => ('Publié', Couleurs.succes),
      'REFUSE' => ('Refusé', Couleurs.danger),
      _ => ('En attente de validation', Couleurs.alerte),
    };
    final extension = doc.extension.toUpperCase();

    return Carte(
      surTap: enCours ? null : surTap,
      rayon: Rayons.carte - 4,
      padding: const EdgeInsets.fromLTRB(Espaces.md, Espaces.md, Espaces.sm, Espaces.md),
      enfant: Row(
        children: [
          Container(
            width: 58,
            height: 70,
            decoration: BoxDecoration(
              color: fond,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: teinte.withValues(alpha: 0.18)),
            ),
            child: enCours
                ? Padding(padding: const EdgeInsets.all(20), child: CircularProgressIndicator(strokeWidth: 2.2, color: teinte))
                : Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(doc.estMemoire ? Icons.school_rounded : Icons.quiz_rounded, size: 26, color: teinte),
                      const SizedBox(height: 4),
                      Text(extension.isEmpty ? 'DOC' : extension,
                          style: Typo.legende.copyWith(color: teinte, fontSize: 9.5, fontWeight: FontWeight.w800)),
                    ],
                  ),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(child: Text(doc.titre, style: Typo.libelle.copyWith(fontSize: 13.5), maxLines: 2, overflow: TextOverflow.ellipsis)),
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(color: fond, borderRadius: BorderRadius.circular(Rayons.pilule)),
                      child: Text(doc.estMemoire ? 'Mémoire' : 'Sujet',
                          style: Typo.legende.copyWith(color: teinte, fontSize: 10, fontWeight: FontWeight.w700)),
                    ),
                  ],
                ),
                const SizedBox(height: 3),
                if (doc.sousTitre.isNotEmpty || doc.annee != null)
                  Row(
                    children: [
                      const Icon(Icons.person_outline_rounded, size: 13, color: Couleurs.encreDiscrete),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          [if (doc.sousTitre.isNotEmpty) doc.sousTitre, if (doc.annee != null) '${doc.annee}'].join(' · '),
                          style: Typo.legendePale.copyWith(fontSize: 11),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                const SizedBox(height: 2),
                Row(
                  children: [
                    const Icon(Icons.insert_drive_file_outlined, size: 13, color: Couleurs.encreDiscrete),
                    const SizedBox(width: 4),
                    Flexible(
                      child: Text(
                        [if (extension.isNotEmpty) extension, doc.tailleLisible, if (doc.filiere != null) doc.filiere!].join(' · '),
                        style: Typo.legendePale.copyWith(fontSize: 11),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
                if (afficherStatut) ...[
                  const SizedBox(height: 4),
                  Text(texteStatut, style: Typo.legende.copyWith(color: couleurStatut, fontWeight: FontWeight.w700, fontSize: 11.5)),
                  if (doc.statut == 'REFUSE' && doc.motifRefus != null)
                    Text('Motif : ${doc.motifRefus}', style: Typo.legendePale.copyWith(fontSize: 11)),
                ],
              ],
            ),
          ),
          const SizedBox(width: 4),
          action ??
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(color: Couleurs.royal200),
                ),
                child: const Icon(Icons.file_download_outlined, size: 19, color: Couleurs.royal600),
              ),
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
      rayon: Rayons.carte - 4,
      padding: const EdgeInsets.all(Espaces.md),
      enfant: Row(
        children: [
          Container(
            width: 58,
            height: 70,
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [Couleurs.royal500, Couleurs.royal800],
              ),
              borderRadius: BorderRadius.circular(8),
              boxShadow: Couleurs.ombreCarte,
            ),
            child: const Icon(Icons.menu_book_rounded, size: 28, color: Colors.white),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(child: Text(livre.titre, style: Typo.libelle.copyWith(fontSize: 13.5), maxLines: 2, overflow: TextOverflow.ellipsis)),
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: dispo ? Couleurs.succesFond : Couleurs.dangerFond,
                        borderRadius: BorderRadius.circular(Rayons.pilule),
                      ),
                      child: Text(
                        dispo ? '${livre.disponibles} dispo' : 'Sorti',
                        style: Typo.legende.copyWith(color: dispo ? Couleurs.succes : Couleurs.danger, fontWeight: FontWeight.w700, fontSize: 10),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 3),
                Row(
                  children: [
                    const Icon(Icons.person_outline_rounded, size: 13, color: Couleurs.encreDiscrete),
                    const SizedBox(width: 4),
                    Flexible(
                      child: Text(
                        [livre.auteur, if (livre.categorie != null) livre.categorie!].join(' · '),
                        style: Typo.legendePale.copyWith(fontSize: 11),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
                if (livre.emplacement != null) ...[
                  const SizedBox(height: 2),
                  Row(
                    children: [
                      const Icon(Icons.place_outlined, size: 13, color: Couleurs.encreDiscrete),
                      const SizedBox(width: 4),
                      Text('Rayon : ${livre.emplacement}', style: Typo.legendePale.copyWith(fontSize: 11)),
                    ],
                  ),
                ],
              ],
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
