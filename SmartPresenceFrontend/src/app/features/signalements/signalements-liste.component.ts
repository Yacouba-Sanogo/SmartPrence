import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import {
  Signalement,
  StatutSignalement,
  libelleStatutSignalement,
  libelleTypeSignalement,
  teinteStatutSignalement,
} from '../../core/models/signalement.model';
import { SignalementService } from '../../core/services/signalement.service';
import { AttenteService } from '../../core/services/attente.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ArbitrageDialogComponent } from './arbitrage-dialog.component';

const MOIS = [
  'janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin',
  'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.',
];

/**
 * File d'arbitrage des anomalies signalées par les enseignants.
 *
 * <p>C'est le pendant administratif du signalement déposé depuis l'application mobile.
 * L'enseignant témoigne, la scolarité tranche — et accepter un « étudiant non reconnu »
 * ne modifie jamais le relevé d'origine : cela produit un relevé <b>distinct</b>, marqué
 * comme régularisation, pour qu'une présence décidée par un humain ne se confonde jamais
 * avec une identification par le capteur.</p>
 */
@Component({
  selector: 'sp-signalements-liste',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent, ArbitrageDialogComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Signalements</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Anomalies de relevé signalées par les enseignants. Retenir un signalement produit
            une régularisation, jamais une modification du relevé d'origine.
          </p>
        </div>
        <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
          <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
          Actualiser
        </button>
      </div>

      <div class="grid grid-cols-3 gap-3">
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

      <div class="carte flex flex-wrap items-center gap-2 px-4 py-3">
        @for (choix of filtres; track choix.valeur) {
          <button
            type="button"
            class="rounded-[8px] border px-3 py-1.5 text-[12.5px] font-medium transition-colors"
            [class]="
              filtre() === choix.valeur
                ? 'border-royal-600 bg-royal-600 text-white'
                : 'border-line bg-white text-ink-muted hover:bg-canvas'
            "
            (click)="changerFiltre(choix.valeur)"
          >
            {{ choix.libelle }}
          </button>
        }
      </div>

      <section class="flex flex-col gap-3">
        @if (chargement()) {
          <div class="carte"><sp-etat titre="Chargement des signalements…" icone="horloge" /></div>
        } @else if (erreur()) {
          <div class="carte">
            <sp-etat
              variante="erreur"
              icone="alerte"
              titre="Impossible de charger les signalements"
              [detail]="erreur()"
              actionLibelle="Réessayer"
              (action)="charger()"
            />
          </div>
        } @else if (signalements().length === 0) {
          <div class="carte">
            <sp-etat
              icone="bouclier"
              [titre]="filtre() === null ? 'Aucun signalement' : 'Aucun signalement dans cet état'"
              detail="Les enseignants déposent leurs signalements depuis l'application mobile, sur la feuille de présence d'une séance."
            />
          </div>
        } @else {
          @for (signalement of signalements(); track signalement.id) {
            <article class="carte flex flex-col gap-3 px-[18px] py-4">
              <div class="flex flex-wrap items-start gap-3">
                <span
                  class="grid size-9 shrink-0 place-items-center rounded-[10px]"
                  [class]="teinte(signalement.statut)"
                >
                  <sp-icone [nom]="icone(signalement.statut)" [taille]="17" [epaisseur]="1.7" />
                </span>

                <div class="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span class="text-[14px] font-semibold text-ink">
                    {{ libelleType(signalement.type) }}
                  </span>
                  <span class="text-[11.5px] text-ink-faint">
                    {{ signalement.matiereLibelle }} · {{ signalement.classeCode }} ·
                    {{ dateCourte(signalement.seanceDebut) }} · signalé par
                    {{ signalement.enseignantNom }}
                  </span>
                </div>

                <span class="badge shrink-0" [class]="teinte(signalement.statut)">
                  {{ libelleStatut(signalement.statut) }}
                </span>
              </div>

              @if (signalement.etudiantNom) {
                <div class="flex items-center gap-2 pl-12">
                  <sp-icone nom="etudiants" [taille]="14" [epaisseur]="1.7" class="text-ink-faint" />
                  <span class="text-[12.5px] text-ink">{{ signalement.etudiantNom }}</span>
                  @if (signalement.etudiantMatricule) {
                    <span class="num text-[11px] text-ink-faint">
                      {{ signalement.etudiantMatricule }}
                    </span>
                  }
                </div>
              }

              <p
                class="m-0 rounded-[10px] bg-canvas px-3.5 py-3 text-[12.5px] leading-relaxed text-[#364057]"
              >
                {{ signalement.description }}
              </p>

              @if (signalement.commentaireTraitement) {
                <div class="flex flex-col gap-1 pl-12">
                  <span class="text-[11px] font-semibold tracking-wide text-ink-muted">
                    Décision de la scolarité
                  </span>
                  <span class="text-[12.5px] leading-relaxed text-[#364057]">
                    {{ signalement.commentaireTraitement }}
                  </span>
                  @if (signalement.presenceCorrectiveId) {
                    <span class="mt-1 flex items-center gap-1.5 text-[11.5px] text-royal-700">
                      <sp-icone nom="crayon" [taille]="13" [epaisseur]="1.8" />
                      Une régularisation a été ajoutée à la feuille, marquée comme saisie.
                    </span>
                  }
                </div>
              }

              @if (signalement.statut === 'EN_ATTENTE') {
                <div class="flex items-center justify-end gap-2.5 border-t border-line-soft pt-3">
                  <button
                    type="button"
                    class="btn btn-secondaire"
                    (click)="ouvrirArbitrage(signalement, false)"
                  >
                    Écarter
                  </button>
                  <button
                    type="button"
                    class="btn btn-primaire"
                    (click)="ouvrirArbitrage(signalement, true)"
                  >
                    Retenir
                  </button>
                </div>
              }
            </article>
          }
        }
      </section>
    </div>

    @if (arbitrage(); as contexte) {
      <sp-arbitrage-dialog
        [signalement]="contexte.signalement"
        [accepte]="contexte.accepte"
        (fermer)="arbitrage.set(null)"
        (traite)="apresArbitrage()"
      />
    }
  `,
})
export class SignalementsListeComponent {
  private readonly service = inject(SignalementService);
  private readonly attente = inject(AttenteService);

  readonly signalements = signal<Signalement[]>([]);
  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly erreurAction = signal('');
  readonly filtre = signal<StatutSignalement | null>('EN_ATTENTE');
  readonly arbitrage = signal<{ signalement: Signalement; accepte: boolean } | null>(null);

  /** Compteurs de tous les signalements, indépendants du filtre affiché. */
  readonly totaux = signal({ enAttente: 0, acceptes: 0, rejetes: 0 });

  protected readonly filtres: readonly { valeur: StatutSignalement | null; libelle: string }[] = [
    { valeur: 'EN_ATTENTE', libelle: 'À arbitrer' },
    { valeur: 'ACCEPTE', libelle: 'Retenus' },
    { valeur: 'REJETE', libelle: 'Écartés' },
    { valeur: null, libelle: 'Tous' },
  ];

  readonly indicateurs = computed(() => {
    const t = this.totaux();
    return [
      {
        libelle: 'À arbitrer',
        valeur: t.enAttente,
        detail: t.enAttente > 0 ? 'en attente' : 'file vide',
        icone: 'horloge' as const,
        teinte: t.enAttente > 0 ? 'bg-alerte-bg text-alerte' : 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Retenus',
        valeur: t.acceptes,
        detail: 'régularisés',
        icone: 'coche-cercle' as const,
        teinte: 'bg-succes-bg text-succes',
      },
      {
        libelle: 'Écartés',
        valeur: t.rejetes,
        detail: 'sans suite',
        icone: 'moins-cercle' as const,
        teinte: 'bg-line-faint text-[#4A5470]',
      },
    ];
  });

  constructor() {
    this.charger();
  }

  libelleType = libelleTypeSignalement;
  libelleStatut = libelleStatutSignalement;
  teinte = teinteStatutSignalement;

  icone(statut: StatutSignalement): 'horloge' | 'coche-cercle' | 'moins-cercle' {
    if (statut === 'ACCEPTE') return 'coche-cercle';
    if (statut === 'REJETE') return 'moins-cercle';
    return 'horloge';
  }

  dateCourte(instantIso: string): string {
    const date = new Date(instantIso);
    return `${date.getDate()} ${MOIS[date.getMonth()]}`;
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');

    // La liste complète sert aussi aux compteurs : demander les trois états
    // séparément multiplierait les requêtes pour la même information.
    this.service.lister(null).subscribe({
      next: (tous) => {
        const liste = tous ?? [];
        this.totaux.set({
          enAttente: liste.filter((s) => s.statut === 'EN_ATTENTE').length,
          acceptes: liste.filter((s) => s.statut === 'ACCEPTE').length,
          rejetes: liste.filter((s) => s.statut === 'REJETE').length,
        });
        const filtre = this.filtre();
        this.signalements.set(
          [...(filtre ? liste.filter((s) => s.statut === filtre) : liste)].sort((a, b) =>
            b.createdAt.localeCompare(a.createdAt),
          ),
        );
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  changerFiltre(valeur: StatutSignalement | null): void {
    this.filtre.set(valeur);
    this.charger();
  }

  ouvrirArbitrage(signalement: Signalement, accepte: boolean): void {
    this.erreurAction.set('');
    this.arbitrage.set({ signalement, accepte });
  }

  apresArbitrage(): void {
    this.arbitrage.set(null);
    this.charger();
    // Le compteur de la barre latérale doit décroître à l'instant où l'on tranche :
    // c'est le seul moment où quelqu'un le regarde.
    this.attente.rafraichir();
  }
}
