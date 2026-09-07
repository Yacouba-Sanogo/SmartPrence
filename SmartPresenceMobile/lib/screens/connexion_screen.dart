import 'package:flutter/material.dart';

import '../core/api/erreurs.dart';
import '../core/auth/auth_service.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../widgets/communs.dart';

/// Écran de connexion — seule porte d'entrée de l'application.
class ConnexionScreen extends StatefulWidget {
  const ConnexionScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<ConnexionScreen> createState() => _ConnexionScreenState();
}

class _ConnexionScreenState extends State<ConnexionScreen> {
  final _formulaire = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _motDePasse = TextEditingController();

  bool _motDePasseVisible = false;
  String? _erreur;

  @override
  void dispose() {
    _email.dispose();
    _motDePasse.dispose();
    super.dispose();
  }

  Future<void> _connecter() async {
    if (!(_formulaire.currentState?.validate() ?? false)) return;
    setState(() => _erreur = null);

    try {
      await widget.auth.connecter(_email.text, _motDePasse.text);
      // La navigation est pilotée par l'état d'authentification, pas d'ici.
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final clavierOuvert = MediaQuery.viewInsetsOf(context).bottom > 0;

    return Scaffold(
      // Le dégradé occupe tout l'écran : c'est la seule page où le produit se
      // présente avant de servir, et un fond gris y ferait pauvre.
      body: Container(
        decoration: const BoxDecoration(gradient: Couleurs.degradeEntete),
        // StackFit.expand : sans cela, le Stack se dimensionne sur son contenu et le
        // dégradé s'arrête au bas du formulaire, laissant une bande grise.
        child: Stack(
          fit: StackFit.expand,
          children: [
            Positioned(
              top: -70,
              right: -60,
              child: Container(
                width: 220,
                height: 220,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white.withValues(alpha: 0.06),
                ),
              ),
            ),
            Positioned(
              bottom: -90,
              left: -70,
              child: Container(
                width: 260,
                height: 260,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white.withValues(alpha: 0.05),
                ),
              ),
            ),
            SafeArea(
        child: ListenableBuilder(
          listenable: widget.auth,
          builder: (context, _) {
            final enCours = widget.auth.enCours;

            return SingleChildScrollView(
              padding: const EdgeInsets.symmetric(
                  horizontal: Espaces.xxl, vertical: Espaces.xxl),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // L'en-tête s'efface quand le clavier monte : sur un petit écran,
                  // le formulaire doit rester entièrement visible.
                  AnimatedSize(
                    duration: const Duration(milliseconds: 200),
                    curve: Curves.easeOut,
                    child: clavierOuvert ? const SizedBox(height: Espaces.sm) : _enTete(),
                  ),

                  const SizedBox(height: Espaces.xxl),

                  Carte(
                    ombre: Couleurs.ombreFlottante,
                    padding: const EdgeInsets.all(Espaces.xl),
                    enfant: Form(
                      key: _formulaire,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text('Connexion', style: Typo.titreEcran),
                          const SizedBox(height: Espaces.xs),
                          Text(
                            'Accédez à vos relevés de présence.',
                            style: Typo.corpsAttenue,
                          ),

                          if (_erreur != null) ...[
                            const SizedBox(height: Espaces.lg),
                            Bandeau.erreur(
                              message: _erreur!,
                              surFermeture: () => setState(() => _erreur = null),
                            ),
                          ],

                          const SizedBox(height: Espaces.xl),
                          Text('Adresse email', style: Typo.suretitre),
                          const SizedBox(height: 6),
                          TextFormField(
                            controller: _email,
                            keyboardType: TextInputType.emailAddress,
                            textInputAction: TextInputAction.next,
                            autocorrect: false,
                            autofillHints: const [AutofillHints.username],
                            enabled: !enCours,
                            decoration: const InputDecoration(
                                hintText: 'prenom.nom@univ.ml'),
                            validator: (valeur) =>
                                (valeur == null || valeur.trim().isEmpty)
                                    ? 'Saisissez votre adresse email'
                                    : null,
                          ),

                          const SizedBox(height: Espaces.lg),
                          Text('Mot de passe', style: Typo.suretitre),
                          const SizedBox(height: 6),
                          TextFormField(
                            controller: _motDePasse,
                            obscureText: !_motDePasseVisible,
                            textInputAction: TextInputAction.done,
                            autofillHints: const [AutofillHints.password],
                            enabled: !enCours,
                            onFieldSubmitted: (_) => enCours ? null : _connecter(),
                            decoration: InputDecoration(
                              hintText: '••••••••',
                              suffixIcon: IconButton(
                                onPressed: () => setState(
                                    () => _motDePasseVisible = !_motDePasseVisible),
                                icon: Icon(
                                  _motDePasseVisible
                                      ? Icons.visibility_off_outlined
                                      : Icons.visibility_outlined,
                                  size: 20,
                                  color: Couleurs.encreDiscrete,
                                ),
                                tooltip: _motDePasseVisible ? 'Masquer' : 'Afficher',
                              ),
                            ),
                            validator: (valeur) => (valeur == null || valeur.isEmpty)
                                ? 'Saisissez votre mot de passe'
                                : null,
                          ),

                          const SizedBox(height: Espaces.xl),
                          FilledButton(
                            onPressed: enCours ? null : _connecter,
                            child: enCours
                                ? const SizedBox(
                                    width: 20,
                                    height: 20,
                                    child: CircularProgressIndicator(
                                        strokeWidth: 2.2, color: Colors.white),
                                  )
                                : const Text('Se connecter'),
                          ),
                        ],
                      ),
                    ),
                  ),

                  const SizedBox(height: Espaces.xl),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.shield_outlined,
                          size: 16, color: Colors.white.withValues(alpha: 0.55)),
                      const SizedBox(width: 7),
                      Flexible(
                        child: Text(
                          "Aucune donnée biométrique n'est stockée.",
                          style: Typo.legendePale
                              .copyWith(color: Colors.white.withValues(alpha: 0.55)),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            );
          },
        ),
      ),
          ],
        ),
      ),
    );
  }

  Widget _enTete() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: Espaces.xl),
        Row(
          children: [
            Container(
              width: 46,
              height: 46,
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.18),
                borderRadius: BorderRadius.circular(Rayons.md + 2),
              ),
              child: const Icon(Icons.fingerprint_rounded,
                  color: Colors.white, size: 25),
            ),
            const SizedBox(width: Espaces.md),
            Text('SmartPresence',
                style: Typo.titreEcran.copyWith(color: Colors.white, fontSize: 20)),
          ],
        ),
        const SizedBox(height: Espaces.xl),
        Text(
          "La présence, établie par l'empreinte.",
          style: Typo.titreEcran
              .copyWith(color: Colors.white, fontSize: 26, height: 1.25),
        ),
        const SizedBox(height: Espaces.sm),
        Text(
          "L'identification se fait sur le capteur, à l'entrée. Le serveur ne reçoit "
          "qu'un identifiant et une heure — jamais une empreinte.",
          style: Typo.corpsAttenue
              .copyWith(color: Colors.white.withValues(alpha: 0.78)),
        ),
      ],
    );
  }
}
