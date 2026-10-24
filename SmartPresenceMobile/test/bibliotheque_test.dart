import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:smartpresence_mobile/core/api/client_api.dart';
import 'package:smartpresence_mobile/core/auth/auth_service.dart';
import 'package:smartpresence_mobile/models/bibliotheque.dart';
import 'package:smartpresence_mobile/screens/bibliotheque_screen.dart';

const _memoire = {
  'id': 'd1a2b3c4-0000-0000-0000-000000000001',
  'type': 'MEMOIRE',
  'titre': 'Securite des reseaux',
  'auteur': 'Sow Modibo',
  'filiereLibelle': 'Informatique',
  'annee': 2024,
  'nomFichier': 'memoire.pdf',
  'taille': 2097152,
  'statut': 'PUBLIE',
  'deposePar': 'Administration',
};

const _sujet = {
  'id': 'd1a2b3c4-0000-0000-0000-000000000002',
  'type': 'SUJET',
  'titre': 'Examen algorithmique',
  'matiere': 'Algorithmique',
  'annee': 2023,
  'nomFichier': 'examen.pdf',
  'taille': 120000,
  'statut': 'PUBLIE',
  'deposePar': 'Administration',
};

const _livre = {
  'id': 'l1',
  'titre': 'Introduction aux bases de donnees',
  'auteur': 'C. Date',
  'categorie': 'Informatique',
  'emplacement': 'Rayon B2',
  'exemplaires': 2,
  'disponibles': 0,
};

AuthService _auth() {
  final routes = <String, Object?>{
    '/bibliotheque/documents': [_memoire, _sujet],
    '/bibliotheque/livres': [_livre],
  };
  final client = MockClient((requete) async {
    final donnees = routes[requete.url.path.replaceFirst('/api', '')] ?? [];
    return http.Response.bytes(utf8.encode(jsonEncode({'success': true, 'data': donnees})), 200,
        headers: {'content-type': 'application/json; charset=utf-8'});
  });
  return AuthService(client: ClientApi(client: client));
}

void main() {
  testWidgets('Bibliothèque : documents, recherche locale et catalogue de livres', (t) async {
    t.view.physicalSize = const Size(390 * 3, 844 * 3);
    t.view.devicePixelRatio = 3;
    addTearDown(t.view.reset);
    await t.pumpWidget(MaterialApp(home: BibliothequeScreen(auth: _auth())));
    await t.pump();
    await t.pump(const Duration(seconds: 1));
    await t.pump(const Duration(seconds: 1));

    expect(find.text('Securite des reseaux'), findsOneWidget);
    expect(find.text('Examen algorithmique'), findsOneWidget);
    expect(find.text('2 documents · 1 livres'), findsOneWidget);

    // La recherche se fait sur l'appareil, donc aussi sans réseau, accents ignorés.
    await t.enterText(find.byType(TextField), 'ALGORITHMIQUE');
    await t.pump(const Duration(seconds: 1));
    expect(find.text('Securite des reseaux'), findsNothing);
    expect(find.text('Examen algorithmique'), findsOneWidget);

    await t.tap(find.widgetWithText(ChoiceChip, 'Livres'));
    await t.pump(const Duration(seconds: 1));
    await t.pump(const Duration(seconds: 1));
    expect(find.text('Introduction aux bases de donnees'), findsOneWidget);
    expect(find.text('Sorti'), findsOneWidget);
  });

  test('un emprunt calcule les jours restants et le retard', () {
    final emprunt = EmpruntBiblio.depuisJson({
      'id': 'e1',
      'livreTitre': 'Livre',
      'livreAuteur': 'Auteur',
      'emprunteLe': '2026-10-01',
      'retourPrevu': '2026-10-15',
      'retourneLe': null,
      'statut': 'EN_COURS',
      'joursDeRetard': 0,
    });
    expect(emprunt.joursRestants(DateTime(2026, 10, 12)), 3);
    expect(emprunt.joursRestants(DateTime(2026, 10, 17)), -2);
    expect(emprunt.enRetard, isFalse);
    expect(emprunt.rendu, isFalse);
  });

  test('un document déduit son sous-titre et sa taille lisible', () {
    final m = DocumentBiblio.depuisJson(Map<String, dynamic>.from(_memoire));
    final s = DocumentBiblio.depuisJson(Map<String, dynamic>.from(_sujet));
    expect(m.sousTitre, 'Sow Modibo');
    expect(s.sousTitre, 'Algorithmique');
    expect(m.tailleLisible, '2,0 Mo');
    expect(s.tailleLisible, '117 Ko');
    expect(m.extension, 'pdf');
  });
}
