# SmartPresence — lecteur biométrique ESP32

Firmware du lecteur d'empreintes de l'ENETP. Il identifie un doigt sur un capteur AS608,
horodate le passage avec une horloge DS3231, l'écrit en mémoire non volatile, puis le
transmet au serveur SmartPresence.

**Le guide de branchement est dans [`docs/Guide-branchement-lecteur-ESP32.pdf`](docs/Guide-branchement-lecteur-ESP32.pdf).**
Il décrit le câblage attendu par ce code, broche par broche.

## Démarrage

```bash
cp src/identifiants.exemple.h src/identifiants.h   # puis renseignez les valeurs
pio run                                            # compilation
pio run -t upload                                  # téléversement
pio device monitor                                 # traces, 115200 bauds
```

## Organisation

| Fichier | Rôle |
|---|---|
| `src/config.h` | Brochage et constantes — **référence unique du câblage** |
| `src/identifiants.h` | Wi-Fi, URL du serveur, clé d'API (hors dépôt) |
| `src/main.cpp` | Boucle principale, modes pointage et enrôlement |
| `src/empreinte.*` | Capteur AS608 — identification et enrôlement |
| `src/horloge.*` | DS3231 et recalage NTP |
| `src/file_attente.*` | File des relevés non acquittés, en NVS |
| `src/api.*` | Envoi des lots au serveur |
| `src/ihm.*` | Afficheur, diodes, buzzer |

## Deux principes qui gouvernent ce code

**Aucune donnée biométrique ne sort du capteur.** Le gabarit reste dans la mémoire de
l'AS608 ; seul un numéro d'emplacement en sort, converti en référence logique du type
`ENETP-B12-007`. Cette référence ne permet pas de reconstituer une empreinte et n'a de
sens que rapprochée de la table d'enrôlement du serveur.

**Aucun passage n'est perdu.** Le relevé est écrit en mémoire non volatile *avant* toute
tentative d'envoi, et n'en est retiré qu'une fois le serveur confirmé. Un lot rejoué après
une coupure ne crée pas de doublon : le serveur écarte les événements déjà connus sur la
clé `(appareil, personne, date, heure)`.

## Ce que le lecteur ne fait pas

Il ne connaît pas l'emploi du temps et ne décide pas d'un retard : il constate un passage
à une heure, et c'est au serveur de le rattacher à une séance. Le sens entrée/sortie du
personnel est également déduit côté serveur, ce qui garde le firmware simple et juste même
après un rejeu dans le désordre.
