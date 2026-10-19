import 'package:file_selector/file_selector.dart';

import 'choix_fichier.dart';

Future<FichierChoisi?> choisirFichier(List<String> extensions) async {
  final choisi = await openFile(acceptedTypeGroups: [
    XTypeGroup(label: 'Documents', extensions: extensions),
  ]);
  if (choisi == null) return null;
  return FichierChoisi(choisi.name, await choisi.readAsBytes());
}
