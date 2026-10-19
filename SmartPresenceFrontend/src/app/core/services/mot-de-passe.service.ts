import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';

/** Changement de mot de passe par son titulaire ; l'ancien est exigé par le serveur. */
@Injectable({ providedIn: 'root' })
export class MotDePasseService {
  private readonly http = inject(HttpClient);

  changer(motDePasseActuel: string, nouveauMotDePasse: string): Observable<void> {
    return extraireDonnees(
      this.http.patch<ApiResponse<void>>(`${API}/auth/mot-de-passe`, { motDePasseActuel, nouveauMotDePasse }),
    );
  }
}
