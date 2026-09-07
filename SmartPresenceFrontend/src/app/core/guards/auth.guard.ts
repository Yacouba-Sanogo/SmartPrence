import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RoleCode } from '../models/auth.model';
import { AuthService } from '../services/auth.service';

/** Réserve la route aux utilisateurs connectés. */
export const authGuard: CanActivateFn = (_route, etat) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.connecte()) return true;
  // On mémorise la destination pour y revenir après la connexion.
  return router.createUrlTree(['/connexion'], { queryParams: { suite: etat.url } });
};

/**
 * Réserve la page d'accueil aux visiteurs non connectés.
 *
 * Un utilisateur déjà identifié n'a rien à faire sur une page de présentation : il
 * rejoint directement son tableau de bord.
 */
export const inviteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.connecte() ? router.createUrlTree(['/tableau-de-bord']) : true;
};

/**
 * Réserve la route aux porteurs d'un des rôles indiqués dans `data.roles`.
 *
 * Un utilisateur connecté mais non habilité est envoyé vers la page 403, jamais
 * vers la connexion : il n'a pas un problème d'identité mais de droits.
 */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const attendus = (route.data['roles'] as RoleCode[] | undefined) ?? [];
  if (attendus.length === 0 || auth.aUnRole(...attendus)) return true;
  return router.createUrlTree(['/acces-refuse']);
};
