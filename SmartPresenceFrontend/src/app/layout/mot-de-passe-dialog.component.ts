import { ChangeDetectionStrategy, Component, computed, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../core/api';
import { MotDePasseService } from '../core/services/mot-de-passe.service';
import { ModaleComponent } from '../shared/ui/modale.component';

/**
 * Changement de mot de passe de l'utilisateur connecté.
 *
 * <p>Les comptes sont ouverts avec un mot de passe initial remis par l'administration :
 * cette fenêtre est le moyen de le remplacer par un mot de passe que seul son titulaire
 * connaît.</p>
 */
@Component({
  selector: 'sp-mot-de-passe-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent],
  template: `
    <sp-modale
      titre="Changer mon mot de passe"
      sousTitre="Choisissez un mot de passe que vous êtes seul à connaître"
      icone="bouclier"
      (fermer)="fermer.emit()"
    >
      <form class="flex flex-col gap-4 px-5 py-5" (ngSubmit)="enregistrer()">
        @if (erreur()) {
          <div class="rounded-[10px] border border-danger-line bg-danger-bg px-3.5 py-3 text-[13px] text-danger" role="alert">
            {{ erreur() }}
          </div>
        }
        @if (succes()) {
          <div class="rounded-[10px] bg-succes-bg px-3.5 py-3 text-[13px] text-succes" role="status">
            Mot de passe modifié.
          </div>
        }

        <label class="flex flex-col gap-1.5">
          <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Mot de passe actuel</span>
          <input class="champ" [type]="visible() ? 'text' : 'password'" name="actuel"
                 autocomplete="current-password" [ngModel]="actuel()" (ngModelChange)="actuel.set($event)" />
        </label>
        <label class="flex flex-col gap-1.5">
          <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Nouveau mot de passe</span>
          <input class="champ" [type]="visible() ? 'text' : 'password'" name="nouveau"
                 autocomplete="new-password" placeholder="8 caractères minimum"
                 [ngModel]="nouveau()" (ngModelChange)="nouveau.set($event)" />
        </label>
        <label class="flex flex-col gap-1.5">
          <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Confirmation</span>
          <input class="champ" [type]="visible() ? 'text' : 'password'" name="confirmation"
                 autocomplete="new-password" [ngModel]="confirmation()" (ngModelChange)="confirmation.set($event)" />
        </label>
        @if (probleme()) {
          <p class="m-0 text-xs text-danger">{{ probleme() }}</p>
        }
        <label class="flex items-center gap-2 text-[12.5px] text-ink-muted">
          <input type="checkbox" [checked]="visible()" (change)="visible.set(!visible())" />
          Afficher les mots de passe
        </label>

        <div class="flex justify-end gap-2 border-t border-line-soft pt-4">
          <button type="button" class="btn btn-secondaire" (click)="fermer.emit()">Fermer</button>
          <button type="submit" class="btn btn-primaire" [disabled]="!valide() || envoi()">
            {{ envoi() ? 'Enregistrement…' : 'Enregistrer' }}
          </button>
        </div>
      </form>
    </sp-modale>
  `,
})
export class MotDePasseDialogComponent {
  private readonly service = inject(MotDePasseService);

  readonly fermer = output<void>();

  readonly actuel = signal('');
  readonly nouveau = signal('');
  readonly confirmation = signal('');
  readonly visible = signal(false);
  readonly envoi = signal(false);
  readonly erreur = signal('');
  readonly succes = signal(false);

  readonly probleme = computed(() => {
    if (this.nouveau() && this.nouveau().length < 8) return 'Au moins 8 caractères.';
    if (this.confirmation() && this.confirmation() !== this.nouveau()) {
      return 'Les deux mots de passe ne correspondent pas.';
    }
    return '';
  });

  readonly valide = computed(
    () => !!this.actuel() && this.nouveau().length >= 8 && this.confirmation() === this.nouveau(),
  );

  enregistrer(): void {
    if (!this.valide() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');
    this.succes.set(false);
    this.service.changer(this.actuel(), this.nouveau()).subscribe({
      next: () => {
        this.envoi.set(false);
        this.succes.set(true);
        this.actuel.set('');
        this.nouveau.set('');
        this.confirmation.set('');
      },
      error: (e: unknown) => {
        this.erreur.set(messageErreur(e));
        this.envoi.set(false);
      },
    });
  }
}
