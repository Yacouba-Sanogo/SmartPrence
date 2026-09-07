import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Noms d'icônes disponibles.
 *
 * Le type ferme la liste : une icône inexistante devient une erreur de compilation
 * plutôt qu'un carré vide découvert en production.
 */
export type NomIcone =
  | 'tableau-de-bord'
  | 'personnel'
  | 'empreinte'
  | 'etudiants'
  | 'presences'
  | 'classes'
  | 'salles'
  | 'appareils'
  | 'statistiques'
  | 'parametres'
  | 'chevron-droit'
  | 'chevron-gauche'
  | 'chevron-bas'
  | 'calendrier'
  | 'recherche'
  | 'telecharger'
  | 'plus'
  | 'entree'
  | 'sortie'
  | 'horloge'
  | 'coche'
  | 'coche-cercle'
  | 'moins-cercle'
  | 'diffusion'
  | 'alerte'
  | 'info'
  | 'bouclier'
  | 'fermer'
  | 'crayon'
  | 'options'
  | 'deconnexion'
  | 'rafraichir'
  | 'menu';

/**
 * Icônes SVG tracées, sur une grille de 24 et en `currentColor`.
 *
 * Des glyphes typographiques (♙, ⚇, ▦) ont été écartés : ils dépendent des polices
 * installées, ne se recolorent pas de façon fiable et rendent mal à grande taille.
 */
@Component({
  selector: 'sp-icone',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="taille()"
      [attr.height]="taille()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      [attr.stroke-width]="epaisseur()"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      @switch (nom()) {
        @case ('tableau-de-bord') {
          <rect x="3" y="3" width="7" height="7" rx="1.5" />
          <rect x="14" y="3" width="7" height="7" rx="1.5" />
          <rect x="3" y="14" width="7" height="7" rx="1.5" />
          <rect x="14" y="14" width="7" height="7" rx="1.5" />
        }
        @case ('personnel') {
          <path d="M16 20v-1.5a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4V20" />
          <circle cx="9" cy="7.5" r="3.5" />
          <path d="M22 20v-1.5a4 4 0 0 0-3-3.87" />
          <path d="M16 4.1a4 4 0 0 1 0 6.8" />
        }
        @case ('empreinte') {
          <path d="M12 20.5c-1.2-2-1.8-4.2-1.8-6.6a1.8 1.8 0 0 1 3.6 0c0 1.2.2 2.4.6 3.5" />
          <path d="M7.6 18.6A12 12 0 0 1 6.6 14a5.4 5.4 0 0 1 10.8 0c0 1 .1 2 .3 3" />
          <path d="M3.9 15.6A15 15 0 0 1 3.6 14a8.4 8.4 0 0 1 4.3-7.3" />
          <path d="M20.4 14a8.4 8.4 0 0 0-9.5-8.3" />
        }
        @case ('etudiants') {
          <path d="M12 3.2 2.6 8 12 12.8 21.4 8 12 3.2Z" />
          <path d="M6.2 10.4V16c0 1.7 2.6 3 5.8 3s5.8-1.3 5.8-3v-5.6" />
        }
        @case ('presences') {
          <rect x="3" y="4.5" width="18" height="16.5" rx="2.5" />
          <path d="M8 3v3M16 3v3M3 9.6h18" />
          <path d="m9 14.8 2 2 4-4" />
        }
        @case ('classes') {
          <path d="m12 2.8 9 4.7-9 4.7-9-4.7 9-4.7Z" />
          <path d="m3 12.5 9 4.7 9-4.7" />
          <path d="m3 17.2 9 4.7 9-4.7" />
        }
        @case ('salles') {
          <rect x="4" y="3" width="16" height="18" rx="2" />
          <path d="M9.5 21v-5.2h5V21" />
          <path d="M8.6 7.6h2M13.4 7.6h2M8.6 11.6h2M13.4 11.6h2" />
        }
        @case ('appareils') {
          <rect x="6" y="6" width="12" height="12" rx="2" />
          <rect x="9.6" y="9.6" width="4.8" height="4.8" rx="1" />
          <path d="M9 3v3M15 3v3M9 18v3M15 18v3M3 9h3M3 15h3M18 9h3M18 15h3" />
        }
        @case ('statistiques') {
          <path d="M3.5 20.5h17" />
          <rect x="5" y="12" width="3.8" height="5.6" rx="1" />
          <rect x="10.1" y="8" width="3.8" height="9.6" rx="1" />
          <rect x="15.2" y="4.4" width="3.8" height="13.2" rx="1" />
        }
        @case ('parametres') {
          <path d="M4 6.5h9M19 6.5h1M4 12h5M14.5 12h5.5M4 17.5h9M19 17.5h1" />
          <circle cx="16" cy="6.5" r="2.2" />
          <circle cx="11.5" cy="12" r="2.2" />
          <circle cx="16" cy="17.5" r="2.2" />
        }
        @case ('chevron-droit') {
          <path d="m9 5 7 7-7 7" />
        }
        @case ('chevron-gauche') {
          <path d="m15 5-7 7 7 7" />
        }
        @case ('chevron-bas') {
          <path d="m5 9 7 7 7-7" />
        }
        @case ('calendrier') {
          <rect x="3" y="4.5" width="18" height="16.5" rx="2.5" />
          <path d="M8 3v3M16 3v3M3 9.6h18" />
        }
        @case ('recherche') {
          <circle cx="11" cy="11" r="6.5" />
          <path d="m16 16 4.5 4.5" />
        }
        @case ('telecharger') {
          <path d="M12 3.5v11" />
          <path d="m7.5 10 4.5 4.5 4.5-4.5" />
          <path d="M4 19.5h16" />
        }
        @case ('plus') {
          <path d="M12 5.5v13M5.5 12h13" />
        }
        @case ('entree') {
          <path d="M15 3.5h3.5a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H15" />
          <path d="m10 16.5 4.5-4.5L10 7.5" />
          <path d="M14.5 12h-11" />
        }
        @case ('sortie') {
          <path d="M9 3.5H5.5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2H9" />
          <path d="m16 16.5 4.5-4.5L16 7.5" />
          <path d="M20.5 12h-11" />
        }
        @case ('horloge') {
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7.4v5l3.2 2" />
        }
        @case ('coche') {
          <path d="m5 12.5 4.5 4.5L19 7" />
        }
        @case ('coche-cercle') {
          <circle cx="12" cy="12" r="9" />
          <path d="m8.4 12.4 2.6 2.6 4.6-5.2" />
        }
        @case ('moins-cercle') {
          <circle cx="12" cy="12" r="9" />
          <path d="M8.5 12h7" />
        }
        @case ('diffusion') {
          <circle cx="12" cy="12" r="2.6" />
          <path d="M8.2 8.2a5.4 5.4 0 0 0 0 7.6M15.8 15.8a5.4 5.4 0 0 0 0-7.6" />
          <path d="M5.6 5.6a9 9 0 0 0 0 12.8M18.4 18.4a9 9 0 0 0 0-12.8" />
        }
        @case ('alerte') {
          <path d="M12 8.5v5" />
          <path d="M12 16.6h.01" />
          <path d="M10.3 3.9 2.6 17.4A2 2 0 0 0 4.3 20.4h15.4a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
        }
        @case ('info') {
          <circle cx="12" cy="12" r="9" />
          <path d="M12 11.2v5" />
          <path d="M12 7.9h.01" />
        }
        @case ('bouclier') {
          <path d="M12 2.8 4.5 6v6c0 4.5 3.2 8.3 7.5 9.2 4.3-.9 7.5-4.7 7.5-9.2V6L12 2.8Z" />
          <path d="m9 12.2 2.2 2.2 4-4.4" />
        }
        @case ('fermer') {
          <path d="M6 6l12 12M18 6 6 18" />
        }
        @case ('crayon') {
          <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Z" />
          <path d="M14.5 7.5 17 10" />
        }
        @case ('options') {
          <circle cx="12" cy="5.5" r="1.3" />
          <circle cx="12" cy="12" r="1.3" />
          <circle cx="12" cy="18.5" r="1.3" />
        }
        @case ('deconnexion') {
          <path d="M9 3.5H5.5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2H9" />
          <path d="m16 16.5 4.5-4.5L16 7.5" />
          <path d="M20.5 12h-11" />
        }
        @case ('rafraichir') {
          <path d="M20.5 12a8.5 8.5 0 1 1-2.5-6" />
          <path d="M20.5 4v5h-5" />
        }
        @case ('menu') {
          <path d="M4 7h16M4 12h16M4 17h16" />
        }
      }
    </svg>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        flex-shrink: 0;
      }
    `,
  ],
})
export class IconeComponent {
  readonly nom = input.required<NomIcone>();
  readonly taille = input(18);
  readonly epaisseur = input(1.6);
}
