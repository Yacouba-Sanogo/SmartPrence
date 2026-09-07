import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';

/**
 * Suite donnée à une demande de justification.
 *
 * Les valeurs reprennent l'énumération `StatutJustification` du backend au caractère
 * près. « APPROUVEE » et « REJETEE » paraissaient plus naturels en français, mais le
 * serveur ne les connaît pas : il refusait le paramètre, et la liste affichait la
 * valeur brute au lieu du libellé.
 */
export type StatutJustification = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';

const LIBELLES: Record<StatutJustification, string> = {
  EN_ATTENTE: 'En attente',
  ACCEPTEE: 'Approuvé',
  REFUSEE: 'Rejeté',
};

export function libelleStatutJustification(statut: StatutJustification): string {
  return LIBELLES[statut] ?? statut;
}

export function teinteStatutJustification(statut: StatutJustification): string {
  switch (statut) {
    case 'ACCEPTEE':
      return 'bg-succes-bg text-succes';
    case 'REFUSEE':
      return 'bg-danger-bg text-danger';
    default:
      return 'bg-alerte-bg text-alerte';
  }
}

/**
 * Justificatif d'absence déposé par un étudiant.
 *
 * Le motif peut être médical : ces pièces ne sont accessibles qu'à l'administration et
 * à l'étudiant concerné, jamais à un autre compte.
 */
export interface Justification {
  id: string;
  etudiantId: string;
  etudiantNom: string;
  etudiantPrenom: string;
  seanceId: string | null;
  dateAbsence: string;
  motif: string;
  pieceJointeUrl: string | null;
  statut: StatutJustification;
  traiteParId: string | null;
  commentaireTraitement: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class JustificationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/justifications`;

  lister(): Observable<Justification[]> {
    return extraireDonnees(this.http.get<ApiResponse<Justification[]>>(this.base));
  }

  /**
   * Tranche une demande.
   *
   * Le backend attend des paramètres d'URL, pas un corps JSON — contrairement au reste
   * de l'API. Le service absorbe cette irrégularité pour que les écrans n'aient pas à
   * la connaître.
   */
  traiter(
    id: string,
    statut: Exclude<StatutJustification, 'EN_ATTENTE'>,
    commentaire?: string | null,
  ): Observable<void> {
    let params = new HttpParams().set('statut', statut);
    if (commentaire?.trim()) params = params.set('commentaire', commentaire.trim());
    return extraireDonnees(
      this.http.patch<ApiResponse<void>>(`${this.base}/${id}/traiter`, null, { params }),
    );
  }
}
