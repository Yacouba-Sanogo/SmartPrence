import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Joint le jeton JWT à chaque requête sortante et ferme la session sur un 401.
 *
 * Un 401 signifie que le jeton n'est plus accepté : le conserver ne ferait que
 * produire des échecs en cascade sur tous les écrans.
 */
export const authInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const auth = inject(AuthService);
  const jeton = auth.jeton();

  const requeteAuthentifiee = jeton
    ? requete.clone({ setHeaders: { Authorization: `Bearer ${jeton}` } })
    : requete;

  return suivant(requeteAuthentifiee).pipe(
    catchError((erreur: unknown) => {
      if (erreur instanceof HttpErrorResponse && erreur.status === 401 && auth.connecte()) {
        auth.logout();
      }
      return throwError(() => erreur);
    }),
  );
};
