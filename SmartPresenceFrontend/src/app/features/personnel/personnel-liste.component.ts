import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import {
  Personnel,
  TYPES_PERSONNEL,
  TypePersonnel,
  initialesAgent,
  libelleType,
  nomComplet,
} from '../../core/models/personnel.model';
import { PersonnelService } from '../../core/services/personnel.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';
import {
  EnrolementDialogComponent,
  SujetEnrolement,
} from '../../shared/ui/enrolement-dialog.component';

type FiltreEnrolement = 'TOUS' | 'ENROLES' | 'NON_ENROLES';

/**
 * Référentiel du personnel.
 *
 * La colonne « Empreinte » est le point d'entrée du dispositif : un agent non enrôlé
 * ne peut pas pointer, et son absence de relevé n'aurait alors rien à voir avec son
 * assiduité. D'où le bandeau d'alerte lorsque des agents restent en attente.
 */
@Component({
  selector: 'sp-personnel-liste',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent, EnrolementDialogComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Personnel</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Référentiel des agents et état de leur enrôlement biométrique. Un agent non enrôlé ne peut
            pas pointer.
          </p>
        </div>
        <button type="button" class="btn btn-secondaire shrink-0" (click)="charger()" [disabled]="chargement()">
          <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
          Actualiser
        </button>
      </div>

      @if (nonEnroles() > 0 && !chargement()) {
        <div class="flex items-center gap-3.5 rounded-[12px] border border-alerte-line bg-[#FFF9EC] px-4 py-3.5">
          <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-alerte-bg text-alerte">
            <sp-icone nom="alerte" [taille]="17" [epaisseur]="1.7" />
          </span>
          <span class="flex min-w-0 flex-1 flex-col gap-0.5">
            <span class="text-[13.5px] font-semibold text-[#6E4806]">
              {{ nonEnroles() }} agent(s) ne sont pas encore enrôlés
            </span>
            <span class="text-xs text-[#8A6420]">
              Tant que leur empreinte n'est pas associée, leurs arrivées ne peuvent pas être relevées
              automatiquement.
            </span>
          </span>
          <button
            type="button"
            class="btn shrink-0 border border-[#E0C489] bg-white !text-[12.5px] font-semibold text-[#6E4806]"
            (click)="filtreEnrolement.set('NON_ENROLES')"
          >
            Voir les agents concernés
          </button>
        </div>
      }

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

        <select
          class="champ text-ink"
          [ngModel]="filtreType()"
          (ngModelChange)="filtreType.set($event)"
          aria-label="Filtrer par catégorie"
        >
          <option [ngValue]="null">Toutes les catégories</option>
          @for (type of types; track type) {
            <option [ngValue]="type">{{ libelleType(type) }}</option>
          }
        </select>

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
          [ngModel]="filtreEnrolement()"
          (ngModelChange)="filtreEnrolement.set($event)"
          aria-label="Filtrer par état d'enrôlement"
        >
          <option value="TOUS">Empreinte : toutes</option>
          <option value="ENROLES">Enrôlés</option>
          <option value="NON_ENROLES">Non enrôlés</option>
        </select>
      </div>

      <section class="carte overflow-hidden">
        @if (chargement()) {
          <sp-etat titre="Chargement du référentiel…" icone="horloge" />
        } @else if (erreur()) {
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger le personnel"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        } @else if (agentsFiltres().length === 0) {
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
                  <th class="th">Catégorie</th>
                  <th class="th">Service</th>
                  <th class="th">Contact</th>
                  <th class="th">Empreinte</th>
                  <th class="th">État</th>
                  <th class="th !pr-[18px] text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (agent of agentsFiltres(); track agent.id) {
                  <tr class="hover:bg-[#FBFCFE]">
                    <td class="td !pl-[18px]">
                      <div class="flex items-center gap-3">
                        <span
                          class="grid size-8 shrink-0 place-items-center rounded-full bg-line-faint text-[11.5px] font-semibold text-[#4A5470]"
                        >
                          {{ initialesAgent(agent) }}
                        </span>
                        <span class="flex min-w-0 flex-col gap-0.5">
                          <span class="truncate text-[13.5px] font-medium text-ink">
                            {{ nomComplet(agent) }}
                          </span>
                          <span class="num text-[11px] text-ink-faint">{{ agent.matricule }}</span>
                        </span>
                      </div>
                    </td>
                    <td class="td">
                      <span class="badge bg-line-faint font-medium text-[#4A5470]">
                        {{ libelleType(agent.type) }}
                      </span>
                    </td>
                    <td class="td text-[#364057]">{{ agent.service || '—' }}</td>
                    <td class="td">
                      <span class="flex flex-col gap-0.5">
                        <span class="truncate text-[12.5px] text-[#364057]">{{ agent.email || '—' }}</span>
                        @if (agent.telephone) {
                          <span class="num text-[11px] text-ink-faint">{{ agent.telephone }}</span>
                        }
                      </span>
                    </td>
                    <td class="td">
                      @if (agent.enrole) {
                        <span class="flex flex-col gap-0.5">
                          <span class="badge self-start bg-succes-bg text-succes">
                            <sp-icone nom="coche" [taille]="12" [epaisseur]="2.2" />
                            Enrôlé
                          </span>
                          <span class="num pl-0.5 text-[10.5px] text-ink-faint">
                            {{ agent.biometricId }}
                          </span>
                        </span>
                      } @else {
                        <span class="badge border border-alerte-line bg-[#FFFBF2] font-medium text-alerte">
                          <sp-icone nom="info" [taille]="12" [epaisseur]="2" />
                          Non enrôlé
                        </span>
                      }
                    </td>
                    <td class="td">
                      <span
                        class="flex items-center gap-1.5 text-[12.5px]"
                        [class]="agent.actif ? 'text-succes' : 'text-ink-faint'"
                      >
                        <span
                          class="size-1.5 rounded-full"
                          [class]="agent.actif ? 'bg-succes' : 'bg-ink-faint'"
                        ></span>
                        {{ agent.actif ? 'Actif' : 'Inactif' }}
                      </span>
                    </td>
                    <td class="td !pr-[18px]">
                      <div class="flex items-center justify-end gap-2">
                        @if (!agent.enrole && agent.actif) {
                          <button
                            type="button"
                            class="btn btn-primaire !h-[30px] !px-3 !text-xs"
                            (click)="ouvrirEnrolement(agent)"
                          >
                            <sp-icone nom="empreinte" [taille]="13" [epaisseur]="1.7" />
                            Enrôler
                          </button>
                        } @else if (agent.enrole) {
                          <button
                            type="button"
                            class="btn btn-secondaire !h-[30px] !px-3 !text-xs"
                            (click)="revoquer(agent)"
                            [disabled]="revocationEnCours() === agent.id"
                          >
                            Révoquer
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <footer
            class="flex flex-wrap items-center gap-2 border-t border-line-soft bg-[#FCFDFE] px-[18px] py-3 text-xs text-ink-subtle"
          >
            <span>{{ agentsFiltres().length }} agent(s) sur {{ agents().length }} —</span>
            <span class="font-medium text-succes">{{ enroles() }} enrôlés</span>
            <span>·</span>
            <span class="font-medium text-alerte">{{ nonEnroles() }} en attente</span>
          </footer>
        }
      </section>
    </div>

    @if (agentAEnroler(); as agent) {
      <sp-enrolement-dialog
        [sujet]="sujetEnrolement(agent)"
        prefixe="PER-"
        sousTitre="Associer une empreinte à un agent du référentiel"
        [envoi]="enrolementEnCours()"
        [erreur]="erreurEnrolement()"
        (fermer)="fermerEnrolement()"
        (confirmer)="enroler($event)"
      />
    }
  `,
})
export class PersonnelListeComponent {
  private readonly service = inject(PersonnelService);

  protected readonly nomComplet = nomComplet;
  protected readonly initialesAgent = initialesAgent;
  protected readonly libelleType = libelleType;
  protected readonly types = TYPES_PERSONNEL;

  readonly agents = signal<Personnel[]>([]);
  readonly chargement = signal(false);
  /** Echec de chargement : l'ecran entier bascule en erreur. */
  readonly erreur = signal('');

  /**
   * Echec d'une action ponctuelle. Distinct de {@link erreur} : la liste reste
   * affichee, seul un bandeau signale le refus.
   */
  readonly erreurAction = signal('');
  readonly agentAEnroler = signal<Personnel | null>(null);
  readonly enrolementEnCours = signal(false);
  readonly erreurEnrolement = signal('');
  readonly revocationEnCours = signal<string | null>(null);

  readonly recherche = signal('');
  readonly filtreType = signal<TypePersonnel | null>(null);
  readonly filtreService = signal<string | null>(null);
  readonly filtreEnrolement = signal<FiltreEnrolement>('TOUS');

  readonly enroles = computed(() => this.agents().filter((a) => a.enrole).length);
  readonly nonEnroles = computed(() => this.agents().filter((a) => !a.enrole && a.actif).length);

  readonly services = computed(() =>
    [...new Set(this.agents().map((a) => a.service).filter((s): s is string => !!s))].sort((a, b) =>
      a.localeCompare(b, 'fr'),
    ),
  );

  readonly agentsFiltres = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    const type = this.filtreType();
    const service = this.filtreService();
    const enrolement = this.filtreEnrolement();

    return this.agents().filter((agent) => {
      if (type && agent.type !== type) return false;
      if (service && agent.service !== service) return false;
      if (enrolement === 'ENROLES' && !agent.enrole) return false;
      if (enrolement === 'NON_ENROLES' && agent.enrole) return false;
      if (!terme) return true;
      return `${agent.prenom} ${agent.nom} ${agent.matricule} ${agent.service ?? ''} ${agent.email ?? ''}`
        .toLowerCase()
        .includes(terme);
    });
  });

  constructor() {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.service.lister().subscribe({
      next: (agents) => {
        this.agents.set(agents ?? []);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  ouvrirEnrolement(agent: Personnel): void {
    this.agentAEnroler.set(agent);
    this.erreurEnrolement.set('');
  }

  fermerEnrolement(): void {
    this.agentAEnroler.set(null);
    this.erreurEnrolement.set('');
  }

  /** Réduit l'agent à ce que la modale d'enrôlement affiche. */
  protected sujetEnrolement(agent: Personnel): SujetEnrolement {
    return {
      nomComplet: nomComplet(agent),
      matricule: agent.matricule,
      contexte: agent.service,
      initiales: initialesAgent(agent),
    };
  }

  /** L'appel est fait ici : la modale ne connaît aucun référentiel. */
  enroler(biometricId: string): void {
    const agent = this.agentAEnroler();
    if (!agent || this.enrolementEnCours()) return;
    this.enrolementEnCours.set(true);
    this.erreurEnrolement.set('');

    this.service.enroler(agent.id, { biometricId }).subscribe({
      next: (misAJour) => {
        this.remplacer(misAJour);
        this.enrolementEnCours.set(false);
        this.agentAEnroler.set(null);
      },
      error: (erreur: unknown) => {
        this.erreurEnrolement.set(messageErreur(erreur));
        this.enrolementEnCours.set(false);
      },
    });
  }

  revoquer(agent: Personnel): void {
    if (this.revocationEnCours()) return;
    this.revocationEnCours.set(agent.id);
    this.service.revoquerEnrolement(agent.id).subscribe({
      next: (misAJour) => {
        this.remplacer(misAJour);
        this.revocationEnCours.set(null);
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.revocationEnCours.set(null);
      },
    });
  }

  private remplacer(agent: Personnel): void {
    this.agents.update((agents) => agents.map((a) => (a.id === agent.id ? agent : a)));
  }
}
