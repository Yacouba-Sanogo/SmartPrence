import { Injectable, inject, signal } from '@angular/core';
import { CompteurNavigation } from '../../layout/navigation';
import { AuthService } from './auth.service';
import { JustificationService } from './justification.service';
import { SignalementService } from './signalement.service';

/**
 * Ce qui attend une décision, pour les compteurs de la barre latérale.
 *
 * <p>Le service ne se rafraîchit pas de lui-même à chaque navigation : deux appels
 * réseau par changement d'écran, pour un chiffre qui ne bouge qu'au moment où
 * quelqu'un tranche un dossier, seraient payés en pure perte. Il se charge une fois
 * à la connexion, et les écrans qui traitent une demande appellent
 * {@link rafraichir} — le compteur décroît alors sous les yeux de celui qui vient
 * d'agir, ce qui est le seul instant où sa justesse se remarque.</p>
 *
 * <p><b>Limite connue.</b> Faute d'endpoint de comptage, les listes complètes sont
 * chargées puis filtrées côté client. C'est sans conséquence sur les volumes d'une
 * école, mais un « GET /justifications/en-attente/compte » serait plus juste et
 * dispenserait de transporter des dossiers que personne n'affiche.</p>
 */
@Injectable({ providedIn: 'root' })
export class AttenteService {
  private readonly justificationService = inject(JustificationService);
  private readonly signalementService = inject(SignalementService);
  private readonly auth = inject(AuthService);

  private readonly compteurs = signal<Record<CompteurNavigation, number>>({
    justificatifs: 0,
    signalements: 0,
  });

  /** Nombre de dossiers en attente pour la file demandée. */
  compte(file: CompteurNavigation): number {
    return this.compteurs()[file];
  }

  /**
   * Recharge les deux files.
   *
   * <p>Chaque appel est isolé : un utilisateur qui n'a pas accès aux justificatifs
   * reçoit un 403 sur cette file, et il n'y a pas de raison que cela efface le
   * compteur des signalements, auxquels il a droit.</p>
   */
  rafraichir(): void {
    if (!this.auth.connecte()) return;

    if (this.auth.aUnRole('ADMIN', 'RESPONSABLE_SCOLARITE')) {
      this.justificationService.lister().subscribe({
        next: (liste) =>
          this.majCompteur(
            'justificatifs',
            liste.filter((j) => j.statut === 'EN_ATTENTE').length,
          ),
        error: () => this.majCompteur('justificatifs', 0),
      });
    }

    if (this.auth.aUnRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')) {
      this.signalementService.lister('EN_ATTENTE').subscribe({
        next: (liste) => this.majCompteur('signalements', liste.length),
        error: () => this.majCompteur('signalements', 0),
      });
    }
  }

  private majCompteur(file: CompteurNavigation, valeur: number): void {
    this.compteurs.update((actuels) => ({ ...actuels, [file]: valeur }));
  }
}
