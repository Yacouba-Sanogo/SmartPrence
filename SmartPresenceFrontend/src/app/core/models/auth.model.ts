/** Codes de rôles applicatifs, alignés sur l'énumération RoleCode du backend. */
export type RoleCode =
  | 'ADMIN'
  | 'ENSEIGNANT'
  | 'RESPONSABLE_SCOLARITE'
  | 'SUPERVISEUR'
  | 'RH'
  | 'PERSONNEL';

export interface LoginRequest {
  email: string;
  motDePasse: string;
}

/**
 * Réponse d'authentification du backend.
 *
 * La structure est **plate** : l'utilisateur n'est pas imbriqué dans un sous-objet.
 * Les rôles arrivent préfixés (`ROLE_ADMIN`) — {@link normaliserRoles} les nettoie.
 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  userId: string;
  email: string;
  nom: string;
  prenom: string;
  roles: string[];
}

/** Session courante telle que conservée côté client. */
export interface Session {
  accessToken: string;
  refreshToken: string;
  /** Instant d'expiration en millisecondes epoch, calculé à la connexion. */
  expireLe: number;
  utilisateur: Utilisateur;
}

export interface Utilisateur {
  id: string;
  email: string;
  nom: string;
  prenom: string;
  roles: RoleCode[];
}

/** Retire le préfixe `ROLE_` que Spring Security ajoute aux autorités. */
export function normaliserRoles(roles: readonly string[]): RoleCode[] {
  return roles.map((role) => role.replace(/^ROLE_/, '') as RoleCode);
}

/** Initiales affichées dans l'avatar, en repli sur l'email si le nom manque. */
export function initiales(utilisateur: Utilisateur | null): string {
  if (!utilisateur) return '';
  const paire = `${utilisateur.prenom?.[0] ?? ''}${utilisateur.nom?.[0] ?? ''}`.trim();
  return (paire || utilisateur.email?.[0] || '').toUpperCase();
}
