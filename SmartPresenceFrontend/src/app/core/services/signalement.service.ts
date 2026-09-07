import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';
import {
  Signalement,
  StatutSignalement,
  TraitementSignalementRequest,
} from '../models/signalement.model';

/**
 * File d'arbitrage des anomalies signalees par les enseignants.
 *
 * Le depot se fait depuis l'application mobile ; l'administration n'arbitre. Les deux
 * chemins sont distincts, et c'est deliberé : celui qui temoigne n'est pas celui qui
 * tranche.
 */
@Injectable({ providedIn: 'root' })
export class SignalementService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/signalements`;

  lister(statut?: StatutSignalement | null): Observable<Signalement[]> {
    let params = new HttpParams();
    if (statut) params = params.set('statut', statut);
    return extraireDonnees(this.http.get<ApiResponse<Signalement[]>>(this.base, { params }));
  }

  traiter(id: string, decision: TraitementSignalementRequest): Observable<Signalement> {
    return extraireDonnees(
      this.http.patch<ApiResponse<Signalement>>(`${this.base}/${id}/traiter`, decision),
    );
  }
}
