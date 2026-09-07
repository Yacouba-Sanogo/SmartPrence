import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API } from '../api';
import { ApiResponse } from '../models/api.model';
import {
  AuthResponse,
  LoginRequest,
  RoleCode,
  Session,
  Utilisateur,
  normaliserRoles,
} from '../models/auth.model';

const CLE_SESSION = 'smartpresence.session';

/**
 * Authentification JWT des utilisateurs humains.
 *
 * La session est conservée dans le `localStorage` afin de survivre à un rechargement,
 * et son expiration est vérifiée à la restauration : un jeton périmé est écarté plutôt
 * que d'être présenté au backend pour se faire rejeter.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(this.restaurer());

  readonly utilisateur = computed<Utilisateur | null>(() => this.session()?.utilisateur ?? null);
  readonly connecte = computed(() => this.session() !== null);
  readonly roles = computed<RoleCode[]>(() => this.session()?.utilisateur.roles ?? []);

  login(identifiants: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${API}/auth/login`, identifiants)
      .pipe(tap((reponse) => this.ouvrirSession(reponse.data)));
  }

  logout(): void {
    this.session.set(null);
    localStorage.removeItem(CLE_SESSION);
    void this.router.navigateByUrl('/connexion');
  }

  jeton(): string | null {
    return this.session()?.accessToken ?? null;
  }

  /** `true` si l'utilisateur possède au moins un des rôles attendus. */
  aUnRole(...attendus: readonly RoleCode[]): boolean {
    const detenus = this.roles();
    return attendus.some((role) => detenus.includes(role));
  }

  /** Accès au module de pointage du personnel : administration et ressources humaines. */
  peutGererLePersonnel(): boolean {
    return this.aUnRole('ADMIN', 'RH');
  }

  private ouvrirSession(reponse: AuthResponse | undefined): void {
    if (!reponse) return;
    const session: Session = {
      accessToken: reponse.accessToken,
      refreshToken: reponse.refreshToken,
      expireLe: Date.now() + reponse.expiresInMs,
      utilisateur: {
        id: reponse.userId,
        email: reponse.email,
        nom: reponse.nom,
        prenom: reponse.prenom,
        roles: normaliserRoles(reponse.roles ?? []),
      },
    };
    this.session.set(session);
    localStorage.setItem(CLE_SESSION, JSON.stringify(session));
  }

  private restaurer(): Session | null {
    try {
      const brut = localStorage.getItem(CLE_SESSION);
      if (!brut) return null;
      const session = JSON.parse(brut) as Session;
      if (!session?.accessToken || session.expireLe <= Date.now()) {
        localStorage.removeItem(CLE_SESSION);
        return null;
      }
      return session;
    } catch {
      localStorage.removeItem(CLE_SESSION);
      return null;
    }
  }
}
