import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

/// Statut d'un relevé de présence.
enum StatutPresence { present, retard, absent, justifie, inconnu }

StatutPresence statutDepuis(String? valeur) => switch (valeur) {
      'PRESENT' => StatutPresence.present,
      'RETARD' => StatutPresence.retard,
      'ABSENT' => StatutPresence.absent,
      'JUSTIFIE' => StatutPresence.justifie,
      _ => StatutPresence.inconnu,
    };

extension AffichageStatut on StatutPresence {
  String get libelle => switch (this) {
        StatutPresence.present => 'Présent',
        StatutPresence.retard => 'Retard',
        StatutPresence.absent => 'Absent',
        StatutPresence.justifie => 'Justifié',
        StatutPresence.inconnu => 'Inconnu',
      };

  Color get teinte => switch (this) {
        StatutPresence.present => Couleurs.succes,
        StatutPresence.retard => Couleurs.alerte,
        StatutPresence.absent => Couleurs.danger,
        StatutPresence.justifie => Couleurs.royal600,
        StatutPresence.inconnu => Couleurs.encreDiscrete,
      };

  Color get fond => switch (this) {
        StatutPresence.present => Couleurs.succesFond,
        StatutPresence.retard => Couleurs.alerteFond,
        StatutPresence.absent => Couleurs.dangerFond,
        StatutPresence.justifie => Couleurs.royal50,
        StatutPresence.inconnu => Couleurs.traitPale,
      };
}

/// Relevé de présence d'un étudiant à une séance.
class Presence {
  const Presence({
    required this.id,
    required this.date,
    required this.heure,
    required this.statut,
    this.matiere,
    this.salle,
    this.source,
  });

  final String id;
  final DateTime date;

  /// Heure au format `HH:mm`, prête à l'affichage.
  final String heure;
  final StatutPresence statut;
  final String? matiere;
  final String? salle;
  final String? source;

  /// `true` si le relevé provient d'une saisie administrative et non du capteur.
  bool get estRegularise => source == 'MANUEL';

  factory Presence.depuisJson(Map<String, dynamic> json) {
    final heureBrute = json['heurePresence'] as String? ?? '';
    return Presence(
      id: json['id'] as String? ?? '',
      date: DateTime.tryParse(json['datePresence'] as String? ?? '') ?? DateTime.now(),
      heure: heureBrute.length >= 5 ? heureBrute.substring(0, 5) : heureBrute,
      statut: statutDepuis(json['statut'] as String?),
      matiere: json['matiereLibelle'] as String?,
      salle: json['salleNom'] as String?,
      source: json['source'] as String?,
    );
  }
}

/// Page de résultats renvoyée par le backend.
class PagePresences {
  const PagePresences({required this.elements, required this.derniere, required this.total});

  final List<Presence> elements;
  final bool derniere;
  final int total;

  factory PagePresences.depuisJson(Map<String, dynamic> json) {
    return PagePresences(
      elements: ((json['content'] as List<dynamic>?) ?? const [])
          .map((e) => Presence.depuisJson(e as Map<String, dynamic>))
          .toList(),
      derniere: json['last'] as bool? ?? true,
      total: (json['totalElements'] as num?)?.toInt() ?? 0,
    );
  }

  static const PagePresences vide =
      PagePresences(elements: [], derniere: true, total: 0);
}
