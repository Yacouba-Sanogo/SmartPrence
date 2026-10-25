#!/usr/bin/env node
/*
 * Remplit le compte d'un étudiant avec des données de démonstration cohérentes :
 * séances des deux dernières semaines et des prochains jours, relevés de présence
 * (présent, retard, absent) et matières si le référentiel est vide.
 *
 * Ce sont l'administration et la scolarité qui créent séances et relevés : l'étudiant ne
 * peut pas les créer lui-même. Le script se connecte donc en administrateur, avec des
 * identifiants lus dans l'environnement — jamais dans le fichier ni dans la ligne de commande.
 *
 * PowerShell :
 *   $env:ADMIN_EMAIL = "admin@exemple.ml"
 *   $env:ADMIN_PASSWORD = Read-Host "Mot de passe administrateur"
 *   node tool/remplir-compte-etudiant.js
 *
 * Variables : API_BASE (défaut : Render), ETUDIANT_EMAIL (défaut : moussa@gmail.com) ou ETUDIANT_MATRICULE.
 * Le script est rejouable : il ne recrée pas une séance déjà présente le même jour.
 */
const API = (process.env.API_BASE || 'https://smartprence.onrender.com/api').replace(/\/$/, '');
const MATRICULE_ETUDIANT = (process.env.ETUDIANT_MATRICULE || '').toUpperCase();
const EMAIL_ETUDIANT = (process.env.ETUDIANT_EMAIL || 'moussa@gmail.com').toLowerCase();

const MATIERES_PAR_DEFAUT = [
  ['ALGO1', 'Algorithmique'],
  ['MATH1', 'Analyse mathématique'],
  ['ARCHI1', 'Architecture des ordinateurs'],
  ['ANG1', 'Anglais technique'],
  ['BDD1', 'Bases de données'],
];

let jeton = null;

async function appeler(methode, chemin, corps) {
  const reponse = await fetch(API + chemin, {
    method: methode,
    headers: { 'Content-Type': 'application/json', ...(jeton ? { Authorization: 'Bearer ' + jeton } : {}) },
    body: corps ? JSON.stringify(corps) : undefined,
    signal: AbortSignal.timeout(90000),
  });
  const texte = await reponse.text();
  const json = texte ? JSON.parse(texte) : {};
  if (!reponse.ok) {
    const erreur = new Error(`${methode} ${chemin} → ${reponse.status} : ${json.message || texte.slice(0, 120)}`);
    erreur.statut = reponse.status;
    throw erreur;
  }
  return json.data;
}

const jour = (d) => d.toISOString().slice(0, 10);

(async () => {
  if (!process.env.ADMIN_EMAIL || !process.env.ADMIN_PASSWORD) {
    console.error('Renseignez ADMIN_EMAIL et ADMIN_PASSWORD (voir l\'en-tête du fichier).');
    process.exit(1);
  }
  console.log('API :', API, '(le serveur gratuit peut mettre 30 s à se réveiller)');

  const session = await appeler('POST', '/auth/login', {
    email: process.env.ADMIN_EMAIL,
    motDePasse: process.env.ADMIN_PASSWORD,
  });
  jeton = session.accessToken;

  const etudiants = await appeler('GET', '/etudiants');
  const etudiant = etudiants.find((e) =>
    MATRICULE_ETUDIANT ? (e.matricule || '').toUpperCase() === MATRICULE_ETUDIANT : (e.email || '').toLowerCase() === EMAIL_ETUDIANT,
  );
  if (!etudiant) throw new Error(`Aucun étudiant trouvé (${MATRICULE_ETUDIANT || EMAIL_ETUDIANT}). Inscrivez-le d'abord.`);
  if (!etudiant.classeId) throw new Error("Cet étudiant n'a pas de classe.");
  console.log(`Étudiant : ${etudiant.prenom} ${etudiant.nom} — classe ${etudiant.classeCode}`);

  const classe = await appeler('GET', `/classes/${etudiant.classeId}`);
  const enseignant = (classe.enseignants || [])[0];
  if (!enseignant) {
    throw new Error(`La classe ${etudiant.classeCode} n'a aucun enseignant rattaché : ouvrez Classes → Modifier et cochez-en un.`);
  }

  let matieres = await appeler('GET', '/matieres');
  if (matieres.length === 0) {
    for (const [code, libelle] of MATIERES_PAR_DEFAUT) {
      await appeler('POST', '/matieres', { code, libelle, credits: 3, active: true });
    }
    matieres = await appeler('GET', '/matieres');
    console.log(`${matieres.length} matières créées`);
  }
  matieres = matieres.slice(0, 5);

  let salles = await appeler('GET', '/salles');
  if (salles.length === 0) {
    await appeler('POST', '/salles', { code: 'AMPHI-A', nom: 'Amphi A', batiment: 'Bloc principal', capacite: 120 });
    await appeler('POST', '/salles', { code: 'S-204', nom: 'Salle 204', batiment: 'Bloc B', capacite: 40 });
    salles = await appeler('GET', '/salles');
    console.log('2 salles créées');
  }

  const aujourdhui = new Date();
  aujourdhui.setUTCHours(0, 0, 0, 0);
  const debut = new Date(aujourdhui.getTime() - 14 * 86400000);
  const fin = new Date(aujourdhui.getTime() + 8 * 86400000);
  const existantes = await appeler('GET', `/seances?debut=${jour(debut)}&fin=${jour(fin)}&classeId=${etudiant.classeId}`);
  const joursPris = new Set(existantes.map((s) => String(s.debut).slice(0, 10)));

  let creees = 0;
  let releves = 0;
  let rang = 0;
  const maintenant = Date.now();
  for (let d = new Date(debut); d <= fin; d = new Date(d.getTime() + 86400000)) {
    const semaine = d.getUTCDay();
    if (semaine === 0 || semaine === 6 || joursPris.has(jour(d))) continue;

    const heureDebut = new Date(d.getTime() + 8 * 3600000);
    const heureFin = new Date(heureDebut.getTime() + 2 * 3600000);
    const matiere = matieres[rang % matieres.length];
    const salle = salles[rang % salles.length];
    rang++;

    let seance;
    try {
      seance = await appeler('POST', '/seances', {
        classeId: etudiant.classeId,
        matiereId: matiere.id,
        enseignantId: enseignant.id,
        salleId: salle.id,
        debut: heureDebut.toISOString(),
        fin: heureFin.toISOString(),
      });
      creees++;
    } catch (e) {
      console.warn('  séance ignorée :', e.message);
      continue;
    }

    if (heureFin.getTime() < maintenant) {
      // Un retard et une absence parmi les séances passées, le reste en présence.
      const statut = rang === 4 ? 'RETARD' : rang === 7 ? 'ABSENT' : 'PRESENT';
      if (statut !== 'ABSENT') {
        const heure = statut === 'RETARD' ? '08:17:00' : '07:55:00';
        await appeler('POST', '/presences/manual', {
          etudiantId: etudiant.id,
          seanceId: seance.id,
          datePresence: jour(d),
          heurePresence: heure,
          statut,
        });
        releves++;
      } else {
        await appeler('POST', '/presences/manual', {
          etudiantId: etudiant.id,
          seanceId: seance.id,
          datePresence: jour(d),
          heurePresence: '08:00:00',
          statut: 'ABSENT',
        });
        releves++;
      }
    }
  }

  console.log(`Terminé : ${creees} séance(s) planifiée(s), ${releves} relevé(s) de présence.`);
  console.log('Ouvrez l\'application, tirez l\'écran d\'accueil vers le bas pour actualiser.');
})().catch((e) => {
  console.error('Échec :', e.message);
  process.exit(1);
});
