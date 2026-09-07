/** Matière du référentiel. */
export interface Matiere {
  id: number;
  code: string;
  libelle: string;
  credits: number | null;
  description: string | null;
  active: boolean;
}

export interface MatiereRequest {
  code: string;
  libelle: string;
  credits?: number | null;
  description?: string | null;
  active?: boolean;
}

export interface PromotionRequest {
  code: string;
  libelle: string;
  anneeAcademique: number;
}

/** Avancement d'une séance (enum StatutSeance du backend). */
export type StatutSeance = 'PLANIFIEE' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE';

export const STATUTS_SEANCE: readonly StatutSeance[] = [
  'PLANIFIEE',
  'EN_COURS',
  'TERMINEE',
  'ANNULEE',
];

const LIBELLES_STATUT_SEANCE: Record<StatutSeance, string> = {
  PLANIFIEE: 'Planifiée',
  EN_COURS: 'En cours',
  TERMINEE: 'Terminée',
  ANNULEE: 'Annulée',
};

export function libelleStatutSeance(statut: StatutSeance): string {
  return LIBELLES_STATUT_SEANCE[statut] ?? statut;
}

/** Teintes alignées sur celles des badges de présence. */
export function teinteStatutSeance(statut: StatutSeance): string {
  switch (statut) {
    case 'EN_COURS':
      return 'bg-royal-50 text-royal-600';
    case 'TERMINEE':
      return 'bg-succes-bg text-succes';
    case 'ANNULEE':
      return 'bg-danger-bg text-danger';
    default:
      return 'bg-line-faint text-ink-subtle';
  }
}

/** Séance planifiée. */
export interface Seance {
  id: string;
  classeId: number;
  classeCode: string;
  matiereId: number;
  matiereLibelle: string;
  enseignantId: string;
  enseignantNom: string;
  salleId: number | null;
  salleLibelle: string | null;
  /** Instants ISO 8601 en UTC. */
  debut: string;
  fin: string;
  statut: StatutSeance;
  note: string | null;
}

export interface SeanceRequest {
  classeId: number;
  matiereId: number;
  enseignantId: string;
  salleId?: number | null;
  debut: string;
  fin: string;
  note?: string | null;
}

/** `08:30` — heure locale d'un instant ISO. */
export function heureDe(instantIso: string): string {
  const date = new Date(instantIso);
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

export function creneauDe(seance: Pick<Seance, 'debut' | 'fin'>): string {
  return `${heureDe(seance.debut)} – ${heureDe(seance.fin)}`;
}

/** Clé `YYYY-MM-DD` locale d'un instant, pour regrouper par jour. */
export function jourDe(instantIso: string): string {
  const date = new Date(instantIso);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-');
}

/**
 * Convertit une date et une heure de formulaire en instant ISO.
 *
 * `new Date('2026-08-23T08:00')` est interprété en heure **locale** par la norme, ce
 * qui est le comportement voulu : l'utilisateur saisit l'heure du cours, pas UTC.
 */
export function versInstant(date: string, heure: string): string {
  return new Date(`${date}T${heure}`).toISOString();
}
