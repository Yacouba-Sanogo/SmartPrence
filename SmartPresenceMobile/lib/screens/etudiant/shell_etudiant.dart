import 'package:flutter/material.dart';

import '../../core/auth/auth_service.dart';
import '../../core/design/couleurs.dart';
import '../../widgets/navigation_flottante.dart';
import '../profil_screen.dart';
import 'accueil_screen.dart';
import 'historique_screen.dart';
import 'notes_screen.dart';

/// Cadre de navigation de l'étudiant.
///
/// Quatre onglets, qui répondent chacun à une question que l'étudiant se pose vraiment :
/// où j'en suis, ce qui s'est passé, ce que je vaux, qui je suis. Rien de plus — la
/// navigation de l'enseignant est distincte, parce que son métier l'est.
class ShellEtudiant extends StatefulWidget {
  const ShellEtudiant({super.key, required this.auth});

  final AuthService auth;

  @override
  State<ShellEtudiant> createState() => _ShellEtudiantState();
}

class _ShellEtudiantState extends State<ShellEtudiant> {
  int _onglet = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      // extendBody laisse le contenu passer sous la barre flottante, qui devient
      // ainsi un élément posé sur la page plutôt qu'un bandeau qui la coupe.
      extendBody: true,
      body: IndexedStack(
        index: _onglet,
        children: [
          AccueilEtudiantScreen(auth: widget.auth, surNaviguer: _allerA),
          HistoriqueScreen(auth: widget.auth),
          NotesScreen(auth: widget.auth),
          ProfilScreen(auth: widget.auth),
        ],
      ),
      bottomNavigationBar: NavigationFlottante(
        indexActif: _onglet,
        surSelection: _allerA,
        onglets: const [
          Onglet(
            icone: Icons.home_outlined,
            iconeActive: Icons.home_rounded,
            libelle: 'Accueil',
          ),
          Onglet(
            icone: Icons.event_note_outlined,
            iconeActive: Icons.event_note_rounded,
            libelle: 'Relevés',
          ),
          Onglet(
            icone: Icons.school_outlined,
            iconeActive: Icons.school_rounded,
            libelle: 'Notes',
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

  void _allerA(int index) => setState(() => _onglet = index);
}
