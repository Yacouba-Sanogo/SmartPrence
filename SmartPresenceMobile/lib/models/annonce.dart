import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

/// Nature d'une notification reçue.
enum TypeAnnonce { annonce, changement, seance, examen, convocation, information }

TypeAnnonce typeAnnonceDepuis(String? valeur) => switch (valeur) {
      'ANNONCE' => TypeAnnonce.annonce,
      'CHANGEMENT' => TypeAnnonce.changement,
      'SEANCE' => TypeAnnonce.seance,
      'EXAMEN' => TypeAnnonce.examen,
      'CONVOCATION' => TypeAnnonce.convocation,
      _ => TypeAnnonce.information,
    };

extension AffichageTypeAnnonce on TypeAnnonce {
  String get libelle => switch (this) {
        TypeAnnonce.annonce => 'Annonce',
        TypeAnnonce.changement => 'Changement',
        TypeAnnonce.seance => 'Séance',
        TypeAnnonce.examen => 'Examen',
        TypeAnnonce.convocation => 'Convocation',
        TypeAnnonce.information => 'Information',
      };

  IconData get icone => switch (this) {
        TypeAnnonce.annonce => Icons.campaign_rounded,
        TypeAnnonce.changement => Icons.swap_horiz_rounded,
        TypeAnnonce.seance => Icons.event_available_rounded,
        TypeAnnonce.examen => Icons.quiz_rounded,
        TypeAnnonce.convocation => Icons.mark_email_unread_rounded,
        TypeAnnonce.information => Icons.info_rounded,
      };

  /// Le changement est la seule teinte chaude : il demande une action, pas une lecture.
  Color get teinte => switch (this) {
        TypeAnnonce.changement => Couleurs.alerte,
        TypeAnnonce.examen => Couleurs.danger,
        TypeAnnonce.seance => Couleurs.succes,
        _ => Couleurs.indigo600,
      };

  Color get fond => switch (this) {
        TypeAnnonce.changement => Couleurs.alerteFond,
        TypeAnnonce.examen => Couleurs.dangerFond,
        TypeAnnonce.seance => Couleurs.succesFond,
        _ => Couleurs.royal50,
      };
}

/// Notification ou annonce reçue par l'enseignant.
class Annonce {
  const Annonce({
    required this.id,
    required this.type,
    required this.titre,
    required this.message,
    required this.lue,
    required this.recueLe,
  });

  final String id;
  final TypeAnnonce type;
  final String titre;
  final String message;
  final bool lue;
  final DateTime recueLe;

  Annonce commeLue() =>
      Annonce(id: id, type: type, titre: titre, message: message, lue: true, recueLe: recueLe);

  factory Annonce.depuisJson(Map<String, dynamic> json) => Annonce(
        id: json['id'] as String? ?? '',
        type: typeAnnonceDepuis(json['type'] as String?),
        titre: json['titre'] as String? ?? '',
        message: json['message'] as String? ?? '',
        lue: json['lue'] == true,
        recueLe: DateTime.tryParse(json['createdAt'] as String? ?? '')?.toLocal() ?? DateTime.now(),
      );
}
