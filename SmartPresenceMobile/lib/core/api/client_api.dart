import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'config_api.dart';
import 'erreurs.dart';

/// Client HTTP du backend SmartPresence.
///
/// Prend en charge trois choses que chaque écran n'a plus à refaire : l'ajout du jeton
/// JWT, le déballage de l'enveloppe uniforme `{success, message, data, timestamp}`, et
/// la traduction de toute panne en [ErreurApi] déjà rédigée pour l'utilisateur.
class ClientApi {
  ClientApi({http.Client? client, this.surSessionExpiree})
      : _client = client ?? http.Client();

  final http.Client _client;

  /// Appelé lorsqu'une réponse 401 arrive : permet de fermer la session en un point unique.
  final void Function()? surSessionExpiree;

  String? _jeton;

  void definirJeton(String? jeton) => _jeton = jeton;

  Map<String, String> get _entetes => {
        'Content-Type': 'application/json; charset=utf-8',
        'Accept': 'application/json',
        if (_jeton != null) 'Authorization': 'Bearer $_jeton',
      };

  Uri _uri(String chemin, [Map<String, dynamic>? parametres]) {
    final nettoyes = parametres?..removeWhere((_, valeur) => valeur == null);
    return Uri.parse('${ConfigApi.baseUrl}$chemin').replace(
      queryParameters: nettoyes?.map((cle, valeur) => MapEntry(cle, '$valeur')),
    );
  }

  Future<dynamic> get(String chemin, {Map<String, dynamic>? parametres}) {
    return _executer(() => _client.get(_uri(chemin, parametres), headers: _entetes));
  }

  Future<dynamic> post(String chemin, {Object? corps}) {
    return _executer(() => _client.post(
          _uri(chemin),
          headers: _entetes,
          body: corps == null ? null : jsonEncode(corps),
        ));
  }

  Future<dynamic> put(String chemin, {Object? corps}) {
    return _executer(() => _client.put(
          _uri(chemin),
          headers: _entetes,
          body: corps == null ? null : jsonEncode(corps),
        ));
  }

  Future<dynamic> delete(String chemin) {
    return _executer(() => _client.delete(_uri(chemin), headers: _entetes));
  }

  /// Exécute la requête et renvoie le contenu du champ `data`.
  Future<dynamic> _executer(Future<http.Response> Function() requete) async {
    final http.Response reponse;
    try {
      reponse = await requete().timeout(ConfigApi.delaiMax);
    } on TimeoutException {
      throw const ErreurApi(MessagesErreur.horsLigne);
    } catch (_) {
      // Panne réseau, DNS, socket : indistinguables pour l'utilisateur, et
      // sans intérêt de les distinguer — le geste attendu est le même.
      throw const ErreurApi(MessagesErreur.horsLigne);
    }

    // Le corps est décodé en UTF-8 explicitement : `response.body` suppose du Latin-1
    // en l'absence de charset, ce qui abîmerait les accents des messages français.
    final texte = utf8.decode(reponse.bodyBytes, allowMalformed: true);
    final Map<String, dynamic>? enveloppe = _decoder(texte);

    if (reponse.statusCode >= 200 && reponse.statusCode < 300) {
      return enveloppe?['data'];
    }

    if (reponse.statusCode == 401) {
      surSessionExpiree?.call();
    }

    final message = enveloppe?['message'] as String?;
    throw ErreurApi(
      message?.isNotEmpty == true
          ? message!
          : MessagesErreur.pourStatut(reponse.statusCode),
      statut: reponse.statusCode,
    );
  }

  Map<String, dynamic>? _decoder(String texte) {
    if (texte.isEmpty) return null;
    try {
      final decode = jsonDecode(texte);
      return decode is Map<String, dynamic> ? decode : null;
    } catch (_) {
      // Réponse non-JSON : page d'erreur d'un proxy, HTML de Tomcat…
      return null;
    }
  }

  void fermer() => _client.close();
}
