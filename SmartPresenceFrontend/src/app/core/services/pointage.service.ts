import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';
import {
  JourneePersonnel,
  ParametresEtablissement,
  PointageManuelRequest,
  PointagePersonnel,
} from '../models/pointage.model';

/** Pointage horaire du personnel : synthèse journalière, flux brut et régularisation. */
@Injectable({ providedIn: 'root' })
export class PointageService {
  private readonly http = inject(HttpClient);

  /**
   * Synthèse de la journée : une ligne par agent actif, y compris ceux qui n'ont
   * pas pointé — ils apparaissent en `ABSENT`, ce qui est l'information attendue.
   */
  journee(date: string): Observable<JourneePersonnel[]> {
    const params = new HttpParams().set('date', date);
    return extraireDonnees(
      this.http.get<ApiResponse<JourneePersonnel[]>>(`${API}/pointages/journee`, { params }),
    );
  }

  /** Flux brut des pointages de la journée, du plus récent au plus ancien. */
  fluxDuJour(date: string): Observable<PointagePersonnel[]> {
    const params = new HttpParams().set('date', date);
    return extraireDonnees(
      this.http.get<ApiResponse<PointagePersonnel[]>>(`${API}/pointages`, { params }),
    );
  }

  historique(personnelId: string, debut: string, fin: string): Observable<JourneePersonnel[]> {
    const params = new HttpParams().set('debut', debut).set('fin', fin);
    return extraireDonnees(
      this.http.get<ApiResponse<JourneePersonnel[]>>(
        `${API}/pointages/personnel/${personnelId}`,
        { params },
      ),
    );
  }

  /** Régularisation administrative — enregistrée avec la source `MANUEL`. */
  regulariser(demande: PointageManuelRequest): Observable<PointagePersonnel> {
    return extraireDonnees(
      this.http.post<ApiResponse<PointagePersonnel>>(`${API}/pointages/manuel`, demande),
    );
  }

  /** Paramètres horaires : heure d'ouverture et seuil de tolérance affichés en tête d'écran. */
  parametres(): Observable<ParametresEtablissement> {
    return extraireDonnees(
      this.http.get<ApiResponse<ParametresEtablissement>>(`${API}/parametres`),
    );
  }
}
