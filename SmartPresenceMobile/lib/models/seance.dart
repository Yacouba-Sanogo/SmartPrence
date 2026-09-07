import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';
import 'presence.dart';

/// Avancement d'une séance.
enum StatutSeance { planifiee, enCours, terminee, annulee, inconnu }

StatutSeance statutSeanceDepuis(String? valeur) => switch (valeur) {
      'PLANIFIEE' => StatutSeance.planifiee,
      'EN_COURS' => StatutSeance.enCours,
      'TERMINEE' => StatutSeance.terminee,
      'ANNULEE' => StatutSeance.annulee,
      _ => StatutSeance.inconnu,
    };

extension AffichageStatutSeance on StatutSeance {
  String get libelle => switch (this) {
        StatutSeance.planifiee => 'Planifiée',
        StatutSeance.enCours => 'En cours',
        StatutSeance.terminee => 'Terminée',
        StatutSeance.annulee => 'Annulée',
        StatutSeance.inconnu => '—',
      };

  Color get teinte => switch (this) {
        StatutSeance.enCours => Couleurs.royal600,
        StatutSeance.terminee => Couleurs.encreDiscrete,
        StatutSeance.annulee => Couleurs.danger,
        _ => Couleurs.encreAttenuee,
      };

  Color get fond => switch (this) {
        StatutSeance.enCours => Couleurs.royal50,
        StatutSeance.annulee => Couleurs.dangerFond,
        _ => Couleurs.traitPale,
      };
}

/// Séance assurée par l'enseignant connecté.
class Seance {
  const Seance({
    required this.id,
    required this.matiere,
    required this.classeCode,
    required this.debut,
    required this.fin,
    required this.statut,
    this.salle,
  });

  final String id;
  final String matiere;
  final String classeCode;
  final DateTime debut;
  final DateTime fin;
  final StatutSeance statut;
  final String? salle;

  /// `true` si l'heure courante tombe dans le créneau — sert à mettre la ligne en avant.
  bool get seDerouleMaintenant {
    final maintenant = DateTime.now();
    return maintenant.isAfter(debut) && maintenant.isBefore(fin);
  }

  String get creneau => '${_hhmm(debut)} – ${_hhmm(fin)}';

  static String _hhmm(DateTime d) =>
      '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';

  factory Seance.depuisJson(Map<String, dynamic> json) {
    return Seance(
      id: json['id'] as String? ?? '',
      matiere: json['matiereLibelle'] as String? ?? 'Séance',
      classeCode: json['classeCode'] as String? ?? '',
      // Les instants arrivent en UTC : convertis en heure locale pour l'affichage.
      debut: DateTime.tryParse(json['debut'] as String? ?? '')?.toLocal() ?? DateTime.now(),
      fin: DateTime.tryParse(json['fin'] as String? ?? '')?.toLocal() ?? DateTime.now(),
      statut: statutSeanceDepuis(json['statut'] as String?),
      salle: json['salleLibelle'] as String?,
    );
  }
}

/// Classe dans laquelle l'enseignant intervient.
class ClasseEnseignee {
  const ClasseEnseignee({
    required this.id,
    required this.code,
    required this.libelle,
    this.promotion,
    this.effectif,
  });

  final int id;
  final String code;
  final String libelle;
  final String? promotion;
  final int? effectif;

  factory ClasseEnseignee.depuisJson(Map<String, dynamic> json) {
    // La promotion arrive imbriquee : {promotion: {libelle: ...}}.
    final promotion = json['promotion'] as Map<String, dynamic>?;
    return ClasseEnseignee(
      id: (json['id'] as num?)?.toInt() ?? 0,
      code: json['code'] as String? ?? '',
      libelle: json['libelle'] as String? ?? '',
      promotion: promotion?['libelle'] as String?,
      effectif: (json['nombreEtudiants'] as num?)?.toInt(),
    );
  }
}

/// Ligne de la feuille de présence.
class LigneFeuille {
  const LigneFeuille({
    required this.etudiantId,
    required this.matricule,
    required this.nom,
    required this.prenom,
    required this.enrole,
    required this.statut,
    this.heure,
    this.source,
  });

  final String etudiantId;
  final String matricule;
  final String nom;
  final String prenom;

  /// `false` : l'étudiant ne peut pas être relevé par un lecteur.
  final bool enrole;

  final StatutPresence statut;
  final String? heure;
  final String? source;

  String get nomComplet => '$prenom $nom'.trim();

  String get initiales =>
      '${prenom.isNotEmpty ? prenom[0] : ''}${nom.isNotEmpty ? nom[0] : ''}'.toUpperCase();

  bool get estRegularise => source == 'MANUEL';

  /// Absent alors qu'il n'était pas identifiable : à ne pas confondre avec un vrai absent.
  bool get absentNonIdentifiable => statut == StatutPresence.absent && !enrole;

  factory LigneFeuille.depuisJson(Map<String, dynamic> json) {
    final heureBrute = json['heure'] as String?;
    return LigneFeuille(
      etudiantId: json['etudiantId'] as String? ?? '',
      matricule: json['matricule'] as String? ?? '',
      nom: json['nom'] as String? ?? '',
      prenom: json['prenom'] as String? ?? '',
      enrole: json['enrole'] as bool? ?? false,
      statut: statutDepuis(json['statut'] as String?),
      heure: heureBrute != null && heureBrute.length >= 5
          ? heureBrute.substring(0, 5)
          : heureBrute,
      source: json['source'] as String?,
    );
  }
}

/// Feuille de présence d'une séance.
class FeuilleSeance {
  const FeuilleSeance({
    required this.seanceId,
    required this.matiere,
    required this.classeCode,
    required this.debut,
    required this.effectif,
    required this.presents,
    required this.retards,
    required this.absents,
    required this.absentsNonEnroles,
    required this.lignes,
    this.salle,
  });

  final String seanceId;
  final String matiere;
  final String classeCode;
  final DateTime debut;
  final int effectif;
  final int presents;
  final int retards;
  final int absents;

  /// Absents que le lecteur ne pouvait pas identifier, faute d'empreinte enrôlée.
  final int absentsNonEnroles;

  final List<LigneFeuille> lignes;
  final String? salle;

  /// Taux de présence effectif, retards compris — ils étaient là.
  int get tauxPresence =>
      effectif == 0 ? 0 : (((presents + retards) / effectif) * 100).round();

  factory FeuilleSeance.depuisJson(Map<String, dynamic> json) {
    int entier(String cle) => (json[cle] as num?)?.toInt() ?? 0;
    return FeuilleSeance(
      seanceId: json['seanceId'] as String? ?? '',
      matiere: json['matiereLibelle'] as String? ?? 'Séance',
      classeCode: json['classeCode'] as String? ?? '',
      debut: DateTime.tryParse(json['debut'] as String? ?? '')?.toLocal() ?? DateTime.now(),
      effectif: entier('effectif'),
      presents: entier('presents'),
      retards: entier('retards'),
      absents: entier('absents'),
      absentsNonEnroles: entier('absentsNonEnroles'),
      salle: json['salleLibelle'] as String?,
      lignes: ((json['lignes'] as List<dynamic>?) ?? const [])
          .map((e) => LigneFeuille.depuisJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
