/// Modèles de la bibliothèque : documents numériques, livres physiques, emprunts.
library;

DateTime _date(Object? v) => DateTime.parse(v as String);

/// Mémoire ou ancien sujet d'examen.
class DocumentBiblio {
  const DocumentBiblio({
    required this.id,
    required this.estMemoire,
    required this.titre,
    required this.nomFichier,
    required this.taille,
    required this.statut,
    required this.deposePar,
    this.auteur,
    this.matiere,
    this.filiere,
    this.annee,
    this.description,
    this.motifRefus,
  });

  factory DocumentBiblio.depuisJson(Map<String, dynamic> j) => DocumentBiblio(
        id: j['id'] as String,
        estMemoire: j['type'] == 'MEMOIRE',
        titre: j['titre'] as String,
        auteur: j['auteur'] as String?,
        matiere: j['matiere'] as String?,
        filiere: j['filiereLibelle'] as String?,
        annee: (j['annee'] as num?)?.toInt(),
        description: j['description'] as String?,
        nomFichier: j['nomFichier'] as String,
        taille: (j['taille'] as num).toInt(),
        statut: j['statut'] as String,
        motifRefus: j['motifRefus'] as String?,
        deposePar: j['deposePar'] as String,
      );

  final String id;
  final bool estMemoire;
  final String titre;
  final String? auteur;
  final String? matiere;
  final String? filiere;
  final int? annee;
  final String? description;
  final String nomFichier;
  final int taille;

  /// `EN_ATTENTE`, `PUBLIE` ou `REFUSE`.
  final String statut;
  final String? motifRefus;
  final String deposePar;

  /// Auteur d'un mémoire, matière d'un sujet : ce qui identifie le document après son titre.
  String get sousTitre => (estMemoire ? auteur : matiere) ?? '';

  String get extension {
    final i = nomFichier.lastIndexOf('.');
    return i < 0 ? '' : nomFichier.substring(i + 1).toLowerCase();
  }

  String get tailleLisible {
    if (taille < 1024) return '$taille o';
    if (taille < 1024 * 1024) return '${(taille / 1024).round()} Ko';
    return '${(taille / 1024 / 1024).toStringAsFixed(1).replaceAll('.', ',')} Mo';
  }
}

/// Livre physique du fonds.
class LivreBiblio {
  const LivreBiblio({
    required this.id,
    required this.titre,
    required this.auteur,
    required this.exemplaires,
    required this.disponibles,
    this.categorie,
    this.filiere,
    this.emplacement,
    this.resume,
  });

  factory LivreBiblio.depuisJson(Map<String, dynamic> j) => LivreBiblio(
        id: j['id'] as String,
        titre: j['titre'] as String,
        auteur: j['auteur'] as String,
        categorie: j['categorie'] as String?,
        filiere: j['filiereLibelle'] as String?,
        emplacement: j['emplacement'] as String?,
        resume: j['resume'] as String?,
        exemplaires: (j['exemplaires'] as num).toInt(),
        disponibles: (j['disponibles'] as num).toInt(),
      );

  final String id;
  final String titre;
  final String auteur;
  final String? categorie;
  final String? filiere;
  final String? emplacement;
  final String? resume;
  final int exemplaires;
  final int disponibles;
}

/// Emprunt d'un livre par l'étudiant connecté.
class EmpruntBiblio {
  const EmpruntBiblio({
    required this.id,
    required this.livreTitre,
    required this.livreAuteur,
    required this.emprunteLe,
    required this.retourPrevu,
    required this.statut,
    required this.joursDeRetard,
    this.retourneLe,
  });

  factory EmpruntBiblio.depuisJson(Map<String, dynamic> j) => EmpruntBiblio(
        id: j['id'] as String,
        livreTitre: j['livreTitre'] as String,
        livreAuteur: j['livreAuteur'] as String,
        emprunteLe: _date(j['emprunteLe']),
        retourPrevu: _date(j['retourPrevu']),
        retourneLe: j['retourneLe'] == null ? null : _date(j['retourneLe']),
        statut: j['statut'] as String,
        joursDeRetard: (j['joursDeRetard'] as num).toInt(),
      );

  final String id;
  final String livreTitre;
  final String livreAuteur;
  final DateTime emprunteLe;
  final DateTime retourPrevu;
  final DateTime? retourneLe;

  /// `EN_COURS`, `EN_RETARD` ou `RETOURNE`.
  final String statut;
  final int joursDeRetard;

  bool get enRetard => statut == 'EN_RETARD';
  bool get rendu => statut == 'RETOURNE';

  /// Jours restants avant la date de retour ; négatif une fois dépassée.
  int joursRestants(DateTime aujourdhui) => DateTime(
          retourPrevu.year, retourPrevu.month, retourPrevu.day)
      .difference(DateTime(aujourdhui.year, aujourdhui.month, aujourdhui.day))
      .inDays;
}

/// Filière proposée au dépôt.
class FiliereBiblio {
  const FiliereBiblio(this.id, this.libelle);

  factory FiliereBiblio.depuisJson(Map<String, dynamic> j) =>
      FiliereBiblio((j['id'] as num).toInt(), j['libelle'] as String);

  final int id;
  final String libelle;
}
