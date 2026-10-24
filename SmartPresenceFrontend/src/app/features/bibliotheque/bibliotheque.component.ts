import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { messageErreur } from '../../core/api';
import { Filiere } from '../../core/models/classe.model';
import { Etudiant } from '../../core/models/etudiant.model';
import { AttenteService } from '../../core/services/attente.service';
import { AuthService } from '../../core/services/auth.service';
import {
  BibliothequeService,
  DocumentBibliotheque,
  Emprunt,
  Livre,
  LivreRequest,
  SyntheseBibliotheque,
  TypeDocument,
} from '../../core/services/bibliotheque.service';
import { ClasseService } from '../../core/services/classe.service';
import { EtudiantService } from '../../core/services/etudiant.service';
import { EtatComponent } from '../../shared/ui/etat.component';
import { IconeComponent } from '../../shared/ui/icone.component';

type Onglet = 'moderer' | 'documents' | 'livres' | 'emprunts';

const LIVRE_VIDE: LivreRequest = { titre: '', auteur: '', exemplaires: 1 };

/**
 * Bibliothèque : dépôts à modérer, mémoires et sujets publiés, fonds de livres physiques et emprunts.
 *
 * <p>Le superviseur consulte seulement ; l'administration et la scolarité tiennent le fonds, décident
 * des dépôts d'étudiants (le refus exige un motif) et enregistrent prêts, retours et prolongations.</p>
 */
@Component({
  selector: 'sp-bibliotheque',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DatePipe, IconeComponent, EtatComponent],
  template: `
    <div class="flex flex-col gap-[18px]">
      <div class="flex flex-wrap items-end gap-5">
        <div class="flex min-w-[min(100%,280px)] flex-1 flex-col gap-1.5">
          <h1 class="m-0 text-[25px] font-semibold tracking-tight text-ink">Bibliothèque</h1>
          <p class="m-0 text-[13px] leading-relaxed text-ink-muted">
            Mémoires et anciens sujets en ligne, livres du fonds et emprunts. Les dépôts des
            étudiants n'apparaissent qu'après votre décision.
          </p>
        </div>
        <button type="button" class="btn btn-secondaire" (click)="charger()" [disabled]="chargement()">
          <sp-icone nom="rafraichir" [taille]="16" [epaisseur]="1.7" />
          Actualiser
        </button>
      </div>

      @if (synthese(); as s) {
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div class="carte px-4 py-3">
            <div class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Dépôts à modérer</div>
            <div class="num text-[24px] font-semibold text-royal-700">{{ s.documentsEnAttente }}</div>
          </div>
          <div class="carte px-4 py-3">
            <div class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Livres sortis</div>
            <div class="num text-[24px] font-semibold text-royal-700">{{ s.empruntsEnCours }}</div>
          </div>
          <div class="carte px-4 py-3">
            <div class="text-[11.5px] font-semibold tracking-wide text-ink-muted">Livres en retard</div>
            <div class="num text-[24px] font-semibold" [class.text-danger]="s.empruntsEnRetard > 0">
              {{ s.empruntsEnRetard }}
            </div>
          </div>
        </div>
      }

      <div class="flex flex-wrap gap-1.5" role="tablist" aria-label="Sections de la bibliothèque">
        @for (o of onglets(); track o.cle) {
          <button
            type="button"
            role="tab"
            class="btn !h-8 !px-3 !text-[12.5px]"
            [class.btn-primaire]="onglet() === o.cle"
            [class.btn-secondaire]="onglet() !== o.cle"
            [attr.aria-selected]="onglet() === o.cle"
            (click)="choisir(o.cle)"
          >
            {{ o.libelle }}
          </button>
        }
      </div>

      @if (erreurAction()) {
        <div class="flex items-start gap-3 rounded-[12px] border border-danger-line bg-danger-bg px-4 py-3.5" role="alert">
          <span class="min-w-0 flex-1 text-[13px] leading-relaxed text-danger">{{ erreurAction() }}</span>
          <button type="button" class="text-danger/70 hover:text-danger" (click)="erreurAction.set('')" aria-label="Masquer">
            <sp-icone nom="fermer" [taille]="15" [epaisseur]="1.9" />
          </button>
        </div>
      }
      @if (succes()) {
        <div class="rounded-[12px] bg-succes-bg px-4 py-3 text-[13px] text-succes" role="status">{{ succes() }}</div>
      }

      @if (chargement()) {
        <section class="carte"><sp-etat titre="Chargement…" icone="horloge" /></section>
      } @else if (erreur()) {
        <section class="carte">
          <sp-etat variante="erreur" icone="alerte" titre="Impossible de charger la bibliothèque"
            [detail]="erreur()" actionLibelle="Réessayer" (action)="charger()" />
        </section>
      } @else {
        @switch (onglet()) {
          @case ('moderer') {
            @for (d of aModerer(); track d.id) {
              <section class="carte flex flex-col gap-3 px-[18px] py-4">
                <header class="flex flex-wrap items-center gap-3">
                  <h2 class="m-0 min-w-0 flex-1 text-[15px] font-semibold text-royal-700">{{ d.titre }}</h2>
                  <span class="badge bg-line-faint text-ink-muted">{{ d.type === 'MEMOIRE' ? 'Mémoire' : 'Sujet' }}</span>
                </header>
                <p class="m-0 text-[13px] text-ink">
                  {{ d.type === 'MEMOIRE' ? 'Auteur : ' + d.auteur : 'Matière : ' + d.matiere }}
                  @if (d.filiereLibelle) { · {{ d.filiereLibelle }} }
                  @if (d.annee) { · {{ d.annee }} }
                </p>
                <p class="m-0 text-xs text-ink-subtle">
                  Déposé par {{ d.deposePar }} le {{ d.deposeLe | date: 'dd/MM/yyyy' }} ·
                  {{ taille(d.taille) }} · {{ d.nomFichier }}
                </p>
                @if (refusOuvert() === d.id) {
                  <textarea class="champ min-h-[72px] py-2" maxlength="500" placeholder="Motif du refus — obligatoire"
                    [ngModel]="motif()" (ngModelChange)="motif.set($event)"></textarea>
                  <div class="flex justify-end gap-2">
                    <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="refusOuvert.set(null)">Annuler</button>
                    <button type="button" class="btn btn-primaire !h-8 !px-3 !text-[12px]"
                      [disabled]="enCours() || !motif().trim()" (click)="decider(d, false)">Confirmer le refus</button>
                  </div>
                } @else {
                  <div class="flex flex-wrap justify-end gap-2">
                    <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="ouvrir(d)">
                      <sp-icone nom="telecharger" [taille]="14" /> Consulter
                    </button>
                    @if (peutGerer()) {
                      <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="ouvrirRefus(d)" [disabled]="enCours()">Refuser…</button>
                      <button type="button" class="btn btn-primaire !h-8 !px-3 !text-[12px]" (click)="decider(d, true)" [disabled]="enCours()">Publier</button>
                    }
                  </div>
                }
              </section>
            } @empty {
              <section class="carte">
                <sp-etat icone="coche-cercle" titre="Aucun dépôt en attente" detail="Les dépôts des étudiants apparaîtront ici." />
              </section>
            }
          }

          @case ('documents') {
            <div class="flex flex-wrap items-center gap-2">
              <label class="champ !h-10 min-w-[min(100%,240px)] flex-1">
                <sp-icone nom="recherche" [taille]="16" />
                <input class="saisie" type="search" placeholder="Titre, auteur, matière…"
                  [ngModel]="recherche()" (ngModelChange)="recherche.set($event)" (keyup.enter)="chargerDocuments()" />
              </label>
              <select class="champ !h-10" [ngModel]="typeFiltre()" (ngModelChange)="typeFiltre.set($event); chargerDocuments()">
                <option [ngValue]="null">Tous les types</option>
                <option value="MEMOIRE">Mémoires</option>
                <option value="SUJET">Anciens sujets</option>
              </select>
              @if (peutGerer()) {
                <button type="button" class="btn btn-primaire" (click)="depotOuvert.set(!depotOuvert())">
                  <sp-icone nom="plus" [taille]="16" /> Déposer
                </button>
              }
            </div>

            @if (depotOuvert()) {
              <section class="carte flex flex-col gap-3 px-[18px] py-4">
                <h2 class="m-0 text-[15px] font-semibold text-royal-700">Nouveau document</h2>
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <label class="flex flex-col gap-1.5">
                    <span class="text-[11.5px] font-semibold text-ink-muted">Type</span>
                    <select class="champ !h-11" [ngModel]="depot().type" (ngModelChange)="majDepot({ type: $event })">
                      <option value="MEMOIRE">Mémoire</option>
                      <option value="SUJET">Ancien sujet</option>
                    </select>
                  </label>
                  <label class="flex flex-col gap-1.5">
                    <span class="text-[11.5px] font-semibold text-ink-muted">Titre</span>
                    <input class="champ !h-11" maxlength="160" [ngModel]="depot().titre" (ngModelChange)="majDepot({ titre: $event })" />
                  </label>
                  @if (depot().type === 'MEMOIRE') {
                    <label class="flex flex-col gap-1.5">
                      <span class="text-[11.5px] font-semibold text-ink-muted">Auteur</span>
                      <input class="champ !h-11" maxlength="150" [ngModel]="depot().auteur" (ngModelChange)="majDepot({ auteur: $event })" />
                    </label>
                  } @else {
                    <label class="flex flex-col gap-1.5">
                      <span class="text-[11.5px] font-semibold text-ink-muted">Matière</span>
                      <input class="champ !h-11" maxlength="150" [ngModel]="depot().matiere" (ngModelChange)="majDepot({ matiere: $event })" />
                    </label>
                  }
                  <label class="flex flex-col gap-1.5">
                    <span class="text-[11.5px] font-semibold text-ink-muted">Filière</span>
                    <select class="champ !h-11" [ngModel]="depot().filiereId" (ngModelChange)="majDepot({ filiereId: $event })">
                      <option [ngValue]="null">Toutes / non précisée</option>
                      @for (f of filieres(); track f.id) {
                        <option [ngValue]="f.id">{{ f.libelle }}</option>
                      }
                    </select>
                  </label>
                  <label class="flex flex-col gap-1.5">
                    <span class="text-[11.5px] font-semibold text-ink-muted">Année</span>
                    <input class="champ !h-11" type="number" min="1950" [ngModel]="depot().annee" (ngModelChange)="majDepot({ annee: $event })" />
                  </label>
                  <label class="flex flex-col gap-1.5">
                    <span class="text-[11.5px] font-semibold text-ink-muted">Fichier (25 Mo maximum)</span>
                    <input class="champ !h-11 py-2" type="file" accept=".pdf,.doc,.docx,.odt,.ppt,.pptx,.zip,.png,.jpg,.jpeg"
                      (change)="choisirFichier($event)" />
                  </label>
                </div>
                <div class="flex justify-end gap-2">
                  <button type="button" class="btn btn-secondaire" (click)="depotOuvert.set(false)">Annuler</button>
                  <button type="button" class="btn btn-primaire" [disabled]="enCours() || !depotValide()" (click)="deposer()">Publier</button>
                </div>
              </section>
            }

            @for (d of documents(); track d.id) {
              <section class="carte flex flex-wrap items-center gap-3 px-[18px] py-3.5">
                <div class="flex min-w-[min(100%,260px)] flex-1 flex-col gap-0.5">
                  <h2 class="m-0 text-[14.5px] font-semibold text-royal-700">{{ d.titre }}</h2>
                  <span class="text-[12.5px] text-ink-muted">
                    {{ d.type === 'MEMOIRE' ? d.auteur : d.matiere }}
                    @if (d.filiereLibelle) { · {{ d.filiereLibelle }} }
                    @if (d.annee) { · {{ d.annee }} }
                  </span>
                  <span class="text-xs text-ink-faint">{{ d.deposePar }} · {{ taille(d.taille) }}</span>
                </div>
                <span class="badge bg-line-faint text-ink-muted">{{ d.type === 'MEMOIRE' ? 'Mémoire' : 'Sujet' }}</span>
                <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="ouvrir(d)">
                  <sp-icone nom="telecharger" [taille]="14" /> Ouvrir
                </button>
                @if (peutGerer()) {
                  <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="retirer(d)" [disabled]="enCours()">Retirer</button>
                }
              </section>
            } @empty {
              <section class="carte"><sp-etat icone="classes" titre="Aucun document" detail="Aucun mémoire ni sujet ne correspond." /></section>
            }
          }

          @case ('livres') {
            <div class="flex flex-wrap items-center gap-2">
              <label class="champ !h-10 min-w-[min(100%,240px)] flex-1">
                <sp-icone nom="recherche" [taille]="16" />
                <input class="saisie" type="search" placeholder="Titre, auteur, ISBN…"
                  [ngModel]="recherche()" (ngModelChange)="recherche.set($event)" (keyup.enter)="chargerLivres()" />
              </label>
              @if (peutGerer()) {
                <button type="button" class="btn btn-primaire" (click)="ouvrirLivre(null)">
                  <sp-icone nom="plus" [taille]="16" /> Ajouter un livre
                </button>
              }
            </div>

            @if (livreOuvert()) {
              <section class="carte flex flex-col gap-3 px-[18px] py-4">
                <h2 class="m-0 text-[15px] font-semibold text-royal-700">{{ livreEdite() ? 'Modifier le livre' : 'Nouveau livre' }}</h2>
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">Titre</span>
                    <input class="champ !h-11" maxlength="200" [ngModel]="livre().titre" (ngModelChange)="majLivre({ titre: $event })" /></label>
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">Auteur</span>
                    <input class="champ !h-11" maxlength="150" [ngModel]="livre().auteur" (ngModelChange)="majLivre({ auteur: $event })" /></label>
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">ISBN</span>
                    <input class="champ !h-11" maxlength="30" [ngModel]="livre().isbn" (ngModelChange)="majLivre({ isbn: $event })" /></label>
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">Catégorie</span>
                    <input class="champ !h-11" maxlength="80" [ngModel]="livre().categorie" (ngModelChange)="majLivre({ categorie: $event })" /></label>
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">Filière</span>
                    <select class="champ !h-11" [ngModel]="livre().filiereId" (ngModelChange)="majLivre({ filiereId: $event })">
                      <option [ngValue]="null">Toutes</option>
                      @for (f of filieres(); track f.id) { <option [ngValue]="f.id">{{ f.libelle }}</option> }
                    </select></label>
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">Emplacement (rayon)</span>
                    <input class="champ !h-11" maxlength="80" [ngModel]="livre().emplacement" (ngModelChange)="majLivre({ emplacement: $event })" /></label>
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">Nombre d'exemplaires</span>
                    <input class="champ !h-11" type="number" min="1" max="500" [ngModel]="livre().exemplaires" (ngModelChange)="majLivre({ exemplaires: $event })" /></label>
                </div>
                <div class="flex justify-end gap-2">
                  <button type="button" class="btn btn-secondaire" (click)="livreOuvert.set(false)">Annuler</button>
                  <button type="button" class="btn btn-primaire" [disabled]="enCours() || !livreValide()" (click)="enregistrerLivre()">Enregistrer</button>
                </div>
              </section>
            }

            @for (l of livres(); track l.id) {
              <section class="carte flex flex-wrap items-center gap-3 px-[18px] py-3.5">
                <div class="flex min-w-[min(100%,260px)] flex-1 flex-col gap-0.5">
                  <h2 class="m-0 text-[14.5px] font-semibold text-royal-700">{{ l.titre }}</h2>
                  <span class="text-[12.5px] text-ink-muted">
                    {{ l.auteur }}
                    @if (l.categorie) { · {{ l.categorie }} }
                    @if (l.filiereLibelle) { · {{ l.filiereLibelle }} }
                  </span>
                  @if (l.emplacement) { <span class="text-xs text-ink-faint">Rayon : {{ l.emplacement }}</span> }
                </div>
                <span class="badge" [class]="l.disponibles > 0 ? 'bg-succes-bg text-succes' : 'bg-danger-bg text-danger'">
                  {{ l.disponibles }} / {{ l.exemplaires }} disponible{{ l.disponibles > 1 ? 's' : '' }}
                </span>
                @if (peutGerer()) {
                  <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="ouvrirLivre(l)">Modifier</button>
                  <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="supprimerLivre(l)" [disabled]="enCours()">Supprimer</button>
                }
              </section>
            } @empty {
              <section class="carte"><sp-etat icone="classes" titre="Aucun livre" detail="Ajoutez les livres du fonds pour suivre les emprunts." /></section>
            }
          }

          @case ('emprunts') {
            <div class="flex flex-wrap items-center gap-2">
              <select class="champ !h-10" [ngModel]="filtreEmprunt()" (ngModelChange)="filtreEmprunt.set($event)">
                <option value="OUVERTS">En cours et en retard</option>
                <option value="EN_RETARD">En retard seulement</option>
                <option value="RETOURNE">Rendus</option>
                <option value="TOUS">Tous</option>
              </select>
              @if (peutGerer()) {
                <button type="button" class="btn btn-primaire" (click)="empruntOuvert.set(!empruntOuvert())">
                  <sp-icone nom="plus" [taille]="16" /> Nouvel emprunt
                </button>
              }
            </div>

            @if (empruntOuvert()) {
              <section class="carte flex flex-col gap-3 px-[18px] py-4">
                <h2 class="m-0 text-[15px] font-semibold text-royal-700">Enregistrer un emprunt</h2>
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">Livre</span>
                    <select class="champ !h-11" [ngModel]="nouvelEmprunt().livreId" (ngModelChange)="majEmprunt({ livreId: $event })">
                      <option [ngValue]="''" disabled>Choisir un livre…</option>
                      @for (l of livres(); track l.id) {
                        <option [ngValue]="l.id" [disabled]="l.disponibles === 0">{{ l.titre }} ({{ l.disponibles }} dispo.)</option>
                      }
                    </select></label>
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">Étudiant</span>
                    <select class="champ !h-11" [ngModel]="nouvelEmprunt().etudiantId" (ngModelChange)="majEmprunt({ etudiantId: $event })">
                      <option [ngValue]="''" disabled>Choisir un étudiant…</option>
                      @for (e of etudiants(); track e.id) {
                        <option [ngValue]="e.id">{{ e.nom }} {{ e.prenom }} — {{ e.matricule }}</option>
                      }
                    </select></label>
                  <label class="flex flex-col gap-1.5"><span class="text-[11.5px] font-semibold text-ink-muted">Durée en jours (14 par défaut)</span>
                    <input class="champ !h-11" type="number" min="1" max="60" [ngModel]="nouvelEmprunt().duree" (ngModelChange)="majEmprunt({ duree: $event })" /></label>
                </div>
                <div class="flex justify-end gap-2">
                  <button type="button" class="btn btn-secondaire" (click)="empruntOuvert.set(false)">Annuler</button>
                  <button type="button" class="btn btn-primaire"
                    [disabled]="enCours() || !nouvelEmprunt().livreId || !nouvelEmprunt().etudiantId" (click)="emprunter()">Enregistrer</button>
                </div>
              </section>
            }

            @for (e of empruntsAffiches(); track e.id) {
              <section class="carte flex flex-wrap items-center gap-3 px-[18px] py-3.5">
                <div class="flex min-w-[min(100%,260px)] flex-1 flex-col gap-0.5">
                  <h2 class="m-0 text-[14.5px] font-semibold text-royal-700">{{ e.livreTitre }}</h2>
                  <span class="text-[12.5px] text-ink">{{ e.etudiantNom }} · {{ e.etudiantMatricule }}</span>
                  <span class="text-xs text-ink-faint">
                    Emprunté le {{ e.emprunteLe | date: 'dd/MM/yyyy' }} · retour prévu le {{ e.retourPrevu | date: 'dd/MM/yyyy' }}
                    @if (e.retourneLe) { · rendu le {{ e.retourneLe | date: 'dd/MM/yyyy' }} }
                  </span>
                </div>
                @switch (e.statut) {
                  @case ('EN_RETARD') {
                    <span class="badge bg-danger-bg text-danger">{{ e.joursDeRetard }} jour{{ e.joursDeRetard > 1 ? 's' : '' }} de retard</span>
                  }
                  @case ('RETOURNE') {
                    <span class="badge bg-line-faint text-ink-muted">
                      Rendu{{ e.joursDeRetard > 0 ? ' avec ' + e.joursDeRetard + ' j de retard' : '' }}
                    </span>
                  }
                  @default { <span class="badge bg-succes-bg text-succes">En cours</span> }
                }
                @if (peutGerer() && e.statut !== 'RETOURNE') {
                  @if (e.statut === 'EN_COURS' && e.prolongations < 2) {
                    <button type="button" class="btn btn-secondaire !h-8 !px-3 !text-[12px]" (click)="prolonger(e)" [disabled]="enCours()">+7 jours</button>
                  }
                  <button type="button" class="btn btn-primaire !h-8 !px-3 !text-[12px]" (click)="retourner(e)" [disabled]="enCours()">Rendu</button>
                }
              </section>
            } @empty {
              <section class="carte"><sp-etat icone="coche-cercle" titre="Aucun emprunt" detail="Aucun emprunt ne correspond à ce filtre." /></section>
            }
          }
        }
      }
    </div>
  `,
})
export class BibliothequeComponent {
  private readonly service = inject(BibliothequeService);
  private readonly classes = inject(ClasseService);
  private readonly etudiantService = inject(EtudiantService);
  private readonly attente = inject(AttenteService);
  private readonly auth = inject(AuthService);

  readonly peutGerer = computed(() => this.auth.aUnRole('ADMIN', 'RESPONSABLE_SCOLARITE'));
  readonly onglets = computed(() => {
    const liste: { cle: Onglet; libelle: string }[] = [];
    if (this.peutGerer()) {
      const n = this.aModerer().length;
      liste.push({ cle: 'moderer', libelle: n > 0 ? `À modérer (${n})` : 'À modérer' });
    }
    liste.push(
      { cle: 'documents', libelle: 'Mémoires et sujets' },
      { cle: 'livres', libelle: 'Livres' },
      { cle: 'emprunts', libelle: 'Emprunts' },
    );
    return liste;
  });

  readonly onglet = signal<Onglet>(this.auth.aUnRole('ADMIN', 'RESPONSABLE_SCOLARITE') ? 'moderer' : 'documents');
  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly erreurAction = signal('');
  readonly succes = signal('');
  readonly enCours = signal(false);

  readonly synthese = signal<SyntheseBibliotheque | null>(null);
  readonly aModerer = signal<DocumentBibliotheque[]>([]);
  readonly documents = signal<DocumentBibliotheque[]>([]);
  readonly livres = signal<Livre[]>([]);
  readonly emprunts = signal<Emprunt[]>([]);
  readonly filieres = signal<Filiere[]>([]);
  readonly etudiants = signal<Etudiant[]>([]);

  readonly recherche = signal('');
  readonly typeFiltre = signal<TypeDocument | null>(null);
  readonly filtreEmprunt = signal<'OUVERTS' | 'EN_RETARD' | 'RETOURNE' | 'TOUS'>('OUVERTS');
  readonly refusOuvert = signal<string | null>(null);
  readonly motif = signal('');

  readonly depotOuvert = signal(false);
  readonly depot = signal<{ type: TypeDocument; titre: string; auteur: string; matiere: string; filiereId: number | null; annee: number | null; fichier: File | null }>(
    { type: 'MEMOIRE', titre: '', auteur: '', matiere: '', filiereId: null, annee: null, fichier: null },
  );
  readonly depotValide = computed(() => {
    const d = this.depot();
    return !!d.titre.trim() && !!d.fichier && (d.type === 'MEMOIRE' ? !!d.auteur.trim() : !!d.matiere.trim());
  });

  readonly livreOuvert = signal(false);
  readonly livreEdite = signal<Livre | null>(null);
  readonly livre = signal<LivreRequest>({ ...LIVRE_VIDE });
  readonly livreValide = computed(() => !!this.livre().titre.trim() && !!this.livre().auteur.trim() && this.livre().exemplaires >= 1);

  readonly empruntOuvert = signal(false);
  readonly nouvelEmprunt = signal<{ livreId: string; etudiantId: string; duree: number | null }>({ livreId: '', etudiantId: '', duree: null });

  readonly empruntsAffiches = computed(() => {
    const f = this.filtreEmprunt();
    return this.emprunts().filter((e) =>
      f === 'TOUS' ? true : f === 'OUVERTS' ? e.statut !== 'RETOURNE' : e.statut === f,
    );
  });

  constructor() {
    this.charger();
    this.classes.listerFilieres().subscribe({ next: (f) => this.filieres.set(f ?? []), error: () => undefined });
    if (this.peutGerer()) {
      this.etudiantService.lister().subscribe({
        next: (e) => this.etudiants.set((e ?? []).filter((x) => x.actif)),
        error: () => undefined,
      });
    }
  }

  choisir(o: Onglet): void {
    this.onglet.set(o);
    this.recherche.set('');
    this.erreurAction.set('');
    this.succes.set('');
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    let restant = this.peutGerer() ? 5 : 3;
    const fini = () => {
      if (--restant === 0) this.chargement.set(false);
    };
    const echec = (e: unknown) => {
      this.erreur.set(messageErreur(e));
      fini();
    };
    this.service.documents(this.typeFiltre(), this.recherche()).subscribe({ next: (d) => { this.documents.set(d ?? []); fini(); }, error: echec });
    this.service.livres(this.onglet() === 'livres' ? this.recherche() : '').subscribe({ next: (l) => { this.livres.set(l ?? []); fini(); }, error: echec });
    this.service.emprunts().subscribe({ next: (e) => { this.emprunts.set(e ?? []); fini(); }, error: echec });
    if (this.peutGerer()) {
      this.service.aModerer().subscribe({ next: (d) => { this.aModerer.set(d ?? []); fini(); }, error: echec });
      this.service.synthese().subscribe({ next: (s) => { this.synthese.set(s); fini(); }, error: echec });
    }
  }

  chargerDocuments(): void {
    this.service.documents(this.typeFiltre(), this.recherche()).subscribe({
      next: (d) => this.documents.set(d ?? []),
      error: (e: unknown) => this.erreurAction.set(messageErreur(e)),
    });
  }

  chargerLivres(): void {
    this.service.livres(this.recherche()).subscribe({
      next: (l) => this.livres.set(l ?? []),
      error: (e: unknown) => this.erreurAction.set(messageErreur(e)),
    });
  }

  taille(octets: number): string {
    return octets >= 1048576 ? `${(octets / 1048576).toFixed(1)} Mo` : `${Math.max(1, Math.round(octets / 1024))} Ko`;
  }

  // ----- Documents -----------------------------------------------------

  ouvrir(d: DocumentBibliotheque): void {
    this.service.telecharger(d).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = d.nomFichier;
        lien.click();
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
      },
      error: (e: unknown) => this.erreurAction.set(messageErreur(e)),
    });
  }

  ouvrirRefus(d: DocumentBibliotheque): void {
    this.motif.set('');
    this.refusOuvert.set(d.id);
  }

  decider(d: DocumentBibliotheque, accepte: boolean): void {
    this.agir(this.service.decider(d.id, accepte, accepte ? undefined : this.motif().trim()), accepte ? `« ${d.titre} » publié.` : `« ${d.titre} » refusé.`, () => {
      this.aModerer.update((l) => l.filter((x) => x.id !== d.id));
      this.refusOuvert.set(null);
      if (accepte) this.chargerDocuments();
    });
  }

  retirer(d: DocumentBibliotheque): void {
    if (!confirm(`Retirer « ${d.titre} » de la bibliothèque ?`)) return;
    this.agir(this.service.retirer(d.id), 'Document retiré.', () => this.documents.update((l) => l.filter((x) => x.id !== d.id)));
  }

  majDepot(patch: Partial<ReturnType<typeof this.depot>>): void {
    this.depot.update((d) => ({ ...d, ...patch }));
  }

  choisirFichier(evenement: Event): void {
    const entree = evenement.target as HTMLInputElement;
    this.majDepot({ fichier: entree.files?.[0] ?? null });
  }

  deposer(): void {
    const d = this.depot();
    if (!d.fichier) return;
    this.agir(
      this.service.deposer({ type: d.type, titre: d.titre.trim(), auteur: d.auteur, matiere: d.matiere, filiereId: d.filiereId, annee: d.annee, fichier: d.fichier }),
      'Document publié.',
      () => {
        this.depotOuvert.set(false);
        this.depot.set({ type: 'MEMOIRE', titre: '', auteur: '', matiere: '', filiereId: null, annee: null, fichier: null });
        this.chargerDocuments();
      },
    );
  }

  // ----- Livres --------------------------------------------------------

  ouvrirLivre(l: Livre | null): void {
    this.livreEdite.set(l);
    this.livre.set(
      l
        ? { titre: l.titre, auteur: l.auteur, isbn: l.isbn, categorie: l.categorie, filiereId: l.filiereId, resume: l.resume, emplacement: l.emplacement, exemplaires: l.exemplaires }
        : { ...LIVRE_VIDE },
    );
    this.livreOuvert.set(true);
  }

  majLivre(patch: Partial<LivreRequest>): void {
    this.livre.update((l) => ({ ...l, ...patch }));
  }

  enregistrerLivre(): void {
    const edite = this.livreEdite();
    const appel = edite ? this.service.modifierLivre(edite.id, this.livre()) : this.service.creerLivre(this.livre());
    this.agir(appel, edite ? 'Livre modifié.' : 'Livre ajouté.', () => {
      this.livreOuvert.set(false);
      this.chargerLivres();
    });
  }

  supprimerLivre(l: Livre): void {
    if (!confirm(`Retirer « ${l.titre} » du fonds ?`)) return;
    this.agir(this.service.supprimerLivre(l.id), 'Livre retiré.', () => this.livres.update((x) => x.filter((y) => y.id !== l.id)));
  }

  // ----- Emprunts ------------------------------------------------------

  majEmprunt(patch: Partial<ReturnType<typeof this.nouvelEmprunt>>): void {
    this.nouvelEmprunt.update((e) => ({ ...e, ...patch }));
  }

  emprunter(): void {
    const e = this.nouvelEmprunt();
    this.agir(this.service.emprunter(e.livreId, e.etudiantId, e.duree), 'Emprunt enregistré.', () => {
      this.empruntOuvert.set(false);
      this.nouvelEmprunt.set({ livreId: '', etudiantId: '', duree: null });
      this.recharger();
    });
  }

  retourner(e: Emprunt): void {
    this.agir(this.service.retourner(e.id), `Retour de « ${e.livreTitre} » enregistré.`, () => this.recharger());
  }

  prolonger(e: Emprunt): void {
    this.agir(this.service.prolonger(e.id, 7), 'Emprunt prolongé de 7 jours.', () => this.recharger());
  }

  /** Recharge emprunts, disponibilités et chiffres : un retour change les trois. */
  private recharger(): void {
    this.service.emprunts().subscribe((e) => this.emprunts.set(e ?? []));
    this.service.livres(this.recherche()).subscribe((l) => this.livres.set(l ?? []));
    this.service.synthese().subscribe((s) => this.synthese.set(s));
  }

  private agir<T>(appel: import('rxjs').Observable<T>, message: string, apres: () => void): void {
    if (this.enCours()) return;
    this.enCours.set(true);
    this.erreurAction.set('');
    this.succes.set('');
    appel.subscribe({
      next: () => {
        apres();
        this.succes.set(message);
        this.enCours.set(false);
        this.service.synthese().subscribe((s) => this.synthese.set(s));
        this.attente.rafraichir();
      },
      error: (e: unknown) => {
        this.erreurAction.set(messageErreur(e));
        this.enCours.set(false);
      },
    });
  }
}
