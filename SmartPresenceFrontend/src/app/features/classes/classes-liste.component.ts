import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of } from 'rxjs';
import { messageErreur } from '../../core/api';
import { Classe, Promotion, libellePromotion } from '../../core/models/classe.model';
import { Personnel } from '../../core/models/personnel.model';
import { ClasseService } from '../../core/services/classe.service';
import { PersonnelService } from '../../core/services/personnel.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ClasseDetailComponent } from './classe-detail.component';
import { ClasseDialogComponent } from './classe-dialog.component';

/**
 * Référentiel des classes.
 *
 * <p>La colonne « Enseignants » n'est pas décorative : une classe sans enseignant
 * rattaché n'apparaît sur l'application mobile de personne, et l'anomalie ne se voit
 * nulle part ailleurs.</p>
 */
@Component({
  selector: 'sp-classes-liste',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    IconeComponent,
    EtatComponent,
    ClasseDialogComponent,
    ClasseDetailComponent,
  ],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Classes</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Composition des classes et rattachement de l'équipe pédagogique.
          </p>
        </div>
        <div class="flex shrink-0 items-center gap-2.5">
          <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
            <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
            Actualiser
          </button>
          <button type="button" class="btn btn-primaire" (click)="ouvrirCreation()">
            <sp-icone nom="plus" [taille]="16" [epaisseur]="1.8" />
            Créer une classe
          </button>
        </div>
      </div>

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

      <div class="carte flex flex-wrap items-center gap-2.5 px-4 py-3">
        <label class="champ min-w-[210px] flex-1 bg-[#FBFCFE]">
          <sp-icone nom="recherche" [taille]="16" [epaisseur]="1.7" class="text-ink-faint" />
          <input
            type="search"
            class="saisie"
            placeholder="Rechercher un code, un libellé…"
            [ngModel]="recherche()"
            (ngModelChange)="recherche.set($event)"
            aria-label="Rechercher une classe"
          />
        </label>

        <select
          class="champ text-ink"
          [ngModel]="filtrePromotion()"
          (ngModelChange)="filtrePromotion.set($event)"
          aria-label="Filtrer par promotion"
        >
          <option [ngValue]="null">Toutes les promotions</option>
          @for (promotion of promotions(); track promotion.id) {
            <option [ngValue]="promotion.id">{{ promotion.libelle }}</option>
          }
        </select>

        <select
          class="champ text-ink"
          [ngModel]="filtreEncadrement()"
          (ngModelChange)="filtreEncadrement.set($event)"
          aria-label="Filtrer par encadrement"
        >
          <option value="TOUTES">Encadrement : toutes</option>
          <option value="ENCADREES">Avec enseignant</option>
          <option value="ORPHELINES">Sans enseignant</option>
        </select>
      </div>

      <section class="carte overflow-hidden">
        @if (chargement()) {
          <sp-etat titre="Chargement des classes…" icone="horloge" />
        } @else if (erreur()) {
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger les classes"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        } @else if (classes().length === 0) {
          <sp-etat
            icone="classes"
            titre="Aucune classe enregistrée"
            detail="Créez une classe pour y inscrire des étudiants et y rattacher des enseignants."
            actionLibelle="Créer une classe"
            (action)="ouvrirCreation()"
          />
        } @else if (classesFiltrees().length === 0) {
          <sp-etat
            icone="recherche"
            titre="Aucune classe ne correspond"
            detail="Ajustez la recherche ou les filtres pour élargir le résultat."
          />
        } @else {
          <div class="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th class="th !pl-[18px]">Classe</th>
                  <th class="th">Promotion</th>
                  <th class="th">Effectif</th>
                  <th class="th">Enseignants</th>
                  <th class="th !pr-[18px] text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (classe of classesFiltrees(); track classe.id) {
                  <tr class="hover:bg-[#FBFCFE]">
                    <td class="td !pl-[18px]">
                      <div class="flex items-center gap-3">
                        <span
                          class="grid size-8 shrink-0 place-items-center rounded-lg bg-line-faint text-[#4A5470]"
                        >
                          <sp-icone nom="classes" [taille]="16" [epaisseur]="1.7" />
                        </span>
                        <span class="flex min-w-0 flex-col gap-0.5">
                          <span class="truncate text-[13.5px] font-medium text-ink">
                            {{ classe.libelle }}
                          </span>
                          <span class="num text-[11px] uppercase text-ink-faint">{{ classe.code }}</span>
                        </span>
                      </div>
                    </td>
                    <td class="td text-[#364057]">{{ promotionDe(classe) }}</td>
                    <td class="td num text-[#364057]">
                      {{ classe.nombreEtudiants }} inscrit(s)
                    </td>
                    <td class="td">
                      @if (classe.nombreEnseignants > 0) {
                        <span class="badge bg-royal-50 text-royal-600">
                          <sp-icone nom="personnel" [taille]="12" [epaisseur]="1.9" />
                          {{ classe.nombreEnseignants }}
                        </span>
                      } @else {
                        <span class="badge bg-alerte-bg text-alerte" title="Invisible sur le mobile">
                          <sp-icone nom="alerte" [taille]="12" [epaisseur]="1.9" />
                          Aucun
                        </span>
                      }
                    </td>
                    <td class="td !pr-[18px]">
                      <div class="flex items-center justify-end gap-2">
                        <button
                          type="button"
                          class="grid size-[30px] place-items-center rounded-[7px] border border-line bg-white text-ink-muted transition-colors hover:bg-canvas"
                          (click)="ouvrirDetail(classe)"
                          aria-label="Voir la composition"
                        >
                          <sp-icone nom="etudiants" [taille]="15" [epaisseur]="1.7" />
                        </button>
                        <button
                          type="button"
                          class="grid size-[30px] place-items-center rounded-[7px] border border-line bg-white text-ink-muted transition-colors hover:bg-canvas disabled:opacity-50"
                          (click)="ouvrirModification(classe)"
                          [disabled]="preparationEdition() === classe.id"
                          aria-label="Modifier"
                        >
                          <sp-icone nom="crayon" [taille]="15" [epaisseur]="1.7" />
                        </button>
                        <button
                          type="button"
                          class="grid size-[30px] place-items-center rounded-[7px] border border-line bg-white text-ink-muted transition-colors hover:border-danger-line hover:bg-danger-bg hover:text-danger disabled:opacity-50"
                          (click)="supprimer(classe)"
                          [disabled]="suppressionEnCours() === classe.id"
                          aria-label="Supprimer"
                        >
                          <sp-icone nom="fermer" [taille]="15" [epaisseur]="1.9" />
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <footer class="border-t border-line-soft bg-[#FCFDFE] px-[18px] py-3 text-xs text-ink-subtle">
            {{ classesFiltrees().length }} classe(s) sur {{ classes().length }}
          </footer>
        }
      </section>
    </div>

    @if (dialogueOuvert()) {
      <sp-classe-dialog
        [classe]="classeEnEdition()"
        [promotions]="promotions()"
        [enseignants]="enseignants()"
        [enseignantsActuels]="enseignantsActuels()"
        (fermer)="fermerDialogue()"
        (enregistre)="apresEnregistrement()"
      />
    }

    @if (classeConsultee(); as classe) {
      <sp-classe-detail [classe]="classe" (fermer)="classeConsultee.set(null)" />
    }
  `,
})
export class ClassesListeComponent {
  private readonly service = inject(ClasseService);
  private readonly personnelService = inject(PersonnelService);

  readonly classes = signal<Classe[]>([]);
  readonly promotions = signal<Promotion[]>([]);
  readonly enseignants = signal<Personnel[]>([]);

  readonly chargement = signal(false);

  /** Échec de chargement : la liste ne peut rien afficher, l'écran bascule en erreur. */
  readonly erreur = signal('');

  /** Échec d'une action ponctuelle : la liste reste utilisable, un bandeau signale le refus. */
  readonly erreurAction = signal('');

  readonly dialogueOuvert = signal(false);
  readonly classeEnEdition = signal<Classe | null>(null);
  readonly enseignantsActuels = signal<string[]>([]);
  readonly classeConsultee = signal<Classe | null>(null);
  readonly suppressionEnCours = signal<number | null>(null);
  readonly preparationEdition = signal<number | null>(null);

  readonly recherche = signal('');
  readonly filtrePromotion = signal<number | null>(null);
  readonly filtreEncadrement = signal<'TOUTES' | 'ENCADREES' | 'ORPHELINES'>('TOUTES');

  readonly indicateurs = computed(() => {
    const liste = this.classes();
    const orphelines = liste.filter((c) => c.nombreEnseignants === 0).length;
    const effectif = liste.reduce((total, c) => total + c.nombreEtudiants, 0);
    return [
      {
        libelle: 'Classes',
        valeur: liste.length,
        detail: 'enregistrées',
        icone: 'classes' as const,
        teinte: 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Promotions',
        valeur: this.promotions().length,
        detail: 'cohortes',
        icone: 'calendrier' as const,
        teinte: 'bg-royal-50 text-royal-600',
      },
      {
        libelle: 'Effectif total',
        valeur: effectif,
        detail: 'inscrits',
        icone: 'etudiants' as const,
        teinte: 'bg-succes-bg text-succes',
      },
      {
        libelle: 'Sans enseignant',
        valeur: orphelines,
        detail: orphelines > 0 ? 'invisibles sur mobile' : 'toutes encadrées',
        icone: 'alerte' as const,
        teinte: orphelines > 0 ? 'bg-alerte-bg text-alerte' : 'bg-line-faint text-[#4A5470]',
      },
    ];
  });

  readonly classesFiltrees = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    const promotionId = this.filtrePromotion();
    const encadrement = this.filtreEncadrement();

    return this.classes().filter((classe) => {
      if (promotionId && classe.promotion?.id !== promotionId) return false;
      if (encadrement === 'ENCADREES' && classe.nombreEnseignants === 0) return false;
      if (encadrement === 'ORPHELINES' && classe.nombreEnseignants > 0) return false;
      if (!terme) return true;
      return `${classe.code} ${classe.libelle} ${classe.promotion?.libelle ?? ''}`
        .toLowerCase()
        .includes(terme);
    });
  });

  constructor() {
    this.charger();
  }

  promotionDe(classe: Classe): string {
    return libellePromotion(classe);
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');

    // Les référentiels annexes ne doivent pas faire échouer l'écran : sans promotions
    // la liste reste lisible, seul le formulaire de création s'en trouve limité.
    forkJoin({
      classes: this.service.lister(),
      promotions: this.service.listerPromotions().pipe(catchError(() => of([] as Promotion[]))),
      enseignants: this.personnelService
        .lister({ type: 'ENSEIGNANT' })
        .pipe(catchError(() => of([] as Personnel[]))),
    }).subscribe({
      next: ({ classes, promotions, enseignants }) => {
        this.classes.set(classes ?? []);
        this.promotions.set(promotions ?? []);
        this.enseignants.set(enseignants ?? []);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  ouvrirCreation(): void {
    this.classeEnEdition.set(null);
    this.enseignantsActuels.set([]);
    this.dialogueOuvert.set(true);
  }

  /**
   * Ouvre la modification après avoir récupéré les rattachements existants.
   *
   * La liste ne les porte pas — elle n'en connaît que le nombre. Ouvrir le formulaire
   * sans eux ferait apparaître une classe sans aucun enseignant coché, et le premier
   * enregistrement les détacherait tous.
   */
  ouvrirModification(classe: Classe): void {
    if (this.preparationEdition()) return;
    this.preparationEdition.set(classe.id);
    this.erreurAction.set('');

    this.service.detail(classe.id).subscribe({
      next: (detail) => {
        this.enseignantsActuels.set(detail.enseignants.map((agent) => agent.id));
        this.classeEnEdition.set(classe);
        this.dialogueOuvert.set(true);
        this.preparationEdition.set(null);
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.preparationEdition.set(null);
      },
    });
  }

  ouvrirDetail(classe: Classe): void {
    this.classeConsultee.set(classe);
  }

  fermerDialogue(): void {
    this.dialogueOuvert.set(false);
    this.classeEnEdition.set(null);
    this.enseignantsActuels.set([]);
  }

  apresEnregistrement(): void {
    this.fermerDialogue();
    this.charger();
  }

  supprimer(classe: Classe): void {
    if (this.suppressionEnCours()) return;
    // Le backend refuse la suppression d'une classe peuplée : son message est
    // plus précis que tout ce que l'interface pourrait deviner.
    this.suppressionEnCours.set(classe.id);
    this.erreurAction.set('');

    this.service.supprimer(classe.id).subscribe({
      next: () => {
        this.classes.update((liste) => liste.filter((c) => c.id !== classe.id));
        this.suppressionEnCours.set(null);
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.suppressionEnCours.set(null);
      },
    });
  }
}
