import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';

/** Note transmise par un enseignant, en attente de la décision de l'administration. */
export interface NoteATraiter {
  id: string;
  etudiantId: string;
  etudiantNom: string;
  etudiantPrenom: string;
  etudiantMatricule: string | null;
  matiereId: number;
  matiereCode: string | null;
  matiereLibelle: string;
  enseignantId: string;
  enseignantNom: string;
  valeur: number;
  coefficient: number;
  libelle: string | null;
  dateEvaluation: string | null;
  statut: string;
}

/**
 * Décision de l'administration sur les notes transmises.
 *
 * Tant qu'une note n'est pas validée, elle n'apparaît dans aucun bulletin. Les lots
 * sont traités en entier ou pas du tout par le serveur.
 */
@Injectable({ providedIn: 'root' })
export class ValidationNoteService {
  private readonly http = inject(HttpClient);

  aValider(classeId?: number | null): Observable<NoteATraiter[]> {
    let params = new HttpParams();
    if (classeId) params = params.set('classeId', String(classeId));
    return extraireDonnees(
      this.http.get<ApiResponse<NoteATraiter[]>>(`${API}/notes/a-valider`, { params }),
    );
  }

  valider(noteIds: string[]): Observable<NoteATraiter[]> {
    return extraireDonnees(
      this.http.post<ApiResponse<NoteATraiter[]>>(`${API}/notes/validation`, { noteIds }),
    );
  }

  /** Le motif est obligatoire : l'enseignant doit savoir quoi corriger. */
  renvoyer(noteIds: string[], motif: string): Observable<NoteATraiter[]> {
    return extraireDonnees(
      this.http.post<ApiResponse<NoteATraiter[]>>(`${API}/notes/renvoi`, { noteIds, motif }),
    );
  }
}
