import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import {
  Matiere,
  MatiereRequest,
  Seance,
  SeanceRequest,
  StatutSeance,
} from '../models/academique.model';
import { ApiResponse } from '../models/api.model';
import { Promotion } from '../models/classe.model';
import { PromotionRequest } from '../models/academique.model';

/** Filtres de l'emploi du temps, tous facultatifs. */
export interface FiltresSeances {
  debut?: string | null;
  fin?: string | null;
  classeId?: number | null;
  enseignantId?: string | null;
}

/** Référentiel académique : promotions, matières et emploi du temps. */
@Injectable({ providedIn: 'root' })
export class AcademiqueService {
  private readonly http = inject(HttpClient);

  // ----- Promotions --------------------------------------------------

  listerPromotions(): Observable<Promotion[]> {
    return extraireDonnees(this.http.get<ApiResponse<Promotion[]>>(`${API}/promotions`));
  }

  creerPromotion(promotion: PromotionRequest): Observable<Promotion> {
    return extraireDonnees(
      this.http.post<ApiResponse<Promotion>>(`${API}/promotions`, promotion),
    );
  }

  modifierPromotion(id: number, promotion: PromotionRequest): Observable<Promotion> {
    return extraireDonnees(
      this.http.put<ApiResponse<Promotion>>(`${API}/promotions/${id}`, promotion),
    );
  }

  supprimerPromotion(id: number): Observable<void> {
    return extraireDonnees(this.http.delete<ApiResponse<void>>(`${API}/promotions/${id}`));
  }

  // ----- Matières ----------------------------------------------------

  listerMatieres(): Observable<Matiere[]> {
    return extraireDonnees(this.http.get<ApiResponse<Matiere[]>>(`${API}/matieres`));
  }

  creerMatiere(matiere: MatiereRequest): Observable<Matiere> {
    return extraireDonnees(this.http.post<ApiResponse<Matiere>>(`${API}/matieres`, matiere));
  }

  modifierMatiere(id: number, matiere: MatiereRequest): Observable<Matiere> {
    return extraireDonnees(
      this.http.put<ApiResponse<Matiere>>(`${API}/matieres/${id}`, matiere),
    );
  }

  supprimerMatiere(id: number): Observable<void> {
    return extraireDonnees(this.http.delete<ApiResponse<void>>(`${API}/matieres/${id}`));
  }

  // ----- Séances -----------------------------------------------------

  listerSeances(filtres: FiltresSeances = {}): Observable<Seance[]> {
    let params = new HttpParams();
    if (filtres.debut) params = params.set('debut', filtres.debut);
    if (filtres.fin) params = params.set('fin', filtres.fin);
    if (filtres.classeId) params = params.set('classeId', String(filtres.classeId));
    if (filtres.enseignantId) params = params.set('enseignantId', filtres.enseignantId);
    return extraireDonnees(this.http.get<ApiResponse<Seance[]>>(`${API}/seances`, { params }));
  }

  creerSeance(seance: SeanceRequest): Observable<Seance> {
    return extraireDonnees(this.http.post<ApiResponse<Seance>>(`${API}/seances`, seance));
  }

  modifierSeance(id: string, seance: SeanceRequest): Observable<Seance> {
    return extraireDonnees(this.http.put<ApiResponse<Seance>>(`${API}/seances/${id}`, seance));
  }

  supprimerSeance(id: string): Observable<void> {
    return extraireDonnees(this.http.delete<ApiResponse<void>>(`${API}/seances/${id}`));
  }

  changerStatutSeance(id: string, statut: StatutSeance): Observable<void> {
    const params = new HttpParams().set('value', statut);
    return extraireDonnees(
      this.http.patch<ApiResponse<void>>(`${API}/seances/${id}/statut`, null, { params }),
    );
  }
}
