import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import { Classe, IssuePassage, LignePassage, Passage } from '../../core/models/classe.model';
import { ClasseService } from '../../core/services/classe.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';

const LIBELLES_ISSUE: Record<IssuePassage, { texte: string; classes: string }> = {
  PROMU: { texte: 'Promu', classes: 'bg-succes-bg text-succes' },
  REDOUBLANT: { texte: 'Redoublant', classes: 'bg-danger-bg text-danger' },
  EN_ATTENTE: { texte: 'En attente', classes: 'bg-alerte-bg text-alerte' },
  TERMINE: { texte: 'Sans classe supérieure', classes: 'bg-line-faint text-ink-subtle' },
  OPTION_A_CHOISIR: { texte: 'Option à choisir', classes: 'bg-alerte-bg text-alerte' },
};

/**
 * Passage en année supérieure.
 *
 * <p>Toujours en deux temps : une <b>simulation</b>, que l'on relit, puis
 * l'<b>application</b>. Déplacer une promotion entière est irréversible d'un clic — il
 * n'existe pas de retour arrière — et c'est la relecture qui permet de repérer, avant,
 * l'étudiant dont l'option manque ou dont les notes sont incomplètes.</p>
 *
 * <p>Le même passage s'exécute seul chaque 1<sup>er</sup> septembre ; cet écran sert à
 * le vérifier à l'avance ou à le déclencher à la main.</p>
 */
@Component({
  selector: 'sp-passage',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex min-w-0 flex-col gap-1.5">
        <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Passage de classe</h1>
        <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
          Les étudiants dont tous les semestres sont validés rejoignent la classe supérieure.
          Un semestre non validé donne un redoublant ; une note manquante laisse l'étudiant en
          attente, sans que rien soit décidé à sa place.
        </p>
      </div>

      <div class="flex flex-wrap items-end gap-3 rounded-[14px] border border-line bg-white p-4">
        <label class="flex min-w-[240px] flex-col gap-1.5">
          <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Portée</span>
          <span class="champ !h-11">
            <select
              name="classe"
              class="saisie bg-transparent"
              [ngModel]="classeId()"
              (ngModelChange)="choisir($event)"
            >
              <option [ngValue]="null">Toutes les classes</option>
              @for (classe of classes(); track classe.id) {
                <option [ngValue]="classe.id">{{ classe.code }} — {{ classe.libelle }}</option>
              }
            </select>
          </span>
        </label>

        <button type="button" class="btn btn-secondaire !h-11" (click)="simuler()" [disabled]="enCours()">
          <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
          Simuler le passage
        </button>

        @if (peutAppliquer()) {
          @if (confirmation()) {
            <span class="flex items-center gap-2">
              <span class="text-[12.5px] text-ink-muted">
                Déplacer {{ resultat()!.promus }} étudiant(s) ? Irréversible.
              </span>
              <button type="button" class="btn btn-primaire !h-11" (click)="appliquer()" [disabled]="enCours()">
                Oui, appliquer
              </button>
              <button type="button" class="btn btn-secondaire !h-11" (click)="confirmation.set(false)">
                Annuler
              </button>
            </span>
          } @else {
            <button type="button" class="btn btn-primaire !h-11" (click)="confirmation.set(true)">
              Appliquer le passage
            </button>
          }
        }
      </div>

      @if (erreur()) {
        <p
          class="m-0 flex items-start gap-2.5 rounded-[10px] border border-danger-line bg-danger-bg px-3.5 py-3 text-[12.5px] text-danger"
          role="alert"
        >
          <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" class="mt-px" />
          {{ erreur() }}
        </p>
      }

      @if (enCours() && !resultat()) {
        <sp-etat titre="Calcul en cours…" icone="horloge" />
      } @else if (resultat(); as r) {
        @if (r.appliquee) {
          <p
            class="m-0 flex items-center gap-2.5 rounded-[10px] border border-succes-line bg-succes-bg px-3.5 py-3 text-[12.5px] text-succes"
            role="status"
          >
            <sp-icone nom="coche" [taille]="16" [epaisseur]="2" />
            Passage appliqué : {{ r.promus }} étudiant(s) ont changé de classe.
          </p>
        } @else {
          <p class="m-0 text-[12.5px] text-ink-muted">
            Simulation : rien n'a été déplacé.
          </p>
        }

        <div class="grid grid-cols-2 gap-3 sm:grid-cols-5">
          @for (carte of cartes(); track carte.libelle) {
            <div class="rounded-[12px] border border-line bg-white px-4 py-3">
              <div class="num text-[22px] font-semibold text-ink">{{ carte.valeur }}</div>
              <div class="text-[11.5px] text-ink-subtle">{{ carte.libelle }}</div>
            </div>
          }
        </div>

        @if (r.lignes.length === 0) {
          <sp-etat
            icone="etudiants"
            titre="Aucun étudiant actif"
            detail="Les classes choisies ne contiennent aucun étudiant actif."
          />
        } @else {
          <div class="overflow-x-auto rounded-[14px] border border-line bg-white">
            <table class="w-full border-collapse text-[13px]">
              <thead>
                <tr>
                  <th class="th">Matricule</th>
                  <th class="th">Étudiant</th>
                  <th class="th">Classe actuelle</th>
                  <th class="th">Classe visée</th>
                  <th class="th">Décision</th>
                  <th class="th">Détail</th>
                </tr>
              </thead>
              <tbody>
                @for (ligne of r.lignes; track ligne.etudiantId) {
                  <tr class="border-b border-line-soft last:border-b-0">
                    <td class="num px-3 py-2.5 text-ink-muted">{{ ligne.matricule }}</td>
                    <td class="px-3 py-2.5 font-medium text-ink">{{ ligne.nomComplet }}</td>
                    <td class="px-3 py-2.5 text-ink-muted">{{ ligne.classeOrigine }}</td>
                    <td class="px-3 py-2.5 text-ink-muted">{{ ligne.classeDestination ?? '—' }}</td>
                    <td class="px-3 py-2.5">
                      <span class="badge" [class]="issue(ligne).classes">{{ issue(ligne).texte }}</span>
                    </td>
                    <td class="px-3 py-2.5 text-[12px] text-ink-subtle">{{ ligne.detail }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      } @else {
        <sp-etat
          icone="classes"
          titre="Aucune simulation"
          detail="Lancez une simulation pour voir, étudiant par étudiant, qui monterait et pourquoi."
        />
      }
    </div>
  `,
})
export class PassageComponent implements OnInit {
  private readonly service = inject(ClasseService);

  readonly classes = signal<Classe[]>([]);
  readonly classeId = signal<number | null>(null);
  readonly resultat = signal<Passage | null>(null);
  readonly enCours = signal(false);
  readonly erreur = signal('');
  readonly confirmation = signal(false);

  /** On n'applique qu'un passage vu : une simulation de la portée courante, avec des promus. */
  readonly peutAppliquer = computed(() => {
    const r = this.resultat();
    return r !== null && !r.appliquee && r.promus > 0;
  });

  readonly cartes = computed(() => {
    const r = this.resultat();
    if (!r) return [];
    return [
      { libelle: 'Promus', valeur: r.promus },
      { libelle: 'Redoublants', valeur: r.redoublants },
      { libelle: 'En attente', valeur: r.enAttente },
      { libelle: 'Option à choisir', valeur: r.optionsManquantes },
      { libelle: 'Sans classe supérieure', valeur: r.termines },
    ];
  });

  ngOnInit(): void {
    this.service.lister().subscribe({
      next: (classes) => this.classes.set(classes ?? []),
      error: (e: unknown) => this.erreur.set(messageErreur(e)),
    });
  }

  issue(ligne: LignePassage): { texte: string; classes: string } {
    return LIBELLES_ISSUE[ligne.issue];
  }

  /** Changer de portée invalide la simulation : on n'applique jamais un autre périmètre. */
  choisir(id: number | null): void {
    this.classeId.set(id);
    this.resultat.set(null);
    this.confirmation.set(false);
  }

  simuler(): void {
    this.lancer(false);
  }

  appliquer(): void {
    this.lancer(true);
  }

  private lancer(appliquer: boolean): void {
    this.enCours.set(true);
    this.erreur.set('');
    this.confirmation.set(false);

    this.service.passage(this.classeId(), appliquer).subscribe({
      next: (passage) => {
        this.resultat.set(passage);
        this.enCours.set(false);
      },
      error: (e: unknown) => {
        this.erreur.set(messageErreur(e));
        this.enCours.set(false);
      },
    });
  }
}
