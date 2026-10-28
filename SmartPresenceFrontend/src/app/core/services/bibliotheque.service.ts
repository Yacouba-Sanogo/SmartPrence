import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';

export type TypeDocument = 'MEMOIRE' | 'SUJET';
export type StatutDocument = 'EN_ATTENTE' | 'PUBLIE' | 'REFUSE';
export type StatutEmprunt = 'EN_COURS' | 'EN_RETARD' | 'RETOURNE';

/** Mémoire ou ancien sujet d'examen. */
export interface DocumentBibliotheque {
  id: string;
  type: TypeDocument;
  titre: string;
  auteur: string | null;
  matiere: string | null;
  filiereId: number | null;
  filiereLibelle: string | null;
  annee: number | null;
  description: string | null;
  nomFichier: string;
  typeMime: string;
  taille: number;
  statut: StatutDocument;
  motifRefus: string | null;
  deposePar: string;
  deposeLe: string;
}

/** Livre physique du fonds, avec le nombre d'exemplaires encore sur l'étagère. */
export interface Livre {
  id: string;
  titre: string;
  auteur: string;
  isbn: string | null;
  categorie: string | null;
  filiereId: number | null;
  filiereLibelle: string | null;
  resume: string | null;
  emplacement: string | null;
  exemplaires: number;
  disponibles: number;
}

export interface LivreRequest {
  titre: string;
  auteur: string;
  isbn?: string | null;
  categorie?: string | null;
  filiereId?: number | null;
  resume?: string | null;
  emplacement?: string | null;
  exemplaires: number;
}

export interface Emprunt {
  id: string;
  livreId: string;
  livreTitre: string;
  livreAuteur: string;
  etudiantId: string;
  etudiantNom: string;
  etudiantMatricule: string;
  emprunteLe: string;
  retourPrevu: string;
  retourneLe: string | null;
  statut: StatutEmprunt;
  joursDeRetard: number;
  prolongations: number;
}

export interface SyntheseBibliotheque {
  documentsEnAttente: number;
  empruntsEnCours: number;
  empruntsEnRetard: number;
}

export interface DepotRequest {
  type: TypeDocument;
  titre: string;
  auteur?: string;
  matiere?: string;
  filiereId?: number | null;
  annee?: number | null;
  description?: string;
  fichier: File;
}

/** Bibliothèque : documents numériques, livres physiques, emprunts. */
@Injectable({ providedIn: 'root' })
export class BibliothequeService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/bibliotheque`;

  synthese(): Observable<SyntheseBibliotheque> {
    return extraireDonnees(this.http.get<ApiResponse<SyntheseBibliotheque>>(`${this.base}/synthese`));
  }

  // ----- Documents -----------------------------------------------------

  documents(type?: TypeDocument | null, q?: string): Observable<DocumentBibliotheque[]> {
    let params = new HttpParams();
    if (type) params = params.set('type', type);
    if (q?.trim()) params = params.set('q', q.trim());
    return extraireDonnees(
      this.http.get<ApiResponse<DocumentBibliotheque[]>>(`${this.base}/documents`, { params }),
    );
  }

  aModerer(): Observable<DocumentBibliotheque[]> {
    return extraireDonnees(
      this.http.get<ApiResponse<DocumentBibliotheque[]>>(`${this.base}/documents/a-moderer`),
    );
  }

  deposer(d: DepotRequest): Observable<DocumentBibliotheque> {
    const corps = new FormData();
    corps.set('type', d.type);
    corps.set('titre', d.titre);
    if (d.auteur?.trim()) corps.set('auteur', d.auteur.trim());
    if (d.matiere?.trim()) corps.set('matiere', d.matiere.trim());
    if (d.filiereId) corps.set('filiereId', String(d.filiereId));
    if (d.annee) corps.set('annee', String(d.annee));
    if (d.description?.trim()) corps.set('description', d.description.trim());
    corps.set('fichier', d.fichier, d.fichier.name);
    return extraireDonnees(
      this.http.post<ApiResponse<DocumentBibliotheque>>(`${this.base}/documents`, corps),
    );
  }

  decider(id: string, accepte: boolean, motif?: string): Observable<DocumentBibliotheque> {
    return extraireDonnees(
      this.http.post<ApiResponse<DocumentBibliotheque>>(`${this.base}/documents/${id}/decision`, {
        accepte,
        motif: motif ?? null,
      }),
    );
  }

  retirer(id: string): Observable<void> {
    return extraireDonnees(this.http.delete<ApiResponse<void>>(`${this.base}/documents/${id}`));
  }

  /** Télécharge le fichier avec le jeton de la session, puis l'ouvre dans le navigateur. */
  telecharger(doc: Pick<DocumentBibliotheque, 'id' | 'nomFichier'>): Observable<Blob> {
    return this.http.get(`${this.base}/documents/${doc.id}/fichier`, { responseType: 'blob' });
  }

  // ----- Livres --------------------------------------------------------

  livres(q?: string): Observable<Livre[]> {
    let params = new HttpParams();
    if (q?.trim()) params = params.set('q', q.trim());
    return extraireDonnees(this.http.get<ApiResponse<Livre[]>>(`${this.base}/livres`, { params }));
  }

  creerLivre(livre: LivreRequest): Observable<Livre> {
    return extraireDonnees(this.http.post<ApiResponse<Livre>>(`${this.base}/livres`, livre));
  }

  modifierLivre(id: string, livre: LivreRequest): Observable<Livre> {
    return extraireDonnees(this.http.put<ApiResponse<Livre>>(`${this.base}/livres/${id}`, livre));
  }

  supprimerLivre(id: string): Observable<void> {
    return extraireDonnees(this.http.delete<ApiResponse<void>>(`${this.base}/livres/${id}`));
  }

  // ----- Emprunts ------------------------------------------------------

  emprunts(): Observable<Emprunt[]> {
    return extraireDonnees(this.http.get<ApiResponse<Emprunt[]>>(`${this.base}/emprunts`));
  }

  emprunter(livreId: string, etudiantId: string, dureeJours?: number | null): Observable<Emprunt> {
    return extraireDonnees(
      this.http.post<ApiResponse<Emprunt>>(`${this.base}/emprunts`, {
        livreId,
        etudiantId,
        dureeJours: dureeJours || null,
      }),
    );
  }

  retourner(id: string): Observable<Emprunt> {
    return extraireDonnees(this.http.post<ApiResponse<Emprunt>>(`${this.base}/emprunts/${id}/retour`, {}));
  }

  prolonger(id: string, jours: number): Observable<Emprunt> {
    return extraireDonnees(
      this.http.post<ApiResponse<Emprunt>>(`${this.base}/emprunts/${id}/prolongation`, { jours }),
    );
  }
}
