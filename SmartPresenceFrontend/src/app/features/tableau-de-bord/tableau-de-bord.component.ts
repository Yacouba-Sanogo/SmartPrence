import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Appareil, paraitInjoignable } from '../../core/models/appareil.model';
import { Personnel } from '../../core/models/personnel.model';
import {
  JourneePersonnel,
  calculerIndicateurs,
  dateDuJourIso,
  formaterDateLongue,
  formaterHeure,
} from '../../core/models/pointage.model';
import { AppareilService } from '../../core/services/appareil.service';
import { AuthService } from '../../core/services/auth.service';
import { PersonnelService } from '../../core/services/personnel.service';
import { PointageService } from '../../core/services/pointage.service';
import { StatistiquesGlobales, StatistiquesService } from '../../core/services/statistiques.service';
import { IconeComponent, NomIcone } from '../../shared/ui/icone.component';

interface Alerte {
  libelle: string;
  detail: string;
  lien: string;
  gravite: 'alerte' | 'danger';
}

/**
 * Tableau de bord — page d'atterrissage après connexion.
 *
 * <p>Accessible à <b>tout utilisateur connecté</b>, alors que les endpoints qui
 * l'alimentent sont restreints par rôle. Chaque bloc n'est donc demandé que si
 * l'utilisateur y a droit, et un refus n'interrompt pas les autres : un enseignant
 * obtient une page réduite et honnête plutôt qu'un mur d'erreurs.</p>
 *
 * <p>Le bloc « Ce qui demande votre attention » passe avant les compteurs : un tableau
 * de bord qui se contente d'afficher des totaux flatte sans servir. Ce qui compte, ce
 * sont les situations sur lesquelles agir — agents non enrôlés, lecteurs muets.</p>
 */
@Component({
  selector: 'sp-tableau-de-bord',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconeComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <!-- Accueil -->
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">{{ salutation() }}</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            {{ dateLisible() }}@if (regleHoraire(); as regle) { — {{ regle }} }
          </p>
        </div>
      </div>

      @if (chargement()) {
        <div class="carte flex items-center gap-3 px-4 py-8">
          <sp-icone nom="horloge" [taille]="18" class="text-ink-faint" />
          <span class="text-[13px] text-ink-subtle">Chargement du tableau de bord…</span>
        </div>
      } @else if (aucunBloc()) {
        <!--
          Aucun bloc accessible : plutôt qu'une page nue qui passerait pour une panne,
          on explique la situation. Les écrans du volet académique ne sont pas encore
          réalisés — le dire vaut mieux que de laisser l'utilisateur chercher.
        -->
        <div class="carte flex flex-col items-center gap-2.5 px-6 py-14 text-center">
          <span class="grid size-11 place-items-center rounded-xl bg-canvas text-ink-subtle">
            <sp-icone nom="tableau-de-bord" [taille]="20" />
          </span>
          <span class="text-[15px] font-semibold text-ink">Rien à afficher pour votre profil</span>
          <span class="max-w-md text-[13px] leading-relaxed text-ink-subtle">
            Les indicateurs de ce tableau de bord relèvent du suivi du personnel et du parc de
            lecteurs, hors de votre périmètre. Les écrans du volet académique ne sont pas encore
            disponibles.
          </span>
        </div>
      } @else {
        <!-- Ce qui demande une action -->
        @if (alertes().length > 0) {
          <section class="carte overflow-hidden">
            <header class="flex items-center gap-2.5 border-b border-line-soft px-4 py-3.5">
              <span class="grid size-7 place-items-center rounded-lg bg-alerte-bg text-alerte">
                <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" />
              </span>
              <h2 class="m-0 text-[14.5px] font-semibold text-ink">Ce qui demande votre attention</h2>
            </header>
            <ul class="m-0 flex list-none flex-col p-0">
              @for (alerte of alertes(); track alerte.libelle) {
                <li class="border-b border-[#F4F6FA] last:border-b-0">
                  <a
                    [routerLink]="alerte.lien"
                    class="flex items-center gap-3 px-4 py-3.5 transition-colors hover:bg-[#FBFCFE] hover:text-inherit"
                  >
                    <span
                      class="grid size-8 shrink-0 place-items-center rounded-lg"
                      [class]="
                        alerte.gravite === 'danger'
                          ? 'bg-danger-bg text-danger'
                          : 'bg-alerte-bg text-alerte'
                      "
                    >
                      <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" />
                    </span>
                    <span class="flex min-w-0 flex-1 flex-col gap-0.5">
                      <span class="text-[13.5px] font-semibold text-ink">{{ alerte.libelle }}</span>
                      <span class="text-xs text-ink-subtle">{{ alerte.detail }}</span>
                    </span>
                    <sp-icone
                      nom="chevron-droit"
                      [taille]="15"
                      [epaisseur]="1.9"
                      class="shrink-0 text-ink-faint"
                    />
                  </a>
                </li>
              }
            </ul>
          </section>
        } @else if (peutVoirPointage()) {
          <div
            class="flex items-center gap-3.5 rounded-[12px] border border-succes-line bg-succes-bg px-4 py-3.5"
          >
            <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-white/60 text-succes">
              <sp-icone nom="coche-cercle" [taille]="17" [epaisseur]="1.7" />
            </span>
            <span class="text-[13.5px] font-medium text-succes">
              Aucune anomalie détectée — parc et enrôlements à jour.
            </span>
          </div>
        }

        <!-- Pointage du personnel -->
        @if (peutVoirPointage()) {
          <section class="carte overflow-hidden">
            <header class="flex flex-wrap items-center gap-2.5 border-b border-line-soft px-4 py-3.5">
              <span class="grid size-7 place-items-center rounded-lg bg-royal-50 text-royal-600">
                <sp-icone nom="empreinte" [taille]="16" [epaisseur]="1.7" />
              </span>
              <h2 class="m-0 text-[14.5px] font-semibold text-ink">Pointage du personnel aujourd'hui</h2>
              <span class="flex-1"></span>
              <a routerLink="/pointage" class="text-[12.5px] font-medium">Voir le détail →</a>
            </header>

            @if (journees().length === 0) {
              <p class="m-0 px-4 py-8 text-center text-[13px] text-ink-subtle">
                Aucun agent actif dans le référentiel.
              </p>
            } @else {
              <div class="grid grid-cols-2 gap-px bg-line-soft sm:grid-cols-4">
                @for (bloc of blocsPointage(); track bloc.libelle) {
                  <div class="flex flex-col gap-1.5 bg-white px-4 py-4">
                    <span class="text-[11.5px] font-medium text-ink-muted">{{ bloc.libelle }}</span>
                    <div class="flex items-baseline gap-1.5">
                      <span
                        class="num text-[26px] font-semibold leading-none tracking-tight"
                        [class]="bloc.teinte"
                      >
                        {{ bloc.valeur }}
                      </span>
                      <span class="truncate text-[11.5px] text-ink-faint">{{ bloc.detail }}</span>
                    </div>
                  </div>
                }
              </div>
            }
          </section>
        }

        <div class="flex flex-col items-start gap-4 xl:flex-row">
          <!-- Parc de lecteurs -->
          @if (peutVoirAppareils()) {
            <section class="carte w-full min-w-0 flex-1 overflow-hidden">
              <header class="flex flex-wrap items-center gap-2.5 border-b border-line-soft px-4 py-3.5">
                <span class="grid size-7 place-items-center rounded-lg bg-line-faint text-[#4A5470]">
                  <sp-icone nom="appareils" [taille]="16" [epaisseur]="1.7" />
                </span>
                <h2 class="m-0 text-[14.5px] font-semibold text-ink">Parc de lecteurs</h2>
                <span class="flex-1"></span>
                <a routerLink="/appareils" class="text-[12.5px] font-medium">Gérer →</a>
              </header>

              @if (appareils().length === 0) {
                <p class="m-0 px-4 py-8 text-center text-[13px] text-ink-subtle">
                  Aucun lecteur déclaré.
                </p>
              } @else {
                <ul class="m-0 flex list-none flex-col p-0">
                  @for (appareil of appareilsRecents(); track appareil.id) {
                    <li class="flex items-center gap-3 border-b border-[#F4F6FA] px-4 py-3 last:border-b-0">
                      <span
                        class="size-2 shrink-0 rounded-full"
                        [class]="
                          paraitInjoignable(appareil)
                            ? 'bg-alerte'
                            : appareil.statut === 'ACTIF'
                              ? 'bg-succes'
                              : 'bg-ink-faint'
                        "
                      ></span>
                      <span class="flex min-w-0 flex-1 flex-col gap-0.5">
                        <span class="truncate text-[13px] font-medium text-ink">{{ appareil.nom }}</span>
                        <span class="truncate text-[11px] text-ink-faint">
                          {{ appareil.salleNom || 'Non affecté' }}
                        </span>
                      </span>
                      <span class="num shrink-0 text-[11.5px] text-ink-subtle">
                        {{ anciennete(appareil) }}
                      </span>
                    </li>
                  }
                </ul>
              }
            </section>
          }

          <!-- Volet académique -->
          @if (statistiques(); as stats) {
            <section class="carte w-full shrink-0 overflow-hidden xl:w-[360px]">
              <header class="flex items-center gap-2.5 border-b border-line-soft px-4 py-3.5">
                <span class="grid size-7 place-items-center rounded-lg bg-succes-bg text-succes">
                  <sp-icone nom="etudiants" [taille]="16" [epaisseur]="1.7" />
                </span>
                <h2 class="m-0 text-[14.5px] font-semibold text-ink">Volet académique</h2>
              </header>

              <dl class="m-0 flex flex-col">
                @for (ligne of lignesAcademiques(stats); track ligne.libelle) {
                  <div
                    class="flex items-baseline gap-3 border-b border-[#F4F6FA] px-4 py-3 last:border-b-0"
                  >
                    <dt class="min-w-0 flex-1 text-[13px] text-ink-muted">{{ ligne.libelle }}</dt>
                    <dd class="num m-0 text-[15px] font-semibold text-ink">{{ ligne.valeur }}</dd>
                  </div>
                }
              </dl>

              @if (stats.totalPresencesEnregistrees === 0) {
                <p class="m-0 bg-[#FCFDFE] px-4 py-3 text-[11.5px] leading-relaxed text-ink-subtle">
                  Aucune présence étudiante enregistrée : le taux d'assiduité restera à zéro tant que
                  les lecteurs de salle n'auront rien transmis.
                </p>
              }
            </section>
          }
        </div>

        <!-- Accès rapides -->
        <section class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          @for (acces of accesRapides(); track acces.lien) {
            <a
              [routerLink]="acces.lien"
              class="carte flex items-center gap-3 px-4 py-3.5 transition-colors hover:border-royal-200 hover:bg-[#FBFCFE] hover:text-inherit"
            >
              <span class="grid size-9 shrink-0 place-items-center rounded-lg bg-royal-50 text-royal-600">
                <sp-icone [nom]="acces.icone" [taille]="17" [epaisseur]="1.7" />
              </span>
              <span class="flex min-w-0 flex-col gap-0.5">
                <span class="truncate text-[13.5px] font-medium text-ink">{{ acces.libelle }}</span>
                <span class="truncate text-[11.5px] text-ink-faint">{{ acces.detail }}</span>
              </span>
            </a>
          }
        </section>
      }
    </div>
  `,
})
export class TableauDeBordComponent {
  private readonly auth = inject(AuthService);
  private readonly pointageService = inject(PointageService);
  private readonly personnelService = inject(PersonnelService);
  private readonly appareilService = inject(AppareilService);
  private readonly statistiquesService = inject(StatistiquesService);

  protected readonly paraitInjoignable = paraitInjoignable;

  readonly chargement = signal(true);
  readonly journees = signal<JourneePersonnel[]>([]);
  readonly agents = signal<Personnel[]>([]);
  readonly appareils = signal<Appareil[]>([]);
  readonly statistiques = signal<StatistiquesGlobales | null>(null);
  readonly seuilRetard = signal<{ ouverture: string; tolerance: number } | null>(null);

  readonly peutVoirPointage = computed(() => this.auth.aUnRole('ADMIN', 'RH', 'SUPERVISEUR'));
  readonly peutVoirPersonnel = computed(() => this.auth.aUnRole('ADMIN', 'RH'));
  readonly peutVoirAppareils = computed(() =>
    this.auth.aUnRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR'),
  );

  readonly dateLisible = computed(() => formaterDateLongue(dateDuJourIso()));

  /** Aucun bloc n'est accessible au profil courant — typiquement un enseignant. */
  readonly aucunBloc = computed(
    () =>
      !this.peutVoirPointage() &&
      !this.peutVoirPersonnel() &&
      !this.peutVoirAppareils() &&
      this.statistiques() === null,
  );

  readonly salutation = computed(() => {
    const prenom = this.auth.utilisateur()?.prenom?.trim();
    const heure = new Date().getHours();
    const moment = heure < 18 ? 'Bonjour' : 'Bonsoir';
    return prenom ? `${moment}, ${prenom}` : 'Tableau de bord';
  });

  readonly regleHoraire = computed(() => {
    const regle = this.seuilRetard();
    if (!regle) return null;
    return `prise de service à ${formaterHeure(regle.ouverture)}, tolérance ${regle.tolerance} min`;
  });

  readonly blocsPointage = computed(() => {
    const i = calculerIndicateurs(this.journees());
    return [
      { libelle: 'Présents', valeur: i.presents, detail: `${i.tauxPresence} %`, teinte: 'text-succes' },
      { libelle: 'En retard', valeur: i.retards, detail: 'arrivées tardives', teinte: 'text-alerte' },
      { libelle: 'Absents', valeur: i.absents, detail: 'aucun pointage', teinte: 'text-danger' },
      { libelle: 'Sur site', valeur: i.surSite, detail: 'sans sortie', teinte: 'text-royal-600' },
    ];
  });

  /** Lecteurs les moins frais en tête : ce sont ceux qui méritent un regard. */
  readonly appareilsRecents = computed(() =>
    [...this.appareils()]
      .sort((a, b) => {
        const cle = (x: Appareil) =>
          x.derniereSynchronisation ? new Date(x.derniereSynchronisation).getTime() : 0;
        return cle(a) - cle(b);
      })
      .slice(0, 5),
  );

  /**
   * Situations appelant une action, ordonnées par gravité.
   *
   * <p>Chaque alerte mène à l'écran qui permet de la traiter — un tableau de bord qui
   * signale sans donner le moyen d'agir ne fait que déplacer le problème.</p>
   */
  readonly alertes = computed<Alerte[]>(() => {
    const liste: Alerte[] = [];

    const nonEnroles = this.agents().filter((a) => !a.enrole && a.actif).length;
    if (nonEnroles > 0) {
      liste.push({
        libelle: `${nonEnroles} agent(s) non enrôlé(s)`,
        detail: "Sans empreinte associée, leurs arrivées ne peuvent pas être relevées.",
        lien: '/personnel',
        gravite: 'danger',
      });
    }

    const muets = this.appareils().filter((a) => paraitInjoignable(a)).length;
    if (muets > 0) {
      liste.push({
        libelle: `${muets} lecteur(s) sans contact récent`,
        detail: 'Ils relèvent peut-être encore localement, mais ne transmettent plus.',
        lien: '/appareils',
        gravite: 'alerte',
      });
    }

    const absents = this.journees().filter((j) => j.statut === 'ABSENT').length;
    if (absents > 0) {
      liste.push({
        libelle: `${absents} agent(s) sans pointage aujourd'hui`,
        detail: 'À vérifier avant clôture de la journée.',
        lien: '/pointage',
        gravite: 'alerte',
      });
    }

    return liste;
  });

  readonly accesRapides = computed(() => {
    const liste: { libelle: string; detail: string; lien: string; icone: NomIcone }[] = [];
    if (this.peutVoirPointage()) {
      liste.push({
        libelle: 'Pointage du jour',
        detail: 'Arrivées et départs',
        lien: '/pointage',
        icone: 'empreinte',
      });
    }
    if (this.peutVoirPersonnel()) {
      liste.push({
        libelle: 'Personnel',
        detail: 'Référentiel et enrôlement',
        lien: '/personnel',
        icone: 'personnel',
      });
    }
    if (this.peutVoirAppareils()) {
      liste.push({
        libelle: 'Appareils ESP32',
        detail: 'Parc et synchronisations',
        lien: '/appareils',
        icone: 'appareils',
      });
      liste.push({ libelle: 'Salles', detail: 'Inventaire', lien: '/salles', icone: 'salles' });
    }
    return liste;
  });

  constructor() {
    this.charger();
  }

  /**
   * Charge les blocs auxquels l'utilisateur a droit.
   *
   * <p>Chaque appel est neutralisé par {@code catchError} : un refus ou une panne sur
   * un bloc laisse les autres s'afficher. Le tableau de bord est une vue de synthèse,
   * pas une transaction — il vaut mieux qu'il s'affiche partiellement que pas du tout.</p>
   */
  private charger(): void {
    const jour = dateDuJourIso();
    const vide = <T>(valeur: T) => of(valeur);

    forkJoin({
      journees: this.peutVoirPointage()
        ? this.pointageService.journee(jour).pipe(catchError(() => vide([] as JourneePersonnel[])))
        : vide([] as JourneePersonnel[]),
      parametres: this.peutVoirPointage()
        ? this.pointageService.parametres().pipe(catchError(() => vide(null)))
        : vide(null),
      agents: this.peutVoirPersonnel()
        ? this.personnelService.lister().pipe(catchError(() => vide([] as Personnel[])))
        : vide([] as Personnel[]),
      appareils: this.peutVoirAppareils()
        ? this.appareilService.lister().pipe(catchError(() => vide([] as Appareil[])))
        : vide([] as Appareil[]),
      statistiques: this.auth.aUnRole('ADMIN', 'SUPERVISEUR')
        ? this.statistiquesService.globales().pipe(catchError(() => vide(null)))
        : vide(null),
    }).subscribe(({ journees, parametres, agents, appareils, statistiques }) => {
      this.journees.set(journees ?? []);
      this.agents.set(agents ?? []);
      this.appareils.set(appareils ?? []);
      this.statistiques.set(statistiques);
      if (parametres) {
        this.seuilRetard.set({
          ouverture: parametres.heureOuverture,
          tolerance: parametres.seuilRetardMinutes,
        });
      }
      this.chargement.set(false);
    });
  }

  protected anciennete(appareil: Appareil): string {
    const dernier = appareil.derniereSynchronisation;
    if (!dernier) return 'jamais';
    const minutes = Math.floor((Date.now() - new Date(dernier).getTime()) / 60000);
    if (minutes < 1) return "à l'instant";
    if (minutes < 60) return `${minutes} min`;
    const heures = Math.floor(minutes / 60);
    return heures < 24 ? `${heures} h` : `${Math.floor(heures / 24)} j`;
  }

  protected lignesAcademiques(stats: StatistiquesGlobales) {
    return [
      { libelle: 'Étudiants inscrits', valeur: stats.totalEtudiants },
      { libelle: 'Classes', valeur: stats.totalClasses },
      { libelle: 'Présences enregistrées', valeur: stats.totalPresencesEnregistrees },
      { libelle: "Taux d'assiduité", valeur: `${Math.round(stats.tauxAssiduite)} %` },
    ];
  }
}
