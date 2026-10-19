import 'package:flutter/material.dart';

import '../core/api/client_api.dart';
import '../core/api/erreurs.dart';
import '../core/design/couleurs.dart';
import '../core/design/typographie.dart';
import 'communs.dart';

class _Option {
  const _Option(this.id, this.libelle);
  final int id;
  final String libelle;
}

/// Choix de l'option (spécialité) de l'étudiant.
///
/// N'apparaît que si la filière de sa classe en propose (ex. Génie Civil : BTP ou
/// Hydraulique). L'option retenue décide de la classe qu'il rejoint en année
/// supérieure : sans elle, un étudiant admis resterait en attente.
class ChoixOption extends StatefulWidget {
  const ChoixOption({super.key, required this.client});

  final ClientApi client;

  @override
  State<ChoixOption> createState() => _ChoixOptionState();
}

class _ChoixOptionState extends State<ChoixOption> {
  String? _filiere;
  List<_Option> _disponibles = const [];
  int? _choisie;
  bool _enCours = false;
  String? _erreur;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  void _lire(Map<String, dynamic> d) {
    final choisie = d['choisie'] as Map<String, dynamic>?;
    setState(() {
      _filiere = d['filiere'] as String?;
      _disponibles = [
        for (final o in (d['disponibles'] as List))
          _Option((o['id'] as num).toInt(), o['libelle'] as String),
      ];
      _choisie = (choisie?['id'] as num?)?.toInt();
    });
  }

  Future<void> _charger() async {
    try {
      _lire(await widget.client.get('/moi/option') as Map<String, dynamic>);
    } on ErreurApi {
      // Une absence d'option n'a rien d'une panne : le bloc reste simplement masqué.
    }
  }

  Future<void> _choisir(int id) async {
    setState(() {
      _enCours = true;
      _erreur = null;
    });
    try {
      _lire(await widget.client.put('/moi/option', corps: {'optionId': id})
          as Map<String, dynamic>);
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _enCours = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_disponibles.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.only(bottom: Espaces.lg),
      child: Carte(
        padding: const EdgeInsets.all(Espaces.lg),
        enfant: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('MON OPTION', style: Typo.suretitre),
            const SizedBox(height: Espaces.xs),
            Text(
              _choisie == null
                  ? 'La filière $_filiere se divise en options. Choisissez la vôtre : '
                      'elle détermine votre classe en année supérieure.'
                  : 'Votre option pour la suite de la filière $_filiere.',
              style: Typo.corpsAttenue,
            ),
            const SizedBox(height: Espaces.md),
            Wrap(
              spacing: Espaces.sm,
              runSpacing: Espaces.sm,
              children: [
                for (final o in _disponibles)
                  ChoiceChip(
                    label: Text(o.libelle),
                    selected: _choisie == o.id,
                    onSelected: _enCours ? null : (_) => _choisir(o.id),
                  ),
              ],
            ),
            if (_erreur != null) ...[
              const SizedBox(height: Espaces.md),
              Bandeau.erreur(message: _erreur!),
            ],
          ],
        ),
      ),
    );
  }
}
