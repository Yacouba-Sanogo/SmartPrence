import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import { Classe, ClasseRequest, Filiere, Promotion } from '../../core/models/classe.model';
import { Personnel, nomComplet } from '../../core/models/personnel.model';
import { ClasseService } from '../../core/services/classe.service';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';

/**
 * Création ou modification d'une classe.
 *
 * <p>Le rattachement des enseignants est intégré au formulaire plutôt que rejeté dans un
 * écran séparé : c'est lui qui ouvre l'espace mobile de l'enseignant, et l'oublier
 * revient à créer une classe que personne ne peut faire.</p>
 */
@Component({
  selector: 'sp-classe-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      [titre]="modeEdition() ? 'Modifier la classe' : 'Créer une classe'"
      sousTitre="Le rattachement des enseignants ouvre leur espace mobile"
      icone="classes"
      (fermer)="fermer.emit()"
    >
      <form (ngSubmit)="enregistrer()">
        <div class="flex max-h-[62vh] flex-col gap-4 overflow-y-auto px-5 py-5">
          @if (erreur()) {
            <p
              class="m-0 flex items-start gap-2.5 rounded-[10px] border border-danger-line bg-danger-bg px-3.5 py-3 text-[12.5px] leading-relaxed text-danger"
              role="alert"
            >
              <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" class="mt-px" />
              {{ erreur() }}
            </p>
          }

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Code</span>
              <span class="champ !h-11">
                <input
                  type="text"
                  name="code"
                  class="saisie num uppercase"
                  placeholder="L3-INFO"
                  required
                  [ngModel]="code()"
                  (ngModelChange)="code.set($event)"
                />
              </span>
              <span class="text-[11px] text-ink-faint">Identifiant court, unique.</span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Libellé</span>
              <span class="champ !h-11">
                <input
                  type="text"
                  name="libelle"
                  class="saisie"
                  placeholder="Licence 3 Informatique"
                  required
                  [ngModel]="libelle()"
                  (ngModelChange)="libelle.set($event)"
                />
              </span>
            </label>
          </div>

          <label class="flex flex-col gap-1.5">
            <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Promotion</span>
            <span class="champ !h-11">
              <select
                name="promotion"
                class="saisie bg-transparent"
                required
                [ngModel]="promotionId()"
                (ngModelChange)="promotionId.set($event)"
              >
                <option [ngValue]="null" disabled>Choisir une promotion…</option>
                @for (promotion of promotions(); track promotion.id) {
                  <option [ngValue]="promotion.id">
                    {{ promotion.libelle }}
                    @if (promotion.anneeAcademique) {
                      — {{ promotion.anneeAcademique }}
                    }
                  </option>
                }
              </select>
            </span>
            @if (promotions().length === 0) {
              <span class="text-[11px] text-alerte">
                Aucune promotion enregistrée : créez-en une avant de créer des classes.
              </span>
            }
          </label>

          @if (filieres().length > 0) {
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <label class="flex flex-col gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Filière</span>
                <span class="champ !h-11">
                  <select
                    name="filiere"
                    class="saisie bg-transparent"
                    [ngModel]="filiereId()"
                    (ngModelChange)="changerFiliere($event)"
                  >
                    <option [ngValue]="null">Aucune</option>
                    @for (filiere of filieres(); track filiere.id) {
                      <option [ngValue]="filiere.id">{{ filiere.libelle }}</option>
                    }
                  </select>
                </span>
              </label>

              <label class="flex flex-col gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
                  Option (3<sup>e</sup> année et plus)
                </span>
                <span class="champ !h-11">
                  <select
                    name="option"
                    class="saisie bg-transparent"
                    [disabled]="optionsDeLaFiliere().length === 0"
                    [ngModel]="optionId()"
                    (ngModelChange)="optionId.set($event)"
                  >
                    <option [ngValue]="null">Aucune</option>
                    @for (option of optionsDeLaFiliere(); track option.id) {
                      <option [ngValue]="option.id">{{ option.libelle }}</option>
                    }
                  </select>
                </span>
              </label>
            </div>
          }

          <label class="flex flex-col gap-1.5">
            <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
              Classe précédente
            </span>
            <span class="champ !h-11">
              <select
                name="classePrecedente"
                class="saisie bg-transparent"
                [ngModel]="classePrecedenteId()"
                (ngModelChange)="classePrecedenteId.set($event)"
              >
                <option [ngValue]="null">Aucune (première année)</option>
                @for (autre of classesPossibles(); track autre.id) {
                  <option [ngValue]="autre.id">{{ autre.code }} — {{ autre.libelle }}</option>
                }
              </select>
            </span>
            <span class="text-[11px] text-ink-faint">
              Les étudiants admis de cette classe passent automatiquement dans celle-ci. Quand
              plusieurs classes partagent la même classe précédente (une par option), chacun
              rejoint celle de l'option qu'il a choisie.
            </span>
          </label>

          <div class="flex flex-col gap-2">
            <div class="flex items-baseline justify-between gap-3">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
                Enseignants rattachés
              </span>
              <span class="text-[11px] text-ink-faint">
                {{ enseignantIds().length }} sélectionné(s)
              </span>
            </div>

            @if (enseignants().length === 0) {
              <p
                class="m-0 rounded-[10px] border border-line bg-canvas px-3.5 py-3 text-[12.5px] leading-relaxed text-ink-subtle"
              >
                Aucun agent de catégorie « Enseignant » dans le référentiel. Ajoutez-en depuis
                l'écran Personnel : sans enseignant rattaché, personne ne verra cette classe sur
                l'application mobile.
              </p>
            } @else {
              <label class="champ bg-[#FBFCFE]">
                <sp-icone nom="recherche" [taille]="16" [epaisseur]="1.7" class="text-ink-faint" />
                <input
                  type="search"
                  name="rechercheEnseignant"
                  class="saisie"
                  placeholder="Filtrer par nom ou matricule…"
                  [ngModel]="recherche()"
                  (ngModelChange)="recherche.set($event)"
                />
              </label>

              <div class="max-h-[190px] overflow-y-auto rounded-[10px] border border-line">
                @for (agent of enseignantsFiltres(); track agent.id) {
                  <button
                    type="button"
                    class="flex w-full items-center gap-3 border-b border-line-soft px-3.5 py-2.5 text-left last:border-b-0 hover:bg-canvas"
                    (click)="basculer(agent.id)"
                  >
                    <span
                      class="grid size-[18px] shrink-0 place-items-center rounded-[5px] border transition-colors"
                      [class]="
                        estSelectionne(agent.id)
                          ? 'border-royal-600 bg-royal-600 text-white'
                          : 'border-line bg-white text-transparent'
                      "
                    >
                      <sp-icone nom="coche" [taille]="12" [epaisseur]="2.2" />
                    </span>
                    <span class="flex min-w-0 flex-1 flex-col gap-0.5">
                      <span class="truncate text-[13px] font-medium text-ink">
                        {{ nomAffiche(agent) }}
                      </span>
                      <span class="num truncate text-[11px] text-ink-faint">{{ agent.matricule }}</span>
                    </span>
                    @if (!agent.actif) {
                      <span class="badge shrink-0 bg-line-faint text-ink-subtle">Inactif</span>
                    }
                  </button>
                } @empty {
                  <p class="m-0 px-3.5 py-4 text-center text-[12.5px] text-ink-faint">
                    Aucun enseignant ne correspond.
                  </p>
                }
              </div>
            }
          </div>
        </div>

        <footer
          class="flex items-center justify-end gap-2.5 border-t border-line-soft bg-[#FCFDFE] px-5 py-3.5"
        >
          <button type="button" class="btn btn-secondaire" (click)="fermer.emit()" [disabled]="envoi()">
            Annuler
          </button>
          <button type="submit" class="btn btn-primaire" [disabled]="!formulaireValide() || envoi()">
            @if (envoi()) {
              Enregistrement…
            } @else {
              {{ modeEdition() ? 'Enregistrer' : 'Créer la classe' }}
            }
          </button>
        </footer>
      </form>
    </sp-modale>
  `,
})
export class ClasseDialogComponent implements OnInit {
  private readonly service = inject(ClasseService);

  /** Classe à modifier, `null` pour une création. */
  readonly classe = input<Classe | null>(null);
  readonly promotions = input.required<Promotion[]>();
  readonly enseignants = input.required<Personnel[]>();
  readonly filieres = input<Filiere[]>([]);
  /** Toutes les classes, pour choisir la classe précédente. */
  readonly classes = input<Classe[]>([]);

  /** Rattachements actuels, chargés par la liste avant l'ouverture. */
  readonly enseignantsActuels = input<string[]>([]);

  readonly fermer = output<void>();
  readonly enregistre = output<void>();

  readonly code = signal('');
  readonly libelle = signal('');
  readonly promotionId = signal<number | null>(null);
  readonly enseignantIds = signal<string[]>([]);
  readonly filiereId = signal<number | null>(null);
  readonly optionId = signal<number | null>(null);
  readonly classePrecedenteId = signal<number | null>(null);

  readonly optionsDeLaFiliere = computed(
    () => this.filieres().find((f) => f.id === this.filiereId())?.options ?? [],
  );

  /** Toute classe sauf elle-même : le serveur refuse en plus les boucles. */
  readonly classesPossibles = computed(() =>
    this.classes().filter((autre) => autre.id !== this.classe()?.id),
  );
  readonly recherche = signal('');
  readonly envoi = signal(false);
  readonly erreur = signal('');

  readonly modeEdition = computed(() => this.classe() !== null);

  readonly enseignantsFiltres = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    const liste = [...this.enseignants()].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
    if (!terme) return liste;
    return liste.filter((agent) =>
      `${agent.prenom} ${agent.nom} ${agent.matricule}`.toLowerCase().includes(terme),
    );
  });

  readonly formulaireValide = computed(
    () =>
      this.code().trim().length > 0 &&
      this.libelle().trim().length > 0 &&
      this.promotionId() !== null,
  );

  ngOnInit(): void {
    const classe = this.classe();
    if (classe) {
      this.code.set(classe.code);
      this.libelle.set(classe.libelle);
      this.promotionId.set(classe.promotion?.id ?? null);
      this.enseignantIds.set([...this.enseignantsActuels()]);
      this.filiereId.set(classe.filiereId ?? null);
      this.optionId.set(classe.optionId ?? null);
      this.classePrecedenteId.set(classe.classePrecedenteId ?? null);
    }
  }

  /** Changer de filière efface l'option : elle n'appartiendrait plus à la filière. */
  changerFiliere(id: number | null): void {
    this.filiereId.set(id);
    this.optionId.set(null);
  }

  nomAffiche(agent: Personnel): string {
    return nomComplet(agent);
  }

  estSelectionne(id: string): boolean {
    return this.enseignantIds().includes(id);
  }

  basculer(id: string): void {
    this.enseignantIds.update((ids) =>
      ids.includes(id) ? ids.filter((autre) => autre !== id) : [...ids, id],
    );
  }

  enregistrer(): void {
    if (!this.formulaireValide() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');

    // La liste est toujours transmise en entier : le backend remplace l'ensemble
    // des rattachements, l'omettre détacherait tous les enseignants.
    const requete: ClasseRequest = {
      code: this.code().trim().toUpperCase(),
      libelle: this.libelle().trim(),
      promotionId: this.promotionId() as number,
      enseignantIds: this.enseignantIds(),
      filiereId: this.filiereId(),
      optionId: this.optionId(),
      classePrecedenteId: this.classePrecedenteId(),
    };

    const classe = this.classe();
    const appel = classe
      ? this.service.modifier(classe.id, requete)
      : this.service.creer(requete);

    appel.subscribe({
      next: () => {
        this.envoi.set(false);
        this.enregistre.emit();
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.envoi.set(false);
      },
    });
  }
}
