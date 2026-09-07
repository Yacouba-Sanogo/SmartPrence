import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { messageErreur } from '../../core/api';
import { PagedResponse } from '../../core/models/api.model';
import { Classe } from '../../core/models/classe.model';
import {
  Presence,
  SourcePresence,
  StatutPresence,
  heureCourte,
  initialesReleve,
  libelleStatutPresence,
  nomCompletReleve,
  teinteStatutPresence,
} from '../../core/models/presence.model';
import { ClasseService } from '../../core/services/classe.service';
import { PresenceService } from '../../core/services/presence.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';

const PAGE_VIDE: PagedResponse<Presence> = {
  content: [],
  page: 0,
  size: 25,
  totalElements: 0,
  totalPages: 0,
  last: true,
};

/**
 * Relevés de présence des étudiants.
 *
 * <p>La colonne <b>Origine</b> n'est pas décorative : elle sépare ce que le lecteur a
 * constaté de ce qu'un humain a décidé. Sans elle, une régularisation administrative
 * deviendrait indiscernable d'une identification biométrique, et le registre perdrait
 * la valeur probante qui justifie tout le dispositif.</p>
 */
@Component({
  selector: 'sp-presences-liste',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Présences</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Relevés des étudiants, constatés par les lecteurs ou régularisés par la scolarité.
          </p>
        </div>
        <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
          <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
          Actualiser
        </button>
      </div>

      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
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

      <div class="carte flex flex-wrap items-end gap-2.5 px-4 py-3">
        <label class="flex flex-col gap-1">
          <span class="text-[10.5px] font-semibold text-ink-muted">Du</span>
          <span class="champ !h-9">
            <input
              type="date"
              class="saisie num"
              [ngModel]="dateDebut()"
              (ngModelChange)="changerDebut($event)"
              aria-label="Début de période"
            />
          </span>
        </label>
        <label class="flex flex-col gap-1">
          <span class="text-[10.5px] font-semibold text-ink-muted">Au</span>
          <span class="champ !h-9">
            <input
              type="date"
              class="saisie num"
              [ngModel]="dateFin()"
              (ngModelChange)="changerFin($event)"
              aria-label="Fin de période"
            />
          </span>
        </label>

        <label class="flex min-w-[140px] flex-1 flex-col gap-1">
          <span class="text-[10.5px] font-semibold text-ink-muted">Classe</span>
          <span class="champ !h-9">
            <select
              class="saisie bg-transparent"
              [ngModel]="filtreClasse()"
              (ngModelChange)="appliquer('classe', $event)"
              aria-label="Filtrer par classe"
            >
              <option [ngValue]="null">Toutes</option>
              @for (classe of classes(); track classe.id) {
                <option [ngValue]="classe.id">{{ classe.code }}</option>
              }
            </select>
          </span>
        </label>

        <label class="flex min-w-[120px] flex-col gap-1">
          <span class="text-[10.5px] font-semibold text-ink-muted">Statut</span>
          <span class="champ !h-9">
            <select
              class="saisie bg-transparent"
              [ngModel]="filtreStatut()"
              (ngModelChange)="appliquer('statut', $event)"
              aria-label="Filtrer par statut"
            >
              <option [ngValue]="null">Tous</option>
              <option value="PRESENT">Présent</option>
              <option value="RETARD">Retard</option>
              <option value="ABSENT">Absent</option>
              <option value="JUSTIFIE">Justifié</option>
            </select>
          </span>
        </label>

        <label class="flex min-w-[120px] flex-col gap-1">
          <span class="text-[10.5px] font-semibold text-ink-muted">Origine</span>
          <span class="champ !h-9">
            <select
              class="saisie bg-transparent"
              [ngModel]="filtreSource()"
              (ngModelChange)="appliquer('source', $event)"
              aria-label="Filtrer par origine"
            >
              <option [ngValue]="null">Toutes</option>
              <option value="ESP32">Lecteur</option>
              <option value="MANUEL">Saisie</option>
              <option value="IMPORT">Import</option>
            </select>
          </span>
        </label>

        @if (filtreActif()) {
          <button type="button" class="btn btn-discret !h-9" (click)="reinitialiser()">
            Réinitialiser
          </button>
        }
      </div>

      <section class="carte overflow-hidden">
        @if (chargement()) {
          <sp-etat titre="Chargement des relevés…" icone="horloge" />
        } @else if (erreur()) {
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger les relevés"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        } @else if (releves().length === 0) {
          <sp-etat
            icone="presences"
            titre="Aucun relevé sur cette période"
            [detail]="messageVide()"
          />
        } @else {
          <div class="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th class="th !pl-[18px]">Étudiant</th>
                  <th class="th">Classe</th>
                  <th class="th">Date</th>
                  <th class="th">Heure</th>
                  <th class="th">Séance</th>
                  <th class="th">Statut</th>
                  <th class="th !pr-[18px]">Origine</th>
                </tr>
              </thead>
              <tbody>
                @for (releve of releves(); track releve.id) {
                  <tr class="hover:bg-[#FBFCFE]">
                    <td class="td !pl-[18px]">
                      <div class="flex items-center gap-3">
                        <span
                          class="grid size-8 shrink-0 place-items-center rounded-lg text-[11px] font-semibold"
                          [class]="teinte(releve.statut)"
                        >
                          {{ initiales(releve) }}
                        </span>
                        <span class="flex min-w-0 flex-col gap-0.5">
                          <span class="truncate text-[13.5px] font-medium text-ink">
                            {{ nomComplet(releve) }}
                          </span>
                          <span class="num text-[11px] text-ink-faint">
                            {{ releve.etudiantMatricule }}
                          </span>
                        </span>
                      </div>
                    </td>
                    <td class="td text-[#364057]">{{ releve.classeCode || '—' }}</td>
                    <td class="td num text-[#364057]">{{ releve.datePresence }}</td>
                    <td class="td num text-[#364057]">{{ heure(releve.heurePresence) }}</td>
                    <td class="td">
                      <span class="flex min-w-0 flex-col gap-0.5">
                        <span class="truncate text-[12.5px] text-ink">
                          {{ releve.matiereLibelle || 'Hors séance' }}
                        </span>
                        @if (releve.salleNom) {
                          <span class="truncate text-[11px] text-ink-faint">{{ releve.salleNom }}</span>
                        }
                      </span>
                    </td>
                    <td class="td">
                      <span class="badge" [class]="teinte(releve.statut)">
                        {{ libelle(releve.statut) }}
                      </span>
                    </td>
                    <td class="td !pr-[18px]">
                      @if (releve.source === 'ESP32') {
                        <span class="badge bg-royal-50 text-royal-600" [title]="releve.deviceNom || ''">
                          <sp-icone nom="empreinte" [taille]="12" [epaisseur]="1.9" />
                          Lecteur
                        </span>
                      } @else if (releve.source === 'MANUEL') {
                        <span
                          class="badge bg-alerte-bg text-alerte"
                          title="Relevé décidé par un humain, non constaté par un lecteur"
                        >
                          <sp-icone nom="crayon" [taille]="12" [epaisseur]="1.9" />
                          Saisie
                        </span>
                      } @else {
                        <span class="badge bg-line-faint text-ink-subtle">Import</span>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <footer
            class="flex flex-wrap items-center justify-between gap-3 border-t border-line-soft bg-[#FCFDFE] px-[18px] py-3"
          >
            <span class="text-xs text-ink-subtle">
              {{ premierAffiche() }}–{{ dernierAffiche() }} sur {{ page().totalElements }} relevé(s)
            </span>
            <div class="flex items-center gap-2">
              <button
                type="button"
                class="btn btn-secondaire !h-8 !px-3"
                (click)="pagePrecedente()"
                [disabled]="page().page === 0"
              >
                <sp-icone nom="chevron-gauche" [taille]="14" [epaisseur]="2" />
              </button>
              <span class="num text-xs text-ink-muted">
                {{ page().page + 1 }} / {{ page().totalPages || 1 }}
              </span>
              <button
                type="button"
                class="btn btn-secondaire !h-8 !px-3"
                (click)="pageSuivante()"
                [disabled]="page().last"
              >
                <sp-icone nom="chevron-droit" [taille]="14" [epaisseur]="2" />
              </button>
            </div>
          </footer>
        }
      </section>
    </div>
  `,
})
export class PresencesListeComponent {
  private readonly service = inject(PresenceService);
  private readonly classeService = inject(ClasseService);

  readonly page = signal<PagedResponse<Presence>>(PAGE_VIDE);
  readonly classes = signal<Classe[]>([]);
  readonly chargement = signal(false);
  readonly erreur = signal('');

  readonly dateDebut = signal(this.ilYAUnMois());
  readonly dateFin = signal(this.aujourdhui());
  readonly filtreClasse = signal<number | null>(null);
  readonly filtreStatut = signal<StatutPresence | null>(null);
  readonly filtreSource = signal<SourcePresence | null>(null);

  readonly releves = computed(() => this.page().content ?? []);

  readonly filtreActif = computed(
    () => this.filtreClasse() !== null || this.filtreStatut() !== null || this.filtreSource() !== null,
  );

  /**
   * Indicateurs de la page affichée, pas de la période entière.
   *
   * Le backend ne renvoie pas d'agrégat sur la recherche ; compter sur le client ne
   * porterait que sur les 25 lignes chargées. Le libellé le dit explicitement plutôt
   * que de laisser croire à un total.
   */
  readonly indicateurs = computed(() => {
    const liste = this.releves();
    const compte = (statut: StatutPresence) => liste.filter((r) => r.statut === statut).length;
    const manuels = liste.filter((r) => r.source === 'MANUEL').length;
    return [
      {
        libelle: 'Relevés (période)',
        valeur: this.page().totalElements,
        detail: 'au total',
        icone: 'presences' as const,
        teinte: 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Présents (page)',
        valeur: compte('PRESENT'),
        detail: 'sur cette page',
        icone: 'coche-cercle' as const,
        teinte: 'bg-succes-bg text-succes',
      },
      {
        libelle: 'Retards (page)',
        valeur: compte('RETARD'),
        detail: 'sur cette page',
        icone: 'horloge' as const,
        teinte: 'bg-alerte-bg text-alerte',
      },
      {
        libelle: 'Saisies (page)',
        valeur: manuels,
        detail: manuels > 0 ? 'non biométriques' : 'tout au lecteur',
        icone: 'crayon' as const,
        teinte: manuels > 0 ? 'bg-alerte-bg text-alerte' : 'bg-line-faint text-[#4A5470]',
      },
    ];
  });

  readonly premierAffiche = computed(() =>
    this.page().totalElements === 0 ? 0 : this.page().page * this.page().size + 1,
  );

  readonly dernierAffiche = computed(() =>
    Math.min((this.page().page + 1) * this.page().size, this.page().totalElements),
  );

  readonly messageVide = computed(() =>
    this.filtreActif()
      ? 'Aucun relevé ne correspond aux filtres. Élargissez la période ou retirez un filtre.'
      : "Aucun passage devant un lecteur sur cette période. Vérifiez que les appareils sont en ligne et que des séances sont planifiées.",
  );

  constructor() {
    this.classeService
      .lister()
      .pipe(catchError(() => of([] as Classe[])))
      .subscribe((classes) => this.classes.set(classes ?? []));
    this.charger();
  }

  nomComplet(releve: Presence): string {
    return nomCompletReleve(releve);
  }

  initiales(releve: Presence): string {
    return initialesReleve(releve);
  }

  libelle(statut: StatutPresence): string {
    return libelleStatutPresence(statut);
  }

  teinte(statut: StatutPresence): string {
    return teinteStatutPresence(statut);
  }

  heure(valeur: string): string {
    return heureCourte(valeur);
  }

  charger(page = 0): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.service
      .rechercher({
        dateDebut: this.dateDebut(),
        dateFin: this.dateFin(),
        classeId: this.filtreClasse(),
        statut: this.filtreStatut(),
        source: this.filtreSource(),
        page,
        size: 25,
      })
      .subscribe({
        next: (resultat) => {
          this.page.set(resultat ?? PAGE_VIDE);
          this.chargement.set(false);
        },
        error: (erreur: unknown) => {
          this.erreur.set(messageErreur(erreur));
          this.chargement.set(false);
        },
      });
  }

  changerDebut(valeur: string): void {
    this.dateDebut.set(valeur);
    if (this.dateFin() < valeur) this.dateFin.set(valeur);
    this.charger();
  }

  changerFin(valeur: string): void {
    this.dateFin.set(valeur);
    if (valeur < this.dateDebut()) this.dateDebut.set(valeur);
    this.charger();
  }

  appliquer(filtre: 'classe' | 'statut' | 'source', valeur: unknown): void {
    if (filtre === 'classe') this.filtreClasse.set(valeur as number | null);
    if (filtre === 'statut') this.filtreStatut.set(valeur as StatutPresence | null);
    if (filtre === 'source') this.filtreSource.set(valeur as SourcePresence | null);
    // Retour à la première page : rester en page 4 d'un résultat qui n'en compte
    // plus que deux afficherait un tableau vide sans explication.
    this.charger();
  }

  reinitialiser(): void {
    this.filtreClasse.set(null);
    this.filtreStatut.set(null);
    this.filtreSource.set(null);
    this.charger();
  }

  pagePrecedente(): void {
    if (this.page().page > 0) this.charger(this.page().page - 1);
  }

  pageSuivante(): void {
    if (!this.page().last) this.charger(this.page().page + 1);
  }

  // ------------------------------------------------------------------

  private aujourdhui(): string {
    return this.enCle(new Date());
  }

  private ilYAUnMois(): string {
    const date = new Date();
    date.setMonth(date.getMonth() - 1);
    return this.enCle(date);
  }

  private enCle(date: Date): string {
    return [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0'),
    ].join('-');
  }
}
