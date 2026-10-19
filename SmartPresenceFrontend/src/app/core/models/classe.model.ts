/** Promotion — la cohorte à laquelle une classe appartient. */
export interface Promotion {
  id: number;
  code: string;
  libelle: string;
  anneeAcademique: number | null;
}

/**
 * Classe du référentiel académique.
 *
 * La promotion arrive **imbriquée** (`promotion: { … }`) et non aplatie : le modèle
 * précédent déclarait un `promotionLibelle` que le serveur n'a jamais envoyé, donc
 * toujours `undefined`. Un champ inexistant ne provoque aucune erreur en TypeScript —
 * il se contente de n'afficher jamais rien.
 */
export interface Classe {
  id: number;
  code: string;
  libelle: string;
  promotion: Promotion | null;
  nombreEtudiants: number;
  /** Une classe à zéro enseignant n'apparaît sur l'application mobile de personne. */
  nombreEnseignants: number;
  /** Rattachement à une filière de l'ENETP ; absent des classes antérieures au référentiel. */
  filiereId?: number | null;
  filiereLibelle?: string | null;
  /** Option (spécialité), à partir de la 3e année. */
  optionId?: number | null;
  optionLibelle?: string | null;
  /** Classe d'où viennent les étudiants : elle fixe la classe supérieure. */
  classePrecedenteId?: number | null;
  classePrecedenteCode?: string | null;
}

export interface OptionFiliere {
  id: number;
  code: string;
  libelle: string;
}

export interface DepartementFiliere {
  id: number;
  code: string;
  libelle: string;
}

/** Filière, avec son département (DER) et ses options. */
export interface Filiere {
  id: number;
  code: string;
  libelle: string;
  departement: DepartementFiliere;
  options: OptionFiliere[];
}

export type IssuePassage =
  | 'PROMU'
  | 'REDOUBLANT'
  | 'EN_ATTENTE'
  | 'TERMINE'
  | 'OPTION_A_CHOISIR';

export interface LignePassage {
  etudiantId: string;
  matricule: string;
  nomComplet: string;
  classeOrigine: string;
  classeDestination: string | null;
  issue: IssuePassage;
  detail: string;
}

/** Compte rendu d'un passage en année supérieure, simulé ou appliqué. */
export interface Passage {
  appliquee: boolean;
  promus: number;
  redoublants: number;
  enAttente: number;
  termines: number;
  optionsManquantes: number;
  lignes: LignePassage[];
}

/** Enseignant rattaché, tel que le détail d'une classe le renvoie. */
export interface EnseignantDeClasse {
  id: string;
  matricule: string;
  nom: string;
  prenom: string;
  email: string | null;
  service: string | null;
  enrole: boolean;
  actif: boolean;
}

/** Étudiant inscrit, tel que le détail d'une classe le renvoie. */
export interface EtudiantDeClasse {
  id: string;
  matricule: string;
  nom: string;
  prenom: string;
  enrole: boolean;
  actif: boolean;
  compteOuvert: boolean;
}

/** Classe avec son équipe pédagogique et son effectif nominatif. */
export interface ClasseDetail extends Omit<Classe, 'nombreEtudiants'> {
  enseignants: EnseignantDeClasse[];
  etudiants: EtudiantDeClasse[];
}

/** Requête de création ou de modification d'une classe. */
export interface ClasseRequest {
  code: string;
  libelle: string;
  promotionId: number;
  /**
   * Enseignants rattachés.
   *
   * Le backend **remplace** l'ensemble : omettre la liste lors d'une modification
   * détacherait tout le monde. Le dialogue la renvoie donc toujours en entier.
   */
  enseignantIds: string[];
  /** Facultatifs : absents, le serveur conserve les valeurs actuelles. */
  filiereId?: number | null;
  optionId?: number | null;
  classePrecedenteId?: number | null;
}

export function libellePromotion(classe: Pick<Classe, 'promotion'>): string {
  const promotion = classe.promotion;
  if (!promotion) return '—';
  return promotion.anneeAcademique
    ? `${promotion.libelle} (${promotion.anneeAcademique})`
    : promotion.libelle;
}

export function nomCompletEnseignant(agent: Pick<EnseignantDeClasse, 'nom' | 'prenom'>): string {
  return `${agent.prenom} ${agent.nom}`.trim();
}

export function initiales(personne: { nom: string; prenom: string }): string {
  return `${personne.prenom?.[0] ?? ''}${personne.nom?.[0] ?? ''}`.toUpperCase();
}
