/** Nature d'une anomalie signalée par un enseignant. */
export type TypeSignalement = 'ETUDIANT_NON_RECONNU' | 'LECTEUR_DEFAILLANT' | 'AUTRE';

/** Suite donnée par la scolarité. */
export type StatutSignalement = 'EN_ATTENTE' | 'ACCEPTE' | 'REJETE';

const LIBELLES_TYPE: Record<TypeSignalement, string> = {
  ETUDIANT_NON_RECONNU: 'Étudiant non reconnu',
  LECTEUR_DEFAILLANT: 'Lecteur défaillant',
  AUTRE: 'Autre anomalie',
};

const LIBELLES_STATUT: Record<StatutSignalement, string> = {
  EN_ATTENTE: 'En attente',
  ACCEPTE: 'Retenu',
  REJETE: 'Écarté',
};

export function libelleTypeSignalement(type: TypeSignalement): string {
  return LIBELLES_TYPE[type] ?? type;
}

export function libelleStatutSignalement(statut: StatutSignalement): string {
  return LIBELLES_STATUT[statut] ?? statut;
}

export function teinteStatutSignalement(statut: StatutSignalement): string {
  switch (statut) {
    case 'ACCEPTE':
      return 'bg-succes-bg text-succes';
    case 'REJETE':
      return 'bg-danger-bg text-danger';
    default:
      return 'bg-alerte-bg text-alerte';
  }
}

/**
 * Anomalie signalée par un enseignant, en attente d'arbitrage.
 *
 * L'enseignant témoigne, la scolarité arbitre : le relevé n'est jamais modifié par le
 * signalement lui-même. Accepter un `ETUDIANT_NON_RECONNU` produit un relevé distinct,
 * de source `MANUEL`, référencé par `presenceCorrectiveId`.
 */
export interface Signalement {
  id: string;
  seanceId: string;
  matiereLibelle: string;
  classeCode: string;
  seanceDebut: string;
  enseignantId: string;
  enseignantNom: string;
  etudiantId: string | null;
  etudiantNom: string | null;
  etudiantMatricule: string | null;
  type: TypeSignalement;
  description: string;
  statut: StatutSignalement;
  commentaireTraitement: string | null;
  traiteLe: string | null;
  /** Relevé correctif produit à l'acceptation, `null` sinon. */
  presenceCorrectiveId: string | null;
  createdAt: string;
}

/** Décision de la scolarité sur un signalement. */
export interface TraitementSignalementRequest {
  accepte: boolean;
  commentaire: string;
  /** `HH:mm:ss`. À défaut, l'heure de début de la séance fait foi. */
  heurePresence?: string | null;
}

export function enAttente(s: Pick<Signalement, 'statut'>): boolean {
  return s.statut === 'EN_ATTENTE';
}
