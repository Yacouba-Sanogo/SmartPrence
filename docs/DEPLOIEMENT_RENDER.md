# Déployer le backend SmartPresence sur Render

Objectif : rendre l'API accessible sur une URL publique en HTTPS
(`https://smartprence.onrender.com/api/...`), joignable depuis le
frontend, l'application mobile et le lecteur ESP32.

---

## 0. Le point qui décide de tout : la base de données

**Render n'héberge pas de MySQL.** Sa base managée est PostgreSQL uniquement.
Deux chemins s'ouvrent donc :

| Chemin | Ce qu'il implique | Verdict |
|---|---|---|
| **Garder MySQL**, hébergé chez un autre fournisseur | Aucune ligne de code à changer : seule `DB_URL` pointe ailleurs | **Recommandé** — le mémoire décrit une architecture MySQL |
| Migrer vers PostgreSQL (base Render) | Changer le pilote, le dialecte, revalider tout le schéma | À éviter en fin de projet |

Le code n'utilise ni requête native ni `columnDefinition` : la migration
PostgreSQL serait techniquement faisable, mais elle rouvrirait la question du
mapping des UUID — précisément le piège qui avait déjà coûté une panne
silencieuse. On garde MySQL.

---

## 1. Créer la base MySQL managée (≈ 5 min)

Fournisseur conseillé : **Aiven** (plan gratuit, MySQL 8, TLS obligatoire).
Alternative : **Alwaysdata** (100 Mo gratuits, hébergeur français).

1. Créer un compte sur `aiven.io`.
2. *Create service* → **MySQL** → plan **Free** → région européenne
   (prendre la même zone que Render : Francfort ou Paris).
3. Attendre que le service passe en *Running*, puis relever dans l'onglet
   *Connection information* : **Host**, **Port**, **User**, **Password**,
   **Database name** (souvent `defaultdb`).
4. En déduire l'URL JDBC :

```
jdbc:mysql://HOTE:PORT/BASE?sslMode=REQUIRED&serverTimezone=UTC
```

> ⚠️ Ne pas recopier l'URL de développement : `useSSL=false` est refusé par une
> base managée, et `createDatabaseIfNotExist=true` réclame un droit que le
> compte fourni n'a pas. Le schéma, lui, sera créé au premier démarrage par
> Hibernate (`ddl-auto: update`).

---

## 2. Pousser le dépôt

Render construit à partir de GitHub. Le dépôt `Yacouba-Sanogo/SmartPrence` est
déjà en place ; il suffit que la branche `main` soit à jour :

```bash
git add -A && git commit -m "Préparation du déploiement Render" && git push
```

Les trois adaptations nécessaires sont déjà faites dans le code :

| Fichier | Changement | Pourquoi |
|---|---|---|
| `application.yml` | `port: ${PORT:8080}` | Render impose son port par la variable `PORT` ; un port figé et le service est déclaré « unhealthy » puis arrêté |
| `application.yml` | `url: ${DB_URL:...}` | Permet de fournir l'URL complète (TLS, base existante) sans rien retirer au confort du développement local |
| `Dockerfile` | `PORT` au lieu de `SERVER_PORT`, plus `JAVA_OPTS` | `SERVER_PORT` aurait écrasé la configuration Spring ; et sur 512 Mo la JVM ne se donne que ~128 Mo de tas par défaut |

---

## 3. Créer le service web sur Render

Dans le tableau de bord Render : **New +** → **Web Service** → connecter le
compte GitHub → choisir `SmartPrence`, branche `main`.

Réglages :

| Champ | Valeur |
|---|---|
| **Name** | `smartpresence-backend` |
| **Language / Runtime** | **Docker** |
| **Branch** | `main` |
| **Root Directory** | *laisser vide* |
| **Dockerfile Path** | `./BackendSmartPresence/Dockerfile` |
| **Docker Build Context Directory** | `./BackendSmartPresence` |
| **Region** | Frankfurt (au plus près de la base) |
| **Instance Type** | Free |
| **Health Check Path** | `/api/v3/api-docs` |

Ces deux chemins sont le réglage clé du dépôt multi-projets, et ils se
comptent **tous les deux depuis la racine du dépôt** — pas l'un depuis l'autre.
Le contexte donne à `COPY` accès au backend ; le chemin du Dockerfile désigne le
fichier lui-même. Combiner `Root Directory` avec un chemin déjà complet revient
à préfixer deux fois et fait échouer la construction.

Astuce : les deux champs proposent une liste déroulante alimentée par le contenu
réel du dépôt — les choisir plutôt que les taper évite la faute de frappe.

### Variables d'environnement

À saisir dans *Environment* → *Add Environment Variable* :

| Clé | Valeur |
|---|---|
| `DB_URL` | l'URL JDBC de l'étape 1 |
| `DB_USERNAME` | l'utilisateur de la base (`avnadmin` chez Aiven) |
| `DB_PASSWORD` | le mot de passe de la base |
| `JWT_SECRET` | une chaîne aléatoire d'**au moins 32 caractères** |
| `DEVICE_API_KEY` | la clé du lecteur ESP32 (identique à `identifiants.h`) |
| `BOOTSTRAP_ADMIN_EMAIL` | l'adresse du premier administrateur |
| `BOOTSTRAP_ADMIN_PASSWORD` | son mot de passe |

> `JWT_SECRET` est utilisé tel quel, octet par octet, pour construire la clé
> HMAC-SHA256 : en dessous de 32 caractères, jjwt refuse de signer et **toute
> connexion échoue**. Pour en fabriquer une :
>
> ```bash
> openssl rand -base64 48
> ```

Puis **Create Web Service**. La première construction prend 5 à 10 minutes :
Maven télécharge toutes les dépendances à l'intérieur de l'image.

### Variante : le fichier `render.yaml`

Un blueprint est fourni à la racine du dépôt. **New +** → **Blueprint** →
choisir le dépôt : Render lit `render.yaml`, crée le service avec les bons
réglages et ne demande plus que les secrets.

---

## 4. Vérifier que ça tourne vraiment

Dans l'onglet *Logs*, attendre la ligne `Started BackendSmartPresenceApplication`.
Puis, depuis un terminal :

```bash
curl -i https://smartprence.onrender.com/api/v3/api-docs
```

Un `200` prouve que le contexte Spring est monté **et** que la base répond —
sans elle, l'application n'aurait pas démarré du tout. Ensuite, la connexion :

```bash
curl -X POST https://smartprence.onrender.com/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"VOTRE_EMAIL_ADMIN\",\"motDePasse\":\"VOTRE_MOT_DE_PASSE\"}"
```

Swagger reste consultable sur `/api/swagger-ui.html`.

**Si le déploiement échoue**, les trois causes habituelles :

| Symptôme dans les logs | Cause |
|---|---|
| `Communications link failure` ou `Access denied` | `DB_URL`, `DB_USERNAME` ou `DB_PASSWORD` erronés, ou `sslMode` absent |
| `failed to read dockerfile: ... /src: is a directory` | le *Dockerfile Path* désigne un dossier : il doit finir par `/Dockerfile` |
| `No open ports detected` | la variable `PORT` a été écrasée par une valeur figée |
| `The specified key byte array is N bits which is not secure enough` | `JWT_SECRET` trop court |

---

## 5. Les clients, déjà branchés

**Application mobile** — `lib/core/api/config_api.dart` choisit désormais seul :

| Situation | Adresse retenue |
|---|---|
| `--dart-define=API_BASE_URL=...` fourni | celle-là, toujours prioritaire |
| Compilation *release* (l'APK distribué) | `https://smartprence.onrender.com/api` |
| Développement (`flutter run`) | `10.0.2.2` sur émulateur, `localhost` ailleurs |

Le délai maximal suit la même bascule : 60 secondes vers le serveur distant, 15
en local. L'instance gratuite met environ 27 secondes à se réveiller, et les 15
secondes du développement faisaient expirer le premier appel de la journée sur
un serveur pourtant sain.

```bash
flutter build apk --release
```

Plus besoin de `--dart-define` : l'adresse de production est la valeur par
défaut d'une compilation *release*.

**Frontend Angular** — `proxy.conf.json` vise maintenant Render. Le code
continue d'appeler `/api` en même origine, donc rien d'autre n'a bougé :

```json
{ "/api": { "target": "https://smartprence.onrender.com", "secure": true, "changeOrigin": true } }
```

> ⚠️ `ng serve` écrit donc dans la **base de production**. Pour revenir au
> backend local, remettre `"target": "http://localhost:8080"` et `"secure": false`.

Une fois déployé sur Render en *Static Site*, c'est la règle de réécriture
`/api/*` déclarée dans `render.yaml` qui joue le rôle du proxy.

**Lecteur ESP32** — le seul client encore en HTTP. En HTTPS,
`HTTPClient::begin(url)` exige un client TLS explicite :

```cpp
WiFiClientSecure client;
client.setInsecure();          // pas de vérification de certificat
http.begin(client, String(URL_SERVEUR) + cheminIngestion());
```

Sans cela, la requête ne part jamais et le lot reste en file d'attente.

---

## 6. Ce que coûte le plan gratuit

- **Mise en veille après 15 minutes sans trafic.** Le premier appel suivant
  prend près d'une minute — le temps de redémarrer la JVM. Pour l'ESP32, dont
  le délai de requête est court, cela se traduit par un lot en échec au premier
  pointage de la journée.
- **512 Mo de RAM et un cœur partagé.** Suffisant pour cette API, d'où le
  réglage `MaxRAMPercentage=70` ajouté au `Dockerfile`.
- Pour une soutenance, deux parades : un *cron* externe (cron-job.org) qui
  appelle `/api/v3/api-docs` toutes les 10 minutes pour garder l'instance
  éveillée, ou l'offre payante à 7 $/mois le temps de la démonstration.
