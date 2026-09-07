import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Logo officiel de l'ENETP.
 *
 * <p>C'est le fichier fourni par l'établissement, affiché tel quel. Une version
 * redessinée en SVG l'a précédé ici : plus nette à l'agrandissement, mais ce n'était
 * pas le logo de l'école, et une identité ne se réinterprète pas.</p>
 *
 * <p>Le disque argenté fait partie du dessin et n'a pas été détouré : le lettrage
 * porte un liseré blanc conçu pour ressortir dessus, et l'isoler laisserait des
 * lettres cernées de blanc flottant sur le fond. Sur un fond sombre, on pose donc le
 * logo sur une plaque claire — voir {@link plaque} — plutôt que de retoucher
 * l'original.</p>
 */
@Component({
  selector: 'sp-logo-enetp',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="inline-grid shrink-0 place-items-center"
      [class]="plaque() ? 'rounded-full bg-white shadow-[0_2px_10px_rgba(19,44,115,0.14)]' : ''"
      [style.width.px]="cadre()"
      [style.height.px]="cadre()"
    >
      <img
        src="logo-enetp.png"
        alt="Logo de l'ENETP — Qui maîtrise enseigne"
        [width]="taille()"
        [height]="taille()"
        [style.width.px]="taille()"
        [style.height.px]="taille()"
        class="object-contain"
        decoding="async"
      />
    </span>
  `,
})
export class LogoEnetpComponent {
  /** Côté du logo en pixels. Le fichier est carré : une seule dimension suffit. */
  readonly taille = input(120);

  /**
   * Pose le logo sur un disque blanc.
   *
   * <p>À utiliser dès que le fond est sombre ou coloré : le disque argenté du logo
   * s'y lirait comme un autocollant posé de travers, alors qu'une plaque claire lui
   * donne l'air d'un sceau.</p>
   */
  readonly plaque = input(false);

  /** Côté du conteneur : la plaque déborde du logo d'une marge régulière. */
  protected cadre(): number {
    return this.plaque() ? Math.round(this.taille() * 1.18) : this.taille();
  }
}
