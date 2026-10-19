import '../core/api/client_api.dart';
import '../models/annonce.dart';
import '../models/enseignement.dart';
import '../models/etudiant_inscrit.dart';
import '../models/gestion.dart';
import '../models/note.dart';
import '../models/seance.dart';
import '../models/signalement.dart';

/// Données de l'enseignant connecté.
///
/// Comme pour l'étudiant, tout passe par l'espace `/moi` : le serveur déduit
/// l'enseignant du jeton. Aucun écran ne peut demander l'emploi du temps d'un collègue,
/// et la feuille d'une séance qui n'est pas la sienne est refusée côté serveur.
class EnseignantService {
  const EnseignantService(this._client);

  final ClientApi _client;

  /// Séances du jour demandé, triées par heure de début.
  Future<List<Seance>> mesSeances(DateTime jour) async {
    final donnees = await _client.get('/moi/seances', parametres: {'date': _iso(jour)});
    if (donnees is! List) return const [];
    final seances = donnees
        .map((e) => Seance.depuisJson(e as Map<String, dynamic>))
        .toList()
      ..sort((a, b) => a.debut.compareTo(b.debut));
    return seances;
  }

  /// Feuille de présence constatée par le lecteur pour une séance.
  Future<FeuilleSeance> feuille(String seanceId) async {
    final donnees = await _client.get('/moi/seances/$seanceId/feuille');
    return FeuilleSeance.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<List<ClasseEnseignee>> mesClasses() async {
    final donnees = await _client.get('/moi/classes');
    if (donnees is! List) return const [];
    return donnees
        .map((e) => ClasseEnseignee.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Effectif d'une classe où l'enseignant intervient.
  ///
  /// Le serveur refuse la classe d'un collègue : le rattachement est vérifié côté
  /// serveur, pas ici. Il trie déjà par nom.
  Future<List<EtudiantInscrit>> etudiantsDeLaClasse(int classeId) async {
    final donnees = await _client.get('/moi/classes/$classeId/etudiants');
    if (donnees is! List) return const [];
    return donnees
        .map((e) => EtudiantInscrit.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  // ----- Notes ---------------------------------------------------------

  /// Matières actives, pour la saisie des notes.
  Future<List<MatiereBreve>> matieres() async {
    final donnees = await _client.get('/moi/matieres');
    if (donnees is! List) return const [];
    return donnees
        .map((e) => MatiereBreve.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Notes d'une classe où l'enseignant intervient.
  Future<List<Note>> notesDeLaClasse(
    int classeId, {
    int? matiereId,
    PeriodeScolaire? periode,
  }) async {
    final donnees = await _client.get('/moi/classes/$classeId/notes', parametres: {
      'matiereId': matiereId,
      'periode': periode?.code,
    });
    if (donnees is! List) return const [];
    return donnees
        .map((e) => Note.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Saisit une note. Le serveur refuse si la classe n'est pas la sienne.
  Future<Note> saisirNote(SaisieNote saisie) async {
    final donnees = await _client.post('/moi/notes', corps: saisie.versJson());
    return Note.depuisJson(donnees as Map<String, dynamic>);
  }

  /// Corrige une note dont l'enseignant est l'auteur.
  Future<Note> modifierNote(String noteId, SaisieNote saisie) async {
    final donnees =
        await _client.put('/moi/notes/$noteId', corps: saisie.versJson());
    return Note.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<void> supprimerNote(String noteId) async {
    await _client.delete('/moi/notes/$noteId');
  }

  /// Dépose une anomalie. Le relevé n'est pas modifié : la scolarité arbitrera.
  Future<Signalement> signaler({
    required String seanceId,
    required TypeSignalement type,
    required String description,
    String? etudiantId,
  }) async {
    final donnees = await _client.post(
      '/moi/seances/$seanceId/signalements',
      corps: {
        'type': type.code,
        'description': description.trim(),
        if (etudiantId != null) 'etudiantId': etudiantId,
      },
    );
    return Signalement.depuisJson(donnees as Map<String, dynamic>);
  }

  /// Signalements déposés par l'enseignant, avec la suite qui leur a été donnée.
  Future<List<Signalement>> mesSignalements() async {
    final donnees = await _client.get('/moi/signalements');
    if (donnees is! List) return const [];
    return donnees
        .map((e) => Signalement.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  // ----- Supports de cours ----------------------------------------------

  Future<List<SupportCours>> supports(int classeId) async {
    final donnees = await _client.get('/moi/classes/$classeId/supports');
    if (donnees is! List) return const [];
    return donnees.map((e) => SupportCours.depuisJson(e as Map<String, dynamic>)).toList();
  }

  Future<SupportCours> publierSupport({
    required int classeId,
    required int matiereId,
    required String titre,
    String? description,
    required String nomFichier,
    required List<int> octets,
  }) async {
    final donnees = await _client.televerser(
      '/moi/classes/$classeId/supports',
      champs: {
        'matiereId': '$matiereId',
        'titre': titre.trim(),
        if (description != null && description.trim().isNotEmpty) 'description': description.trim(),
      },
      champFichier: 'fichier',
      octets: octets,
      nomFichier: nomFichier,
    );
    return SupportCours.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<void> supprimerSupport(String id) async {
    await _client.delete('/moi/supports/$id');
  }

  // ----- Congés ---------------------------------------------------------

  Future<List<DemandeConge>> mesConges() async {
    final donnees = await _client.get('/moi/conges');
    if (donnees is! List) return const [];
    return donnees.map((e) => DemandeConge.depuisJson(e as Map<String, dynamic>)).toList();
  }

  Future<DemandeConge> demanderConge({
    required TypeConge type,
    required DateTime debut,
    required DateTime fin,
    required String motif,
  }) async {
    final donnees = await _client.post('/moi/conges', corps: {
      'type': type.code,
      'debut': _iso(debut),
      'fin': _iso(fin),
      'motif': motif.trim(),
    });
    return DemandeConge.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<void> annulerConge(String id) async {
    await _client.post('/moi/conges/$id/annulation');
  }

  // ----- Réclamations reçues -------------------------------------------

  Future<List<Reclamation>> reclamationsRecues() async {
    final donnees = await _client.get('/moi/reclamations/recues');
    if (donnees is! List) return const [];
    return donnees.map((e) => Reclamation.depuisJson(e as Map<String, dynamic>)).toList();
  }

  Future<Reclamation> repondreReclamation(
    String id, {
    required bool accepter,
    required String reponse,
    double? valeurCorrigee,
  }) async {
    final donnees = await _client.post('/moi/reclamations/$id/reponse', corps: {
      'accepter': accepter,
      'reponse': reponse.trim(),
      if (accepter && valeurCorrigee != null) 'valeurCorrigee': valeurCorrigee,
    });
    return Reclamation.depuisJson(donnees as Map<String, dynamic>);
  }

  // ----- Annonces et notifications -------------------------------------

  /// Mes notifications, de la plus récente à la plus ancienne.
  Future<List<Annonce>> annonces() async {
    final donnees = await _client.get('/notifications');
    if (donnees is! List) return const [];
    return donnees.map((e) => Annonce.depuisJson(e as Map<String, dynamic>)).toList();
  }

  Future<int> nombreNonLues() async {
    final donnees = await _client.get('/notifications/non-lues');
    return donnees is num ? donnees.toInt() : 0;
  }

  Future<void> marquerLue(String id) async {
    await _client.patch('/notifications/$id/lue');
  }

  Future<void> toutMarquerLu() async {
    await _client.patch('/notifications/lues');
  }

  // ----- Présence de l'enseignant et séances effectuées ----------------

  /// Mes pointages sur la période (par défaut les trente derniers jours).
  Future<List<JourneePointage>> mesPointages({DateTime? debut, DateTime? fin}) async {
    final donnees = await _client.get('/moi/pointages', parametres: {
      'debut': debut == null ? null : _iso(debut),
      'fin': fin == null ? null : _iso(fin),
    });
    if (donnees is! List) return const [];
    return donnees
        .map((e) => JourneePointage.depuisJson(e as Map<String, dynamic>))
        .toList()
      ..sort((a, b) => b.date.compareTo(a.date));
  }

  /// Déclare la séance effectuée. Refusée si elle est annulée ou pas encore commencée.
  Future<Seance> declarerSeance(String seanceId) async {
    final donnees = await _client.post('/moi/seances/$seanceId/declaration');
    return Seance.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<BilanEnseignement> bilan({DateTime? debut, DateTime? fin}) async {
    final donnees = await _client.get('/moi/bilan', parametres: {
      'debut': debut == null ? null : _iso(debut),
      'fin': fin == null ? null : _iso(fin),
    });
    return BilanEnseignement.depuisJson(donnees as Map<String, dynamic>);
  }

  // ----- Cahier de textes et programme ---------------------------------

  /// Page du cahier d'une séance, ou `null` si rien n'a encore été écrit.
  Future<PageCahier?> cahierDeSeance(String seanceId) async {
    final donnees = await _client.get('/moi/seances/$seanceId/cahier');
    if (donnees is! Map<String, dynamic>) return null;
    return PageCahier.depuisJson(donnees);
  }

  Future<PageCahier> ecrireCahier(
    String seanceId, {
    required String contenu,
    String? travailAFaire,
    String? chapitreId,
    bool chapitreTermine = false,
  }) async {
    final donnees = await _client.put('/moi/seances/$seanceId/cahier', corps: {
      'contenu': contenu.trim(),
      if (travailAFaire != null && travailAFaire.trim().isNotEmpty)
        'travailAFaire': travailAFaire.trim(),
      if (chapitreId != null) 'chapitreId': chapitreId,
      'chapitreTermine': chapitreTermine,
    });
    return PageCahier.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<List<PageCahier>> cahierDeClasse(int classeId, {int? matiereId}) async {
    final donnees = await _client.get('/moi/classes/$classeId/cahier',
        parametres: {'matiereId': matiereId});
    if (donnees is! List) return const [];
    return donnees
        .map((e) => PageCahier.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<Syllabus>> syllabus(int classeId, {int? matiereId}) async {
    final donnees = await _client.get('/moi/classes/$classeId/chapitres',
        parametres: {'matiereId': matiereId});
    if (donnees is! List) return const [];
    return donnees
        .map((e) => Syllabus.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<Chapitre> ajouterChapitre({
    required int classeId,
    required int matiereId,
    required String titre,
    String? objectifs,
  }) async {
    final donnees = await _client.post('/moi/chapitres', corps: {
      'classeId': classeId,
      'matiereId': matiereId,
      'titre': titre.trim(),
      if (objectifs != null && objectifs.trim().isNotEmpty) 'objectifs': objectifs.trim(),
    });
    return Chapitre.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<Chapitre> modifierChapitre(
    String chapitreId, {
    required String titre,
    String? objectifs,
    required bool termine,
  }) async {
    final donnees = await _client.put('/moi/chapitres/$chapitreId', corps: {
      'titre': titre.trim(),
      if (objectifs != null && objectifs.trim().isNotEmpty) 'objectifs': objectifs.trim(),
      'termine': termine,
    });
    return Chapitre.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<void> supprimerChapitre(String chapitreId) async {
    await _client.delete('/moi/chapitres/$chapitreId');
  }

  // ----- Évaluations et validation des notes ---------------------------

  Future<List<Evaluation>> evaluations(int classeId, {int? matiereId}) async {
    final donnees = await _client.get('/moi/classes/$classeId/evaluations',
        parametres: {'matiereId': matiereId});
    if (donnees is! List) return const [];
    return donnees
        .map((e) => Evaluation.depuisJson(e as Map<String, dynamic>))
        .toList();
  }

  Map<String, dynamic> _corpsEvaluation({
    int? classeId,
    required int matiereId,
    required TypeEvaluation type,
    required String libelle,
    required DateTime date,
    required int bareme,
    required int coefficient,
    required PeriodeScolaire periode,
    String? sujet,
  }) =>
      {
        if (classeId != null) 'classeId': classeId,
        'matiereId': matiereId,
        'type': type.code,
        'libelle': libelle.trim(),
        'dateEvaluation': _iso(date),
        'bareme': bareme,
        'coefficient': coefficient,
        'periode': periode.code,
        if (sujet != null && sujet.trim().isNotEmpty) 'sujet': sujet.trim(),
      };

  Future<Evaluation> planifierEvaluation({
    required int classeId,
    required int matiereId,
    required TypeEvaluation type,
    required String libelle,
    required DateTime date,
    required int bareme,
    required int coefficient,
    required PeriodeScolaire periode,
    String? sujet,
  }) async {
    final donnees = await _client.post('/moi/evaluations',
        corps: _corpsEvaluation(
          classeId: classeId,
          matiereId: matiereId,
          type: type,
          libelle: libelle,
          date: date,
          bareme: bareme,
          coefficient: coefficient,
          periode: periode,
          sujet: sujet,
        ));
    return Evaluation.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<Evaluation> modifierEvaluation(
    String evaluationId, {
    required int matiereId,
    required TypeEvaluation type,
    required String libelle,
    required DateTime date,
    required int bareme,
    required int coefficient,
    required PeriodeScolaire periode,
    String? sujet,
  }) async {
    final donnees = await _client.put('/moi/evaluations/$evaluationId',
        corps: _corpsEvaluation(
          matiereId: matiereId,
          type: type,
          libelle: libelle,
          date: date,
          bareme: bareme,
          coefficient: coefficient,
          periode: periode,
          sujet: sujet,
        ));
    return Evaluation.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<void> supprimerEvaluation(String evaluationId) async {
    await _client.delete('/moi/evaluations/$evaluationId');
  }

  Future<ResultatsEvaluation> resultats(String evaluationId) async {
    final donnees = await _client.get('/moi/evaluations/$evaluationId/resultats');
    return ResultatsEvaluation.depuisJson(donnees as Map<String, dynamic>);
  }

  /// Enregistre des notes sur le barème de l'épreuve, ou des absences. Le serveur
  /// ramène lui-même chaque note sur 20.
  Future<ResultatsEvaluation> enregistrerResultats(
    String evaluationId,
    List<SaisieResultat> saisies,
  ) async {
    final donnees = await _client.put('/moi/evaluations/$evaluationId/resultats', corps: {
      'resultats': saisies.map((s) => s.versJson()).toList(),
    });
    return ResultatsEvaluation.depuisJson(donnees as Map<String, dynamic>);
  }

  /// Envoie à l'administration les notes en brouillon de l'évaluation.
  Future<void> transmettreEvaluation(String evaluationId) async {
    await _client.post('/moi/evaluations/$evaluationId/transmission');
  }

  /// Envoie à l'administration des notes saisies hors évaluation.
  Future<void> transmettreNotes(List<String> noteIds) async {
    await _client.post('/moi/notes/transmettre', corps: {'noteIds': noteIds});
  }

  static String _iso(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}';
}
