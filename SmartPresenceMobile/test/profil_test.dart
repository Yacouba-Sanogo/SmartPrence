import 'package:flutter_test/flutter_test.dart';
import 'package:smartpresence_mobile/models/profil.dart';

void main() {
  test('le profil étudiant sait s\u2019il est responsable de classe et qui l\u2019est', () {
    final delegue = Profil.depuisJson({
      'typeProfil': 'ETUDIANT',
      'nom': 'Keita',
      'prenom': 'Sira',
      'responsableClasse': true,
      'responsableClasseNom': 'Sira Keita',
    });
    expect(delegue.responsableClasse, isTrue);

    final camarade = Profil.depuisJson({
      'typeProfil': 'ETUDIANT',
      'nom': 'Traore',
      'prenom': 'Moussa',
      'responsableClasse': false,
      'responsableClasseNom': 'Sira Keita',
    });
    expect(camarade.responsableClasse, isFalse);
    expect(camarade.responsableClasseNom, 'Sira Keita');
  });

  test('un serveur plus ancien, sans ces champs, ne casse pas le profil', () {
    final p = Profil.depuisJson({'typeProfil': 'ETUDIANT', 'nom': 'A', 'prenom': 'B'});
    expect(p.responsableClasse, isFalse);
    expect(p.responsableClasseNom, isNull);
  });
}
