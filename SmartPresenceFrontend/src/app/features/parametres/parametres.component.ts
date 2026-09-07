import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import { ParametresEtablissement, formaterHeure } from '../../core/models/pointage.model';
import { ParametreService, fuseauxDisponibles } from '../../core/services/parametre.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';

/**
 * Configuration de l'établissement.
 *
 * <p>Le bloc « Horaires de service » n'est pas décoratif : l'heure d'ouverture et le
 * seuil de tolérance qu'on règle ici décident, pour chaque agent, si son arrivée est
 * comptée à l'heure ou en retard. L'aperçu affiche donc en continu la conséquence du
 * réglage, plutôt que de laisser l'utilisateur faire l'addition de tête.</p>
 */
@Component({
  selector: 'sp-parametres',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent, EtatComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-0 flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Paramètres</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Identité de l'établissement et règles horaires appliquées au pointage du personnel.
          </p>
        </div>
      </div>

      @if (chargement()) {
        <div class="carte"><sp-etat titre="Chargement des paramètres…" icone="horloge" /></div>
      } @else if (erreur()) {
        <div class="carte">
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Impossible de charger les paramètres"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        </div>
      } @else {
        @if (messageAction()) {
          <div
            class="flex items-start gap-3 rounded-[12px] border px-4 py-3.5"
            [class]="
              actionEnEchec()
                ? 'border-danger-line bg-danger-bg'
                : 'border-succes-line bg-succes-bg'
            "
            role="status"
          >
            <span
              class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-white/60"
              [class]="actionEnEchec() ? 'text-danger' : 'text-succes'"
            >
              <sp-icone [nom]="actionEnEchec() ? 'alerte' : 'coche-cercle'" [taille]="17" [epaisseur]="1.7" />
            </span>
            <span
              class="min-w-0 flex-1 text-[13px] leading-relaxed"
              [class]="actionEnEchec() ? 'text-danger' : 'text-succes'"
            >
              {{ messageAction() }}
            </span>
            <button
              type="button"
              class="grid size-7 shrink-0 place-items-center rounded-lg opacity-70 hover:opacity-100"
              [class]="actionEnEchec() ? 'text-danger' : 'text-succes'"
              (click)="messageAction.set('')"
              aria-label="Masquer"
            >
              <sp-icone nom="fermer" [taille]="15" [epaisseur]="1.9" />
            </button>
          </div>
        }

        <form (ngSubmit)="enregistrer()" class="flex flex-col gap-4">
          <!-- Identité -->
          <section class="carte overflow-hidden">
            <header class="flex items-center gap-2.5 border-b border-line-soft px-4 py-3.5">
              <span class="grid size-7 place-items-center rounded-lg bg-line-faint text-[#4A5470]">
                <sp-icone nom="salles" [taille]="16" [epaisseur]="1.7" />
              </span>
              <h2 class="m-0 text-[14.5px] font-semibold text-ink">Identité de l'établissement</h2>
            </header>

            <div class="grid grid-cols-1 gap-4 px-4 py-4 sm:grid-cols-2">
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

              <label class="flex flex-col gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Sigle</span>
                <span class="champ !h-11">
                  <input
                    type="text"
                    name="sigle"
                    class="saisie"
                    placeholder="USP"
                    [ngModel]="sigle()"
                    (ngModelChange)="sigle.set($event)"
                  />
                </span>
              </label>

              <label class="flex flex-col gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Email</span>
                <span class="champ !h-11">
                  <input
                    type="email"
                    name="email"
                    class="saisie"
                    placeholder="contact@univ.ml"
                    [ngModel]="email()"
                    (ngModelChange)="email.set($event)"
                  />
                </span>
              </label>

              <label class="flex flex-col gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Téléphone</span>
                <span class="champ !h-11">
                  <input
                    type="tel"
                    name="telephone"
                    class="saisie num"
                    placeholder="+223 20 00 00 00"
                    [ngModel]="telephone()"
                    (ngModelChange)="telephone.set($event)"
                  />
                </span>
              </label>
            </div>
          </section>

          <!-- Horaires -->
          <section class="carte overflow-hidden">
            <header class="flex items-center gap-2.5 border-b border-line-soft px-4 py-3.5">
              <span class="grid size-7 place-items-center rounded-lg bg-royal-50 text-royal-600">
                <sp-icone nom="horloge" [taille]="16" [epaisseur]="1.7" />
              </span>
              <h2 class="m-0 text-[14.5px] font-semibold text-ink">Horaires de service</h2>
              <span class="rounded-full bg-canvas px-2.5 py-0.5 text-[11.5px] text-ink-subtle">
                gouverne le calcul des retards
              </span>
            </header>

            <div class="grid grid-cols-1 gap-4 px-4 py-4 sm:grid-cols-2 lg:grid-cols-4">
              <label class="flex flex-col gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
                  Prise de service
                </span>
                <span class="champ !h-11">
                  <input
                    type="time"
                    name="heureOuverture"
                    class="saisie num"
                    required
                    [ngModel]="heureOuverture()"
                    (ngModelChange)="heureOuverture.set($event)"
                  />
                </span>
              </label>

              <label class="flex flex-col gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Fin de service</span>
                <span class="champ !h-11" [class.!border-danger]="horairesIncoherents()">
                  <input
                    type="time"
                    name="heureFermeture"
                    class="saisie num"
                    required
                    [ngModel]="heureFermeture()"
                    (ngModelChange)="heureFermeture.set($event)"
                  />
                </span>
                @if (horairesIncoherents()) {
                  <span class="text-[11px] text-danger">
                    La fin doit être postérieure à la prise de service.
                  </span>
                }
              </label>

              <label class="flex flex-col gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">
                  Tolérance de retard
                </span>
                <span class="champ !h-11">
                  <input
                    type="number"
                    name="seuilRetardMinutes"
                    class="saisie num"
                    min="0"
                    max="120"
                    required
                    [ngModel]="seuilRetard()"
                    (ngModelChange)="seuilRetard.set($event)"
                  />
                  <span class="text-[11.5px] text-ink-faint">min</span>
                </span>
              </label>

              <label class="flex flex-col gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Fuseau horaire</span>
                <select
                  class="champ !h-11 text-ink"
                  name="fuseauHoraire"
                  [ngModel]="fuseau()"
                  (ngModelChange)="fuseau.set($event)"
                >
                  @for (zone of fuseaux(); track zone) {
                    <option [ngValue]="zone">{{ zone }}</option>
                  }
                </select>
              </label>
            </div>

            <!-- Conséquence du réglage -->
            <div class="flex items-start gap-3 border-t border-line-soft bg-[#F5F8FF] px-4 py-3.5">
              <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-royal-100 text-royal-600">
                <sp-icone nom="info" [taille]="17" [epaisseur]="1.7" />
              </span>
              <span class="min-w-0 text-xs leading-relaxed text-[#33447A]">
                Avec ce réglage, une arrivée après
                <span class="num font-semibold text-royal-600">{{ limiteRetard() }}</span>
                est comptée en <span class="font-semibold">retard</span>. Le retard affiché reste
                mesuré depuis <span class="num font-semibold">{{ heureOuverture() || '—' }}</span> :
                un agent arrivé à
                <span class="num font-semibold">{{ exempleArrivee() }}</span> apparaît donc
                <span class="font-semibold">{{ exempleStatut() }}</span>, avec
                <span class="num font-semibold">{{ exempleRetard() }}</span> de retard.
              </span>
            </div>
          </section>

          <div class="flex flex-wrap items-center gap-2.5">
            <span class="flex-1"></span>
            <button
              type="button"
              class="btn btn-secondaire"
              (click)="reinitialiser()"
              [disabled]="!modifie() || envoi()"
            >
              Annuler les modifications
            </button>
            <button type="submit" class="btn btn-primaire" [disabled]="!valide() || !modifie() || envoi()">
              <sp-icone nom="coche" [taille]="15" [epaisseur]="2.1" />
              {{ envoi() ? 'Enregistrement…' : 'Enregistrer' }}
            </button>
          </div>
        </form>
      }
    </div>
  `,
})
export class ParametresComponent {
  private readonly service = inject(ParametreService);

  private readonly initial = signal<ParametresEtablissement | null>(null);

  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly envoi = signal(false);
  readonly messageAction = signal('');
  readonly actionEnEchec = signal(false);

  readonly nom = signal('');
  readonly sigle = signal('');
  readonly email = signal('');
  readonly telephone = signal('');
  readonly fuseau = signal('Africa/Bamako');
  readonly seuilRetard = signal(15);
  readonly heureOuverture = signal('08:00');
  readonly heureFermeture = signal('17:00');

  readonly fuseaux = computed(() => fuseauxDisponibles(this.fuseau()));

  readonly horairesIncoherents = computed(() => {
    const ouverture = this.heureOuverture();
    const fermeture = this.heureFermeture();
    if (!ouverture || !fermeture) return false;
    return fermeture <= ouverture;
  });

  readonly valide = computed(
    () =>
      this.nom().trim().length > 0 &&
      this.heureOuverture().length > 0 &&
      this.heureFermeture().length > 0 &&
      !this.horairesIncoherents() &&
      this.seuilRetard() >= 0,
  );

  /** Rien à enregistrer tant que rien n'a bougé — le bouton reste inactif. */
  readonly modifie = computed(() => {
    const initial = this.initial();
    if (!initial) return false;
    return (
      this.nom() !== (initial.nom ?? '') ||
      this.sigle() !== (initial.sigle ?? '') ||
      this.email() !== (initial.email ?? '') ||
      this.telephone() !== (initial.telephone ?? '') ||
      this.fuseau() !== initial.fuseauHoraire ||
      this.seuilRetard() !== initial.seuilRetardMinutes ||
      this.heureOuverture() !== formaterHeure(initial.heureOuverture) ||
      this.heureFermeture() !== formaterHeure(initial.heureFermeture)
    );
  });

  /** Heure au-delà de laquelle une arrivée bascule en retard. */
  readonly limiteRetard = computed(() => {
    const [heures, minutes] = this.heureOuverture().split(':').map(Number);
    if (Number.isNaN(heures) || Number.isNaN(minutes)) return '—';
    const total = heures * 60 + minutes + (this.seuilRetard() || 0);
    const h = Math.floor(total / 60) % 24;
    return `${String(h).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
  });

  /** Arrivée d'exemple : 10 minutes après l'ouverture, pour illustrer la tolérance. */
  private readonly exempleMinutes = computed(() => {
    const [heures, minutes] = this.heureOuverture().split(':').map(Number);
    if (Number.isNaN(heures) || Number.isNaN(minutes)) return null;
    return heures * 60 + minutes + 10;
  });

  readonly exempleArrivee = computed(() => {
    const total = this.exempleMinutes();
    if (total === null) return '—';
    const h = Math.floor(total / 60) % 24;
    return `${String(h).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
  });

  readonly exempleRetard = computed(() => (this.exempleMinutes() === null ? '—' : '10 min'));

  readonly exempleStatut = computed(() => ((this.seuilRetard() || 0) >= 10 ? 'présent' : 'en retard'));

  constructor() {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.service.consulter().subscribe({
      next: (parametres) => {
        this.appliquer(parametres);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }

  reinitialiser(): void {
    const initial = this.initial();
    if (initial) this.appliquer(initial);
    this.messageAction.set('');
  }

  enregistrer(): void {
    if (!this.valide() || !this.modifie() || this.envoi()) return;
    this.envoi.set(true);
    this.messageAction.set('');

    this.service
      .enregistrer({
        nom: this.nom().trim(),
        sigle: this.sigle().trim() || null,
        email: this.email().trim() || null,
        telephone: this.telephone().trim() || null,
        fuseauHoraire: this.fuseau(),
        seuilRetardMinutes: this.seuilRetard(),
        // Le backend attend HH:mm:ss ; un champ <input type="time"> ne fournit que HH:mm.
        heureOuverture: `${this.heureOuverture()}:00`,
        heureFermeture: `${this.heureFermeture()}:00`,
      })
      .subscribe({
        next: (parametres) => {
          this.appliquer(parametres);
          this.actionEnEchec.set(false);
          this.messageAction.set(
            `Paramètres enregistrés — une arrivée après ${this.limiteRetard()} est désormais comptée en retard.`,
          );
          this.envoi.set(false);
        },
        error: (erreur: unknown) => {
          this.actionEnEchec.set(true);
          this.messageAction.set(messageErreur(erreur));
          this.envoi.set(false);
        },
      });
  }

  private appliquer(parametres: ParametresEtablissement): void {
    this.initial.set(parametres);
    this.nom.set(parametres.nom ?? '');
    this.sigle.set(parametres.sigle ?? '');
    this.email.set(parametres.email ?? '');
    this.telephone.set(parametres.telephone ?? '');
    this.fuseau.set(parametres.fuseauHoraire);
    this.seuilRetard.set(parametres.seuilRetardMinutes);
    this.heureOuverture.set(formaterHeure(parametres.heureOuverture));
    this.heureFermeture.set(formaterHeure(parametres.heureFermeture));
  }
}
