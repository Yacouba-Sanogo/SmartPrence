import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import { Classe } from '../../core/models/classe.model';
import { BilanImport, EtudiantService, LigneImport, StatutLigneImport } from '../../core/services/etudiant.service';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';
import { SourceListe, lireListe } from './lecture-liste';

interface LigneAffichee extends LigneImport {
  cle: number;
  statut: StatutLigneImport | null;
  message: string;
}

/**
 * Import d'une liste d'étudiants : fichier CSV, classeur Excel ou photo d'une liste imprimée.
 *
 * <p>Le fichier est lu dans le navigateur puis présenté sous forme de tableau modifiable : une
 * photo se lit rarement sans faute, et un fichier peut contenir un matricule déjà inscrit. Chaque
 * ligne est vérifiée par le serveur (simulation) avant l'inscription réelle, qui traite les
 * lignes une à une — une ligne fautive n'empêche pas les autres.</p>
 */
@Component({
  selector: 'sp-import-etudiants-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      titre="Importer une liste d'étudiants"
      sousTitre="Fichier CSV, Excel ou photo d'une liste"
      icone="etudiants"
      (fermer)="fermer.emit()"
    >
      <div class="flex flex-col gap-4 px-5 py-5">
        @if (erreur()) {
          <p class="m-0 flex items-start gap-2.5 rounded-[10px] border border-danger-line bg-danger-bg px-3.5 py-3 text-[12.5px] leading-relaxed text-danger" role="alert">
            <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" class="mt-px" />
            {{ erreur() }}
          </p>
        }

        @if (bilanFinal(); as b) {
          <div class="flex flex-col items-center gap-2 py-4 text-center">
            <span class="grid size-12 place-items-center rounded-full bg-succes-bg text-succes">
              <sp-icone nom="coche-cercle" [taille]="26" />
            </span>
            <h3 class="m-0 text-[16px] font-semibold text-ink">{{ b.creees }} étudiant{{ b.creees > 1 ? 's' : '' }} inscrit{{ b.creees > 1 ? 's' : '' }}</h3>
            <p class="m-0 text-[13px] text-ink-muted">
              {{ b.ignorees }} déjà inscrit{{ b.ignorees > 1 ? 's' : '' }} (ignoré{{ b.ignorees > 1 ? 's' : '' }}) ·
              {{ b.erreurs }} erreur{{ b.erreurs > 1 ? 's' : '' }}
            </p>
            @if (b.erreurs > 0) {
              <p class="m-0 text-[12px] text-ink-subtle">Les lignes en erreur sont restées dans le tableau : corrigez-les puis relancez.</p>
            }
          </div>
        }
        @if (!bilanFinal() || lignes().length > 0) {
          <label class="flex flex-col gap-1.5">
            <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Classe de destination</span>
            <select class="champ !h-11" [ngModel]="classeId()" (ngModelChange)="changerClasse($event)" [disabled]="occupe()">
              <option [ngValue]="null" disabled>Choisir une classe…</option>
              @for (c of classes(); track c.id) {
                <option [ngValue]="c.id">{{ c.code }} — {{ c.libelle }}</option>
              }
            </select>
          </label>

          <label
            class="flex cursor-pointer flex-col items-center gap-2 rounded-[12px] border-[1.5px] border-dashed border-royal-200 bg-royal-50 px-4 py-6 text-center transition-colors hover:border-royal-400"
          >
            <sp-icone nom="telecharger" [taille]="24" class="text-royal-500" />
            <span class="text-[13.5px] font-semibold text-royal-700">
              {{ nomFichier() || 'Choisir un fichier ou une photo' }}
            </span>
            <span class="text-[12px] text-ink-subtle">
              .csv · .xlsx · .jpg · .png — colonnes : matricule, nom, prénom (email, téléphone en option)
            </span>
            <input class="sr-only" type="file" accept=".csv,.tsv,.txt,.xlsx,image/*" (change)="choisir($event)" [disabled]="occupe()" />
          </label>

          @if (progression()) {
            <div class="flex flex-col gap-1.5">
              <span class="text-[12.5px] text-ink-muted">{{ progression() }}</span>
              <span class="h-1.5 overflow-hidden rounded-full bg-line-faint">
                <span
                  class="block h-full rounded-full bg-royal-500 transition-all"
                  [class.animate-pulse]="part() === null"
                  [style.width.%]="part() === null ? 40 : part()! * 100"
                ></span>
              </span>
            </div>
          }

          @if (source() === 'IMAGE' && lignes().length > 0) {
            <p class="m-0 rounded-[10px] bg-alerte-bg px-3.5 py-2.5 text-[12.5px] leading-relaxed text-alerte">
              La lecture d'une photo peut se tromper (0 et O, 1 et l). Relisez chaque ligne avant d'importer.
            </p>
          }

          @if (lignes().length > 0) {
            <div class="flex flex-wrap items-center gap-2 text-[12.5px]">
              <span class="font-semibold text-ink">{{ lignes().length }} ligne{{ lignes().length > 1 ? 's' : '' }}</span>
              <span class="badge bg-succes-bg text-succes">{{ compte('CREE') }} à inscrire</span>
              <span class="badge bg-line-faint text-ink-muted">{{ compte('IGNORE') }} déjà inscrit{{ compte('IGNORE') > 1 ? 's' : '' }}</span>
              <span class="badge" [class]="compte('ERREUR') > 0 ? 'bg-danger-bg text-danger' : 'bg-line-faint text-ink-muted'">{{ compte('ERREUR') }} erreur{{ compte('ERREUR') > 1 ? 's' : '' }}</span>
            </div>

            <div class="max-h-[300px] overflow-auto rounded-[10px] border border-line">
              <table class="w-full min-w-[560px] border-collapse text-[12.5px]">
                <thead class="sticky top-0 bg-canvas text-left text-[11px] uppercase tracking-wide text-ink-muted">
                  <tr>
                    <th class="px-2 py-2">Matricule</th>
                    <th class="px-2 py-2">Nom</th>
                    <th class="px-2 py-2">Prénom</th>
                    <th class="px-2 py-2">Email</th>
                    <th class="px-2 py-2">État</th>
                    <th class="w-8 px-1"></th>
                  </tr>
                </thead>
                <tbody>
                  @for (l of lignes(); track l.cle) {
                    <tr class="border-t border-line-faint align-top">
                      <td class="px-1 py-1"><input class="saisie !h-8 w-full rounded-md border border-line px-2 num uppercase" [ngModel]="l.matricule" (ngModelChange)="modifier(l.cle, { matricule: $event })" /></td>
                      <td class="px-1 py-1"><input class="saisie !h-8 w-full rounded-md border border-line px-2" [ngModel]="l.nom" (ngModelChange)="modifier(l.cle, { nom: $event })" /></td>
                      <td class="px-1 py-1"><input class="saisie !h-8 w-full rounded-md border border-line px-2" [ngModel]="l.prenom" (ngModelChange)="modifier(l.cle, { prenom: $event })" /></td>
                      <td class="px-1 py-1"><input class="saisie !h-8 w-full rounded-md border border-line px-2" [ngModel]="l.email ?? ''" (ngModelChange)="modifier(l.cle, { email: $event })" /></td>
                      <td class="px-2 py-1.5">
                        @switch (l.statut) {
                          @case ('CREE') { <span class="text-succes">À inscrire</span> }
                          @case ('IGNORE') { <span class="text-ink-subtle">Déjà inscrit</span> }
                          @case ('ERREUR') { <span class="text-danger">{{ l.message }}</span> }
                          @default { <span class="text-ink-faint">—</span> }
                        }
                      </td>
                      <td class="px-1 py-1">
                        <button type="button" class="grid size-7 place-items-center rounded-md text-ink-faint hover:bg-canvas hover:text-danger" (click)="retirer(l.cle)" aria-label="Retirer la ligne">
                          <sp-icone nom="fermer" [taille]="14" [epaisseur]="1.9" />
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        }
      </div>

      <footer class="flex flex-wrap items-center justify-end gap-2.5 border-t border-line-soft bg-[#FBFCFE] px-5 py-3.5">
        @if (bilanFinal() && compte('ERREUR') === 0) {
          <button type="button" class="btn btn-primaire" (click)="termine.emit()">Terminer</button>
        } @else {
          <button type="button" class="btn btn-secondaire" (click)="bilanFinal() ? termine.emit() : fermer.emit()">
            {{ bilanFinal() ? 'Fermer' : 'Annuler' }}
          </button>
          @if (lignes().length > 0) {
            <button type="button" class="btn btn-secondaire" (click)="verifier()" [disabled]="occupe() || !classeId()">Vérifier</button>
            <button type="button" class="btn btn-primaire" (click)="importer()" [disabled]="occupe() || !classeId() || aImporter() === 0">
              <sp-icone nom="coche" [taille]="15" [epaisseur]="2.1" />
              {{ occupe() ? 'Traitement…' : 'Importer ' + aImporter() + ' étudiant' + (aImporter() > 1 ? 's' : '') }}
            </button>
          }
        }
      </footer>
    </sp-modale>
  `,
})
export class ImportEtudiantsDialogComponent {
  private readonly service = inject(EtudiantService);

  readonly classes = input.required<Classe[]>();
  /** Classe présélectionnée, par exemple celle du filtre de la liste. */
  readonly classeInitiale = input<number | null>(null);

  readonly fermer = output<void>();
  /** Émis quand l'import est terminé : la liste se recharge. */
  readonly termine = output<void>();

  readonly classeId = signal<number | null>(null);
  readonly nomFichier = signal('');
  readonly source = signal<SourceListe | null>(null);
  readonly lignes = signal<LigneAffichee[]>([]);
  readonly progression = signal('');
  readonly part = signal<number | null>(null);
  readonly occupe = signal(false);
  readonly erreur = signal('');
  readonly bilanFinal = signal<BilanImport | null>(null);

  readonly aImporter = computed(() => this.lignes().filter((l) => l.statut !== 'IGNORE' && l.statut !== 'ERREUR').length);

  private prochaineCle = 1;

  constructor() {
    queueMicrotask(() => this.classeId.set(this.classeInitiale()));
  }

  compte(statut: StatutLigneImport): number {
    return this.lignes().filter((l) => l.statut === statut).length;
  }

  changerClasse(id: number | null): void {
    this.classeId.set(id);
    if (this.lignes().length > 0) this.verifier();
  }

  async choisir(evenement: Event): Promise<void> {
    const entree = evenement.target as HTMLInputElement;
    const fichier = entree.files?.[0];
    entree.value = '';
    if (!fichier) return;

    this.erreur.set('');
    this.bilanFinal.set(null);
    this.lignes.set([]);
    this.nomFichier.set(fichier.name);
    this.occupe.set(true);
    try {
      const liste = await lireListe(fichier, (message, part) => {
        this.progression.set(message);
        this.part.set(part);
      });
      this.source.set(liste.source);
      if (liste.lignes.length === 0) {
        this.erreur.set(
          liste.source === 'IMAGE'
            ? "Aucun étudiant reconnu sur cette photo. Cadrez la liste de près, bien éclairée, avec le matricule sur chaque ligne."
            : 'Aucun étudiant trouvé. Vérifiez les colonnes : matricule, nom, prénom.',
        );
      } else {
        this.lignes.set(liste.lignes.map((l) => ({ ...l, cle: this.prochaineCle++, statut: null, message: '' })));
      }
    } catch (e) {
      this.erreur.set(e instanceof Error ? e.message : 'Lecture du fichier impossible.');
    } finally {
      this.progression.set('');
      this.part.set(null);
      this.occupe.set(false);
    }
    if (this.lignes().length > 0 && this.classeId()) this.verifier();
  }

  modifier(cle: number, patch: Partial<LigneImport>): void {
    this.lignes.update((l) => l.map((x) => (x.cle === cle ? { ...x, ...patch, statut: null, message: '' } : x)));
  }

  retirer(cle: number): void {
    this.lignes.update((l) => l.filter((x) => x.cle !== cle));
  }

  verifier(): void {
    this.lancer(true);
  }

  importer(): void {
    this.lancer(false);
  }

  private lancer(simulation: boolean): void {
    const classe = this.classeId();
    if (!classe || this.occupe() || this.lignes().length === 0) return;
    this.occupe.set(true);
    this.erreur.set('');
    const envoyees = this.lignes();
    this.service.importer(classe, envoyees.map(({ matricule, nom, prenom, email, telephone, dateNaissance }) => ({ matricule, nom, prenom, email, telephone, dateNaissance })), simulation).subscribe({
      next: (bilan) => {
        const parIndice = new Map(bilan.lignes.map((l) => [l.indice, l]));
        const maj = envoyees.map((l, i) => {
          const r = parIndice.get(i + 1);
          return r ? { ...l, statut: r.statut, message: r.message } : l;
        });
        if (simulation) {
          this.lignes.set(maj);
        } else {
          // Les inscrits et les doublons quittent le tableau ; seules les erreurs restent à corriger.
          this.lignes.set(maj.filter((l) => l.statut === 'ERREUR'));
          this.bilanFinal.set(bilan);
        }
        this.occupe.set(false);
      },
      error: (e: unknown) => {
        this.erreur.set(messageErreur(e));
        this.occupe.set(false);
      },
    });
  }
}
