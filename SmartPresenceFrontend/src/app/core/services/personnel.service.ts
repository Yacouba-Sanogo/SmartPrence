import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';
import {
  EnrolementRequest,
  Personnel,
  PersonnelRequest,
  TypePersonnel,
} from '../models/personnel.model';

/** Référentiel du personnel et enrôlement biométrique. */
@Injectable({ providedIn: 'root' })
export class PersonnelService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/personnels`;

  lister(options: { type?: TypePersonnel | null; actifsSeul?: boolean } = {}): Observable<Personnel[]> {
    let params = new HttpParams();
    if (options.type) params = params.set('type', options.type);
    if (options.actifsSeul) params = params.set('actifsSeul', 'true');
    return extraireDonnees(this.http.get<ApiResponse<Personnel[]>>(this.base, { params }));
  }

  consulter(id: string): Observable<Personnel> {
    return extraireDonnees(this.http.get<ApiResponse<Personnel>>(`${this.base}/${id}`));
  }

  creer(agent: PersonnelRequest): Observable<Personnel> {
    return extraireDonnees(this.http.post<ApiResponse<Personnel>>(this.base, agent));
  }

  modifier(id: string, agent: PersonnelRequest): Observable<Personnel> {
    return extraireDonnees(this.http.put<ApiResponse<Personnel>>(`${this.base}/${id}`, agent));
  }

  changerActivite(id: string, actif: boolean): Observable<Personnel> {
    const params = new HttpParams().set('value', String(actif));
    return extraireDonnees(
      this.http.patch<ApiResponse<Personnel>>(`${this.base}/${id}/actif`, null, { params }),
    );
  }

  /**
   * Associe la référence logique de l'empreinte à un agent.
   * Aucune donnée biométrique ne transite : seule la référence est transmise.
   */
  enroler(id: string, demande: EnrolementRequest): Observable<Personnel> {
    return extraireDonnees(
      this.http.post<ApiResponse<Personnel>>(`${this.base}/${id}/enrolement`, demande),
    );
  }

  revoquerEnrolement(id: string): Observable<Personnel> {
    return extraireDonnees(
      this.http.delete<ApiResponse<Personnel>>(`${this.base}/${id}/enrolement`),
    );
  }
}
