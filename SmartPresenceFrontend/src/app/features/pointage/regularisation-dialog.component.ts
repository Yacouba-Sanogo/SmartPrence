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
import { Personnel, nomComplet } from '../../core/models/personnel.model';
import { SensPointage } from '../../core/models/pointage.model';
import { PersonnelService } from '../../core/services/personnel.service';
import { PointageService } from '../../core/services/pointage.service';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';

/**
 * Régularisation administrative d'un pointage.
 *
 * Couvre les cas où le lecteur n'a pas pu opérer : panne du capteur, doigt non
 * reconnu, agent en mission. Le motif est exigé — c'est lui qui rend la correction
 * auditable et la distingue d'une altération du relevé.
 */
@Component({
  selector: 'sp-regularisation-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      titre="Régulariser un pointage"
      sousTitre="Saisie administrative en cas de panne du lecteur ou de non-reconnaissance"
      icone="crayon"
      (fermer)="fermer.emit()"
    >
      <form (ngSubmit)="enregistrer()">
        <div class="flex flex-col gap-4 px-5 py-5">
          @if (erreur()) {
            <p
              class="m-0 flex items-start gap-2.5 rounded-[10px] border border-danger-line bg-danger-bg px-3.5 py-3 text-[12.5px] leading-relaxed text-danger"
            >
              <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" class="mt-px" />
              {{ erreur() }}
            </p>
          }

          <label class="flex flex-col gap-1.5">
            <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Agent</span>
            <select
              class="champ !h-11 text-ink"
              [ngModel]="personnelId()"
              (ngModelChange)="personnelId.set($event)"
              name="personnelId"
              required
            >
              <option [ngValue]="''" disabled>Choisir un agent…</option>
              @for (agent of agents(); track agent.id) {
                <option [ngValue]="agent.id">{{ nomComplet(agent) }} — {{ agent.matricule }}</option>
              }
            </select>
          </label>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Date</span>
              <span class="champ !h-11">
                <sp-icone nom="calendrier" [taille]="16" class="text-ink-muted" />
                <input
                  type="date"
                  class="saisie num"
                  [ngModel]="datePointage()"
                  (ngModelChange)="datePointage.set($event)"
                  name="datePointage"
                  [max]="aujourdhui"
                  required
                />
              </span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Heure</span>
              <span class="champ !h-11">
                <sp-icone nom="horloge" [taille]="16" class="text-ink-muted" />
                <input
                  type="time"
                  class="saisie num"
                  [ngModel]="heurePointage()"
                  (ngModelChange)="heurePointage.set($event)"
                  name="heurePointage"
                  required
                />
              </span>
            </label>
          </div>

          <fieldset class="m-0 flex flex-col gap-1.5 border-0 p-0">
            <legend class="mb-1.5 p-0 text-[11.5px] font-semibold tracking-wide text-ink-muted">
              Sens du pointage
            </legend>
            <div class="grid grid-cols-2 gap-2.5">
              @for (option of optionsSens; track option.valeur) {
                <button
                  type="button"
                  (click)="sens.set(option.valeur)"
                  [attr.aria-pressed]="sens() === option.valeur"
                  class="flex h-[46px] items-center gap-2.5 rounded-[9px] border px-3.5 text-left transition-colors"
                  [class]="
                    sens() === option.valeur
                      ? 'border-[1.5px] border-royal-600 bg-[#F5F8FF]'
                      : 'border-line bg-white hover:bg-canvas'
                  "
                >
                  <span
                    class="grid size-[26px] shrink-0 place-items-center rounded-[7px]"
                    [class]="
                      option.valeur === 'ENTREE'
                        ? 'bg-succes-bg text-succes'
                        : 'bg-[#EFF2F8] text-[#4A5470]'
                    "
                  >
                    <sp-icone [nom]="option.icone" [taille]="15" [epaisseur]="1.8" />
                  </span>
                  <span
                    class="flex-1 text-[13.5px]"
                    [class]="sens() === option.valeur ? 'font-semibold text-ink' : 'text-ink-muted'"
                  >
                    {{ option.libelle }}
                  </span>
                  @if (sens() === option.valeur) {
                    <span class="grid size-[18px] shrink-0 place-items-center rounded-full bg-royal-600 text-white">
                      <sp-icone nom="coche" [taille]="11" [epaisseur]="3" />
                    </span>
                  } @else {
                    <span class="size-[18px] shrink-0 rounded-full border-[1.5px] border-[#D3DAE8]"></span>
                  }
                </button>
              }
            </div>
          </fieldset>

          <label class="flex flex-col gap-1.5">
            <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
              Motif de la régularisation
            </span>
            <textarea
              rows="3"
              maxlength="500"
              required
              name="motif"
              [ngModel]="motif()"
              (ngModelChange)="motif.set($event)"
              placeholder="Ex. : capteur du hall A hors service entre 07h50 et 08h20 — arrivée confirmée par le registre de la sécurité."
              class="w-full resize-y rounded-[9px] border border-line bg-white px-3.5 py-3 text-[13px] leading-relaxed text-ink outline-0 placeholder:text-ink-faint focus:border-royal-400"
            ></textarea>
            <span class="num self-end text-[11px] text-ink-faint">{{ motif().length }} / 500</span>
          </label>

          <p
            class="m-0 flex items-start gap-3 rounded-[11px] border border-royal-200 bg-[#F5F8FF] px-4 py-3.5 text-xs leading-relaxed text-[#33447A]"
          >
            <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-royal-100 text-royal-600">
              <sp-icone nom="info" [taille]="17" [epaisseur]="1.7" />
            </span>
            <span>
              Ce pointage sera enregistré avec la source
              <span class="font-semibold text-royal-600">MANUEL</span> et restera distinguable d'une
              identification biométrique dans l'historique et les exports.
            </span>
          </p>
        </div>

        <footer class="flex items-center gap-2.5 border-t border-line-soft bg-[#FCFDFE] px-5 py-4">
          <span class="flex-1"></span>
          <button type="button" class="btn btn-secondaire" (click)="fermer.emit()">Annuler</button>
          <button type="submit" class="btn btn-primaire" [disabled]="!formulaireValide() || envoi()">
            <sp-icone nom="coche" [taille]="15" [epaisseur]="2.1" />
            {{ envoi() ? 'Enregistrement…' : 'Enregistrer le pointage' }}
          </button>
        </footer>
      </form>
    </sp-modale>
  `,
})
export class RegularisationDialogComponent implements OnInit {
  private readonly personnelService = inject(PersonnelService);
  private readonly pointageService = inject(PointageService);

  protected readonly nomComplet = nomComplet;
  protected readonly aujourdhui = new Date().toISOString().slice(0, 10);

  protected readonly optionsSens = [
    { valeur: 'ENTREE' as SensPointage, libelle: 'Entrée', icone: 'entree' as const },
    { valeur: 'SORTIE' as SensPointage, libelle: 'Sortie', icone: 'sortie' as const },
  ];

  /** Date pré-sélectionnée, héritée de la journée consultée. */
  readonly date = input.required<string>();

  readonly fermer = output<void>();
  readonly enregistre = output<void>();

  readonly agents = signal<Personnel[]>([]);
  readonly personnelId = signal('');
  readonly datePointage = signal('');
  readonly heurePointage = signal('');
  readonly sens = signal<SensPointage>('ENTREE');
  readonly motif = signal('');
  readonly envoi = signal(false);
  readonly erreur = signal('');

  readonly formulaireValide = computed(
    () =>
      this.personnelId() !== '' &&
      this.datePointage() !== '' &&
      this.heurePointage() !== '' &&
      this.motif().trim().length > 0,
  );

  constructor() {
    // Seuls les agents actifs peuvent recevoir un pointage : le backend rejette les autres.
    this.personnelService.lister({ actifsSeul: true }).subscribe({
      next: (agents) => this.agents.set(agents ?? []),
      error: (erreur: unknown) => this.erreur.set(messageErreur(erreur)),
    });
  }

  /** La date consultée sert de valeur initiale ; elle n'est lisible qu'après l'init. */
  ngOnInit(): void {
    this.datePointage.set(this.date());
  }

  enregistrer(): void {
    if (!this.formulaireValide() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');

    this.pointageService
      .regulariser({
        personnelId: this.personnelId(),
        datePointage: this.datePointage(),
        // Le backend attend HH:mm:ss ; un champ <input type="time"> ne fournit que HH:mm.
        heurePointage: `${this.heurePointage()}:00`,
        sens: this.sens(),
        motif: this.motif().trim(),
      })
      .subscribe({
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
