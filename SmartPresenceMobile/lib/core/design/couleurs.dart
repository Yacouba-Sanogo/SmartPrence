import 'package:flutter/material.dart';

/// Palette SmartPresence — bleu royal, blanc, gris.
///
/// Les trois couleurs du mémoire §6. Les valeurs sont **celles du web**, reprises
/// jeton pour jeton depuis ses variables `--color-royal-*`, `--color-ink-*` et
/// `--color-line-*` : deux clients de la même application ne peuvent pas afficher
/// deux bleus différents, et un relecteur de mémoire regarde d'abord cette
/// cohérence-là.
///
/// Une version antérieure avait glissé vers l'indigo, au motif qu'un bleu franc
/// paraît terne sur un petit écran. L'argument valait pour l'œil, pas pour le
/// document : la charte n'est pas un choix esthétique révisable. Les dégradés et
/// les ombres colorées rendent la profondeur recherchée sans quitter la famille.
///
/// Les trois teintes de statut — succès, alerte, danger — ne sont pas des couleurs
/// de marque mais des signaux : une moyenne sous 10 doit se voir sans qu'on lise le
/// chiffre. Elles restent volontairement sourdes, dans le registre d'un rapport
/// imprimé, pour rester subordonnées au bleu.
class Couleurs {
  Couleurs._();

  // ------------------------------------------------------------ Bleu royal
  //
  // Reprise exacte des jetons `--color-royal-*` du frontend Angular.
  static const Color royal50 = Color(0xFFEAF0FE);
  static const Color royal100 = Color(0xFFE4ECFC);
  static const Color royal200 = Color(0xFFC9D8F7);
  static const Color royal300 = Color(0xFFA8C0F1);
  static const Color royal400 = Color(0xFF5C81DD);
  static const Color royal500 = Color(0xFF2E5BD8);
  static const Color royal600 = Color(0xFF1B3FA0);
  static const Color royal700 = Color(0xFF17357F);
  static const Color royal800 = Color(0xFF132C73);
  static const Color royal900 = Color(0xFF0E205A);

  /// Anciens noms de l'épisode indigo, conservés le temps que les écrans migrent.
  ///
  /// Ils désignent désormais le bleu de la charte : un écran oublié revient donc
  /// dans le rang de lui-même, au lieu de rester seul en indigo.
  static const Color indigo50 = royal50;
  static const Color indigo100 = royal100;
  static const Color indigo200 = royal200;
  static const Color indigo300 = royal300;
  static const Color indigo400 = royal400;
  static const Color indigo500 = royal500;
  static const Color indigo600 = royal600;
  static const Color indigo700 = royal700;
  static const Color indigo800 = royal800;
  static const Color indigo900 = royal900;

  // ------------------------------------------------------- Encre et gris
  static const Color encre = Color(0xFF0F1729);
  static const Color encreAttenuee = Color(0xFF5D667F);
  static const Color encreDiscrete = Color(0xFF7A8399);
  static const Color encrePale = Color(0xFF9AA2B5);

  // ---------------------------------------------------- Surfaces et traits
  static const Color fond = Color(0xFFF4F6FB);
  static const Color carte = Color(0xFFFFFFFF);
  static const Color trait = Color(0xFFE2E7F0);
  static const Color traitLeger = Color(0xFFEDF0F6);
  static const Color traitPale = Color(0xFFF1F3F8);

  // ------------------------------------------------------------- Statuts
  //
  // Teintes du web, plus une variante légèrement soutenue pour les pastilles et
  // les anneaux : sur vingt pixels de large, la teinte de base manque de corps
  // sans pour autant justifier le néon de la version précédente.
  static const Color succes = Color(0xFF0F7355);
  static const Color succesVif = Color(0xFF14926B);
  static const Color succesFond = Color(0xFFE4F2EC);
  static const Color succesTrait = Color(0xFFC9DCD4);

  static const Color alerte = Color(0xFF96620A);
  static const Color alerteVif = Color(0xFFC18310);
  static const Color alerteFond = Color(0xFFFBF0DC);
  static const Color alerteTrait = Color(0xFFEBD8AC);

  static const Color danger = Color(0xFFA32B2B);
  static const Color dangerVif = Color(0xFFC43A3A);
  static const Color dangerFond = Color(0xFFF9EAEA);
  static const Color dangerTrait = Color(0xFFEDCFCF);

  // ------------------------------------------------------------ Dégradés

  /// En-tête principal : le produit s'ouvre toujours dessus.
  static const LinearGradient degradeEntete = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [royal900, royal800, royal600],
    stops: [0.0, 0.56, 1.0],
  );

  /// Barre de navigation flottante — plus dense que l'en-tête pour s'en détacher.
  static const LinearGradient degradeNavigation = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [royal900, royal700],
  );

  /// Action de signalement : le geste qui interrompt le cours normal des choses.
  ///
  /// L'orange de la version précédente introduisait une quatrième couleur, lue
  /// comme une seconde couleur de marque. Le rouge du jeton de danger suffit à
  /// dire l'exception sans sortir de la charte.
  static const LinearGradient degradeSignalement = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [dangerVif, Color(0xFF8E2323)],
  );

  // ---------------------------------------------------------------- Ombres

  /// Ombre des cartes posées sur le fond.
  static List<BoxShadow> get ombreCarte => const [
        BoxShadow(color: Color(0x0F0F1729), blurRadius: 12, offset: Offset(0, 3)),
      ];

  /// Ombre des éléments qui chevauchent l'en-tête, donc plus haut dans la pile.
  static List<BoxShadow> get ombreFlottante => const [
        BoxShadow(color: Color(0x140F1729), blurRadius: 20, offset: Offset(0, 6)),
      ];

  /// Ombre colorée sous un bloc bleu : une ombre grise y paraît sale, alors
  /// qu'une ombre de la même teinte l'ancre.
  static List<BoxShadow> get ombreRoyal => const [
        BoxShadow(color: Color(0x4D17357F), blurRadius: 20, offset: Offset(0, 8)),
      ];

  /// Ancien nom, le temps que les écrans migrent.
  static List<BoxShadow> get ombreIndigo => ombreRoyal;

  static List<BoxShadow> get ombreDanger => const [
        BoxShadow(color: Color(0x4DA32B2B), blurRadius: 20, offset: Offset(0, 8)),
      ];
}

/// Espacements, en pas de 4.
class Espaces {
  Espaces._();
  static const double xs = 4;
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double xl = 20;
  static const double xxl = 24;
  static const double xxxl = 32;
}

/// Rayons d'arrondi.
///
/// Plus généreux que sur le web : à la main, un angle vif paraît dur, et les
/// références retenues pour cette refonte tiennent largement leur douceur de là.
class Rayons {
  Rayons._();
  static const double sm = 10;
  static const double md = 14;
  static const double tuile = 16;
  static const double carte = 20;
  static const double bloc = 24;
  static const double pilule = 999;
}

/// Couleurs propres à l'identité de l'ENETP.
///
/// Elles ne rejoignent pas la palette applicative : ce sont celles de
/// l'établissement, reprises du drapeau malien, et elles n'ont pas à suivre les
/// évolutions du produit. Elles vivaient auprès du logo tant qu'il était redessiné ;
/// le logo étant désormais une image, leur place est ici.
class CouleursEnetp {
  CouleursEnetp._();

  static const Color vert = Color(0xFF16A34A);
  static const Color jaune = Color(0xFFF5C518);
  static const Color rouge = Color(0xFFDC2626);
  static const Color bleu = Color(0xFF1B4B9E);
  static const Color plume = Color(0xFFF7C948);
}
