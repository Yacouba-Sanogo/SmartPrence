/**
 * Étudiant du référentiel.
 *
 * `biometricId` n'est qu'une **référence logique** de correspondance : il ne contient
 * aucune empreinte ni gabarit biométrique.
 */
export interface Etudiant {
  id: string;
  matricule: string;
  nom: string;
  prenom: string;
  email: string | null;
  telephone: string | null;
  dateNaissance: string | null;
  biometricId: string | null;
  /** `true` si l'étudiant dispose d'une empreinte enrôlée. */
  enrole: boolean;
  actif: boolean;
  classeId: number | null;
  classeCode: string | null;
  classeLibelle: string | null;
  promotionLibelle: string | null;
  /** Compte de connexion mobile, `null` si l'étudiant n'a pas d'accès. */
  utilisateurId: string | null;
  /** `true` si l'étudiant peut se connecter à l'application mobile. */
  compteOuvert: boolean;
}

export interface EtudiantRequest {
  matricule: string;
  nom: string;
  prenom: string;
  email?: string | null;
  telephone?: string | null;
  dateNaissance?: string | null;
  /** Facultatif : l'enrôlement est un acte distinct. */
  biometricId?: string | null;
  classeId: number;
  actif?: boolean;
}

/**
 * Identifiants d'un accès mobile fraîchement ouvert.
 *
 * Le mot de passe n'est restitué qu'à cette occasion : le serveur n'en conserve que
 * l'empreinte et ne pourra plus l'afficher.
 */
export interface CompteEtudiant {
  etudiantId: string;
  matricule: string;
  nomComplet: string;
  utilisateurId: string;
  email: string;
  motDePasseInitial: string;
}

export function nomCompletEtudiant(etudiant: Pick<Etudiant, 'nom' | 'prenom'>): string {
  return `${etudiant.prenom} ${etudiant.nom}`.trim();
}

export function initialesEtudiant(etudiant: Pick<Etudiant, 'nom' | 'prenom'>): string {
  return `${etudiant.prenom?.[0] ?? ''}${etudiant.nom?.[0] ?? ''}`.toUpperCase();
}
