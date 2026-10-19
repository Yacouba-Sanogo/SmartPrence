import 'package:flutter_test/flutter_test.dart';
import 'package:smartpresence_mobile/models/enseignement.dart';
import 'package:smartpresence_mobile/models/note.dart';
import 'package:smartpresence_mobile/models/seance.dart';

/// Les charges ci-dessous sont des réponses réelles du serveur, relevées pendant les
/// tests d'intégration du backend. Un nom de champ qui diverge ne se voit ni à la
/// compilation ni à l'analyse : seul ce test le révèle, avant qu'un écran ne reste vide.
void main() {
  test('résultats d’une évaluation : note sur le barème, absent, état de validation', () {
    final json = {
      'evaluation': {
        'id': 'c0c68701',
        'classeId': 1,
        'classeCode': 'EGN-H',
        'matiereId': 1,
        'matiereLibelle': 'Matiere EGN-H',
        'type': 'DEVOIR',
        'libelle': 'Devoir 1',
        'dateEvaluation': '2026-10-19',
        'bareme': 10,
        'coefficient': 1,
        'periode': 'SEMESTRE_1',
        'sujet': null,
        'effectif': 2,
        'notes': 1,
        'absents': 1,
        'brouillons': 1,
        'transmises': 0,
        'validees': 0,
      },
      'lignes': [
        {
          'etudiantId': 'a',
          'matricule': 'EGN-H-ET2',
          'nom': 'Coulibaly',
          'prenom': 'Bakary',
          'valeurSurBareme': null,
          'valeurSur20': null,
          'noteId': null,
          'statut': null,
          'motifRejet': null,
          'absent': true,
          'motifAbsence': 'Maladie',
        },
        {
          'etudiantId': 'b',
          'matricule': 'EGN-H-ET1',
          'nom': 'Keita',
          'prenom': 'Sira',
          'valeurSurBareme': 7.00,
          'valeurSur20': 14.00,
          'noteId': 'n1',
          'statut': 'TRANSMISE',
          'motifRejet': null,
          'absent': false,
          'motifAbsence': null,
        },
      ],
    };

    final r = ResultatsEvaluation.depuisJson(json);
    expect(r.evaluation.bareme, 10);
    expect(r.evaluation.restants, 0);
    expect(r.lignes[0].absent, isTrue);
    expect(r.lignes[0].verrouille, isFalse);
    expect(r.lignes[1].valeurSurBareme, 7.0);
    expect(r.lignes[1].statut, StatutNote.transmise);
    expect(r.lignes[1].verrouille, isTrue);
  });

  test('syllabus : avancement et libellé de matière repris du premier chapitre', () {
    final s = Syllabus.depuisJson({
      'classeId': 2,
      'matiereId': 2,
      'total': 2,
      'termines': 1,
      'chapitres': [
        {
          'id': 'f1',
          'classeId': 2,
          'matiereId': 2,
          'matiereLibelle': 'Algorithmique',
          'ordre': 1,
          'titre': 'Introduction',
          'objectifs': null,
          'termine': true,
          'termineLe': '2026-10-19',
        },
        {
          'id': 'f2',
          'classeId': 2,
          'matiereId': 2,
          'matiereLibelle': 'Algorithmique',
          'ordre': 2,
          'titre': 'Pointeurs',
          'objectifs': null,
          'termine': false,
          'termineLe': null,
        },
      ],
    });
    expect(s.matiereLibelle, 'Algorithmique');
    expect(s.avancement, 0.5);
    expect(s.chapitres.first.termineLe, isNotNull);
  });

  test('bilan et journée de pointage', () {
    final b = BilanEnseignement.depuisJson({
      'debut': '2026-10-17',
      'fin': '2026-10-20',
      'seancesPrevues': 3,
      'seancesEffectuees': 1,
      'seancesAnnulees': 0,
      'seancesADeclarer': 2,
      'minutesEnseignees': 150,
      'joursPointes': 2,
      'joursEnRetard': 1,
      'parMatiere': [
        {'matiereId': 1, 'matiereLibelle': 'Réseaux', 'effectuees': 1, 'minutes': 150},
      ],
    });
    expect(b.heures, '2 h 30');
    expect(b.aDeclarer, 2);
    expect(b.parMatiere.single.libelle, 'Réseaux');

    final j = JourneePointage.depuisJson({
      'personnelId': 'p',
      'date': '2026-10-19',
      'heureEntree': '08:12:05',
      'heureSortie': null,
      'statut': 'RETARD',
      'minutesRetard': 12,
      'minutesTravaillees': null,
      'minutesEcoulees': 95,
      'nombrePointages': 1,
      'present': true,
    });
    expect(j.entree, '08:12');
    expect(j.sortie, isNull);
    expect(j.retard, isTrue);
    expect(j.surPlace, isTrue);
  });

  test('séance déclarée : la date de déclaration est lue, une séance passée non déclarée est à régulariser', () {
    final declaree = Seance.depuisJson({
      'id': 's1',
      'classeId': 2,
      'classeCode': 'EGN-E',
      'matiereId': 2,
      'matiereLibelle': 'Algorithmique',
      'debut': '2020-01-01T08:00:00Z',
      'fin': '2020-01-01T10:00:00Z',
      'statut': 'TERMINEE',
      'declareeLe': '2020-01-01T10:05:00.123Z',
    });
    expect(declaree.declaree, isTrue);
    expect(declaree.aDeclarer, isFalse);

    final aRegulariser = Seance.depuisJson({
      'id': 's2',
      'matiereLibelle': 'Algorithmique',
      'classeCode': 'EGN-E',
      'debut': '2020-01-01T08:00:00Z',
      'fin': '2020-01-01T10:00:00Z',
      'statut': 'PLANIFIEE',
    });
    expect(aRegulariser.declaree, isFalse);
    expect(aRegulariser.aDeclarer, isTrue);
  });

  test('note : l’état de validation est lu, un brouillon seul est modifiable', () {
    final brouillon = Note.depuisJson({
      'id': 'n',
      'valeur': 14.0,
      'coefficient': 1,
      'type': 'DEVOIR',
      'libelle': 'Devoir 1',
      'dateEvaluation': '2026-10-19',
      'statut': 'BROUILLON',
      'motifRejet': 'Vérifier la copie 4',
    });
    expect(brouillon.modifiable, isTrue);
    expect(brouillon.motifRejet, 'Vérifier la copie 4');

    final transmise = Note.depuisJson({
      'id': 'n2',
      'valeur': 12.0,
      'coefficient': 1,
      'libelle': 'Devoir 2',
      'dateEvaluation': '2026-10-19',
      'statut': 'TRANSMISE',
    });
    expect(transmise.modifiable, isFalse);
  });
}
