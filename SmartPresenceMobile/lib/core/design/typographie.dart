import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'couleurs.dart';

/// Échelle typographique SmartPresence.
///
/// **IBM Plex Sans** pour l'interface, **IBM Plex Mono** pour les heures, matricules et
/// pourcentages — les mêmes que l'administration web. Le monospace n'est pas un choix
/// esthétique : dans une liste de relevés, il aligne les chiffres à la colonne près et
/// rend la lecture d'une série d'horaires immédiate.
class Typo {
  Typo._();

  static TextStyle _sans(double taille, FontWeight graisse, Color couleur,
      {double? hauteur, double? interlettre}) {
    return GoogleFonts.ibmPlexSans(
      fontSize: taille,
      fontWeight: graisse,
      color: couleur,
      height: hauteur,
      letterSpacing: interlettre,
    );
  }

  /// Chiffres tabulaires : indispensable pour aligner heures et durées en colonne.
  static TextStyle mono(double taille,
      {FontWeight graisse = FontWeight.w500, Color couleur = Couleurs.encre}) {
    return GoogleFonts.ibmPlexMono(
      fontSize: taille,
      fontWeight: graisse,
      color: couleur,
      fontFeatures: const [FontFeature.tabularFigures()],
    );
  }

  static TextStyle get titreEcran =>
      _sans(24, FontWeight.w600, Couleurs.encre, hauteur: 1.2, interlettre: -0.4);

  static TextStyle get titreSection =>
      _sans(15, FontWeight.w600, Couleurs.encre, hauteur: 1.3);

  static TextStyle get corps => _sans(14, FontWeight.w400, Couleurs.encre, hauteur: 1.5);

  static TextStyle get corpsAttenue =>
      _sans(13.5, FontWeight.w400, Couleurs.encreAttenuee, hauteur: 1.5);

  static TextStyle get libelle =>
      _sans(13, FontWeight.w500, Couleurs.encre, hauteur: 1.3);

  static TextStyle get legende =>
      _sans(12, FontWeight.w400, Couleurs.encreDiscrete, hauteur: 1.4);

  static TextStyle get legendePale =>
      _sans(11.5, FontWeight.w400, Couleurs.encrePale, hauteur: 1.4);

  /// Intitulé de champ ou d'en-tête de section, en capitales espacées.
  static TextStyle get suretitre => _sans(11, FontWeight.w600, Couleurs.encreAttenuee,
      interlettre: 0.6);

  static TextStyle get bouton => _sans(14, FontWeight.w600, Colors.white);

  /// Grand nombre d'un indicateur.
  static TextStyle get chiffreCle => mono(28, graisse: FontWeight.w600);
}
