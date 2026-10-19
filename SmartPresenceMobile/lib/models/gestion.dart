import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

enum TypeConge { conge, absence, maladie, formation }

TypeConge typeCongeDepuis(String? valeur) => switch (valeur) {
      'ABSENCE' => TypeConge.absence,
      'MALADIE' => TypeConge.maladie,
      'FORMATION' => TypeConge.formation,
      _ => TypeConge.conge,
    };

extension AffichageTypeConge on TypeConge {
  String get code => switch (this) {
        TypeConge.conge => 'CONGE',
        TypeConge.absence => 'ABSENCE',
        TypeConge.maladie => 'MALADIE',
        TypeConge.formation => 'FORMATION',
      };

  String get libelle => switch (this) {
        TypeConge.conge => 'Congé',
        TypeConge.absence => 'Absence',
        TypeConge.maladie => 'Maladie',
        TypeConge.formation => 'Formation',
      };

  IconData get icone => switch (this) {
        TypeConge.conge => Icons.beach_access_rounded,
        TypeConge.absence => Icons.event_busy_rounded,
        TypeConge.maladie => Icons.healing_rounded,
        TypeConge.formation => Icons.school_rounded,
      };
}

enum StatutDemande { enAttente, acceptee, refusee, annulee }

StatutDemande statutDemandeDepuis(String? valeur) => switch (valeur) {
      'ACCEPTEE' => StatutDemande.acceptee,
      'REFUSEE' => StatutDemande.refusee,
      'ANNULEE' => StatutDemande.annulee,
      _ => StatutDemande.enAttente,
    };

extension AffichageStatutDemande on StatutDemande {
  String get libelle => switch (this) {
        StatutDemande.enAttente => 'En attente',
        StatutDemande.acceptee => 'Acceptée',
        StatutDemande.refusee => 'Refusée',
        StatutDemande.annulee => 'Retirée',
      };

  Color get teinte => switch (this) {
        StatutDemande.enAttente => Couleurs.alerte,
        StatutDemande.acceptee => Couleurs.succes,
        StatutDemande.refusee => Couleurs.danger,
        StatutDemande.annulee => Couleurs.encreDiscrete,
      };

  Color get fond => switch (this) {
        StatutDemande.enAttente => Couleurs.alerteFond,
        StatutDemande.acceptee => Couleurs.succesFond,
        StatutDemande.refusee => Couleurs.dangerFond,
        StatutDemande.annulee => Couleurs.traitPale,
      };
}

/// Demande de congé ou d'absence d'un enseignant.
class DemandeConge {
  const DemandeConge({
    required this.id,
    required this.type,
    required this.debut,
    required this.fin,
    required this.motif,
    required this.statut,
    required this.seancesConcernees,
    this.reponse,
  });

  final String id;
  final TypeConge type;
  final DateTime debut;
  final DateTime fin;
  final String motif;
  final StatutDemande statut;
  final int seancesConcernees;
  final String? reponse;

  int get jours => fin.difference(debut).inDays + 1;

  factory DemandeConge.depuisJson(Map<String, dynamic> json) => DemandeConge(
        id: json['id'] as String? ?? '',
        type: typeCongeDepuis(json['type'] as String?),
        debut: DateTime.tryParse(json['debut'] as String? ?? '') ?? DateTime.now(),
        fin: DateTime.tryParse(json['fin'] as String? ?? '') ?? DateTime.now(),
        motif: json['motif'] as String? ?? '',
        statut: statutDemandeDepuis(json['statut'] as String?),
        seancesConcernees: (json['seancesConcernees'] as num?)?.toInt() ?? 0,
        reponse: json['reponse'] as String?,
      );
}

enum StatutReclamation { ouverte, acceptee, rejetee }

StatutReclamation statutReclamationDepuis(String? valeur) => switch (valeur) {
      'ACCEPTEE' => StatutReclamation.acceptee,
      'REJETEE' => StatutReclamation.rejetee,
      _ => StatutReclamation.ouverte,
    };

extension AffichageStatutReclamation on StatutReclamation {
  String get libelle => switch (this) {
        StatutReclamation.ouverte => 'En cours',
        StatutReclamation.acceptee => 'Acceptée',
        StatutReclamation.rejetee => 'Rejetée',
      };

  Color get teinte => switch (this) {
        StatutReclamation.ouverte => Couleurs.alerte,
        StatutReclamation.acceptee => Couleurs.succes,
        StatutReclamation.rejetee => Couleurs.danger,
      };

  Color get fond => switch (this) {
        StatutReclamation.ouverte => Couleurs.alerteFond,
        StatutReclamation.acceptee => Couleurs.succesFond,
        StatutReclamation.rejetee => Couleurs.dangerFond,
      };
}

/// Contestation d'une note, vue par l'étudiant qui l'a déposée comme par l'enseignant
/// qui la reçoit.
class Reclamation {
  const Reclamation({
    required this.id,
    required this.noteId,
    required this.matiere,
    required this.libelleNote,
    required this.valeurActuelle,
    required this.etudiantNom,
    required this.etudiantMatricule,
    required this.motif,
    required this.statut,
    required this.creeLe,
    this.reponse,
    this.valeurAvant,
    this.valeurApres,
  });

  final String id;
  final String noteId;
  final String matiere;
  final String libelleNote;
  final double valeurActuelle;
  final String etudiantNom;
  final String etudiantMatricule;
  final String motif;
  final StatutReclamation statut;
  final DateTime creeLe;
  final String? reponse;
  final double? valeurAvant;
  final double? valeurApres;

  bool get ouverte => statut == StatutReclamation.ouverte;
  bool get corrigee => valeurAvant != null && valeurApres != null;

  factory Reclamation.depuisJson(Map<String, dynamic> json) => Reclamation(
        id: json['id'] as String? ?? '',
        noteId: json['noteId'] as String? ?? '',
        matiere: json['matiereLibelle'] as String? ?? '',
        libelleNote: json['libelleNote'] as String? ?? '',
        valeurActuelle: (json['valeurActuelle'] as num?)?.toDouble() ?? 0,
        etudiantNom: json['etudiantNom'] as String? ?? '',
        etudiantMatricule: json['etudiantMatricule'] as String? ?? '',
        motif: json['motif'] as String? ?? '',
        statut: statutReclamationDepuis(json['statut'] as String?),
        creeLe: DateTime.tryParse(json['creeLe'] as String? ?? '')?.toLocal() ?? DateTime.now(),
        reponse: json['reponse'] as String?,
        valeurAvant: (json['valeurAvant'] as num?)?.toDouble(),
        valeurApres: (json['valeurApres'] as num?)?.toDouble(),
      );
}

/// `12` ou `12,5` : virgule décimale, pas de zéro inutile.
String formaterNote(double v) =>
    (v == v.roundToDouble() ? v.toStringAsFixed(0) : v.toStringAsFixed(2)).replaceAll('.', ',');

/// Support de cours publié pour une classe.
class SupportCours {
  const SupportCours({
    required this.id,
    required this.matiere,
    required this.enseignantNom,
    required this.titre,
    required this.nomFichier,
    required this.taille,
    required this.publieLe,
    this.description,
  });

  final String id;
  final String matiere;
  final String enseignantNom;
  final String titre;
  final String? description;
  final String nomFichier;
  final int taille;
  final DateTime publieLe;

  String get extension {
    final i = nomFichier.lastIndexOf('.');
    return i < 0 ? '' : nomFichier.substring(i + 1).toLowerCase();
  }

  /// `1,2 Mo`, `340 Ko` : l'ordre de grandeur suffit pour décider de télécharger ou non.
  String get tailleLisible {
    if (taille < 1024) return '$taille o';
    if (taille < 1024 * 1024) return '${(taille / 1024).round()} Ko';
    return '${(taille / 1024 / 1024).toStringAsFixed(1).replaceAll('.', ',')} Mo';
  }

  /// Pictogramme et teinte selon le format : on reconnaît un PDF avant de lire son nom.
  (IconData, Color) get apparence => switch (extension) {
        'pdf' => (Icons.picture_as_pdf_rounded, Couleurs.dangerVif),
        'doc' || 'docx' || 'odt' || 'txt' => (Icons.description_rounded, Couleurs.royal500),
        'ppt' || 'pptx' || 'odp' => (Icons.slideshow_rounded, Couleurs.alerteVif),
        'xls' || 'xlsx' || 'ods' || 'csv' => (Icons.table_chart_rounded, Couleurs.succesVif),
        'png' || 'jpg' || 'jpeg' => (Icons.image_rounded, Couleurs.royal400),
        'zip' => (Icons.folder_zip_rounded, Couleurs.encreAttenuee),
        _ => (Icons.insert_drive_file_rounded, Couleurs.encreAttenuee),
      };

  factory SupportCours.depuisJson(Map<String, dynamic> json) => SupportCours(
        id: json['id'] as String? ?? '',
        matiere: json['matiereLibelle'] as String? ?? '',
        enseignantNom: json['enseignantNom'] as String? ?? '',
        titre: json['titre'] as String? ?? '',
        description: json['description'] as String?,
        nomFichier: json['nomFichier'] as String? ?? '',
        taille: (json['taille'] as num?)?.toInt() ?? 0,
        publieLe: DateTime.tryParse(json['publieLe'] as String? ?? '')?.toLocal() ?? DateTime.now(),
      );
}
