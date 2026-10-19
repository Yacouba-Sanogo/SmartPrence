import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

import '../core/api/client_api.dart';
import '../core/api/erreurs.dart';
import '../models/gestion.dart';


/// Télécharge un support puis l'ouvre avec l'application du téléphone.
///
/// Le fichier passe par le dossier temporaire : l'application n'a besoin d'aucune
/// permission de stockage, et le système le nettoie seul. Le téléchargement exige le
/// jeton de l'utilisateur, d'où l'impossibilité de simplement ouvrir une adresse.
Future<void> ouvrirSupport(ClientApi client, SupportCours support) async {
  if (kIsWeb) {
    throw const ErreurApi('Le téléchargement est disponible dans l’application mobile.');
  }
  final octets = await client.telechargerOctets('/moi/supports/${support.id}/fichier');
  final dossier = await getTemporaryDirectory();
  // L'identifiant en préfixe évite qu'un fichier du même nom n'en écrase un autre.
  final chemin = '${dossier.path}/${support.id.substring(0, 8)}_${support.nomFichier}';
  await File(chemin).writeAsBytes(octets, flush: true);
  final resultat = await OpenFilex.open(chemin);
  if (resultat.type != ResultType.done) {
    throw ErreurApi('Aucune application ne peut ouvrir ce fichier (.${support.extension}).');
  }
}
