import 'note.dart';

/// Verdict d'un semestre.
enum DecisionSemestre { valide, nonValide, enAttente }

DecisionSemestre decisionDepuis(String? valeur) => switch (valeur) {
      'VALIDE' => DecisionSemestre.valide,
      'NON_VALIDE' => DecisionSemestre.nonValide,
      _ => DecisionSemestre.enAttente,
    };

extension AffichageDecision on DecisionSemestre {
  String get libelle => switch (this) {
        DecisionSemestre.valide => 'Validé',
        DecisionSemestre.nonValide => 'Non validé',
        DecisionSemestre.enAttente => 'En attente',
      };
}

/// Relevé de notes d'un semestre, au format LMD.
///
/// Rien n'est calculé ici. Les moyennes, les crédits et la décision viennent du
/// serveur : trois clients qui les recalculeraient chacun de leur côté finiraient
/// par annoncer trois résultats différents à un centième près, et c'est le relevé
/// de l'administration qui fait foi.
class ReleveSemestre {
  const ReleveSemestre({
    required this.semestre,
    required this.semestresDisponibles,
    required this.unites,
    required this.creditsAcquis,
    required this.creditsRequis,
    required this.decision,
    required this.nombreNotes,
    this.moyenneGenerale,
    this.classeLibelle,
    this.promotionLibelle,
    this.compensationAppliquee = false,
  });

  final PeriodeScolaire semestre;

  /// Semestres où la promotion a une maquette — les onglets à proposer, et eux seuls.
  final List<PeriodeScolaire> semestresDisponibles;

  final List<UniteReleve> unites;
  final int creditsAcquis;
  final int creditsRequis;
  final DecisionSemestre decision;
  final int nombreNotes;

  /// `null` — et non zéro — quand aucune note n'a encore été saisie.
  final double? moyenneGenerale;

  final String? classeLibelle;
  final String? promotionLibelle;

  /// Vrai lorsque des UE sous la barre ont été acquises par la moyenne générale.
  final bool compensationAppliquee;

  bool get sansNote => nombreNotes == 0;

  /// `15,38` — virgule décimale, comme sur un relevé imprimé.
  String get moyenneFormatee => moyenneGenerale == null
      ? '—'
      : moyenneGenerale!.toStringAsFixed(2).replaceAll('.', ',');

  /// Part de 0 à 100, pour l'anneau de l'en-tête.
  double get progression =>
      moyenneGenerale == null ? 0 : (moyenneGenerale! / 20 * 100).clamp(0, 100);

  factory ReleveSemestre.depuisJson(Map<String, dynamic> json) {
    final semestres = ((json['semestresDisponibles'] as List<dynamic>?) ?? const [])
        .map((e) => periodeDepuis(e as String?))
        .whereType<PeriodeScolaire>()
        .toList();

    return ReleveSemestre(
      semestre: periodeDepuis(json['semestre'] as String?) ?? PeriodeScolaire.semestre1,
      // Toujours au moins l'onglet affiché : une barre vide serait déroutante.
      semestresDisponibles: semestres.isEmpty
          ? [periodeDepuis(json['semestre'] as String?) ?? PeriodeScolaire.semestre1]
          : semestres,
      unites: ((json['unites'] as List<dynamic>?) ?? const [])
          .map((e) => UniteReleve.depuisJson(e as Map<String, dynamic>))
          .toList(),
      creditsAcquis: (json['creditsAcquis'] as num?)?.toInt() ?? 0,
      creditsRequis: (json['creditsRequis'] as num?)?.toInt() ?? 0,
      decision: decisionDepuis(json['decision'] as String?),
      nombreNotes: (json['nombreNotes'] as num?)?.toInt() ?? 0,
      moyenneGenerale: (json['moyenneGenerale'] as num?)?.toDouble(),
      classeLibelle: json['classeLibelle'] as String?,
      promotionLibelle: json['promotionLibelle'] as String?,
      compensationAppliquee: json['compensationAppliquee'] as bool? ?? false,
    );
  }

  static const ReleveSemestre vide = ReleveSemestre(
    semestre: PeriodeScolaire.semestre1,
    semestresDisponibles: [PeriodeScolaire.semestre1],
    unites: [],
    creditsAcquis: 0,
    creditsRequis: 0,
    decision: DecisionSemestre.enAttente,
    nombreNotes: 0,
  );
}

/// Résultat dans une unité d'enseignement.
class UniteReleve {
  const UniteReleve({
    required this.id,
    required this.code,
    required this.libelle,
    required this.credits,
    required this.creditsAcquis,
    required this.acquise,
    required this.ecues,
    this.moyenne,
    this.acquiseParCompensation = false,
  });

  final int id;
  final String code;
  final String libelle;
  final int credits;
  final int creditsAcquis;
  final bool acquise;
  final bool acquiseParCompensation;
  final double? moyenne;
  final List<EcueReleve> ecues;

  String get moyenneFormatee =>
      moyenne == null ? '—' : moyenne!.toStringAsFixed(2).replaceAll('.', ',');

  /// `24 / 30` — ce que l'UE rapporte sur ce qu'elle pourrait rapporter.
  String get creditsFormates => '$creditsAcquis / $credits';

  factory UniteReleve.depuisJson(Map<String, dynamic> json) => UniteReleve(
        id: (json['uniteId'] as num?)?.toInt() ?? 0,
        code: json['code'] as String? ?? '',
        libelle: json['libelle'] as String? ?? '',
        credits: (json['credits'] as num?)?.toInt() ?? 0,
        creditsAcquis: (json['creditsAcquis'] as num?)?.toInt() ?? 0,
        acquise: json['acquise'] as bool? ?? false,
        acquiseParCompensation: json['acquiseParCompensation'] as bool? ?? false,
        moyenne: (json['moyenne'] as num?)?.toDouble(),
        ecues: ((json['ecues'] as List<dynamic>?) ?? const [])
            .map((e) => EcueReleve.depuisJson(e as Map<String, dynamic>))
            .toList(),
      );
}

/// Résultat dans un élément constitutif — une matière.
class EcueReleve {
  const EcueReleve({
    required this.matiereId,
    required this.code,
    required this.libelle,
    required this.credits,
    this.noteDevoir,
    this.noteExamen,
    this.moyenne,
  });

  final int matiereId;
  final String code;
  final String libelle;
  final int credits;

  /// Moyenne du contrôle continu. `null` tant qu'aucun devoir n'est rendu.
  final double? noteDevoir;

  /// Moyenne des examens. `null` tant que l'examen n'a pas eu lieu.
  final double? noteExamen;

  /// Devoir et examen combinés selon la pondération du règlement.
  final double? moyenne;

  String get devoirFormate => _format(noteDevoir);
  String get examenFormate => _format(noteExamen);
  String get moyenneFormatee => _format(moyenne);

  static String _format(double? valeur) {
    if (valeur == null) return '—';
    final entier = valeur == valeur.roundToDouble();
    return valeur.toStringAsFixed(entier ? 0 : 2).replaceAll('.', ',');
  }

  factory EcueReleve.depuisJson(Map<String, dynamic> json) => EcueReleve(
        matiereId: (json['matiereId'] as num?)?.toInt() ?? 0,
        code: json['code'] as String? ?? '',
        libelle: json['libelle'] as String? ?? '',
        credits: (json['credits'] as num?)?.toInt() ?? 0,
        noteDevoir: (json['noteDevoir'] as num?)?.toDouble(),
        noteExamen: (json['noteExamen'] as num?)?.toDouble(),
        moyenne: (json['moyenne'] as num?)?.toDouble(),
      );
}
