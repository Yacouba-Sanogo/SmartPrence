import 'package:flutter/material.dart';

import '../core/api/client_api.dart';
import '../core/api/erreurs.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import 'communs.dart';
import 'formulaire.dart';

/// Ouvre le changement de mot de passe et renvoie `true` s'il a abouti.
///
/// Les comptes reçoivent un mot de passe initial de l'administration : le premier geste
/// raisonnable d'un nouvel utilisateur est de le remplacer par un mot de passe à lui.
Future<bool> ouvrirChangementMotDePasse(BuildContext context, ClientApi client) async {
  final ok = await ouvrirFormulaire<bool>(context, (_) => _Formulaire(client: client));
  return ok ?? false;
}

class _Formulaire extends StatefulWidget {
  const _Formulaire({required this.client});

  final ClientApi client;

  @override
  State<_Formulaire> createState() => _FormulaireState();
}

class _FormulaireState extends State<_Formulaire> {
  final _actuel = TextEditingController();
  final _nouveau = TextEditingController();
  final _confirmation = TextEditingController();
  bool _voir = false;
  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() {
    _actuel.dispose();
    _nouveau.dispose();
    _confirmation.dispose();
    super.dispose();
  }

  String? get _probleme {
    if (_nouveau.text.isNotEmpty && _nouveau.text.length < 8) return 'Au moins 8 caractères.';
    if (_confirmation.text.isNotEmpty && _confirmation.text != _nouveau.text) {
      return 'Les deux mots de passe ne correspondent pas.';
    }
    return null;
  }

  bool get _valide =>
      _actuel.text.isNotEmpty &&
      _nouveau.text.length >= 8 &&
      _confirmation.text == _nouveau.text;

  Future<void> _envoyer() async {
    if (!_valide || _envoi) return;
    setState(() {
      _envoi = true;
      _erreur = null;
    });
    try {
      await widget.client.patch('/auth/mot-de-passe', corps: {
        'motDePasseActuel': _actuel.text,
        'nouveauMotDePasse': _nouveau.text,
      });
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ErreurApi catch (e) {
      if (!mounted) return;
      setState(() {
        _erreur = e.message;
        _envoi = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return CadreFormulaire(
      titre: 'Changer mon mot de passe',
      sousTitre: 'Choisissez un mot de passe que vous êtes seul à connaître.',
      enfants: [
        if (_erreur != null) ...[
          Bandeau.erreur(message: _erreur!),
          const SizedBox(height: Espaces.md),
        ],
        const EtiquetteChamp('Mot de passe actuel'),
        _champ(_actuel, 'Mot de passe actuel'),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Nouveau mot de passe'),
        _champ(_nouveau, '8 caractères minimum'),
        const SizedBox(height: Espaces.lg),
        const EtiquetteChamp('Confirmation'),
        _champ(_confirmation, 'Saisissez-le à nouveau'),
        if (_probleme != null)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: Text(_probleme!, style: Typo.legende.copyWith(color: Couleurs.danger)),
          ),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            onPressed: () => setState(() => _voir = !_voir),
            icon: Icon(_voir ? Icons.visibility_off_rounded : Icons.visibility_rounded, size: 17),
            label: Text(_voir ? 'Masquer' : 'Afficher'),
          ),
        ),
        const SizedBox(height: Espaces.xs),
        ActionsFormulaire(
          libelle: 'Enregistrer',
          envoi: _envoi,
          surValidation: _valide ? _envoyer : null,
        ),
      ],
    );
  }

  Widget _champ(TextEditingController c, String indice) {
    return TextField(
      controller: c,
      obscureText: !_voir,
      enabled: !_envoi,
      autocorrect: false,
      enableSuggestions: false,
      onChanged: (_) => setState(() {}),
      decoration: InputDecoration(hintText: indice),
    );
  }
}
