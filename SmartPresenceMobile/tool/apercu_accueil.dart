// Aperçu de l'espace étudiant avec des données de démonstration, sans serveur.
//
// À compiler pour le web : flutter build web --release -t tool/apercu_accueil.dart
// Les dates sont calculées à partir d'aujourd'hui : la semaine en cours est toujours remplie.
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:smartpresence_mobile/core/api/client_api.dart';
import 'package:smartpresence_mobile/core/auth/auth_service.dart';
import 'package:smartpresence_mobile/core/design/theme.dart';
import 'package:smartpresence_mobile/core/preferences_app.dart';
import 'package:smartpresence_mobile/screens/bibliotheque_screen.dart';
import 'package:smartpresence_mobile/screens/connexion_screen.dart';
import 'package:smartpresence_mobile/screens/etudiant/shell_etudiant.dart';
import 'package:smartpresence_mobile/screens/landing_screen.dart';

String _jour(DateTime d) =>
    '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

Object? _donnees(String chemin) {
  final maintenant = DateTime.now();
  final lundi = maintenant.subtract(Duration(days: maintenant.weekday - 1));

  if (chemin.endsWith('/auth/login')) {
    return {'accessToken': 'demo', 'refreshToken': 'demo', 'expiresInMs': 3600000, 'email': 'moussa@gmail.com'};
  }
  if (chemin.endsWith('/moi/profil')) {
    return {
      'utilisateurId': 'u1',
      'email': 'moussa@gmail.com',
      'nom': 'TRAORE',
      'prenom': 'Moussa',
      'roles': ['ROLE_ETUDIANT'],
      'typeProfil': 'ETUDIANT',
      'profilId': 'e1',
      'matricule': 'ETU-2026-002',
      'classeId': 1,
      'classeCode': 'L1-INFO-A',
      'classeLibelle': 'Licence 1 Informatique',
    };
  }
  if (chemin.endsWith('/moi/statistiques')) {
    return {
      'totalPresences': 20,
      'totalPresents': 17,
      'totalRetards': 2,
      'totalAbsents': 1,
      'totalJustifies': 0,
      'tauxAssiduite': 92.5,
      'classeCode': 'L1-INFO-A',
    };
  }
  if (chemin.endsWith('/moi/presences')) {
    const matieres = ['Algorithmique', 'Analyse mathématique', 'Architecture des ordinateurs', 'Anglais technique', 'Bases de données'];
    final contenu = <Map<String, Object?>>[];
    for (var i = 0; i < 5; i++) {
      final jour = lundi.add(Duration(days: i));
      if (jour.isAfter(maintenant)) break;
      contenu.add({
        'id': 'p$i',
        'datePresence': _jour(jour),
        'heurePresence': i == 2 ? '08:17:00' : '07:56:00',
        'statut': i == 2 ? 'RETARD' : 'PRESENT',
        'matiereLibelle': matieres[i],
        'salleNom': i.isEven ? 'Amphi A' : 'Salle 204',
        'source': 'CAPTEUR',
      });
    }
    return {'content': contenu.reversed.toList(), 'page': 0, 'size': 20, 'totalElements': contenu.length, 'last': true};
  }
  if (chemin.endsWith('/moi/emploi-du-temps')) {
    const cours = [
      ['Bases de données relationnelles', 'Salle 204', 2, 0],
      ['Algorithmique avancée', 'Amphi A', 1, 1],
      ['Analyse mathématique', 'Amphi A', 3, 1],
      ['Architecture des ordinateurs', 'Salle 101', 2, 2],
      ['Anglais technique', 'Salle 204', 1, 3],
      ['Réseaux et protocoles', 'Salle 305', 2, 4],
    ];
    final jourBase = DateTime(maintenant.year, maintenant.month, maintenant.day);
    final seances = <Map<String, Object?>>[];
    for (var i = 0; i < cours.length; i++) {
      final c = cours[i];
      final debut = i == 0
          ? maintenant.add(const Duration(hours: 2, minutes: 15))
          : jourBase.add(Duration(days: c[3] as int, hours: 8 + (i % 3) * 2));
      seances.add({
        'id': 's$i',
        'matiereLibelle': c[0],
        'classeCode': 'L1-INFO-A',
        'debut': debut.toUtc().toIso8601String(),
        'fin': debut.add(Duration(hours: c[2] as int)).toUtc().toIso8601String(),
        'statut': 'PLANIFIEE',
        'salleLibelle': c[1],
      });
    }
    return seances;
  }
  if (chemin.endsWith('/moi/bulletin')) {
    Map<String, Object?> matiere(int id, String libelle, double moyenne, List<List<Object>> notes) => {
          'matiereId': id,
          'matiereCode': libelle.substring(0, 4).toUpperCase(),
          'matiereLibelle': libelle,
          'coefficient': 3,
          'moyenne': moyenne,
          'notes': [
            for (final n in notes)
              {
                'id': 'n$id${n[0]}',
                'valeur': n[1],
                'coefficient': 1,
                'type': n[2],
                'libelle': n[0],
                'dateEvaluation': _jour(maintenant.subtract(Duration(days: 20 + id))),
                'matiereId': id,
                'matiereLibelle': libelle,
                'statut': 'VALIDEE',
              },
          ],
        };
    return {
      'nombreNotes': 7,
      'moyenneGenerale': 14.2,
      'classeLibelle': 'Licence 1 Informatique',
      'matieres': [
        matiere(1, 'Algorithmique', 16.0, [['Devoir 1', 15.5, 'DEVOIR'], ['Examen', 16.5, 'EXAMEN']]),
        matiere(2, 'Analyse mathématique', 12.5, [['Devoir 1', 11.0, 'DEVOIR'], ['Examen', 14.0, 'EXAMEN']]),
        matiere(3, 'Bases de données', 14.0, [['TP noté', 14.0, 'TP']]),
        matiere(4, 'Anglais technique', 9.0, [['Devoir 1', 9.0, 'DEVOIR'], ['Oral', 9.0, 'ORAL']]),
      ],
    };
  }
  if (chemin.endsWith('/moi/releve')) return null;
  if (chemin.endsWith('/notifications')) {
    return [
      {
        'id': 'n1',
        'type': 'CHANGEMENT',
        'titre': 'Séance déplacée',
        'message': 'Bases de données passe en salle 204 à 10 h 15.',
        'lue': false,
        'createdAt': maintenant.subtract(const Duration(minutes: 40)).toUtc().toIso8601String(),
      },
      {
        'id': 'n2',
        'type': 'INFORMATION',
        'titre': 'Nouveau support — Algorithmique',
        'message': 'Chapitre 3 — Les piles et les files',
        'lue': false,
        'createdAt': maintenant.subtract(const Duration(hours: 5)).toUtc().toIso8601String(),
      },
      {
        'id': 'n3',
        'type': 'ANNONCE',
        'titre': 'Calendrier des examens',
        'message': 'Le calendrier du premier semestre est publié.',
        'lue': true,
        'createdAt': maintenant.subtract(const Duration(days: 2)).toUtc().toIso8601String(),
      },
    ];
  }
  if (chemin.endsWith('/bibliotheque/documents')) {
    return [
      {'id': 'd1', 'type': 'MEMOIRE', 'titre': 'Sécurité des réseaux sans fil', 'auteur': 'Sow Modibo', 'filiereLibelle': 'Informatique', 'annee': 2024, 'nomFichier': 'memoire.pdf', 'taille': 2097152, 'statut': 'PUBLIE', 'deposePar': 'Administration'},
      {'id': 'd2', 'type': 'SUJET', 'titre': 'Examen d’algorithmique — session 1', 'matiere': 'Algorithmique', 'annee': 2023, 'nomFichier': 'examen.pdf', 'taille': 120000, 'statut': 'PUBLIE', 'deposePar': 'Administration'},
      {'id': 'd3', 'type': 'MEMOIRE', 'titre': 'Système de gestion de présence biométrique', 'auteur': 'Keita Sira', 'filiereLibelle': 'Informatique', 'annee': 2025, 'nomFichier': 'memoire2.pdf', 'taille': 3145728, 'statut': 'PUBLIE', 'deposePar': 'Administration'},
    ];
  }
  if (chemin.endsWith('/bibliotheque/livres')) {
    return [
      {'id': 'l1', 'titre': 'Introduction aux bases de données', 'auteur': 'C. J. Date', 'categorie': 'Informatique', 'emplacement': 'Rayon B2', 'exemplaires': 3, 'disponibles': 2},
      {'id': 'l2', 'titre': 'Algorithmique et structures de données', 'auteur': 'T. Cormen', 'categorie': 'Informatique', 'emplacement': 'Rayon A1', 'exemplaires': 1, 'disponibles': 0},
    ];
  }
  if (chemin.endsWith('/bibliotheque/mes-emprunts')) {
    final prevu = maintenant.add(const Duration(days: 2));
    return [
      {'id': 'e1', 'livreTitre': 'Algorithmique et structures de données', 'livreAuteur': 'T. Cormen', 'emprunteLe': _jour(maintenant.subtract(const Duration(days: 12))), 'retourPrevu': _jour(prevu), 'statut': 'EN_COURS', 'joursDeRetard': 0},
    ];
  }
  return [];
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final client = MockClient((requete) async {
    final chemin = requete.url.path;
    return http.Response.bytes(
      utf8.encode(jsonEncode({'success': true, 'data': _donnees(chemin)})),
      200,
      headers: {'content-type': 'application/json; charset=utf-8'},
    );
  });
  final auth = AuthService(client: ClientApi(client: client));
  await auth.connecter('moussa@gmail.com', 'demo');
  // ?ecran=accueil (défaut) | connexion | landing | bibliotheque
  final ecran = Uri.base.queryParameters['ecran'] ?? 'accueil';
  final Widget page = switch (ecran) {
    'connexion' => ConnexionScreen(auth: AuthService(client: ClientApi(client: client)), surRetour: () {}),
    'landing' => LandingScreen(auth: auth, surCommencer: () {}),
    'bibliotheque' => BibliothequeScreen(auth: auth),
    'cours' => ShellEtudiant(auth: auth, ongletInitial: OngletsEtudiant.emploiDuTemps),
    'releves' => ShellEtudiant(auth: auth, ongletInitial: OngletsEtudiant.releves),
    'notes' => ShellEtudiant(auth: auth, ongletInitial: OngletsEtudiant.notes),
    'profil' => ShellEtudiant(auth: auth, ongletInitial: OngletsEtudiant.profil),
    'empreinte' => ShellEtudiant(auth: auth, ongletInitial: OngletsEtudiant.empreinte),
    _ => ShellEtudiant(auth: auth),
  };
  final prefs = PreferencesApp();
  final photo = PhotoProfil();
  final sombre = Uri.base.queryParameters['sombre'] == '1';
  final texte = double.tryParse(Uri.base.queryParameters['texte'] ?? '') ?? 1.0;
  await prefs.definirTheme(sombre ? ModeTheme.sombre : ModeTheme.clair);
  await prefs.definirEchelleTexte(texte);
  runApp(MaterialApp(
    debugShowCheckedModeBanner: false,
    theme: themeSmartPresence(),
    builder: (context, child) {
      Widget contenu = child ?? const SizedBox.shrink();
      if (sombre) contenu = ColorFiltered(colorFilter: const ColorFilter.matrix(matriceSombre), child: contenu);
      return ContexteApp(
        preferences: prefs,
        photo: photo,
        sombre: sombre,
        child: MediaQuery(data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(texte)), child: contenu),
      );
    },
    home: page,
  ));
}
