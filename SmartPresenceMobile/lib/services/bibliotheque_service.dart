import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

import '../core/api/client_api.dart';
import '../core/api/erreurs.dart';
import '../models/bibliotheque.dart';

/// Accès à la bibliothèque : catalogue, documents, emprunts et dépôts de l'étudiant.
///
/// Les lectures passent par [ClientApi.get] : sans réseau, elles reviennent du cache, donc
/// le catalogue et les dates de retour restent consultables hors ligne.
class BibliothequeService {
  BibliothequeService(this._client);

  final ClientApi _client;

  Future<List<DocumentBiblio>> documents({String? type, String? q}) async {
    final donnees = await _client.get('/bibliotheque/documents', parametres: {
      'type': type,
      'q': (q?.trim().isEmpty ?? true) ? null : q!.trim(),
    });
    return _liste(donnees, DocumentBiblio.depuisJson);
  }

  Future<List<DocumentBiblio>> mesDepots() async => _liste(
      await _client.get('/bibliotheque/mes-depots'), DocumentBiblio.depuisJson);

  Future<List<LivreBiblio>> livres({String? q}) async {
    final donnees = await _client.get('/bibliotheque/livres',
        parametres: {'q': (q?.trim().isEmpty ?? true) ? null : q!.trim()});
    return _liste(donnees, LivreBiblio.depuisJson);
  }

  Future<List<EmpruntBiblio>> mesEmprunts() async => _liste(
      await _client.get('/bibliotheque/mes-emprunts'),
      EmpruntBiblio.depuisJson);

  Future<List<FiliereBiblio>> filieres() async => _liste(
      await _client.get('/referentiel/filieres'), FiliereBiblio.depuisJson);

  /// Dépose un mémoire ou un sujet : il attend la décision de l'administration.
  Future<DocumentBiblio> deposer({
    required bool memoire,
    required String titre,
    String? auteur,
    String? matiere,
    int? filiereId,
    int? annee,
    String? description,
    required String nomFichier,
    required List<int> octets,
  }) async {
    String? net(String? s) => (s == null || s.trim().isEmpty) ? null : s.trim();
    final donnees = await _client.televerser(
      '/bibliotheque/documents',
      champs: {
        'type': memoire ? 'MEMOIRE' : 'SUJET',
        'titre': titre.trim(),
        if (net(auteur) != null) 'auteur': net(auteur)!,
        if (net(matiere) != null) 'matiere': net(matiere)!,
        if (filiereId != null) 'filiereId': '$filiereId',
        if (annee != null) 'annee': '$annee',
        if (net(description) != null) 'description': net(description)!,
      },
      champFichier: 'fichier',
      octets: octets,
      nomFichier: nomFichier,
    );
    return DocumentBiblio.depuisJson(donnees as Map<String, dynamic>);
  }

  Future<void> retirerDepot(String id) async {
    await _client.delete('/bibliotheque/documents/$id');
  }

  /// Télécharge un document puis l'ouvre avec l'application du téléphone.
  Future<void> ouvrir(DocumentBiblio doc) async {
    if (kIsWeb) {
      throw const ErreurApi(
          'Le téléchargement est disponible dans l’application mobile.');
    }
    final octets = await _client
        .telechargerOctets('/bibliotheque/documents/${doc.id}/fichier');
    final dossier = await getTemporaryDirectory();
    final chemin =
        '${dossier.path}/${doc.id.substring(0, 8)}_${doc.nomFichier}';
    await File(chemin).writeAsBytes(octets, flush: true);
    final resultat = await OpenFilex.open(chemin);
    if (resultat.type != ResultType.done) {
      throw ErreurApi(
          'Aucune application ne peut ouvrir ce fichier (.${doc.extension}).');
    }
  }

  List<T> _liste<T>(dynamic donnees, T Function(Map<String, dynamic>) depuis) =>
      [
        for (final e in (donnees as List? ?? const []))
          depuis(e as Map<String, dynamic>)
      ];
}
