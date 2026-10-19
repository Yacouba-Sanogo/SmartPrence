import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';
import 'note.dart';

/// Une journée de présence de l'enseignant, d'après les pointages du lecteur.
class JourneePointage {
  const JourneePointage({
    required this.date,
    required this.retard,
    required this.minutesRetard,
    required this.surPlace,
    this.entree,
    this.sortie,
    this.minutesTravaillees,
  });

  final DateTime date;
  final String? entree;
  final String? sortie;
  final bool retard;
  final int minutesRetard;

  /// Entré sans avoir pointé sa sortie.
  final bool surPlace;
  final int? minutesTravaillees;

  static String? _hhmm(String? brut) =>
      brut == null || brut.length < 5 ? null : brut.substring(0, 5);

  factory JourneePointage.depuisJson(Map<String, dynamic> json) => JourneePointage(
        date: DateTime.tryParse(json['date'] as String? ?? '') ?? DateTime.now(),
        entree: _hhmm(json['heureEntree'] as String?),
        sortie: _hhmm(json['heureSortie'] as String?),
        retard: json['statut'] == 'RETARD',
        minutesRetard: (json['minutesRetard'] as num?)?.toInt() ?? 0,
        surPlace: json['present'] == true,
        minutesTravaillees: (json['minutesTravaillees'] as num?)?.toInt(),
      );
}

/// Bilan d'enseignement d'une période.
class BilanEnseignement {
  const BilanEnseignement({
    required this.prevues,
    required this.effectuees,
    required this.annulees,
    required this.aDeclarer,
    required this.minutes,
    required this.joursPointes,
    required this.joursEnRetard,
    required this.parMatiere,
  });

  final int prevues;
  final int effectuees;
  final int annulees;

  /// Séances passées, ni annulées ni déclarées : ce qu'il reste à régulariser.
  final int aDeclarer;
  final int minutes;
  final int joursPointes;
  final int joursEnRetard;
  final List<HeuresMatiere> parMatiere;

  String get heures {
    final h = minutes ~/ 60;
    final m = minutes % 60;
    return m == 0 ? '$h h' : '$h h ${m.toString().padLeft(2, '0')}';
  }

  factory BilanEnseignement.depuisJson(Map<String, dynamic> json) => BilanEnseignement(
        prevues: (json['seancesPrevues'] as num?)?.toInt() ?? 0,
        effectuees: (json['seancesEffectuees'] as num?)?.toInt() ?? 0,
        annulees: (json['seancesAnnulees'] as num?)?.toInt() ?? 0,
        aDeclarer: (json['seancesADeclarer'] as num?)?.toInt() ?? 0,
        minutes: (json['minutesEnseignees'] as num?)?.toInt() ?? 0,
        joursPointes: (json['joursPointes'] as num?)?.toInt() ?? 0,
        joursEnRetard: (json['joursEnRetard'] as num?)?.toInt() ?? 0,
        parMatiere: ((json['parMatiere'] as List<dynamic>?) ?? const [])
            .map((e) => HeuresMatiere.depuisJson(e as Map<String, dynamic>))
            .toList(),
      );
}

class HeuresMatiere {
  const HeuresMatiere({required this.libelle, required this.effectuees, required this.minutes});

  final String libelle;
  final int effectuees;
  final int minutes;

  factory HeuresMatiere.depuisJson(Map<String, dynamic> json) => HeuresMatiere(
        libelle: json['matiereLibelle'] as String? ?? '',
        effectuees: (json['effectuees'] as num?)?.toInt() ?? 0,
        minutes: (json['minutes'] as num?)?.toInt() ?? 0,
      );
}

class Chapitre {
  const Chapitre({
    required this.id,
    required this.ordre,
    required this.titre,
    required this.termine,
    this.objectifs,
    this.termineLe,
  });

  final String id;
  final int ordre;
  final String titre;
  final String? objectifs;
  final bool termine;
  final DateTime? termineLe;

  factory Chapitre.depuisJson(Map<String, dynamic> json) => Chapitre(
        id: json['id'] as String? ?? '',
        ordre: (json['ordre'] as num?)?.toInt() ?? 0,
        titre: json['titre'] as String? ?? '',
        objectifs: json['objectifs'] as String?,
        termine: json['termine'] == true,
        termineLe: DateTime.tryParse(json['termineLe'] as String? ?? ''),
      );
}

/// Programme d'une matière dans une classe, avec son avancement.
class Syllabus {
  const Syllabus({
    required this.matiereId,
    required this.matiereLibelle,
    required this.chapitres,
    required this.termines,
  });

  final int matiereId;
  final String matiereLibelle;
  final List<Chapitre> chapitres;
  final int termines;

  double get avancement => chapitres.isEmpty ? 0 : termines / chapitres.length;

  factory Syllabus.depuisJson(Map<String, dynamic> json) {
    final chapitres = ((json['chapitres'] as List<dynamic>?) ?? const [])
        .map((e) => Chapitre.depuisJson(e as Map<String, dynamic>))
        .toList();
    final brut = (json['chapitres'] as List<dynamic>?) ?? const [];
    final premier = brut.isEmpty ? null : brut.first as Map<String, dynamic>;
    return Syllabus(
      matiereId: (json['matiereId'] as num?)?.toInt() ?? 0,
      matiereLibelle: premier?['matiereLibelle'] as String? ?? '',
      chapitres: chapitres,
      termines: (json['termines'] as num?)?.toInt() ?? 0,
    );
  }
}

/// Page du cahier de textes.
class PageCahier {
  const PageCahier({
    required this.id,
    required this.seanceId,
    required this.debutSeance,
    required this.matiereLibelle,
    required this.contenu,
    this.travailAFaire,
    this.chapitreId,
    this.chapitreTitre,
  });

  final String id;
  final String seanceId;
  final DateTime debutSeance;
  final String matiereLibelle;
  final String contenu;
  final String? travailAFaire;
  final String? chapitreId;
  final String? chapitreTitre;

  factory PageCahier.depuisJson(Map<String, dynamic> json) => PageCahier(
        id: json['id'] as String? ?? '',
        seanceId: json['seanceId'] as String? ?? '',
        debutSeance:
            DateTime.tryParse(json['debutSeance'] as String? ?? '')?.toLocal() ?? DateTime.now(),
        matiereLibelle: json['matiereLibelle'] as String? ?? '',
        contenu: json['contenu'] as String? ?? '',
        travailAFaire: json['travailAFaire'] as String?,
        chapitreId: json['chapitreId'] as String?,
        chapitreTitre: json['chapitreTitre'] as String?,
      );
}

/// Étape d'une note dans son circuit de validation.
enum StatutNote { brouillon, transmise, validee }

StatutNote statutNoteDepuis(String? valeur) => switch (valeur) {
      'TRANSMISE' => StatutNote.transmise,
      'VALIDEE' => StatutNote.validee,
      _ => StatutNote.brouillon,
    };

extension AffichageStatutNote on StatutNote {
  String get libelle => switch (this) {
        StatutNote.brouillon => 'Brouillon',
        StatutNote.transmise => 'Transmise',
        StatutNote.validee => 'Validée',
      };

  Color get teinte => switch (this) {
        StatutNote.brouillon => Couleurs.encreAttenuee,
        StatutNote.transmise => Couleurs.alerte,
        StatutNote.validee => Couleurs.succes,
      };

  Color get fond => switch (this) {
        StatutNote.brouillon => Couleurs.traitPale,
        StatutNote.transmise => Couleurs.alerteFond,
        StatutNote.validee => Couleurs.succesFond,
      };

  /// Seul un brouillon peut encore être corrigé ou supprimé.
  bool get modifiable => this == StatutNote.brouillon;
}

class Evaluation {
  const Evaluation({
    required this.id,
    required this.matiereId,
    required this.matiereLibelle,
    required this.type,
    required this.libelle,
    required this.date,
    required this.bareme,
    required this.coefficient,
    required this.periode,
    required this.effectif,
    required this.notes,
    required this.absents,
    required this.brouillons,
    required this.transmises,
    required this.validees,
    this.sujet,
  });

  final String id;
  final int matiereId;
  final String matiereLibelle;
  final TypeEvaluation type;
  final String libelle;
  final DateTime date;
  final int bareme;
  final int coefficient;
  final PeriodeScolaire periode;
  final String? sujet;
  final int effectif;
  final int notes;
  final int absents;
  final int brouillons;
  final int transmises;
  final int validees;

  /// Étudiants ni notés ni déclarés absents.
  int get restants => effectif - notes - absents;

  factory Evaluation.depuisJson(Map<String, dynamic> json) => Evaluation(
        id: json['id'] as String? ?? '',
        matiereId: (json['matiereId'] as num?)?.toInt() ?? 0,
        matiereLibelle: json['matiereLibelle'] as String? ?? '',
        type: typeEvaluationDepuis(json['type'] as String?),
        libelle: json['libelle'] as String? ?? '',
        date: DateTime.tryParse(json['dateEvaluation'] as String? ?? '') ?? DateTime.now(),
        bareme: (json['bareme'] as num?)?.toInt() ?? 20,
        coefficient: (json['coefficient'] as num?)?.toInt() ?? 1,
        periode: periodeDepuis(json['periode'] as String?) ?? PeriodeScolaire.semestre1,
        sujet: json['sujet'] as String?,
        effectif: (json['effectif'] as num?)?.toInt() ?? 0,
        notes: (json['notes'] as num?)?.toInt() ?? 0,
        absents: (json['absents'] as num?)?.toInt() ?? 0,
        brouillons: (json['brouillons'] as num?)?.toInt() ?? 0,
        transmises: (json['transmises'] as num?)?.toInt() ?? 0,
        validees: (json['validees'] as num?)?.toInt() ?? 0,
      );
}

/// Ligne des résultats : un étudiant, sa note sur le barème, ou son absence.
class ResultatEtudiant {
  const ResultatEtudiant({
    required this.etudiantId,
    required this.matricule,
    required this.nom,
    required this.prenom,
    required this.absent,
    this.valeurSurBareme,
    this.statut,
    this.motifRejet,
    this.motifAbsence,
  });

  final String etudiantId;
  final String matricule;
  final String nom;
  final String prenom;
  final double? valeurSurBareme;
  final StatutNote? statut;
  final String? motifRejet;
  final bool absent;
  final String? motifAbsence;

  String get nomComplet => '$prenom $nom'.trim();

  /// Une note transmise ou validée n'est plus entre les mains de l'enseignant.
  bool get verrouille => statut != null && !statut!.modifiable;

  factory ResultatEtudiant.depuisJson(Map<String, dynamic> json) => ResultatEtudiant(
        etudiantId: json['etudiantId'] as String? ?? '',
        matricule: json['matricule'] as String? ?? '',
        nom: json['nom'] as String? ?? '',
        prenom: json['prenom'] as String? ?? '',
        valeurSurBareme: (json['valeurSurBareme'] as num?)?.toDouble(),
        statut: json['statut'] == null ? null : statutNoteDepuis(json['statut'] as String?),
        motifRejet: json['motifRejet'] as String?,
        absent: json['absent'] == true,
        motifAbsence: json['motifAbsence'] as String?,
      );
}

class ResultatsEvaluation {
  const ResultatsEvaluation({required this.evaluation, required this.lignes});

  final Evaluation evaluation;
  final List<ResultatEtudiant> lignes;

  factory ResultatsEvaluation.depuisJson(Map<String, dynamic> json) => ResultatsEvaluation(
        evaluation: Evaluation.depuisJson(json['evaluation'] as Map<String, dynamic>),
        lignes: ((json['lignes'] as List<dynamic>?) ?? const [])
            .map((e) => ResultatEtudiant.depuisJson(e as Map<String, dynamic>))
            .toList(),
      );
}

/// Une ligne de saisie : une note sur le barème de l'épreuve, ou une absence.
class SaisieResultat {
  const SaisieResultat({
    required this.etudiantId,
    this.valeur,
    this.absent = false,
    this.motif,
    this.appreciation,
  });

  final String etudiantId;
  final double? valeur;
  final bool absent;
  final String? motif;
  final String? appreciation;

  Map<String, dynamic> versJson() => {
        'etudiantId': etudiantId,
        if (!absent && valeur != null) 'valeur': valeur,
        'absent': absent,
        if (motif != null && motif!.trim().isNotEmpty) 'motif': motif!.trim(),
        if (appreciation != null && appreciation!.trim().isNotEmpty)
          'appreciation': appreciation!.trim(),
      };
}
