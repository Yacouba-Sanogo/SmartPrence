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
import { Salle } from '../../core/models/appareil.model';
import { SalleService } from '../../core/services/salle.service';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';

/** Création ou modification d'une salle. */
@Component({
  selector: 'sp-salle-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      [titre]="modeEdition() ? 'Modifier la salle' : 'Ajouter une salle'"
      sousTitre="Une salle peut ensuite accueillir un lecteur biométrique"
      icone="salles"
      (fermer)="fermer.emit()"
    >
      <form (ngSubmit)="enregistrer()">
        <div class="flex flex-col gap-4 px-5 py-5">
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
                  placeholder="B12-101"
                  required
                  [ngModel]="code()"
                  (ngModelChange)="code.set($event)"
                />
              </span>
              <span class="text-[11px] text-ink-faint">Identifiant unique, affiché sur la porte.</span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Nom</span>
              <span class="champ !h-11">
                <input
                  type="text"
                  name="nom"
                  class="saisie"
                  placeholder="Amphithéâtre Nord"
                  required
                  [ngModel]="nom()"
                  (ngModelChange)="nom.set($event)"
                />
              </span>
            </label>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1.5">
              <span class="flex items-baseline gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Bâtiment</span>
                <span class="text-[11px] text-ink-faint">facultatif</span>
              </span>
              <span class="champ !h-11">
                <input
                  type="text"
                  name="batiment"
                  class="saisie"
                  placeholder="Bâtiment B"
                  [ngModel]="batiment()"
                  (ngModelChange)="batiment.set($event)"
                />
              </span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="flex items-baseline gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Capacité</span>
                <span class="text-[11px] text-ink-faint">facultatif</span>
              </span>
              <span class="champ !h-11">
                <input
                  type="number"
                  name="capacite"
                  class="saisie num"
                  min="1"
                  placeholder="80"
                  [ngModel]="capacite()"
                  (ngModelChange)="capacite.set($event)"
                />
                <span class="text-[11.5px] text-ink-faint">places</span>
              </span>
            </label>
          </div>
        </div>

        <footer class="flex items-center gap-2.5 border-t border-line-soft bg-[#FCFDFE] px-5 py-4">
          <span class="flex-1"></span>
          <button type="button" class="btn btn-secondaire" (click)="fermer.emit()">Annuler</button>
          <button type="submit" class="btn btn-primaire" [disabled]="!valide() || envoi()">
            <sp-icone nom="coche" [taille]="15" [epaisseur]="2.1" />
            {{ envoi() ? 'Enregistrement…' : 'Enregistrer' }}
          </button>
        </footer>
      </form>
    </sp-modale>
  `,
})
export class SalleDialogComponent implements OnInit {
  private readonly service = inject(SalleService);

  readonly salle = input<Salle | null>(null);

  readonly fermer = output<void>();
  readonly enregistre = output<void>();

  readonly code = signal('');
  readonly nom = signal('');
  readonly batiment = signal('');
  readonly capacite = signal<number | null>(null);
  readonly envoi = signal(false);
  readonly erreur = signal('');

  readonly modeEdition = computed(() => this.salle() !== null);
  readonly valide = computed(() => this.code().trim().length > 0 && this.nom().trim().length > 0);

  ngOnInit(): void {
    const existante = this.salle();
    if (!existante) return;
    this.code.set(existante.code);
    this.nom.set(existante.nom);
    this.batiment.set(existante.batiment ?? '');
    this.capacite.set(existante.capacite);
  }

  enregistrer(): void {
    if (!this.valide() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');

    const requete = {
      code: this.code().trim().toUpperCase(),
      nom: this.nom().trim(),
      batiment: this.batiment().trim() || null,
      capacite: this.capacite(),
    };

    const existante = this.salle();
    const appel = existante
      ? this.service.modifier(existante.id, requete)
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
