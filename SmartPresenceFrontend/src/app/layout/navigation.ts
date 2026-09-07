import { RoleCode } from '../core/models/auth.model';
import { NomIcone } from '../shared/ui/icone.component';

export interface EntreeNavigation {
  chemin: string;
  libelle: string;
  icone: NomIcone;
  /** Rôles autorisés. Une liste vide signifie « tout utilisateur connecté ». */
  roles: RoleCode[];
  /**
   * Compteur affiché à droite de l'entrée, s'il y a lieu.
   *
   * Seuls les écrans où quelque chose *attend une décision* en portent un : un
   * compteur qui ne décroît jamais devient un décor et cesse d'être lu.
   */
  compteur?: CompteurNavigation;
}

/** Files d'attente susceptibles d'alimenter un compteur de la barre latérale. */
export type CompteurNavigation = 'justificatifs' | 'signalements';

export interface GroupeNavigation {
  titre: string;
  entrees: EntreeNavigation[];
}

/**
 * Arborescence de navigation de l'administration.
 *
 * Les entrées portent leurs rôles, exactement ceux exigés par les endpoints
 * correspondants : la barre latérale ne propose donc jamais un écran qui
 * renverrait un 403.
 *
 * ENSEIGNANT n'y figure nulle part : l'administration web n'est pas sa place.
 * Ses classes, ses séances et ses feuilles de présence sont sur l'application
 * mobile, qui les sert depuis l'espace /moi.
 */
export const NAVIGATION: readonly GroupeNavigation[] = [
  {
    titre: 'Pilotage',
    entrees: [
      { chemin: '/tableau-de-bord', libelle: 'Tableau de bord', icone: 'tableau-de-bord', roles: [] },
    ],
  },
  {
    titre: 'Ressources humaines',
    entrees: [
      { chemin: '/personnel', libelle: 'Personnel', icone: 'personnel', roles: ['ADMIN', 'RH'] },
      {
        chemin: '/pointage',
        libelle: 'Pointage du jour',
        icone: 'empreinte',
        roles: ['ADMIN', 'RH', 'SUPERVISEUR'],
      },
    ],
  },
  {
    titre: 'Vie académique',
    entrees: [
      {
        chemin: '/etudiants',
        libelle: 'Étudiants',
        icone: 'etudiants',
        roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
      },
      {
        chemin: '/presences',
        libelle: 'Présences',
        icone: 'presences',
        roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
      },
      {
        chemin: '/classes',
        libelle: 'Classes',
        icone: 'classes',
        roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
      },
      {
        chemin: '/justificatifs',
        libelle: 'Justificatifs',
        icone: 'info',
        roles: ['ADMIN', 'RESPONSABLE_SCOLARITE'],
        compteur: 'justificatifs',
      },
      {
        chemin: '/signalements',
        libelle: 'Signalements',
        icone: 'bouclier',
        roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
        compteur: 'signalements',
      },
      {
        chemin: '/seances',
        libelle: 'Emploi du temps',
        icone: 'horloge',
        roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
      },
      {
        chemin: '/referentiel',
        libelle: 'Référentiel',
        icone: 'calendrier',
        roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
      },
      {
        chemin: '/salles',
        libelle: 'Salles',
        icone: 'salles',
        roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
      },
    ],
  },
  {
    titre: 'Système',
    entrees: [
      {
        chemin: '/appareils',
        libelle: 'Appareils ESP32',
        icone: 'appareils',
        roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
      },
      {
        chemin: '/statistiques',
        libelle: 'Statistiques',
        icone: 'statistiques',
        roles: ['ADMIN', 'SUPERVISEUR'],
      },
      { chemin: '/parametres', libelle: 'Paramètres', icone: 'parametres', roles: ['ADMIN', 'SUPERVISEUR'] },
    ],
  },
];
