#!/usr/bin/env node
/*
 * Capture des écrans de l'aperçu (tool/apercu_accueil.dart) à la taille exacte d'un téléphone.
 *
 * Chrome impose une largeur minimale de fenêtre qui élargit la mise en page ; on pilote donc le
 * protocole DevTools pour émuler 390 x 844 à l'échelle 3, comme un téléphone récent.
 *
 *   flutter build web --release -t tool/apercu_accueil.dart --output build/apercu
 *   python -m http.server 5003 --directory build/apercu
 *   node tool/capturer.js http://127.0.0.1:5003 ./captures accueil connexion landing
 */
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const [base, sortie, ...ecrans] = process.argv.slice(2);
const chrome = process.env.CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const port = 9333;
const dossier = path.join(process.env.TEMP || '.', 'capture-profil-' + Date.now());

const attendre = (ms) => new Promise((r) => setTimeout(r, ms));

(async () => {
  fs.mkdirSync(sortie, { recursive: true });
  const proc = spawn(chrome, [
    '--headless=new', '--disable-gpu', '--hide-scrollbars', `--remote-debugging-port=${port}`,
    `--user-data-dir=${dossier}`, '--no-first-run', 'about:blank',
  ], { stdio: 'ignore' });

  let cible;
  for (let i = 0; i < 40 && !cible; i++) {
    await attendre(500);
    try {
      const onglets = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
      cible = onglets.find((o) => o.type === 'page');
    } catch { /* Chrome démarre */ }
  }
  if (!cible) throw new Error('Chrome ne répond pas');

  const ws = new WebSocket(cible.webSocketDebuggerUrl);
  await new Promise((r) => (ws.onopen = r));
  let id = 0;
  const attente = new Map();
  ws.onmessage = (m) => {
    const d = JSON.parse(m.data);
    if (d.id && attente.has(d.id)) attente.get(d.id)(d);
  };
  const cmd = (method, params = {}) =>
    new Promise((r) => {
      const n = ++id;
      attente.set(n, r);
      ws.send(JSON.stringify({ id: n, method, params }));
    });

  await cmd('Page.enable');
  await cmd('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 3, mobile: true });
  // Premier chargement à froid : on le passe pour que la première capture soit prête.
  await cmd('Page.navigate', { url: base + '/?ecran=chauffe' });
  await attendre(12000);
  for (const ecran of ecrans) {
    // Suffixes : -bas (défile), -sombre (mode sombre), -grand (texte très grand).
    const nom = ecran.replace('-bas', '').replace('-sombre', '').replace('-grand', '');
    const options = (ecran.includes('-sombre') ? '&sombre=1' : '') + (ecran.includes('-grand') ? '&texte=1.3' : '');
    await cmd('Page.navigate', { url: `${base}/?ecran=${nom}${options}` });
    await attendre(9000);
    const defiler = Number(process.env.DEFILER || 0);
    if (defiler > 0 && ecran.includes('-bas')) {
      await cmd('Input.dispatchMouseEvent', { type: 'mouseWheel', x: 200, y: 400, deltaX: 0, deltaY: defiler });
      await attendre(1200);
    }
    const r = await cmd('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(path.join(sortie, `${ecran}.png`), Buffer.from(r.result.data, 'base64'));
    console.log('capture', ecran);
  }
  ws.close();
  proc.kill();
})().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
