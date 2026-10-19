# Compte rendu de mise en service — ESP32-S3 N16R8

À remplir après le premier branchement, puis à me transmettre. Cocher ce qui marche, noter
ce qui diffère, **coller les traces du moniteur série en cas d'échec**.

- Date : ………………………   Carte exacte (inscription sur le module) : ………………………
- Version PlatformIO : ………   Port COM : ………   Réseau Wi-Fi 2,4 GHz : oui / non
- Backend : local (`http://…:8080`) / autre : ………………………

## Résultats des tests (voir le plan, section 7 du guide)

| # | Test | OK | Observation / trace |
|---|---|:-:|---|
| 1 | Carte seule, port COM visible, traces au démarrage | ☐ | |
| 2 | Afficheur allumé | ☐ | |
| 3 | DS3231 détecté, heure recalée | ☐ | |
| 4 | AS608 détecté (`AS608 pret`) | ☐ | |
| 5 | Doigt inconnu → « Non reconnu » | ☐ | |
| 6 | Enrôlement par bouton, référence affichée | ☐ | |
| 7 | Enrôlement libre-service (code identique, lié au dossier) | ☐ | |
| 8 | Doigt enrôlé reconnu (LED verte, notes) | ☐ | |
| 9 | Passages hors ligne renvoyés sans doublon | ☐ | |
| 10 | Passage visible dans l'application, bonne séance | ☐ | |

## Écarts de câblage constatés

(Broche réellement utilisée à la place de celle du guide, fil changé, composant différent…)

……………………………………………………………………………………………………………………

## Problèmes rencontrés

(Message exact du moniteur série, ce qui a été essayé, ce qui a fini par marcher.)

……………………………………………………………………………………………………………………

## Mesures utiles si une alimentation pose problème

- Tension sur 3V3 au repos : ……… V — pendant une lecture : ……… V
- Alimentation utilisée : port PC / chargeur ……… A

## À décider ensuite

☐ Passer en HTTPS pour joindre le serveur Render
☐ Boîtier / fixation du lecteur
☐ Autres lecteurs à déployer (un préfixe biométrique distinct par lecteur) : ………
