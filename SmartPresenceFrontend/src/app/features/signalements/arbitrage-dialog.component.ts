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
import { Signalement, libelleTypeSignalement } from '../../core/models/signalement.model';
import { SignalementService } from '../../core/services/signalement.service';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';

/**
 * Décision de la scolarité sur un signalement.
 *
 * <p>Le motif est <b>obligatoire dans les deux sens</b>, refus compris : c'est la seule
 * trace de ce qui a fondé la décision, et un enseignant dont le signalement est écarté
 * sans explication cesse d'en déposer.</p>
 */
@Component({
  selector: 'sp-arbitrage-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      [titre]="accepte() ? 'Retenir le signalement' : 'Écarter le signalement'"
      [sousTitre]="typeLisible()"
      [icone]="accepte() ? 'coche-cercle' : 'moins-cercle'"
      [variante]="accepte() ? 'succes' : 'royal'"
      (fermer)="fermer.emit()"
    >
      <form (ngSubmit)="valider()">
        <div class="flex flex-col gap-4 px-5 py-5">
          @if (erreur()) {
            <p
              class="m-0 flex items-start gap-2.5 rounded-[10px] border border-danger-line bg-danger-bg px-3.5 py-3 text-[12.5px] leading-relaxed text-danger"
              role="alert"
            >
              <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" class="mt-px shrink-0" />
              {{ erreur() }}
            </p>
          }

          <div class="flex flex-col gap-1.5 rounded-[10px] bg-canvas px-3.5 py-3">
            <span class="text-[11px] font-semibold tracking-wide text-ink-muted">
              Témoignage de {{ signalement().enseignantNom }}
            </span>
            <span class="text-[12.5px] leading-relaxed text-[#364057]">
              {{ signalement().description }}
            </span>
          </div>

          @if (accepte() && produitUneRegularisation()) {
            <div
              class="flex items-start gap-2.5 rounded-[10px] border border-royal-200 bg-royal-50 px-3.5 py-3"
            >
              <sp-icone
                nom="info"
                [taille]="16"
                [epaisseur]="1.7"
                class="mt-px shrink-0 text-royal-600"
              />
              <span class="text-[12.5px] leading-relaxed text-royal-800">
                Un relevé de présence sera créé pour
                <strong>{{ signalement().etudiantNom }}</strong> et marqué comme
                <strong>saisie</strong> — jamais comme identification par le lecteur.
              </span>
            </div>

            <label class="flex flex-col gap-1.5">
              <span class="flex items-baseline gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
                  Heure de présence
                </span>
                <span class="text-[11px] text-ink-faint">facultatif</span>
              </span>
              <span class="champ !h-11 max-w-[160px]">
                <input
                  type="time"
                  name="heure"
                  class="saisie num"
                  [ngModel]="heure()"
                  (ngModelChange)="heure.set($event)"
                />
              </span>
              <span class="text-[11px] text-ink-faint">
                À défaut, l'heure de début de la séance fait foi.
              </span>
            </label>
          }

          <label class="flex flex-col gap-1.5">
            <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
              Motif de la décision
            </span>
            <textarea
              name="commentaire"
              rows="3"
              class="w-full resize-none rounded-[10px] border border-line bg-white px-3.5 py-2.5 text-[13px] text-ink outline-none focus:border-royal-400"
              [placeholder]="
                accepte()
                  ? 'Confirmé par le registre de la salle.'
                  : 'Aucun élément ne corrobore la présence.'
              "
              maxlength="500"
              [ngModel]="commentaire()"
              (ngModelChange)="commentaire.set($event)"
            ></textarea>
            <span class="text-[11px] text-ink-faint">
              Obligatoire. L'enseignant verra cette réponse dans son application.
            </span>
          </label>
        </div>

        <footer
          class="flex items-center justify-end gap-2.5 border-t border-line-soft bg-[#FCFDFE] px-5 py-3.5"
        >
          <button type="button" class="btn btn-secondaire" (click)="fermer.emit()" [disabled]="envoi()">
            Annuler
          </button>
          <button type="submit" class="btn btn-primaire" [disabled]="!valide() || envoi()">
            @if (envoi()) {
              Enregistrement…
            } @else {
              {{ accepte() ? 'Retenir' : 'Écarter' }}
            }
          </button>
        </footer>
      </form>
    </sp-modale>
  `,
})
export class ArbitrageDialogComponent implements OnInit {
  private readonly service = inject(SignalementService);

  readonly signalement = input.required<Signalement>();
  readonly accepte = input.required<boolean>();

  readonly fermer = output<void>();
  readonly traite = output<void>();

  readonly commentaire = signal('');
  readonly heure = signal('');
  readonly envoi = signal(false);
  readonly erreur = signal('');

  readonly typeLisible = computed(() => libelleTypeSignalement(this.signalement().type));

  /** Seul un « étudiant non reconnu » retenu donne lieu à un relevé correctif. */
  readonly produitUneRegularisation = computed(
    () => this.signalement().type === 'ETUDIANT_NON_RECONNU',
  );

  readonly valide = computed(() => this.commentaire().trim().length > 0);

  ngOnInit(): void {
    if (this.produitUneRegularisation()) {
      // L'heure de début de la séance est le repli le plus défendable.
      const debut = new Date(this.signalement().seanceDebut);
      this.heure.set(
        `${String(debut.getHours()).padStart(2, '0')}:${String(debut.getMinutes()).padStart(2, '0')}`,
      );
    }
  }

  valider(): void {
    if (!this.valide() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');

    const heure = this.heure();
    this.service
      .traiter(this.signalement().id, {
        accepte: this.accepte(),
        commentaire: this.commentaire().trim(),
        heurePresence:
          this.accepte() && this.produitUneRegularisation() && heure ? `${heure}:00` : null,
      })
      .subscribe({
        next: () => {
          this.envoi.set(false);
          this.traite.emit();
        },
        error: (erreur: unknown) => {
          // Le backend refuse notamment un signalement déjà tranché, ou une
          // régularisation pour un étudiant qui a entre-temps été relevé.
          this.erreur.set(messageErreur(erreur));
          this.envoi.set(false);
        },
      });
  }
}
