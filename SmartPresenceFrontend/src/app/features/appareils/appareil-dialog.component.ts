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
import {
  Appareil,
  Salle,
  USAGES_APPAREIL,
  UsageAppareil,
  adresseMacValide,
  descriptionUsage,
  genererCleApi,
  libelleUsage,
} from '../../core/models/appareil.model';
import { AppareilService } from '../../core/services/appareil.service';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';

/**
 * Déclaration ou modification d'un lecteur ESP32.
 *
 * <p>La clé d'API est tirée au hasard par l'interface et affichée une seule fois : le
 * backend n'en conserve que l'empreinte et ne pourra jamais la restituer.</p>
 */
@Component({
  selector: 'sp-appareil-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ModaleComponent, IconeComponent],
  template: `
    <sp-modale
      [titre]="modeEdition() ? 'Modifier le lecteur' : 'Déclarer un lecteur'"
      [sousTitre]="
        modeEdition()
          ? 'Le remplacement de la clé invalide immédiatement l\\'ancienne'
          : 'Enregistrer un nouvel appareil ESP32 dans le parc'
      "
      icone="appareils"
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
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Nom du lecteur</span>
              <span class="champ !h-11">
                <input
                  type="text"
                  name="nom"
                  class="saisie"
                  placeholder="ESP32-Entrée-Principale"
                  required
                  [ngModel]="nom()"
                  (ngModelChange)="nom.set($event)"
                />
              </span>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Adresse MAC</span>
              <span class="champ !h-11" [class.!border-danger]="macInvalide()">
                <input
                  type="text"
                  name="adresseMac"
                  class="saisie num uppercase"
                  placeholder="AA:BB:CC:DD:EE:FF"
                  required
                  [ngModel]="adresseMac()"
                  (ngModelChange)="adresseMac.set($event)"
                />
              </span>
              @if (macInvalide()) {
                <span class="text-[11px] text-danger">Format attendu : AA:BB:CC:DD:EE:FF</span>
              }
            </label>
          </div>

          <fieldset class="m-0 flex flex-col gap-1.5 border-0 p-0">
            <legend class="mb-1.5 p-0 text-[11.5px] font-semibold tracking-wide text-ink-muted">
              Vocation du lecteur
            </legend>
            <div class="flex flex-col gap-2">
              @for (option of usages; track option) {
                <button
                  type="button"
                  (click)="usage.set(option)"
                  [attr.aria-pressed]="usage() === option"
                  class="flex items-center gap-3 rounded-[9px] border px-3.5 py-2.5 text-left transition-colors"
                  [class]="
                    usage() === option
                      ? 'border-[1.5px] border-royal-600 bg-[#F5F8FF]'
                      : 'border-line bg-white hover:bg-canvas'
                  "
                >
                  <span class="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span
                      class="text-[13.5px]"
                      [class]="usage() === option ? 'font-semibold text-ink' : 'text-ink-muted'"
                    >
                      {{ libelleUsage(option) }}
                    </span>
                    <span class="text-[11.5px] text-ink-faint">{{ descriptionUsage(option) }}</span>
                  </span>
                  @if (usage() === option) {
                    <span class="grid size-[18px] shrink-0 place-items-center rounded-full bg-royal-600 text-white">
                      <sp-icone nom="coche" [taille]="11" [epaisseur]="3" />
                    </span>
                  } @else {
                    <span class="size-[18px] shrink-0 rounded-full border-[1.5px] border-[#D3DAE8]"></span>
                  }
                </button>
              }
            </div>
          </fieldset>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1.5">
              <span class="flex items-baseline gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Salle</span>
                <span class="text-[11px] text-ink-faint">facultatif</span>
              </span>
              <select
                class="champ !h-11 text-ink"
                name="salleId"
                [ngModel]="salleId()"
                (ngModelChange)="salleId.set($event)"
              >
                <option [ngValue]="null">Aucune salle</option>
                @for (salle of salles(); track salle.id) {
                  <option [ngValue]="salle.id">{{ salle.code }} — {{ salle.nom }}</option>
                }
              </select>
            </label>

            <label class="flex flex-col gap-1.5">
              <span class="flex items-baseline gap-1.5">
                <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Firmware</span>
                <span class="text-[11px] text-ink-faint">facultatif</span>
              </span>
              <span class="champ !h-11">
                <input
                  type="text"
                  name="versionFirmware"
                  class="saisie num"
                  placeholder="1.2.0"
                  [ngModel]="versionFirmware()"
                  (ngModelChange)="versionFirmware.set($event)"
                />
              </span>
            </label>
          </div>

          <!-- Clé d'API -->
          <div class="flex flex-col gap-2">
            <span class="flex items-baseline gap-2">
              <span class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Clé d'API</span>
              <span class="text-[11px] text-ink-faint">à copier dans le firmware</span>
            </span>
            <div class="flex items-center gap-2">
              <span class="champ !h-11 flex-1">
                <input
                  type="text"
                  name="apiKey"
                  class="saisie num text-[12px]"
                  readonly
                  [ngModel]="cleApi()"
                  (ngModelChange)="cleApi.set($event)"
                />
              </span>
              <button type="button" class="btn btn-secondaire !h-11" (click)="regenerer()">
                <sp-icone nom="rafraichir" [taille]="15" [epaisseur]="1.7" />
                Régénérer
              </button>
              <button type="button" class="btn btn-discret !h-11" (click)="copier()">
                {{ copiee() ? 'Copiée' : 'Copier' }}
              </button>
            </div>

            <p
              class="m-0 flex items-start gap-3 rounded-[11px] border border-alerte-line bg-[#FFF9EC] px-4 py-3 text-xs leading-relaxed text-[#6E4806]"
            >
              <span class="grid size-8 shrink-0 place-items-center rounded-[9px] bg-alerte-bg text-alerte">
                <sp-icone nom="alerte" [taille]="16" [epaisseur]="1.7" />
              </span>
              <span>
                Notez cette clé maintenant : le serveur n'en conserve que l'empreinte et ne pourra
                jamais vous la redonner.
                @if (modeEdition()) {
                  <span class="font-semibold"> Enregistrer remplacera la clé actuelle du lecteur.</span>
                }
              </span>
            </p>
          </div>
        </div>

        <footer class="flex items-center gap-2.5 border-t border-line-soft bg-[#FCFDFE] px-5 py-4">
          <span class="flex-1"></span>
          <button type="button" class="btn btn-secondaire" (click)="fermer.emit()">Annuler</button>
          <button type="submit" class="btn btn-primaire" [disabled]="!valide() || envoi()">
            <sp-icone nom="coche" [taille]="15" [epaisseur]="2.1" />
            {{ envoi() ? 'Enregistrement…' : modeEdition() ? 'Enregistrer' : 'Déclarer le lecteur' }}
          </button>
        </footer>
      </form>
    </sp-modale>
  `,
})
export class AppareilDialogComponent implements OnInit {
  private readonly service = inject(AppareilService);

  protected readonly usages = USAGES_APPAREIL;
  protected readonly libelleUsage = libelleUsage;
  protected readonly descriptionUsage = descriptionUsage;

  /** Appareil à modifier, ou `null` pour une déclaration. */
  readonly appareil = input<Appareil | null>(null);

  readonly fermer = output<void>();
  readonly enregistre = output<void>();

  readonly salles = signal<Salle[]>([]);
  readonly nom = signal('');
  readonly adresseMac = signal('');
  readonly usage = signal<UsageAppareil>('PERSONNEL');
  readonly salleId = signal<number | null>(null);
  readonly versionFirmware = signal('');
  readonly cleApi = signal(genererCleApi());
  readonly copiee = signal(false);
  readonly envoi = signal(false);
  readonly erreur = signal('');

  readonly modeEdition = computed(() => this.appareil() !== null);

  readonly macInvalide = computed(
    () => this.adresseMac().trim().length > 0 && !adresseMacValide(this.adresseMac()),
  );

  readonly valide = computed(
    () =>
      this.nom().trim().length > 0 &&
      adresseMacValide(this.adresseMac()) &&
      this.cleApi().length >= 16,
  );

  constructor() {
    this.service.listerSalles().subscribe({
      next: (salles) => this.salles.set(salles ?? []),
      // L'absence de salles n'empêche pas de déclarer un lecteur d'entrée.
      error: () => this.salles.set([]),
    });
  }

  ngOnInit(): void {
    const existant = this.appareil();
    if (!existant) return;
    this.nom.set(existant.nom);
    this.adresseMac.set(existant.adresseMac);
    this.usage.set(existant.usage ?? 'MIXTE');
    this.salleId.set(existant.salleId);
    this.versionFirmware.set(existant.versionFirmware ?? '');
  }

  regenerer(): void {
    this.cleApi.set(genererCleApi());
    this.copiee.set(false);
  }

  copier(): void {
    void navigator.clipboard?.writeText(this.cleApi()).then(() => this.copiee.set(true));
  }

  enregistrer(): void {
    if (!this.valide() || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');

    const requete = {
      nom: this.nom().trim(),
      adresseMac: this.adresseMac().trim().toUpperCase(),
      apiKey: this.cleApi(),
      usage: this.usage(),
      versionFirmware: this.versionFirmware().trim() || null,
      salleId: this.salleId(),
    };

    const existant = this.appareil();
    const requeteHttp = existant
      ? this.service.modifier(existant.id, requete)
      : this.service.creer(requete);

    requeteHttp.subscribe({
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
