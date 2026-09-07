import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse, PagedResponse } from '../models/api.model';
import {
  FiltresPresences,
  Presence,
  PresenceManuelleRequest,
} from '../models/presence.model';

/** Relevés de présence des étudiants. */
@Injectable({ providedIn: 'root' })
export class PresenceService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/presences`;

  rechercher(filtres: FiltresPresences = {}): Observable<PagedResponse<Presence>> {
    let params = new HttpParams()
      .set('page', String(filtres.page ?? 0))
      .set('size', String(filtres.size ?? 25));

    if (filtres.etudiantId) params = params.set('etudiantId', filtres.etudiantId);
    if (filtres.classeId) params = params.set('classeId', String(filtres.classeId));
    if (filtres.dateDebut) params = params.set('dateDebut', filtres.dateDebut);
    if (filtres.dateFin) params = params.set('dateFin', filtres.dateFin);
    if (filtres.statut) params = params.set('statut', filtres.statut);
    if (filtres.source) params = params.set('source', filtres.source);

    return extraireDonnees(
      this.http.get<ApiResponse<PagedResponse<Presence>>>(this.base, { params }),
    );
  }

  /**
   * Saisie manuelle d'un relevé.
   *
   * Produit une présence de source `MANUEL`, distinguable d'une identification par le
   * lecteur. La distinction est portée par le serveur : le client ne peut pas prétendre
   * qu'une saisie vient du capteur.
   */
  saisir(presence: PresenceManuelleRequest): Observable<Presence> {
    return extraireDonnees(
      this.http.post<ApiResponse<Presence>>(`${this.base}/manual`, presence),
    );
  }
}
