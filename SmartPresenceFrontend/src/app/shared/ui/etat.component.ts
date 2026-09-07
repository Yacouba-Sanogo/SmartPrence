import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { IconeComponent, NomIcone } from './icone.component';

/** Zone de chargement, d'absence de résultat ou d'erreur, à l'intérieur d'une carte. */
@Component({
  selector: 'sp-etat',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconeComponent],
  template: `
    <div class="flex flex-col items-center gap-2.5 px-6 py-14 text-center">
      <span class="grid size-11 place-items-center rounded-xl" [class]="teinte()">
        <sp-icone [nom]="icone()" [taille]="20" />
      </span>
      <span class="text-[15px] font-semibold text-ink">{{ titre() }}</span>
      @if (detail()) {
        <span class="max-w-md text-[13px] leading-relaxed text-ink-subtle">{{ detail() }}</span>
      }
      @if (actionLibelle()) {
        <button type="button" class="btn btn-secondaire mt-2" (click)="action.emit()">
          {{ actionLibelle() }}
        </button>
      }
    </div>
  `,
})
export class EtatComponent {
  readonly titre = input.required<string>();
  readonly detail = input<string>('');
  readonly icone = input<NomIcone>('info');
  readonly variante = input<'neutre' | 'erreur'>('neutre');
  readonly actionLibelle = input<string>('');

  readonly action = output<void>();

  protected teinte(): string {
    return this.variante() === 'erreur'
      ? 'bg-danger-bg text-danger'
      : 'bg-canvas text-ink-subtle';
  }
}
