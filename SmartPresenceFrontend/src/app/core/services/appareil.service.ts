import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';
import {
  Appareil,
  AppareilRequest,
  Salle,
  StatutAppareil,
  Synchronisation,
} from '../models/appareil.model';

/** Parc des lecteurs ESP32 et journal de leurs synchronisations. */
@Injectable({ providedIn: 'root' })
export class AppareilService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/devices`;

  lister(): Observable<Appareil[]> {
    return extraireDonnees(this.http.get<ApiResponse<Appareil[]>>(this.base));
  }

  consulter(id: string): Observable<Appareil> {
    return extraireDonnees(this.http.get<ApiResponse<Appareil>>(`${this.base}/${id}`));
  }

  creer(appareil: AppareilRequest): Observable<Appareil> {
    return extraireDonnees(this.http.post<ApiResponse<Appareil>>(this.base, appareil));
  }

  modifier(id: string, appareil: AppareilRequest): Observable<Appareil> {
    return extraireDonnees(this.http.put<ApiResponse<Appareil>>(`${this.base}/${id}`, appareil));
  }

  changerStatut(id: string, statut: StatutAppareil): Observable<void> {
    const params = new HttpParams().set('value', statut);
    return extraireDonnees(
      this.http.patch<ApiResponse<void>>(`${this.base}/${id}/statut`, null, { params }),
    );
  }

  supprimer(id: string): Observable<void> {
    return extraireDonnees(this.http.delete<ApiResponse<void>>(`${this.base}/${id}`));
  }

  /** Journal de synchronisation d'un lecteur, du plus récent au plus ancien. */
  journalAppareil(id: string): Observable<Synchronisation[]> {
    return extraireDonnees(
      this.http.get<ApiResponse<Synchronisation[]>>(`${this.base}/${id}/synchronisations`),
    );
  }

  /** Dernières synchronisations du parc, tous lecteurs confondus. */
  journalRecent(limite = 40): Observable<Synchronisation[]> {
    const params = new HttpParams().set('limite', String(limite));
    return extraireDonnees(
      this.http.get<ApiResponse<Synchronisation[]>>(`${API}/synchronisations`, { params }),
    );
  }

  /** Salles disponibles pour l'installation d'un lecteur. */
  listerSalles(): Observable<Salle[]> {
    return extraireDonnees(this.http.get<ApiResponse<Salle[]>>(`${API}/salles`));
  }
}
