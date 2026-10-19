import 'package:flutter/material.dart';

import '../core/api/erreurs.dart';
import '../core/auth/auth_service.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import '../widgets/communs.dart';

/// Inscription libre d'un étudiant.
///
/// Le verrou n'est pas le compte (le candidat n'en a pas) mais le numéro CENOU, qui
/// doit figurer au référentiel de la scolarité et n'avoir jamais servi. Une fois
/// l'inscription acceptée, l'étudiant est connecté d'emblée.
class InscriptionScreen extends StatefulWidget {
  const InscriptionScreen({super.key, required this.auth});

  final AuthService auth;

  @override
  State<InscriptionScreen> createState() => _InscriptionScreenState();
}

class _ClasseOuverte {
  const _ClasseOuverte(this.id, this.libelle);
  final int id;
  final String libelle;
}

class _InscriptionScreenState extends State<InscriptionScreen> {
  /// Domaine que le serveur ajoute au numéro CENOU quand l'étudiant n'a pas
  /// d'email (`app.security.domaine-etudiants`). Il sert uniquement à ouvrir la
  /// session juste après l'inscription ; à défaut, l'étudiant revient à la connexion.
  static const _domaineParDefaut = 'etudiant.smartpresence.local';

  final _formulaire = GlobalKey<FormState>();
  final _cenou = TextEditingController();
  final _nom = TextEditingController();
  final _prenom = TextEditingController();
  final _email = TextEditingController();
  final _motDePasse = TextEditingController();

  List<_ClasseOuverte> _classes = const [];
  int? _classeId;
  bool _motDePasseVisible = false;
  bool _enCours = false;
  String? _erreur;

  @override
  void initState() {
    super.initState();
    _chargerClasses();
  }

  @override
  void dispose() {
    _cenou.dispose();
    _nom.dispose();
    _prenom.dispose();
    _email.dispose();
    _motDePasse.dispose();
    super.dispose();
  }

  Future<void> _chargerClasses() async {
    try {
      final donnees = await widget.auth.client.get('/inscription/classes');
      final liste = (donnees as List)
          .map((c) => _ClasseOuverte(
                (c['id'] as num).toInt(),
                '${c['libelle'] ?? c['code']}',
              ))
          .toList();
      if (mounted) setState(() => _classes = liste);
    } on ErreurApi {
      // Le choix de classe est facultatif (le référentiel peut l'imposer) :
      // son absence ne doit pas bloquer l'inscription.
    }
  }

  Future<void> _inscrire() async {
    if (!(_formulaire.currentState?.validate() ?? false)) return;
    setState(() {
      _erreur = null;
      _enCours = true;
    });

    final numero = _cenou.text.trim();
    final email = _email.text.trim();
    try {
      await widget.auth.client.post('/inscription', corps: {
        'numeroCenou': numero,
        'nom': _nom.text.trim(),
        'prenom': _prenom.text.trim(),
        if (email.isNotEmpty) 'email': email,
        if (_classeId != null) 'classeId': _classeId,
        'motDePasse': _motDePasse.text,
      });
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
      if (mounted) setState(() => _enCours = false);
      return;
    }

    final identifiant = email.isNotEmpty
        ? email.toLowerCase()
        : '${numero.toLowerCase()}@$_domaineParDefaut';
    try {
      await widget.auth.connecter(identifiant, _motDePasse.text);
      // La navigation est pilotée par l'état d'authentification.
      if (mounted) Navigator.of(context).popUntil((r) => r.isFirst);
    } on ErreurApi {
      if (!mounted) return;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('Inscription enregistrée. Vous pouvez vous connecter.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(gradient: Couleurs.degradeEntete),
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(Espaces.xxl),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Align(
                  alignment: Alignment.centerLeft,
                  child: IconButton(
                    onPressed: _enCours ? null : () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
                    tooltip: 'Retour',
                  ),
                ),
                const SizedBox(height: Espaces.md),
                Text('Créer mon compte',
                    style: Typo.titreEcran
                        .copyWith(color: Colors.white, fontSize: 26)),
                const SizedBox(height: Espaces.sm),
                Text(
                  'Votre numéro CENOU doit figurer au référentiel de la scolarité.',
                  style: Typo.corpsAttenue
                      .copyWith(color: Colors.white.withValues(alpha: 0.78)),
                ),
                const SizedBox(height: Espaces.xl),
                Carte(
                  ombre: Couleurs.ombreFlottante,
                  padding: const EdgeInsets.all(Espaces.xl),
                  enfant: Form(
                    key: _formulaire,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        if (_erreur != null) ...[
                          Bandeau.erreur(
                            message: _erreur!,
                            surFermeture: () => setState(() => _erreur = null),
                          ),
                          const SizedBox(height: Espaces.lg),
                        ],
                        _champ('Numéro CENOU', _cenou,
                            validateur: (v) => _requis(v, 'Saisissez votre numéro CENOU')),
                        _champ('Nom', _nom,
                            validateur: (v) => _requis(v, 'Saisissez votre nom'),
                            capitalisation: TextCapitalization.words),
                        _champ('Prénom', _prenom,
                            validateur: (v) => _requis(v, 'Saisissez votre prénom'),
                            capitalisation: TextCapitalization.words),
                        _champ('Email (facultatif)', _email,
                            clavier: TextInputType.emailAddress,
                            validateur: (v) => (v == null ||
                                    v.trim().isEmpty ||
                                    v.contains('@'))
                                ? null
                                : "Format d'email invalide"),
                        if (_classes.isNotEmpty) ...[
                          Text('Classe', style: Typo.suretitre),
                          const SizedBox(height: 6),
                          DropdownButtonFormField<int>(
                            initialValue: _classeId,
                            isExpanded: true,
                            decoration: const InputDecoration(
                                hintText: 'Choisissez votre classe'),
                            items: [
                              for (final c in _classes)
                                DropdownMenuItem(value: c.id, child: Text(c.libelle)),
                            ],
                            onChanged:
                                _enCours ? null : (v) => setState(() => _classeId = v),
                          ),
                          const SizedBox(height: Espaces.lg),
                        ],
                        Text('Mot de passe', style: Typo.suretitre),
                        const SizedBox(height: 6),
                        TextFormField(
                          controller: _motDePasse,
                          obscureText: !_motDePasseVisible,
                          enabled: !_enCours,
                          onFieldSubmitted: (_) => _enCours ? null : _inscrire(),
                          decoration: InputDecoration(
                            hintText: '8 caractères minimum',
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
                            ),
                          ),
                          validator: (v) => (v == null || v.length < 8)
                              ? 'Au moins 8 caractères'
                              : null,
                        ),
                        const SizedBox(height: Espaces.xl),
                        FilledButton(
                          onPressed: _enCours ? null : _inscrire,
                          child: _enCours
                              ? const SizedBox(
                                  width: 20,
                                  height: 20,
                                  child: CircularProgressIndicator(
                                      strokeWidth: 2.2, color: Colors.white),
                                )
                              : const Text("S'inscrire"),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String? _requis(String? v, String message) =>
      (v == null || v.trim().isEmpty) ? message : null;

  Widget _champ(
    String libelle,
    TextEditingController controleur, {
    required String? Function(String?) validateur,
    TextInputType? clavier,
    TextCapitalization capitalisation = TextCapitalization.none,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(libelle, style: Typo.suretitre),
          const SizedBox(height: 6),
          TextFormField(
            controller: controleur,
            enabled: !_enCours,
            keyboardType: clavier,
            textInputAction: TextInputAction.next,
            textCapitalization: capitalisation,
            autocorrect: false,
            validator: validateur,
          ),
        ],
      ),
    );
  }
}
