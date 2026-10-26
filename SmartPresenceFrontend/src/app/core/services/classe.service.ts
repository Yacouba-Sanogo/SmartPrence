import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';
import {
  Classe,
  ClasseDetail,
  ClasseRequest,
  Filiere,
  Passage,
  Promotion,
} from '../models/classe.model';

/**
 * Référentiel des classes et de leur équipe pédagogique.
 *
 * C'est ici que se noue le rattachement enseignant → classe, qui conditionne tout
 * l'espace mobile de l'enseignant : sans lui, ses classes et ses feuilles de présence
 * restent vides.
 */
@Injectable({ providedIn: 'root' })
export class ClasseService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/classes`;

  lister(promotionId?: number | null): Observable<Classe[]> {
    let params = new HttpParams();
    if (promotionId) params = params.set('promotionId', String(promotionId));
    return extraireDonnees(this.http.get<ApiResponse<Classe[]>>(this.base, { params }));
  }

  /** Classe avec ses enseignants et son effectif nominatif. */
  detail(id: number): Observable<ClasseDetail> {
    return extraireDonnees(this.http.get<ApiResponse<ClasseDetail>>(`${this.base}/${id}`));
  }

  /** Désigne le responsable de la classe (administrateur seulement) ; `null` retire la désignation. */
  designerResponsable(classeId: number, etudiantId: string | null): Observable<Classe> {
    return extraireDonnees(
      this.http.put<ApiResponse<Classe>>(`${this.base}/${classeId}/responsable`, { etudiantId }),
    );
  }

  creer(classe: ClasseRequest): Observable<Classe> {
    return extraireDonnees(this.http.post<ApiResponse<Classe>>(this.base, classe));
  }

  modifier(id: number, classe: ClasseRequest): Observable<Classe> {
    return extraireDonnees(this.http.put<ApiResponse<Classe>>(`${this.base}/${id}`, classe));
  }

  supprimer(id: number): Observable<void> {
    return extraireDonnees(this.http.delete<ApiResponse<void>>(`${this.base}/${id}`));
  }

  /** Filières de l'ENETP avec leur DER et leurs options (lecture publique). */
  listerFilieres(): Observable<Filiere[]> {
    return extraireDonnees(this.http.get<ApiResponse<Filiere[]>>(`${API}/referentiel/filieres`));
  }

  /**
   * Passage en année supérieure.
   *
   * Sans `appliquer`, le serveur ne fait que simuler : rien n'est déplacé.
   */
  passage(classeId: number | null, appliquer: boolean): Observable<Passage> {
    const chemin =
      classeId === null ? `${this.base}/passage` : `${this.base}/${classeId}/passage`;
    const params = new HttpParams().set('appliquer', String(appliquer));
    return extraireDonnees(this.http.post<ApiResponse<Passage>>(chemin, null, { params }));
  }

  /** Promotions disponibles pour le rattachement d'une classe. */
  listerPromotions(): Observable<Promotion[]> {
    return extraireDonnees(this.http.get<ApiResponse<Promotion[]>>(`${API}/promotions`));
  }
}
