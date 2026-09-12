import '../core/api/client_api.dart';
import '../models/note.dart';
import '../models/presence.dart';
import '../models/releve.dart';
import '../models/seance.dart';
import '../models/statistiques.dart';

/// Données personnelles de l'étudiant connecté.
///
/// Toutes les requêtes passent par l'espace `/moi` : aucun identifiant d'étudiant n'est
/// jamais transmis, le serveur le déduit du jeton. L'application n'a donc structurellement
/// pas les moyens de demander le dossier de quelqu'un d'autre.
class EtudiantService {
  const EtudiantService(this._client);

  final ClientApi _client;

  /// Relevés de présence sur une période, paginés.
  Future<PagePresences> mesPresences({
    DateTime? debut,
    DateTime? fin,
    int page = 0,
    int taille = 20,
  }) async {
    final donnees = await _client.get('/moi/presences', parametres: {
      'debut': debut == null ? null : _iso(debut),
      'fin': fin == null ? null : _iso(fin),
      'page': page,
      'taille': taille,
    });
    if (donnees == null) return PagePresences.vide;
    return PagePresences.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<Assiduite> monAssiduite() async {
    final donnees = await _client.get('/moi/statistiques');
    if (donnees == null) return Assiduite.vide;
    return Assiduite.depuisJson(donnees as Map<String, dynamic>);
  }

  /// Bulletin de l'étudiant connecté.
  ///
  /// Les moyennes sont calculées par le serveur, pas ici : trois clients qui les
  /// recalculeraient chacun de leur côté finiraient par diverger d'un centième.
  Future<Bulletin> monBulletin({PeriodeScolaire? periode}) async {
    final donnees = await _client.get('/moi/bulletin', parametres: {
      'periode': periode?.code,
    });
    if (donnees == null) return Bulletin.vide;
    return Bulletin.depuisJson(donnees as Map<String, dynamic>);
  }

  /// Relevé de notes LMD du semestre : UE, ECUE, crédits et décision.
  ///
  /// Sans semestre, le serveur rend le premier de la maquette de la promotion —
  /// ouvrir sur un onglet vide alors qu'un seul semestre existe serait un écran
  /// blanc pour rien.
  Future<ReleveSemestre> monReleve({PeriodeScolaire? semestre}) async {
    final donnees = await _client.get('/moi/releve', parametres: {
      'semestre': semestre?.code,
    });
    if (donnees == null) return ReleveSemestre.vide;
    return ReleveSemestre.depuisJson(donnees as Map<String, dynamic>);
  }

  /// Séances de ma classe sur la période. Sans dates, les sept jours à venir.
  Future<List<Seance>> monEmploiDuTemps({DateTime? debut, DateTime? fin}) async {
    final donnees = await _client.get('/moi/emploi-du-temps', parametres: {
      'debut': debut == null ? null : _iso(debut),
      'fin': fin == null ? null : _iso(fin),
    });
    if (donnees is! List) return const [];
    return donnees
        .map((e) => Seance.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<Justificatif>> mesJustificatifs() async {
    final donnees = await _client.get('/moi/justificatifs');
    if (donnees is! List) return const [];
    return donnees
        .map((e) => Justificatif.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  static String _iso(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}';
}

/// Justificatif d'absence déposé par l'étudiant.
class Justificatif {
  const Justificatif({
    required this.id,
    required this.dateAbsence,
    required this.motif,
    required this.statut,
    this.commentaire,
  });

  final String id;
  final DateTime dateAbsence;
  final String motif;

  /// `EN_ATTENTE`, `ACCEPTEE` ou `REFUSEE`.
  final String statut;
  final String? commentaire;

  bool get enAttente => statut == 'EN_ATTENTE';
  bool get approuve => statut == 'ACCEPTEE';

  String get libelleStatut => switch (statut) {
        'EN_ATTENTE' => 'En attente',
        'ACCEPTEE' => 'Approuvé',
        'REFUSEE' => 'Rejeté',
        _ => statut,
      };

  factory Justificatif.depuisJson(Map<String, dynamic> json) => Justificatif(
        id: json['id'] as String? ?? '',
        dateAbsence:
            DateTime.tryParse(json['dateAbsence'] as String? ?? '') ?? DateTime.now(),
        motif: json['motif'] as String? ?? '',
        statut: json['statut'] as String? ?? '',
        commentaire: json['commentaireTraitement'] as String?,
      );
}
