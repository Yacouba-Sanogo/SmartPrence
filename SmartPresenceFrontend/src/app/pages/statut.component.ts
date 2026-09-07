import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

/** Page d'état : accès refusé ou route inconnue. */
@Component({
  selector: 'sp-statut',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <div class="flex min-h-screen flex-col items-center justify-center gap-4 bg-canvas px-6 text-center">
      <span class="num text-[64px] font-semibold leading-none tracking-tight text-royal-200">
        {{ code }}
      </span>
      <h1 class="m-0 text-[20px] font-semibold text-ink">{{ titre }}</h1>
      <p class="m-0 max-w-md text-[13.5px] leading-relaxed text-ink-muted">{{ detail }}</p>
      <a routerLink="/tableau-de-bord" class="btn btn-primaire mt-2">Revenir au tableau de bord</a>
    </div>
  `,
})
export class StatutComponent {
  private readonly donnees = inject(ActivatedRoute).snapshot.data;

  readonly code = (this.donnees['code'] as string) ?? '404';
  readonly titre = (this.donnees['titre'] as string) ?? 'Page introuvable';
  readonly detail =
    (this.donnees['detail'] as string) ?? "L'adresse demandée ne correspond à aucun écran.";
}
