import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AttenteService } from '../core/services/attente.service';
import { AuthService } from '../core/services/auth.service';
import { IconeComponent } from '../shared/ui/icone.component';
import { LogoEnetpComponent } from '../shared/ui/logo-enetp.component';
import { GroupeNavigation, NAVIGATION } from './navigation';

/**
 * Barre latérale de l'administration.
 *
 * <p>L'entrée active n'est pas un bouton posé sur le bleu mais une échancrure
 * creusée dedans : elle prend la couleur du contenu et déborde jusqu'au bord droit,
 * de sorte que l'écran affiché paraisse en prolonger le rail. Le raccord concave
 * est décrit dans {@code styles.css} sous {@code .lien-nav-actif}.</p>
 */
@Component({
  selector: 'sp-barre-laterale',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconeComponent, LogoEnetpComponent],
  template: `
    <aside class="flex h-full w-[248px] shrink-0 flex-col bg-royal-800 px-3.5 pt-5 pb-4.5">
      <!--
        Le logo de l'établissement plutôt qu'une icône générique : c'est son outil,
        et la plaque claire est nécessaire ici — le disque argenté du logo posé
        directement sur le bleu se lirait comme un autocollant.
      -->
      <a routerLink="/tableau-de-bord" class="flex items-center gap-3 px-2 pb-6 hover:text-white">
        <sp-logo-enetp [taille]="40" [plaque]="true" />
        <span class="flex min-w-0 flex-col gap-0.5">
          <span class="text-[15px] font-semibold tracking-tight text-white">SmartPresence</span>
          <span class="text-[10.5px] uppercase tracking-[0.06em] text-royal-300">ENETP · Administration</span>
        </span>
      </a>

      <nav class="flex flex-col gap-[18px]">
        @for (groupe of groupes(); track groupe.titre) {
          <div class="flex flex-col gap-0.5">
            <span
              class="px-2.5 pb-1.5 text-[10px] font-semibold uppercase tracking-[0.1em] text-royal-300/80"
            >
              {{ groupe.titre }}
            </span>
            @for (entree of groupe.entrees; track entree.chemin) {
              <a
                [routerLink]="entree.chemin"
                routerLinkActive="lien-nav-actif"
                (click)="navigation.emit()"
                class="lien-nav"
              >
                <sp-icone [nom]="entree.icone" [taille]="18" />
                <span>{{ entree.libelle }}</span>
                @if (entree.compteur && attente.compte(entree.compteur) > 0) {
                  <span
                    class="compteur-nav"
                    [attr.aria-label]="attente.compte(entree.compteur) + ' en attente'"
                  >
                    {{ attente.compte(entree.compteur) }}
                  </span>
                }
              </a>
            }
          </div>
        }
      </nav>

      <div class="mt-auto flex flex-col gap-2.5 pt-6">
        <div class="flex items-center gap-2.5 rounded-[9px] bg-white/[0.07] px-3 py-2.5">
          <span class="size-[7px] shrink-0 rounded-full bg-emerald-400"></span>
          <span class="flex min-w-0 flex-col gap-px">
            <span class="text-xs font-medium text-royal-50">Lecteurs biométriques</span>
            <span class="text-[10.5px] text-royal-300">état supervisé depuis « Appareils »</span>
          </span>
        </div>

        <button
          type="button"
          (click)="auth.logout()"
          class="flex w-full items-center gap-3 rounded-lg px-2.5 py-2.5 text-[13.5px] text-royal-100/85 transition-colors hover:bg-danger hover:text-white"
        >
          <sp-icone nom="deconnexion" [taille]="18" />
          <span>Déconnexion</span>
        </button>
      </div>
    </aside>
  `,
})
export class BarreLateraleComponent {
  readonly auth = inject(AuthService);
  readonly attente = inject(AttenteService);

  /** Émis à chaque navigation, pour refermer le tiroir en affichage mobile. */
  readonly navigation = output<void>();

  constructor() {
    this.attente.rafraichir();
  }

  /** Groupes dont au moins une entrée est accessible à l'utilisateur courant. */
  readonly groupes = computed<GroupeNavigation[]>(() =>
    NAVIGATION.map((groupe) => ({
      titre: groupe.titre,
      entrees: groupe.entrees.filter(
        (entree) => entree.roles.length === 0 || this.auth.aUnRole(...entree.roles),
      ),
    })).filter((groupe) => groupe.entrees.length > 0),
  );
}
