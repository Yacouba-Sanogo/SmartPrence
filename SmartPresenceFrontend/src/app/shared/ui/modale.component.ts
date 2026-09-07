import { ChangeDetectionStrategy, Component, HostListener, input, output } from '@angular/core';
import { IconeComponent, NomIcone } from './icone.component';

/**
 * Boîte de dialogue modale.
 *
 * Le fond assombri et la touche Échap ferment la modale ; le contenu est projeté,
 * ce qui laisse chaque écran maître de son formulaire.
 */
@Component({
  selector: 'sp-modale',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconeComponent],
  template: `
    <div class="fixed inset-0 z-[60] flex items-center justify-center overflow-y-auto bg-ink/55 p-4">
      <button
        type="button"
        aria-label="Fermer"
        class="absolute inset-0 cursor-default"
        (click)="fermer.emit()"
      ></button>

      <div
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="titre()"
        class="apparait relative my-auto w-full max-w-[620px] overflow-hidden rounded-[14px] bg-white shadow-[0_24px_60px_rgba(10,18,40,0.34)]"
      >
        <header class="flex items-start gap-3 border-b border-line-soft px-5 py-4">
          <span class="grid size-[38px] shrink-0 place-items-center rounded-[10px]" [class]="teinte()">
            <sp-icone [nom]="icone()" [taille]="20" />
          </span>
          <span class="flex min-w-0 flex-1 flex-col gap-0.5">
            <h2 class="m-0 text-[17px] font-semibold tracking-tight text-ink">{{ titre() }}</h2>
            @if (sousTitre()) {
              <p class="m-0 text-[12.5px] text-ink-subtle">{{ sousTitre() }}</p>
            }
          </span>
          <button
            type="button"
            (click)="fermer.emit()"
            aria-label="Fermer"
            class="grid size-[30px] shrink-0 place-items-center rounded-lg text-ink-faint transition-colors hover:bg-canvas hover:text-ink"
          >
            <sp-icone nom="fermer" [taille]="16" [epaisseur]="1.9" />
          </button>
        </header>

        <ng-content />
      </div>
    </div>
  `,
})
export class ModaleComponent {
  readonly titre = input.required<string>();
  readonly sousTitre = input<string>('');
  readonly icone = input<NomIcone>('info');
  readonly variante = input<'royal' | 'succes'>('royal');

  readonly fermer = output<void>();

  @HostListener('document:keydown.escape')
  protected surEchap(): void {
    this.fermer.emit();
  }

  protected teinte(): string {
    return this.variante() === 'succes'
      ? 'bg-succes-bg text-succes'
      : 'bg-royal-50 text-royal-600';
  }
}
