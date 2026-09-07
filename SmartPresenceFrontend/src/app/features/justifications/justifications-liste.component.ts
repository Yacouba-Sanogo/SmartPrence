import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import {
  Justification,
  JustificationService,
  StatutJustification,
  libelleStatutJustification,
  teinteStatutJustification,
} from '../../core/services/justification.service';
import { AttenteService } from '../../core/services/attente.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';

const MOIS = [
  'janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin',
  'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.',
];

/**
 * Justificatifs d'absence déposés par les étudiants.
 *
 * <p>Approuver un justificatif ne réécrit pas le relevé d'absence : il le qualifie.
 * L'absence reste un fait constaté, le justificatif en explique la raison — les
 * confondre reviendrait à effacer l'absence des registres.</p>
 */
@Component({
  selector: 'sp-justifications-liste',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Justificatifs</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Demandes déposées par les étudiants. Approuver qualifie l'absence, sans l'effacer.
          </p>
        </div>
        <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
          <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
          Actualiser
        </button>
      </div>

      <div class="grid grid-cols-3 gap-3">
        @for (kpi of indicateurs(); track kpi.libelle) {
          <div class="carte flex flex-col gap-2.5 px-4 py-3.5">
            <div class="flex items-center gap-2.5">
              <span class="grid size-7 place-items-center rounded-lg" [class]="kpi.teinte">
                <sp-icone [nom]="kpi.icone" [taille]="16" [epaisseur]="1.7" />
              </span>
              <span class="truncate text-xs font-medium text-ink-muted">{{ kpi.libelle }}</span>
            </div>
            <span class="num text-[27px] font-semibold leading-none tracking-tight text-ink">
              {{ kpi.valeur }}
            </span>
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

      <div class="carte flex flex-wrap items-center gap-2 px-4 py-3">
        @for (choix of filtres; track choix.libelle) {
          <button
            type="button"
            class="rounded-[8px] border px-3 py-1.5 text-[12.5px] font-medium transition-colors"
            [class]="
              filtre() === choix.valeur
                ? 'border-royal-600 bg-royal-600 text-white'
                : 'border-line bg-white text-ink-muted hover:bg-canvas'
            "
            (click)="changerFiltre(choix.valeur)"
          >
            {{ choix.libelle }}
          </button>
        }
      </div>

      <section class="carte overflow-hidden">
        @if (chargement()) {
          <sp-etat titre="Chargement des justificatifs…" icone="horloge" />
        } @else if (erreur()) {
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger les justificatifs"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        } @else if (affichees().length === 0) {
          <sp-etat
            icone="info"
            [titre]="filtre() === null ? 'Aucun justificatif' : 'Aucun justificatif dans cet état'"
            detail="Les étudiants déposent leurs justificatifs depuis l'application mobile."
          />
        } @else {
          <div class="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th class="th !pl-[18px]">Étudiant</th>
                  <th class="th">Date d'absence</th>
                  <th class="th">Motif</th>
                  <th class="th">Statut</th>
                  <th class="th !pr-[18px] text-right">Décision</th>
                </tr>
              </thead>
              <tbody>
                @for (demande of affichees(); track demande.id) {
                  <tr class="hover:bg-[#FBFCFE]">
                    <td class="td !pl-[18px]">
                      <span class="text-[13.5px] font-medium text-ink">
                        {{ demande.etudiantPrenom }} {{ demande.etudiantNom }}
                      </span>
                    </td>
                    <td class="td num text-[#364057]">{{ dateCourte(demande.dateAbsence) }}</td>
                    <td class="td">
                      <span class="flex min-w-0 max-w-[320px] flex-col gap-0.5">
                        <span class="truncate text-[12.5px] text-[#364057]" [title]="demande.motif">
                          {{ demande.motif }}
                        </span>
                        @if (demande.commentaireTraitement) {
                          <span class="truncate text-[11px] text-ink-faint">
                            Réponse : {{ demande.commentaireTraitement }}
                          </span>
                        }
                      </span>
                    </td>
                    <td class="td">
                      <span class="badge" [class]="teinte(demande.statut)">
                        {{ libelle(demande.statut) }}
                      </span>
                    </td>
                    <td class="td !pr-[18px]">
                      @if (demande.statut === 'EN_ATTENTE') {
                        <div class="flex items-center justify-end gap-2">
                          <button
                            type="button"
                            class="btn btn-secondaire !h-8 !px-3 !text-[12px]"
                            (click)="trancher(demande, 'REFUSEE')"
                            [disabled]="enCours() === demande.id"
                          >
                            Rejeter
                          </button>
                          <button
                            type="button"
                            class="btn btn-primaire !h-8 !px-3 !text-[12px]"
                            (click)="trancher(demande, 'ACCEPTEE')"
                            [disabled]="enCours() === demande.id"
                          >
                            Approuver
                          </button>
                        </div>
                      } @else {
                        <span class="block text-right text-[12px] text-ink-faint">Traité</span>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <footer class="border-t border-line-soft bg-[#FCFDFE] px-[18px] py-3 text-xs text-ink-subtle">
            {{ affichees().length }} justificatif(s) sur {{ demandes().length }}
          </footer>
        }
      </section>
    </div>
  `,
})
export class JustificationsListeComponent {
  private readonly service = inject(JustificationService);
  private readonly attente = inject(AttenteService);

  readonly demandes = signal<Justification[]>([]);
  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly erreurAction = signal('');
  readonly enCours = signal<string | null>(null);
  readonly filtre = signal<StatutJustification | null>('EN_ATTENTE');

  protected readonly filtres: readonly { valeur: StatutJustification | null; libelle: string }[] = [
    { valeur: 'EN_ATTENTE', libelle: 'À traiter' },
    { valeur: 'ACCEPTEE', libelle: 'Approuvés' },
    { valeur: 'REFUSEE', libelle: 'Rejetés' },
    { valeur: null, libelle: 'Tous' },
  ];

  readonly affichees = computed(() => {
    const filtre = this.filtre();
    const liste = filtre ? this.demandes().filter((d) => d.statut === filtre) : this.demandes();
    return [...liste].sort((a, b) => b.dateAbsence.localeCompare(a.dateAbsence));
  });

  readonly indicateurs = computed(() => {
    const compte = (statut: StatutJustification) =>
      this.demandes().filter((d) => d.statut === statut).length;
    const enAttente = compte('EN_ATTENTE');
    return [
      {
        libelle: 'À traiter',
        valeur: enAttente,
        icone: 'horloge' as const,
        teinte: enAttente > 0 ? 'bg-alerte-bg text-alerte' : 'bg-line-faint text-[#4A5470]',
      },
      {
        libelle: 'Approuvés',
        valeur: compte('ACCEPTEE'),
        icone: 'coche-cercle' as const,
        teinte: 'bg-succes-bg text-succes',
      },
      {
        libelle: 'Rejetés',
        valeur: compte('REFUSEE'),
        icone: 'moins-cercle' as const,
        teinte: 'bg-line-faint text-[#4A5470]',
      },
    ];
  });

  constructor() {
    this.charger();
  }

  libelle = libelleStatutJustification;
  teinte = teinteStatutJustification;

  dateCourte(iso: string): string {
    const date = new Date(`${iso}T12:00:00`);
    return `${date.getDate()} ${MOIS[date.getMonth()]} ${date.getFullYear()}`;
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.service.lister().subscribe({
      next: (demandes) => {
        this.demandes.set(demandes ?? []);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  changerFiltre(valeur: StatutJustification | null): void {
    this.filtre.set(valeur);
  }

  trancher(demande: Justification, statut: 'ACCEPTEE' | 'REFUSEE'): void {
    if (this.enCours()) return;
    this.enCours.set(demande.id);
    this.erreurAction.set('');

    this.service.traiter(demande.id, statut).subscribe({
      next: () => {
        // Le backend ne renvoie pas la demande mise à jour : on reflète la décision
        // localement plutôt que de recharger toute la liste pour une ligne.
        this.demandes.update((liste) =>
          liste.map((d) => (d.id === demande.id ? { ...d, statut } : d)),
        );
        this.enCours.set(null);
        // Idem : la pastille « à traiter » de la barre latérale suit la décision.
        this.attente.rafraichir();
      },
      error: (erreur: unknown) => {
        this.erreurAction.set(messageErreur(erreur));
        this.enCours.set(null);
      },
    });
  }
}
