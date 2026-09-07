import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

/// Nature de l'anomalie signalée par l'enseignant.
enum TypeSignalement { etudiantNonReconnu, lecteurDefaillant, autre }

extension AffichageTypeSignalement on TypeSignalement {
  /// Valeur attendue par l'API.
  String get code => switch (this) {
        TypeSignalement.etudiantNonReconnu => 'ETUDIANT_NON_RECONNU',
        TypeSignalement.lecteurDefaillant => 'LECTEUR_DEFAILLANT',
        TypeSignalement.autre => 'AUTRE',
      };

  String get libelle => switch (this) {
        TypeSignalement.etudiantNonReconnu => 'Étudiant non reconnu',
        TypeSignalement.lecteurDefaillant => 'Lecteur défaillant',
        TypeSignalement.autre => 'Autre anomalie',
      };

  String get explication => switch (this) {
        TypeSignalement.etudiantNonReconnu =>
          "L'étudiant était présent mais le lecteur ne l'a pas identifié.",
        TypeSignalement.lecteurDefaillant =>
          "Le lecteur de la salle n'a pas fonctionné pendant la séance.",
        TypeSignalement.autre => "Une situation qui n'entre dans aucun des cas ci-dessus.",
      };

  IconData get icone => switch (this) {
        TypeSignalement.etudiantNonReconnu => Icons.person_search_outlined,
        TypeSignalement.lecteurDefaillant => Icons.sensors_off_rounded,
        TypeSignalement.autre => Icons.more_horiz_rounded,
      };

  /// Seul ce type désigne un étudiant : les autres visent la séance entière.
  bool get viseUnEtudiant => this == TypeSignalement.etudiantNonReconnu;
}

TypeSignalement typeSignalementDepuis(String? valeur) => switch (valeur) {
      'ETUDIANT_NON_RECONNU' => TypeSignalement.etudiantNonReconnu,
      'LECTEUR_DEFAILLANT' => TypeSignalement.lecteurDefaillant,
      _ => TypeSignalement.autre,
    };

/// Suite donnée au signalement par la scolarité.
enum StatutSignalement { enAttente, accepte, rejete }

extension AffichageStatutSignalement on StatutSignalement {
  String get libelle => switch (this) {
        StatutSignalement.enAttente => 'En attente',
        StatutSignalement.accepte => 'Retenu',
        StatutSignalement.rejete => 'Écarté',
      };

  Color get teinte => switch (this) {
        StatutSignalement.enAttente => Couleurs.alerte,
        StatutSignalement.accepte => Couleurs.succes,
        StatutSignalement.rejete => Couleurs.danger,
      };

  Color get fond => switch (this) {
        StatutSignalement.enAttente => Couleurs.alerteFond,
        StatutSignalement.accepte => Couleurs.succesFond,
        StatutSignalement.rejete => Couleurs.dangerFond,
      };

  IconData get icone => switch (this) {
        StatutSignalement.enAttente => Icons.hourglass_empty_rounded,
        StatutSignalement.accepte => Icons.check_circle_outline_rounded,
        StatutSignalement.rejete => Icons.cancel_outlined,
      };
}

StatutSignalement statutSignalementDepuis(String? valeur) => switch (valeur) {
      'ACCEPTE' => StatutSignalement.accepte,
      'REJETE' => StatutSignalement.rejete,
      _ => StatutSignalement.enAttente,
    };

/// Anomalie signalée par l'enseignant, et son suivi.
class Signalement {
  const Signalement({
    required this.id,
    required this.seanceId,
    required this.matiere,
    required this.classeCode,
    required this.seanceDebut,
    required this.type,
    required this.description,
    required this.statut,
    required this.depose,
    this.etudiantNom,
    this.etudiantMatricule,
    this.commentaireTraitement,
    this.traiteLe,
    this.corrige = false,
  });

  final String id;
  final String seanceId;
  final String matiere;
  final String classeCode;
  final DateTime seanceDebut;
  final TypeSignalement type;
  final String description;
  final StatutSignalement statut;
  final DateTime depose;

  final String? etudiantNom;
  final String? etudiantMatricule;
  final String? commentaireTraitement;
  final DateTime? traiteLe;

  /// `true` si l'acceptation a produit un relevé correctif.
  final bool corrige;

  bool get enAttente => statut == StatutSignalement.enAttente;

  factory Signalement.depuisJson(Map<String, dynamic> json) {
    DateTime? instant(String cle) {
      final brut = json[cle] as String?;
      return brut == null ? null : DateTime.tryParse(brut)?.toLocal();
    }

    return Signalement(
      id: json['id'] as String? ?? '',
      seanceId: json['seanceId'] as String? ?? '',
      matiere: json['matiereLibelle'] as String? ?? 'Séance',
      classeCode: json['classeCode'] as String? ?? '',
      seanceDebut: instant('seanceDebut') ?? DateTime.now(),
      type: typeSignalementDepuis(json['type'] as String?),
      description: json['description'] as String? ?? '',
      statut: statutSignalementDepuis(json['statut'] as String?),
      depose: instant('createdAt') ?? DateTime.now(),
      etudiantNom: json['etudiantNom'] as String?,
      etudiantMatricule: json['etudiantMatricule'] as String?,
      commentaireTraitement: json['commentaireTraitement'] as String?,
      traiteLe: instant('traiteLe'),
      corrige: json['presenceCorrectiveId'] != null,
    );
  }
}
