import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  ActivatedRoute,
  ActivatedRouteSnapshot,
  NavigationEnd,
  Router,
  RouterOutlet,
} from '@angular/router';
import { filter, map } from 'rxjs';
import { initiales } from '../core/models/auth.model';
import { AuthService } from '../core/services/auth.service';
import { IconeComponent } from '../shared/ui/icone.component';
import { BarreLateraleComponent } from './barre-laterale.component';

/** Données de route consommées par le fil d'Ariane. */
interface DonneesFilAriane {
  titre?: string;
  rubrique?: string;
}

/**
 * Cadre de l'administration : barre latérale fixe, en-tête et zone de contenu.
 *
 * Sous 1024 px la barre latérale devient un tiroir superposé — la table de pointage
 * a besoin de toute la largeur disponible sur un écran étroit.
 */
@Component({
  selector: 'sp-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, BarreLateraleComponent, IconeComponent],
  template: `
    <div class="flex min-h-screen bg-canvas">
      @if (tiroirOuvert()) {
        <button
          type="button"
          aria-label="Fermer le menu"
          (click)="tiroirOuvert.set(false)"
          class="fixed inset-0 z-40 bg-ink/35 lg:hidden"
        ></button>
      }

      <!--
        Le retrait du tiroir est porté par « max-lg:-translate-x-full », et non par
        « -translate-x-full » corrigé d'un « lg:translate-x-0 ». En Tailwind v4 la
        première classe écrit la propriété « translate: -100% » tandis que la seconde
        se contente de renseigner la variable --tw-translate-x : elle ne la reprend
        donc jamais, et la barre restait décalée de 248 px hors de l'écran sur poste
        fixe, laissant une colonne vide à sa place.
      -->
      <div
        class="fixed inset-y-0 left-0 z-50 transition-transform duration-200 lg:static"
        [class.max-lg:-translate-x-full]="!tiroirOuvert()"
      >
        <sp-barre-laterale (navigation)="tiroirOuvert.set(false)" />
      </div>

      <div class="flex min-w-0 flex-1 flex-col">
        <header
          class="flex h-[66px] shrink-0 items-center gap-4 border-b border-line bg-white px-5 sm:px-6"
        >
          <button
            type="button"
            (click)="tiroirOuvert.set(true)"
            aria-label="Ouvrir le menu"
            class="grid size-9 place-items-center rounded-lg border border-line text-ink-muted lg:hidden"
          >
            <sp-icone nom="menu" [taille]="18" />
          </button>

          <nav class="flex min-w-0 items-center gap-2" aria-label="Fil d'Ariane">
            <span class="hidden truncate text-[13px] text-ink-subtle sm:inline">{{ rubrique() }}</span>
            <sp-icone
              nom="chevron-droit"
              [taille]="14"
              [epaisseur]="1.8"
              class="hidden text-ink-faint sm:inline-flex"
            />
            <span class="truncate text-[13px] font-medium text-ink">{{ titre() }}</span>
          </nav>

          <span class="flex-1"></span>

          <div class="flex items-center gap-2.5">
            <span
              class="grid size-[34px] place-items-center rounded-full bg-royal-50 text-[12.5px] font-semibold text-royal-600"
            >
              {{ initiales() }}
            </span>
            <span class="hidden flex-col gap-px sm:flex">
              <span class="text-[13px] font-medium text-ink">{{ nomAffiche() }}</span>
              <span class="text-[11px] text-ink-subtle">{{ roleAffiche() }}</span>
            </span>
          </div>
        </header>

        <main class="min-w-0 flex-1 px-5 py-6 sm:px-6">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly tiroirOuvert = signal(false);

  /**
   * Titre et rubrique lus dans les données de la route active.
   *
   * Le fil d'Ariane suit ainsi le routeur plutôt que `location.pathname`, qui n'est
   * pas réévalué lors d'une navigation interne.
   */
  private readonly donneesRoute = toSignal(
    this.router.events.pipe(
      filter((evenement) => evenement instanceof NavigationEnd),
      map(() => this.donneesRouteActive()),
    ),
    { initialValue: this.donneesRouteActive() },
  );

  /**
   * Agrège les données de route de la racine jusqu'à la feuille active.
   *
   * <p>Le parcours tolère un {@code snapshot} absent : lors de la toute première
   * évaluation, la navigation initiale n'est pas terminée et une route en cours
   * d'activation n'a pas encore d'instantané. Y accéder sans précaution levait une
   * erreur dans la construction du shell — et un shell qui échoue emporte avec lui
   * la barre de navigation et l'ensemble des écrans enfants.</p>
   */
  private donneesRouteActive(): DonneesFilAriane {
    let route: ActivatedRoute | null = this.route;
    let donnees: DonneesFilAriane = {};
    while (route) {
      const instantane = route.snapshot as ActivatedRouteSnapshot | undefined;
      if (instantane?.data) {
        donnees = { ...donnees, ...(instantane.data as DonneesFilAriane) };
      }
      route = route.firstChild;
    }
    return donnees;
  }

  readonly titre = computed(() => this.donneesRoute().titre ?? 'Administration');
  readonly rubrique = computed(() => this.donneesRoute().rubrique ?? 'SmartPresence');

  readonly initiales = computed(() => initiales(this.auth.utilisateur()));

  readonly nomAffiche = computed(() => {
    const utilisateur = this.auth.utilisateur();
    return utilisateur ? `${utilisateur.prenom} ${utilisateur.nom}`.trim() : '';
  });

  /** Rôle le plus significatif de l'utilisateur, affiché sous son nom. */
  readonly roleAffiche = computed(() => {
    const libelles: Record<string, string> = {
      ADMIN: 'Administrateur',
      RH: 'Ressources humaines',
      RESPONSABLE_SCOLARITE: 'Responsable scolarité',
      SUPERVISEUR: 'Superviseur',
      ENSEIGNANT: 'Enseignant',
      PERSONNEL: 'Personnel',
    };
    const ordre = ['ADMIN', 'RH', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR', 'ENSEIGNANT', 'PERSONNEL'];
    const roles = this.auth.roles();
    const principal = ordre.find((role) => roles.includes(role as never));
    return principal ? libelles[principal] : '';
  });
}
