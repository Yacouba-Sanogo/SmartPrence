import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { messageErreur } from '../../core/api';
import { Classe } from '../../core/models/classe.model';
import { Personnel } from '../../core/models/personnel.model';
import { AnnonceService, TypeAnnonce } from '../../core/services/annonce.service';
import { ClasseService } from '../../core/services/classe.service';
import { PersonnelService } from '../../core/services/personnel.service';
import { IconeComponent } from '../../shared/ui/icone.component';

type Audience = 'TOUS' | 'CLASSE' | 'ENSEIGNANT';

interface Envoi {
  titre: string;
  type: TypeAnnonce;
  cible: string;
  notifies: number;
  heure: Date;
}

const TYPES: { code: TypeAnnonce; libelle: string; aide: string }[] = [
  { code: 'ANNONCE', libelle: 'Annonce', aide: 'Information générale' },
  { code: 'CHANGEMENT', libelle: 'Changement', aide: 'Salle, horaire, organisation' },
  { code: 'EXAMEN', libelle: 'Examen', aide: 'Calendrier, surveillance' },
  { code: 'CONVOCATION', libelle: 'Convocation', aide: 'Réunion, jury, conseil' },
];

/**
 * Rédaction et diffusion d'une annonce aux enseignants.
 *
 * <p>L'aperçu montre la notification telle que l'enseignant la verra sur son téléphone :
 * le titre est tronqué à une ligne et le message à deux, il vaut mieux le voir avant
 * d'envoyer qu'après. Le serveur ne garde pas d'historique des annonces diffusées ; la
 * liste du bas ne porte donc que les envois de cette session.</p>
 */
@Component({
  selector: 'sp-annonce-nouvelle',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconeComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex min-w-0 flex-col gap-1.5">
        <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Annonces aux enseignants</h1>
        <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
          Rédigez un message : il arrive dans l'application mobile des enseignants concernés,
          avec une alerte tant qu'il n'est pas lu.
        </p>
      </div>

      @if (erreur()) {
        <div
          class="flex items-start gap-3 rounded-[12px] border border-danger-line bg-danger-bg px-4 py-3.5"
          role="alert"
        >
          <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-white/60 text-danger">
            <sp-icone nom="alerte" [taille]="17" [epaisseur]="1.7" />
          </span>
          <span class="min-w-0 flex-1 text-[13px] leading-relaxed text-danger">{{ erreur() }}</span>
          <button
            type="button"
            class="grid size-7 shrink-0 place-items-center rounded-lg text-danger/70 hover:text-danger"
            (click)="erreur.set('')"
            aria-label="Masquer"
          >
            <sp-icone nom="fermer" [taille]="15" [epaisseur]="1.9" />
          </button>
        </div>
      }

      @if (succes().texte) {
        <div
          class="flex items-center gap-3 rounded-[12px] px-4 py-3 text-[13px]"
          [class]="succes().alerte ? 'bg-alerte-bg text-alerte' : 'bg-succes-bg text-succes'"
          role="status"
        >
          <sp-icone [nom]="succes().alerte ? 'alerte' : 'coche-cercle'" [taille]="17" [epaisseur]="1.7" />
          {{ succes().texte }}
        </div>
      }

      <div class="grid items-start gap-[18px] lg:grid-cols-[minmax(0,1fr)_340px]">
        <form class="carte flex flex-col gap-5 p-5" (ngSubmit)="diffuser()">
          <fieldset class="m-0 flex flex-col gap-2 border-0 p-0">
            <legend class="mb-1 p-0 text-[13px] font-medium text-ink">Nature du message</legend>
            <div class="grid grid-cols-2 gap-2 xl:grid-cols-4">
              @for (t of types; track t.code) {
                <button
                  type="button"
                  class="flex flex-col items-start gap-0.5 rounded-[12px] border px-3 py-2.5 text-left transition-colors"
                  [class]="
                    type() === t.code
                      ? 'border-royal-500 bg-royal-50 text-royal-700'
                      : 'border-line bg-white text-ink hover:border-royal-300'
                  "
                  [attr.aria-pressed]="type() === t.code"
                  (click)="type.set(t.code)"
                >
                  <span class="text-[13px] font-semibold">{{ t.libelle }}</span>
                  <span class="text-[11px] leading-snug text-ink-subtle">{{ t.aide }}</span>
                </button>
              }
            </div>
          </fieldset>

          <fieldset class="m-0 flex flex-col gap-2 border-0 p-0">
            <legend class="mb-1 p-0 text-[13px] font-medium text-ink">Destinataires</legend>
            <div class="inline-flex w-fit rounded-[10px] bg-canvas p-1" role="radiogroup">
              @for (a of audiences; track a.code) {
                <button
                  type="button"
                  role="radio"
                  class="rounded-[8px] px-3.5 py-1.5 text-[12.5px] font-medium transition-colors"
                  [class]="
                    audience() === a.code
                      ? 'bg-white text-royal-700 shadow-sm'
                      : 'text-ink-muted hover:text-ink'
                  "
                  [attr.aria-checked]="audience() === a.code"
                  (click)="audience.set(a.code)"
                >
                  {{ a.libelle }}
                </button>
              }
            </div>

            @if (audience() === 'CLASSE') {
              <select class="champ" aria-label="Classe" [ngModel]="classeId()" (ngModelChange)="classeId.set($event)" name="classe">
                <option [ngValue]="null">Choisir une classe…</option>
                @for (c of classes(); track c.id) {
                  <option [ngValue]="c.id">
                    {{ c.code }} — {{ c.libelle }} ({{ c.nombreEnseignants }} enseignant(s))
                  </option>
                }
              </select>
            } @else if (audience() === 'ENSEIGNANT') {
              <select class="champ" aria-label="Enseignant" [ngModel]="personnelId()" (ngModelChange)="personnelId.set($event)" name="enseignant">
                <option [ngValue]="null">Choisir un enseignant…</option>
                @for (p of enseignants(); track p.id) {
                  <option [ngValue]="p.id">{{ p.nom }} {{ p.prenom }} — {{ p.matricule }}</option>
                }
              </select>
            }
            <p class="m-0 text-xs text-ink-subtle">{{ resumeCible() }}</p>
          </fieldset>

          <div class="flex flex-col gap-1.5">
            <label class="text-[13px] font-medium text-ink" for="annonce-titre">Titre</label>
            <input
              id="annonce-titre"
              class="champ"
              maxlength="160"
              placeholder="Réunion pédagogique de rentrée"
              [ngModel]="titre()"
              (ngModelChange)="titre.set($event)"
              name="titre"
              autocomplete="off"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <div class="flex items-baseline justify-between">
              <label class="text-[13px] font-medium text-ink" for="annonce-message">Message</label>
              <span class="text-xs tabular-nums" [class]="message().length > 1800 ? 'text-alerte' : 'text-ink-faint'">
                {{ message().length }} / 2000
              </span>
            </div>
            <textarea
              id="annonce-message"
              class="champ min-h-[130px] py-2.5 leading-relaxed"
              maxlength="2000"
              placeholder="Jeudi à 10 h, salle des professeurs. Merci de confirmer votre présence."
              [ngModel]="message()"
              (ngModelChange)="message.set($event)"
              name="message"
            ></textarea>
          </div>

          <div class="flex items-center justify-end gap-2.5 border-t border-line-soft pt-4">
            <button type="button" class="btn btn-secondaire" (click)="reinitialiser()" [disabled]="envoi()">
              Effacer
            </button>
            <button type="submit" class="btn btn-primaire" [disabled]="!valide() || envoi()">
              <sp-icone nom="diffusion" [taille]="16" [epaisseur]="1.7" />
              {{ envoi() ? 'Envoi…' : 'Diffuser' }}
            </button>
          </div>
        </form>

        <aside class="flex flex-col gap-3 lg:sticky lg:top-4" aria-label="Aperçu">
          <span class="text-xs font-semibold uppercase tracking-wide text-ink-subtle">
            Aperçu sur le téléphone
          </span>
          <div class="rounded-[26px] bg-gradient-to-b from-royal-700 to-royal-500 p-4 shadow-lg">
            <p class="m-0 mb-3 text-center text-[11px] font-medium uppercase tracking-wider text-white/70">
              Annonces · 1 non lue
            </p>
            <div class="flex overflow-hidden rounded-[18px] bg-white shadow-md">
              <span class="w-1 shrink-0" [class]="barre()"></span>
              <div class="flex min-w-0 flex-1 gap-3 p-3.5">
                <span class="grid size-10 shrink-0 place-items-center rounded-[13px]" [class]="pastille()">
                  <sp-icone [nom]="iconeType()" [taille]="19" [epaisseur]="1.8" />
                </span>
                <div class="flex min-w-0 flex-1 flex-col gap-0.5">
                  <div class="flex items-center gap-2">
                    <span class="min-w-0 flex-1 truncate text-[14px] font-bold text-ink">
                      {{ titre().trim() || 'Titre de l’annonce' }}
                    </span>
                    <span class="size-2 shrink-0 animate-pulse rounded-full" [class]="point()"></span>
                  </div>
                  <span class="text-[11.5px] text-ink-subtle">{{ typeLibelle() }} · à l'instant</span>
                  <span class="mt-1 line-clamp-2 text-[13px] leading-snug text-ink-muted">
                    {{ message().trim() || 'Le message s’affichera ici, sur deux lignes.' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </div>

      @if (envois().length > 0) {
        <section class="carte overflow-hidden">
          <header class="border-b border-line-soft bg-[#FCFDFE] px-[18px] py-3">
            <h2 class="m-0 text-[15px] font-semibold text-royal-700">Envoyées pendant cette session</h2>
          </header>
          @for (e of envois(); track e.heure.getTime()) {
            <div class="flex items-center gap-3 border-b border-line-soft px-[18px] py-3 last:border-b-0">
              <div class="flex min-w-0 flex-1 flex-col">
                <span class="truncate text-[13.5px] font-medium text-ink">{{ e.titre }}</span>
                <span class="text-xs text-ink-subtle">{{ e.cible }}</span>
              </div>
              <span class="text-xs tabular-nums text-ink-subtle">
                {{ e.notifies }} notifié(s) · {{ e.heure.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }) }}
              </span>
            </div>
          }
        </section>
      }
    </div>
  `,
})
export class AnnonceNouvelleComponent implements OnInit {
  private readonly service = inject(AnnonceService);
  private readonly classeService = inject(ClasseService);
  private readonly personnelService = inject(PersonnelService);

  readonly types = TYPES;
  readonly audiences: { code: Audience; libelle: string }[] = [
    { code: 'TOUS', libelle: 'Tous les enseignants' },
    { code: 'CLASSE', libelle: 'Une classe' },
    { code: 'ENSEIGNANT', libelle: 'Un enseignant' },
  ];

  readonly classes = signal<Classe[]>([]);
  readonly enseignants = signal<Personnel[]>([]);

  readonly type = signal<TypeAnnonce>('ANNONCE');
  readonly audience = signal<Audience>('TOUS');
  readonly classeId = signal<number | null>(null);
  readonly personnelId = signal<string | null>(null);
  readonly titre = signal('');
  readonly message = signal('');

  readonly envoi = signal(false);
  readonly erreur = signal('');
  readonly succes = signal<{ texte: string; alerte: boolean }>({ texte: '', alerte: false });
  readonly envois = signal<Envoi[]>([]);

  readonly valide = computed(() => {
    if (!this.titre().trim() || !this.message().trim()) return false;
    if (this.audience() === 'CLASSE') return this.classeId() !== null;
    if (this.audience() === 'ENSEIGNANT') return this.personnelId() !== null;
    return true;
  });

  readonly resumeCible = computed(() => {
    switch (this.audience()) {
      case 'TOUS':
        return `${this.enseignants().length} enseignant(s) actif(s) dans l'établissement.`;
      case 'CLASSE': {
        const c = this.classes().find((x) => x.id === this.classeId());
        return c
          ? c.nombreEnseignants === 0
            ? 'Cette classe n’a aucun enseignant rattaché : personne ne recevrait l’annonce.'
            : `${c.nombreEnseignants} enseignant(s) rattaché(s) à cette classe.`
          : 'Seuls les enseignants rattachés à la classe la recevront.';
      }
      default:
        return 'Le message ne parviendra qu’à cette personne.';
    }
  });

  readonly typeLibelle = computed(() => TYPES.find((t) => t.code === this.type())?.libelle ?? '');

  /** Le changement est la seule teinte chaude : il demande une action, pas une lecture. */
  readonly barre = computed(() => this.teinte('bg-alerte', 'bg-danger', 'bg-royal-600'));
  readonly point = computed(() => this.teinte('bg-alerte', 'bg-danger', 'bg-royal-600'));
  readonly pastille = computed(() =>
    this.teinte('bg-alerte-bg text-alerte', 'bg-danger-bg text-danger', 'bg-royal-50 text-royal-600'),
  );
  readonly iconeType = computed(() => (this.type() === 'CHANGEMENT' ? 'alerte' : 'diffusion'));

  private teinte(changement: string, examen: string, defaut: string): string {
    if (this.type() === 'CHANGEMENT') return changement;
    if (this.type() === 'EXAMEN') return examen;
    return defaut;
  }

  ngOnInit(): void {
    forkJoin({
      classes: this.classeService.lister(),
      enseignants: this.personnelService.lister({ type: 'ENSEIGNANT', actifsSeul: true }),
    }).subscribe({
      next: ({ classes, enseignants }) => {
        this.classes.set(classes);
        this.enseignants.set(enseignants);
      },
      error: (e: unknown) => this.erreur.set(messageErreur(e)),
    });
  }

  reinitialiser(): void {
    this.titre.set('');
    this.message.set('');
    this.classeId.set(null);
    this.personnelId.set(null);
    this.audience.set('TOUS');
    this.type.set('ANNONCE');
  }

  diffuser(): void {
    if (!this.valide() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');
    this.succes.set({ texte: '', alerte: false });

    const audience = this.audience();
    const cible =
      audience === 'CLASSE'
        ? (this.classes().find((c) => c.id === this.classeId())?.code ?? 'Classe')
        : audience === 'ENSEIGNANT'
          ? (() => {
              const p = this.enseignants().find((x) => x.id === this.personnelId());
              return p ? `${p.nom} ${p.prenom}` : 'Enseignant';
            })()
          : 'Tous les enseignants';
    const titre = this.titre().trim();

    this.service
      .diffuser({
        titre,
        message: this.message().trim(),
        type: this.type(),
        classeId: audience === 'CLASSE' ? this.classeId() : null,
        personnelId: audience === 'ENSEIGNANT' ? this.personnelId() : null,
      })
      .subscribe({
        next: (notifies) => {
          this.envoi.set(false);
          // Zéro notifié n'est pas une erreur du serveur, mais l'annonce n'est arrivée
          // à personne : on le dit au lieu d'afficher un succès trompeur.
          this.succes.set(
            notifies === 0
              ? {
                  texte:
                    'Annonce enregistrée, mais aucun destinataire n’a de compte mobile : personne ne l’a reçue.',
                  alerte: true,
                }
              : { texte: `Annonce diffusée à ${notifies} enseignant(s).`, alerte: false },
          );
          this.envois.update((liste) => [
            { titre, type: this.type(), cible, notifies, heure: new Date() },
            ...liste,
          ]);
          if (notifies > 0) this.reinitialiser();
        },
        error: (e: unknown) => {
          this.erreur.set(messageErreur(e));
          this.envoi.set(false);
        },
      });
  }
}
