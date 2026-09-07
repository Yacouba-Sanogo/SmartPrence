import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';
import { Classe } from '../models/classe.model';
import { CompteEtudiant, Etudiant, EtudiantRequest } from '../models/etudiant.model';

/** Référentiel des étudiants, enrôlement biométrique et accès mobile. */
@Injectable({ providedIn: 'root' })
export class EtudiantService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/etudiants`;

  lister(classeId?: number | null): Observable<Etudiant[]> {
    let params = new HttpParams();
    if (classeId) params = params.set('classeId', String(classeId));
    return extraireDonnees(this.http.get<ApiResponse<Etudiant[]>>(this.base, { params }));
  }

  creer(etudiant: EtudiantRequest): Observable<Etudiant> {
    return extraireDonnees(this.http.post<ApiResponse<Etudiant>>(this.base, etudiant));
  }

  modifier(id: string, etudiant: EtudiantRequest): Observable<Etudiant> {
    return extraireDonnees(this.http.put<ApiResponse<Etudiant>>(`${this.base}/${id}`, etudiant));
  }

  changerActivite(id: string, actif: boolean): Observable<void> {
    const params = new HttpParams().set('value', String(actif));
    return extraireDonnees(
      this.http.patch<ApiResponse<void>>(`${this.base}/${id}/active`, null, { params }),
    );
  }

  /** Associe la référence logique de l'empreinte capturée sur le lecteur. */
  enroler(id: string, biometricId: string): Observable<Etudiant> {
    return extraireDonnees(
      this.http.post<ApiResponse<Etudiant>>(`${this.base}/${id}/enrolement`, { biometricId }),
    );
  }

  revoquerEnrolement(id: string): Observable<Etudiant> {
    return extraireDonnees(this.http.delete<ApiResponse<Etudiant>>(`${this.base}/${id}/enrolement`));
  }

  /** Ouvre l'accès mobile et renvoie les identifiants — affichés une seule fois. */
  ouvrirCompte(id: string): Observable<CompteEtudiant> {
    return extraireDonnees(
      this.http.post<ApiResponse<CompteEtudiant>>(`${this.base}/${id}/compte`, null),
    );
  }

  fermerCompte(id: string): Observable<Etudiant> {
    return extraireDonnees(this.http.delete<ApiResponse<Etudiant>>(`${this.base}/${id}/compte`));
  }

  /** Classes disponibles pour l'affectation. */
  listerClasses(): Observable<Classe[]> {
    return extraireDonnees(this.http.get<ApiResponse<Classe[]>>(`${API}/classes`));
  }
}
