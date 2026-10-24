import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import 'cache_hors_ligne.dart';
import 'config_api.dart';
import 'erreurs.dart';

/// Client HTTP du backend SmartPresence.
///
/// Prend en charge trois choses que chaque écran n'a plus à refaire : l'ajout du jeton
/// JWT, le déballage de l'enveloppe uniforme `{success, message, data, timestamp}`, et
/// la traduction de toute panne en [ErreurApi] déjà rédigée pour l'utilisateur.
class ClientApi {
  ClientApi({http.Client? client, CacheHorsLigne? cache, this.surSessionExpiree})
      : _client = client ?? http.Client(),
        _cache = cache ?? CacheHorsLigne();

  final http.Client _client;
  final CacheHorsLigne _cache;

  /// `true` tant que la dernière requête a échoué faute de réseau : l'interface l'affiche.
  final ValueNotifier<bool> horsLigne = ValueNotifier<bool>(false);

  /// Date des données affichées quand elles viennent du cache, `null` sinon.
  DateTime? donneesDuCache;

  /// Oublie les données conservées pour l'accès hors ligne (déconnexion).
  Future<void> viderCache() => _cache.vider();

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

  /// Lit une ressource. Sans réseau, rend la dernière réponse connue plutôt qu'une erreur.
  Future<dynamic> get(String chemin, {Map<String, dynamic>? parametres}) async {
    final uri = _uri(chemin, parametres);
    final cle = uri.path + (uri.hasQuery ? '?${uri.query}' : '');
    try {
      final donnees = await _executer(() => _client.get(uri, headers: _entetes));
      donneesDuCache = null;
      horsLigne.value = false;
      unawaited(_cache.ecrire(cle, donnees));
      return donnees;
    } on ErreurApi catch (e) {
      if (!e.horsLigne) rethrow;
      horsLigne.value = true;
      final entree = await _cache.lire(cle);
      if (entree == null) rethrow;
      donneesDuCache = entree.enregistreLe;
      return entree.donnees;
    }
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

  Future<dynamic> patch(String chemin, {Object? corps}) {
    return _executer(() => _client.patch(
          _uri(chemin),
          headers: _entetes,
          body: corps == null ? null : jsonEncode(corps),
        ));
  }

  /// Envoie un fichier avec ses champs de formulaire.
  ///
  /// Le délai est plus large que celui des requêtes ordinaires : un PDF de plusieurs
  /// mégaoctets sur une connexion mobile ne tient pas dans quelques secondes.
  Future<dynamic> televerser(
    String chemin, {
    required Map<String, String> champs,
    required String champFichier,
    required List<int> octets,
    required String nomFichier,
  }) {
    return _executer(() async {
      final requete = http.MultipartRequest('POST', _uri(chemin))
        ..headers.addAll({
          'Accept': 'application/json',
          if (_jeton != null) 'Authorization': 'Bearer $_jeton',
        })
        ..fields.addAll(champs)
        ..files.add(http.MultipartFile.fromBytes(champFichier, octets, filename: nomFichier));
      return http.Response.fromStream(await _client.send(requete));
    }, delai: delaiTransfert);
  }

  /// Récupère le contenu brut d'un fichier. Les erreurs gardent le message du serveur.
  Future<List<int>> telechargerOctets(String chemin) async {
    final http.Response reponse;
    try {
      reponse = await _client
          .get(_uri(chemin), headers: {if (_jeton != null) 'Authorization': 'Bearer $_jeton'})
          .timeout(delaiTransfert);
    } catch (_) {
      throw const ErreurApi(MessagesErreur.horsLigne);
    }
    if (reponse.statusCode >= 200 && reponse.statusCode < 300) return reponse.bodyBytes;
    if (reponse.statusCode == 401) surSessionExpiree?.call();
    final enveloppe = _decoder(utf8.decode(reponse.bodyBytes, allowMalformed: true));
    final message = enveloppe?['message'] as String?;
    throw ErreurApi(
      message?.isNotEmpty == true ? message! : MessagesErreur.pourStatut(reponse.statusCode),
      statut: reponse.statusCode,
    );
  }

  static const Duration delaiTransfert = Duration(seconds: 90);

  Future<dynamic> delete(String chemin) {
    return _executer(() => _client.delete(_uri(chemin), headers: _entetes));
  }

  /// Exécute la requête et renvoie le contenu du champ `data`.
  Future<dynamic> _executer(Future<http.Response> Function() requete, {Duration? delai}) async {
    final http.Response reponse;
    try {
      reponse = await requete().timeout(delai ?? ConfigApi.delaiMax);
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
