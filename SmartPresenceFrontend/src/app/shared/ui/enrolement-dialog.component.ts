import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconeComponent } from './icone.component';
import { ModaleComponent } from './modale.component';

/** Personne à enrôler, réduite à ce que la modale affiche. */
export interface SujetEnrolement {
  nomComplet: string;
  matricule: string;
  /** Contexte affiché sous le nom : service pour un agent, classe pour un étudiant. */
  contexte: string | null;
  initiales: string;
}

/**
 * Enrôlement biométrique — commun au personnel et aux étudiants.
 *
 * <p>La modale est <b>purement présentationnelle</b> : elle collecte la référence et
 * l'émet, sans jamais appeler d'API. C'est l'écran appelant qui sait à quel référentiel
 * la personne appartient. Une modale qui appellerait elle-même un service ne serait
 * réutilisable que pour ce service-là.</p>
 *
 * <p>Le geste étant identique des deux côtés, le message de confidentialité l'est aussi,
 * mot pour mot — ce qui est précisément l'intérêt de n'avoir qu'un seul composant.</p>
 */
@Component({
  selector: 'sp-enrolement-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      titre="Enrôlement biométrique"
      [sousTitre]="sousTitre()"
      icone="empreinte"
      (fermer)="fermer.emit()"
    >
      <form (ngSubmit)="valider()">
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

          <div class="flex items-center gap-3 rounded-[10px] border border-[#E7EBF4] bg-[#F8FAFD] px-3.5 py-3">
            <span
              class="grid size-10 shrink-0 place-items-center rounded-full bg-[#E4E9F5] text-[13px] font-semibold text-[#364057]"
            >
              {{ sujet().initiales }}
            </span>
            <span class="flex min-w-0 flex-1 flex-col gap-0.5">
              <span class="truncate text-sm font-semibold text-ink">{{ sujet().nomComplet }}</span>
              <span class="truncate text-xs text-ink-subtle">
                <span class="num">{{ sujet().matricule }}</span>
                @if (sujet().contexte) { · {{ sujet().contexte }} }
              </span>
            </span>
            <span class="badge shrink-0 border border-alerte-line bg-[#FFFBF2] font-medium text-alerte">
              Non enrôlé
            </span>
          </div>

          <label class="flex flex-col gap-2">
            <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
              Référence de correspondance
            </span>
            <span class="champ !h-11">
              <input
                type="text"
                name="biometricId"
                class="saisie num font-medium tracking-wide"
                [placeholder]="exemple()"
                maxlength="100"
                required
                [ngModel]="reference()"
                (ngModelChange)="reference.set($event)"
              />
            </span>
            <span class="text-[11.5px] leading-relaxed text-ink-faint">
              Identifiant du slot attribué par le capteur lors de la capture — par convention
              <span class="num">{{ prefixe() }}</span> suivi du numéro de slot.
            </span>
          </label>

          <p
            class="m-0 flex items-start gap-3 rounded-[11px] border border-succes-line bg-[#F2F9F6] px-4 py-3.5"
          >
            <span class="grid size-[34px] shrink-0 place-items-center rounded-[9px] bg-[#DCEEE6] text-succes">
              <sp-icone nom="bouclier" [taille]="18" [epaisseur]="1.7" />
            </span>
            <span class="flex flex-col gap-1.5">
              <span class="text-[13px] font-semibold text-[#0B4F3B]">
                Aucune empreinte n'est transmise au serveur
              </span>
              <span class="text-xs leading-relaxed text-[#326354]">
                Le gabarit biométrique reste dans la mémoire interne du capteur AS608. Seule la
                référence logique est enregistrée en base — elle ne permet ni de reconstituer, ni
                de comparer une empreinte.
              </span>
            </span>
          </p>
        </div>

        <footer class="flex items-center gap-2.5 border-t border-line-soft bg-[#FCFDFE] px-5 py-4">
          <span class="flex-1"></span>
          <button type="button" class="btn btn-secondaire" (click)="fermer.emit()">Annuler</button>
          <button type="submit" class="btn btn-primaire" [disabled]="!valide() || envoi()">
            <sp-icone nom="coche" [taille]="15" [epaisseur]="2.1" />
            {{ envoi() ? 'Enrôlement…' : "Confirmer l'enrôlement" }}
          </button>
        </footer>
      </form>
    </sp-modale>
  `,
})
export class EnrolementDialogComponent {
  readonly sujet = input.required<SujetEnrolement>();

  /** Préfixe conventionnel de la référence : `PER-` pour un agent, `ETU-` pour un étudiant. */
  readonly prefixe = input('PER-');

  readonly sousTitre = input('Associer une empreinte à une personne du référentiel');

  /** Pilotés par l'appelant, qui exécute réellement l'appel. */
  readonly envoi = input(false);
  readonly erreur = input('');

  readonly fermer = output<void>();
  readonly confirmer = output<string>();

  readonly reference = signal('');

  readonly exemple = computed(() => `${this.prefixe()}0042`);
  readonly valide = computed(() => this.reference().trim().length > 0);

  valider(): void {
    if (!this.valide() || this.envoi()) return;
    this.confirmer.emit(this.reference().trim());
  }
}
