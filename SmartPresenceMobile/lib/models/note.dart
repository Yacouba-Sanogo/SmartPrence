import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

/// Nature d'une évaluation.
enum TypeEvaluation { devoir, interrogation, travauxPratiques, composition, examen }

TypeEvaluation typeEvaluationDepuis(String? valeur) => switch (valeur) {
      'INTERROGATION' => TypeEvaluation.interrogation,
      'TRAVAUX_PRATIQUES' => TypeEvaluation.travauxPratiques,
      'COMPOSITION' => TypeEvaluation.composition,
      'EXAMEN' => TypeEvaluation.examen,
      _ => TypeEvaluation.devoir,
    };

extension AffichageTypeEvaluation on TypeEvaluation {
  /// Valeur attendue par l'API.
  String get code => switch (this) {
        TypeEvaluation.devoir => 'DEVOIR',
        TypeEvaluation.interrogation => 'INTERROGATION',
        TypeEvaluation.travauxPratiques => 'TRAVAUX_PRATIQUES',
        TypeEvaluation.composition => 'COMPOSITION',
        TypeEvaluation.examen => 'EXAMEN',
      };

  String get libelle => switch (this) {
        TypeEvaluation.devoir => 'Devoir',
        TypeEvaluation.interrogation => 'Interrogation',
        TypeEvaluation.travauxPratiques => 'Travaux pratiques',
        TypeEvaluation.composition => 'Composition',
        TypeEvaluation.examen => 'Examen',
      };

  IconData get icone => switch (this) {
        TypeEvaluation.devoir => Icons.edit_note_rounded,
        TypeEvaluation.interrogation => Icons.help_outline_rounded,
        TypeEvaluation.travauxPratiques => Icons.science_outlined,
        TypeEvaluation.composition => Icons.article_outlined,
        TypeEvaluation.examen => Icons.workspace_premium_outlined,
      };
}

/// Période scolaire.
enum PeriodeScolaire { semestre1, semestre2 }

extension AffichagePeriode on PeriodeScolaire {
  String get code => this == PeriodeScolaire.semestre1 ? 'SEMESTRE_1' : 'SEMESTRE_2';
  String get libelle => this == PeriodeScolaire.semestre1 ? 'Semestre 1' : 'Semestre 2';
}

/// Teinte d'une note sur 20.
///
/// Le seuil de 10 est celui qui décide : il doit se voir sans qu'on ait à lire le
/// chiffre. Au-delà de 14, la réussite est nette.
Color teinteNote(double valeur) {
  if (valeur >= 14) return Couleurs.succesVif;
  if (valeur >= 10) return Couleurs.alerteVif;
  return Couleurs.dangerVif;
}

Color fondNote(double valeur) {
  if (valeur >= 14) return Couleurs.succesFond;
  if (valeur >= 10) return Couleurs.alerteFond;
  return Couleurs.dangerFond;
}

/// Une note obtenue par l'étudiant.
class Note {
  const Note({
    required this.id,
    required this.valeur,
    required this.coefficient,
    required this.type,
    required this.libelle,
    required this.dateEvaluation,
    this.appreciation,
    this.enseignantNom,
    this.matiereId,
    this.matiereLibelle,
    this.etudiantId,
    this.etudiantNom,
    this.etudiantPrenom,
  });

  final String id;
  final double valeur;
  final int coefficient;
  final TypeEvaluation type;
  final String libelle;
  final DateTime dateEvaluation;
  final String? appreciation;
  final String? enseignantNom;

  /// Renseignés côté enseignant : la même note se lit alors « qui a eu quoi »,
  /// alors que l'étudiant la lit déjà groupée par matière.
  final int? matiereId;
  final String? matiereLibelle;
  final String? etudiantId;
  final String? etudiantNom;
  final String? etudiantPrenom;

  String get etudiantComplet =>
      '${etudiantPrenom ?? ''} ${etudiantNom ?? ''}'.trim();

  /// `12,5` — virgule décimale, et pas de zéro inutile sur un entier.
  String get valeurFormatee {
    final entier = valeur == valeur.roundToDouble();
    return valeur.toStringAsFixed(entier ? 0 : 2).replaceAll('.', ',');
  }

  factory Note.depuisJson(Map<String, dynamic> json) => Note(
        id: json['id'] as String? ?? '',
        valeur: (json['valeur'] as num?)?.toDouble() ?? 0,
        coefficient: (json['coefficient'] as num?)?.toInt() ?? 1,
        type: typeEvaluationDepuis(json['type'] as String?),
        libelle: json['libelle'] as String? ?? 'Évaluation',
        dateEvaluation:
            DateTime.tryParse(json['dateEvaluation'] as String? ?? '') ?? DateTime.now(),
        appreciation: json['appreciation'] as String?,
        enseignantNom: json['enseignantNom'] as String?,
        matiereId: (json['matiereId'] as num?)?.toInt(),
        matiereLibelle: json['matiereLibelle'] as String?,
        etudiantId: json['etudiantId'] as String?,
        etudiantNom: json['etudiantNom'] as String?,
        etudiantPrenom: json['etudiantPrenom'] as String?,
      );
}

/// Résultat dans une matière.
class LigneMatiere {
  const LigneMatiere({
    required this.matiereId,
    required this.code,
    required this.libelle,
    required this.coefficient,
    required this.notes,
    this.moyenne,
  });

  final int matiereId;
  final String code;
  final String libelle;
  final int coefficient;
  final double? moyenne;
  final List<Note> notes;

  String get moyenneFormatee =>
      moyenne == null ? '—' : moyenne!.toStringAsFixed(2).replaceAll('.', ',');

  factory LigneMatiere.depuisJson(Map<String, dynamic> json) => LigneMatiere(
        matiereId: (json['matiereId'] as num?)?.toInt() ?? 0,
        code: json['matiereCode'] as String? ?? '',
        libelle: json['matiereLibelle'] as String? ?? '',
        coefficient: (json['coefficient'] as num?)?.toInt() ?? 1,
        moyenne: (json['moyenne'] as num?)?.toDouble(),
        notes: ((json['notes'] as List<dynamic>?) ?? const [])
            .map((e) => Note.depuisJson(e as Map<String, dynamic>))
            .toList(),
      );
}

/// Bulletin d'un étudiant sur une période.
///
/// Les moyennes viennent du serveur. Les recalculer ici produirait, au moindre écart
/// d'arrondi, un bulletin différent de celui que voit l'administration.
class Bulletin {
  const Bulletin({
    required this.matieres,
    required this.nombreNotes,
    this.moyenneGenerale,
    this.classeLibelle,
  });

  final List<LigneMatiere> matieres;
  final int nombreNotes;

  /// `null` — et non zéro — quand aucune note n'a été saisie.
  final double? moyenneGenerale;

  final String? classeLibelle;

  bool get sansNote => nombreNotes == 0;

  String get moyenneFormatee => moyenneGenerale == null
      ? '—'
      : moyenneGenerale!.toStringAsFixed(2).replaceAll('.', ',');

  /// Part de 0 à 100, pour l'anneau.
  double get progression =>
      moyenneGenerale == null ? 0 : (moyenneGenerale! / 20 * 100).clamp(0, 100);

  /// Matières où la moyenne est sous la barre.
  int get matieresEnDifficulte =>
      matieres.where((m) => m.moyenne != null && m.moyenne! < 10).length;

  factory Bulletin.depuisJson(Map<String, dynamic> json) => Bulletin(
        matieres: ((json['matieres'] as List<dynamic>?) ?? const [])
            .map((e) => LigneMatiere.depuisJson(e as Map<String, dynamic>))
            .toList(),
        nombreNotes: (json['nombreNotes'] as num?)?.toInt() ?? 0,
        moyenneGenerale: (json['moyenneGenerale'] as num?)?.toDouble(),
        classeLibelle: json['classeLibelle'] as String?,
      );

  static const Bulletin vide =
      Bulletin(matieres: [], nombreNotes: 0, moyenneGenerale: null);
}

/// Matière, réduite à ce dont la saisie a besoin.
class MatiereBreve {
  const MatiereBreve({
    required this.id,
    required this.code,
    required this.libelle,
    this.credits,
  });

  final int id;
  final String code;
  final String libelle;
  final int? credits;

  factory MatiereBreve.depuisJson(Map<String, dynamic> json) => MatiereBreve(
        id: (json['id'] as num?)?.toInt() ?? 0,
        code: json['code'] as String? ?? '',
        libelle: json['libelle'] as String? ?? '',
        credits: (json['credits'] as num?)?.toInt(),
      );
}

/// Saisie d'une note, telle que l'enseignant la transmet.
class SaisieNote {
  const SaisieNote({
    required this.etudiantId,
    required this.matiereId,
    required this.valeur,
    required this.libelle,
    required this.dateEvaluation,
    this.coefficient = 1,
    this.type = TypeEvaluation.devoir,
    this.periode = PeriodeScolaire.semestre1,
    this.appreciation,
  });

  final String etudiantId;
  final int matiereId;
  final double valeur;
  final String libelle;
  final DateTime dateEvaluation;
  final int coefficient;
  final TypeEvaluation type;
  final PeriodeScolaire periode;
  final String? appreciation;

  Map<String, dynamic> versJson() => {
        'etudiantId': etudiantId,
        'matiereId': matiereId,
        'valeur': valeur,
        'coefficient': coefficient,
        'type': type.code,
        'periode': periode.code,
        'libelle': libelle.trim(),
        // Le serveur attend une date seule, sans heure ni fuseau.
        'dateEvaluation': _isoJour(dateEvaluation),
        if (appreciation != null && appreciation!.trim().isNotEmpty)
          'appreciation': appreciation!.trim(),
      };

  static String _isoJour(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-'
      '${d.month.toString().padLeft(2, '0')}-'
      '${d.day.toString().padLeft(2, '0')}';
}
