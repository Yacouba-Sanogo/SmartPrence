/// Erreur remontée par la couche réseau, déjà formulée pour l'utilisateur.
///
/// Le backend renvoie ses erreurs métier dans le champ `message` de son enveloppe
/// uniforme ; c'est ce message qui est privilégié, parce qu'il est précis et rédigé en
/// français. Les libellés génériques ne servent que de repli.
class ErreurApi implements Exception {
  const ErreurApi(this.message, {this.statut});

  final String message;
  final int? statut;

  /// `true` si l'utilisateur n'est plus authentifié : la session doit être fermée.
  bool get sessionExpiree => statut == 401;

  /// `true` si l'utilisateur est authentifié mais n'a pas les droits.
  bool get accesRefuse => statut == 403;

  /// Pas de réseau, serveur injoignable, DNS en échec.
  bool get horsLigne => statut == null;

  @override
  String toString() => message;
}

/// Messages de repli, employés quand le serveur n'en fournit aucun.
class MessagesErreur {
  MessagesErreur._();

  static const String horsLigne =
      "Serveur injoignable. Vérifiez votre connexion, puis réessayez.";
  static const String identifiants = 'Email ou mot de passe incorrect.';
  static const String accesRefuse = "Vous n'avez pas accès à cette information.";
  static const String introuvable = 'Cette information est introuvable.';
  static const String inattendue = "Une erreur inattendue s'est produite.";

  static String pourStatut(int statut) {
    return switch (statut) {
      401 => identifiants,
      403 => accesRefuse,
      404 => introuvable,
      _ => 'Erreur $statut — la requête n\'a pas abouti.',
    };
  }
}
