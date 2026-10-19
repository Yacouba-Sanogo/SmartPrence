import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { messageErreur } from '../../core/api';
import { Classe } from '../../core/models/classe.model';
import { ClasseService } from '../../core/services/classe.service';
import { NoteATraiter, ValidationNoteService } from '../../core/services/validation-note.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';

interface GroupeMatiere {
  matiere: string;
  notes: NoteATraiter[];
}

interface GroupeClasse {
  classe: Classe;
  matieres: GroupeMatiere[];
  total: number;
}

/**
 * Notes transmises par les enseignants, à publier ou à renvoyer.
 *
 * <p>Seules les notes validées ici entrent dans les bulletins. Le serveur ne renvoie
 * pas la classe d'une note : la file est donc lue classe par classe et regroupée
 * côté écran.</p>
 */
@Component({
  selector: 'sp-notes-a-valider',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-[min(100%,280px)] flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Validation des notes</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Notes transmises par les enseignants. Une note validée entre dans les bulletins ;
            une note renvoyée redevient un brouillon, avec votre motif.
          </p>
        </div>
        <div class="flex items-center gap-2">
          <select
            class="champ !h-9 min-w-[180px]"
            aria-label="Filtrer par classe"
            [ngModel]="classeId()"
            (ngModelChange)="changerClasse($event)"
          >
            <option [ngValue]="null">Toutes les classes</option>
            @for (c of classes(); track c.id) {
              <option [ngValue]="c.id">{{ c.code }} — {{ c.libelle }}</option>
            }
          </select>
          <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
            <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
            Actualiser
          </button>
        </div>
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
      @if (succes()) {
        <div class="rounded-[12px] bg-succes-bg px-4 py-3 text-[13px] text-succes" role="status">
          {{ succes() }}
        </div>
      }

      @if (selection().size > 0) {
        <div class="carte flex flex-wrap items-center gap-3 px-4 py-3">
          <span class="text-[13px] font-medium text-ink">{{ selection().size }} note(s) sélectionnée(s)</span>
          <span class="flex-1"></span>
          <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="viderSelection()">
            Tout désélectionner
          </button>
          <button
            type="button"
            class="btn btn-secondaire !h-8 !px-3 !text-[12px]"
            (click)="ouvrirRenvoi()"
            [disabled]="enCours()"
          >
            Renvoyer…
          </button>
          <button
            type="button"
            class="btn btn-primaire !h-8 !px-3 !text-[12px]"
            (click)="valider()"
            [disabled]="enCours()"
          >
            Valider la sélection
          </button>
        </div>
      }

      @if (renvoiOuvert()) {
        <div class="carte flex flex-col gap-3 px-4 py-4">
          <label class="text-[13px] font-medium text-ink" for="motif-renvoi">
            Motif du renvoi ({{ selection().size }} note(s)) — obligatoire
          </label>
          <textarea
            id="motif-renvoi"
            class="champ min-h-[80px] py-2"
            maxlength="500"
            placeholder="Ce que l'enseignant doit corriger…"
            [ngModel]="motif()"
            (ngModelChange)="motif.set($event)"
          ></textarea>
          <div class="flex justify-end gap-2">
            <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="renvoiOuvert.set(false)">
              Annuler
            </button>
            <button
              type="button"
              class="btn btn-primaire !h-8 !px-3 !text-[12px]"
              (click)="renvoyer()"
              [disabled]="enCours() || !motif().trim()"
            >
              Confirmer le renvoi
            </button>
          </div>
        </div>
      }

      @if (chargement()) {
        <section class="carte"><sp-etat titre="Chargement des notes transmises…" icone="horloge" /></section>
      } @else if (erreur()) {
        <section class="carte">
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger les notes"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        </section>
      } @else if (groupes().length === 0) {
        <section class="carte">
          <sp-etat
            icone="coche-cercle"
            titre="Aucune note en attente"
            detail="Les notes transmises par les enseignants apparaîtront ici."
          />
        </section>
      } @else {
        @for (groupe of groupes(); track groupe.classe.id) {
          <section class="carte overflow-hidden">
            <header class="flex items-center gap-3 border-b border-line-soft bg-[#FCFDFE] px-[18px] py-3">
              <h2 class="m-0 flex-1 text-[15px] font-semibold text-royal-700">
                {{ groupe.classe.code }} — {{ groupe.classe.libelle }}
              </h2>
              <span class="text-xs text-ink-subtle">{{ groupe.total }} note(s)</span>
              <button
                type="button"
                class="btn btn-secondaire !h-7 !px-2.5 !text-[11.5px]"
                (click)="basculer(toutesLesNotes(groupe))"
              >
                {{ toutSelectionne(toutesLesNotes(groupe)) ? 'Désélectionner' : 'Tout sélectionner' }}
              </button>
            </header>
            @for (m of groupe.matieres; track m.matiere) {
              <div class="border-b border-line-soft last:border-b-0">
                <div class="flex items-center gap-3 bg-canvas px-[18px] py-2">
                  <input
                    type="checkbox"
                    [checked]="toutSelectionne(m.notes)"
                    (change)="basculer(m.notes)"
                    [attr.aria-label]="'Sélectionner toutes les notes de ' + m.matiere"
                  />
                  <span class="text-[13px] font-semibold text-ink">{{ m.matiere }}</span>
                  <span class="text-xs text-ink-faint">{{ m.notes.length }}</span>
                </div>
                <div class="overflow-x-auto">
                  <table>
                    <thead>
                      <tr>
                        <th class="th !pl-[18px]"></th>
                        <th class="th">Étudiant</th>
                        <th class="th">Évaluation</th>
                        <th class="th">Enseignant</th>
                        <th class="th text-right">Coef.</th>
                        <th class="th !pr-[18px] text-right">Note</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (n of m.notes; track n.id) {
                        <tr class="hover:bg-[#FBFCFE]">
                          <td class="td !pl-[18px]">
                            <input
                              type="checkbox"
                              [checked]="selection().has(n.id)"
                              (change)="basculerUne(n.id)"
                              [attr.aria-label]="'Sélectionner la note de ' + n.etudiantNom"
                            />
                          </td>
                          <td class="td text-[13.5px] font-medium text-ink">
                            {{ n.etudiantPrenom }} {{ n.etudiantNom }}
                          </td>
                          <td class="td text-[12.5px] text-[#364057]">{{ n.libelle || '—' }}</td>
                          <td class="td text-[12.5px] text-[#364057]">{{ n.enseignantNom }}</td>
                          <td class="td num text-right">{{ n.coefficient }}</td>
                          <td class="td num !pr-[18px] text-right font-semibold text-ink">
                            {{ n.valeur }}/20
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              </div>
            }
          </section>
        }
      }
    </div>
  `,
})
export class NotesAValiderComponent {
  private readonly service = inject(ValidationNoteService);
  private readonly classeService = inject(ClasseService);

  readonly classes = signal<Classe[]>([]);
  readonly classeId = signal<number | null>(null);
  readonly groupes = signal<GroupeClasse[]>([]);
  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly erreurAction = signal('');
  readonly succes = signal('');
  readonly enCours = signal(false);
  readonly selection = signal<ReadonlySet<string>>(new Set<string>());
  readonly renvoiOuvert = signal(false);
  readonly motif = signal('');

  constructor() {
    this.charger();
  }

  changerClasse(id: number | null): void {
    this.classeId.set(id);
    this.charger();
  }

  viderSelection(): void {
    this.selection.set(new Set<string>());
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.succes.set('');
    this.viderSelection();
    this.renvoiOuvert.set(false);

    this.classeService.lister().subscribe({
      next: (classes) => {
        this.classes.set(classes ?? []);
        const cibles = (classes ?? []).filter(
          (c) => this.classeId() === null || c.id === this.classeId(),
        );
        if (cibles.length === 0) {
          this.groupes.set([]);
          this.chargement.set(false);
          return;
        }
        forkJoin(
          cibles.map((classe) =>
            this.service.aValider(classe.id).pipe(map((notes) => ({ classe, notes: notes ?? [] }))),
          ),
        ).subscribe({
          next: (resultats) => {
            this.groupes.set(
              resultats.filter((r) => r.notes.length > 0).map((r) => this.grouper(r.classe, r.notes)),
            );
            this.chargement.set(false);
          },
          error: (e: unknown) => {
            this.erreur.set(messageErreur(e));
            this.chargement.set(false);
          },
        });
      },
      error: (e: unknown) => {
        this.erreur.set(messageErreur(e));
        this.chargement.set(false);
      },
    });
  }

  private grouper(classe: Classe, notes: NoteATraiter[]): GroupeClasse {
    const parMatiere = new Map<string, NoteATraiter[]>();
    for (const note of notes) {
      const liste = parMatiere.get(note.matiereLibelle) ?? [];
      liste.push(note);
      parMatiere.set(note.matiereLibelle, liste);
    }
    const matieres = [...parMatiere.entries()]
      .sort(([a], [b]) => a.localeCompare(b, 'fr'))
      .map(([matiere, liste]) => ({
        matiere,
        notes: liste.sort((a, b) => a.etudiantNom.localeCompare(b.etudiantNom, 'fr')),
      }));
    return { classe, matieres, total: notes.length };
  }

  toutesLesNotes(groupe: GroupeClasse): NoteATraiter[] {
    return groupe.matieres.flatMap((m) => m.notes);
  }

  toutSelectionne(notes: NoteATraiter[]): boolean {
    const s = this.selection();
    return notes.length > 0 && notes.every((n) => s.has(n.id));
  }

  basculer(notes: NoteATraiter[]): void {
    const suivante = new Set(this.selection());
    if (this.toutSelectionne(notes)) notes.forEach((n) => suivante.delete(n.id));
    else notes.forEach((n) => suivante.add(n.id));
    this.selection.set(suivante);
  }

  basculerUne(id: string): void {
    const suivante = new Set(this.selection());
    if (!suivante.delete(id)) suivante.add(id);
    this.selection.set(suivante);
  }

  ouvrirRenvoi(): void {
    this.motif.set('');
    this.renvoiOuvert.set(true);
  }

  valider(): void {
    this.executer(
      (ids) => this.service.valider(ids),
      (n) => `${n} note(s) validée(s) : elles figurent désormais aux bulletins.`,
    );
  }

  renvoyer(): void {
    const motif = this.motif().trim();
    if (!motif) return;
    this.executer(
      (ids) => this.service.renvoyer(ids, motif),
      (n) => `${n} note(s) renvoyée(s) à leur enseignant.`,
    );
  }

  private executer(
    appel: (ids: string[]) => ReturnType<ValidationNoteService['valider']>,
    message: (n: number) => string,
  ): void {
    if (this.enCours() || this.selection().size === 0) return;
    this.enCours.set(true);
    this.erreurAction.set('');
    this.succes.set('');
    const ids = [...this.selection()];

    appel(ids).subscribe({
      next: () => {
        // Le lot est traité en entier : on retire ces notes de la file sans tout recharger.
        const traites = new Set(ids);
        this.groupes.update((gs) =>
          gs
            .map((g) => {
              const matieres = g.matieres
                .map((m) => ({ ...m, notes: m.notes.filter((n) => !traites.has(n.id)) }))
                .filter((m) => m.notes.length > 0);
              return { ...g, matieres, total: matieres.reduce((s, m) => s + m.notes.length, 0) };
            })
            .filter((g) => g.total > 0),
        );
        this.viderSelection();
        this.renvoiOuvert.set(false);
        this.succes.set(message(ids.length));
        this.enCours.set(false);
      },
      error: (e: unknown) => {
        this.erreurAction.set(messageErreur(e));
        this.enCours.set(false);
      },
    });
  }
}
