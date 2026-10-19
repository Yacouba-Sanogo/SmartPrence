import 'choix_fichier_mobile.dart' if (dart.library.js_interop) 'choix_fichier_web.dart' as impl;

/// Fichier retenu par l'utilisateur : son nom et son contenu, déjà lus.
class FichierChoisi {
  const FichierChoisi(this.nom, this.octets);

  final String nom;
  final List<int> octets;
}

/// Ouvre le sélecteur de fichiers de la plateforme.
///
/// Renvoie `null` si l'utilisateur renonce. Le navigateur et le téléphone n'ont pas le même
/// sélecteur : le premier passe par un champ de formulaire natif, le second par le
/// sélecteur du système. Les deux sont isolés ici pour que l'écran n'en sache rien.
Future<FichierChoisi?> choisirFichier(List<String> extensions) => impl.choisirFichier(extensions);
