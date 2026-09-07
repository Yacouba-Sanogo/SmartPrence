/** Catégories de personnel suivies par l'université (enum TypePersonnel du backend). */
export type TypePersonnel = 'ENSEIGNANT' | 'ADMINISTRATIF' | 'TECHNIQUE' | 'SECURITE';

export const TYPES_PERSONNEL: readonly TypePersonnel[] = [
  'ENSEIGNANT',
  'ADMINISTRATIF',
  'TECHNIQUE',
  'SECURITE',
];

const LIBELLES_TYPE: Record<TypePersonnel, string> = {
  ENSEIGNANT: 'Enseignant',
  ADMINISTRATIF: 'Administratif',
  TECHNIQUE: 'Technique',
  SECURITE: 'Sécurité',
};

export function libelleType(type: TypePersonnel): string {
  return LIBELLES_TYPE[type] ?? type;
}

/**
 * Agent du référentiel.
 *
 * `biometricId` n'est qu'une **référence logique** de correspondance : il ne contient
 * aucune empreinte ni gabarit biométrique.
 */
export interface Personnel {
  id: string;
  matricule: string;
  nom: string;
  prenom: string;
  email: string | null;
  telephone: string | null;
  type: TypePersonnel;
  service: string | null;
  actif: boolean;
  biometricId: string | null;
  /** `true` si l'agent dispose d'une empreinte enrôlée et peut donc pointer. */
  enrole: boolean;
}

export interface PersonnelRequest {
  matricule: string;
  nom: string;
  prenom: string;
  email?: string | null;
  telephone?: string | null;
  type: TypePersonnel;
  service?: string | null;
  actif?: boolean;
  utilisateurId?: string | null;
}

/** Enrôlement biométrique : associe une référence logique à un agent. */
export interface EnrolementRequest {
  biometricId: string;
  deviceId?: string | null;
}

/** Filtres de la liste du personnel, appliqués côté client sur le référentiel chargé. */
export interface FiltresPersonnel {
  recherche: string;
  type: TypePersonnel | null;
  service: string | null;
  enrolement: 'TOUS' | 'ENROLES' | 'NON_ENROLES';
}

export function filtresPersonnelParDefaut(): FiltresPersonnel {
  return { recherche: '', type: null, service: null, enrolement: 'TOUS' };
}

export function nomComplet(agent: Pick<Personnel, 'nom' | 'prenom'>): string {
  return `${agent.prenom} ${agent.nom}`.trim();
}

export function initialesAgent(agent: Pick<Personnel, 'nom' | 'prenom'>): string {
  return `${agent.prenom?.[0] ?? ''}${agent.nom?.[0] ?? ''}`.toUpperCase();
}
