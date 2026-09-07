/// Mise en forme des dates en français, sans dépendance à `intl`.
///
/// L'application n'affiche que des dates courtes et des jours de la semaine : charger
/// `intl` et initialiser une locale pour cela coûterait plus que ces trois tables.
class Dates {
  Dates._();

  static const List<String> moisCourts = [
    'janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin',
    'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.',
  ];

  static const List<String> moisLongs = [
    'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
    'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
  ];

  /// `DateTime.weekday` vaut 1 pour lundi : la table démarre donc à lundi.
  static const List<String> jours = [
    'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi', 'dimanche',
  ];

  /// `12 mars`
  static String courte(DateTime d) => '${d.day} ${moisCourts[d.month - 1]}';

  /// `mardi 12 mars`
  static String jourEtDate(DateTime d) =>
      '${jours[d.weekday - 1]} ${d.day} ${moisLongs[d.month - 1]}';

  /// `08:30`
  static String heure(DateTime d) =>
      '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';

  static bool memeJour(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  static bool estAujourdhui(DateTime d) => memeJour(d, DateTime.now());

  /// Libellé relatif quand il aide à se repérer, date explicite sinon.
  static String relatif(DateTime d) {
    final aujourdhui = DateTime.now();
    if (memeJour(d, aujourdhui)) return "Aujourd'hui";
    if (memeJour(d, aujourdhui.add(const Duration(days: 1)))) return 'Demain';
    if (memeJour(d, aujourdhui.subtract(const Duration(days: 1)))) return 'Hier';
    return jourEtDate(d);
  }
}
