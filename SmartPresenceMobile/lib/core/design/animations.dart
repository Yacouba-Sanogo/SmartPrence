import 'package:flutter/material.dart';

/// Durées d'animation de l'application.
///
/// Trois valeurs, et pas une de plus. Une interface où chaque écran choisit sa
/// durée paraît mal réglée sans qu'on sache dire pourquoi : ce sont les petits
/// écarts, pas les grands, que l'œil relève.
///
/// Les bornes viennent de l'usage : sous 150 ms un mouvement se lit comme un
/// saut, au-delà de 500 ms il se lit comme une lenteur.
class Durees {
  Durees._();

  /// Réactions immédiates : bascule d'un onglet, rotation d'un chevron.
  static const Duration rapide = Duration(milliseconds: 180);

  /// Le régime ordinaire : dépliage, apparition, fondu d'un contenu.
  static const Duration moyenne = Duration(milliseconds: 280);

  /// Mouvements qu'on veut voir : remplissage d'un anneau, entrée d'une liste.
  static const Duration ample = Duration(milliseconds: 420);
}

/// Courbes d'accélération.
class Courbes {
  Courbes._();

  /// Départ vif, arrivée posée — le mouvement naturel d'un objet qu'on relâche.
  static const Curve sortie = Curves.easeOutCubic;

  /// Pour ce qui disparaît : l'inverse, sinon la sortie semble hésiter.
  static const Curve entree = Curves.easeInCubic;

  /// Les deux, pour un aller-retour dans le même geste.
  static const Curve douce = Curves.easeInOutCubic;
}

/// Fait entrer un élément de liste en fondu, légèrement décalé vers le bas.
///
/// <p>Le décalage par le rang produit une cascade : les cartes ne surgissent pas
/// toutes ensemble, elles se posent l'une après l'autre. L'effet n'est pas
/// décoratif — il donne à lire l'ordre de la liste avant même son contenu.</p>
///
/// <p>Le retard est plafonné : au-delà de la sixième carte, il ferait attendre
/// pour rien quelqu'un qui a fait défiler jusqu'en bas.</p>
class ApparitionEnCascade extends StatefulWidget {
  const ApparitionEnCascade({
    super.key,
    required this.rang,
    required this.enfant,
    this.decalage = const Duration(milliseconds: 55),
    this.rangsMax = 6,
  });

  final int rang;
  final Widget enfant;
  final Duration decalage;
  final int rangsMax;

  @override
  State<ApparitionEnCascade> createState() => _ApparitionEnCascadeState();
}

class _ApparitionEnCascadeState extends State<ApparitionEnCascade>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controleur = AnimationController(
    vsync: this,
    duration: Durees.ample,
  );

  late final Animation<double> _opacite =
      CurvedAnimation(parent: _controleur, curve: Courbes.sortie);

  late final Animation<Offset> _glissement = Tween<Offset>(
    begin: const Offset(0, 0.06),
    end: Offset.zero,
  ).animate(CurvedAnimation(parent: _controleur, curve: Courbes.sortie));

  @override
  void initState() {
    super.initState();
    final rang = widget.rang.clamp(0, widget.rangsMax);
    Future<void>.delayed(widget.decalage * rang, () {
      // L'écran peut avoir été quitté pendant le retard.
      if (mounted) _controleur.forward();
    });
  }

  @override
  void dispose() {
    _controleur.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _opacite,
      child: SlideTransition(position: _glissement, child: widget.enfant),
    );
  }
}

/// Remplace un contenu par un autre en fondu, sans faire sauter la mise en page.
///
/// <p>Employé là où un rechargement remplaçait le contenu par un indicateur de
/// progression : l'écran se vidait, puis se remplissait, et le regard perdait sa
/// place. Le fondu conserve le repère.</p>
class TransitionContenu extends StatelessWidget {
  const TransitionContenu({super.key, required this.enfant, this.duree});

  final Widget enfant;
  final Duration? duree;

  @override
  Widget build(BuildContext context) {
    return AnimatedSwitcher(
      duration: duree ?? Durees.moyenne,
      switchInCurve: Courbes.sortie,
      switchOutCurve: Courbes.entree,
      // Par défaut, l'entrant et le sortant se superposent et la pile prend la
      // taille du plus grand : la page tressaute. Ne garder que l'entrant en
      // mesure supprime le soubresaut.
      layoutBuilder: (entrant, sortants) => Stack(
        alignment: Alignment.topCenter,
        children: [
          ...sortants,
          if (entrant != null) entrant,
        ],
      ),
      child: enfant,
    );
  }
}
