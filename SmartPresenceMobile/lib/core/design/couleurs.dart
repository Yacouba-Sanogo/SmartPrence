import 'package:flutter/material.dart';

/// Palette SmartPresence — indigo profond.
///
/// Le document de mémoire §6 impose **bleu royal, blanc, gris**. L'indigo retenu ici
/// reste dans cette famille : il en garde la teinte, mais gagne en saturation et en
/// profondeur, ce que réclame une interface mobile où l'écran est petit et regardé de
/// près. Un bleu trop pâle y paraît terne et administratif.
///
/// Les dégradés ne sont pas décoratifs : ils créent la hiérarchie que la couleur plate
/// ne donne pas — un en-tête qui s'enfonce, des cartes qui flottent au-dessus.
class Couleurs {
  Couleurs._();

  // ---------------------------------------------------------------- Indigo
  static const Color indigo50 = Color(0xFFEEF0FE);
  static const Color indigo100 = Color(0xFFE0E4FC);
  static const Color indigo200 = Color(0xFFC3CBF8);
  static const Color indigo300 = Color(0xFF97A3F2);
  static const Color indigo400 = Color(0xFF6C7BFF);
  static const Color indigo500 = Color(0xFF5B6BFF);
  static const Color indigo600 = Color(0xFF4356E8);
  static const Color indigo700 = Color(0xFF2F3FB8);
  static const Color indigo800 = Color(0xFF241C6B);
  static const Color indigo900 = Color(0xFF171246);

  /// Alias conservés pour ne pas casser les écrans existants d'un seul coup.
  static const Color royal50 = indigo50;
  static const Color royal100 = indigo100;
  static const Color royal200 = indigo200;
  static const Color royal400 = indigo400;
  static const Color royal500 = indigo500;
  static const Color royal600 = indigo600;
  static const Color royal700 = indigo700;
  static const Color royal800 = indigo800;
  static const Color royal900 = indigo900;

  // ------------------------------------------------------- Encre et gris
  static const Color encre = Color(0xFF0D1030);
  static const Color encreAttenuee = Color(0xFF4A5070);
  static const Color encreDiscrete = Color(0xFF6B7192);
  static const Color encrePale = Color(0xFF9AA0BD);

  // ---------------------------------------------------- Surfaces et traits
  static const Color fond = Color(0xFFF4F5FC);
  static const Color carte = Color(0xFFFFFFFF);
  static const Color trait = Color(0xFFE2E5F3);
  static const Color traitLeger = Color(0xFFEDEFF8);
  static const Color traitPale = Color(0xFFF2F3FB);

  // ------------------------------------------------------------- Statuts
  //
  // Plus vifs que des teintes de rapport imprimé : sur une pastille de 20 px, une
  // couleur sourde devient illisible.
  static const Color succes = Color(0xFF0E9B72);
  static const Color succesVif = Color(0xFF12B886);
  static const Color succesFond = Color(0xFFE4F8F1);
  static const Color succesTrait = Color(0xFFBCE9D9);

  static const Color alerte = Color(0xFFB4780A);
  static const Color alerteVif = Color(0xFFF59F0A);
  static const Color alerteFond = Color(0xFFFEF3DC);
  static const Color alerteTrait = Color(0xFFFBE3B8);

  static const Color danger = Color(0xFFD6304A);
  static const Color dangerVif = Color(0xFFF2506A);
  static const Color dangerFond = Color(0xFFFEE9ED);
  static const Color dangerTrait = Color(0xFFF9C9D2);

  // ------------------------------------------------------------ Dégradés

  /// En-tête principal : le produit s'ouvre toujours dessus.
  static const LinearGradient degradeEntete = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [indigo800, indigo700, indigo600],
    stops: [0.0, 0.56, 1.0],
  );

  /// Barre de navigation flottante — plus dense que l'en-tête pour s'en détacher.
  static const LinearGradient degradeNavigation = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [indigo800, Color(0xFF3A4AD4)],
  );

  /// Action de signalement : la seule teinte chaude de l'application, réservée au
  /// geste qui interrompt le cours normal des choses.
  static const LinearGradient degradeSignalement = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [dangerVif, Color(0xFFFF7A5C)],
  );

  // ---------------------------------------------------------------- Ombres

  /// Ombre des cartes posées sur le fond.
  static List<BoxShadow> get ombreCarte => const [
        BoxShadow(color: Color(0x0F141846), blurRadius: 12, offset: Offset(0, 3)),
      ];

  /// Ombre des éléments qui chevauchent l'en-tête, donc plus haut dans la pile.
  static List<BoxShadow> get ombreFlottante => const [
        BoxShadow(color: Color(0x14141846), blurRadius: 20, offset: Offset(0, 6)),
      ];

  /// Ombre colorée d'un élément indigo : une ombre grise sous un bloc coloré
  /// paraît sale, alors qu'une ombre de la même teinte l'ancre.
  static List<BoxShadow> get ombreIndigo => const [
        BoxShadow(color: Color(0x522D3AB4), blurRadius: 20, offset: Offset(0, 8)),
      ];

  static List<BoxShadow> get ombreDanger => const [
        BoxShadow(color: Color(0x66F2506A), blurRadius: 20, offset: Offset(0, 8)),
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
