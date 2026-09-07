import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { IconeComponent, NomIcone } from '../../shared/ui/icone.component';
import { LogoEnetpComponent } from '../../shared/ui/logo-enetp.component';

/** Bloc de présentation d'une capacité du système. */
interface Capacite {
  icone: NomIcone;
  titre: string;
  detail: string;
}

/**
 * Page d'accueil publique de l'administration.
 *
 * <p>L'administration s'ouvrait directement sur un formulaire de connexion. Un
 * formulaire ne dit ni de quel établissement il s'agit, ni ce que fait l'outil : le
 * visiteur devait s'identifier avant de savoir où il était. Cette page répond aux deux
 * questions d'abord.</p>
 *
 * <p>Elle reste publique — aucun garde ne la protège — et ne présente que ce qui est
 * déjà affiché sur la façade de l'école : son nom et sa devise. Aucune donnée.</p>
 */
@Component({
  selector: 'sp-accueil',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconeComponent, LogoEnetpComponent],
  template: `
    <div class="min-h-screen bg-royal-900">
      <!-- Bandeau -->
      <header class="mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
        <div class="flex items-center gap-3">
          <span class="grid size-[38px] place-items-center rounded-[11px] bg-white/15 text-white">
            <sp-icone nom="empreinte" [taille]="21" />
          </span>
          <span class="text-[15px] font-semibold tracking-tight text-white">SmartPresence</span>
        </div>

        <a
          routerLink="/connexion"
          class="rounded-[10px] border border-white/25 px-4 py-2 text-[13px] font-medium text-white transition-colors hover:bg-white/10"
        >
          Se connecter
        </a>
      </header>

      <!-- Hero -->
      <section
        class="mx-auto grid max-w-6xl items-center gap-12 px-6 pb-16 pt-8 lg:grid-cols-[1.15fr_1fr] lg:pb-24 lg:pt-14"
      >
        <div class="flex flex-col items-start gap-6">
          <span
            class="rounded-full bg-white/10 px-3.5 py-1.5 text-[11.5px] font-semibold uppercase tracking-[0.16em] text-[#F7C948]"
          >
            ENETP
          </span>

          <h1
            class="m-0 text-[34px] font-semibold leading-[1.15] tracking-tight text-white sm:text-[44px]"
          >
            La présence,<br />établie par l'empreinte.
          </h1>

          <p class="m-0 max-w-xl text-[15px] leading-relaxed text-royal-200">
            Administration du système de présence biométrique de l'École Normale
            d'Enseignement Technique et Professionnel. L'identification se fait sur le
            capteur, à l'entrée ; le serveur ne reçoit qu'un identifiant et une heure.
          </p>

          <div class="mt-2 flex flex-wrap items-center gap-3">
            <a
              routerLink="/connexion"
              class="flex items-center gap-2.5 rounded-[12px] bg-[#F7C948] px-6 py-3.5 text-[14px] font-semibold text-royal-900 transition-transform hover:-translate-y-0.5"
            >
              {{ connecte() ? "Retour à l'administration" : 'Accéder à l’administration' }}
              <sp-icone nom="chevron-droit" [taille]="17" [epaisseur]="2.2" />
            </a>

            <span class="flex items-center gap-2 text-[12.5px] text-royal-300">
              <sp-icone nom="bouclier" [taille]="16" [epaisseur]="1.7" />
              Aucune donnée biométrique n'est stockée.
            </span>
          </div>
        </div>

        <!-- Carte d'identité de l'établissement -->
        <div class="flex justify-center lg:justify-end">
          <div
            class="flex w-full max-w-[330px] flex-col items-center gap-6 rounded-[26px] bg-white px-8 py-10 shadow-[0_24px_60px_rgba(7,11,37,0.4)]"
          >
            <sp-logo-enetp [taille]="132" />

            <div class="h-px w-full bg-line-soft"></div>

            <div class="flex flex-col items-center gap-1 text-center">
              <span class="text-[12.5px] font-semibold text-ink">
                École Normale d'Enseignement
              </span>
              <span class="text-[12.5px] font-semibold text-ink">
                Technique et Professionnel
              </span>
              <span class="mt-1 text-[11.5px] text-ink-faint">Bamako, Mali</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Ce que fait l'outil -->
      <section class="bg-canvas py-14">
        <div class="mx-auto max-w-6xl px-6">
          <h2 class="m-0 mb-2 text-[20px] font-semibold tracking-tight text-ink">
            Ce que l'administration permet
          </h2>
          <p class="m-0 mb-8 max-w-2xl text-[13.5px] leading-relaxed text-ink-muted">
            Le relevé lui-même est fait par les lecteurs. L'administration prépare ce qui
            le rend possible, et arbitre ce qu'il ne peut pas trancher seul.
          </p>

          <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (capacite of capacites; track capacite.titre) {
              <article class="carte flex flex-col gap-3 px-5 py-5">
                <span class="grid size-10 place-items-center rounded-[12px] bg-royal-50 text-royal-600">
                  <sp-icone [nom]="capacite.icone" [taille]="19" [epaisseur]="1.7" />
                </span>
                <span class="text-[14px] font-semibold text-ink">{{ capacite.titre }}</span>
                <span class="text-[12.5px] leading-relaxed text-ink-muted">
                  {{ capacite.detail }}
                </span>
              </article>
            }
          </div>
        </div>
      </section>

      <footer class="bg-royal-900 py-8">
        <div
          class="mx-auto flex max-w-6xl flex-col items-center gap-3 px-6 text-center sm:flex-row sm:justify-between sm:text-left"
        >
          <span class="text-[12px] text-royal-300">
            SmartPresence — ENETP · Système de présence par empreinte digitale
          </span>
          <span class="text-[12px] text-royal-300">
            Les étudiants et enseignants passent par l'application mobile.
          </span>
        </div>
      </footer>
    </div>
  `,
})
export class AccueilComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Change le libellé du bouton quand une session est déjà ouverte. */
  readonly connecte = this.auth.connecte;

  protected readonly capacites: readonly Capacite[] = [
    {
      icone: 'empreinte',
      titre: 'Enrôlement et lecteurs',
      detail:
        "Associer une empreinte à un étudiant ou un agent, et suivre l'état du parc de " +
        'lecteurs ESP32 installés dans les salles.',
    },
    {
      icone: 'calendrier',
      titre: 'Emploi du temps',
      detail:
        'Planifier les séances. Sans elles, un passage devant un lecteur ne peut se ' +
        'rattacher à aucun cours.',
    },
    {
      icone: 'presences',
      titre: 'Registre des présences',
      detail:
        'Consulter les relevés, en distinguant toujours ce que le capteur a constaté ' +
        "de ce qu'un humain a saisi.",
    },
    {
      icone: 'bouclier',
      titre: 'Arbitrage des signalements',
      detail:
        "L'enseignant témoigne d'une anomalie, la scolarité tranche. Le relevé " +
        "d'origine n'est jamais réécrit.",
    },
    {
      icone: 'classes',
      titre: 'Référentiel académique',
      detail:
        'Promotions, classes, matières, salles — et le rattachement des enseignants, ' +
        'qui ouvre leur espace mobile.',
    },
    {
      icone: 'statistiques',
      titre: 'Assiduité',
      detail:
        "Taux d'assiduité de l'établissement, par classe et dans le temps, calculés " +
        'sur les relevés réels.',
    },
  ];

  /** Réservé à un futur bouton de reprise directe vers le tableau de bord. */
  protected allerAuTableauDeBord(): void {
    void this.router.navigateByUrl('/tableau-de-bord');
  }
}
