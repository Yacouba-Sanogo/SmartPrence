/// Profil métier résolu pour le compte connecté.
enum TypeProfil { etudiant, enseignant, personnel, aucun }

TypeProfil _typeDepuis(String? valeur) => switch (valeur) {
      'ETUDIANT' => TypeProfil.etudiant,
      'ENSEIGNANT' => TypeProfil.enseignant,
      'PERSONNEL' => TypeProfil.personnel,
      _ => TypeProfil.aucun,
    };

/// Identité complète de l'utilisateur connecté, renvoyée par `GET /moi/profil`.
///
/// C'est le premier appel de l'application après connexion : le compte seul ne dit pas
/// qui est la personne dans le référentiel, et donc quels écrans lui ouvrir.
class Profil {
  const Profil({
    required this.utilisateurId,
    required this.email,
    required this.nom,
    required this.prenom,
    required this.roles,
    required this.type,
    this.profilId,
    this.matricule,
    this.classeId,
    this.classeCode,
    this.classeLibelle,
    this.promotionLibelle,
    this.service,
  });

  final String utilisateurId;
  final String email;
  final String nom;
  final String prenom;
  final Set<String> roles;
  final TypeProfil type;

  final String? profilId;
  final String? matricule;

  final int? classeId;
  final String? classeCode;
  final String? classeLibelle;
  final String? promotionLibelle;

  final String? service;

  bool get estEtudiant => type == TypeProfil.etudiant;
  bool get estEnseignant => type == TypeProfil.enseignant;

  String get nomComplet => '$prenom $nom'.trim();

  /// Initiales de l'avatar, en repli sur l'email quand le nom manque.
  String get initiales {
    final paire = '${prenom.isNotEmpty ? prenom[0] : ''}'
            '${nom.isNotEmpty ? nom[0] : ''}'
        .trim();
    if (paire.isNotEmpty) return paire.toUpperCase();
    return email.isNotEmpty ? email[0].toUpperCase() : '?';
  }

  factory Profil.depuisJson(Map<String, dynamic> json) {
    return Profil(
      utilisateurId: json['utilisateurId'] as String? ?? '',
      email: json['email'] as String? ?? '',
      nom: json['nom'] as String? ?? '',
      prenom: json['prenom'] as String? ?? '',
      // Les rôles arrivent préfixés par Spring Security (`ROLE_ETUDIANT`).
      roles: ((json['roles'] as List<dynamic>?) ?? const [])
          .map((r) => '$r'.replaceFirst('ROLE_', ''))
          .toSet(),
      type: _typeDepuis(json['typeProfil'] as String?),
      profilId: json['profilId'] as String?,
      matricule: json['matricule'] as String?,
      classeId: (json['classeId'] as num?)?.toInt(),
      classeCode: json['classeCode'] as String?,
      classeLibelle: json['classeLibelle'] as String?,
      promotionLibelle: json['promotionLibelle'] as String?,
      service: json['service'] as String?,
    );
  }
}
