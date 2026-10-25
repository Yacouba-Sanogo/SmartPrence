import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:smartpresence_mobile/core/api/client_api.dart';
import 'package:smartpresence_mobile/core/auth/auth_service.dart';
import 'package:smartpresence_mobile/screens/etudiant/accueil_screen.dart';

String _jour(DateTime d) =>
    '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

AuthService _auth({bool vide = false}) {
  final maintenant = DateTime.now();
  final debut = maintenant.add(const Duration(hours: 2));
  final client = MockClient((requete) async {
    final chemin = requete.url.path;
    Object? donnees = [];
    if (chemin.endsWith('/moi/statistiques')) {
      donnees = vide
          ? {'totalPresences': 0, 'totalPresents': 0, 'totalRetards': 0, 'totalAbsents': 0, 'totalJustifies': 0, 'tauxAssiduite': 0}
          : {'totalPresences': 20, 'totalPresents': 17, 'totalRetards': 2, 'totalAbsents': 1, 'totalJustifies': 0, 'tauxAssiduite': 92.5};
    } else if (chemin.endsWith('/moi/presences')) {
      donnees = {
        'content': vide
            ? []
            : [
                {
                  'id': 'p1',
                  'datePresence': _jour(maintenant),
                  'heurePresence': '07:56:00',
                  'statut': 'PRESENT',
                  'matiereLibelle': 'Algorithmique',
                  'salleNom': 'Amphi A',
                }
              ],
        'last': true,
        'totalElements': vide ? 0 : 1,
      };
    } else if (chemin.endsWith('/moi/emploi-du-temps') && !vide) {
      donnees = [
        {
          'id': 's1',
          'matiereLibelle': 'Bases de données relationnelles',
          'classeCode': 'L1',
          'debut': debut.toUtc().toIso8601String(),
          'fin': debut.add(const Duration(hours: 2)).toUtc().toIso8601String(),
          'statut': 'PLANIFIEE',
          'salleLibelle': 'Salle 204',
        }
      ];
    } else if (chemin.endsWith('/notifications') && !vide) {
      donnees = [
        {'id': 'n1', 'type': 'INFORMATION', 'titre': 'Nouveau support', 'message': 'Chapitre 3', 'lue': false, 'createdAt': maintenant.toUtc().toIso8601String()},
      ];
    }
    return http.Response.bytes(utf8.encode(jsonEncode({'success': true, 'data': donnees})), 200,
        headers: {'content-type': 'application/json; charset=utf-8'});
  });
  return AuthService(client: ClientApi(client: client));
}

Future<void> _afficher(WidgetTester t, AuthService auth, {double largeur = 390}) async {
  t.view.physicalSize = Size(largeur * 3, 844 * 3);
  t.view.devicePixelRatio = 3;
  addTearDown(t.view.reset);
  await t.pumpWidget(MaterialApp(home: AccueilEtudiantScreen(auth: auth)));
  await t.pump();
  await t.pump(const Duration(seconds: 1));
  await t.pump(const Duration(seconds: 1));
}

void main() {
  testWidgets('Accueil : assiduité, statistiques, prochaine séance et activité, sans débordement', (t) async {
    await _afficher(t, _auth());
    expect(find.text('Excellente assiduité'), findsOneWidget);
    expect(find.text('Présences'), findsOneWidget);
    expect(find.text('PROCHAINE SÉANCE'), findsOneWidget);
    expect(find.text('Bases de données relationnelles'), findsOneWidget);
    expect(find.text('Nouveau support'), findsWidgets);
    expect(tester(t), isNull);
  });

  testWidgets('Accueil sur un petit écran (320 px) : aucun débordement', (t) async {
    await _afficher(t, _auth(), largeur: 320);
    expect(find.text('Excellente assiduité'), findsOneWidget);
    expect(tester(t), isNull);
  });

  testWidgets('Accueil sans aucune donnée : états vides soignés', (t) async {
    await _afficher(t, _auth(vide: true));
    expect(find.text('Aucune présence enregistrée'), findsOneWidget);
    expect(find.text('Aucune séance à venir'), findsOneWidget);
    expect(find.text('Rien de nouveau'), findsOneWidget);
    expect(tester(t), isNull);
  });
}

/// Dernière exception de rendu (débordement, contrainte), `null` si tout s'est bien passé.
Object? tester(WidgetTester t) => t.takeException();
