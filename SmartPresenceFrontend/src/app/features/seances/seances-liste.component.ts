import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of } from 'rxjs';
import { messageErreur } from '../../core/api';
import {
  Matiere,
  Seance,
  StatutSeance,
  creneauDe,
  jourDe,
  libelleStatutSeance,
  teinteStatutSeance,
} from '../../core/models/academique.model';
import { Salle } from '../../core/models/appareil.model';
import { Classe } from '../../core/models/classe.model';
import { Personnel } from '../../core/models/personnel.model';
import { AcademiqueService } from '../../core/services/academique.service';
import { ClasseService } from '../../core/services/classe.service';
import { PersonnelService } from '../../core/services/personnel.service';
import { SalleService } from '../../core/services/salle.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';
import { SeanceDialogComponent } from './seance-dialog.component';

/** Séances d'une même journée, pour l'affichage groupé. */
interface Journee {
  cle: string;
  libelle: string;
  seances: Seance[];
}

const JOURS = ['dimanche', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi'];
const MOIS = [
  'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
  'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
];

/**
 * Emploi du temps.
 *
 * <p>C'est le pivot du volet étudiant : sans séance, un passage devant un lecteur n'a
 * rien à quoi se rattacher. L'écran affiche une semaine par défaut, groupée par jour,
 * parce que c'est l'unité dans laquelle un emploi du temps se pense et se corrige.</p>
 */
@Component({
  selector: 'sp-seances-liste',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent, SeanceDialogComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Emploi du temps</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Séances planifiées. Chaque relevé de présence étudiant se rattache à l'une d'elles.
          </p>
        </div>
        <div class="flex shrink-0 items-center gap-2.5">
          <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
            <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
            Actualiser
          </button>
          <button type="button" class="btn btn-primaire" (click)="ouvrirCreation()">
            <sp-icone nom="plus" [taille]="16" [epaisseur]="1.8" />
            Planifier une séance
          </button>
        </div>
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

      <div class="carte flex flex-wrap items-end gap-2.5 px-4 py-3">
        <label class="flex flex-col gap-1">
          <span class="text-[10.5px] font-semibold text-ink-muted">Du</span>
          <span class="champ !h-9">
            <input
              type="date"
              class="saisie num"
              [ngModel]="debut()"
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
              [ngModel]="fin()"
              (ngModelChange)="changerFin($event)"
              aria-label="Fin de période"
            />
          </span>
        </label>

        <div class="flex items-center gap-1.5 pb-px">
          <button type="button" class="btn btn-secondaire !h-9 !px-3" (click)="decalerSemaine(-1)">
            <sp-icone nom="chevron-gauche" [taille]="14" [epaisseur]="2" />
          </button>
          <button type="button" class="btn btn-secondaire !h-9 !px-3" (click)="semaineCourante()">
            Cette semaine
          </button>
          <button type="button" class="btn btn-secondaire !h-9 !px-3" (click)="decalerSemaine(1)">
            <sp-icone nom="chevron-droit" [taille]="14" [epaisseur]="2" />
          </button>
        </div>

        <label class="flex min-w-[150px] flex-1 flex-col gap-1">
          <span class="text-[10.5px] font-semibold text-ink-muted">Classe</span>
          <span class="champ !h-9">
            <select
              class="saisie bg-transparent"
              [ngModel]="filtreClasse()"
              (ngModelChange)="appliquerFiltreClasse($event)"
              aria-label="Filtrer par classe"
            >
              <option [ngValue]="null">Toutes les classes</option>
              @for (classe of classes(); track classe.id) {
                <option [ngValue]="classe.id">{{ classe.code }}</option>
              }
            </select>
          </span>
        </label>

        <label class="flex min-w-[170px] flex-1 flex-col gap-1">
          <span class="text-[10.5px] font-semibold text-ink-muted">Enseignant</span>
          <span class="champ !h-9">
            <select
              class="saisie bg-transparent"
              [ngModel]="filtreEnseignant()"
              (ngModelChange)="appliquerFiltreEnseignant($event)"
              aria-label="Filtrer par enseignant"
            >
              <option [ngValue]="null">Tous les enseignants</option>
              @for (agent of enseignants(); track agent.id) {
                <option [ngValue]="agent.id">{{ agent.prenom }} {{ agent.nom }}</option>
              }
            </select>
          </span>
        </label>
      </div>

      <section class="flex flex-col gap-3">
        @if (chargement()) {
          <div class="carte"><sp-etat titre="Chargement de l'emploi du temps…" icone="horloge" /></div>
        } @else if (erreur()) {
          <div class="carte">
            <sp-etat
              variante="erreur"
              icone="alerte"
              titre="Impossible de charger l'emploi du temps"
              [detail]="erreur()"
              actionLibelle="Réessayer"
              (action)="charger()"
            />
          </div>
        } @else if (journees().length === 0) {
          <div class="carte">
            <sp-etat
              icone="calendrier"
              titre="Aucune séance sur cette période"
              [detail]="messageVide()"
              actionLibelle="Planifier une séance"
              (action)="ouvrirCreation()"
            />
          </div>
        } @else {
          @for (journee of journees(); track journee.cle) {
            <div class="carte overflow-hidden">
              <header
                class="flex items-center justify-between gap-3 border-b border-line-soft bg-[#FCFDFE] px-[18px] py-2.5"
              >
                <span class="text-[13px] font-semibold text-ink first-letter:uppercase">
                  {{ journee.libelle }}
                </span>
                <span class="text-[11.5px] text-ink-faint">
                  {{ journee.seances.length }} séance(s)
                </span>
              </header>

              @for (seance of journee.seances; track seance.id) {
                <div
                  class="flex items-center gap-3 border-b border-line-soft px-[18px] py-3 last:border-b-0 hover:bg-[#FBFCFE]"
                >
                  <span class="num w-[104px] shrink-0 text-[12.5px] text-ink">
                    {{ creneau(seance) }}
                  </span>
                  <span class="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span class="truncate text-[13.5px] font-medium text-ink">
                      {{ seance.matiereLibelle }}
                    </span>
                    <span class="truncate text-[11.5px] text-ink-faint">
                      {{ seance.classeCode }} · {{ seance.enseignantNom }}
                      @if (seance.salleLibelle) {
                        · {{ seance.salleLibelle }}
                      } @else {
                        · <span class="text-alerte">sans salle</span>
                      }
                    </span>
                  </span>

                  <span class="badge shrink-0" [class]="teinte(seance.statut)">
                    {{ libelle(seance.statut) }}
                  </span>

                  <div class="flex shrink-0 items-center gap-2">
                    <select
                      class="champ !h-[30px] !min-w-0 !px-2 text-[11.5px] text-ink"
                      [ngModel]="seance.statut"
                      (ngModelChange)="changerStatut(seance, $event)"
                      aria-label="Changer le statut"
                    >
                      <option value="PLANIFIEE">Planifiée</option>
                      <option value="EN_COURS">En cours</option>
                      <option value="TERMINEE">Terminée</option>
                      <option value="ANNULEE">Annulée</option>
                    </select>
                    <button
                      type="button"
                      class="grid size-[30px] place-items-center rounded-[7px] border border-line bg-white text-ink-muted transition-colors hover:bg-canvas"
                      (click)="ouvrirModification(seance)"
                      aria-label="Modifier"
                    >
                      <sp-icone nom="crayon" [taille]="15" [epaisseur]="1.7" />
                    </button>
                    <button
                      type="button"
                      class="grid size-[30px] place-items-center rounded-[7px] border border-line bg-white text-ink-muted transition-colors hover:border-danger-line hover:bg-danger-bg hover:text-danger disabled:opacity-50"
                      (click)="supprimer(seance)"
                      [disabled]="suppressionEnCours() === seance.id"
                      aria-label="Supprimer"
                    >
                      <sp-icone nom="fermer" [taille]="15" [epaisseur]="1.9" />
                    </button>
                  </div>
                </div>
              }
            </div>
          }
        }
      </section>
    </div>

    @if (dialogueOuvert()) {
      <sp-seance-dialog
        [seance]="seanceEnEdition()"
        [classes]="classes()"
        [matieres]="matieres()"
        [enseignants]="enseignants()"
        [salles]="salles()"
        [jourParDefaut]="debut()"
        (fermer)="fermerDialogue()"
        (enregistre)="apresEnregistrement()"
      />
    }
  `,
})
export class SeancesListeComponent {
  private readonly service = inject(AcademiqueService);
  private readonly classeService = inject(ClasseService);
  private readonly personnelService = inject(PersonnelService);
  private readonly salleService = inject(SalleService);

  readonly seances = signal<Seance[]>([]);
  readonly classes = signal<Classe[]>([]);
  readonly matieres = signal<Matiere[]>([]);
  readonly enseignants = signal<Personnel[]>([]);
  readonly salles = signal<Salle[]>([]);

  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly erreurAction = signal('');
  readonly dialogueOuvert = signal(false);
  readonly seanceEnEdition = signal<Seance | null>(null);
  readonly suppressionEnCours = signal<string | null>(null);

  readonly debut = signal(this.lundiDeLaSemaine());
  readonly fin = signal(this.ajouterJours(this.lundiDeLaSemaine(), 6));
  readonly filtreClasse = signal<number | null>(null);
  readonly filtreEnseignant = signal<string | null>(null);

  readonly indicateurs = computed(() => {
    const liste = this.seances();
    const annulees = liste.filter((s) => s.statut === 'ANNULEE').length;
    const sansSalle = liste.filter((s) => !s.salleId && s.statut !== 'ANNULEE').length;
    return [
      {
        libelle: 'Séances',
        valeur: liste.length,
        detail: 'sur la période',
        icone: 'calendrier' as const,
        teinte: 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Classes concernées',
        valeur: new Set(liste.map((s) => s.classeId)).size,
        detail: 'distinctes',
        icone: 'classes' as const,
        teinte: 'bg-royal-50 text-royal-600',
      },
      {
        libelle: 'Annulées',
        valeur: annulees,
        detail: annulees > 0 ? 'sur la période' : '—',
        icone: 'moins-cercle' as const,
        teinte: annulees > 0 ? 'bg-danger-bg text-danger' : 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Sans salle',
        valeur: sansSalle,
        detail: sansSalle > 0 ? 'non relevables' : 'toutes situées',
        icone: 'alerte' as const,
        teinte: sansSalle > 0 ? 'bg-alerte-bg text-alerte' : 'bg-line-faint text-[#4A5470]',
      },
    ];
  });

  /** Séances groupées par jour ; le serveur les renvoie déjà triées par début. */
  readonly journees = computed<Journee[]>(() => {
    const groupes = new Map<string, Seance[]>();
    for (const seance of this.seances()) {
      const cle = jourDe(seance.debut);
      const existant = groupes.get(cle);
      if (existant) existant.push(seance);
      else groupes.set(cle, [seance]);
    }
    return [...groupes.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([cle, seances]) => ({ cle, libelle: this.libelleJour(cle), seances }));
  });

  readonly messageVide = computed(() =>
    this.filtreClasse() || this.filtreEnseignant()
      ? 'Aucune séance ne correspond aux filtres. Élargissez la période ou retirez un filtre.'
      : 'Planifiez une séance : sans elle, aucun passage devant un lecteur ne peut être rattaché à un cours.',
  );

  constructor() {
    this.chargerReferentiels();
    this.charger();
  }

  creneau(seance: Seance): string {
    return creneauDe(seance);
  }

  libelle(statut: StatutSeance): string {
    return libelleStatutSeance(statut);
  }

  teinte(statut: StatutSeance): string {
    return teinteStatutSeance(statut);
  }

  /**
   * Charge les référentiels une seule fois.
   *
   * Séparé du chargement des séances, qui se rejoue à chaque filtre : recharger les
   * classes et les salles à chaque changement de semaine serait du gaspillage.
   */
  private chargerReferentiels(): void {
    forkJoin({
      classes: this.classeService.lister().pipe(catchError(() => of([] as Classe[]))),
      matieres: this.service.listerMatieres().pipe(catchError(() => of([] as Matiere[]))),
      enseignants: this.personnelService
        .lister({ type: 'ENSEIGNANT' })
        .pipe(catchError(() => of([] as Personnel[]))),
      salles: this.salleService.lister().pipe(catchError(() => of([] as Salle[]))),
    }).subscribe(({ classes, matieres, enseignants, salles }) => {
      this.classes.set(classes ?? []);
      this.matieres.set(matieres ?? []);
      this.enseignants.set(
        (enseignants ?? []).sort((a, b) => a.nom.localeCompare(b.nom, 'fr')),
      );
      this.salles.set(salles ?? []);
    });
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.service
      .listerSeances({
        debut: this.debut(),
        fin: this.fin(),
        classeId: this.filtreClasse(),
        enseignantId: this.filtreEnseignant(),
      })
      .subscribe({
        next: (seances) => {
          this.seances.set(seances ?? []);
          this.chargement.set(false);
        },
        error: (erreur: unknown) => {
          this.erreur.set(messageErreur(erreur));
          this.chargement.set(false);
        },
      });
  }

  changerDebut(valeur: string): void {
    this.debut.set(valeur);
    // Une période inversée est refusée par le serveur : on recale la fin plutôt
    // que d'envoyer une requête vouée à échouer.
    if (this.fin() < valeur) this.fin.set(valeur);
    this.charger();
  }

  changerFin(valeur: string): void {
    this.fin.set(valeur);
    if (valeur < this.debut()) this.debut.set(valeur);
    this.charger();
  }

  decalerSemaine(sens: number): void {
    this.debut.set(this.ajouterJours(this.debut(), 7 * sens));
    this.fin.set(this.ajouterJours(this.fin(), 7 * sens));
    this.charger();
  }

  semaineCourante(): void {
    const lundi = this.lundiDeLaSemaine();
    this.debut.set(lundi);
    this.fin.set(this.ajouterJours(lundi, 6));
    this.charger();
  }

  appliquerFiltreClasse(valeur: number | null): void {
    this.filtreClasse.set(valeur);
    this.charger();
  }

  appliquerFiltreEnseignant(valeur: string | null): void {
    this.filtreEnseignant.set(valeur);
    this.charger();
  }

  ouvrirCreation(): void {
    this.seanceEnEdition.set(null);
    this.dialogueOuvert.set(true);
  }

  ouvrirModification(seance: Seance): void {
    this.seanceEnEdition.set(seance);
    this.dialogueOuvert.set(true);
  }

  fermerDialogue(): void {
    this.dialogueOuvert.set(false);
    this.seanceEnEdition.set(null);
  }

  apresEnregistrement(): void {
    this.fermerDialogue();
    this.charger();
  }

  changerStatut(seance: Seance, statut: StatutSeance): void {
    if (statut === seance.statut) return;
    this.erreurAction.set('');
    this.service.changerStatutSeance(seance.id, statut).subscribe({
      next: () => {
        this.seances.update((liste) =>
          liste.map((s) => (s.id === seance.id ? { ...s, statut } : s)),
        );
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        // Rejouer la liste remet le select sur la valeur réelle du serveur.
        this.charger();
      },
    });
  }

  supprimer(seance: Seance): void {
    if (this.suppressionEnCours()) return;
    // Le backend refuse la suppression d'une séance déjà relevée : son message
    // explique qu'il faut l'annuler pour préserver les présences.
    this.suppressionEnCours.set(seance.id);
    this.erreurAction.set('');
    this.service.supprimerSeance(seance.id).subscribe({
      next: () => {
        this.seances.update((liste) => liste.filter((s) => s.id !== seance.id));
        this.suppressionEnCours.set(null);
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.suppressionEnCours.set(null);
      },
    });
  }

  // ------------------------------------------------------------------

  /** `YYYY-MM-DD` du lundi de la semaine courante. */
  private lundiDeLaSemaine(): string {
    const aujourdhui = new Date();
    // getDay() vaut 0 le dimanche : on le ramène à 7 pour que lundi soit l'origine.
    const jour = aujourdhui.getDay() === 0 ? 7 : aujourdhui.getDay();
    aujourdhui.setDate(aujourdhui.getDate() - (jour - 1));
    return this.enCle(aujourdhui);
  }

  private ajouterJours(cle: string, jours: number): string {
    const date = new Date(`${cle}T12:00:00`);
    date.setDate(date.getDate() + jours);
    return this.enCle(date);
  }

  private enCle(date: Date): string {
    return [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0'),
    ].join('-');
  }

  private libelleJour(cle: string): string {
    // Midi plutôt que minuit : évite qu'un décalage horaire ne recule d'un jour.
    const date = new Date(`${cle}T12:00:00`);
    return `${JOURS[date.getDay()]} ${date.getDate()} ${MOIS[date.getMonth()]}`;
  }
}
