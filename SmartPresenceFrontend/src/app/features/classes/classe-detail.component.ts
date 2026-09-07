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
import { messageErreur } from '../../core/api';
import {
  Classe,
  ClasseDetail,
  EnseignantDeClasse,
  EtudiantDeClasse,
  initiales,
  libellePromotion,
} from '../../core/models/classe.model';
import { ClasseService } from '../../core/services/classe.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';
import { ModaleComponent } from '../../shared/ui/modale.component';

/**
 * Composition d'une classe : son équipe pédagogique et son effectif.
 *
 * <p>La couverture d'enrôlement est mise en avant parce qu'elle conditionne tout le
 * reste : un étudiant sans empreinte associée ne sera jamais relevé par un lecteur, et
 * ses absences ne voudront rien dire.</p>
 */
@Component({
  selector: 'sp-classe-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModaleComponent, IconeComponent, EtatComponent],
  template: `
    <sp-modale
      [titre]="classe().libelle"
      [sousTitre]="classe().code + ' · ' + promotion()"
      icone="classes"
      (fermer)="fermer.emit()"
    >
      <div class="max-h-[70vh] overflow-y-auto">
        @if (chargement()) {
          <sp-etat titre="Chargement de la composition…" icone="horloge" />
        } @else if (erreur()) {
          <sp-etat
            variante="erreur"
            icone="alerte"
            titre="Composition indisponible"
            [detail]="erreur()"
            actionLibelle="Réessayer"
            (action)="charger()"
          />
        } @else {
          <div class="flex flex-col gap-5 px-5 py-5">
            <div class="grid grid-cols-3 gap-3">
              <div class="rounded-[11px] bg-canvas px-3.5 py-3">
                <div class="text-[11px] text-ink-muted">Inscrits</div>
                <div class="num mt-1 text-[22px] font-semibold leading-none text-ink">
                  {{ etudiants().length }}
                </div>
              </div>
              <div class="rounded-[11px] bg-canvas px-3.5 py-3">
                <div class="text-[11px] text-ink-muted">Enrôlés</div>
                <div class="num mt-1 text-[22px] font-semibold leading-none text-succes">
                  {{ enroles() }}
                </div>
              </div>
              <div class="rounded-[11px] bg-canvas px-3.5 py-3">
                <div class="text-[11px] text-ink-muted">Enseignants</div>
                <div class="num mt-1 text-[22px] font-semibold leading-none text-royal-600">
                  {{ enseignants().length }}
                </div>
              </div>
            </div>

            @if (nonEnroles() > 0) {
              <p
                class="m-0 flex items-start gap-2.5 rounded-[11px] border border-alerte-line bg-alerte-bg px-3.5 py-3 text-[12.5px] leading-relaxed text-alerte"
              >
                <sp-icone nom="empreinte" [taille]="16" [epaisseur]="1.7" class="mt-px shrink-0" />
                <span>
                  {{ nonEnroles() }} étudiant(s) sans empreinte enrôlée. Aucun lecteur ne pourra
                  les identifier : leurs absences de relevé ne diront rien de leur assiduité.
                </span>
              </p>
            }

            @if (enseignants().length === 0) {
              <p
                class="m-0 flex items-start gap-2.5 rounded-[11px] border border-line bg-canvas px-3.5 py-3 text-[12.5px] leading-relaxed text-ink-subtle"
              >
                <sp-icone nom="info" [taille]="16" [epaisseur]="1.7" class="mt-px shrink-0" />
                <span>
                  Aucun enseignant rattaché : cette classe n'apparaîtra sur l'application mobile
                  d'aucun enseignant.
                </span>
              </p>
            } @else {
              <section class="flex flex-col gap-2">
                <h3 class="m-0 text-[11.5px] font-semibold tracking-wide text-ink-muted">
                  Équipe pédagogique
                </h3>
                <div class="flex flex-wrap gap-2">
                  @for (agent of enseignants(); track agent.id) {
                    <span
                      class="flex items-center gap-2.5 rounded-[10px] border border-line bg-white py-1.5 pl-1.5 pr-3"
                    >
                      <span
                        class="grid size-7 shrink-0 place-items-center rounded-lg bg-royal-50 text-[11px] font-semibold text-royal-600"
                      >
                        {{ initialesDe(agent) }}
                      </span>
                      <span class="flex flex-col">
                        <span class="text-[12.5px] font-medium leading-tight text-ink">
                          {{ agent.prenom }} {{ agent.nom }}
                        </span>
                        <span class="num text-[10.5px] leading-tight text-ink-faint">
                          {{ agent.matricule }}
                        </span>
                      </span>
                    </span>
                  }
                </div>
              </section>
            }

            <section class="flex flex-col gap-2">
              <h3 class="m-0 text-[11.5px] font-semibold tracking-wide text-ink-muted">Effectif</h3>

              @if (etudiants().length === 0) {
                <p
                  class="m-0 rounded-[11px] border border-line bg-canvas px-3.5 py-4 text-center text-[12.5px] text-ink-subtle"
                >
                  Aucun étudiant inscrit dans cette classe.
                </p>
              } @else {
                <div class="overflow-hidden rounded-[11px] border border-line">
                  <table>
                    <thead>
                      <tr>
                        <th class="th !pl-3.5">Étudiant</th>
                        <th class="th">Empreinte</th>
                        <th class="th !pr-3.5">Accès mobile</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (etudiant of etudiantsTries(); track etudiant.id) {
                        <tr>
                          <td class="td !pl-3.5">
                            <div class="flex items-center gap-2.5">
                              <span
                                class="grid size-7 shrink-0 place-items-center rounded-lg text-[10.5px] font-semibold"
                                [class]="
                                  etudiant.enrole
                                    ? 'bg-succes-bg text-succes'
                                    : 'bg-alerte-bg text-alerte'
                                "
                              >
                                {{ initialesDe(etudiant) }}
                              </span>
                              <span class="flex min-w-0 flex-col">
                                <span class="truncate text-[13px] font-medium leading-tight text-ink">
                                  {{ etudiant.prenom }} {{ etudiant.nom }}
                                </span>
                                <span class="num truncate text-[10.5px] leading-tight text-ink-faint">
                                  {{ etudiant.matricule }}
                                </span>
                              </span>
                            </div>
                          </td>
                          <td class="td">
                            @if (etudiant.enrole) {
                              <span class="badge bg-succes-bg text-succes">
                                <sp-icone nom="empreinte" [taille]="12" [epaisseur]="1.9" />
                                Enrôlé
                              </span>
                            } @else {
                              <span class="badge bg-alerte-bg text-alerte">Non enrôlé</span>
                            }
                          </td>
                          <td class="td !pr-3.5">
                            @if (etudiant.compteOuvert) {
                              <span class="text-[12.5px] text-ink-muted">Ouvert</span>
                            } @else {
                              <span class="text-[12.5px] text-ink-faint">—</span>
                            }
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            </section>
          </div>
        }
      </div>
    </sp-modale>
  `,
})
export class ClasseDetailComponent implements OnInit {
  private readonly service = inject(ClasseService);

  readonly classe = input.required<Classe>();
  readonly fermer = output<void>();

  readonly detail = signal<ClasseDetail | null>(null);
  readonly chargement = signal(false);
  readonly erreur = signal('');

  readonly enseignants = computed<EnseignantDeClasse[]>(() => this.detail()?.enseignants ?? []);
  readonly etudiants = computed<EtudiantDeClasse[]>(() => this.detail()?.etudiants ?? []);

  /** Même ordre que le backend : nom puis prénom, sans quoi les homonymes se mélangent. */
  readonly etudiantsTries = computed(() =>
    [...this.etudiants()].sort(
      (a, b) =>
        a.nom.localeCompare(b.nom, 'fr') || a.prenom.localeCompare(b.prenom, 'fr'),
    ),
  );

  readonly enroles = computed(() => this.etudiants().filter((e) => e.enrole).length);
  readonly nonEnroles = computed(() => this.etudiants().length - this.enroles());

  readonly promotion = computed(() => libellePromotion(this.classe()));

  /**
   * Le chargement attend {@link ngOnInit}, pas le constructeur.
   *
   * Une entrée {@code input.required} n'est pas encore renseignée pendant la
   * construction : lire {@link classe} à cet instant lève NG0950 et la modale ne
   * s'ouvre jamais. Le compilateur ne le détecte pas — seule l'ouverture le montre.
   */
  ngOnInit(): void {
    this.charger();
  }

  initialesDe(personne: { nom: string; prenom: string }): string {
    return initiales(personne);
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.service.detail(this.classe().id).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreur(erreur));
        this.chargement.set(false);
      },
    });
  }
}
