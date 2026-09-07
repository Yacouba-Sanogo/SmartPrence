import { TypePersonnel } from './personnel.model';

export type SensPointage = 'ENTREE' | 'SORTIE';
export type SourcePointage = 'ESP32' | 'MANUEL';
export type StatutPresence = 'PRESENT' | 'RETARD' | 'ABSENT' | 'JUSTIFIE';

/**
 * Synthèse de la journée de travail d'un agent — une ligne du tableau principal.
 *
 * `minutesTravaillees` n'est renseigné qu'une fois la sortie pointée ;
 * `minutesEcoulees` prend le relais pour un agent encore sur site le jour même.
 */
export interface JourneePersonnel {
  personnelId: string;
  matricule: string;
  nom: string;
  prenom: string;
  type: TypePersonnel;
  service: string | null;
  date: string;
  heureEntree: string | null;
  heureSortie: string | null;
  statut: StatutPresence;
  minutesRetard: number;
  minutesTravaillees: number | null;
  minutesEcoulees: number | null;
  nombrePointages: number;
  /** `true` si l'agent est entré sans avoir encore pointé sa sortie. */
  present: boolean;
}

/** Pointage unitaire, tel qu'affiché dans le flux en direct. */
export interface PointagePersonnel {
  id: string;
  personnelId: string;
  personnelMatricule: string;
  personnelNom: string;
  personnelPrenom: string;
  personnelType: TypePersonnel;
  personnelService: string | null;
  deviceId: string | null;
  deviceNom: string | null;
  datePointage: string;
  heurePointage: string;
  sens: SensPointage;
  statut: StatutPresence;
  source: SourcePointage;
  /** Justification d'une régularisation ; `null` pour un pointage biométrique. */
  motif: string | null;
  /** Écart entre la capture sur l'appareil et la réception serveur. */
  latenceSynchronisationMs: number | null;
}

export interface PointageManuelRequest {
  personnelId: string;
  datePointage: string;
  heurePointage: string;
  sens: SensPointage;
  deviceId?: string | null;
  /** Obligatoire : une saisie manuelle sans justification ne serait pas traçable. */
  motif: string;
}

/** Paramètres horaires de l'établissement, qui gouvernent la qualification des retards. */
export interface ParametresEtablissement {
  id: number;
  nom: string;
  sigle: string | null;
  email: string | null;
  telephone: string | null;
  fuseauHoraire: string;
  seuilRetardMinutes: number;
  heureOuverture: string;
  heureFermeture: string;
}

/** Indicateurs de tête de page, dérivés de la synthèse journalière. */
export interface IndicateursJournee {
  effectif: number;
  presents: number;
  retards: number;
  absents: number;
  surSite: number;
  tauxPresence: number;
}

/**
 * Agrège la synthèse en indicateurs.
 *
 * Un agent en retard est aussi un agent présent : les retards sont donc un
 * sous-ensemble des présents, jamais une catégorie à part.
 */
export function calculerIndicateurs(journees: readonly JourneePersonnel[]): IndicateursJournee {
  const effectif = journees.length;
  const absents = journees.filter((j) => j.statut === 'ABSENT').length;
  const presents = effectif - absents;
  return {
    effectif,
    presents,
    retards: journees.filter((j) => j.statut === 'RETARD').length,
    absents,
    surSite: journees.filter((j) => j.present).length,
    tauxPresence: effectif === 0 ? 0 : Math.round((presents / effectif) * 100),
  };
}

/** `08:04:00` → `08:04`. Renvoie un tiret cadratin lorsque l'heure est absente. */
export function formaterHeure(heure: string | null | undefined): string {
  if (!heure) return '—';
  return heure.slice(0, 5);
}

/** `553` → `9 h 13`. Renvoie un tiret cadratin pour une durée absente. */
export function formaterDuree(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined) return '—';
  const heures = Math.floor(minutes / 60);
  const reste = minutes % 60;
  if (heures === 0) return `${reste} min`;
  return `${heures} h ${String(reste).padStart(2, '0')}`;
}

/** `31` → `+31 min`. Un retard nul n'est pas affiché. */
export function formaterRetard(minutes: number | null | undefined): string {
  if (!minutes || minutes <= 0) return '—';
  return `+${minutes} min`;
}

/** Durée à afficher : temps travaillé si la sortie est pointée, sinon temps écoulé. */
export function dureePresence(journee: JourneePersonnel): string {
  return formaterDuree(journee.minutesTravaillees ?? journee.minutesEcoulees);
}

export function libelleSens(sens: SensPointage): string {
  return sens === 'ENTREE' ? 'Entrée' : 'Sortie';
}

const LIBELLES_STATUT: Record<StatutPresence, string> = {
  PRESENT: 'Présent',
  RETARD: 'Retard',
  ABSENT: 'Absent',
  JUSTIFIE: 'Justifié',
};

export function libelleStatut(statut: StatutPresence): string {
  return LIBELLES_STATUT[statut] ?? statut;
}

/** Date du jour au format ISO attendu par l'API (`yyyy-MM-dd`), en heure locale. */
export function dateDuJourIso(): string {
  const maintenant = new Date();
  const mois = String(maintenant.getMonth() + 1).padStart(2, '0');
  const jour = String(maintenant.getDate()).padStart(2, '0');
  return `${maintenant.getFullYear()}-${mois}-${jour}`;
}

/** `2026-03-12` → `jeu. 12 mars 2026`. */
export function formaterDateLongue(iso: string): string {
  const [annee, mois, jour] = iso.split('-').map(Number);
  const date = new Date(annee, mois - 1, jour);
  return date.toLocaleDateString('fr-FR', {
    weekday: 'short',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}
