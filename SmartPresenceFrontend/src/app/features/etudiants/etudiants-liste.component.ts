import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { messageErreur } from '../../core/api';
import { Classe } from '../../core/models/classe.model';
import { CompteEtudiant, Etudiant, initialesEtudiant, nomCompletEtudiant } from '../../core/models/etudiant.model';
import { EtudiantService } from '../../core/services/etudiant.service';
import {
  EnrolementDialogComponent,
  SujetEnrolement,
} from '../../shared/ui/enrolement-dialog.component';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';
import { CompteDialogComponent } from './compte-dialog.component';
import { EtudiantDialogComponent } from './etudiant-dialog.component';

type FiltreEnrolement = 'TOUS' | 'ENROLES' | 'NON_ENROLES';
type FiltreAcces = 'TOUS' | 'AVEC' | 'SANS';

/**
 * Référentiel des étudiants.
 *
 * <p>Deux colonnes portent l'essentiel : <b>Empreinte</b> décide si l'étudiant peut être
 * relevé par un lecteur, <b>Accès mobile</b> s'il peut consulter ses propres relevés.
 * Les deux sont indépendantes — un étudiant peut être enrôlé sans compte, et
 * réciproquement.</p>
 */
@Component({
  selector: 'sp-etudiants-liste',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    IconeComponent,
    EtatComponent,
    EtudiantDialogComponent,
    CompteDialogComponent,
    EnrolementDialogComponent,
  ],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Étudiants</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Référentiel de scolarité, enrôlement biométrique et accès à l'application mobile.
          </p>
        </div>
        <div class="flex shrink-0 items-center gap-2.5">
          <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
            <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
            Actualiser
          </button>
          <button
            type="button"
            class="btn btn-primaire"
            (click)="ouvrirCreation()"
            [disabled]="classes().length === 0"
            [title]="classes().length === 0 ? 'Créez d\\'abord une classe' : ''"
          >
            <sp-icone nom="plus" [taille]="16" [epaisseur]="1.8" />
            Inscrire un étudiant
          </button>
        </div>
      </div>

      @if (classes().length === 0 && !chargement()) {
        <div class="flex items-center gap-3.5 rounded-[12px] border border-alerte-line bg-[#FFF9EC] px-4 py-3.5">
          <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-alerte-bg text-alerte">
            <sp-icone nom="alerte" [taille]="17" [epaisseur]="1.7" />
          </span>
          <span class="flex min-w-0 flex-1 flex-col gap-0.5">
            <span class="text-[13.5px] font-semibold text-[#6E4806]">Aucune classe enregistrée</span>
            <span class="text-xs text-[#8A6420]">
              Un étudiant doit être rattaché à une classe. Créez-en une avant d'inscrire.
            </span>
          </span>
        </div>
      } @else if (nonEnroles() > 0 && !chargement()) {
        <div class="flex items-center gap-3.5 rounded-[12px] border border-alerte-line bg-[#FFF9EC] px-4 py-3.5">
          <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-alerte-bg text-alerte">
            <sp-icone nom="alerte" [taille]="17" [epaisseur]="1.7" />
          </span>
          <span class="flex min-w-0 flex-1 flex-col gap-0.5">
            <span class="text-[13.5px] font-semibold text-[#6E4806]">
              {{ nonEnroles() }} étudiant(s) non enrôlé(s)
            </span>
            <span class="text-xs text-[#8A6420]">
              Sans empreinte associée, le lecteur de salle ne peut pas relever leur présence.
            </span>
          </span>
          <button
            type="button"
            class="btn shrink-0 border border-[#E0C489] bg-white !text-[12.5px] font-semibold text-[#6E4806]"
            (click)="filtreEnrolement.set('NON_ENROLES')"
          >
            Voir les étudiants concernés
          </button>
        </div>
      }

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

      <div class="carte flex flex-wrap items-center gap-2.5 px-4 py-3">
        <label class="champ min-w-[210px] flex-1 bg-[#FBFCFE]">
          <sp-icone nom="recherche" [taille]="16" [epaisseur]="1.7" class="text-ink-faint" />
          <input
            type="search"
            class="saisie"
            placeholder="Rechercher un étudiant, un matricule…"
            [ngModel]="recherche()"
            (ngModelChange)="recherche.set($event)"
            aria-label="Rechercher un étudiant"
          />
        </label>

        <select
          class="champ text-ink"
          [ngModel]="filtreClasse()"
          (ngModelChange)="filtreClasse.set($event)"
          aria-label="Filtrer par classe"
        >
          <option [ngValue]="null">Toutes les classes</option>
          @for (classe of classes(); track classe.id) {
            <option [ngValue]="classe.id">{{ classe.code }}</option>
          }
        </select>

        <select
          class="champ text-ink"
          [ngModel]="filtreEnrolement()"
          (ngModelChange)="filtreEnrolement.set($event)"
          aria-label="Filtrer par enrôlement"
        >
          <option value="TOUS">Empreinte : toutes</option>
          <option value="ENROLES">Enrôlés</option>
          <option value="NON_ENROLES">Non enrôlés</option>
        </select>

        <select
          class="champ text-ink"
          [ngModel]="filtreAcces()"
          (ngModelChange)="filtreAcces.set($event)"
          aria-label="Filtrer par accès mobile"
        >
          <option value="TOUS">Accès : tous</option>
          <option value="AVEC">Avec accès</option>
          <option value="SANS">Sans accès</option>
        </select>
      </div>

      <section class="carte overflow-hidden">
        @if (chargement()) {
          <sp-etat titre="Chargement du référentiel…" icone="horloge" />
        } @else if (erreur()) {
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger les étudiants"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        } @else if (etudiants().length === 0) {
          <sp-etat
            icone="etudiants"
            titre="Aucun étudiant inscrit"
            detail="Inscrivez un étudiant pour pouvoir l'enrôler et lui ouvrir un accès mobile."
          />
        } @else if (etudiantsFiltres().length === 0) {
          <sp-etat
            icone="recherche"
            titre="Aucun étudiant ne correspond"
            detail="Ajustez la recherche ou les filtres pour élargir le résultat."
          />
        } @else {
          <div class="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th class="th !pl-[18px]">Étudiant</th>
                  <th class="th">Classe</th>
                  <th class="th">Empreinte</th>
                  <th class="th">Accès mobile</th>
                  <th class="th">État</th>
                  <th class="th !pr-[18px] text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (etudiant of etudiantsFiltres(); track etudiant.id) {
                  <tr class="hover:bg-[#FBFCFE]">
                    <td class="td !pl-[18px]">
                      <div class="flex items-center gap-3">
                        <span
                          class="grid size-8 shrink-0 place-items-center rounded-full bg-line-faint text-[11.5px] font-semibold text-[#4A5470]"
                        >
                          {{ initialesEtudiant(etudiant) }}
                        </span>
                        <span class="flex min-w-0 flex-col gap-0.5">
                          <span class="truncate text-[13.5px] font-medium text-ink">
                            {{ nomCompletEtudiant(etudiant) }}
                          </span>
                          <span class="num text-[11px] text-ink-faint">{{ etudiant.matricule }}</span>
                        </span>
                      </div>
                    </td>
                    <td class="td">
                      <span class="flex flex-col gap-0.5">
                        <span class="text-[13px] text-[#364057]">{{ etudiant.classeCode || '—' }}</span>
                        @if (etudiant.promotionLibelle) {
                          <span class="text-[10.5px] text-ink-faint">
                            {{ etudiant.promotionLibelle }}
                          </span>
                        }
                      </span>
                    </td>
                    <td class="td">
                      @if (etudiant.enrole) {
                        <span class="flex flex-col gap-0.5">
                          <span class="badge self-start bg-succes-bg text-succes">
                            <sp-icone nom="coche" [taille]="12" [epaisseur]="2.2" />
                            Enrôlé
                          </span>
                          <span class="num pl-0.5 text-[10.5px] text-ink-faint">
                            {{ etudiant.biometricId }}
                          </span>
                        </span>
                      } @else {
                        <span class="badge border border-alerte-line bg-[#FFFBF2] font-medium text-alerte">
                          <sp-icone nom="info" [taille]="12" [epaisseur]="2" />
                          Non enrôlé
                        </span>
                      }
                    </td>
                    <td class="td">
                      @if (etudiant.compteOuvert) {
                        <span class="badge bg-royal-50 text-royal-600">
                          <sp-icone nom="coche" [taille]="12" [epaisseur]="2.2" />
                          Ouvert
                        </span>
                      } @else {
                        <span class="text-[12.5px] text-ink-faint">Aucun</span>
                      }
                    </td>
                    <td class="td">
                      <span
                        class="flex items-center gap-1.5 text-[12.5px]"
                        [class]="etudiant.actif ? 'text-succes' : 'text-ink-faint'"
                      >
                        <span
                          class="size-1.5 rounded-full"
                          [class]="etudiant.actif ? 'bg-succes' : 'bg-ink-faint'"
                        ></span>
                        {{ etudiant.actif ? 'Actif' : 'Inactif' }}
                      </span>
                    </td>
                    <td class="td !pr-[18px]">
                      <div class="flex items-center justify-end gap-2">
                        @if (!etudiant.enrole && etudiant.actif) {
                          <button
                            type="button"
                            class="btn btn-secondaire !h-[30px] !px-3 !text-xs"
                            (click)="ouvrirEnrolement(etudiant)"
                          >
                            <sp-icone nom="empreinte" [taille]="13" [epaisseur]="1.7" />
                            Enrôler
                          </button>
                        }
                        @if (!etudiant.compteOuvert && etudiant.actif) {
                          <button
                            type="button"
                            class="btn btn-primaire !h-[30px] !px-3 !text-xs"
                            (click)="ouvrirAcces(etudiant)"
                            [disabled]="actionEnCours() === etudiant.id"
                          >
                            Ouvrir l'accès
                          </button>
                        } @else if (etudiant.compteOuvert) {
                          <button
                            type="button"
                            class="btn btn-secondaire !h-[30px] !px-3 !text-xs"
                            (click)="fermerAcces(etudiant)"
                            [disabled]="actionEnCours() === etudiant.id"
                          >
                            Fermer l'accès
                          </button>
                        }
                        <button
                          type="button"
                          class="grid size-[30px] place-items-center rounded-[7px] border border-line bg-white text-ink-muted transition-colors hover:bg-canvas"
                          (click)="ouvrirModification(etudiant)"
                          aria-label="Modifier"
                        >
                          <sp-icone nom="crayon" [taille]="15" [epaisseur]="1.7" />
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <footer
            class="flex flex-wrap items-center gap-2 border-t border-line-soft bg-[#FCFDFE] px-[18px] py-3 text-xs text-ink-subtle"
          >
            <span>{{ etudiantsFiltres().length }} étudiant(s) sur {{ etudiants().length }} —</span>
            <span class="font-medium text-succes">{{ enroles() }} enrôlés</span>
            <span>·</span>
            <span class="font-medium text-royal-600">{{ avecAcces() }} avec accès mobile</span>
          </footer>
        }
      </section>
    </div>

    @if (dialogueOuvert()) {
      <sp-etudiant-dialog
        [etudiant]="etudiantEnEdition()"
        [classes]="classes()"
        (fermer)="fermerDialogue()"
        (enregistre)="apresEnregistrement()"
      />
    }

    @if (etudiantAEnroler(); as cible) {
      <sp-enrolement-dialog
        [sujet]="sujetEnrolement(cible)"
        prefixe="ETU-"
        sousTitre="Associer une empreinte à un étudiant du référentiel"
        [envoi]="enrolementEnCours()"
        [erreur]="erreurEnrolement()"
        (fermer)="fermerEnrolement()"
        (confirmer)="enroler($event)"
      />
    }

    @if (compteCree(); as compte) {
      <sp-compte-dialog [compte]="compte" (fermer)="compteCree.set(null)" />
    }
  `,
})
export class EtudiantsListeComponent {
  private readonly service = inject(EtudiantService);

  protected readonly nomCompletEtudiant = nomCompletEtudiant;
  protected readonly initialesEtudiant = initialesEtudiant;

  readonly etudiants = signal<Etudiant[]>([]);
  readonly classes = signal<Classe[]>([]);
  readonly chargement = signal(false);

  /** Échec de chargement : l'écran entier bascule en erreur. */
  readonly erreur = signal('');

  /** Échec d'une action ponctuelle : la liste reste affichée sous un bandeau. */
  readonly erreurAction = signal('');

  readonly dialogueOuvert = signal(false);
  readonly etudiantEnEdition = signal<Etudiant | null>(null);
  readonly etudiantAEnroler = signal<Etudiant | null>(null);
  readonly enrolementEnCours = signal(false);
  readonly erreurEnrolement = signal('');
  readonly compteCree = signal<CompteEtudiant | null>(null);
  readonly actionEnCours = signal<string | null>(null);

  readonly recherche = signal('');
  readonly filtreClasse = signal<number | null>(null);
  readonly filtreEnrolement = signal<FiltreEnrolement>('TOUS');
  readonly filtreAcces = signal<FiltreAcces>('TOUS');

  readonly enroles = computed(() => this.etudiants().filter((e) => e.enrole).length);
  readonly nonEnroles = computed(
    () => this.etudiants().filter((e) => !e.enrole && e.actif).length,
  );
  readonly avecAcces = computed(() => this.etudiants().filter((e) => e.compteOuvert).length);

  readonly indicateurs = computed(() => {
    const total = this.etudiants().length;
    return [
      {
        libelle: 'Inscrits',
        valeur: total,
        detail: 'au référentiel',
        icone: 'etudiants' as const,
        teinte: 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Enrôlés',
        valeur: this.enroles(),
        detail: total ? `${Math.round((this.enroles() / total) * 100)} %` : '—',
        icone: 'empreinte' as const,
        teinte: 'bg-succes-bg text-succes',
      },
      {
        libelle: 'En attente',
        valeur: this.nonEnroles(),
        detail: "d'enrôlement",
        icone: 'alerte' as const,
        teinte: 'bg-alerte-bg text-alerte',
      },
      {
        libelle: 'Accès mobile',
        valeur: this.avecAcces(),
        detail: 'comptes ouverts',
        icone: 'personnel' as const,
        teinte: 'bg-royal-50 text-royal-600',
      },
    ];
  });

  readonly etudiantsFiltres = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    const classe = this.filtreClasse();
    const enrolement = this.filtreEnrolement();
    const acces = this.filtreAcces();

    return this.etudiants().filter((etudiant) => {
      if (classe && etudiant.classeId !== classe) return false;
      if (enrolement === 'ENROLES' && !etudiant.enrole) return false;
      if (enrolement === 'NON_ENROLES' && etudiant.enrole) return false;
      if (acces === 'AVEC' && !etudiant.compteOuvert) return false;
      if (acces === 'SANS' && etudiant.compteOuvert) return false;
      if (!terme) return true;
      return `${etudiant.prenom} ${etudiant.nom} ${etudiant.matricule} ${etudiant.classeCode ?? ''}`
        .toLowerCase()
        .includes(terme);
    });
  });

  constructor() {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');

    forkJoin({
      etudiants: this.service.lister(),
      // Sans classe, on ne peut pas inscrire — mais la liste doit rester consultable.
      classes: this.service.listerClasses().pipe(catchError(() => of([] as Classe[]))),
    }).subscribe({
      next: ({ etudiants, classes }) => {
        this.etudiants.set(etudiants ?? []);
        this.classes.set(classes ?? []);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  ouvrirCreation(): void {
    this.etudiantEnEdition.set(null);
    this.dialogueOuvert.set(true);
  }

  ouvrirModification(etudiant: Etudiant): void {
    this.etudiantEnEdition.set(etudiant);
    this.dialogueOuvert.set(true);
  }

  fermerDialogue(): void {
    this.dialogueOuvert.set(false);
    this.etudiantEnEdition.set(null);
  }

  apresEnregistrement(): void {
    this.fermerDialogue();
    this.charger();
  }

  ouvrirEnrolement(etudiant: Etudiant): void {
    this.etudiantAEnroler.set(etudiant);
    this.erreurEnrolement.set('');
  }

  fermerEnrolement(): void {
    this.etudiantAEnroler.set(null);
    this.erreurEnrolement.set('');
  }

  /** Réduit l'étudiant à ce que la modale d'enrôlement affiche. */
  protected sujetEnrolement(etudiant: Etudiant): SujetEnrolement {
    return {
      nomComplet: nomCompletEtudiant(etudiant),
      matricule: etudiant.matricule,
      contexte: etudiant.classeLibelle,
      initiales: initialesEtudiant(etudiant),
    };
  }

  enroler(biometricId: string): void {
    const cible = this.etudiantAEnroler();
    if (!cible || this.enrolementEnCours()) return;
    this.enrolementEnCours.set(true);
    this.erreurEnrolement.set('');

    this.service.enroler(cible.id, biometricId).subscribe({
      next: (misAJour) => {
        this.remplacer(misAJour);
        this.enrolementEnCours.set(false);
        this.etudiantAEnroler.set(null);
      },
      error: (erreur: unknown) => {
        this.erreurEnrolement.set(messageErreur(erreur));
        this.enrolementEnCours.set(false);
      },
    });
  }

  ouvrirAcces(etudiant: Etudiant): void {
    if (this.actionEnCours()) return;
    this.actionEnCours.set(etudiant.id);
    this.erreurAction.set('');

    this.service.ouvrirCompte(etudiant.id).subscribe({
      next: (compte) => {
        this.compteCree.set(compte);
        this.actionEnCours.set(null);
        // Le compte n'est pas dans la réponse : on recharge pour refléter l'état.
        this.charger();
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.actionEnCours.set(null);
      },
    });
  }

  fermerAcces(etudiant: Etudiant): void {
    if (this.actionEnCours()) return;
    this.actionEnCours.set(etudiant.id);
    this.erreurAction.set('');

    this.service.fermerCompte(etudiant.id).subscribe({
      next: (misAJour) => {
        this.remplacer(misAJour);
        this.actionEnCours.set(null);
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.actionEnCours.set(null);
      },
    });
  }

  private remplacer(etudiant: Etudiant): void {
    this.etudiants.update((liste) => liste.map((e) => (e.id === etudiant.id ? etudiant : e)));
  }
}
