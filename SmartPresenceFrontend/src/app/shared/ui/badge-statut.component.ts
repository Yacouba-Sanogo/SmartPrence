import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { StatutPresence, libelleStatut } from '../../core/models/pointage.model';

/**
 * Pastille de statut de présence.
 *
 * Les teintes restent sobres et se distinguent aussi en niveaux de gris : les
 * relevés de présence sont imprimés et versés à des dossiers administratifs.
 */
@Component({
  selector: 'sp-badge-statut',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge" [class]="classes()">{{ texte() }}</span>`,
})
export class BadgeStatutComponent {
  readonly statut = input.required<StatutPresence>();

  readonly texte = computed(() => libelleStatut(this.statut()).toUpperCase());

  readonly classes = computed(() => {
    switch (this.statut()) {
      case 'PRESENT':
        return 'bg-succes-bg text-succes';
      case 'RETARD':
        return 'bg-alerte-bg text-alerte';
      case 'ABSENT':
        return 'bg-danger-bg text-danger';
      case 'JUSTIFIE':
        return 'bg-royal-50 text-royal-600';
    }
  });
}
