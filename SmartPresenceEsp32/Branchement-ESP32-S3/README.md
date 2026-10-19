# Branchement du lecteur SmartPresence sur ESP32-S3 N16R8

Ce dossier explique, pas à pas, comment câbler et mettre en service le lecteur d'empreintes
sur une carte **ESP32-S3-DevKitC-1 N16R8** (16 Mo de flash, 8 Mo de PSRAM octale).

> **État du projet.** Le firmware est prêt et compile pour cette carte, mais **n'a pas encore
> été essayé sur le matériel** : tout ce qui suit est à valider au premier branchement. La
> dernière section, [`COMPTE-RENDU.md`](COMPTE-RENDU.md), est faite pour noter ce qui a marché
> ou non. Rien du lecteur n'est branché au reste du système tant que cette validation n'est pas faite.

Schéma : [`schema-branchement.svg`](schema-branchement.svg) (à ouvrir dans le navigateur).

---

## 1. Matériel

| Pièce | Remarque |
|---|---|
| ESP32-S3-DevKitC-1 **N16R8** | Deux ports USB-C : utiliser celui marqué **UART**, pas celui marqué **USB** |
| Capteur d'empreintes **AS608** | Alimenter en **3,3 V** |
| Horloge **DS3231** | Garde l'heure hors tension (pile CR2032) |
| Afficheur **OLED SSD1306** 128×64, I2C | Adresse `0x3C` |
| 2 LED (verte, rouge) + 2 résistances **220 Ω** | |
| Buzzer **passif** | Un buzzer actif ne module pas les notes : les signaux sonores seraient tous identiques |
| Bouton poussoir | Appui long = enrôlement par bouton |
| Plaque de montage et fils Dupont | |

## 2. Table de câblage

| Composant | Broche du composant | Broche de la carte |
|---|---|---|
| AS608 | VCC | **3V3** |
| AS608 | GND | **GND** |
| AS608 | TX | **GPIO 18** |
| AS608 | RX | **GPIO 17** |
| AS608 | WAKEUP / TOUCH | **GPIO 4** |
| DS3231 | VCC | 3V3 |
| DS3231 | GND | GND |
| DS3231 | SDA | **GPIO 8** |
| DS3231 | SCL | **GPIO 9** |
| SSD1306 | VCC | 3V3 |
| SSD1306 | GND | GND |
| SSD1306 | SDA | GPIO 8 (le même que le DS3231) |
| SSD1306 | SCL | GPIO 9 (le même que le DS3231) |
| LED verte | anode via 220 Ω | **GPIO 5** (cathode à GND) |
| LED rouge | anode via 220 Ω | **GPIO 6** (cathode à GND) |
| Buzzer passif | + | **GPIO 7** (− à GND) |
| Bouton | une patte | **GPIO 15** (autre patte à GND) |

**Trois points qui piègent.**

1. **TX et RX sont croisés** : la sortie (TX) du capteur entre dans la carte (GPIO 18),
   l'entrée (RX) du capteur reçoit de la carte (GPIO 17). Un branchement « droit » donne un
   capteur muet, sans autre symptôme.
2. **Le DS3231 et l'afficheur partagent les deux mêmes fils** (SDA et SCL). C'est normal :
   leurs adresses diffèrent (`0x68` et `0x3C`).
3. **Le bouton n'a pas de résistance externe** : le firmware active la résistance interne.
   Il suffit de le relier entre la broche et la masse.

### Pourquoi ces broches et pas celles de l'ESP32 classique

Sur la N16R8, les **GPIO 26 à 32 portent la flash** et les **GPIO 33 à 37 la PSRAM
octale** : y brancher quoi que ce soit fige la carte au démarrage. Le brochage de l'ESP32
classique (LED en 25/26, buzzer en 27, bouton en 32) ne peut donc pas être repris tel quel.
Sont aussi évités : GPIO 0, 3, 45, 46 (configuration au démarrage), 19 et 20 (USB natif),
43 et 44 (console série) et 48 (diode RVB de la carte).

Le brochage vit dans un seul fichier, [`../src/config.h`](../src/config.h), branche
`CONFIG_IDF_TARGET_ESP32S3`. **Pour changer une broche, modifier ce fichier — et ce guide.**

## 3. Alimentation

- Alimenter la carte par le **port USB marqué UART**, avec un câble qui transmet les données
  (certains câbles ne servent qu'à charger : la carte s'allume, mais n'apparaît pas sur le PC).
- Le capteur AS608 consomme jusqu'à environ 120 mA pendant une lecture. Le 3V3 de la carte
  y suffit ; si l'afficheur clignote ou si la carte redémarre à chaque lecture, utiliser une
  alimentation USB plus robuste (chargeur 5 V / 1 A, pas un port de PC faible).
- Ne **jamais** relier la broche 5V de la carte aux entrées des composants : leurs signaux
  ne supportent que 3,3 V.

## 4. Installer le firmware

Prérequis : PlatformIO (déjà utilisé pour le projet) et le pilote de la puce **CH343** si le
PC ne voit pas de port COM quand la carte est branchée.

```bash
cd SmartPresenceEsp32
cp src/identifiants.exemple.h src/identifiants.h   # si ce n'est pas déjà fait
```

Renseigner `src/identifiants.h` (voir la section 5), puis :

```bash
pio run -e esp32s3                  # compilation
pio run -e esp32s3 -t upload        # téléversement
pio device monitor -e esp32s3       # traces, 115200 bauds
```

Si le téléversement échoue : maintenir le bouton **BOOT**, appuyer brièvement sur **RST**,
relâcher **BOOT**, puis relancer la commande.

## 5. Préparer le serveur

Le lecteur s'authentifie par une clé d'API, pas par un compte. Dans l'administration web :

1. Écran **Appareils** → créer un lecteur d'usage **Étudiants**.
2. Noter la **clé d'API** (elle n'est affichée qu'une fois) et l'**identifiant** du lecteur.
3. Dans `src/identifiants.h` :

| Constante | Valeur |
|---|---|
| `WIFI_SSID`, `WIFI_MOT_DE_PASSE` | Le réseau Wi-Fi **2,4 GHz** (l'ESP32-S3 ne gère pas le 5 GHz) |
| `URL_SERVEUR` | `http://<adresse IP du PC>:8080`, par exemple `http://192.168.1.20:8080` |
| `CLE_API` | La clé notée à l'étape 2 |
| `DEVICE_ID` | L'identifiant noté à l'étape 2 |
| `PREFIXE_BIOMETRIQUE` | Court et **unique par lecteur** (ex. `ENETP-B12`) |

**Limites actuelles à connaître :**

- Le firmware parle en **HTTP simple**. Il ne peut donc pas encore joindre le serveur déployé
  sur Render, qui n'accepte que du HTTPS. Pour les premiers essais, lancer le backend sur le
  PC (profil `local`) et pointer vers l'adresse IP du PC. La prise en charge du HTTPS est la
  suite logique.
- Le **pare-feu Windows** doit autoriser les connexions entrantes sur le port **8080**, et le
  PC et le lecteur doivent être sur le **même réseau**.
- Le lecteur envoie toujours « PRÉSENT » : il ne juge pas des retards (il ne connaît pas
  l'emploi du temps). C'est voulu, le serveur rattache le passage à une séance.

## 6. Ce qu'il faut voir au démarrage

Dans le moniteur série, dans cet ordre :

```
=== SmartPresence — lecteur ESP32 ===
[empreinte] AS608 pret, N empreinte(s) en memoire
[api] Wi-Fi connecte, adresse 192.168.x.x
[demarrage] N empreinte(s) au capteur, 0 en file
```

L'afficheur montre l'heure et l'état du réseau.

## 7. Plan de test (à dérouler dans l'ordre)

Cocher chaque ligne dans [`COMPTE-RENDU.md`](COMPTE-RENDU.md).

| # | Test | Résultat attendu |
|---|---|---|
| 1 | Brancher la carte seule, sans composant | Le PC voit un port COM ; le moniteur affiche les traces de démarrage |
| 2 | Ajouter l'afficheur | Il s'allume, indique l'heure |
| 3 | Ajouter le DS3231 | Pas de message « Horloge absente » ; l'heure se recale sur le réseau |
| 4 | Ajouter l'AS608 | Trace `AS608 pret` ; sinon « Capteur absent » (voir dépannage) |
| 5 | Poser un doigt non enrôlé | « Non reconnu » + note grave + LED rouge |
| 6 | **Enrôlement par bouton** : appui long (1,5 s) | L'afficheur guide ; à la fin la **référence** s'affiche (ex. `ENETP-B12-001`) |
| 7 | **Enrôlement libre-service** (voir ci-dessous) | L'étudiant est lié à son dossier sans intervention de l'administration |
| 8 | Poser le doigt enrôlé | LED verte, trois notes montantes, référence affichée |
| 9 | Couper le Wi-Fi, faire 2 passages, rétablir | Les passages sont envoyés après reconnexion, sans doublon |
| 10 | Vérifier côté application (relevé de présence) | Le passage apparaît, rattaché à la bonne séance |

### Enrôlement en libre-service (test 7)

1. Avoir un étudiant dont le numéro CENOU est au référentiel et qui s'est inscrit (écran
   « Créer mon compte » de l'application mobile).
2. L'étudiant ouvre l'onglet **Empreinte** de l'application → « Obtenir mon code » : un code à
   six chiffres s'affiche.
3. Dans les quatre secondes qui suivent, le lecteur affiche **le même code et le nom** de
   l'étudiant.
4. L'étudiant **compare le code et son nom** puis pose le doigt, le retire, le repose.
5. L'application bascule sur « Empreinte enregistrée » toute seule.

Si le serveur est injoignable après la capture, le lecteur affiche la référence : la saisir à
la main dans l'administration (même chemin que le test 6).

## 8. Dépannage

| Symptôme | Cause probable | Action |
|---|---|---|
| Aucun port COM sur le PC | Câble « charge seule », mauvais port USB-C, pilote CH343 absent | Autre câble, port **UART**, installer le pilote |
| La carte redémarre en boucle, trace `Octal PSRAM` ou `flash id` | Mauvais type de mémoire | Garder les trois lignes `memory_type` / `flash_size` / `flash_mode` de `platformio.ini` |
| La carte se fige dès qu'on branche un fil | Broche 26–37 utilisée | Reprendre exactement les broches de la table |
| « Capteur absent » | TX/RX non croisés, ou VCC absent | Vérifier le croisement ; mesurer 3,3 V sur VCC |
| « Horloge absente » | SDA/SCL inversés ou DS3231 mal alimenté | Revoir GPIO 8 / 9 |
| Afficheur noir | Mauvaise adresse ou fils inversés | L'adresse attendue est `0x3C` |
| `Wi-Fi injoignable` | Réseau 5 GHz, mauvais mot de passe | Réseau 2,4 GHz, vérifier `identifiants.h` |
| `echec HTTP -1` | Serveur injoignable | IP du PC, pare-feu (port 8080), même réseau, backend lancé |
| `echec HTTP 401` ou `403` | Clé d'API fausse, ou usage du lecteur inadapté | Reprendre la clé ; l'usage doit être **Étudiants** |
| Le code d'enrôlement ne s'affiche jamais | Pas de demande ouverte, ou lecteur non joint | Redemander un code dans l'application ; surveiller `[api]` dans le moniteur |
| Passages relevés mais absents de l'application | Pas de séance en cours pour la classe, ou étudiant non lié | Voir l'emploi du temps ; vérifier l'enrôlement |

## 9. Ce qui n'a pas été fait volontairement

- Aucun changement du serveur ni de l'application pour cette carte : le contrat avec le
  serveur est le même que pour l'ESP32 classique.
- Le firmware pour l'ESP32 classique reste intact et compile toujours
  (`pio run -e esp32dev`).
- La PSRAM n'est pas utilisée : ce firmware n'en a pas besoin.
