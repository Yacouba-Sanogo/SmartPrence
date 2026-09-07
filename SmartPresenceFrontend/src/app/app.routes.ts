import { Routes } from '@angular/router';
import { authGuard, inviteGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    /*
     * Racine exacte : la page d'accueil.
     *
     * Déclarée ici, avant la coquille, et en `pathMatch: 'full'`. Placer ce choix
     * dans le garde de la coquille ne fonctionnait pas : la redirection enfant
     * `'' -> tableau-de-bord` est résolue *avant* le garde parent, qui ne voyait
     * donc jamais l'URL `/`.
     */
    path: '',
    pathMatch: 'full',
    canActivate: [inviteGuard],
    loadComponent: () =>
      import('./features/accueil/accueil.component').then((m) => m.AccueilComponent),
  },

  {
    // Même page, accessible explicitement. Elle ne présente que ce qui figure déjà
    // sur la façade de l'école : son nom et sa devise. Aucune donnée.
    path: 'accueil',
    loadComponent: () =>
      import('./features/accueil/accueil.component').then((m) => m.AccueilComponent),
  },

  {
    path: 'connexion',
    loadComponent: () => import('./features/auth/connexion.component').then((m) => m.ConnexionComponent),
  },

  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },

      {
        path: 'tableau-de-bord',
        data: { titre: 'Tableau de bord', rubrique: 'Pilotage' },
        loadComponent: () =>
          import('./features/tableau-de-bord/tableau-de-bord.component').then(
            (m) => m.TableauDeBordComponent,
          ),
      },

      {
        path: 'pointage',
        canActivate: [roleGuard],
        data: { titre: 'Pointage du jour', rubrique: 'Ressources humaines', roles: ['ADMIN', 'RH', 'SUPERVISEUR'] },
        loadComponent: () =>
          import('./features/pointage/pointage-jour.component').then((m) => m.PointageJourComponent),
      },
      {
        path: 'personnel',
        canActivate: [roleGuard],
        data: { titre: 'Personnel', rubrique: 'Ressources humaines', roles: ['ADMIN', 'RH'] },
        loadComponent: () =>
          import('./features/personnel/personnel-liste.component').then((m) => m.PersonnelListeComponent),
      },

      {
        path: 'etudiants',
        canActivate: [roleGuard],
        data: {
          titre: 'Étudiants',
          rubrique: 'Vie académique',
          roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
        },
        loadComponent: () =>
          import('./features/etudiants/etudiants-liste.component').then(
            (m) => m.EtudiantsListeComponent,
          ),
      },
      {
        path: 'presences',
        canActivate: [roleGuard],
        data: {
          titre: 'Présences',
          rubrique: 'Vie académique',
          roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
        },
        loadComponent: () =>
          import('./features/presences/presences-liste.component').then(
            (m) => m.PresencesListeComponent,
          ),
      },
      {
        path: 'justificatifs',
        canActivate: [roleGuard],
        data: {
          titre: 'Justificatifs',
          rubrique: 'Vie académique',
          roles: ['ADMIN', 'RESPONSABLE_SCOLARITE'],
        },
        loadComponent: () =>
          import('./features/justifications/justifications-liste.component').then(
            (m) => m.JustificationsListeComponent,
          ),
      },
      {
        path: 'signalements',
        canActivate: [roleGuard],
        data: {
          titre: 'Signalements',
          rubrique: 'Vie académique',
          roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
        },
        loadComponent: () =>
          import('./features/signalements/signalements-liste.component').then(
            (m) => m.SignalementsListeComponent,
          ),
      },
      {
        path: 'classes',
        canActivate: [roleGuard],
        data: {
          titre: 'Classes',
          rubrique: 'Vie académique',
          roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
        },
        loadComponent: () =>
          import('./features/classes/classes-liste.component').then(
            (m) => m.ClassesListeComponent,
          ),
      },
      {
        path: 'seances',
        canActivate: [roleGuard],
        data: {
          titre: 'Emploi du temps',
          rubrique: 'Vie académique',
          roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
        },
        loadComponent: () =>
          import('./features/seances/seances-liste.component').then(
            (m) => m.SeancesListeComponent,
          ),
      },
      {
        path: 'referentiel',
        canActivate: [roleGuard],
        data: {
          titre: 'Référentiel',
          rubrique: 'Vie académique',
          roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
        },
        loadComponent: () =>
          import('./features/referentiel/referentiel.component').then(
            (m) => m.ReferentielComponent,
          ),
      },
      {
        path: 'salles',
        canActivate: [roleGuard],
        data: {
          titre: 'Salles',
          rubrique: 'Vie académique',
          roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
        },
        loadComponent: () =>
          import('./features/salles/salles-liste.component').then((m) => m.SallesListeComponent),
      },
      {
        path: 'appareils',
        canActivate: [roleGuard],
        data: {
          titre: 'Appareils ESP32',
          rubrique: 'Système',
          roles: ['ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'],
        },
        loadComponent: () =>
          import('./features/appareils/appareils-liste.component').then(
            (m) => m.AppareilsListeComponent,
          ),
      },
      {
        path: 'statistiques',
        canActivate: [roleGuard],
        data: { titre: 'Statistiques', rubrique: 'Système', roles: ['ADMIN', 'SUPERVISEUR'] },
        loadComponent: () =>
          import('./features/statistiques/statistiques.component').then(
            (m) => m.StatistiquesComponent,
          ),
      },
      {
        path: 'parametres',
        canActivate: [roleGuard],
        data: { titre: 'Paramètres', rubrique: 'Système', roles: ['ADMIN', 'SUPERVISEUR'] },
        loadComponent: () =>
          import('./features/parametres/parametres.component').then((m) => m.ParametresComponent),
      },
    ],
  },

  {
    path: 'acces-refuse',
    loadComponent: () => import('./pages/statut.component').then((m) => m.StatutComponent),
    data: {
      code: '403',
      titre: 'Accès refusé',
      detail: "Votre compte n'a pas les droits requis pour consulter cet écran.",
    },
  },
  {
    path: '**',
    loadComponent: () => import('./pages/statut.component').then((m) => m.StatutComponent),
    data: {
      code: '404',
      titre: 'Page introuvable',
      detail: "L'adresse demandée ne correspond à aucun écran de l'administration.",
    },
  },
];
