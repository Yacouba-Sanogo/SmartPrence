import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import { Classe } from '../../core/models/classe.model';
import { Etudiant } from '../../core/models/etudiant.model';
import { EtudiantService } from '../../core/services/etudiant.service';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';

/** Création ou modification d'un étudiant. */
@Component({
  selector: 'sp-etudiant-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      [titre]="modeEdition() ? 'Modifier l\\'étudiant' : 'Inscrire un étudiant'"
      sousTitre="L'enrôlement biométrique se fait dans un second temps"
      icone="etudiants"
      (fermer)="fermer.emit()"
    >
      <form (ngSubmit)="enregistrer()">
        <div class="flex flex-col gap-4 px-5 py-5">
          @if (erreur()) {
            <p
              class="m-0 flex items-start gap-2.5 rounded-[10px] border border-danger-line bg-danger-bg px-3.5 py-3 text-[12.5px] leading-relaxed text-danger"
              role="alert"
            >
              <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" class="mt-px" />
              {{ erreur() }}
            </p>
          }

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Matricule</span>
              <span class="champ !h-11">
                <input
                  type="text"
                  name="matricule"
                  class="saisie num uppercase"
                  placeholder="ET-2026-001"
                  required
                  [ngModel]="matricule()"
                  (ngModelChange)="matricule.set($event)"
                />
              </span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Classe</span>
              <select
                class="champ !h-11 text-ink"
                name="classeId"
                [ngModel]="classeId()"
                (ngModelChange)="classeId.set($event)"
                required
              >
                <option [ngValue]="null" disabled>Choisir une classe…</option>
                @for (classe of classes(); track classe.id) {
                  <option [ngValue]="classe.id">{{ classe.code }} — {{ classe.libelle }}</option>
                }
              </select>
              @if (classes().length === 0) {
                <span class="text-[11px] text-alerte">
                  Aucune classe enregistrée : créez-en une avant d'inscrire un étudiant.
                </span>
              }
            </label>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Prénom</span>
              <span class="champ !h-11">
                <input
                  type="text"
                  name="prenom"
                  class="saisie"
                  required
                  [ngModel]="prenom()"
                  (ngModelChange)="prenom.set($event)"
                />
              </span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Nom</span>
              <span class="champ !h-11">
                <input
                  type="text"
                  name="nom"
                  class="saisie"
                  required
                  [ngModel]="nom()"
                  (ngModelChange)="nom.set($event)"
                />
              </span>
            </label>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1.5">
              <span class="flex items-baseline gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Email</span>
                <span class="text-[11px] text-ink-faint">facultatif</span>
              </span>
              <span class="champ !h-11">
                <input
                  type="email"
                  name="email"
                  class="saisie"
                  placeholder="prenom.nom@univ.ml"
                  [ngModel]="email()"
                  (ngModelChange)="email.set($event)"
                />
              </span>
              <span class="text-[11px] leading-relaxed text-ink-faint">
                Servira d'identifiant de connexion. À défaut, une adresse est fabriquée sur le
                matricule.
              </span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="flex items-baseline gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Téléphone</span>
                <span class="text-[11px] text-ink-faint">facultatif</span>
              </span>
              <span class="champ !h-11">
                <input
                  type="tel"
                  name="telephone"
                  class="saisie num"
                  placeholder="+223 76 00 00 00"
                  [ngModel]="telephone()"
                  (ngModelChange)="telephone.set($event)"
                />
              </span>
            </label>
          </div>

          <label class="flex flex-col gap-1.5 sm:w-1/2 sm:pr-1.5">
            <span class="flex items-baseline gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
                Date de naissance
              </span>
              <span class="text-[11px] text-ink-faint">facultatif</span>
            </span>
            <span class="champ !h-11">
              <sp-icone nom="calendrier" [taille]="16" class="text-ink-muted" />
              <input
                type="date"
                name="dateNaissance"
                class="saisie num"
                [max]="aujourdhui"
                [ngModel]="dateNaissance()"
                (ngModelChange)="dateNaissance.set($event)"
              />
            </span>
          </label>
        </div>

        <footer class="flex items-center gap-2.5 border-t border-line-soft bg-[#FCFDFE] px-5 py-4">
          <span class="flex-1"></span>
          <button type="button" class="btn btn-secondaire" (click)="fermer.emit()">Annuler</button>
          <button type="submit" class="btn btn-primaire" [disabled]="!valide() || envoi()">
            <sp-icone nom="coche" [taille]="15" [epaisseur]="2.1" />
            {{ envoi() ? 'Enregistrement…' : modeEdition() ? 'Enregistrer' : "Inscrire l'étudiant" }}
          </button>
        </footer>
      </form>
    </sp-modale>
  `,
})
export class EtudiantDialogComponent implements OnInit {
  private readonly service = inject(EtudiantService);

  protected readonly aujourdhui = new Date().toISOString().slice(0, 10);

  readonly etudiant = input<Etudiant | null>(null);
  readonly classes = input.required<Classe[]>();

  readonly fermer = output<void>();
  readonly enregistre = output<void>();

  readonly matricule = signal('');
  readonly nom = signal('');
  readonly prenom = signal('');
  readonly email = signal('');
  readonly telephone = signal('');
  readonly dateNaissance = signal('');
  readonly classeId = signal<number | null>(null);
  readonly envoi = signal(false);
  readonly erreur = signal('');

  readonly modeEdition = computed(() => this.etudiant() !== null);

  readonly valide = computed(
    () =>
      this.matricule().trim().length > 0 &&
      this.nom().trim().length > 0 &&
      this.prenom().trim().length > 0 &&
      this.classeId() !== null,
  );

  ngOnInit(): void {
    const existant = this.etudiant();
    if (!existant) {
      // Une seule classe disponible : la pré-sélectionner évite un clic inutile.
      const classes = this.classes();
      if (classes.length === 1) this.classeId.set(classes[0].id);
      return;
    }
    this.matricule.set(existant.matricule);
    this.nom.set(existant.nom);
    this.prenom.set(existant.prenom);
    this.email.set(existant.email ?? '');
    this.telephone.set(existant.telephone ?? '');
    this.dateNaissance.set(existant.dateNaissance ?? '');
    this.classeId.set(existant.classeId);
  }

  enregistrer(): void {
    if (!this.valide() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');

    const requete = {
      matricule: this.matricule().trim().toUpperCase(),
      nom: this.nom().trim(),
      prenom: this.prenom().trim(),
      email: this.email().trim() || null,
      telephone: this.telephone().trim() || null,
      dateNaissance: this.dateNaissance() || null,
      classeId: this.classeId()!,
      // La référence biométrique n'est jamais posée ici : l'enrôlement est un acte
      // distinct, pour ne pas mêler inscription administrative et capture d'empreinte.
      biometricId: this.etudiant()?.biometricId ?? null,
    };

    const existant = this.etudiant();
    const appel = existant
      ? this.service.modifier(existant.id, requete)
      : this.service.creer(requete);

    appel.subscribe({
      next: () => {
        this.envoi.set(false);
        this.enregistre.emit();
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.envoi.set(false);
      },
    });
  }
}
