import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:smartpresence_mobile/core/api/client_api.dart';
import 'package:smartpresence_mobile/core/api/erreurs.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  test('sans réseau, une lecture déjà faite revient du cache et le signale', () async {
    var reseau = true;
    final api = ClientApi(
      client: MockClient((_) async {
        if (!reseau) throw http.ClientException('coupé');
        return http.Response.bytes(utf8.encode(jsonEncode({'data': {'classe': 'L3-INFO'}})), 200);
      }),
    );

    expect(await api.get('/moi/classes'), {'classe': 'L3-INFO'});
    expect(api.horsLigne.value, isFalse);

    reseau = false;
    expect(await api.get('/moi/classes'), {'classe': 'L3-INFO'});
    expect(api.horsLigne.value, isTrue);
    expect(api.donneesDuCache, isNotNull);

    reseau = true;
    await api.get('/moi/classes');
    expect(api.horsLigne.value, isFalse);
  });

  test('sans réseau ni cache, l’erreur reste celle du réseau', () async {
    final api = ClientApi(client: MockClient((_) async => throw http.ClientException('coupé')));
    await expectLater(api.get('/moi/notes'), throwsA(isA<ErreurApi>().having((e) => e.horsLigne, 'horsLigne', true)));
  });

  test('une écriture hors ligne échoue au lieu d’être rejouée', () async {
    final api = ClientApi(client: MockClient((_) async => throw http.ClientException('coupé')));
    await expectLater(api.post('/moi/annonces'), throwsA(isA<ErreurApi>()));
  });

  test('les paramètres de requête ont chacun leur entrée de cache', () async {
    var reseau = true;
    final api = ClientApi(
      client: MockClient((r) async {
        if (!reseau) throw http.ClientException('coupé');
        return http.Response.bytes(utf8.encode(jsonEncode({'data': r.url.queryParameters['mois']})), 200);
      }),
    );
    await api.get('/moi/seances', parametres: {'mois': '1'});
    await api.get('/moi/seances', parametres: {'mois': '2'});
    reseau = false;
    expect(await api.get('/moi/seances', parametres: {'mois': '1'}), '1');
    expect(await api.get('/moi/seances', parametres: {'mois': '2'}), '2');
  });

  test('vider le cache retire tout après déconnexion', () async {
    var reseau = true;
    final api = ClientApi(
      client: MockClient((_) async {
        if (!reseau) throw http.ClientException('coupé');
        return http.Response.bytes(utf8.encode(jsonEncode({'data': 'secret'})), 200);
      }),
    );
    await api.get('/moi/profil');
    await api.viderCache();
    reseau = false;
    await expectLater(api.get('/moi/profil'), throwsA(isA<ErreurApi>()));
  });
}
