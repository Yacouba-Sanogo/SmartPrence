import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../core/design/couleurs.dart';

/// Pastille discrète en haut de l'écran quand le réseau manque.
///
/// Elle ne déplace rien et ne capte aucun geste : elle prévient seulement que ce qui est
/// affiché date de la dernière connexion, et que les actions d'écriture attendront le retour du réseau.
class BandeauHorsLigne extends StatelessWidget {
  const BandeauHorsLigne({super.key, required this.etat});

  final ValueListenable<bool> etat;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      top: MediaQuery.paddingOf(context).top + 6,
      left: 0,
      right: 0,
      child: IgnorePointer(
        child: ValueListenableBuilder<bool>(
          valueListenable: etat,
          builder: (context, horsLigne, _) => AnimatedSlide(
            offset: horsLigne ? Offset.zero : const Offset(0, -3),
            duration: const Duration(milliseconds: 280),
            curve: Curves.easeOutCubic,
            child: AnimatedOpacity(
              opacity: horsLigne ? 1 : 0,
              duration: const Duration(milliseconds: 220),
              child: Center(
                child: Material(
                  color: Couleurs.royal800,
                  elevation: 3,
                  borderRadius: BorderRadius.circular(20),
                  child: const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.cloud_off_rounded,
                            size: 14, color: Colors.white),
                        SizedBox(width: 6),
                        Text('Hors ligne · dernières données connues',
                            style: TextStyle(
                                color: Colors.white,
                                fontSize: 11.5,
                                fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
