import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { messageErreur } from '../../core/api';
import {
  Appareil,
  StatutAppareil,
  Synchronisation,
  UsageAppareil,
  anciennete,
  formaterInstantCourt,
  libelleStatutAppareil,
  libelleUsage,
  paraitInjoignable,
} from '../../core/models/appareil.model';
import { AppareilService } from '../../core/services/appareil.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';
import { AppareilDialogComponent } from './appareil-dialog.component';

/**
 * Supervision du parc de lecteurs ESP32.
 *
 * <p>Deux lectures complémentaires : l'inventaire, qui dit ce qui est déployé et dans
 * quel état, et le journal de synchronisation, qui dit ce que les lecteurs ont
 * réellement transmis — et à quel prix en tentatives réseau.</p>
 */
@Component({
  selector: 'sp-appareils-liste',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent, AppareilDialogComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Appareils ESP32</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Parc des lecteurs biométriques déployés, leurs clés d'accès et leur activité de
            synchronisation.
          </p>
        </div>
        <div class="flex shrink-0 items-center gap-2.5">
          <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
            <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
            Actualiser
          </button>
          <button type="button" class="btn btn-primaire" (click)="ouvrirDeclaration()">
            <sp-icone nom="plus" [taille]="16" [epaisseur]="1.8" />
            Déclarer un lecteur
          </button>
        </div>
      </div>

      @if (injoignables() > 0 && !chargement()) {
        <div class="flex items-center gap-3.5 rounded-[12px] border border-alerte-line bg-[#FFF9EC] px-4 py-3.5">
          <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-alerte-bg text-alerte">
            <sp-icone nom="alerte" [taille]="17" [epaisseur]="1.7" />
          </span>
          <span class="flex min-w-0 flex-1 flex-col gap-0.5">
            <span class="text-[13.5px] font-semibold text-[#6E4806]">
              {{ injoignables() }} lecteur(s) actif(s) sans contact depuis plus de 15 minutes
            </span>
            <span class="text-xs text-[#8A6420]">
              Un lecteur muet continue de relever localement, mais ses pointages ne remontent plus.
            </span>
          </span>
        </div>
      }

      <!-- Indicateurs -->
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
        @for (kpi of indicateurs(); track kpi.libelle) {
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

      @if (erreurAction()) {
        <div
          class="flex items-start gap-3 rounded-[12px] border border-danger-line bg-danger-bg px-4 py-3.5"
          role="alert"
        >
          <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-white/60 text-danger">
            <sp-icone nom="alerte" [taille]="17" [epaisseur]="1.7" />
          </span>
          <span class="min-w-0 flex-1 text-[13px] leading-relaxed text-danger">{{ erreurAction() }}</span>
          <button
            type="button"
            class="grid size-7 shrink-0 place-items-center rounded-lg text-danger/70 hover:text-danger"
            (click)="erreurAction.set('')"
            aria-label="Masquer"
          >
            <sp-icone nom="fermer" [taille]="15" [epaisseur]="1.9" />
          </button>
        </div>
      }

      <!-- Filtres -->
      <div class="carte flex flex-wrap items-center gap-2.5 px-4 py-3">
        <label class="champ min-w-[210px] flex-1 bg-[#FBFCFE]">
          <sp-icone nom="recherche" [taille]="16" [epaisseur]="1.7" class="text-ink-faint" />
          <input
            type="search"
            class="saisie"
            placeholder="Rechercher un lecteur, une adresse MAC, une salle…"
            [ngModel]="recherche()"
            (ngModelChange)="recherche.set($event)"
            aria-label="Rechercher un lecteur"
          />
        </label>

        <select
          class="champ text-ink"
          [ngModel]="filtreUsage()"
          (ngModelChange)="filtreUsage.set($event)"
          aria-label="Filtrer par vocation"
        >
          <option [ngValue]="null">Toutes les vocations</option>
          <option [ngValue]="'PERSONNEL'">Personnel</option>
          <option [ngValue]="'ETUDIANT'">Étudiants</option>
          <option [ngValue]="'MIXTE'">Mixte</option>
        </select>

        <select
          class="champ text-ink"
          [ngModel]="filtreStatut()"
          (ngModelChange)="filtreStatut.set($event)"
          aria-label="Filtrer par état"
        >
          <option [ngValue]="null">Tous les états</option>
          <option [ngValue]="'ACTIF'">Actif</option>
          <option [ngValue]="'INACTIF'">Inactif</option>
          <option [ngValue]="'HORS_LIGNE'">Hors ligne</option>
          <option [ngValue]="'EN_PANNE'">En panne</option>
        </select>
      </div>

      <div class="flex flex-col items-start gap-4 xl:flex-row">
        <!-- Inventaire -->
        <section class="carte w-full min-w-0 flex-1 overflow-hidden">
          <header class="flex flex-wrap items-center gap-2.5 border-b border-line-soft px-4 py-3.5">
            <h2 class="m-0 text-[14.5px] font-semibold text-ink">Inventaire du parc</h2>
            <span class="rounded-full bg-canvas px-2.5 py-0.5 text-[11.5px] text-ink-subtle">
              {{ appareils().length }} lecteur(s)
            </span>
          </header>

          @if (chargement()) {
            <sp-etat titre="Chargement du parc…" icone="horloge" />
          } @else if (erreur()) {
            <sp-etat
              variante="erreur"
              icone="alerte"
              titre="Impossible de charger les appareils"
              [detail]="erreur()"
              actionLibelle="Réessayer"
              (action)="charger()"
            />
          } @else if (appareils().length === 0) {
            <sp-etat
              icone="appareils"
              titre="Aucun lecteur déclaré"
              detail="Déclarez un lecteur pour qu'il puisse transmettre ses pointages au serveur."
              actionLibelle="Déclarer un lecteur"
              (action)="ouvrirDeclaration()"
            />
          } @else if (appareilsFiltres().length === 0) {
            <sp-etat
              icone="recherche"
              titre="Aucun lecteur ne correspond"
              detail="Ajustez la recherche ou les filtres pour élargir le résultat."
            />
          } @else {
            <div class="overflow-x-auto">
              <table>
                <thead>
                  <tr>
                    <th class="th !pl-[18px]">Lecteur</th>
                    <th class="th">Vocation</th>
                    <th class="th">Emplacement</th>
                    <th class="th">Dernière sync.</th>
                    <th class="th">État</th>
                    <th class="th !pr-[18px] text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (appareil of appareilsFiltres(); track appareil.id) {
                    <tr class="hover:bg-[#FBFCFE]">
                      <td class="td !pl-[18px]">
                        <div class="flex items-center gap-3">
                          <span
                            class="grid size-8 shrink-0 place-items-center rounded-lg"
                            [class]="teinteUsage(appareil.usage)"
                          >
                            <sp-icone nom="appareils" [taille]="16" [epaisseur]="1.7" />
                          </span>
                          <span class="flex min-w-0 flex-col gap-0.5">
                            <span class="truncate text-[13.5px] font-medium text-ink">
                              {{ appareil.nom }}
                            </span>
                            <span class="num text-[11px] uppercase text-ink-faint">
                              {{ appareil.adresseMac }}
                            </span>
                          </span>
                        </div>
                      </td>
                      <td class="td">
                        <span class="badge" [class]="teinteUsage(appareil.usage)">
                          {{ libelleUsage(appareil.usage) }}
                        </span>
                      </td>
                      <td class="td">
                        <span class="flex flex-col gap-0.5">
                          <span class="text-[13px] text-[#364057]">
                            {{ appareil.salleNom || 'Non affecté' }}
                          </span>
                          @if (appareil.versionFirmware) {
                            <span class="num text-[10.5px] text-ink-faint">
                              firmware {{ appareil.versionFirmware }}
                            </span>
                          }
                        </span>
                      </td>
                      <td class="td">
                        <span class="flex flex-col gap-0.5">
                          <span
                            class="text-[13px]"
                            [class]="paraitInjoignable(appareil) ? 'text-alerte' : 'text-[#364057]'"
                          >
                            {{ anciennete(appareil.derniereSynchronisation) }}
                          </span>
                          @if (appareil.derniereSynchronisation) {
                            <span class="num text-[10.5px] text-ink-faint">
                              {{ formaterInstantCourt(appareil.derniereSynchronisation) }}
                            </span>
                          }
                        </span>
                      </td>
                      <td class="td">
                        <div class="flex items-center gap-1.5">
                          <span class="badge" [class]="teinteStatut(appareil.statut)">
                            {{ libelleStatutAppareil(appareil.statut) }}
                          </span>
                          @if (paraitInjoignable(appareil)) {
                            <span
                              class="badge border border-alerte-line bg-[#FFFBF2] font-medium text-alerte"
                              title="Actif mais sans contact récent"
                            >
                              muet
                            </span>
                          }
                        </div>
                      </td>
                      <td class="td !pr-[18px]">
                        <div class="flex items-center justify-end gap-2">
                          <button
                            type="button"
                            class="btn btn-secondaire !h-[30px] !px-3 !text-xs"
                            (click)="basculerStatut(appareil)"
                            [disabled]="actionEnCours() === appareil.id"
                          >
                            {{ appareil.statut === 'ACTIF' ? 'Désactiver' : 'Activer' }}
                          </button>
                          <button
                            type="button"
                            class="grid size-[30px] place-items-center rounded-[7px] border border-line bg-white text-ink-muted transition-colors hover:bg-canvas"
                            (click)="ouvrirModification(appareil)"
                            aria-label="Modifier"
                          >
                            <sp-icone nom="crayon" [taille]="15" [epaisseur]="1.7" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>

        <!-- Journal de synchronisation -->
        <aside class="carte w-full shrink-0 overflow-hidden xl:w-[360px]">
          <header class="flex items-center gap-2 border-b border-line-soft px-4 py-3.5">
            <sp-icone nom="diffusion" [taille]="16" [epaisseur]="1.7" class="text-royal-600" />
            <h2 class="m-0 text-[14.5px] font-semibold text-ink">Journal de synchronisation</h2>
          </header>

          @if (chargement()) {
            <sp-etat titre="Chargement…" icone="horloge" />
          } @else if (journal().length === 0) {
            <sp-etat
              icone="diffusion"
              titre="Aucune synchronisation"
              detail="Le journal se remplira dès qu'un lecteur transmettra un lot au serveur."
            />
          } @else {
            <ul class="m-0 flex list-none flex-col p-0">
              @for (sync of journal(); track sync.id) {
                <li class="flex gap-3 border-b border-[#F4F6FA] px-4 py-3 last:border-b-0">
                  <span
                    class="grid size-[30px] shrink-0 place-items-center rounded-lg"
                    [class]="teinteSync(sync.statut)"
                  >
                    <sp-icone [nom]="iconeSync(sync.statut)" [taille]="16" [epaisseur]="1.7" />
                  </span>
                  <span class="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span class="flex items-baseline gap-2">
                      <span class="truncate text-[13px] font-medium text-ink">{{ sync.deviceNom }}</span>
                      <span class="flex-1"></span>
                      <span class="num shrink-0 text-[11px] text-ink-faint">
                        {{ anciennete(sync.dateHeure) }}
                      </span>
                    </span>
                    <span class="text-[11px] text-ink-subtle">
                      <span class="num">{{ sync.nombreEvenements ?? 0 }}</span> événement(s)
                      @if (sync.nombreTentatives && sync.nombreTentatives > 1) {
                        · <span class="num text-alerte">{{ sync.nombreTentatives }} tentatives</span>
                      }
                    </span>
                    @if (sync.messageErreur) {
                      <span class="mt-0.5 line-clamp-2 text-[10.5px] leading-snug text-danger">
                        {{ sync.messageErreur }}
                      </span>
                    }
                  </span>
                </li>
              }
            </ul>

            @if (tauxSucces() !== null) {
              <footer class="flex items-center gap-2 bg-[#FCFDFE] px-4 py-3">
                <sp-icone nom="coche-cercle" [taille]="14" [epaisseur]="1.7" class="text-ink-faint" />
                <span class="text-[11px] text-ink-subtle">
                  Taux de synchronisation réussie
                  <span class="num font-medium text-[#364057]">{{ tauxSucces() }} %</span>
                  sur les {{ journal().length }} derniers lots
                </span>
              </footer>
            }
          }
        </aside>
      </div>
    </div>

    @if (dialogueOuvert()) {
      <sp-appareil-dialog
        [appareil]="appareilEnEdition()"
        (fermer)="fermerDialogue()"
        (enregistre)="apresEnregistrement()"
      />
    }
  `,
})
export class AppareilsListeComponent {
  private readonly service = inject(AppareilService);

  protected readonly libelleUsage = libelleUsage;
  protected readonly libelleStatutAppareil = libelleStatutAppareil;
  protected readonly anciennete = anciennete;
  protected readonly formaterInstantCourt = formaterInstantCourt;
  protected readonly paraitInjoignable = paraitInjoignable;

  readonly appareils = signal<Appareil[]>([]);
  readonly journal = signal<Synchronisation[]>([]);
  readonly chargement = signal(false);
  /** Échec de chargement : l'écran entier bascule en erreur. */
  readonly erreur = signal('');

  /**
   * Échec d'une action ponctuelle. Distinct de {@link erreur} : la liste reste
   * affichée, seul un bandeau signale le refus.
   */
  readonly erreurAction = signal('');
  readonly actionEnCours = signal<string | null>(null);
  readonly dialogueOuvert = signal(false);
  readonly appareilEnEdition = signal<Appareil | null>(null);

  readonly recherche = signal('');
  readonly filtreUsage = signal<UsageAppareil | null>(null);
  readonly filtreStatut = signal<StatutAppareil | null>(null);

  readonly injoignables = computed(
    () => this.appareils().filter((a) => paraitInjoignable(a)).length,
  );

  readonly indicateurs = computed(() => {
    const parc = this.appareils();
    const actifs = parc.filter((a) => a.statut === 'ACTIF').length;
    const personnels = parc.filter((a) => (a.usage ?? 'MIXTE') !== 'ETUDIANT').length;
    return [
      {
        libelle: 'Lecteurs déclarés',
        valeur: parc.length,
        detail: 'dans le parc',
        icone: 'appareils' as const,
        teinte: 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Actifs',
        valeur: actifs,
        detail: parc.length ? `${Math.round((actifs / parc.length) * 100)} %` : '—',
        icone: 'coche-cercle' as const,
        teinte: 'bg-succes-bg text-succes',
      },
      {
        libelle: 'Sans contact',
        valeur: this.injoignables(),
        detail: 'depuis 15 min',
        icone: 'alerte' as const,
        teinte: 'bg-alerte-bg text-alerte',
      },
      {
        libelle: 'Habilités personnel',
        valeur: personnels,
        detail: 'pointage RH',
        icone: 'empreinte' as const,
        teinte: 'bg-royal-50 text-royal-600',
      },
    ];
  });

  readonly appareilsFiltres = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    const usage = this.filtreUsage();
    const statut = this.filtreStatut();

    return this.appareils().filter((appareil) => {
      if (usage && (appareil.usage ?? 'MIXTE') !== usage) return false;
      if (statut && appareil.statut !== statut) return false;
      if (!terme) return true;
      return `${appareil.nom} ${appareil.adresseMac} ${appareil.salleNom ?? ''} ${appareil.salleCode ?? ''}`
        .toLowerCase()
        .includes(terme);
    });
  });

  /** Part des lots passés intégralement — indicateur de fiabilité du lien réseau. */
  readonly tauxSucces = computed(() => {
    const lots = this.journal();
    if (lots.length === 0) return null;
    const succes = lots.filter((s) => s.statut === 'SUCCES').length;
    return Math.round((succes / lots.length) * 100);
  });

  constructor() {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');

    forkJoin({
      appareils: this.service.lister(),
      // Le journal est un complément : son indisponibilité ne doit pas masquer l'inventaire.
      journal: this.service.journalRecent(40).pipe(catchError(() => of([] as Synchronisation[]))),
    }).subscribe({
      next: ({ appareils, journal }) => {
        this.appareils.set(appareils ?? []);
        this.journal.set(journal ?? []);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  ouvrirDeclaration(): void {
    this.appareilEnEdition.set(null);
    this.dialogueOuvert.set(true);
  }

  ouvrirModification(appareil: Appareil): void {
    this.appareilEnEdition.set(appareil);
    this.dialogueOuvert.set(true);
  }

  fermerDialogue(): void {
    this.dialogueOuvert.set(false);
    this.appareilEnEdition.set(null);
  }

  apresEnregistrement(): void {
    this.fermerDialogue();
    this.charger();
  }

  basculerStatut(appareil: Appareil): void {
    if (this.actionEnCours()) return;
    const cible: StatutAppareil = appareil.statut === 'ACTIF' ? 'INACTIF' : 'ACTIF';
    this.actionEnCours.set(appareil.id);
    this.erreurAction.set('');

    this.service.changerStatut(appareil.id, cible).subscribe({
      next: () => {
        this.appareils.update((parc) =>
          parc.map((a) => (a.id === appareil.id ? { ...a, statut: cible } : a)),
        );
        this.actionEnCours.set(null);
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.actionEnCours.set(null);
      },
    });
  }

  protected teinteUsage(usage: UsageAppareil | null): string {
    switch (usage ?? 'MIXTE') {
      case 'PERSONNEL':
        return 'bg-royal-50 text-royal-600';
      case 'ETUDIANT':
        return 'bg-succes-bg text-succes';
      default:
        return 'bg-line-faint text-[#4A5470]';
    }
  }

  protected teinteStatut(statut: StatutAppareil): string {
    switch (statut) {
      case 'ACTIF':
        return 'bg-succes-bg text-succes';
      case 'INACTIF':
        return 'bg-line-faint text-ink-muted';
      case 'HORS_LIGNE':
        return 'bg-alerte-bg text-alerte';
      case 'EN_PANNE':
        return 'bg-danger-bg text-danger';
    }
  }

  protected teinteSync(statut: Synchronisation['statut']): string {
    switch (statut) {
      case 'SUCCES':
        return 'bg-succes-bg text-succes';
      case 'PARTIEL':
        return 'bg-alerte-bg text-alerte';
      case 'ECHEC':
        return 'bg-danger-bg text-danger';
    }
  }

  protected iconeSync(statut: Synchronisation['statut']): 'coche-cercle' | 'alerte' | 'moins-cercle' {
    switch (statut) {
      case 'SUCCES':
        return 'coche-cercle';
      case 'PARTIEL':
        return 'alerte';
      case 'ECHEC':
        return 'moins-cercle';
    }
  }
}
