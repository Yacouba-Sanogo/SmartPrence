import 'package:flutter/material.dart';

import '../../core/auth/auth_service.dart';
import '../../core/design/couleurs.dart';
import '../../widgets/navigation_flottante.dart';
import '../profil_screen.dart';
import 'activite_screen.dart';
import 'classes_screen.dart';
import 'seances_screen.dart';
import 'signalements_screen.dart';

/// Cadre de navigation de l'enseignant.
///
/// Cinq onglets qui suivent le déroulé réel d'une journée : ce que j'assure, qui je
/// vois, comment s'est passé mon mois, ce que j'ai signalé, qui je suis. L'application
/// mobile ne sert pas à administrer — les emplois du temps, la validation des notes et
/// la gestion des comptes restent du ressort de l'administration.
class ShellEnseignant extends StatefulWidget {
  const ShellEnseignant({super.key, required this.auth});

  final AuthService auth;

  @override
  State<ShellEnseignant> createState() => _ShellEnseignantState();
}

class _ShellEnseignantState extends State<ShellEnseignant> {
  int _onglet = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      extendBody: true,
      body: IndexedStack(
        index: _onglet,
        children: [
          SeancesScreen(auth: widget.auth),
          ClassesScreen(auth: widget.auth),
          ActiviteScreen(auth: widget.auth),
          SignalementsScreen(auth: widget.auth),
          ProfilScreen(auth: widget.auth),
        ],
      ),
      bottomNavigationBar: NavigationFlottante(
        indexActif: _onglet,
        surSelection: (index) => setState(() => _onglet = index),
        onglets: const [
          Onglet(
            icone: Icons.today_outlined,
            iconeActive: Icons.today_rounded,
            libelle: 'Journée',
          ),
          Onglet(
            icone: Icons.groups_outlined,
            iconeActive: Icons.groups_rounded,
            libelle: 'Classes',
          ),
          Onglet(
            icone: Icons.insights_outlined,
            iconeActive: Icons.insights_rounded,
            libelle: 'Activité',
          ),
          Onglet(
            icone: Icons.flag_outlined,
            iconeActive: Icons.flag_rounded,
            libelle: 'Signalements',
          ),
          Onglet(
            icone: Icons.person_outline_rounded,
            iconeActive: Icons.person_rounded,
            libelle: 'Profil',
          ),
        ],
      ),
    );
  }
}
