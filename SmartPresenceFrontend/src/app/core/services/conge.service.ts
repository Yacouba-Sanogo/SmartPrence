import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';

export type StatutConge = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE' | 'ANNULEE';
export type TypeConge = 'CONGE' | 'ABSENCE' | 'MALADIE' | 'FORMATION';

/** Demande de congé ou d'absence soumise par un enseignant depuis le mobile. */
export interface DemandeConge {
  id: string;
  enseignantId: string;
  enseignantNom: string;
  type: TypeConge;
  debut: string;
  fin: string;
  motif: string | null;
  statut: StatutConge;
  reponse: string | null;
  traiteeLe: string | null;
  seancesConcernees: number;
  creeLe: string;
}

/**
 * Décision de l'administration sur les demandes de congé des enseignants.
 *
 * Le refus exige une réponse (400 sinon) ; une demande déjà traitée renvoie 409.
 */
@Injectable({ providedIn: 'root' })
export class CongeService {
  private readonly http = inject(HttpClient);

  lister(statut: StatutConge): Observable<DemandeConge[]> {
    const params = new HttpParams().set('statut', statut);
    return extraireDonnees(this.http.get<ApiResponse<DemandeConge[]>>(`${API}/conges`, { params }));
  }

  accepter(id: string, reponse?: string): Observable<DemandeConge> {
    return this.decider(id, { accepter: true, reponse: reponse || undefined });
  }

  refuser(id: string, reponse: string): Observable<DemandeConge> {
    return this.decider(id, { accepter: false, reponse });
  }

  private decider(id: string, corps: { accepter: boolean; reponse?: string }): Observable<DemandeConge> {
    return extraireDonnees(
      this.http.post<ApiResponse<DemandeConge>>(`${API}/conges/${id}/decision`, corps),
    );
  }
}
