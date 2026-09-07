/// Assiduité d'un étudiant, renvoyée par `GET /moi/statistiques`.
class Assiduite {
  const Assiduite({
    required this.total,
    required this.presents,
    required this.retards,
    required this.absents,
    required this.justifies,
    required this.taux,
    this.classeCode,
  });

  final int total;
  final int presents;
  final int retards;
  final int absents;
  final int justifies;

  /// Pourcentage déjà calculé par le backend.
  final double taux;

  final String? classeCode;

  /// `true` tant qu'aucun relevé n'existe : un taux de 0 % n'aurait alors aucun sens.
  bool get sansHistorique => total == 0;

  factory Assiduite.depuisJson(Map<String, dynamic> json) {
    int entier(String cle) => (json[cle] as num?)?.toInt() ?? 0;
    return Assiduite(
      total: entier('totalPresences'),
      presents: entier('totalPresents'),
      retards: entier('totalRetards'),
      absents: entier('totalAbsents'),
      justifies: entier('totalJustifies'),
      taux: (json['tauxAssiduite'] as num?)?.toDouble() ?? 0,
      classeCode: json['classeCode'] as String?,
    );
  }

  static const Assiduite vide = Assiduite(
    total: 0,
    presents: 0,
    retards: 0,
    absents: 0,
    justifies: 0,
    taux: 0,
  );
}
