import 'package:flutter/material.dart';

import '../../core/auth/auth_service.dart';
import '../../core/design/animations.dart';
import '../../core/design/couleurs.dart';
import '../../widgets/navigation_flottante.dart';
import '../profil_screen.dart';
import 'accueil_screen.dart';
import 'emploi_du_temps_screen.dart';
import 'historique_screen.dart';
import 'notes_screen.dart';

/// Cadre de navigation de l'étudiant.
///
/// Cinq onglets, qui répondent chacun à une question que l'étudiant se pose vraiment :
/// où j'en suis, où dois-je être, ce qui s'est passé, ce que je vaux, qui je suis.
/// Rien de plus — la navigation de l'enseignant est distincte, parce que son
/// métier l'est.
/// Rang des onglets, nommé.
///
/// L'accueil renvoie vers les autres écrans ; il le faisait par des nombres écrits
/// en clair, que l'insertion d'un onglet suffisait à rendre faux — sans que rien
/// ne le signale, ni à la compilation ni à l'exécution.
class OngletsEtudiant {
  OngletsEtudiant._();

  static const int accueil = 0;
  static const int emploiDuTemps = 1;
  static const int releves = 2;
  static const int notes = 3;
  static const int profil = 4;
}

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
      body: _pile([
        AccueilEtudiantScreen(auth: widget.auth, surNaviguer: _allerA),
        EmploiDuTempsScreen(auth: widget.auth),
        HistoriqueScreen(auth: widget.auth),
        NotesScreen(auth: widget.auth),
        ProfilScreen(auth: widget.auth),
      ]),
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
            icone: Icons.calendar_month_outlined,
            iconeActive: Icons.calendar_month_rounded,
            libelle: 'Cours',
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

  /// Les cinq écrans empilés, celui de l'onglet actif seul visible.
  ///
  /// <p>Un `IndexedStack` conserve bien l'état de chaque écran — la position
  /// de défilement, les données déjà chargées — mais bascule d'un coup sec. Un
  /// `AnimatedSwitcher`, lui, fondrait joliment en reconstruisant tout, donc
  /// en rechargeant à chaque aller-retour.</p>
  ///
  /// <p>Cette pile garde les deux : les écrans restent montés, et seule leur
  /// opacité change. `IgnorePointer` empêche un écran devenu transparent de
  /// capter les touchers, et `TickerMode` met en pause ses animations — sans
  /// quoi cinq écrans animés tourneraient en permanence.</p>
  Widget _pile(List<Widget> ecrans) {
    return Stack(
      children: [
        for (var index = 0; index < ecrans.length; index++)
          _calque(index: index, actif: index == _onglet, enfant: ecrans[index]),
      ],
    );
  }

  Widget _calque({required int index, required bool actif, required Widget enfant}) {
    return IgnorePointer(
      ignoring: !actif,
      child: AnimatedOpacity(
        opacity: actif ? 1 : 0,
        duration: Durees.rapide,
        curve: Courbes.douce,
        // TickerMode à l'intérieur du fondu, et non autour : placé au-dessus, il
        // coupait les tickers de l'écran sortant dès le début de sa disparition,
        // donc le fondu lui-même — qui restait figé à pleine opacité. L'écran
        // quitté demeurait alors visible par-dessus le nouveau.
        child: TickerMode(enabled: actif, child: enfant),
      ),
    );
  }

  void _allerA(int index) => setState(() => _onglet = index);
}
