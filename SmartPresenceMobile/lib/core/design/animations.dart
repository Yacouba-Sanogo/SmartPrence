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

/// Nombre qui monte jusqu'à sa valeur à l'arrivée des données.
///
/// <p>Un compteur qui s'affiche d'un bloc se lit comme un texte ; celui qui s'égrène
/// se lit comme une mesure. Sur un bilan, c'est ce qui donne le sentiment que les
/// chiffres viennent d'être calculés plutôt que d'avoir été recopiés.</p>
class CompteurAnime extends StatelessWidget {
  const CompteurAnime({
    super.key,
    required this.valeur,
    required this.style,
    this.suffixe = '',
  });

  final int valeur;
  final TextStyle style;
  final String suffixe;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: valeur.toDouble()),
      duration: Durees.ample,
      curve: Courbes.sortie,
      builder: (_, v, __) => Text('${v.round()}$suffixe', style: style),
    );
  }
}

/// Barre de progression qui se remplit, et se réajuste en douceur quand la valeur change.
class BarreAnimee extends StatelessWidget {
  const BarreAnimee({
    super.key,
    required this.valeur,
    required this.couleur,
    this.fond,
    this.hauteur = 6,
  });

  /// De 0 à 1.
  final double valeur;
  final Color couleur;
  final Color? fond;
  final double hauteur;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(999),
      child: TweenAnimationBuilder<double>(
        tween: Tween(begin: 0, end: valeur.clamp(0, 1)),
        duration: Durees.ample,
        curve: Courbes.sortie,
        builder: (_, v, __) => LinearProgressIndicator(
          value: v,
          minHeight: hauteur,
          backgroundColor: fond ?? const Color(0xFFF1F3F8),
          color: couleur,
        ),
      ),
    );
  }
}

/// Enfonce légèrement son contenu sous le doigt.
///
/// <p>Le ressort au relâchement est ce qui distingue un bouton qui a « pris » d'une zone
/// qui n'a rien senti. Sans effet visible, l'utilisateur retouche, et ouvre deux fois.</p>
class PressionDouce extends StatefulWidget {
  const PressionDouce({super.key, required this.enfant, this.actif = true});

  final Widget enfant;
  final bool actif;

  @override
  State<PressionDouce> createState() => _PressionDouceState();
}

class _PressionDouceState extends State<PressionDouce> {
  bool _enfonce = false;

  void _regler(bool v) {
    if (widget.actif && _enfonce != v) setState(() => _enfonce = v);
  }

  @override
  Widget build(BuildContext context) {
    return Listener(
      onPointerDown: (_) => _regler(true),
      onPointerUp: (_) => _regler(false),
      onPointerCancel: (_) => _regler(false),
      child: AnimatedScale(
        scale: _enfonce ? 0.975 : 1,
        duration: Durees.rapide,
        curve: _enfonce ? Courbes.sortie : Curves.easeOutBack,
        child: widget.enfant,
      ),
    );
  }
}

/// Point qui bat doucement : « en direct », « sur site ».
class PointVivant extends StatefulWidget {
  const PointVivant({super.key, required this.couleur, this.taille = 9});

  final Color couleur;
  final double taille;

  @override
  State<PointVivant> createState() => _PointVivantState();
}

class _PointVivantState extends State<PointVivant> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1400),
  )..repeat(reverse: true);

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _c,
      builder: (_, __) => Container(
        width: widget.taille,
        height: widget.taille,
        decoration: BoxDecoration(
          color: widget.couleur,
          shape: BoxShape.circle,
          boxShadow: [
            BoxShadow(
              color: widget.couleur.withValues(alpha: 0.45 * (1 - _c.value)),
              blurRadius: 2 + 8 * _c.value,
              spreadRadius: 4 * _c.value,
            ),
          ],
        ),
      ),
    );
  }
}

/// Transition entre écrans : fondu et glissement léger vers le haut.
///
/// <p>Le changement de page par défaut est une translation latérale pleine largeur,
/// pensée pour la profondeur d'une pile. Pour descendre d'une liste à son détail, un
/// glissement court garde le repère et coûte moins à l'œil.</p>
Route<T> routeDouce<T>(WidgetBuilder construire) {
  return PageRouteBuilder<T>(
    transitionDuration: Durees.moyenne,
    reverseTransitionDuration: Durees.rapide,
    pageBuilder: (context, _, __) => construire(context),
    transitionsBuilder: (_, animation, __, enfant) {
      final courbe = CurvedAnimation(parent: animation, curve: Courbes.sortie);
      return FadeTransition(
        opacity: courbe,
        child: SlideTransition(
          position: Tween<Offset>(begin: const Offset(0, 0.04), end: Offset.zero).animate(courbe),
          child: enfant,
        ),
      );
    },
  );
}
