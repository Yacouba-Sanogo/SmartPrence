# Documentation SmartPresence

| Document | Sujet |
|---|---|
| [SmartPresence-Workflow.pdf](SmartPresence-Workflow.pdf) | Le fonctionnement de l'application, du doigt pose sur le capteur au bulletin de l'etudiant |
| [../SmartPresenceEsp32/docs/Guide-branchement-lecteur-ESP32.pdf](../SmartPresenceEsp32/docs/Guide-branchement-lecteur-ESP32.pdf) | Le cablage du lecteur, broche par broche |

## Regenerer le document de workflow

```bash
python docs/workflow_pdf.py docs/SmartPresence-Workflow.pdf logo-enetp.png
```

Le generateur est conserve a cote du PDF pour une raison precise : **le document
decrit des comportements releves dans le code**, et non des intentions. La tolerance
de rattachement vient de `PresenceServiceImpl.AVANCE_TOLEREE`, le seuil de retard de
`ParametreEtablissement.seuilRetardMinutes`, les etats des circuits de decision des
enumerations correspondantes. Si l'un de ces mecanismes change, le document devient
faux et doit etre regenere apres correction de la source.

La derniere section liste trois limites connues. Les corriger dans le code suppose
de les retirer d'ici.
