import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of } from 'rxjs';
import { messageErreur } from '../../core/api';
import { Matiere, MatiereRequest, PromotionRequest } from '../../core/models/academique.model';
import { Promotion } from '../../core/models/classe.model';
import { AcademiqueService } from '../../core/services/academique.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';

/** Ligne en cours de saisie, création comme modification. */
interface BrouillonPromotion {
  id: number | null;
  code: string;
  libelle: string;
  anneeAcademique: number;
}

interface BrouillonMatiere {
  id: number | null;
  code: string;
  libelle: string;
  credits: number | null;
  active: boolean;
}

/**
 * Référentiel académique : promotions et matières.
 *
 * <p>Les deux tiennent sur un seul écran parce qu'ils sont indissociables en pratique —
 * on ne crée pas une classe sans promotion, ni une séance sans matière — et parce que
 * leur formulaire tient en trois champs. Deux écrans séparés auraient allongé la
 * navigation sans rien clarifier.</p>
 *
 * <p>La saisie est <b>en ligne</b>, sans modale : ajouter cinq matières d'affilée est le
 * cas courant au démarrage d'une année, et ouvrir puis fermer une fenêtre cinq fois
 * serait une friction inutile.</p>
 */
@Component({
  selector: 'sp-referentiel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Référentiel</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Promotions et matières — les deux socles sur lesquels reposent les classes et
            l'emploi du temps.
          </p>
        </div>
        <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
          <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
          Actualiser
        </button>
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

      @if (chargement()) {
        <section class="carte"><sp-etat titre="Chargement du référentiel…" icone="horloge" /></section>
      } @else if (erreur()) {
        <section class="carte">
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger le référentiel"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        </section>
      } @else {
        <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
          <!-- ---------------------------------------------------- Promotions -->
          <section class="carte flex flex-col overflow-hidden">
            <header class="flex items-center gap-3 border-b border-line-soft px-[18px] py-3.5">
              <span class="grid size-8 place-items-center rounded-lg bg-royal-50 text-royal-600">
                <sp-icone nom="calendrier" [taille]="16" [epaisseur]="1.7" />
              </span>
              <span class="flex min-w-0 flex-1 flex-col">
                <span class="text-[14px] font-semibold text-ink">Promotions</span>
                <span class="text-[11.5px] text-ink-faint">
                  {{ promotions().length }} cohorte(s)
                </span>
              </span>
            </header>

            <div class="flex flex-col">
              @for (promotion of promotions(); track promotion.id) {
                @if (promotionEnEdition()?.id === promotion.id) {
                  <div class="border-b border-line-soft bg-[#FBFCFE] px-[18px] py-3">
                    @if (promotionEnEdition(); as f) {
                      <div class="flex flex-wrap items-end gap-2">
                        <label class="flex min-w-[110px] flex-1 flex-col gap-1">
                          <span class="text-[10.5px] font-semibold text-ink-muted">Code</span>
                          <span class="champ !h-9">
                            <input
                              type="text"
                              class="saisie num uppercase"
                              [ngModel]="f.code"
                              (ngModelChange)="majPromotion('code', $event)"
                            />
                          </span>
                        </label>
                        <label class="flex min-w-[150px] flex-[2] flex-col gap-1">
                          <span class="text-[10.5px] font-semibold text-ink-muted">Libellé</span>
                          <span class="champ !h-9">
                            <input
                              type="text"
                              class="saisie"
                              [ngModel]="f.libelle"
                              (ngModelChange)="majPromotion('libelle', $event)"
                            />
                          </span>
                        </label>
                        <label class="flex w-[92px] flex-col gap-1">
                          <span class="text-[10.5px] font-semibold text-ink-muted">Année</span>
                          <span class="champ !h-9">
                            <input
                              type="number"
                              class="saisie num"
                              [ngModel]="f.anneeAcademique"
                              (ngModelChange)="majPromotion('anneeAcademique', $event)"
                            />
                          </span>
                        </label>
                        <div class="flex items-center gap-1.5">
                          <button
                            type="button"
                            class="btn btn-primaire !h-9 !px-3"
                            (click)="enregistrerPromotion()"
                            [disabled]="!promotionValide() || envoi()"
                          >
                            <sp-icone nom="coche" [taille]="14" [epaisseur]="2" />
                          </button>
                          <button
                            type="button"
                            class="btn btn-secondaire !h-9 !px-3"
                            (click)="annulerPromotion()"
                          >
                            <sp-icone nom="fermer" [taille]="14" [epaisseur]="2" />
                          </button>
                        </div>
                      </div>
                    }
                  </div>
                } @else {
                  <div
                    class="flex items-center gap-3 border-b border-line-soft px-[18px] py-2.5 hover:bg-[#FBFCFE]"
                  >
                    <span class="num w-[86px] shrink-0 text-[11.5px] uppercase text-ink-faint">
                      {{ promotion.code }}
                    </span>
                    <span class="min-w-0 flex-1 truncate text-[13px] text-ink">
                      {{ promotion.libelle }}
                    </span>
                    <span class="num shrink-0 text-[12px] text-ink-subtle">
                      {{ promotion.anneeAcademique ?? '—' }}
                    </span>
                    <div class="flex shrink-0 items-center gap-1.5">
                      <button
                        type="button"
                        class="grid size-[26px] place-items-center rounded-[6px] border border-line bg-white text-ink-muted hover:bg-canvas"
                        (click)="editerPromotion(promotion)"
                        aria-label="Modifier la promotion"
                      >
                        <sp-icone nom="crayon" [taille]="13" [epaisseur]="1.7" />
                      </button>
                      <button
                        type="button"
                        class="grid size-[26px] place-items-center rounded-[6px] border border-line bg-white text-ink-muted hover:border-danger-line hover:bg-danger-bg hover:text-danger disabled:opacity-50"
                        (click)="supprimerPromotion(promotion)"
                        [disabled]="envoi()"
                        aria-label="Supprimer la promotion"
                      >
                        <sp-icone nom="fermer" [taille]="13" [epaisseur]="1.9" />
                      </button>
                    </div>
                  </div>
                }
              } @empty {
                <p class="m-0 px-[18px] py-6 text-center text-[12.5px] text-ink-subtle">
                  Aucune promotion. Créez-en une : sans elle, aucune classe ne peut exister.
                </p>
              }
            </div>

            <div class="border-t border-line-soft bg-[#FCFDFE] px-[18px] py-3">
                @if (enCreationPromotion()) {
                  @if (promotionEnEdition(); as f) {
                    <div class="flex flex-wrap items-end gap-2">
                      <label class="flex min-w-[110px] flex-1 flex-col gap-1">
                        <span class="text-[10.5px] font-semibold text-ink-muted">Code</span>
                        <span class="champ !h-9">
                          <input
                            type="text"
                            class="saisie num uppercase"
                            placeholder="L3-2026"
                            [ngModel]="f.code"
                            (ngModelChange)="majPromotion('code', $event)"
                          />
                        </span>
                      </label>
                      <label class="flex min-w-[150px] flex-[2] flex-col gap-1">
                        <span class="text-[10.5px] font-semibold text-ink-muted">Libellé</span>
                        <span class="champ !h-9">
                          <input
                            type="text"
                            class="saisie"
                            placeholder="Licence 3"
                            [ngModel]="f.libelle"
                            (ngModelChange)="majPromotion('libelle', $event)"
                          />
                        </span>
                      </label>
                      <label class="flex w-[92px] flex-col gap-1">
                        <span class="text-[10.5px] font-semibold text-ink-muted">Année</span>
                        <span class="champ !h-9">
                          <input
                            type="number"
                            class="saisie num"
                            [ngModel]="f.anneeAcademique"
                            (ngModelChange)="majPromotion('anneeAcademique', $event)"
                          />
                        </span>
                      </label>
                      <div class="flex items-center gap-1.5">
                        <button
                          type="button"
                          class="btn btn-primaire !h-9 !px-3.5"
                          (click)="enregistrerPromotion()"
                          [disabled]="!promotionValide() || envoi()"
                        >
                          Ajouter
                        </button>
                        <button
                          type="button"
                          class="btn btn-secondaire !h-9 !px-3"
                          (click)="annulerPromotion()"
                        >
                          <sp-icone nom="fermer" [taille]="14" [epaisseur]="2" />
                        </button>
                      </div>
                    </div>
                  }
                } @else {
                  <button
                    type="button"
                    class="flex items-center gap-2 text-[12.5px] font-medium text-royal-600 hover:text-royal-800"
                    (click)="nouvellePromotion()"
                  >
                    <sp-icone nom="plus" [taille]="14" [epaisseur]="2" />
                    Ajouter une promotion
                  </button>
                }
            </div>
          </section>

          <!-- ------------------------------------------------------ Matières -->
          <section class="carte flex flex-col overflow-hidden">
            <header class="flex items-center gap-3 border-b border-line-soft px-[18px] py-3.5">
              <span class="grid size-8 place-items-center rounded-lg bg-succes-bg text-succes">
                <sp-icone nom="classes" [taille]="16" [epaisseur]="1.7" />
              </span>
              <span class="flex min-w-0 flex-1 flex-col">
                <span class="text-[14px] font-semibold text-ink">Matières</span>
                <span class="text-[11.5px] text-ink-faint">
                  {{ matieres().length }} matière(s), {{ matieresActives() }} active(s)
                </span>
              </span>
            </header>

            <div class="flex flex-col">
              @for (matiere of matieres(); track matiere.id) {
                @if (matiereEnEdition()?.id === matiere.id) {
                  <div class="border-b border-line-soft bg-[#FBFCFE] px-[18px] py-3">
                    @if (matiereEnEdition(); as f) {
                      <div class="flex flex-wrap items-end gap-2">
                        <label class="flex min-w-[100px] flex-1 flex-col gap-1">
                          <span class="text-[10.5px] font-semibold text-ink-muted">Code</span>
                          <span class="champ !h-9">
                            <input
                              type="text"
                              class="saisie num uppercase"
                              [ngModel]="f.code"
                              (ngModelChange)="majMatiere('code', $event)"
                            />
                          </span>
                        </label>
                        <label class="flex min-w-[150px] flex-[2] flex-col gap-1">
                          <span class="text-[10.5px] font-semibold text-ink-muted">Libellé</span>
                          <span class="champ !h-9">
                            <input
                              type="text"
                              class="saisie"
                              [ngModel]="f.libelle"
                              (ngModelChange)="majMatiere('libelle', $event)"
                            />
                          </span>
                        </label>
                        <label class="flex w-[76px] flex-col gap-1">
                          <span class="text-[10.5px] font-semibold text-ink-muted">Crédits</span>
                          <span class="champ !h-9">
                            <input
                              type="number"
                              class="saisie num"
                              [ngModel]="f.credits"
                              (ngModelChange)="majMatiere('credits', $event)"
                            />
                          </span>
                        </label>
                        <label class="flex h-9 cursor-pointer items-center gap-1.5 px-1">
                          <input
                            type="checkbox"
                            class="size-3.5 accent-[#1b3fa0]"
                            [ngModel]="f.active"
                            (ngModelChange)="majMatiere('active', $event)"
                          />
                          <span class="text-[11.5px] text-ink-muted">Active</span>
                        </label>
                        <div class="flex items-center gap-1.5">
                          <button
                            type="button"
                            class="btn btn-primaire !h-9 !px-3"
                            (click)="enregistrerMatiere()"
                            [disabled]="!matiereValide() || envoi()"
                          >
                            <sp-icone nom="coche" [taille]="14" [epaisseur]="2" />
                          </button>
                          <button
                            type="button"
                            class="btn btn-secondaire !h-9 !px-3"
                            (click)="annulerMatiere()"
                          >
                            <sp-icone nom="fermer" [taille]="14" [epaisseur]="2" />
                          </button>
                        </div>
                      </div>
                    }
                  </div>
                } @else {
                  <div
                    class="flex items-center gap-3 border-b border-line-soft px-[18px] py-2.5 hover:bg-[#FBFCFE]"
                  >
                    <span class="num w-[86px] shrink-0 text-[11.5px] uppercase text-ink-faint">
                      {{ matiere.code }}
                    </span>
                    <span class="min-w-0 flex-1 truncate text-[13px] text-ink">
                      {{ matiere.libelle }}
                    </span>
                    @if (matiere.credits !== null) {
                      <span class="num shrink-0 text-[12px] text-ink-subtle">
                        {{ matiere.credits }} cr.
                      </span>
                    }
                    @if (!matiere.active) {
                      <span class="badge shrink-0 bg-line-faint text-ink-subtle">Inactive</span>
                    }
                    <div class="flex shrink-0 items-center gap-1.5">
                      <button
                        type="button"
                        class="grid size-[26px] place-items-center rounded-[6px] border border-line bg-white text-ink-muted hover:bg-canvas"
                        (click)="editerMatiere(matiere)"
                        aria-label="Modifier la matière"
                      >
                        <sp-icone nom="crayon" [taille]="13" [epaisseur]="1.7" />
                      </button>
                      <button
                        type="button"
                        class="grid size-[26px] place-items-center rounded-[6px] border border-line bg-white text-ink-muted hover:border-danger-line hover:bg-danger-bg hover:text-danger disabled:opacity-50"
                        (click)="supprimerMatiere(matiere)"
                        [disabled]="envoi()"
                        aria-label="Supprimer la matière"
                      >
                        <sp-icone nom="fermer" [taille]="13" [epaisseur]="1.9" />
                      </button>
                    </div>
                  </div>
                }
              } @empty {
                <p class="m-0 px-[18px] py-6 text-center text-[12.5px] text-ink-subtle">
                  Aucune matière. Créez-en une : sans elle, aucune séance ne peut être planifiée.
                </p>
              }
            </div>

            <div class="border-t border-line-soft bg-[#FCFDFE] px-[18px] py-3">
              @if (enCreationMatiere()) {
                @if (matiereEnEdition(); as f) {
                  <div class="flex flex-wrap items-end gap-2">
                    <label class="flex min-w-[100px] flex-1 flex-col gap-1">
                      <span class="text-[10.5px] font-semibold text-ink-muted">Code</span>
                      <span class="champ !h-9">
                        <input
                          type="text"
                          class="saisie num uppercase"
                          placeholder="INF301"
                          [ngModel]="f.code"
                          (ngModelChange)="majMatiere('code', $event)"
                        />
                      </span>
                    </label>
                    <label class="flex min-w-[150px] flex-[2] flex-col gap-1">
                      <span class="text-[10.5px] font-semibold text-ink-muted">Libellé</span>
                      <span class="champ !h-9">
                        <input
                          type="text"
                          class="saisie"
                          placeholder="Algorithmique"
                          [ngModel]="f.libelle"
                          (ngModelChange)="majMatiere('libelle', $event)"
                        />
                      </span>
                    </label>
                    <label class="flex w-[76px] flex-col gap-1">
                      <span class="text-[10.5px] font-semibold text-ink-muted">Crédits</span>
                      <span class="champ !h-9">
                        <input
                          type="number"
                          class="saisie num"
                          [ngModel]="f.credits"
                          (ngModelChange)="majMatiere('credits', $event)"
                        />
                      </span>
                    </label>
                    <div class="flex items-center gap-1.5">
                      <button
                        type="button"
                        class="btn btn-primaire !h-9 !px-3.5"
                        (click)="enregistrerMatiere()"
                        [disabled]="!matiereValide() || envoi()"
                      >
                        Ajouter
                      </button>
                      <button
                        type="button"
                        class="btn btn-secondaire !h-9 !px-3"
                        (click)="annulerMatiere()"
                      >
                        <sp-icone nom="fermer" [taille]="14" [epaisseur]="2" />
                      </button>
                    </div>
                  </div>
                }
              } @else {
                <button
                  type="button"
                  class="flex items-center gap-2 text-[12.5px] font-medium text-royal-600 hover:text-royal-800"
                  (click)="nouvelleMatiere()"
                >
                  <sp-icone nom="plus" [taille]="14" [epaisseur]="2" />
                  Ajouter une matière
                </button>
              }
            </div>
          </section>
        </div>
      }
    </div>
  `,
})
export class ReferentielComponent {
  private readonly service = inject(AcademiqueService);

  readonly promotions = signal<Promotion[]>([]);
  readonly matieres = signal<Matiere[]>([]);
  readonly chargement = signal(false);
  readonly envoi = signal(false);
  readonly erreur = signal('');
  readonly erreurAction = signal('');

  readonly promotionEnEdition = signal<BrouillonPromotion | null>(null);
  readonly matiereEnEdition = signal<BrouillonMatiere | null>(null);

  /**
   * Vrai uniquement lorsqu'une <b>nouvelle</b> ligne est en cours de saisie.
   *
   * L'écriture directe {@code promotionEnEdition()?.id === null} paraît équivalente mais
   * ne l'est pas : dans un template Angular, l'opérateur de navigation sûre renvoie
   * {@code null} — et non {@code undefined} — quand la cible est nulle. La condition
   * était donc vraie à l'état initial, et le pied de carte basculait sur la branche
   * formulaire, elle-même vide faute de brouillon : plus aucun bouton d'ajout.
   */
  readonly enCreationPromotion = computed(() => {
    const f = this.promotionEnEdition();
    return f !== null && f.id === null;
  });

  readonly enCreationMatiere = computed(() => {
    const f = this.matiereEnEdition();
    return f !== null && f.id === null;
  });

  readonly matieresActives = computed(() => this.matieres().filter((m) => m.active).length);

  readonly promotionValide = computed(() => {
    const f = this.promotionEnEdition();
    return !!f && f.code.trim().length > 0 && f.libelle.trim().length > 0 && !!f.anneeAcademique;
  });

  readonly matiereValide = computed(() => {
    const f = this.matiereEnEdition();
    return !!f && f.code.trim().length > 0 && f.libelle.trim().length > 0;
  });

  constructor() {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    forkJoin({
      promotions: this.service.listerPromotions(),
      matieres: this.service.listerMatieres().pipe(catchError(() => of([] as Matiere[]))),
    }).subscribe({
      next: ({ promotions, matieres }) => {
        this.promotions.set(promotions ?? []);
        this.matieres.set(matieres ?? []);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  // ----- Promotions --------------------------------------------------

  nouvellePromotion(): void {
    this.matiereEnEdition.set(null);
    this.promotionEnEdition.set({
      id: null,
      code: '',
      libelle: '',
      anneeAcademique: new Date().getFullYear(),
    });
  }

  editerPromotion(promotion: Promotion): void {
    this.matiereEnEdition.set(null);
    this.promotionEnEdition.set({
      id: promotion.id,
      code: promotion.code,
      libelle: promotion.libelle,
      anneeAcademique: promotion.anneeAcademique ?? new Date().getFullYear(),
    });
  }

  annulerPromotion(): void {
    this.promotionEnEdition.set(null);
  }

  majPromotion(champ: keyof BrouillonPromotion, valeur: unknown): void {
    this.promotionEnEdition.update((f) => (f ? { ...f, [champ]: valeur } : f));
  }

  enregistrerPromotion(): void {
    const f = this.promotionEnEdition();
    if (!f || !this.promotionValide() || this.envoi()) return;

    const requete: PromotionRequest = {
      code: f.code.trim().toUpperCase(),
      libelle: f.libelle.trim(),
      anneeAcademique: Number(f.anneeAcademique),
    };

    this.envoi.set(true);
    this.erreurAction.set('');
    const appel = f.id
      ? this.service.modifierPromotion(f.id, requete)
      : this.service.creerPromotion(requete);

    appel.subscribe({
      next: () => {
        this.envoi.set(false);
        // Rester en création enchaîne naturellement plusieurs ajouts.
        if (f.id) this.promotionEnEdition.set(null);
        else this.nouvellePromotion();
        this.charger();
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.envoi.set(false);
      },
    });
  }

  supprimerPromotion(promotion: Promotion): void {
    if (this.envoi()) return;
    this.envoi.set(true);
    this.erreurAction.set('');
    this.service.supprimerPromotion(promotion.id).subscribe({
      next: () => {
        this.promotions.update((liste) => liste.filter((p) => p.id !== promotion.id));
        this.envoi.set(false);
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.envoi.set(false);
      },
    });
  }

  // ----- Matières ----------------------------------------------------

  nouvelleMatiere(): void {
    this.promotionEnEdition.set(null);
    this.matiereEnEdition.set({ id: null, code: '', libelle: '', credits: null, active: true });
  }

  editerMatiere(matiere: Matiere): void {
    this.promotionEnEdition.set(null);
    this.matiereEnEdition.set({
      id: matiere.id,
      code: matiere.code,
      libelle: matiere.libelle,
      credits: matiere.credits,
      active: matiere.active,
    });
  }

  annulerMatiere(): void {
    this.matiereEnEdition.set(null);
  }

  majMatiere(champ: keyof BrouillonMatiere, valeur: unknown): void {
    this.matiereEnEdition.update((f) => (f ? { ...f, [champ]: valeur } : f));
  }

  enregistrerMatiere(): void {
    const f = this.matiereEnEdition();
    if (!f || !this.matiereValide() || this.envoi()) return;

    const requete: MatiereRequest = {
      code: f.code.trim().toUpperCase(),
      libelle: f.libelle.trim(),
      credits: f.credits === null || f.credits === undefined ? null : Number(f.credits),
      active: f.active,
    };

    this.envoi.set(true);
    this.erreurAction.set('');
    const appel = f.id
      ? this.service.modifierMatiere(f.id, requete)
      : this.service.creerMatiere(requete);

    appel.subscribe({
      next: () => {
        this.envoi.set(false);
        if (f.id) this.matiereEnEdition.set(null);
        else this.nouvelleMatiere();
        this.charger();
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.envoi.set(false);
      },
    });
  }

  supprimerMatiere(matiere: Matiere): void {
    if (this.envoi()) return;
    // Le backend refuse la suppression d'une matière utilisée : son message nomme
    // le nombre de séances concernées, ce que l'interface ne saurait pas dire.
    this.envoi.set(true);
    this.erreurAction.set('');
    this.service.supprimerMatiere(matiere.id).subscribe({
      next: () => {
        this.matieres.update((liste) => liste.filter((m) => m.id !== matiere.id));
        this.envoi.set(false);
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.envoi.set(false);
      },
    });
  }
}
