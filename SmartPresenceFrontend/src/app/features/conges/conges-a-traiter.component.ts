import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import { CongeService, DemandeConge, StatutConge, TypeConge } from '../../core/services/conge.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';

const ONGLETS: { statut: StatutConge; libelle: string }[] = [
  { statut: 'EN_ATTENTE', libelle: 'En attente' },
  { statut: 'ACCEPTEE', libelle: 'Acceptées' },
  { statut: 'REFUSEE', libelle: 'Refusées' },
  { statut: 'ANNULEE', libelle: 'Annulées' },
];

const TYPES: Record<TypeConge, string> = {
  CONGE: 'Congé',
  ABSENCE: 'Absence',
  MALADIE: 'Maladie',
  FORMATION: 'Formation',
};

/**
 * Demandes de congé des enseignants, à accepter ou à refuser.
 *
 * <p>Une demande acceptée touche des séances de l'emploi du temps : le nombre est mis en
 * avant pour que la décision se prenne en connaissance de cause. Le refus exige un motif.</p>
 */
@Component({
  selector: 'sp-conges-a-traiter',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DatePipe, IconeComponent, EtatComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-[min(100%,280px)] flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Demandes de congé</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Demandes des enseignants, soumises depuis l'application mobile. Un refus doit être
            motivé : l'enseignant reçoit votre réponse.
          </p>
        </div>
        <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
          <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
          Actualiser
        </button>
      </div>

      <div class="flex flex-wrap gap-1.5" role="tablist" aria-label="Statut des demandes">
        @for (o of onglets; track o.statut) {
          <button
            type="button"
            role="tab"
            class="btn !h-8 !px-3 !text-[12.5px]"
            [class.btn-primaire]="statut() === o.statut"
            [class.btn-secondaire]="statut() !== o.statut"
            [attr.aria-selected]="statut() === o.statut"
            (click)="choisir(o.statut)"
          >
            {{ o.libelle }}
          </button>
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
      @if (succes()) {
        <div class="rounded-[12px] bg-succes-bg px-4 py-3 text-[13px] text-succes" role="status">
          {{ succes() }}
        </div>
      }

      @if (chargement()) {
        <section class="carte"><sp-etat titre="Chargement des demandes…" icone="horloge" /></section>
      } @else if (erreur()) {
        <section class="carte">
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger les demandes"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        </section>
      } @else if (demandes().length === 0) {
        <section class="carte">
          <sp-etat
            icone="coche-cercle"
            [titre]="statut() === 'EN_ATTENTE' ? 'Aucune demande en attente' : 'Aucune demande'"
            detail="Les demandes des enseignants apparaîtront ici."
          />
        </section>
      } @else {
        @for (d of demandes(); track d.id) {
          <section class="carte flex flex-col gap-3 px-[18px] py-4">
            <header class="flex flex-wrap items-center gap-3">
              <h2 class="m-0 min-w-0 flex-1 text-[15px] font-semibold text-royal-700">
                {{ d.enseignantNom }}
              </h2>
              <span class="badge bg-line-faint text-ink-muted">{{ libelleType(d.type) }}</span>
              @if (d.seancesConcernees > 0) {
                <span class="badge border border-alerte-line bg-alerte-bg text-alerte">
                  {{ d.seancesConcernees }} séance{{ d.seancesConcernees > 1 ? 's' : '' }} touchée{{
                    d.seancesConcernees > 1 ? 's' : ''
                  }}
                </span>
              } @else {
                <span class="badge bg-line-faint text-ink-muted">Aucune séance touchée</span>
              }
            </header>

            <div class="flex flex-wrap items-center gap-2 text-[13px] text-ink">
              <sp-icone nom="calendrier" [taille]="15" [epaisseur]="1.7" />
              <span>Du {{ d.debut | date: 'dd/MM/yyyy' }} au {{ d.fin | date: 'dd/MM/yyyy' }}</span>
              <span class="text-xs text-ink-faint">· demandée le {{ d.creeLe | date: 'dd/MM/yyyy' }}</span>
            </div>

            <p class="m-0 text-[13px] leading-relaxed text-[#364057]">
              {{ d.motif || 'Aucun motif précisé.' }}
            </p>

            @if (d.statut === 'EN_ATTENTE') {
              @if (refusOuvert() === d.id) {
                <div class="flex flex-col gap-2">
                  <label class="text-[13px] font-medium text-ink" [for]="'motif-' + d.id">
                    Motif du refus — obligatoire
                  </label>
                  <textarea
                    [id]="'motif-' + d.id"
                    class="champ min-h-[72px] py-2"
                    maxlength="500"
                    placeholder="Pourquoi la demande est refusée…"
                    [ngModel]="motif()"
                    (ngModelChange)="motif.set($event)"
                  ></textarea>
                  <div class="flex justify-end gap-2">
                    <button
                      type="button"
                      class="btn btn-secondaire !h-8 !px-3 !text-[12px]"
                      (click)="refusOuvert.set(null)"
                    >
                      Annuler
                    </button>
                    <button
                      type="button"
                      class="btn btn-primaire !h-8 !px-3 !text-[12px]"
                      (click)="refuser(d)"
                      [disabled]="enCours() || !motif().trim()"
                    >
                      Confirmer le refus
                    </button>
                  </div>
                </div>
              } @else {
                <div class="flex justify-end gap-2">
                  <button
                    type="button"
                    class="btn btn-secondaire !h-8 !px-3 !text-[12px]"
                    (click)="ouvrirRefus(d)"
                    [disabled]="enCours()"
                  >
                    Refuser…
                  </button>
                  <button
                    type="button"
                    class="btn btn-primaire !h-8 !px-3 !text-[12px]"
                    (click)="accepter(d)"
                    [disabled]="enCours()"
                  >
                    Accepter
                  </button>
                </div>
              }
            } @else if (d.reponse || d.traiteeLe) {
              <p class="m-0 text-xs text-ink-subtle">
                @if (d.traiteeLe) {
                  Traitée le {{ d.traiteeLe | date: 'dd/MM/yyyy' }}.
                }
                @if (d.reponse) {
                  Réponse : {{ d.reponse }}
                }
              </p>
            }
          </section>
        }
      }
    </div>
  `,
})
export class CongesATraiterComponent {
  private readonly service = inject(CongeService);

  protected readonly onglets = ONGLETS;
  readonly statut = signal<StatutConge>('EN_ATTENTE');
  readonly demandes = signal<DemandeConge[]>([]);
  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly erreurAction = signal('');
  readonly succes = signal('');
  readonly enCours = signal(false);
  readonly refusOuvert = signal<string | null>(null);
  readonly motif = signal('');

  constructor() {
    this.charger();
  }

  libelleType(type: TypeConge): string {
    return TYPES[type] ?? type;
  }

  choisir(statut: StatutConge): void {
    if (statut === this.statut()) return;
    this.statut.set(statut);
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.erreurAction.set('');
    this.succes.set('');
    this.refusOuvert.set(null);
    this.service.lister(this.statut()).subscribe({
      next: (liste) => {
        this.demandes.set(liste ?? []);
        this.chargement.set(false);
      },
      error: (e: unknown) => {
        this.erreur.set(messageErreur(e));
        this.chargement.set(false);
      },
    });
  }

  ouvrirRefus(d: DemandeConge): void {
    this.motif.set('');
    this.refusOuvert.set(d.id);
  }

  accepter(d: DemandeConge): void {
    this.decider(d, this.service.accepter(d.id), `Demande de ${d.enseignantNom} acceptée.`);
  }

  refuser(d: DemandeConge): void {
    const motif = this.motif().trim();
    if (!motif) return;
    this.decider(d, this.service.refuser(d.id, motif), `Demande de ${d.enseignantNom} refusée.`);
  }

  private decider(
    d: DemandeConge,
    appel: ReturnType<CongeService['accepter']>,
    message: string,
  ): void {
    if (this.enCours()) return;
    this.enCours.set(true);
    this.erreurAction.set('');
    this.succes.set('');
    appel.subscribe({
      next: () => {
        // La demande quitte la file sans tout recharger.
        this.demandes.update((liste) => liste.filter((x) => x.id !== d.id));
        this.refusOuvert.set(null);
        this.succes.set(message);
        this.enCours.set(false);
      },
      error: (e: unknown) => {
        this.erreurAction.set(messageErreur(e));
        this.enCours.set(false);
      },
    });
  }
}
