import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';

/** Indicateurs agrégés du volet académique. */
export interface StatistiquesGlobales {
  totalEtudiants: number;
  totalClasses: number;
  totalDevicesActifs: number;
  totalPresencesEnregistrees: number;
  totalPresents: number;
  totalRetards: number;
  totalAbsents: number;
  totalJustifies: number;
  /** Pourcentage, déjà calculé par le backend. */
  tauxAssiduite: number;
}

/** Indicateurs d'une classe. */
export interface StatistiquesClasse {
  classeId: number;
  classeCode: string;
  classeLibelle: string;
  totalEtudiants: number;
  totalPresences: number;
  totalPresents: number;
  totalRetards: number;
  totalAbsents: number;
  totalJustifies: number;
  tauxAssiduite: number;
}

@Injectable({ providedIn: 'root' })
export class StatistiquesService {
  private readonly http = inject(HttpClient);

  globales(): Observable<StatistiquesGlobales> {
    return extraireDonnees(
      this.http.get<ApiResponse<StatistiquesGlobales>>(`${API}/statistiques/globales`),
    );
  }

  parClasse(classeId: number): Observable<StatistiquesClasse> {
    return extraireDonnees(
      this.http.get<ApiResponse<StatistiquesClasse>>(`${API}/statistiques/classes/${classeId}`),
    );
  }

  /**
   * Nombre de relevés par jour sur une période.
   *
   * Le backend renvoie une map `{ 'YYYY-MM-DD': nombre }` dont les jours sans relevé
   * sont absents : le graphique doit combler les trous lui-même, sinon il tasserait
   * les points et donnerait une courbe fausse.
   */
  tendance(debut: string, fin: string): Observable<Record<string, number>> {
    const params = new HttpParams().set('debut', debut).set('fin', fin);
    return extraireDonnees(
      this.http.get<ApiResponse<Record<string, number>>>(`${API}/statistiques/tendance`, {
        params,
      }),
    );
  }
}
