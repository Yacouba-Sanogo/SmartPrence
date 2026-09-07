import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { messageErreur } from '../../core/api';
import { AuthService } from '../../core/services/auth.service';
import { IconeComponent } from '../../shared/ui/icone.component';

/** Écran de connexion — seule porte d'entrée de l'administration. */
@Component({
  selector: 'sp-connexion',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent],
  template: `
    <div class="flex min-h-screen">
      <!-- Panneau d'identité, masqué sur écran étroit -->
      <div class="hidden w-[46%] flex-col justify-between bg-royal-800 p-12 lg:flex">
        <div class="flex items-center gap-3">
          <span class="grid size-[38px] place-items-center rounded-[11px] bg-royal-600 text-white">
            <sp-icone nom="empreinte" [taille]="21" />
          </span>
          <span class="text-[15px] font-semibold tracking-tight text-white">SmartPresence</span>
        </div>

        <div class="flex max-w-md flex-col gap-5">
          <h2 class="m-0 text-[30px] font-semibold leading-tight tracking-tight text-white">
            La présence, établie par l'empreinte.
          </h2>
          <p class="m-0 text-[14px] leading-relaxed text-royal-200">
            L'identification est réalisée sur le capteur, à l'entrée du bâtiment. Le serveur ne reçoit
            qu'un identifiant et une heure — jamais une empreinte.
          </p>
        </div>

        <div class="flex items-center gap-2.5 text-royal-300">
          <sp-icone nom="bouclier" [taille]="17" [epaisseur]="1.7" />
          <span class="text-[12.5px]">Aucune donnée biométrique n'est stockée en base.</span>
        </div>
      </div>

      <!-- Formulaire -->
      <div class="flex flex-1 items-center justify-center bg-canvas px-5 py-10">
        <form (ngSubmit)="connecter()" class="carte apparait w-full max-w-[400px] px-7 py-8">
          <div class="mb-6 flex flex-col gap-1.5">
            <h1 class="m-0 text-[22px] font-semibold tracking-tight text-ink">Connexion</h1>
            <p class="m-0 text-[13px] text-ink-muted">Accédez à l'administration SmartPresence.</p>
          </div>

          @if (erreur()) {
            <p
              class="mb-4 flex items-start gap-2.5 rounded-[10px] border border-danger-line bg-danger-bg px-3.5 py-3 text-[12.5px] leading-relaxed text-danger"
              role="alert"
            >
              <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" class="mt-px" />
              {{ erreur() }}
            </p>
          }

          <div class="flex flex-col gap-4">
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Adresse email</span>
              <span class="champ !h-11">
                <input
                  type="email"
                  name="email"
                  class="saisie"
                  autocomplete="username"
                  placeholder="prenom.nom@univ.ml"
                  required
                  [ngModel]="email()"
                  (ngModelChange)="email.set($event)"
                />
              </span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Mot de passe</span>
              <span class="champ !h-11">
                <input
                  [type]="motDePasseVisible() ? 'text' : 'password'"
                  name="motDePasse"
                  class="saisie"
                  autocomplete="current-password"
                  placeholder="••••••••"
                  required
                  [ngModel]="motDePasse()"
                  (ngModelChange)="motDePasse.set($event)"
                />
                <button
                  type="button"
                  class="text-[11.5px] font-medium text-royal-600"
                  (click)="motDePasseVisible.set(!motDePasseVisible())"
                >
                  {{ motDePasseVisible() ? 'Masquer' : 'Afficher' }}
                </button>
              </span>
            </label>

            <button type="submit" class="btn btn-primaire !h-11 w-full" [disabled]="!valide() || envoi()">
              {{ envoi() ? 'Connexion…' : 'Se connecter' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class ConnexionComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly email = signal('');
  readonly motDePasse = signal('');
  readonly motDePasseVisible = signal(false);
  readonly envoi = signal(false);
  readonly erreur = signal('');

  readonly valide = computed(() => this.email().trim() !== '' && this.motDePasse() !== '');

  connecter(): void {
    if (!this.valide() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');

    this.auth.login({ email: this.email().trim(), motDePasse: this.motDePasse() }).subscribe({
      next: () => {
        // On revient à la page initialement demandée lorsque le garde en a mémorisé une.
        const suite = this.route.snapshot.queryParamMap.get('suite');
        void this.router.navigateByUrl(suite || '/tableau-de-bord');
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.envoi.set(false);
      },
    });
  }
}
