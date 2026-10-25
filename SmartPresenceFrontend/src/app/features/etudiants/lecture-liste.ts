import { LigneImport } from '../../core/services/etudiant.service';

/** Nature du fichier lu : elle décide du degré de confiance affiché à l'administrateur. */
export type SourceListe = 'CSV' | 'EXCEL' | 'IMAGE';

export interface ListeLue {
  source: SourceListe;
  lignes: LigneImport[];
}

const EXTENSIONS_IMAGE = ['png', 'jpg', 'jpeg', 'webp', 'bmp'];

/** Lit un fichier d'étudiants : tableur CSV, classeur Excel ou photo d'une liste imprimée. */
export async function lireListe(
  fichier: File,
  progression: (message: string, part: number | null) => void,
): Promise<ListeLue> {
  const nom = fichier.name.toLowerCase();
  const extension = nom.slice(nom.lastIndexOf('.') + 1);

  if (extension === 'xlsx') {
    progression('Lecture du classeur Excel…', null);
    const { default: lireExcel } = await import('read-excel-file');
    const lignes = await lireExcel(fichier);
    return { source: 'EXCEL', lignes: depuisTableau(lignes.map((l) => l.map(texteDeCellule))) };
  }
  if (['csv', 'txt', 'tsv'].includes(extension)) {
    progression('Lecture du fichier…', null);
    return { source: 'CSV', lignes: depuisTableau(analyserCsv(await fichier.text())) };
  }
  if (EXTENSIONS_IMAGE.includes(extension) || fichier.type.startsWith('image/')) {
    return { source: 'IMAGE', lignes: depuisTexteLibre(await reconnaitreTexte(fichier, progression)) };
  }
  throw new Error('Format non pris en charge. Utilisez un fichier CSV, Excel (.xlsx) ou une image (JPG, PNG).');
}

async function reconnaitreTexte(
  image: File,
  progression: (message: string, part: number | null) => void,
): Promise<string> {
  progression('Chargement du module de reconnaissance…', null);
  const { createWorker } = await import('tesseract.js');
  const worker = await createWorker('fra', 1, {
    logger: (m: { status: string; progress: number }) => {
      if (m.status === 'recognizing text') progression('Lecture de la photo…', m.progress);
    },
  });
  try {
    const { data } = await worker.recognize(image);
    return data.text;
  } finally {
    await worker.terminate();
  }
}

// ---------------------------------------------------------------------------
// Tableaux (CSV, Excel)
// ---------------------------------------------------------------------------

function texteDeCellule(valeur: unknown): string {
  if (valeur === null || valeur === undefined) return '';
  if (valeur instanceof Date) return valeur.toISOString().slice(0, 10);
  return String(valeur).trim();
}

/** Découpe un CSV en gérant le séparateur (; , tabulation), les guillemets et le BOM. */
export function analyserCsv(texte: string): string[][] {
  const brut = texte.replace(/^﻿/, '');
  const premiere = brut.split(/\r?\n/, 1)[0] ?? '';
  const separateur = [';', '\t', ','].reduce(
    (meilleur, s) => (premiere.split(s).length > premiere.split(meilleur).length ? s : meilleur),
    ';',
  );

  const lignes: string[][] = [];
  let ligne: string[] = [];
  let cellule = '';
  let guillemets = false;
  for (let i = 0; i < brut.length; i++) {
    const c = brut[i];
    if (guillemets) {
      if (c === '"' && brut[i + 1] === '"') {
        cellule += '"';
        i++;
      } else if (c === '"') guillemets = false;
      else cellule += c;
    } else if (c === '"') guillemets = true;
    else if (c === separateur) {
      ligne.push(cellule.trim());
      cellule = '';
    } else if (c === '\n' || c === '\r') {
      if (c === '\r' && brut[i + 1] === '\n') i++;
      ligne.push(cellule.trim());
      if (ligne.some((x) => x !== '')) lignes.push(ligne);
      ligne = [];
      cellule = '';
    } else cellule += c;
  }
  ligne.push(cellule.trim());
  if (ligne.some((x) => x !== '')) lignes.push(ligne);
  return lignes;
}

function sansAccent(texte: string): string {
  return texte.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^a-z0-9 ]/g, ' ').replace(/\s+/g, ' ').trim();
}

type Colonne = 'matricule' | 'nom' | 'prenom' | 'nomComplet' | 'email' | 'telephone' | 'dateNaissance';

function colonneDepuisEntete(entete: string): Colonne | null {
  const e = sansAccent(entete);
  if (!e) return null;
  if (/^(matricule|n ?matricule|numero|num|no|id|identifiant|code)( etudiant)?$/.test(e) || e.startsWith('matricule')) return 'matricule';
  if (/(nom et prenom|nom prenom|nom complet|noms et prenoms|etudiant)/.test(e)) return 'nomComplet';
  if (/^prenoms?$/.test(e) || e.startsWith('prenom')) return 'prenom';
  if (/^(nom|nom de famille|noms|patronyme)$/.test(e)) return 'nom';
  if (/(mail|courriel)/.test(e)) return 'email';
  if (/(tel|phone|contact|mobile|portable)/.test(e)) return 'telephone';
  if (/naissance/.test(e) || e === 'date') return 'dateNaissance';
  return null;
}

/** Transforme un tableau en lignes d'étudiants, avec ou sans ligne d'en-tête. */
export function depuisTableau(tableau: string[][]): LigneImport[] {
  if (tableau.length === 0) return [];

  const reconnues = tableau[0].map(colonneDepuisEntete);
  const avecEntete = reconnues.filter((c) => c !== null).length >= 2;
  const ordre: (Colonne | null)[] = avecEntete
    ? reconnues
    : ['matricule', 'nom', 'prenom', 'email', 'telephone', 'dateNaissance'];
  const donnees = avecEntete ? tableau.slice(1) : tableau;

  const lignes: LigneImport[] = [];
  for (const cellules of donnees) {
    const ligne: LigneImport = { matricule: '', nom: '', prenom: '' };
    cellules.forEach((valeur, i) => {
      const colonne = ordre[i];
      if (!colonne || !valeur) return;
      if (colonne === 'nomComplet') {
        const { nom, prenom } = separerNomPrenom(valeur.split(/\s+/));
        ligne.nom = nom;
        ligne.prenom = prenom;
      } else if (colonne === 'dateNaissance') {
        ligne.dateNaissance = dateIso(valeur);
      } else {
        ligne[colonne] = valeur;
      }
    });
    if (ligne.matricule || ligne.nom || ligne.prenom) lignes.push(ligne);
  }
  return lignes;
}

/** `12/03/2004`, `12-03-04` ou déjà `2004-03-12` → `2004-03-12` ; sinon la valeur est écartée. */
export function dateIso(valeur: string): string | null {
  const iso = /^(\d{4})-(\d{2})-(\d{2})/.exec(valeur);
  if (iso) return `${iso[1]}-${iso[2]}-${iso[3]}`;
  const fr = /^(\d{1,2})[/.-](\d{1,2})[/.-](\d{2}|\d{4})$/.exec(valeur.trim());
  if (!fr) return null;
  const annee = fr[3].length === 2 ? (Number(fr[3]) > 40 ? `19${fr[3]}` : `20${fr[3]}`) : fr[3];
  return `${annee}-${fr[2].padStart(2, '0')}-${fr[1].padStart(2, '0')}`;
}

// ---------------------------------------------------------------------------
// Texte libre (photo d'une liste)
// ---------------------------------------------------------------------------

/** Les jetons en capitales forment le nom, les autres le prénom : « TRAORE Moussa ». */
function separerNomPrenom(jetons: string[]): { nom: string; prenom: string } {
  const propres = jetons.filter((j) => /\p{L}/u.test(j));
  const majuscules = propres.filter((j) => j.length > 1 && j === j.toUpperCase());
  if (majuscules.length > 0 && majuscules.length < propres.length) {
    return {
      nom: majuscules.join(' '),
      prenom: propres.filter((j) => !majuscules.includes(j)).join(' '),
    };
  }
  return { nom: propres[0] ?? '', prenom: propres.slice(1).join(' ') };
}

/**
 * Lit le texte reconnu sur une photo, une ligne par étudiant.
 *
 * <p>La reconnaissance de caractères se trompe parfois (0/O, 1/l) : le résultat n'est jamais
 * importé tel quel, il est présenté à l'écran pour correction.</p>
 */
export function depuisTexteLibre(texte: string): LigneImport[] {
  const lignes: LigneImport[] = [];
  for (const brute of texte.split(/\r?\n/)) {
    // Retire le numéro d'ordre de la liste (« 12. », « 03 - ») et les séparateurs de tableau.
    let ligne = brute.replace(/[|_]+/g, ' ').replace(/^\s*\d{1,3}\s*[.)\-:]\s+/, '').trim();
    if (ligne.length < 4) continue;

    const email = /[\w.+-]+@[\w-]+\.[\w.-]+/.exec(ligne)?.[0] ?? null;
    if (email) ligne = ligne.replace(email, ' ');

    const jetons = ligne.split(/\s+/).filter(Boolean);
    const indexMatricule = jetons.findIndex((j) => /\d/.test(j) && j.length >= 3 && /^[\w\-/.]+$/.test(j));
    if (indexMatricule < 0) continue;
    const matricule = jetons[indexMatricule].replace(/[.,;]+$/, '');
    const reste = jetons.filter((_, i) => i !== indexMatricule);

    const indexTel = reste.findIndex((j) => /^\+?\d[\d .]{7,}$/.test(j));
    const telephone = indexTel >= 0 ? reste[indexTel] : null;
    if (indexTel >= 0) reste.splice(indexTel, 1);

    const { nom, prenom } = separerNomPrenom(reste);
    if (!nom) continue;
    lignes.push({ matricule, nom, prenom, email, telephone });
  }
  return lignes;
}
