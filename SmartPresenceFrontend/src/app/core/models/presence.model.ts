/** Statut d'un relevé de présence étudiant (enum StatutPresence du backend). */
export type StatutPresence = 'PRESENT' | 'RETARD' | 'ABSENT' | 'JUSTIFIE';

export const STATUTS_PRESENCE: readonly StatutPresence[] = [
  'PRESENT',
  'RETARD',
  'ABSENT',
  'JUSTIFIE',
];

/** Origine d'un relevé (enum SourcePresence du backend). */
export type SourcePresence = 'ESP32' | 'MANUEL' | 'IMPORT';

const LIBELLES_STATUT: Record<StatutPresence, string> = {
  PRESENT: 'Présent',
  RETARD: 'Retard',
  ABSENT: 'Absent',
  JUSTIFIE: 'Justifié',
};

export function libelleStatutPresence(statut: StatutPresence): string {
  return LIBELLES_STATUT[statut] ?? statut;
}

export function teinteStatutPresence(statut: StatutPresence): string {
  switch (statut) {
    case 'PRESENT':
      return 'bg-succes-bg text-succes';
    case 'RETARD':
      return 'bg-alerte-bg text-alerte';
    case 'ABSENT':
      return 'bg-danger-bg text-danger';
    case 'JUSTIFIE':
      return 'bg-royal-50 text-royal-600';
    default:
      return 'bg-line-faint text-ink-subtle';
  }
}

/**
 * Relevé de présence d'un étudiant.
 *
 * `source` distingue une identification par le lecteur d'une saisie humaine. C'est ce
 * qui donne au relevé sa valeur probante : confondre les deux effacerait la différence
 * entre un fait constaté et une décision.
 */
export interface Presence {
  id: string;
  etudiantId: string;
  etudiantMatricule: string;
  etudiantNom: string;
  etudiantPrenom: string;
  classeCode: string | null;
  deviceId: string | null;
  deviceNom: string | null;
  salleNom: string | null;
  seanceId: string | null;
  matiereLibelle: string | null;
  datePresence: string;
  heurePresence: string;
  statut: StatutPresence;
  source: SourcePresence;
  latenceSynchronisationMs: number | null;
}

/** Saisie manuelle d'un relevé, réservée à la scolarité. */
export interface PresenceManuelleRequest {
  etudiantId: string;
  seanceId?: string | null;
  datePresence: string;
  heurePresence: string;
  statut: StatutPresence;
}

/** Filtres de recherche, tous facultatifs. */
export interface FiltresPresences {
  etudiantId?: string | null;
  classeId?: number | null;
  dateDebut?: string | null;
  dateFin?: string | null;
  statut?: StatutPresence | null;
  source?: SourcePresence | null;
  page?: number;
  size?: number;
}

export function nomCompletReleve(p: Pick<Presence, 'etudiantNom' | 'etudiantPrenom'>): string {
  return `${p.etudiantPrenom} ${p.etudiantNom}`.trim();
}

export function initialesReleve(p: Pick<Presence, 'etudiantNom' | 'etudiantPrenom'>): string {
  return `${p.etudiantPrenom?.[0] ?? ''}${p.etudiantNom?.[0] ?? ''}`.toUpperCase();
}

/** `08:30` — les heures arrivent en `HH:mm:ss`. */
export function heureCourte(heure: string | null): string {
  return heure ? heure.slice(0, 5) : '—';
}
