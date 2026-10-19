import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:smartpresence_mobile/core/api/client_api.dart';
import 'package:smartpresence_mobile/core/auth/auth_service.dart';
import 'package:smartpresence_mobile/widgets/entete.dart';
import 'package:smartpresence_mobile/models/seance.dart';
import 'package:smartpresence_mobile/screens/enseignant/activite_screen.dart';
import 'package:smartpresence_mobile/screens/enseignant/annonces_screen.dart';
import 'package:smartpresence_mobile/screens/enseignant/conges_screen.dart';
import 'package:smartpresence_mobile/screens/enseignant/reclamations_screen.dart';
import 'package:smartpresence_mobile/screens/enseignant/supports_screen.dart';
import 'package:smartpresence_mobile/screens/etudiant/contestations_screen.dart';
import 'package:smartpresence_mobile/screens/etudiant/supports_screen.dart';
import 'package:smartpresence_mobile/screens/enseignant/evaluations_screen.dart';
import 'package:smartpresence_mobile/screens/enseignant/programme_screen.dart';
import 'package:smartpresence_mobile/screens/enseignant/resultats_screen.dart';

/// Rend les écrans ajoutés à l'espace enseignant avec des réponses du serveur.
///
/// Une animation mal câblée ou un débordement de mise en page ne se voit pas à
/// l'analyse : Flutter ne le signale qu'au rendu, par une exception.
AuthService _auth(Map<String, Object?> routes) {
  final client = MockClient((requete) async {
    final chemin = requete.url.path.replaceFirst('/api', '');
    for (final entree in routes.entries) {
      if (chemin.endsWith(entree.key)) {
        return http.Response.bytes(
          utf8.encode(jsonEncode({'success': true, 'data': entree.value})),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      }
    }
    return http.Response(jsonEncode({'success': true, 'data': []}), 200);
  });
  return AuthService(client: ClientApi(client: client));
}

Future<void> _afficher(WidgetTester t, Widget ecran) async {
  t.view.physicalSize = const Size(390 * 3, 844 * 3);
  t.view.devicePixelRatio = 3;
  addTearDown(t.view.reset);
  await t.pumpWidget(MaterialApp(home: ecran));
  await t.pump();
  await t.pump(const Duration(seconds: 1));
  await t.pump(const Duration(seconds: 1));
  // L'en-tête dégradé doit couvrir toute la largeur : un titre court le faisait rétrécir
  // à la taille de son texte, sans qu'aucune exception ne le signale.
  for (final entete in find.byType(EnteteDegrade).evaluate()) {
    expect(t.getSize(find.byWidget(entete.widget)).width, 390);
  }
}

const _evaluation = {
  'id': 'e1',
  'classeId': 1,
  'classeCode': 'L1-INFO',
  'matiereId': 1,
  'matiereLibelle': 'Algorithmique',
  'type': 'DEVOIR',
  'libelle': 'Devoir 1',
  'dateEvaluation': '2026-10-19',
  'bareme': 10,
  'coefficient': 2,
  'periode': 'SEMESTRE_1',
  'sujet': 'Piles et files',
  'effectif': 2,
  'notes': 1,
  'absents': 1,
  'brouillons': 1,
  'transmises': 0,
  'validees': 0,
};

void main() {
  testWidgets('Activité : pointage du jour, anneau du bilan, heures par matière', (t) async {
    final aujourdhui = DateTime.now().toIso8601String().substring(0, 10);
    await _afficher(
      t,
      ActiviteScreen(
        auth: _auth({
          '/moi/pointages': [
            {
              'date': aujourdhui,
              'heureEntree': '08:12:05',
              'heureSortie': null,
              'statut': 'RETARD',
              'minutesRetard': 12,
              'present': true,
            },
          ],
          '/moi/bilan': {
            'seancesPrevues': 4,
            'seancesEffectuees': 2,
            'seancesAnnulees': 1,
            'seancesADeclarer': 1,
            'minutesEnseignees': 240,
            'joursPointes': 5,
            'joursEnRetard': 1,
            'parMatiere': [
              {'matiereId': 1, 'matiereLibelle': 'Algorithmique', 'effectuees': 2, 'minutes': 240},
            ],
          },
        }),
      ),
    );
    expect(find.text('Mon activité'), findsOneWidget);
    expect(find.textContaining('Arrivé à 08:12'), findsOneWidget);
    expect(find.text('Algorithmique'), findsOneWidget);
    expect(find.textContaining('1 séance passée attend'), findsOneWidget);
    expect(t.takeException(), isNull);
  });

  testWidgets('Programme : avancement des chapitres', (t) async {
    await _afficher(
      t,
      ProgrammeScreen(
        auth: _auth({
          '/classes/1/chapitres': [
            {
              'classeId': 1,
              'matiereId': 1,
              'total': 2,
              'termines': 1,
              'chapitres': [
                {
                  'id': 'c1',
                  'matiereLibelle': 'Algorithmique',
                  'ordre': 1,
                  'titre': 'Introduction',
                  'termine': true,
                  'termineLe': '2026-10-19',
                },
                {
                  'id': 'c2',
                  'matiereLibelle': 'Algorithmique',
                  'ordre': 2,
                  'titre': 'Pointeurs',
                  'termine': false,
                },
              ],
            },
          ],
        }),
        classe: const ClasseEnseignee(id: 1, code: 'L1-INFO', libelle: 'Licence 1'),
      ),
    );
    expect(find.text('1/2'), findsOneWidget);
    expect(find.text('1. Introduction'), findsOneWidget);
    expect(t.takeException(), isNull);
  });

  testWidgets('Évaluations : liste avec barème et progression de saisie', (t) async {
    await _afficher(
      t,
      EvaluationsScreen(
        auth: _auth({'/moi/classes/1/evaluations': [_evaluation]}),
        classe: const ClasseEnseignee(id: 1, code: 'L1-INFO', libelle: 'Licence 1'),
      ),
    );
    expect(find.text('Devoir 1'), findsOneWidget);
    expect(find.textContaining('sur 10'), findsOneWidget);
    expect(find.text('Planifier'), findsOneWidget);
    expect(t.takeException(), isNull);
  });

  testWidgets('Résultats : note sur barème, absent, ligne transmise verrouillée', (t) async {
    await _afficher(
      t,
      ResultatsScreen(
        auth: _auth({
          '/moi/evaluations/e1/resultats': {
            'evaluation': _evaluation,
            'lignes': [
              {
                'etudiantId': 'a',
                'matricule': 'M1',
                'nom': 'Coulibaly',
                'prenom': 'Bakary',
                'absent': true,
                'motifAbsence': 'Maladie',
              },
              {
                'etudiantId': 'b',
                'matricule': 'M2',
                'nom': 'Keita',
                'prenom': 'Sira',
                'valeurSurBareme': 7.0,
                'valeurSur20': 14.0,
                'noteId': 'n1',
                'statut': 'BROUILLON',
                'absent': false,
              },
              {
                'etudiantId': 'c',
                'matricule': 'M3',
                'nom': 'Sow',
                'prenom': 'Modibo',
                'valeurSurBareme': 5.0,
                'valeurSur20': 10.0,
                'noteId': 'n2',
                'statut': 'TRANSMISE',
                'absent': false,
              },
            ],
          },
        }),
        evaluationId: 'e1',
      ),
    );
    expect(find.text('Sira Keita'), findsOneWidget);
    expect(find.text('Transmise'), findsOneWidget);
    expect(find.text('ABS'), findsOneWidget);
    expect(find.text('3 / 3'), findsOneWidget);
    expect(find.text('Enregistrer'), findsOneWidget);

    // Taper hors barème signale la ligne sans lever d'exception.
    await t.enterText(find.byType(TextField).first, '12');
    await t.pump(const Duration(milliseconds: 400));
    expect(t.takeException(), isNull);
  });

  testWidgets('Annonces : non lues en avant, lecture au toucher, filtre et « tout lire »', (t) async {
    final maintenant = DateTime.now().toUtc().toIso8601String();
    final hier = DateTime.now().subtract(const Duration(days: 1)).toUtc().toIso8601String();
    await _afficher(
      t,
      AnnoncesScreen(
        auth: _auth({
          '/notifications': [
            {
              'id': 'n1',
              'destinataireId': 'u',
              'type': 'CHANGEMENT',
              'titre': 'Séance modifiée — Réseaux',
              'message': 'L1-INFO · Salle : Amphi D',
              'lue': false,
              'lueLe': null,
              'createdAt': maintenant,
            },
            {
              'id': 'n2',
              'destinataireId': 'u',
              'type': 'ANNONCE',
              'titre': 'Réunion pédagogique',
              'message': 'Jeudi 10h, salle des profs',
              'lue': true,
              'lueLe': hier,
              'createdAt': hier,
            },
          ],
        }),
      ),
    );
    expect(find.text('1 non lue'), findsOneWidget);
    expect(find.text('Séance modifiée — Réseaux'), findsOneWidget);
    expect(find.text("AUJOURD'HUI"), findsOneWidget);
    expect(find.text('HIER'), findsOneWidget);

    await t.tap(find.text('Séance modifiée — Réseaux'));
    await t.pump(const Duration(milliseconds: 600));
    expect(find.text('Tout est lu'), findsOneWidget);

    await t.tap(find.text('Non lues'));
    await t.pump(const Duration(milliseconds: 600));
    expect(find.text('Vous êtes à jour : aucune annonce en attente de lecture.'), findsOneWidget);
    expect(t.takeException(), isNull);
  });

  testWidgets('Congés : statut, séances touchées, réponse, retrait d\u2019une demande en attente', (t) async {
    await _afficher(
      t,
      CongesScreen(
        auth: _auth({
          '/moi/conges': [
            {
              'id': 'd1',
              'type': 'CONGE',
              'debut': '2026-11-02',
              'fin': '2026-11-06',
              'motif': 'Congé annuel',
              'statut': 'EN_ATTENTE',
              'reponse': null,
              'seancesConcernees': 3,
            },
            {
              'id': 'd2',
              'type': 'FORMATION',
              'debut': '2026-10-01',
              'fin': '2026-10-01',
              'motif': 'Stage',
              'statut': 'REFUSEE',
              'reponse': 'Période d\u2019examens',
              'seancesConcernees': 0,
            },
          ],
        }),
      ),
    );
    expect(find.text('1 en attente'), findsOneWidget);
    expect(find.text('3 séances sur cette période'), findsOneWidget);
    expect(find.textContaining('Période d\u2019examens'), findsOneWidget);
    expect(find.text('Retirer'), findsOneWidget);
    expect(find.text('Nouvelle demande'), findsOneWidget);
    expect(t.takeException(), isNull);
  });

  testWidgets('Réclamations : ouvertes en tête, ancienne valeur barrée après correction', (t) async {
    await _afficher(
      t,
      ReclamationsScreen(
        auth: _auth({
          '/moi/reclamations/recues': [
            {
              'id': 'r1',
              'noteId': 'n1',
              'matiereLibelle': 'Réseaux',
              'libelleNote': 'Devoir 1',
              'valeurActuelle': 8.0,
              'etudiantNom': 'Sira Keita',
              'etudiantMatricule': 'M1',
              'motif': 'Question 3 non comptée',
              'statut': 'OUVERTE',
              'creeLe': DateTime.now().toUtc().toIso8601String(),
            },
            {
              'id': 'r2',
              'noteId': 'n2',
              'matiereLibelle': 'Réseaux',
              'libelleNote': 'Devoir 2',
              'valeurActuelle': 11.0,
              'etudiantNom': 'Bakary Coulibaly',
              'etudiantMatricule': 'M2',
              'motif': 'Trop sévère',
              'statut': 'ACCEPTEE',
              'reponse': 'Vous avez raison',
              'valeurAvant': 8.0,
              'valeurApres': 11.0,
              'creeLe': DateTime.now().toUtc().toIso8601String(),
            },
          ],
        }),
      ),
    );
    expect(find.text('1 à traiter'), findsOneWidget);
    expect(find.text('Toucher pour répondre'), findsOneWidget);
    expect(find.textContaining('Vous avez raison'), findsOneWidget);

    await t.tap(find.text('Sira Keita'));
    await t.pump(const Duration(milliseconds: 600));
    expect(find.text('Accepter et répondre'), findsOneWidget);
    await t.ensureVisible(find.text('Rejeter'));
    await t.pump(const Duration(milliseconds: 600));
    await t.tap(find.text('Rejeter'));
    await t.pump(const Duration(milliseconds: 600));
    expect(find.text('Rejeter et répondre'), findsOneWidget);
    expect(t.takeException(), isNull);
  });

  testWidgets('Contestations étudiant : réponse de l\u2019enseignant et note corrigée', (t) async {
    await _afficher(
      t,
      ContestationsScreen(
        auth: _auth({
          '/moi/reclamations': [
            {
              'id': 'r1',
              'noteId': 'n1',
              'matiereLibelle': 'Réseaux',
              'libelleNote': 'Devoir 1',
              'valeurActuelle': 11.0,
              'etudiantNom': 'Sira Keita',
              'etudiantMatricule': 'M1',
              'motif': 'Question 3 non comptée',
              'statut': 'ACCEPTEE',
              'reponse': 'Vous avez raison',
              'valeurAvant': 8.0,
              'valeurApres': 11.0,
              'creeLe': DateTime.now().toUtc().toIso8601String(),
            },
          ],
        }),
      ),
    );
    expect(find.text('Acceptée'), findsOneWidget);
    expect(find.text('Note corrigée : 8 → 11 / 20'), findsOneWidget);
    expect(find.text('Contester une note'), findsOneWidget);
    expect(t.takeException(), isNull);
  });

  const supports = [
    {
      'id': 'aaaaaaaa-1111-2222-3333-444444444444',
      'classeId': 1,
      'classeCode': 'L1-INFO',
      'matiereId': 1,
      'matiereLibelle': 'Algorithmique',
      'enseignantNom': 'Awa Diallo',
      'titre': 'Chapitre 1 — Introduction',
      'description': 'À lire avant jeudi',
      'nomFichier': 'chapitre1.pdf',
      'typeMime': 'application/pdf',
      'taille': 1500000,
      'publieLe': '2026-10-19T08:00:00Z',
    },
    {
      'id': 'bbbbbbbb-1111-2222-3333-444444444444',
      'classeId': 1,
      'classeCode': 'L1-INFO',
      'matiereId': 2,
      'matiereLibelle': 'Réseaux',
      'enseignantNom': 'Modibo Sow',
      'titre': 'TP 1',
      'description': null,
      'nomFichier': 'tp1.docx',
      'typeMime': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'taille': 340000,
      'publieLe': '2026-10-18T08:00:00Z',
    },
  ];

  testWidgets('Supports enseignant : liste, format, taille et publication', (t) async {
    await _afficher(
      t,
      SupportsScreen(
        auth: _auth({'/supports': supports}),
        classe: const ClasseEnseignee(id: 1, code: 'L1-INFO', libelle: 'Licence 1'),
      ),
    );
    expect(find.text('Chapitre 1 — Introduction'), findsOneWidget);
    expect(find.textContaining('PDF · 1,4 Mo'), findsOneWidget);
    expect(find.textContaining('DOCX · 332 Ko'), findsOneWidget);
    expect(find.text('Publier'), findsOneWidget);
    expect(t.takeException(), isNull);
  });

  testWidgets('Supports étudiant : regroupés par matière', (t) async {
    await _afficher(t, SupportsEtudiantScreen(auth: _auth({'/moi/supports': supports})));
    expect(find.text('ALGORITHMIQUE'), findsOneWidget);
    expect(find.text('RÉSEAUX'), findsOneWidget);
    expect(find.text('2 documents pour votre classe'), findsOneWidget);
    expect(find.textContaining('Awa Diallo'), findsOneWidget);
    expect(t.takeException(), isNull);
  });
}
