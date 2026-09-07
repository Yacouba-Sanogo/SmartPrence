/// Étudiant de la classe, tel que l'enseignant a besoin de le connaître.
///
/// Le serveur n'envoie que ces six champs sur `/moi/classes/{id}/etudiants` : ni email,
/// ni téléphone, ni référence biométrique. La restriction est côté serveur, pas
/// seulement dans ce modèle — un client curieux n'obtiendrait rien de plus.
class EtudiantInscrit {
  const EtudiantInscrit({
    required this.id,
    required this.matricule,
    required this.nom,
    required this.prenom,
    required this.enrole,
    required this.actif,
  });

  final String id;
  final String matricule;
  final String nom;
  final String prenom;

  /// `false` : aucun lecteur ne peut relever la présence de cet étudiant.
  final bool enrole;

  final bool actif;

  String get nomComplet => '$prenom $nom'.trim();

  String get initiales {
    final paire = '${prenom.isNotEmpty ? prenom[0] : ''}'
        '${nom.isNotEmpty ? nom[0] : ''}';
    return paire.isEmpty ? '?' : paire.toUpperCase();
  }

  factory EtudiantInscrit.depuisJson(Map<String, dynamic> json) => EtudiantInscrit(
        id: json['id'] as String? ?? '',
        matricule: json['matricule'] as String? ?? '',
        nom: json['nom'] as String? ?? '',
        prenom: json['prenom'] as String? ?? '',
        enrole: json['enrole'] as bool? ?? false,
        actif: json['actif'] as bool? ?? true,
      );
}
