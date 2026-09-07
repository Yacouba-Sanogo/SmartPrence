import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { CompteEtudiant } from '../../core/models/etudiant.model';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';

/**
 * Identifiants d'un accès mobile fraîchement ouvert.
 *
 * <p>Écran de remise : le mot de passe n'est restitué qu'ici, le serveur n'en conserve
 * que l'empreinte. D'où l'insistance visuelle et le bouton de copie — un administrateur
 * qui ferme cette fenêtre sans noter devra fermer puis rouvrir le compte.</p>
 */
@Component({
  selector: 'sp-compte-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      titre="Accès mobile créé"
      sousTitre="Identifiants à remettre à l'étudiant"
      icone="coche-cercle"
      variante="succes"
      (fermer)="fermer.emit()"
    >
      <div class="flex flex-col gap-4 px-5 py-5">
        <div class="flex items-center gap-3 rounded-[10px] border border-[#E7EBF4] bg-[#F8FAFD] px-3.5 py-3">
          <span
            class="grid size-10 shrink-0 place-items-center rounded-full bg-[#E4E9F5] text-[13px] font-semibold text-[#364057]"
          >
            {{ initiales() }}
          </span>
          <span class="flex min-w-0 flex-col gap-0.5">
            <span class="truncate text-sm font-semibold text-ink">{{ compte().nomComplet }}</span>
            <span class="num truncate text-xs text-ink-subtle">{{ compte().matricule }}</span>
          </span>
        </div>

        <label class="flex flex-col gap-1.5">
          <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
            Identifiant de connexion
          </span>
          <div class="flex items-center gap-2">
            <span class="champ !h-11 flex-1">
              <input type="text" class="saisie num text-[12.5px]" readonly [value]="compte().email" />
            </span>
            <button type="button" class="btn btn-discret !h-11" (click)="copier(compte().email, 'email')">
              {{ copie() === 'email' ? 'Copié' : 'Copier' }}
            </button>
          </div>
        </label>

        <label class="flex flex-col gap-1.5">
          <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
            Mot de passe initial
          </span>
          <div class="flex items-center gap-2">
            <span class="champ !h-11 flex-1">
              <input
                type="text"
                class="saisie num text-[13px] font-medium tracking-wide"
                readonly
                [value]="compte().motDePasseInitial"
              />
            </span>
            <button
              type="button"
              class="btn btn-discret !h-11"
              (click)="copier(compte().motDePasseInitial, 'motDePasse')"
            >
              {{ copie() === 'motDePasse' ? 'Copié' : 'Copier' }}
            </button>
          </div>
        </label>

        <p
          class="m-0 flex items-start gap-3 rounded-[11px] border border-alerte-line bg-[#FFF9EC] px-4 py-3.5 text-xs leading-relaxed text-[#6E4806]"
        >
          <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-alerte-bg text-alerte">
            <sp-icone nom="alerte" [taille]="17" [epaisseur]="1.7" />
          </span>
          <span>
            <span class="font-semibold">Notez ce mot de passe maintenant.</span> Il n'est affiché
            qu'une fois : le serveur n'en conserve que l'empreinte et ne pourra pas le redonner.
            Invitez l'étudiant à le changer après sa première connexion.
          </span>
        </p>
      </div>

      <footer class="flex items-center gap-2.5 border-t border-line-soft bg-[#FCFDFE] px-5 py-4">
        <span class="flex-1"></span>
        <button type="button" class="btn btn-primaire" (click)="fermer.emit()">
          J'ai noté les identifiants
        </button>
      </footer>
    </sp-modale>
  `,
})
export class CompteDialogComponent {
  readonly compte = input.required<CompteEtudiant>();
  readonly fermer = output<void>();

  readonly copie = signal<'email' | 'motDePasse' | null>(null);

  protected initiales(): string {
    const parties = this.compte().nomComplet.split(' ').filter(Boolean);
    return parties
      .slice(0, 2)
      .map((p) => p[0])
      .join('')
      .toUpperCase();
  }

  copier(valeur: string, champ: 'email' | 'motDePasse'): void {
    void navigator.clipboard?.writeText(valeur).then(() => this.copie.set(champ));
  }
}
