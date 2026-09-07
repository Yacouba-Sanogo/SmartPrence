import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { messageErreur } from '../../core/api';
import { TypePersonnel, libelleType } from '../../core/models/personnel.model';
import {
  JourneePersonnel,
  ParametresEtablissement,
  PointagePersonnel,
  StatutPresence,
  calculerIndicateurs,
  dateDuJourIso,
  dureePresence,
  formaterDateLongue,
  formaterHeure,
  formaterRetard,
  libelleSens,
} from '../../core/models/pointage.model';
import { PointageService } from '../../core/services/pointage.service';
import { BadgeStatutComponent } from '../../shared/ui/badge-statut.component';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';
import { RegularisationDialogComponent } from './regularisation-dialog.component';

type FiltreStatut = StatutPresence | 'TOUS' | 'SUR_SITE';

/**
 * Pointage du jour — écran central du suivi horaire du personnel.
 *
 * Deux lectures complémentaires de la même journée : la synthèse par agent, qui
 * répond à « qui est là et depuis quand », et le flux brut, qui répond à « que
 * vient-il de se passer sur les lecteurs ».
 */
@Component({
  selector: 'sp-pointage-jour',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    IconeComponent,
    BadgeStatutComponent,
    EtatComponent,
    RegularisationDialogComponent,
  ],
  template: `
    <div class="flex flex-col gap-[18px]">
      <!-- Titre et actions -->
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Pointage du jour</h1>
          @if (parametres(); as p) {
            <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
              Prise de service à
              <span class="num font-medium text-ink">{{ formaterHeure(p.heureOuverture) }}</span>,
              tolérance de
              <span class="num font-medium text-ink">{{ p.seuilRetardMinutes }} min</span> — au-delà de
              <span class="num font-medium text-ink">{{ limiteRetard() }}</span
              >, l'arrivée est comptée en retard.
            </p>
          } @else {
            <p class="m-0 text-[13px] text-ink-muted">Suivi des arrivées et des départs du personnel.</p>
          }
        </div>

        <div class="flex shrink-0 items-center gap-2.5">
          <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
            <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
            Actualiser
          </button>
          <button type="button" class="btn btn-primaire" (click)="dialogueOuvert.set(true)">
            <sp-icone nom="plus" [taille]="16" [epaisseur]="1.8" />
            Régulariser un pointage
          </button>
        </div>
      </div>

      <!-- Indicateurs -->
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5">
        @for (kpi of indicateursAffiches(); track kpi.libelle) {
          <div class="carte flex flex-col gap-2.5 px-4 py-3.5">
            <div class="flex items-center gap-2.5">
              <span class="grid size-7 place-items-center rounded-lg" [class]="kpi.teinte">
                <sp-icone [nom]="kpi.icone" [taille]="16" [epaisseur]="1.7" />
              </span>
              <span class="truncate text-xs font-medium text-ink-muted">{{ kpi.libelle }}</span>
            </div>
            <div class="flex items-baseline gap-1.5">
              <span class="num text-[27px] font-semibold leading-none tracking-tight text-ink">
                {{ kpi.valeur }}
              </span>
              <span class="truncate text-[11.5px] text-ink-faint">{{ kpi.detail }}</span>
            </div>
          </div>
        }
      </div>

      <!-- Filtres -->
      <div class="carte flex flex-wrap items-center gap-2.5 px-4 py-3">
        <label class="champ min-w-[210px] flex-1 bg-[#FBFCFE]">
          <sp-icone nom="recherche" [taille]="16" [epaisseur]="1.7" class="text-ink-faint" />
          <input
            type="search"
            class="saisie"
            placeholder="Rechercher un agent, un matricule…"
            [ngModel]="recherche()"
            (ngModelChange)="recherche.set($event)"
            aria-label="Rechercher un agent"
          />
        </label>

        <label class="champ">
          <sp-icone nom="calendrier" [taille]="16" [epaisseur]="1.6" class="text-ink-muted" />
          <input
            type="date"
            class="saisie num"
            [ngModel]="date()"
            (ngModelChange)="changerDate($event)"
            aria-label="Date observée"
          />
        </label>

        <select
          class="champ text-ink"
          [ngModel]="filtreService()"
          (ngModelChange)="filtreService.set($event)"
          aria-label="Filtrer par service"
        >
          <option [ngValue]="null">Tous les services</option>
          @for (service of services(); track service) {
            <option [ngValue]="service">{{ service }}</option>
          }
        </select>

        <select
          class="champ text-ink"
          [ngModel]="filtreType()"
          (ngModelChange)="filtreType.set($event)"
          aria-label="Filtrer par catégorie"
        >
          <option [ngValue]="null">Toutes les catégories</option>
          @for (type of types(); track type) {
            <option [ngValue]="type">{{ libelleType(type) }}</option>
          }
        </select>

        <select
          class="champ text-ink"
          [ngModel]="filtreStatut()"
          (ngModelChange)="filtreStatut.set($event)"
          aria-label="Filtrer par statut"
        >
          <option value="TOUS">Tous les statuts</option>
          <option value="PRESENT">Présents</option>
          <option value="RETARD">En retard</option>
          <option value="ABSENT">Absents</option>
          <option value="SUR_SITE">Encore sur site</option>
        </select>
      </div>

      <!-- Synthèse et flux -->
      <div class="flex flex-col items-start gap-4 xl:flex-row">
        <section class="carte w-full min-w-0 flex-1 overflow-hidden">
          <header class="flex flex-wrap items-center gap-2.5 border-b border-line-soft px-4 py-3.5">
            <h2 class="m-0 text-[14.5px] font-semibold text-ink">Synthèse par agent</h2>
            <span class="rounded-full bg-canvas px-2.5 py-0.5 text-[11.5px] text-ink-subtle">
              {{ journees().length }} agents actifs
            </span>
            <span class="flex-1"></span>
            <span class="text-[11.5px] text-ink-faint">{{ dateLisible() }}</span>
          </header>

          @if (chargement()) {
            <sp-etat titre="Chargement de la journée…" icone="horloge" />
          } @else if (erreur()) {
            <sp-etat
              variante="erreur"
              icone="alerte"
              titre="Impossible de charger le pointage"
              [detail]="erreur()"
              actionLibelle="Réessayer"
              (action)="charger()"
            />
          } @else if (journeesFiltrees().length === 0) {
            <sp-etat
              icone="personnel"
              titre="Aucun agent ne correspond"
              detail="Ajustez la recherche ou les filtres pour élargir le résultat."
            />
          } @else {
            <div class="overflow-x-auto">
              <table>
                <thead>
                  <tr>
                    <th class="th !pl-[18px]">Agent</th>
                    <th class="th">Service</th>
                    <th class="th">Arrivée</th>
                    <th class="th">Départ</th>
                    <th class="th">Retard</th>
                    <th class="th">Présence</th>
                    <th class="th !pr-[18px]">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  @for (journee of journeesFiltrees(); track journee.personnelId) {
                    <tr class="hover:bg-[#FBFCFE]">
                      <td class="td !pl-[18px]">
                        <div class="flex items-center gap-3">
                          <span
                            class="grid size-8 shrink-0 place-items-center rounded-full bg-line-faint text-[11.5px] font-semibold text-[#4A5470]"
                          >
                            {{ initiales(journee) }}
                          </span>
                          <span class="flex min-w-0 flex-col gap-0.5">
                            <span class="truncate text-[13.5px] font-medium text-ink">
                              {{ journee.prenom }} {{ journee.nom }}
                            </span>
                            <span class="num text-[11px] text-ink-faint">{{ journee.matricule }}</span>
                          </span>
                        </div>
                      </td>
                      <td class="td">
                        <span class="flex flex-col gap-0.5">
                          <span class="text-[13px] text-[#364057]">{{ journee.service || '—' }}</span>
                          <span class="text-[10.5px] tracking-wide text-ink-faint">
                            {{ libelleType(journee.type) }}
                          </span>
                        </span>
                      </td>
                      <td class="td num font-medium" [class]="teinteEntree(journee)">
                        {{ formaterHeure(journee.heureEntree) }}
                      </td>
                      <td class="td num font-medium" [class]="journee.heureSortie ? 'text-ink' : 'text-ink-faint'">
                        {{ formaterHeure(journee.heureSortie) }}
                      </td>
                      <td class="td num" [class]="journee.minutesRetard > 0 ? 'text-alerte' : 'text-ink-faint'">
                        {{ formaterRetard(journee.minutesRetard) }}
                      </td>
                      <td class="td">
                        <span class="flex items-baseline gap-1.5">
                          <span class="num text-[13px] text-[#364057]">{{ dureePresence(journee) }}</span>
                          @if (journee.present) {
                            <span class="text-[10.5px] italic text-ink-faint">en cours</span>
                          }
                        </span>
                      </td>
                      <td class="td !pr-[18px]">
                        <div class="flex items-center gap-1.5">
                          <sp-badge-statut [statut]="journee.statut" />
                          @if (journee.present) {
                            <span
                              class="badge border border-royal-200 bg-[#F4F8FF] font-medium text-royal-600"
                            >
                              <span class="size-[5px] rounded-full bg-royal-500"></span>
                              sur site
                            </span>
                          }
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <footer class="border-t border-line-soft bg-[#FCFDFE] px-[18px] py-3 text-xs text-ink-subtle">
              {{ journeesFiltrees().length }} agent(s) affiché(s) sur {{ journees().length }}
            </footer>
          }
        </section>

        <!-- Flux en direct -->
        <aside class="carte w-full shrink-0 overflow-hidden xl:w-[336px]">
          <header class="flex items-center gap-2 border-b border-line-soft px-4 py-3.5">
            <span class="size-[7px] shrink-0 rounded-full bg-emerald-400"></span>
            <h2 class="m-0 text-[14.5px] font-semibold text-ink">Pointages en direct</h2>
            <span class="flex-1"></span>
            <span class="text-[11px] text-ink-faint">{{ flux().length }} aujourd'hui</span>
          </header>

          @if (chargement()) {
            <sp-etat titre="Chargement…" icone="horloge" />
          } @else if (flux().length === 0) {
            <sp-etat
              icone="empreinte"
              titre="Aucun pointage"
              detail="Les identifications apparaîtront ici dès qu'un agent se présentera au lecteur."
            />
          } @else {
            <ul class="m-0 flex list-none flex-col p-0">
              @for (pointage of flux(); track pointage.id) {
                <li class="flex gap-3 border-b border-[#F4F6FA] px-4 py-3 last:border-b-0">
                  <span
                    class="grid size-[30px] shrink-0 place-items-center rounded-lg"
                    [class]="pointage.sens === 'ENTREE' ? 'bg-succes-bg text-succes' : 'bg-[#EFF2F8] text-[#4A5470]'"
                  >
                    <sp-icone
                      [nom]="pointage.sens === 'ENTREE' ? 'entree' : 'sortie'"
                      [taille]="16"
                      [epaisseur]="1.7"
                    />
                  </span>
                  <span class="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span class="flex items-baseline gap-2">
                      <span class="truncate text-[13px] font-medium text-ink">
                        {{ pointage.personnelPrenom }} {{ pointage.personnelNom }}
                      </span>
                      <span class="flex-1"></span>
                      <span
                        class="num shrink-0 text-[13px] font-medium"
                        [class]="pointage.sens === 'ENTREE' ? 'text-succes' : 'text-[#4A5470]'"
                      >
                        {{ formaterHeure(pointage.heurePointage) }}
                      </span>
                    </span>
                    <span class="truncate text-[11px] text-ink-faint">
                      {{ libelleSens(pointage.sens) }} ·
                      {{ pointage.deviceNom || 'Saisie administrative' }}
                    </span>
                    @if (pointage.source === 'MANUEL') {
                      <span
                        class="mt-0.5 self-start rounded-[5px] bg-royal-50 px-1.5 py-0.5 text-[10px] font-semibold tracking-wide text-royal-600"
                      >
                        RÉGULARISÉ
                      </span>
                    } @else if (pointage.statut === 'RETARD') {
                      <span
                        class="mt-0.5 self-start rounded-[5px] bg-alerte-bg px-1.5 py-0.5 text-[10px] font-semibold tracking-wide text-alerte"
                      >
                        RETARD
                      </span>
                    }
                  </span>
                </li>
              }
            </ul>

            @if (latenceMediane(); as latence) {
              <footer class="flex items-center gap-2 bg-[#FCFDFE] px-4 py-3">
                <sp-icone nom="horloge" [taille]="14" [epaisseur]="1.7" class="text-ink-faint" />
                <span class="text-[11px] text-ink-subtle">
                  Latence de synchronisation médiane
                  <span class="num font-medium text-[#364057]">{{ latence }} ms</span>
                </span>
              </footer>
            }
          }
        </aside>
      </div>
    </div>

    @if (dialogueOuvert()) {
      <sp-regularisation-dialog
        [date]="date()"
        (fermer)="dialogueOuvert.set(false)"
        (enregistre)="apresRegularisation()"
      />
    }
  `,
})
export class PointageJourComponent {
  private readonly service = inject(PointageService);

  protected readonly formaterHeure = formaterHeure;
  protected readonly formaterRetard = formaterRetard;
  protected readonly dureePresence = dureePresence;
  protected readonly libelleType = libelleType;
  protected readonly libelleSens = libelleSens;

  readonly date = signal(dateDuJourIso());
  readonly journees = signal<JourneePersonnel[]>([]);
  readonly flux = signal<PointagePersonnel[]>([]);
  readonly parametres = signal<ParametresEtablissement | null>(null);
  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly dialogueOuvert = signal(false);

  readonly recherche = signal('');
  readonly filtreService = signal<string | null>(null);
  readonly filtreType = signal<TypePersonnel | null>(null);
  readonly filtreStatut = signal<FiltreStatut>('TOUS');

  readonly dateLisible = computed(() => formaterDateLongue(this.date()));

  /** Heure au-delà de laquelle une arrivée bascule en retard. */
  readonly limiteRetard = computed(() => {
    const p = this.parametres();
    if (!p) return '—';
    const [heures, minutes] = p.heureOuverture.split(':').map(Number);
    const total = heures * 60 + minutes + p.seuilRetardMinutes;
    const h = Math.floor(total / 60) % 24;
    return `${String(h).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
  });

  readonly services = computed(() =>
    [...new Set(this.journees().map((j) => j.service).filter((s): s is string => !!s))].sort(
      (a, b) => a.localeCompare(b, 'fr'),
    ),
  );

  readonly types = computed(() => [...new Set(this.journees().map((j) => j.type))].sort());

  readonly journeesFiltrees = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    const service = this.filtreService();
    const type = this.filtreType();
    const statut = this.filtreStatut();

    return this.journees().filter((journee) => {
      if (service && journee.service !== service) return false;
      if (type && journee.type !== type) return false;
      if (statut === 'SUR_SITE' && !journee.present) return false;
      if (statut !== 'TOUS' && statut !== 'SUR_SITE' && journee.statut !== statut) return false;
      if (!terme) return true;
      return `${journee.prenom} ${journee.nom} ${journee.matricule} ${journee.service ?? ''}`
        .toLowerCase()
        .includes(terme);
    });
  });

  readonly indicateursAffiches = computed(() => {
    const i = calculerIndicateurs(this.journees());
    return [
      {
        libelle: 'Effectif attendu',
        valeur: i.effectif,
        detail: 'agents actifs',
        icone: 'personnel' as const,
        teinte: 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Présents',
        valeur: i.presents,
        detail: `${i.tauxPresence} %`,
        icone: 'coche-cercle' as const,
        teinte: 'bg-succes-bg text-succes',
      },
      {
        libelle: 'En retard',
        valeur: i.retards,
        // Volontairement sans mention du seuil courant : le statut de chaque pointage
        // est figé à l'ingestion, selon la règle en vigueur ce jour-là. Afficher ici
        // le seuil actuel laisserait croire qu'il explique des lignes historiques
        // qualifiées sous un autre réglage.
        detail: 'arrivées tardives',
        icone: 'horloge' as const,
        teinte: 'bg-alerte-bg text-alerte',
      },
      {
        libelle: 'Absents',
        valeur: i.absents,
        detail: 'aucun pointage',
        icone: 'moins-cercle' as const,
        teinte: 'bg-danger-bg text-danger',
      },
      {
        libelle: 'Encore sur site',
        valeur: i.surSite,
        detail: 'sans sortie',
        icone: 'diffusion' as const,
        teinte: 'bg-royal-50 text-royal-600',
      },
    ];
  });

  /**
   * Médiane plutôt que moyenne : une seule resynchronisation tardive après une
   * coupure réseau suffirait à rendre une moyenne trompeuse.
   */
  readonly latenceMediane = computed(() => {
    const latences = this.flux()
      .map((p) => p.latenceSynchronisationMs)
      .filter((valeur): valeur is number => valeur !== null && valeur >= 0)
      .sort((a, b) => a - b);
    if (latences.length === 0) return null;
    const milieu = Math.floor(latences.length / 2);
    return latences.length % 2 === 0
      ? Math.round((latences[milieu - 1] + latences[milieu]) / 2)
      : latences[milieu];
  });

  constructor() {
    this.charger();
  }

  changerDate(date: string): void {
    if (!date) return;
    this.date.set(date);
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    const date = this.date();

    forkJoin({
      journees: this.service.journee(date),
      flux: this.service.fluxDuJour(date),
      parametres: this.service.parametres(),
    }).subscribe({
      next: ({ journees, flux, parametres }) => {
        this.journees.set(journees ?? []);
        this.flux.set(flux ?? []);
        this.parametres.set(parametres ?? null);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  apresRegularisation(): void {
    this.dialogueOuvert.set(false);
    this.charger();
  }

  protected initiales(journee: JourneePersonnel): string {
    return `${journee.prenom?.[0] ?? ''}${journee.nom?.[0] ?? ''}`.toUpperCase();
  }

  protected teinteEntree(journee: JourneePersonnel): string {
    if (!journee.heureEntree) return 'text-ink-faint';
    return journee.statut === 'RETARD' ? 'text-alerte' : 'text-ink';
  }
}
