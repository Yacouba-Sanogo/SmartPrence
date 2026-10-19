import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/api/erreurs.dart';
import '../../core/auth/auth_service.dart';
import '../../core/design/couleurs.dart';
import '../../core/design/typographie.dart';
import '../../widgets/communs.dart';
import '../../widgets/entete.dart';

/// État de la demande d'enrôlement, tel que le serveur le rapporte.
class _Demande {
  const _Demande({
    required this.enrole,
    this.statut,
    this.code,
    this.lecteur,
    this.secondesRestantes = 0,
  });

  final bool enrole;
  final String? statut;
  final String? code;
  final String? lecteur;
  final int secondesRestantes;

  bool get enAttente => statut == 'EN_ATTENTE';

  factory _Demande.depuisJson(Map<String, dynamic> j) => _Demande(
        enrole: j['enrole'] == true,
        statut: j['statut'] as String?,
        code: j['code'] as String?,
        lecteur: j['lecteur'] as String?,
        secondesRestantes: (j['secondesRestantes'] as num?)?.toInt() ?? 0,
      );
}

/// Enregistrement de l'empreinte de l'étudiant, en libre-service.
///
/// L'étudiant ouvre une demande et reçoit un code à six chiffres. Le lecteur affiche
/// ce même code : il le compare, puis pose le doigt. L'application n'envoie, ne reçoit
/// ni ne conserve jamais d'empreinte — seul le capteur la lit.
class EmpreinteScreen extends StatefulWidget {
  const EmpreinteScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<EmpreinteScreen> createState() => _EmpreinteScreenState();
}

class _EmpreinteScreenState extends State<EmpreinteScreen> {
  _Demande? _demande;
  bool _chargement = true;
  bool _action = false;
  String? _erreur;
  Timer? _sondage;

  @override
  void initState() {
    super.initState();
    _actualiser();
  }

  @override
  void dispose() {
    _sondage?.cancel();
    super.dispose();
  }

  Future<void> _actualiser() async {
    try {
      final d = await widget.auth.client.get('/moi/enrolement');
      _appliquer(_Demande.depuisJson(d as Map<String, dynamic>));
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _chargement = false);
    }
  }

  void _appliquer(_Demande demande) {
    if (!mounted) return;
    setState(() {
      _demande = demande;
      _erreur = null;
    });
    // Tant que l'étudiant attend devant le capteur, l'écran bascule seul dès que
    // l'empreinte est prise.
    _sondage?.cancel();
    if (demande.enAttente) {
      _sondage = Timer.periodic(const Duration(seconds: 3), (_) => _actualiser());
    }
  }

  Future<void> _demander() async {
    setState(() {
      _action = true;
      _erreur = null;
    });
    try {
      final d = await widget.auth.client.post('/moi/enrolement');
      _appliquer(_Demande.depuisJson(d as Map<String, dynamic>));
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _action = false);
    }
  }

  Future<void> _annuler() async {
    setState(() => _action = true);
    try {
      await widget.auth.client.delete('/moi/enrolement');
      await _actualiser();
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _action = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Couleurs.fond,
      body: Column(
        children: [
          EnteteDegrade(
            debordement: 32,
            padding: const EdgeInsets.fromLTRB(
                Espaces.lg, Espaces.md, Espaces.lg, Espaces.md),
            enfant: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Mon empreinte',
                    style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 21)),
                const SizedBox(height: 3),
                Text(
                  'Elle vous identifie sur le lecteur, à l’entrée de la salle.',
                  style: Typo.legende.copyWith(
                      color: Colors.white.withValues(alpha: 0.72), fontSize: 12),
                ),
              ],
            ),
          ),
          Expanded(
            child: Transform.translate(
              offset: const Offset(0, -26),
              child: _chargement && _demande == null
                  ? const Center(
                      child: CircularProgressIndicator(color: Couleurs.royal600))
                  : RefreshIndicator(
                      onRefresh: _actualiser,
                      child: ListView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.fromLTRB(Espaces.md + 2,
                            Espaces.sm, Espaces.md + 2, Espaces.xxxl * 3),
                        children: [
                          _visuel(),
                          const SizedBox(height: Espaces.lg),
                          if (_erreur != null) ...[
                            Bandeau.erreur(message: _erreur!),
                            const SizedBox(height: Espaces.lg),
                          ],
                          _contenu(),
                          const SizedBox(height: Espaces.lg),
                          _confidentialite(),
                        ],
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _visuel() {
    final d = _demande;
    final fait = d?.enrole == true && d?.enAttente != true;

    return Container(
      height: 250,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(Rayons.bloc),
        gradient: const RadialGradient(
          colors: [Color(0xFF16337F), Color(0xFF071333)],
          radius: 0.95,
        ),
        boxShadow: Couleurs.ombreRoyal,
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        alignment: Alignment.center,
        children: [
          EmpreinteAnimee(balayage: !fait, acquise: fait),
          Positioned(
            bottom: 14,
            child: Text(
              fait
                  ? 'EMPREINTE ENREGISTRÉE'
                  : (d?.enAttente == true ? 'EN ATTENTE DU LECTEUR' : 'PRÊT À ENRÔLER'),
              style: Typo.suretitre.copyWith(
                  color: fait ? const Color(0xFF6EE7B7) : const Color(0xFF7DD3FC),
                  letterSpacing: 1.6),
            ),
          ),
        ],
      ),
    );
  }

  Widget _contenu() {
    final d = _demande;

    if (d != null && d.enAttente) return _attente(d);
    if (d != null && d.enrole) return _acquise();
    return _depart(expiree: d?.statut == 'EXPIREE');
  }

  Widget _depart({required bool expiree}) {
    return Carte(
      ombre: Couleurs.ombreFlottante,
      padding: const EdgeInsets.all(Espaces.xl),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(expiree ? 'Votre demande a expiré' : 'Enregistrer mon empreinte',
              style: Typo.titreSection),
          const SizedBox(height: Espaces.sm),
          Text(
            'Trois étapes, une seule fois : demandez un code, présentez-vous devant '
            'un lecteur, vérifiez que le code et votre nom affichés sont les vôtres, '
            'puis posez le doigt.',
            style: Typo.corpsAttenue,
          ),
          const SizedBox(height: Espaces.lg),
          _etape('1', 'Demandez votre code ci-dessous'),
          _etape('2', 'Rendez-vous devant un lecteur'),
          _etape('3', 'Vérifiez le code, puis posez le doigt'),
          const SizedBox(height: Espaces.lg),
          FilledButton.icon(
            onPressed: _action ? null : _demander,
            icon: _action
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.fingerprint_rounded),
            label: Text(expiree ? 'Refaire une demande' : 'Obtenir mon code'),
          ),
        ],
      ),
    );
  }

  Widget _attente(_Demande d) {
    final minutes = d.secondesRestantes ~/ 60;
    final secondes = (d.secondesRestantes % 60).toString().padLeft(2, '0');

    return Carte(
      ombre: Couleurs.ombreFlottante,
      padding: const EdgeInsets.all(Espaces.xl),
      enfant: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Votre code', style: Typo.suretitre, textAlign: TextAlign.center),
          const SizedBox(height: Espaces.sm),
          Text(
            (d.code ?? '------').split('').join(' '),
            textAlign: TextAlign.center,
            style: Typo.mono(38, graisse: FontWeight.w700, couleur: Couleurs.royal700),
          ),
          const SizedBox(height: Espaces.md),
          Text(
            d.lecteur == null
                ? 'Présentez-vous devant un lecteur : il affichera ce même code.'
                : 'Lecteur « ${d.lecteur} » : vérifiez que ce code s’affiche, puis posez le doigt.',
            textAlign: TextAlign.center,
            style: Typo.corpsAttenue,
          ),
          const SizedBox(height: Espaces.md),
          Text('Valable encore $minutes:$secondes',
              textAlign: TextAlign.center, style: Typo.legendePale),
          const SizedBox(height: Espaces.lg),
          OutlinedButton(
            onPressed: _action ? null : _annuler,
            child: const Text('Annuler ma demande'),
          ),
        ],
      ),
    );
  }

  Widget _acquise() {
    return Carte(
      ombre: Couleurs.ombreFlottante,
      padding: const EdgeInsets.all(Espaces.xl),
      enfant: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(
                color: Couleurs.succesFond, shape: BoxShape.circle),
            child: const Icon(Icons.check_rounded, color: Couleurs.succes),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Empreinte enregistrée', style: Typo.titreSection),
                const SizedBox(height: 3),
                Text(
                  'Posez simplement le doigt sur le lecteur pour signaler votre '
                  'présence. Un changement d’empreinte passe par la scolarité.',
                  style: Typo.corpsAttenue,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _etape(String numero, String texte) {
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.sm),
      child: Row(
        children: [
          Container(
            width: 24,
            height: 24,
            alignment: Alignment.center,
            decoration: const BoxDecoration(
                color: Couleurs.royal50, shape: BoxShape.circle),
            child: Text(numero,
                style: Typo.legende.copyWith(
                    fontWeight: FontWeight.w700, color: Couleurs.royal600)),
          ),
          const SizedBox(width: Espaces.md),
          Expanded(child: Text(texte, style: Typo.corps)),
        ],
      ),
    );
  }

  Widget _confidentialite() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const Icon(Icons.shield_outlined, size: 16, color: Couleurs.encreDiscrete),
        const SizedBox(width: 7),
        Flexible(
          child: Text(
            "L'application ne voit ni ne stocke jamais votre empreinte.",
            style: Typo.legendePale,
          ),
        ),
      ],
    );
  }
}

/// Empreinte tracée en arcs concentriques, traversée par un trait de balayage.
class EmpreinteAnimee extends StatefulWidget {
  const EmpreinteAnimee({super.key, this.balayage = true, this.acquise = false});

  final bool balayage;
  final bool acquise;

  @override
  State<EmpreinteAnimee> createState() => _EmpreinteAnimeeState();
}

class _EmpreinteAnimeeState extends State<EmpreinteAnimee>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(seconds: 3))
        ..repeat(reverse: true);

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _c,
      builder: (_, __) => CustomPaint(
        size: const Size(170, 200),
        painter: _PeintreEmpreinte(
          progression: Curves.easeInOut.transform(_c.value),
          balayage: widget.balayage,
          couleur: widget.acquise ? const Color(0xFF6EE7B7) : const Color(0xFF38BDF8),
        ),
      ),
    );
  }
}

class _PeintreEmpreinte extends CustomPainter {
  _PeintreEmpreinte(
      {required this.progression, required this.balayage, required this.couleur});

  final double progression;
  final bool balayage;
  final Color couleur;

  @override
  void paint(Canvas canvas, Size size) {
    final centre = Offset(size.width / 2, size.height * 0.52);
    final y = size.height * progression;

    final crete = Paint()
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = 3.2;

    // Chaque crête est une ellipse ouverte : la rotation et l'ouverture varient d'un
    // anneau à l'autre, ce qui donne le dessin irrégulier d'un doigt.
    for (var i = 1; i <= 11; i++) {
      final rx = 9.0 * i + 4;
      final ry = rx * 1.22;
      final rect = Rect.fromCenter(center: centre, width: rx * 2, height: ry * 2);
      final depart = math.pi * (0.9 + (i % 3) * 0.08);
      final balai = math.pi * (1.35 + (i % 4) * 0.12);

      // Une crête s'allume quand le trait de balayage passe à sa hauteur.
      final proche = balayage
          ? (1 - ((centre.dy - ry * 0.4) - y).abs() / 70).clamp(0.0, 1.0)
          : 0.0;
      crete.color = couleur.withValues(alpha: 0.38 + 0.62 * proche);
      canvas.drawArc(rect, depart, balai, false, crete);
      canvas.drawArc(rect, depart + balai + 0.55, math.pi * 0.28, false, crete);
    }

    if (balayage) {
      final trait = Paint()
        ..shader = LinearGradient(colors: [
          couleur.withValues(alpha: 0),
          couleur,
          couleur.withValues(alpha: 0),
        ]).createShader(Rect.fromLTWH(-40, y, size.width + 80, 2))
        ..strokeWidth = 2.4;
      canvas.drawLine(Offset(-40, y), Offset(size.width + 40, y), trait);
      canvas.drawRect(
        Rect.fromLTWH(-40, y - 26, size.width + 80, 26),
        Paint()
          ..shader = LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [couleur.withValues(alpha: 0), couleur.withValues(alpha: 0.18)],
          ).createShader(Rect.fromLTWH(-40, y - 26, size.width + 80, 26)),
      );
    }
  }

  @override
  bool shouldRepaint(_PeintreEmpreinte ancien) =>
      ancien.progression != progression ||
      ancien.balayage != balayage ||
      ancien.couleur != couleur;
}
