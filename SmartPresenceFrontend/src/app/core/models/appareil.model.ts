/** État opérationnel d'un lecteur (enum DeviceStatut du backend). */
export type StatutAppareil = 'ACTIF' | 'INACTIF' | 'HORS_LIGNE' | 'EN_PANNE';

/** Vocation fonctionnelle du lecteur (enum UsageDevice du backend). */
export type UsageAppareil = 'ETUDIANT' | 'PERSONNEL' | 'MIXTE';

/** Issue d'une synchronisation (enum StatutSynchronisation du backend). */
export type StatutSynchronisation = 'SUCCES' | 'PARTIEL' | 'ECHEC';

export const STATUTS_APPAREIL: readonly StatutAppareil[] = [
  'ACTIF',
  'INACTIF',
  'HORS_LIGNE',
  'EN_PANNE',
];

export const USAGES_APPAREIL: readonly UsageAppareil[] = ['PERSONNEL', 'ETUDIANT', 'MIXTE'];

const LIBELLES_STATUT: Record<StatutAppareil, string> = {
  ACTIF: 'Actif',
  INACTIF: 'Inactif',
  HORS_LIGNE: 'Hors ligne',
  EN_PANNE: 'En panne',
};

const LIBELLES_USAGE: Record<UsageAppareil, string> = {
  PERSONNEL: 'Personnel',
  ETUDIANT: 'Étudiants',
  MIXTE: 'Mixte',
};

const DESCRIPTIONS_USAGE: Record<UsageAppareil, string> = {
  PERSONNEL: "Lecteur d'entrée — pointages du personnel uniquement",
  ETUDIANT: 'Lecteur de salle — présences des étudiants uniquement',
  MIXTE: 'Lecteur polyvalent — accepte les deux flux',
};

export function libelleStatutAppareil(statut: StatutAppareil): string {
  return LIBELLES_STATUT[statut] ?? statut;
}

export function libelleUsage(usage: UsageAppareil | null): string {
  return usage ? (LIBELLES_USAGE[usage] ?? usage) : 'Mixte';
}

export function descriptionUsage(usage: UsageAppareil): string {
  return DESCRIPTIONS_USAGE[usage] ?? '';
}

export interface Appareil {
  id: string;
  nom: string;
  adresseMac: string;
  statut: StatutAppareil;
  /** `null` sur un appareil enregistré avant l'introduction du champ — traité comme MIXTE. */
  usage: UsageAppareil | null;
  versionFirmware: string | null;
  derniereConnexion: string | null;
  derniereSynchronisation: string | null;
  salleId: number | null;
  salleNom: string | null;
  salleCode: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AppareilRequest {
  nom: string;
  adresseMac: string;
  /** Clé en clair : le backend n'en conserve que l'empreinte. Minimum 16 caractères. */
  apiKey: string;
  usage: UsageAppareil;
  versionFirmware?: string | null;
  salleId?: number | null;
}

/** Entrée du journal de synchronisation d'un lecteur. */
export interface Synchronisation {
  id: string;
  deviceId: string;
  deviceNom: string;
  dateHeure: string;
  statut: StatutSynchronisation;
  nombreEvenements: number | null;
  nombreTentatives: number | null;
  messageErreur: string | null;
  createdAt: string;
}

export interface Salle {
  id: number;
  code: string;
  nom: string;
  batiment: string | null;
  capacite: number | null;
  deviceId: string | null;
  deviceNom: string | null;
}

/**
 * Génère une clé d'API aléatoire.
 *
 * <p>L'empreinte stockée côté serveur est déterministe et rapide à calculer : la
 * robustesse repose donc entièrement sur l'entropie de la clé. La tirer ici, plutôt
 * que de la laisser inventer par un administrateur, écarte les clés devinables du
 * type « esp32-salle-b12 ».</p>
 */
export function genererCleApi(): string {
  const octets = new Uint8Array(24);
  crypto.getRandomValues(octets);
  return Array.from(octets, (o) => o.toString(16).padStart(2, '0')).join('');
}

/** Format d'adresse MAC attendu par le backend : `AA:BB:CC:DD:EE:FF`. */
export const MOTIF_ADRESSE_MAC = /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/;

export function adresseMacValide(valeur: string): boolean {
  return MOTIF_ADRESSE_MAC.test(valeur.trim());
}

/**
 * Ancienneté lisible d'un horodatage : « il y a 12 s », « il y a 3 h ».
 *
 * Un lecteur IoT se juge à sa fraîcheur bien plus qu'à sa date absolue de dernier
 * contact — c'est l'écart qui signale une panne.
 */
export function anciennete(instantIso: string | null): string {
  if (!instantIso) return 'jamais';
  const secondes = Math.floor((Date.now() - new Date(instantIso).getTime()) / 1000);
  if (secondes < 0) return "à l'instant";
  if (secondes < 60) return `il y a ${secondes} s`;
  const minutes = Math.floor(secondes / 60);
  if (minutes < 60) return `il y a ${minutes} min`;
  const heures = Math.floor(minutes / 60);
  if (heures < 24) return `il y a ${heures} h`;
  const jours = Math.floor(heures / 24);
  return jours === 1 ? 'hier' : `il y a ${jours} j`;
}

/** `2026-03-12T08:31:00Z` → `12/03 08:31`. */
export function formaterInstantCourt(instantIso: string | null): string {
  if (!instantIso) return '—';
  const d = new Date(instantIso);
  const deuxChiffres = (n: number) => String(n).padStart(2, '0');
  return `${deuxChiffres(d.getDate())}/${deuxChiffres(d.getMonth() + 1)} ${deuxChiffres(d.getHours())}:${deuxChiffres(d.getMinutes())}`;
}

/**
 * Un lecteur est considéré injoignable si son dernier contact remonte au-delà du seuil.
 *
 * Le statut enregistré en base est déclaratif : il ne bascule pas tout seul quand un
 * appareil cesse d'émettre. Cette lecture complète l'information plutôt que de la
 * remplacer.
 */
export function paraitInjoignable(appareil: Appareil, seuilMinutes = 15): boolean {
  if (appareil.statut !== 'ACTIF') return false;
  const dernier = appareil.derniereSynchronisation ?? appareil.derniereConnexion;
  if (!dernier) return true;
  return Date.now() - new Date(dernier).getTime() > seuilMinutes * 60_000;
}
