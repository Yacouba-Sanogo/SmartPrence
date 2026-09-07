import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';
import { Salle } from '../models/appareil.model';

/** Requête de création ou de modification d'une salle. */
export interface SalleRequest {
  code: string;
  /** Correspond au libellé de l'entité côté backend. */
  nom: string;
  batiment?: string | null;
  capacite?: number | null;
}

/** Inventaire des salles physiques susceptibles d'accueillir un lecteur. */
@Injectable({ providedIn: 'root' })
export class SalleService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/salles`;

  lister(): Observable<Salle[]> {
    return extraireDonnees(this.http.get<ApiResponse<Salle[]>>(this.base));
  }

  creer(salle: SalleRequest): Observable<Salle> {
    return extraireDonnees(this.http.post<ApiResponse<Salle>>(this.base, salle));
  }

  modifier(id: number, salle: SalleRequest): Observable<Salle> {
    return extraireDonnees(this.http.put<ApiResponse<Salle>>(`${this.base}/${id}`, salle));
  }

  supprimer(id: number): Observable<void> {
    return extraireDonnees(this.http.delete<ApiResponse<void>>(`${this.base}/${id}`));
  }
}
