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
import { Matiere, Seance, SeanceRequest, versInstant } from '../../core/models/academique.model';
import { Classe } from '../../core/models/classe.model';
import { Personnel, nomComplet } from '../../core/models/personnel.model';
import { Salle } from '../../core/models/appareil.model';
import { AcademiqueService } from '../../core/services/academique.service';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';

/**
 * Planification d'une séance.
 *
 * <p>Le créneau se saisit en date + heure locale, jamais en UTC : l'utilisateur pense
 * « cours de 8 h », pas « 06:00Z ». La conversion est faite au dernier moment.</p>
 */
@Component({
  selector: 'sp-seance-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      [titre]="modeEdition() ? 'Modifier la séance' : 'Planifier une séance'"
      sousTitre="Le créneau est refusé s'il est déjà occupé"
      icone="calendrier"
      (fermer)="fermer.emit()"
    >
      <form (ngSubmit)="enregistrer()">
        <div class="flex max-h-[62vh] flex-col gap-4 overflow-y-auto px-5 py-5">
          @if (erreur()) {
            <p
              class="m-0 flex items-start gap-2.5 rounded-[10px] border border-danger-line bg-danger-bg px-3.5 py-3 text-[12.5px] leading-relaxed text-danger"
              role="alert"
            >
              <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" class="mt-px shrink-0" />
              {{ erreur() }}
            </p>
          }

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Classe</span>
              <span class="champ !h-11">
                <select
                  name="classe"
                  class="saisie bg-transparent"
                  [ngModel]="classeId()"
                  (ngModelChange)="classeId.set($event)"
                >
                  <option [ngValue]="null" disabled>Choisir…</option>
                  @for (classe of classes(); track classe.id) {
                    <option [ngValue]="classe.id">{{ classe.code }} — {{ classe.libelle }}</option>
                  }
                </select>
              </span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Matière</span>
              <span class="champ !h-11">
                <select
                  name="matiere"
                  class="saisie bg-transparent"
                  [ngModel]="matiereId()"
                  (ngModelChange)="matiereId.set($event)"
                >
                  <option [ngValue]="null" disabled>Choisir…</option>
                  @for (matiere of matieresActives(); track matiere.id) {
                    <option [ngValue]="matiere.id">{{ matiere.code }} — {{ matiere.libelle }}</option>
                  }
                </select>
              </span>
            </label>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Enseignant</span>
              <span class="champ !h-11">
                <select
                  name="enseignant"
                  class="saisie bg-transparent"
                  [ngModel]="enseignantId()"
                  (ngModelChange)="enseignantId.set($event)"
                >
                  <option [ngValue]="null" disabled>Choisir…</option>
                  @for (agent of enseignantsActifs(); track agent.id) {
                    <option [ngValue]="agent.id">{{ nomDe(agent) }} — {{ agent.matricule }}</option>
                  }
                </select>
              </span>
              @if (enseignantsActifs().length === 0) {
                <span class="text-[11px] text-alerte">
                  Aucun enseignant actif. Créez-en un depuis l'écran Personnel.
                </span>
              }
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="flex items-baseline gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Salle</span>
                <span class="text-[11px] text-ink-faint">facultatif</span>
              </span>
              <span class="champ !h-11">
                <select
                  name="salle"
                  class="saisie bg-transparent"
                  [ngModel]="salleId()"
                  (ngModelChange)="salleId.set($event)"
                >
                  <option [ngValue]="null">Aucune salle</option>
                  @for (salle of salles(); track salle.id) {
                    <option [ngValue]="salle.id">{{ salle.code }} — {{ salle.nom }}</option>
                  }
                </select>
              </span>
              <span class="text-[11px] text-ink-faint">
                Sans salle, aucun lecteur ne relèvera cette séance.
              </span>
            </label>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Date</span>
              <span class="champ !h-11">
                <input
                  type="date"
                  name="date"
                  class="saisie num"
                  [ngModel]="date()"
                  (ngModelChange)="date.set($event)"
                />
              </span>
            </label>
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Début</span>
              <span class="champ !h-11">
                <input
                  type="time"
                  name="heureDebut"
                  class="saisie num"
                  [ngModel]="heureDebut()"
                  (ngModelChange)="heureDebut.set($event)"
                />
              </span>
            </label>
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Fin</span>
              <span class="champ !h-11">
                <input
                  type="time"
                  name="heureFin"
                  class="saisie num"
                  [ngModel]="heureFin()"
                  (ngModelChange)="heureFin.set($event)"
                />
              </span>
            </label>
          </div>

          @if (creneauInvalide()) {
            <p class="m-0 text-[11.5px] text-alerte">
              La fin doit être postérieure au début.
            </p>
          }

          <label class="flex flex-col gap-1.5">
            <span class="flex items-baseline gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Note</span>
              <span class="text-[11px] text-ink-faint">facultatif</span>
            </span>
            <textarea
              name="note"
              rows="2"
              class="w-full resize-none rounded-[10px] border border-line bg-white px-3.5 py-2.5 text-[13px] text-ink outline-none focus:border-royal-400"
              placeholder="Salle changée, contrôle continu…"
              [ngModel]="note()"
              (ngModelChange)="note.set($event)"
            ></textarea>
          </label>
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
              {{ modeEdition() ? 'Enregistrer' : 'Planifier' }}
            }
          </button>
        </footer>
      </form>
    </sp-modale>
  `,
})
export class SeanceDialogComponent implements OnInit {
  private readonly service = inject(AcademiqueService);

  readonly seance = input<Seance | null>(null);
  readonly classes = input.required<Classe[]>();
  readonly matieres = input.required<Matiere[]>();
  readonly enseignants = input.required<Personnel[]>();
  readonly salles = input.required<Salle[]>();
  /** Jour pré-sélectionné à la création, repris de l'écran appelant. */
  readonly jourParDefaut = input<string>('');

  readonly fermer = output<void>();
  readonly enregistre = output<void>();

  readonly classeId = signal<number | null>(null);
  readonly matiereId = signal<number | null>(null);
  readonly enseignantId = signal<string | null>(null);
  readonly salleId = signal<number | null>(null);
  readonly date = signal('');
  readonly heureDebut = signal('08:00');
  readonly heureFin = signal('10:00');
  readonly note = signal('');
  readonly envoi = signal(false);
  readonly erreur = signal('');

  readonly modeEdition = computed(() => this.seance() !== null);

  /** Une matière désactivée ne doit plus apparaître dans un nouveau planning. */
  readonly matieresActives = computed(() =>
    this.matieres().filter((m) => m.active || m.id === this.matiereId()),
  );

  readonly enseignantsActifs = computed(() =>
    this.enseignants()
      .filter((a) => a.actif || a.id === this.enseignantId())
      .sort((a, b) => a.nom.localeCompare(b.nom, 'fr')),
  );

  readonly creneauInvalide = computed(
    () => !!this.heureDebut() && !!this.heureFin() && this.heureFin() <= this.heureDebut(),
  );

  readonly formulaireValide = computed(
    () =>
      this.classeId() !== null &&
      this.matiereId() !== null &&
      this.enseignantId() !== null &&
      !!this.date() &&
      !!this.heureDebut() &&
      !!this.heureFin() &&
      !this.creneauInvalide(),
  );

  ngOnInit(): void {
    const seance = this.seance();
    if (seance) {
      const debut = new Date(seance.debut);
      const fin = new Date(seance.fin);
      this.classeId.set(seance.classeId);
      this.matiereId.set(seance.matiereId);
      this.enseignantId.set(seance.enseignantId);
      this.salleId.set(seance.salleId);
      this.date.set(this.dateLocale(debut));
      this.heureDebut.set(this.heureLocale(debut));
      this.heureFin.set(this.heureLocale(fin));
      this.note.set(seance.note ?? '');
    } else {
      this.date.set(this.jourParDefaut() || this.dateLocale(new Date()));
    }
  }

  nomDe(agent: Personnel): string {
    return nomComplet(agent);
  }

  enregistrer(): void {
    if (!this.formulaireValide() || this.envoi()) return;

    const requete: SeanceRequest = {
      classeId: this.classeId() as number,
      matiereId: this.matiereId() as number,
      enseignantId: this.enseignantId() as string,
      salleId: this.salleId(),
      debut: versInstant(this.date(), this.heureDebut()),
      fin: versInstant(this.date(), this.heureFin()),
      note: this.note().trim() || null,
    };

    this.envoi.set(true);
    this.erreur.set('');

    const seance = this.seance();
    const appel = seance
      ? this.service.modifierSeance(seance.id, requete)
      : this.service.creerSeance(requete);

    appel.subscribe({
      next: () => {
        this.envoi.set(false);
        this.enregistre.emit();
      },
      error: (erreur: unknown) => {
        // Le backend nomme le cours qui occupe déjà le créneau : son message est
        // plus utile que tout ce que l'interface pourrait deviner.
        this.erreur.set(messageErreur(erreur));
        this.envoi.set(false);
      },
    });
  }

  /** `YYYY-MM-DD` en heure locale — `toISOString()` décalerait d'un jour le soir. */
  private dateLocale(d: Date): string {
    return [
      d.getFullYear(),
      String(d.getMonth() + 1).padStart(2, '0'),
      String(d.getDate()).padStart(2, '0'),
    ].join('-');
  }

  private heureLocale(d: Date): string {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }
}
