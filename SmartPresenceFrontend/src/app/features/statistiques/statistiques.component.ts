import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of } from 'rxjs';
import { messageErreur } from '../../core/api';
import { Classe } from '../../core/models/classe.model';
import { ClasseService } from '../../core/services/classe.service';
import {
  StatistiquesClasse,
  StatistiquesGlobales,
  StatistiquesService,
} from '../../core/services/statistiques.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';

const GLOBALES_VIDES: StatistiquesGlobales = {
  totalEtudiants: 0,
  totalClasses: 0,
  totalDevicesActifs: 0,
  totalPresencesEnregistrees: 0,
  totalPresents: 0,
  totalRetards: 0,
  totalAbsents: 0,
  totalJustifies: 0,
  tauxAssiduite: 0,
};

/** Point de la courbe de tendance, jours creux compris. */
interface PointTendance {
  jour: string;
  libelle: string;
  valeur: number;
  hauteur: number;
}

const MOIS = [
  'janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin',
  'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.',
];

/**
 * Indicateurs d'assiduité.
 *
 * <p>La courbe comble elle-même les jours sans relevé : le backend ne renvoie que les
 * jours peuplés, et les tasser côté client donnerait une tendance fausse — une semaine
 * creuse ressemblerait à une semaine pleine.</p>
 */
@Component({
  selector: 'sp-statistiques',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Statistiques</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Assiduité de l'établissement, par classe et dans le temps.
          </p>
        </div>
        <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
          <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
          Actualiser
        </button>
      </div>

      @if (chargement()) {
        <section class="carte"><sp-etat titre="Calcul des indicateurs…" icone="horloge" /></section>
      } @else if (erreur()) {
        <section class="carte">
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Indicateurs indisponibles"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        </section>
      } @else {
        <!-- Assiduité globale -->
        <section class="carte flex flex-col gap-4 px-5 py-5">
          <div class="flex flex-wrap items-end justify-between gap-4">
            <div class="flex flex-col gap-1">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
                Taux d'assiduité
              </span>
              <div class="flex items-baseline gap-1.5">
                <span class="num text-[40px] font-semibold leading-none tracking-tight text-ink">
                  {{ taux() }}
                </span>
                <span class="num text-[20px] text-ink-subtle">%</span>
              </div>
            </div>
            <span class="text-[12.5px] text-ink-faint">
              {{ globales().totalPresencesEnregistrees }} relevé(s) ·
              {{ globales().totalEtudiants }} étudiant(s) ·
              {{ globales().totalDevicesActifs }} lecteur(s) actif(s)
            </span>
          </div>

          <div class="h-2 overflow-hidden rounded-full bg-line-faint">
            <div class="h-full rounded-full transition-all" [class]="teinteTaux()" [style.width.%]="taux()"></div>
          </div>

          <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
            @for (part of repartition(); track part.libelle) {
              <div class="flex flex-col gap-1 rounded-[11px] bg-canvas px-3.5 py-3">
                <span class="text-[11px] text-ink-muted">{{ part.libelle }}</span>
                <div class="flex items-baseline gap-1.5">
                  <span class="num text-[22px] font-semibold leading-none" [class]="part.teinte">
                    {{ part.valeur }}
                  </span>
                  <span class="text-[11px] text-ink-faint">{{ part.pourcentage }} %</span>
                </div>
              </div>
            }
          </div>
        </section>

        <!-- Tendance -->
        <section class="carte flex flex-col gap-4 px-5 py-5">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <span class="text-[14px] font-semibold text-ink">Relevés par jour</span>
            <div class="flex items-center gap-1.5">
              @for (choix of periodes; track choix.jours) {
                <button
                  type="button"
                  class="rounded-[8px] border px-2.5 py-1 text-[11.5px] font-medium transition-colors"
                  [class]="
                    joursTendance() === choix.jours
                      ? 'border-royal-600 bg-royal-600 text-white'
                      : 'border-line bg-white text-ink-muted hover:bg-canvas'
                  "
                  (click)="changerPeriode(choix.jours)"
                >
                  {{ choix.libelle }}
                </button>
              }
            </div>
          </div>

          @if (maxTendance() === 0) {
            <p class="m-0 py-8 text-center text-[12.5px] text-ink-subtle">
              Aucun relevé sur cette période.
            </p>
          } @else {
            <div class="flex h-[150px] items-end gap-[3px]">
              @for (point of tendance(); track point.jour) {
                <div
                  class="group relative flex flex-1 flex-col justify-end"
                  [title]="point.libelle + ' — ' + point.valeur + ' relevé(s)'"
                >
                  <div
                    class="rounded-t-[3px] transition-colors"
                    [class]="point.valeur > 0 ? 'bg-royal-500 group-hover:bg-royal-700' : 'bg-line'"
                    [style.height.%]="point.hauteur"
                    [style.min-height.px]="point.valeur > 0 ? 3 : 1"
                  ></div>
                </div>
              }
            </div>
            <div class="flex items-center justify-between text-[11px] text-ink-faint">
              <span>{{ tendance()[0]?.libelle }}</span>
              <span class="num">maximum : {{ maxTendance() }}</span>
              <span>{{ tendance()[tendance().length - 1]?.libelle }}</span>
            </div>
          }
        </section>

        <!-- Par classe -->
        <section class="carte overflow-hidden">
          <header class="border-b border-line-soft px-[18px] py-3.5">
            <span class="text-[14px] font-semibold text-ink">Assiduité par classe</span>
          </header>

          @if (parClasse().length === 0) {
            <p class="m-0 px-[18px] py-8 text-center text-[12.5px] text-ink-subtle">
              Aucune classe enregistrée.
            </p>
          } @else {
            <div class="overflow-x-auto">
              <table>
                <thead>
                  <tr>
                    <th class="th !pl-[18px]">Classe</th>
                    <th class="th">Effectif</th>
                    <th class="th">Présents</th>
                    <th class="th">Retards</th>
                    <th class="th">Absents</th>
                    <th class="th !pr-[18px]">Assiduité</th>
                  </tr>
                </thead>
                <tbody>
                  @for (ligne of parClasse(); track ligne.classeId) {
                    <tr class="hover:bg-[#FBFCFE]">
                      <td class="td !pl-[18px]">
                        <span class="flex min-w-0 flex-col gap-0.5">
                          <span class="truncate text-[13.5px] font-medium text-ink">
                            {{ ligne.classeLibelle }}
                          </span>
                          <span class="num text-[11px] uppercase text-ink-faint">
                            {{ ligne.classeCode }}
                          </span>
                        </span>
                      </td>
                      <td class="td num text-[#364057]">{{ ligne.totalEtudiants }}</td>
                      <td class="td num text-succes">{{ ligne.totalPresents }}</td>
                      <td class="td num text-alerte">{{ ligne.totalRetards }}</td>
                      <td class="td num text-danger">{{ ligne.totalAbsents }}</td>
                      <td class="td !pr-[18px]">
                        <div class="flex items-center gap-2.5">
                          <div class="h-1.5 w-[70px] overflow-hidden rounded-full bg-line-faint">
                            <div
                              class="h-full rounded-full"
                              [class]="teinteDe(ligne.tauxAssiduite)"
                              [style.width.%]="arrondi(ligne.tauxAssiduite)"
                            ></div>
                          </div>
                          <span class="num text-[12.5px] text-ink">
                            {{ arrondi(ligne.tauxAssiduite) }} %
                          </span>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>
      }
    </div>
  `,
})
export class StatistiquesComponent {
  private readonly service = inject(StatistiquesService);
  private readonly classeService = inject(ClasseService);

  readonly globales = signal<StatistiquesGlobales>(GLOBALES_VIDES);
  readonly parClasse = signal<StatistiquesClasse[]>([]);
  readonly tendance = signal<PointTendance[]>([]);
  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly joursTendance = signal(14);

  protected readonly periodes = [
    { jours: 7, libelle: '7 j' },
    { jours: 14, libelle: '14 j' },
    { jours: 30, libelle: '30 j' },
  ];

  readonly taux = computed(() => Math.round(this.globales().tauxAssiduite));

  readonly maxTendance = computed(() =>
    this.tendance().reduce((max, p) => Math.max(max, p.valeur), 0),
  );

  readonly repartition = computed(() => {
    const g = this.globales();
    const total = g.totalPresencesEnregistrees || 1;
    const part = (valeur: number) => Math.round((valeur / total) * 100);
    return [
      { libelle: 'Présents', valeur: g.totalPresents, pourcentage: part(g.totalPresents), teinte: 'text-succes' },
      { libelle: 'Retards', valeur: g.totalRetards, pourcentage: part(g.totalRetards), teinte: 'text-alerte' },
      { libelle: 'Absents', valeur: g.totalAbsents, pourcentage: part(g.totalAbsents), teinte: 'text-danger' },
      { libelle: 'Justifiés', valeur: g.totalJustifies, pourcentage: part(g.totalJustifies), teinte: 'text-royal-600' },
    ];
  });

  readonly teinteTaux = computed(() => this.teinteDe(this.globales().tauxAssiduite));

  constructor() {
    this.charger();
  }

  arrondi(valeur: number): number {
    return Math.round(valeur);
  }

  teinteDe(taux: number): string {
    if (taux >= 75) return 'bg-succes';
    if (taux >= 50) return 'bg-alerte';
    return 'bg-danger';
  }

  changerPeriode(jours: number): void {
    this.joursTendance.set(jours);
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');

    const fin = new Date();
    const debut = new Date();
    debut.setDate(debut.getDate() - (this.joursTendance() - 1));

    forkJoin({
      globales: this.service.globales(),
      classes: this.classeService.lister().pipe(catchError(() => of([] as Classe[]))),
      tendance: this.service
        .tendance(this.enCle(debut), this.enCle(fin))
        .pipe(catchError(() => of({} as Record<string, number>))),
    }).subscribe({
      next: ({ globales, classes, tendance }) => {
        this.globales.set(globales ?? GLOBALES_VIDES);
        this.construireTendance(tendance ?? {}, debut, this.joursTendance());
        this.chargerClasses(classes ?? []);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  /**
   * Charge les indicateurs de chaque classe.
   *
   * Une classe en échec n'annule pas les autres : le tableau se remplit avec ce qui
   * répond, plutôt que de tout perdre pour une classe supprimée entre-temps.
   */
  private chargerClasses(classes: Classe[]): void {
    if (classes.length === 0) {
      this.parClasse.set([]);
      return;
    }
    forkJoin(
      classes.map((classe) =>
        this.service.parClasse(classe.id).pipe(catchError(() => of(null))),
      ),
    ).subscribe((resultats) => {
      this.parClasse.set(
        resultats
          .filter((r): r is StatistiquesClasse => r !== null)
          .sort((a, b) => b.tauxAssiduite - a.tauxAssiduite),
      );
    });
  }

  /** Reconstruit la série jour par jour, y compris les jours absents de la réponse. */
  private construireTendance(
    brut: Record<string, number>,
    debut: Date,
    jours: number,
  ): void {
    const points: PointTendance[] = [];
    let maximum = 0;

    for (let i = 0; i < jours; i++) {
      const date = new Date(debut);
      date.setDate(date.getDate() + i);
      const jour = this.enCle(date);
      const valeur = brut[jour] ?? 0;
      maximum = Math.max(maximum, valeur);
      points.push({
        jour,
        libelle: `${date.getDate()} ${MOIS[date.getMonth()]}`,
        valeur,
        hauteur: 0,
      });
    }

    // Hauteur relative au maximum, calculée une fois la série complète connue.
    this.tendance.set(
      points.map((p) => ({ ...p, hauteur: maximum === 0 ? 0 : (p.valeur / maximum) * 100 })),
    );
  }

  private enCle(date: Date): string {
    return [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0'),
    ].join('-');
  }
}
