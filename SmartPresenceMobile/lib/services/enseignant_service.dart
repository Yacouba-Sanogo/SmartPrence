import '../core/api/client_api.dart';
import '../models/etudiant_inscrit.dart';
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

  static String _iso(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}';
}
