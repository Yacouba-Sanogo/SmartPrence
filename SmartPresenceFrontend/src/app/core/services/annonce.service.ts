import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';

export type TypeAnnonce = 'ANNONCE' | 'CHANGEMENT' | 'EXAMEN' | 'CONVOCATION' | 'INFORMATION';

/** Annonce à diffuser. Sans classe ni enseignant, elle part à tous les enseignants actifs. */
export interface AnnonceRequest {
  titre: string;
  message: string;
  type: TypeAnnonce;
  classeId?: number | null;
  personnelId?: string | null;
}

/**
 * Diffusion d'annonces aux enseignants.
 *
 * Le serveur renvoie le nombre de comptes réellement notifiés : un enseignant sans compte
 * mobile ne reçoit rien et n'est pas compté, ce qui permet d'avertir l'administration.
 */
@Injectable({ providedIn: 'root' })
export class AnnonceService {
  private readonly http = inject(HttpClient);

  diffuser(annonce: AnnonceRequest): Observable<number> {
    return extraireDonnees(
      this.http.post<ApiResponse<number>>(`${API}/notifications/annonces`, annonce),
    );
  }
}
