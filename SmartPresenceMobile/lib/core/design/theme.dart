import 'package:flutter/cupertino.dart' show CupertinoPageTransitionsBuilder;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'couleurs.dart';
import 'typographie.dart';

/// Thème de l'application, assemblé à partir des jetons de design.
///
/// Aucun composant ne redéfinit ses couleurs localement : tout descend d'ici, ce qui
/// garantit qu'un changement de teinte se propage à l'ensemble de l'application.
ThemeData themeSmartPresence() {
  final base = ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    scaffoldBackgroundColor: Couleurs.fond,
    colorScheme: const ColorScheme.light(
      primary: Couleurs.royal600,
      onPrimary: Colors.white,
      primaryContainer: Couleurs.royal50,
      onPrimaryContainer: Couleurs.royal700,
      secondary: Couleurs.royal500,
      surface: Couleurs.carte,
      onSurface: Couleurs.encre,
      error: Couleurs.danger,
      onError: Colors.white,
      outline: Couleurs.trait,
    ),
  );

  return base.copyWith(
    textTheme: base.textTheme.apply(
      bodyColor: Couleurs.encre,
      displayColor: Couleurs.encre,
    ),

    // Les écrans portent leur propre en-tête dégradé : l'AppBar Material ne sert
    // plus que de repli, et reste donc transparente.
    appBarTheme: AppBarTheme(
      backgroundColor: Colors.transparent,
      foregroundColor: Couleurs.encre,
      elevation: 0,
      scrolledUnderElevation: 0,
      surfaceTintColor: Colors.transparent,
      centerTitle: false,
      titleTextStyle: Typo.titreSection,
      systemOverlayStyle: SystemUiOverlayStyle.dark,
    ),

    cardTheme: CardThemeData(
      color: Couleurs.carte,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Rayons.carte),
      ),
    ),

    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: Couleurs.royal600,
        foregroundColor: Colors.white,
        // 48 px : au-delà du minimum d'accessibilité de 44, confortable au pouce.
        minimumSize: const Size.fromHeight(48),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Rayons.tuile)),
        textStyle: Typo.bouton,
      ),
    ),

    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: Couleurs.encreAttenuee,
        minimumSize: const Size.fromHeight(48),
        side: const BorderSide(color: Couleurs.trait),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Rayons.tuile)),
        textStyle: Typo.bouton.copyWith(color: Couleurs.encreAttenuee),
      ),
    ),

    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: Couleurs.royal600,
        textStyle: Typo.libelle.copyWith(color: Couleurs.royal600),
      ),
    ),

    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Couleurs.carte,
      contentPadding:
          const EdgeInsets.symmetric(horizontal: Espaces.lg, vertical: Espaces.lg),
      hintStyle: Typo.corps.copyWith(color: Couleurs.encrePale),
      labelStyle: Typo.libelle,
      border: _bordure(Couleurs.trait),
      enabledBorder: _bordure(Couleurs.trait),
      focusedBorder: _bordure(Couleurs.royal400, epaisseur: 1.5),
      errorBorder: _bordure(Couleurs.dangerTrait),
      focusedErrorBorder: _bordure(Couleurs.danger, epaisseur: 1.5),
      errorStyle: Typo.legende.copyWith(color: Couleurs.danger),
    ),

    dividerTheme: const DividerThemeData(
      color: Couleurs.traitLeger,
      thickness: 1,
      space: 1,
    ),


    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: Couleurs.indigo800,
      contentTextStyle: Typo.corps.copyWith(color: Colors.white),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Rayons.tuile)),
    ),

    // Transitions sobres et rapides : la fluidité tient à la brièveté, pas à l'effet.
    pageTransitionsTheme: const PageTransitionsTheme(builders: {
      TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
      TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
    }),
  );
}

OutlineInputBorder _bordure(Color couleur, {double epaisseur = 1}) {
  return OutlineInputBorder(
    borderRadius: BorderRadius.circular(Rayons.tuile),
    borderSide: BorderSide(color: couleur, width: epaisseur),
  );
}
