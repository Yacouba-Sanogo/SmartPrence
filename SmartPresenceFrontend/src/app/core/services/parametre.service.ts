import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, extraireDonnees } from '../api';
import { ApiResponse } from '../models/api.model';
import { ParametresEtablissement } from '../models/pointage.model';

/**
 * Requête de mise à jour des paramètres.
 *
 * <p>Tous les champs sont facultatifs : le backend n'applique que ceux qui sont
 * présents, ce qui évite qu'un formulaire partiel écrase une valeur non modifiée.</p>
 */
export interface ParametresRequest {
  nom?: string | null;
  sigle?: string | null;
  email?: string | null;
  telephone?: string | null;
  fuseauHoraire?: string | null;
  seuilRetardMinutes?: number | null;
  /** Format `HH:mm:ss` attendu par le backend. */
  heureOuverture?: string | null;
  heureFermeture?: string | null;
}

/** Configuration de l'établissement — singleton côté backend. */
@Injectable({ providedIn: 'root' })
export class ParametreService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/parametres`;

  consulter(): Observable<ParametresEtablissement> {
    return extraireDonnees(this.http.get<ApiResponse<ParametresEtablissement>>(this.base));
  }

  enregistrer(parametres: ParametresRequest): Observable<ParametresEtablissement> {
    return extraireDonnees(
      this.http.put<ApiResponse<ParametresEtablissement>>(this.base, parametres),
    );
  }
}

/** Fuseaux de référence conservés hors du continent africain. */
const FUSEAUX_REFERENCE = ['Europe/Paris', 'Europe/London', 'UTC'];

/** Repli lorsque la plateforme n'expose pas la liste des fuseaux. */
const FUSEAUX_REPLI = [
  'Africa/Bamako',
  'Africa/Abidjan',
  'Africa/Dakar',
  'Africa/Ouagadougou',
  'Africa/Conakry',
  'Africa/Niamey',
  'Africa/Lome',
  'Africa/Accra',
  'Africa/Casablanca',
  'Africa/Tunis',
  ...FUSEAUX_REFERENCE,
];

/**
 * Fuseaux horaires proposés à la sélection.
 *
 * <p>La liste est restreinte au continent africain, augmentée de quelques fuseaux de
 * référence. Proposer les quelque 400 zones que connaît la plateforme rendrait la
 * recherche du bon fuseau pénible pour aucun bénéfice : un établissement ne déménage
 * pas d'un continent à l'autre. Le fuseau déjà enregistré est toujours conservé, même
 * s'il sort de ce périmètre — l'écran ne doit jamais faire disparaître silencieusement
 * un réglage existant.</p>
 *
 * <p>Une liste fermée plutôt qu'une saisie libre : une faute de frappe ferait
 * basculer le calcul des retards sur le fuseau du serveur, sans le moindre signal.</p>
 */
export function fuseauxDisponibles(courant: string): string[] {
  const intl = Intl as typeof Intl & { supportedValuesOf?: (cle: string) => string[] };
  const complete = intl.supportedValuesOf?.('timeZone');

  const liste = complete
    ? [
        ...complete.filter((zone) => zone.startsWith('Africa/')),
        ...FUSEAUX_REFERENCE.filter((zone) => complete.includes(zone)),
      ]
    : [...FUSEAUX_REPLI];

  return liste.includes(courant) ? liste : [courant, ...liste];
}
