import { HttpErrorResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from './models/api.model';

/**
 * Préfixe de l'API.
 *
 * Le backend expose son contexte sous `/api`. En développement, `proxy.conf.json`
 * redirige cet appel vers `http://localhost:8080` : le frontend n'a donc jamais
 * d'adresse de serveur codée en dur, et le déploiement reste en même origine.
 */
export const API = '/api';

/**
 * Extrait la charge utile de l'enveloppe uniforme du backend.
 *
 * Toutes les réponses ont la forme `{ success, message, data, timestamp }` ; les
 * composants ne manipulent que `data`.
 */
export function extraireDonnees<T>(source: Observable<ApiResponse<T>>): Observable<T> {
  return source.pipe(map((reponse) => reponse.data as T));
}

/**
 * Message d'erreur destiné à l'utilisateur.
 *
 * Le backend renvoie ses erreurs métier dans le champ `message` de l'enveloppe ;
 * on le privilégie, et l'on ne retombe sur un message générique que lorsque la
 * réponse n'en contient pas — panne réseau ou erreur non gérée.
 */
export function messageErreur(erreur: unknown): string {
  if (erreur instanceof HttpErrorResponse) {
    const message = (erreur.error as ApiResponse<unknown> | null)?.message;
    if (message) return message;
    if (erreur.status === 0) {
      return "Le serveur est injoignable. Vérifiez que le backend est démarré sur le port 8080.";
    }
    if (erreur.status === 403) return "Vous n'avez pas les droits nécessaires pour cette opération.";
    if (erreur.status === 404) return 'La ressource demandée est introuvable.';
    return `Erreur ${erreur.status} — la requête n'a pas abouti.`;
  }
  return "Une erreur inattendue s'est produite.";
}
