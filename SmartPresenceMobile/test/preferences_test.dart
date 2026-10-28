import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:smartpresence_mobile/core/preferences_app.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  test('les réglages se conservent d’un lancement à l’autre', () async {
    final a = PreferencesApp();
    await a.definirTheme(ModeTheme.sombre);
    await a.definirEchelleTexte(1.3);
    await a.definirVibrations(false);
    await a.definirAnimationsReduites(true);

    final b = PreferencesApp();
    await b.charger();
    expect(b.theme, ModeTheme.sombre);
    expect(b.echelleTexte, 1.3);
    expect(b.vibrations, isFalse);
    expect(b.animationsReduites, isTrue);
  });

  test('par défaut : thème clair, texte normal, vibrations actives', () async {
    final p = PreferencesApp();
    await p.charger();
    expect(p.theme, ModeTheme.clair);
    expect(p.echelleTexte, 1.0);
    expect(p.vibrations, isTrue);
  });

  test('la photo est propre à chaque compte et disparaît quand on la retire', () async {
    final photo = PhotoProfil();
    await photo.pour('compte-1');
    await photo.definir(Uint8List.fromList([1, 2, 3]));
    expect(photo.octets, [1, 2, 3]);

    await photo.pour('compte-2');
    expect(photo.octets, isNull, reason: 'un autre compte ne voit pas la photo du précédent');

    await photo.pour('compte-1');
    expect(photo.octets, [1, 2, 3]);

    await photo.retirer();
    final autre = PhotoProfil();
    await autre.pour('compte-1');
    expect(autre.octets, isNull);
  });

  test('la matrice du mode sombre est son propre inverse (les photos retrouvent leurs couleurs)', () {
    List<double> appliquer(List<double> m, List<double> c) => [
          for (var i = 0; i < 3; i++) (m[i * 5] * c[0] + m[i * 5 + 1] * c[1] + m[i * 5 + 2] * c[2] + m[i * 5 + 4]).clamp(0, 255).toDouble(),
        ];

    // Un bleu moyen, sans saturation à l'aller : l'aller-retour doit retomber sur lui-même.
    const origine = [120.0, 150.0, 200.0];
    final retour = appliquer(matriceSombre, appliquer(matriceSombre, origine));
    for (var i = 0; i < 3; i++) {
      expect(retour[i], closeTo(origine[i], 2));
    }
    // Le blanc devient noir, le noir devient blanc.
    expect(appliquer(matriceSombre, [255, 255, 255]), everyElement(closeTo(0, 1)));
    expect(appliquer(matriceSombre, [0, 0, 0]), everyElement(closeTo(255, 1)));
  });

  testWidgets('ProtegeCouleurs ne touche à rien en mode clair', (t) async {
    await t.pumpWidget(const MaterialApp(home: ProtegeCouleurs(child: Text('x'))));
    expect(find.byType(ColorFiltered), findsNothing);
  });
}
