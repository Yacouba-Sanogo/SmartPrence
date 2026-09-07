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
