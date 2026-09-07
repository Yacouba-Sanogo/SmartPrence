import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import { Salle } from '../../core/models/appareil.model';
import { SalleService } from '../../core/services/salle.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';
import { SalleDialogComponent } from './salle-dialog.component';

/**
 * Inventaire des salles physiques.
 *
 * <p>Une salle n'a d'intérêt ici que par ce qu'elle porte : la colonne « Lecteur » est
 * la seule qui relie cet inventaire au dispositif biométrique.</p>
 */
@Component({
  selector: 'sp-salles-liste',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent, SalleDialogComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Salles</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Inventaire des salles physiques et des lecteurs biométriques qui y sont installés.
          </p>
        </div>
        <div class="flex shrink-0 items-center gap-2.5">
          <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
            <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
            Actualiser
          </button>
          <button type="button" class="btn btn-primaire" (click)="ouvrirCreation()">
            <sp-icone nom="plus" [taille]="16" [epaisseur]="1.8" />
            Ajouter une salle
          </button>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3 sm:grid-cols-3">
        @for (kpi of indicateurs(); track kpi.libelle) {
          <div class="carte flex flex-col gap-2.5 px-4 py-3.5">
            <div class="flex items-center gap-2.5">
              <span class="grid size-7 place-items-center rounded-lg" [class]="kpi.teinte">
                <sp-icone [nom]="kpi.icone" [taille]="16" [epaisseur]="1.7" />
              </span>
              <span class="truncate text-xs font-medium text-ink-muted">{{ kpi.libelle }}</span>
            </div>
            <div class="flex items-baseline gap-1.5">
              <span class="num text-[27px] font-semibold leading-none tracking-tight text-ink">
                {{ kpi.valeur }}
              </span>
              <span class="truncate text-[11.5px] text-ink-faint">{{ kpi.detail }}</span>
            </div>
          </div>
        }
      </div>

      @if (erreurAction()) {
        <div
          class="flex items-start gap-3 rounded-[12px] border border-danger-line bg-danger-bg px-4 py-3.5"
          role="alert"
        >
          <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-white/60 text-danger">
            <sp-icone nom="alerte" [taille]="17" [epaisseur]="1.7" />
          </span>
          <span class="min-w-0 flex-1 text-[13px] leading-relaxed text-danger">{{ erreurAction() }}</span>
          <button
            type="button"
            class="grid size-7 shrink-0 place-items-center rounded-lg text-danger/70 hover:text-danger"
            (click)="erreurAction.set('')"
            aria-label="Masquer"
          >
            <sp-icone nom="fermer" [taille]="15" [epaisseur]="1.9" />
          </button>
        </div>
      }

      <div class="carte flex flex-wrap items-center gap-2.5 px-4 py-3">
        <label class="champ min-w-[210px] flex-1 bg-[#FBFCFE]">
          <sp-icone nom="recherche" [taille]="16" [epaisseur]="1.7" class="text-ink-faint" />
          <input
            type="search"
            class="saisie"
            placeholder="Rechercher un code, un nom, un bâtiment…"
            [ngModel]="recherche()"
            (ngModelChange)="recherche.set($event)"
            aria-label="Rechercher une salle"
          />
        </label>

        <select
          class="champ text-ink"
          [ngModel]="filtreBatiment()"
          (ngModelChange)="filtreBatiment.set($event)"
          aria-label="Filtrer par bâtiment"
        >
          <option [ngValue]="null">Tous les bâtiments</option>
          @for (batiment of batiments(); track batiment) {
            <option [ngValue]="batiment">{{ batiment }}</option>
          }
        </select>

        <select
          class="champ text-ink"
          [ngModel]="filtreEquipement()"
          (ngModelChange)="filtreEquipement.set($event)"
          aria-label="Filtrer par équipement"
        >
          <option value="TOUTES">Équipement : toutes</option>
          <option value="EQUIPEES">Équipées</option>
          <option value="NON_EQUIPEES">Non équipées</option>
        </select>
      </div>

      <section class="carte overflow-hidden">
        @if (chargement()) {
          <sp-etat titre="Chargement des salles…" icone="horloge" />
        } @else if (erreur()) {
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger les salles"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        } @else if (salles().length === 0) {
          <sp-etat
            icone="salles"
            titre="Aucune salle enregistrée"
            detail="Ajoutez une salle pour pouvoir y rattacher un lecteur biométrique."
            actionLibelle="Ajouter une salle"
            (action)="ouvrirCreation()"
          />
        } @else if (sallesFiltrees().length === 0) {
          <sp-etat
            icone="recherche"
            titre="Aucune salle ne correspond"
            detail="Ajustez la recherche ou les filtres pour élargir le résultat."
          />
        } @else {
          <div class="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th class="th !pl-[18px]">Salle</th>
                  <th class="th">Bâtiment</th>
                  <th class="th">Capacité</th>
                  <th class="th">Lecteur installé</th>
                  <th class="th !pr-[18px] text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (salle of sallesFiltrees(); track salle.id) {
                  <tr class="hover:bg-[#FBFCFE]">
                    <td class="td !pl-[18px]">
                      <div class="flex items-center gap-3">
                        <span
                          class="grid size-8 shrink-0 place-items-center rounded-lg bg-line-faint text-[#4A5470]"
                        >
                          <sp-icone nom="salles" [taille]="16" [epaisseur]="1.7" />
                        </span>
                        <span class="flex min-w-0 flex-col gap-0.5">
                          <span class="truncate text-[13.5px] font-medium text-ink">{{ salle.nom }}</span>
                          <span class="num text-[11px] uppercase text-ink-faint">{{ salle.code }}</span>
                        </span>
                      </div>
                    </td>
                    <td class="td text-[#364057]">{{ salle.batiment || '—' }}</td>
                    <td class="td num text-[#364057]">
                      {{ salle.capacite !== null ? salle.capacite + ' places' : '—' }}
                    </td>
                    <td class="td">
                      @if (salle.deviceNom) {
                        <span class="badge bg-royal-50 text-royal-600">
                          <sp-icone nom="appareils" [taille]="12" [epaisseur]="1.9" />
                          {{ salle.deviceNom }}
                        </span>
                      } @else {
                        <span class="text-[12.5px] text-ink-faint">Non équipée</span>
                      }
                    </td>
                    <td class="td !pr-[18px]">
                      <div class="flex items-center justify-end gap-2">
                        <button
                          type="button"
                          class="grid size-[30px] place-items-center rounded-[7px] border border-line bg-white text-ink-muted transition-colors hover:bg-canvas"
                          (click)="ouvrirModification(salle)"
                          aria-label="Modifier"
                        >
                          <sp-icone nom="crayon" [taille]="15" [epaisseur]="1.7" />
                        </button>
                        <button
                          type="button"
                          class="grid size-[30px] place-items-center rounded-[7px] border border-line bg-white text-ink-muted transition-colors hover:border-danger-line hover:bg-danger-bg hover:text-danger disabled:opacity-50"
                          (click)="supprimer(salle)"
                          [disabled]="suppressionEnCours() === salle.id"
                          aria-label="Supprimer"
                        >
                          <sp-icone nom="fermer" [taille]="15" [epaisseur]="1.9" />
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <footer class="border-t border-line-soft bg-[#FCFDFE] px-[18px] py-3 text-xs text-ink-subtle">
            {{ sallesFiltrees().length }} salle(s) sur {{ salles().length }}
          </footer>
        }
      </section>
    </div>

    @if (dialogueOuvert()) {
      <sp-salle-dialog
        [salle]="salleEnEdition()"
        (fermer)="fermerDialogue()"
        (enregistre)="apresEnregistrement()"
      />
    }
  `,
})
export class SallesListeComponent {
  private readonly service = inject(SalleService);

  readonly salles = signal<Salle[]>([]);
  readonly chargement = signal(false);

  /** Échec de chargement : la liste ne peut rien afficher, l'écran bascule en erreur. */
  readonly erreur = signal('');

  /**
   * Échec d'une action ponctuelle (suppression refusée, conflit).
   *
   * Distinct de {@link erreur} à dessein : la liste reste affichée et exploitable,
   * seul un bandeau signale le refus. Réutiliser le même signal ferait disparaître
   * tout l'inventaire parce qu'une seule suppression a échoué.
   */
  readonly erreurAction = signal('');
  readonly dialogueOuvert = signal(false);
  readonly salleEnEdition = signal<Salle | null>(null);
  readonly suppressionEnCours = signal<number | null>(null);

  readonly recherche = signal('');
  readonly filtreBatiment = signal<string | null>(null);
  readonly filtreEquipement = signal<'TOUTES' | 'EQUIPEES' | 'NON_EQUIPEES'>('TOUTES');

  readonly batiments = computed(() =>
    [...new Set(this.salles().map((s) => s.batiment).filter((b): b is string => !!b))].sort((a, b) =>
      a.localeCompare(b, 'fr'),
    ),
  );

  readonly indicateurs = computed(() => {
    const inventaire = this.salles();
    const equipees = inventaire.filter((s) => !!s.deviceNom).length;
    const places = inventaire.reduce((total, s) => total + (s.capacite ?? 0), 0);
    return [
      {
        libelle: 'Salles',
        valeur: inventaire.length,
        detail: 'enregistrées',
        icone: 'salles' as const,
        teinte: 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Équipées',
        valeur: equipees,
        detail: inventaire.length ? `${Math.round((equipees / inventaire.length) * 100)} %` : '—',
        icone: 'appareils' as const,
        teinte: 'bg-royal-50 text-royal-600',
      },
      {
        libelle: 'Capacité totale',
        valeur: places,
        detail: 'places',
        icone: 'personnel' as const,
        teinte: 'bg-succes-bg text-succes',
      },
    ];
  });

  readonly sallesFiltrees = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    const batiment = this.filtreBatiment();
    const equipement = this.filtreEquipement();

    return this.salles().filter((salle) => {
      if (batiment && salle.batiment !== batiment) return false;
      if (equipement === 'EQUIPEES' && !salle.deviceNom) return false;
      if (equipement === 'NON_EQUIPEES' && salle.deviceNom) return false;
      if (!terme) return true;
      return `${salle.code} ${salle.nom} ${salle.batiment ?? ''}`.toLowerCase().includes(terme);
    });
  });

  constructor() {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.service.lister().subscribe({
      next: (salles) => {
        this.salles.set(salles ?? []);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  ouvrirCreation(): void {
    this.salleEnEdition.set(null);
    this.dialogueOuvert.set(true);
  }

  ouvrirModification(salle: Salle): void {
    this.salleEnEdition.set(salle);
    this.dialogueOuvert.set(true);
  }

  fermerDialogue(): void {
    this.dialogueOuvert.set(false);
    this.salleEnEdition.set(null);
  }

  apresEnregistrement(): void {
    this.fermerDialogue();
    this.charger();
  }

  supprimer(salle: Salle): void {
    if (this.suppressionEnCours()) return;
    // Le backend refuse la suppression d'une salle équipée : son message est
    // plus précis que tout ce que l'interface pourrait deviner.
    this.suppressionEnCours.set(salle.id);
    this.erreurAction.set('');

    this.service.supprimer(salle.id).subscribe({
      next: () => {
        this.salles.update((inventaire) => inventaire.filter((s) => s.id !== salle.id));
        this.suppressionEnCours.set(null);
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.suppressionEnCours.set(null);
      },
    });
  }
}
