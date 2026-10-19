import 'dart:async';
import 'dart:js_interop';

import 'package:web/web.dart' as web;

import 'choix_fichier.dart';

/// Sélecteur du navigateur : un champ `<input type="file">` créé à la volée.
///
/// Écrit à la main plutôt que par un greffon : le greffon de sélection de fichiers ne
/// s'enregistrait pas dans ce navigateur, et le bouton restait muet.
Future<FichierChoisi?> choisirFichier(List<String> extensions) {
  final fin = Completer<FichierChoisi?>();
  final champ = web.HTMLInputElement()
    ..type = 'file'
    ..accept = extensions.map((e) => '.$e').join(',');

  champ.onchange = (web.Event _) {
    final fichier = champ.files?.item(0);
    if (fichier == null) {
      if (!fin.isCompleted) fin.complete(null);
      return;
    }
    fichier.arrayBuffer().toDart.then((tampon) {
      if (!fin.isCompleted) {
        fin.complete(FichierChoisi(fichier.name, tampon.toDart.asUint8List()));
      }
    }, onError: (Object e) {
      if (!fin.isCompleted) fin.completeError(e);
    });
  }.toJS;

  // Fermer la boîte sans choisir ne déclenche pas « change » : sans cet écouteur, l'attente
  // ne se terminerait jamais.
  champ.oncancel = (web.Event _) {
    if (!fin.isCompleted) fin.complete(null);
  }.toJS;

  champ.click();
  return fin.future;
}
