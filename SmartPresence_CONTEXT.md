# SmartPresence — Document de Contexte Technique Officiel

> **Document de référence permanent pour le développement du projet SmartPresence.**
> Ce document accompagne l'intégralité du cycle de développement et constitue la source de vérité technique, architecturale et fonctionnelle du projet. Il doit être lu et respecté par tout développeur, contributeur ou assistant IA intervenant sur le projet.

> **Version du modèle : v2 (officielle)** — intègre RBAC multi-rôles, `biometricId`, `Presence.source`, `StatutPresence.JUSTIFIE`, suivi IoT du `Device`, métriques de synchronisation.

---

## Table des matières

1. [Présentation du projet](#1-présentation-du-projet)
2. [Contexte](#2-contexte)
3. [Architecture générale](#3-architecture-générale)
4. [Objectifs du backend](#4-objectifs-du-backend)
5. [Technologies](#5-technologies)
6. [Architecture logicielle](#6-architecture-logicielle)
7. [Architecture de sécurité](#7-architecture-de-sécurité)
8. [Communication ESP32](#8-communication-esp32)
9. [Base de données](#9-base-de-données)
10. [Convention de développement](#10-convention-de-développement)
11. [Conventions REST](#11-conventions-rest)
12. [Évolutions futures](#12-évolutions-futures)
13. [Contraintes du projet](#13-contraintes-du-projet)
14. [Règles pour l'IA](#14-règles-pour-lia)

---

## 1. Présentation du projet

### 1.1 Présentation générale de SmartPresence

**SmartPresence** est un système biométrique embarqué de gestion automatisée de présence universitaire. Il combine un dispositif matériel autonome (microcontrôleur ESP32 couplé à un capteur d'empreintes digitales AS608), un backend applicatif robuste (Java / Spring Boot) et une application mobile de consultation et d'administration (Flutter).

Le système a pour vocation de remplacer les méthodes traditionnelles et manuelles de relevé de présence — feuilles de signature, pointage par carte RFID, scan de QR Code — par une identification biométrique fiable, infalsifiable et décentralisée. Chaque salle équipée d'un appareil SmartPresence identifie localement l'étudiant par son empreinte digitale, horodate l'événement, puis synchronise le relevé avec le serveur central de manière sécurisée.

SmartPresence se distingue par une architecture volontairement **privacy‑by‑design** : les données biométriques ne quittent jamais le dispositif embarqué. Seul l'identifiant logique de l'étudiant est transmis au backend, garantissant ainsi la conformité avec les principes de minimisation des données et de protection de la vie privée.

### 1.2 Cadre académique

Ce projet est réalisé dans le cadre d'un **mémoire de Master**. Il fait l'objet d'une soutenance devant un jury académique et professionnel.

**Titre du mémoire :**

> *Conception, modélisation et évaluation d'un système biométrique embarqué pour la gestion automatisée de présence universitaire : étude des performances, de la fiabilité et de la consommation énergétique.*

Ce mémoire ne se limite pas à la production d'un logiciel fonctionnel : il intègre une véritable démarche scientifique incluant la modélisation du système, l'évaluation expérimentale de ses performances, l'analyse de sa fiabilité et la mesure de sa consommation énergétique. Le livrable logiciel est donc indissociable d'un travail de recherche appliquée.

### 1.3 Objectifs scientifiques

Du point de vue de la recherche, le projet vise à :

- **Modéliser formellement** un système biométrique embarqué distribué, en identifiant ses composants, ses flux de données et ses points de défaillance potentiels.
- **Évaluer les performances** du dispositif embarqué, notamment le temps d'identification d'une empreinte, le temps de réponse de la chaîne complète (capteur → backend → application) et la capacité de traitement en situation de charge (arrivée massive d'étudiants en début de cours).
- **Mesurer la fiabilité** du système : taux de fausse acceptation (FAR), taux de faux rejet (FRR), robustesse face aux conditions réelles d'utilisation (doigts humides, usure, luminosité, positionnement du doigt).
- **Quantifier la consommation énergétique** de l'architecture embarquée, élément déterminant pour la viabilité d'un déploiement à grande échelle et pour l'autonomie des dispositifs en cas de coupure réseau ou électrique.
- **Comparer** la solution biométrique embarquée aux approches existantes (RFID, QR Code, feuilles manuelles) sur des critères objectifs de fiabilité, de coût, de sécurité et d'expérience utilisateur.
- **Valider expérimentalement** la cohérence de l'architecture distribuée, en particulier le mode hors ligne et la synchronisation différée, qui sont des facteurs clés de robustesse opérationnelle.

### 1.4 Objectifs techniques

Du point de vue de l'ingénierie logicielle, le projet poursuit les objectifs suivants :

- Concevoir un **backend professionnel** de qualité industrielle, respectant les standards d'une application d'entreprise (architecture en couches, sécurité JWT, API REST documentée, gestion d'erreurs centralisée).
- Réaliser un **firmware embarqué** stable pour l'ESP32, capable d'identification biométrique locale, de stockage non volatile et de communication réseau résiliente.
- Développer une **application mobile Flutter** offrant un accès sécurisé aux données de présence, aux statistiques et aux fonctions d'administration.
- Garantir la **sécurité de bout en bout** : authentification JWT pour les humains, clé d'API dédiée pour les appareils ESP32, chiffrement HTTPS, hachage BCrypt des mots de passe.
- Assurer la **traçabilité** complète du système via des logs structurés et un historique de synchronisation.
- Fournir une **documentation exhaustive** et une architecture suffisamment modulaire pour absorber les évolutions futures (export PDF/Excel, notifications, dashboard, conteneurisation, CI/CD).

### 1.5 Bénéfices attendus

Pour l'institution universitaire :

- **Fiabilité** : suppression des fraudes (émargement par un tiers, partage de carte, capture de QR Code) grâce à l'identification biométrique.
- **Gain de temps** : automatisation totale du relevé de présence, libérant l'enseignant de toute tâche administrative en début de cours.
- **Exactitude** : suppression des erreurs de saisie, des oublis et des doublons.
- **Données exploitables** : production immédiate de statistiques fiables (taux d'assiduité, absentéisme par classe, par promotion, par période).
- **Confidentialité** : aucun stockage centralisé des données biométriques, respect des principes de protection des données personnelles.
- **Robustesse opérationnelle** : fonctionnement assuré même en cas de panne réseau grâce au mode hors ligne et à la synchronisation différée.

Pour la communauté académique :

- Un **cas d'étude reproductible** d'architecture IoT/Edge biométrique, documenté et évaluable.
- Une **base de comparaison** entre approches d'identification en environnement universitaire.

---

## 2. Contexte

### 2.1 La problématique de la gestion des présences en université

La gestion des présences constitue une tâche administrative centrale et quotidienne dans tout établissement d'enseignement supérieur. Elle conditionne non seulement l'évaluation de l'assiduité — souvent exigée comme critère de validation d'un module — mais aussi la certification réglementaire de la scolarité. Malgré son importance, cette gestion demeure, dans la majorité des établissements, manuelle, fastidieuse et sujette à de nombreuses défaillances.

Les enseignants consacrent un temps non négligeable de chaque cours à l'appel, au détriment du temps pédagogique. Les données produites sont souvent incomplètes, retardées, voire inutilisables pour produire des statistiques fiables. Enfin, les méthodes employées laissent une large place à la fraude, ce qui compromet la valeur même de l'information de présence.

### 2.2 Limites des feuilles de présence

La feuille de signature papier, bien que largement répandue, présente des limites structurelles majeures :

- **Fraude massive** : la signature peut être réalisée par un camarade à la place de l'étudiant absent (pratique couramment appelée « signe pour moi »). Aucun contrôle d'identité n'est possible.
- **Perte et dégradation** : les feuilles peuvent être égarées, oubliées, mouillées ou détériorées, entraînant une perte définitive des données.
- **Saisie manuelle ultérieure** : le relevé doit être retranscrit manuellement dans un système informatique, ce qui génère erreurs de saisie, doublons et perte de temps.
- **Absence d'horodatage fiable** : l'heure exacte de signature n'est pas contrôlable ni vérifiable.
- **Impossibilité d'analyse en temps réel** : aucune donnée n'est disponible instantanément pour détecter un absentéisme problématique.
- **Coût caché** : impression, archivage, gestion physique des documents, espace de stockage.

### 2.3 Limites des cartes RFID

Les systèmes d'identification par carte RFID (Radio‑Frequency Identification) apportent une automatisation, mais souffrent de limites sérieuses :

- **Transfert de carte** : rien n'empêche un étudiant de remettre sa carte à un camarade qui badgera pour lui. L'identité réelle du porteur n'est jamais vérifiée.
- **Perte, vol et duplication** : une carte perdue ou volée reste fonctionnelle jusqu'à sa révocation. Certaines cartes sont d'ailleurs clonables.
- **Coût récurrent** : émission initiale, remplacement en cas de perte, infrastructure de lecteurs.
- **Dépendance matérielle** : oubli de la carte équivaut à une absence injustifiée.
- **Aucune biométrie** : le système authentifie un **objet**, non une **personne**.

### 2.4 Limites des QR Codes

Le scan de QR Code, solution apparemment moderne et économique, présente lui aussi des faiblesses déterminantes :

- **Capture et rejeu** : un QR Code peut être photographié et scanné à distance ou à un autre moment par un tiers.
- **Absence de présence physique** : rien ne prouve que l'étudiant se trouve réellement dans la salle au moment du scan.
- **Partage trivial** : une capture d'écran du QR Code peut être diffusée instantanément à un groupe entiter.
- **Dépendance au smartphone** : suppose que chaque étudiant dispose d'un téléphone chargé et fonctionnel.
- **Aucune authentification forte** : le QR Code est un identifiant visuel, pas une preuve d'identité.

### 2.5 Pourquoi la biométrie constitue une solution plus fiable

Face à ces limites, la **biométrie** — et en particulier la reconnaissance d'empreintes digitales — apporte des réponses décisive :

- **Lien intrinsèque à l'individu** : l'empreinte digitale est une caractéristique physiologique propre à chaque personne, impossible à transférer, à prêter ou à contrefaire de façon simple.
- **Présence physique requise** : la vérification biométrique impose la présence effective de l'étudiant devant le capteur, ce qui élimine les fraudes à distance.
- **Fiabilité élevée** : les capteurs modernes de type AS608 offerent un taux de fausse acceptation (FAR) très faible et un taux de faux rejet (FRR) acceptable, validés dans le cadre de l'évaluation expérimentale du projet.
- **Aucun élément à porter ou à mémoriser** : l'étudiant n'a besoin ni de carte, ni de téléphone, ni de code. La simplicité d'usage est maximale.
- **Confidentialité préservée** : dans l'architecture SmartPresence, les empreintes sont stockées **uniquement** dans le capteur embarqué et ne transitent jamais sur le réseau ni dans la base centrale.
- **Robustesse opérationnelle** : l'identification est réalisée localement sur l'ESP32, ce qui supprime la dépendance à une connexion permanente pour autoriser un pointage.

La biométrie embarquée représente donc, dans le contexte universitaire, le meilleur compromis entre **fiabilité**, **sécurité**, **simplicité d'usage** et **respect de la vie privée**.

---

## 3. Architecture générale

SmartPresence repose sur une architecture distribuée à **trois parties** clairement séparées, communiquant exclusivement via des canaux sécurisés. Cette séparation des responsabilités est un principe structurant du projet.

### 3.1 Vue d'ensemble

```
┌─────────────────────┐        ┌─────────────────────┐        ┌─────────────────────┐
│   SYSTÈME EMBARQUÉ  │  HTTPS │       BACKEND       │  HTTPS │    APPLICATION      │
│      (Edge/IoT)     │ ─────► │   Spring Boot (Java)│ ◄────► │      FLUTTER        │
│  Identification     │        │   API REST sécurisée│        │  Admin / Stats /    │
│  biométrique locale │        │   Base MySQL        │        │  Consultation       │
└─────────────────────┘        └─────────────────────┘        └─────────────────────┘
```

### 3.2 Partie 1 — Système embarqué

Le dispositif matériel est le point d'entrée physique du système. Il est responsable de l'identification biométrique, de l'horodatage et de la transmission sécurisée des événements de présence.

| Composant | Rôle |
|---|---|
| **ESP32‑WROOM‑32** | Microcontrôleur principal. Pilote l'ensemble du dispositif, exécute la logique d'identification, gère le stockage local, la connectivité réseau et la synchronisation avec le backend. |
| **AS608** | Capteur d'empreintes digitales optique. Réalise l'acquisition, la comparaison et l'identification biométrique. Stocke les modèles d'empreintes dans sa propre mémoire interne. |
| **RTC DS3231** | Horloge temps réel de précision. Garantit un horodatage fiable et indépendant du réseau, même en cas de coupure du Wi‑Fi. |
| **OLED** | Écran d'affichage compact. Fournit un retour visuel immédiat à l'utilisateur (identification réussie/échec, nom de l'étudiant, heure, messages d'erreur). |
| **Buzzer** | Signal sonore. Confirme le succès ou l'échec d'une opération, améliorant l'ergonomie en environnement bruyant. |
| **LED** | Indicateur lumineux. État du système, statut réseau, retour d'identification (succès/échec). |
| **PlatformIO** | Environnement de développement embarqué. Compilation, gestion des dépendances, flashage et débogage du firmware ESP32. |

### 3.3 Partie 2 — Backend

Le backend constitue le cœur applicatif et de persistance du système. Il s'agit d'une application d'entreprise Java/Spring Boot.

| Composant | Rôle |
|---|---|
| **Java 25** | Langage de programmation principal du backend, version LTS moderne offrant les dernières fonctionnalités de sécurité et de performance. |
| **Spring Boot** | Framework applicatif. Inversion de contrôle, injection de dépendances, autoconfiguration, serveur applicatif embarqué. |
| **MySQL** | Système de gestion de base de données relationnelle. Persistance durable, transactionnelle et performante des données métier. |
| **API REST** | Interface de communication normalisée. Exposée aux appareils ESP32 et à l'application Flutter. |
| **JWT** | Mécanisme d'authentification stateless pour les utilisateurs humains (administrateurs, enseignants). |
| **Swagger** | Documentation interactive et automatique de l'API REST. Référence consommable par tout client. |

### 3.4 Partie 3 — Application Flutter

L'application mobile constitue l'interface utilisateur distante du système. Elle ne communique **jamais** directement avec l'ESP32, mais uniquement avec le backend.

| Fonction | Description |
|---|---|
| **Administration** | Gestion des utilisateurs, étudiants, enseignants, classes, promotions, salles et appareils ESP32. |
| **Consultation** | Accès en temps réel aux relevés de présence, par classe, par étudiant, par promotion, par période. |
| **Statistiques** | Tableaux de bord et indicateurs : taux d'assiduité, absentéisme, tendances, comparaisons. |

### 3.5 Flux complet de fonctionnement

```
   Empreinte digitale
          │
          ▼
   Capteur AS608   ──── acquisition + identification locale
          │
          ▼
   ESP32‑WROOM‑32  ──── traitement, horodatage (RTC DS3231)
          │
          ▼
   Stockage NVS    ──── persistance locale (mode hors ligne)
          │
          ▼
 Synchronisation REST ── transmission sécurisée vers le backend
          │
          ▼
 Backend Spring Boot ── validation, persistance, traçabilité
          │
          ▼
   Base MySQL     ──── stockage définitif
          │
          ▼
   Application Flutter ── consultation, statistiques, administration
```

### 3.6 Principe fondamental de protection des données biométriques

**Le backend ne reçoit jamais les empreintes digitales.**

L'identification biométrique est **intégralement** réalisée sur le capteur AS608, au sein du dispositif embarqué. À l'issue de cette identification, l'ESP32 ne transmet au backend qu'un jeu de **données métier minimales**, strictement non biométriques :

| Champ | Description |
|---|---|
| `studentId` | Identifiant logique de l'étudiant identifié (issu du référentiel central). |
| `deviceId` | Identifiant unique de l'appareil ESP32 émetteur. |
| `date` | Date du relevé de présence. |
| `heure` | Heure précise du relevé (fournie par la RTC DS3231). |
| `statut` | Statut de présence (`PRESENT`, `RETARD`, `ABSENT`, `JUSTIFIE`). |
| `createdAt` | Horodatage de création de l'événement côté ESP32. |
| `synchronizedAt` | Horodatage de la synchronisation réussie avec le backend. |

Ce principe de **minimisation** garantit que la base centrale ne contient à aucun moment la moindre donnée biométrique.

---

## 4. Objectifs du backend

Le backend assure l'ensemble des responsabilités métier et techniques du système côté serveur. Ses objectifs se déclinent en domaines fonctionnels précis.

### 4.1 Gestion des utilisateurs

Administration complète du cycle de vie des comptes utilisateurs du système (administrateurs, enseignants, responsables de scolarité, superviseurs, tout rôle habilité) : création, consultation, modification, désactivation, réinitialisation des accès. Chaque utilisateur dispose d'identifiants sécurisés et d'un **ensemble de rôles** déterminant ses privilèges (voir §7 et §9.2).

### 4.2 Gestion des étudiants

Référentiel central des étudiants : informations administratives, rattachement à une classe et à une promotion, association à un identifiant logique utilisé lors de l'identification biométrique côté ESP32. Le backend stocke uniquement un **identifiant de correspondance logique** (`biometricId`) reliant un étudiant à son empreinte, **sans jamais stocker l'empreinte elle‑même**.

### 4.3 Gestion des enseignants

Référentiel des enseignants, leur rattachement aux classes et aux cours qu'ils dispensent, permettant le suivi et la consultation ciblée des présences.

### 4.4 Gestion des classes

Définition et administration des classes (groupes d'étudiants), leur composition et leur rattachement hiérarchique aux promotions.

### 4.5 Gestion des promotions

Gestion des promotions universitaires (années, filières, niveaux), permettant l'organisation structurée des étudiants et des classes et le calcul d'indicateurs agrégés.

### 4.6 Gestion des salles

Inventaire des salles physiques équipées d'un appareil SmartPresence. Chaque salle est associée à un dispositif ESP32 identifiable.

### 4.7 Gestion des appareils ESP32

Inventaire et contrôle des dispositifs embarqués déployés : identifiants uniques, clés d'API, **adresses MAC**, état, salle d'installation, activation/désactivation, suivi de la **dernière synchronisation**. Cette gestion est essentielle au contrôle d'accès machine au backend.

### 4.8 Gestion des présences

Cœur fonctionnel du système : réception, validation, persistance et restitution des événements de présence en provenance des appareils ESP32 **ou** saisis manuellement par un administrateur (champ `source`). Aucune empreinte n'est jamais manipulée à ce niveau.

### 4.9 Gestion des synchronisations

Suivi détaillé des opérations de synchronisation entre les appareils ESP32 et le backend : journalisation, traçabilité, détection des doublons, reprise des échecs, **nombre de tentatives** (évaluation de la fiabilité réseau). Ce module garantit l'intégrité et l'auditabilité du flux de données distribué.

### 4.10 Authentification

Mécanisme d'authentification des utilisateurs humains via **JWT** (JSON Web Token) : émission, validation, expiration, rafraîchissement. Authentification des appareils ESP32 via **clé d'API dédiée** (jamais via JWT — l'ESP32 n'a pas de compte utilisateur).

### 4.11 Autorisation (RBAC multi‑rôles)

Gestion fine des droits et rôles selon le modèle **RBAC (Role‑Based Access Control)**. Chaque utilisateur possède un **ensemble de rôles** (`Set<Role>`), ce qui permet à une même personne de cumuler plusieurs responsabilités (par exemple un enseignant qui est également superviseur de département). Chaque endpoint est protégé selon le principe du moindre privilège : les autorisations requises sont dérivées de l'agrégation des rôles de l'utilisateur.

### 4.12 Statistiques

Production d'indicateurs agrégés exploitables par l'application Flutter : taux de présence, absentéisme par classe, promotion ou période, tendances temporelles, comparatifs.

### 4.13 API REST

Exposition d'une API REST complète, cohérente, versionnée et documentée (Swagger), constituant le contrat d'intégration unique entre les appareils ESP32, l'application Flutter et tout futur client.

---

## 5. Technologies

La pile technologique de SmartPresence est choisie pour sa maturité, sa fiabilité et son adéquation aux standards professionnels d'une application d'entreprise.

| Technologie | Rôle dans le projet |
|---|---|
| **Java 25** | Langage principal du backend. Version LTS moderne, sûre et performante. |
| **Spring Boot** | Framework applicatif. Autoconfiguration, injection de dépendances, serveur embarqué, productivité. |
| **Spring Security** | Sécurité applicative. Authentification, autorisation, gestion des filtres, protection des endpoints. |
| **JWT** | Jetons d'authentification stateless pour les utilisateurs humains. |
| **MySQL** | Base de données relationnelle. Persistance transactionnelle, intégrité référentielle. |
| **Hibernate** | ORM. Mapping objet‑relationnel, abstraction de la couche de persistance. |
| **JPA** | Spécification de gestion des données. API standard de manipulation des entités. |
| **Swagger** | Documentation interactive de l'API REST (OpenAPI). Référence vivante du contrat d'intéface. |
| **Maven** | Outil de build et de gestion du cycle de vie du projet backend. |
| **Lombok** | Réduction du code boilerplate (getters, setters, constructeurs, logs) via annotations. |
| **MapStruct** | Génération automatique de mappers entre entités et DTO, au moment de la compilation. |
| **Validation** | Validation déclarative des données entrantes (Bean Validation). |
| **Docker** | Conteneurisation. Reproductibilité des environnements, déploiement maîtrisé. |
| **Flutter** | Framework cross‑platform de l'application mobile (Android/iOS). |
| **PlatformIO** | Environnement de développement embarqué pour le firmware ESP32. |
| **ESP32** | Microcontrôleur principal du dispositif embarqué. |
| **AS608** | Capteur d'empreintes digitales optique. |

---

## 6. Architecture logicielle

### 6.1 Architecture en couches

Le backend adopte une **architecture en couches** stricte, héritée des bonnes pratiques de l'ingénierie logicielle d'entreprise. Chaque couche a une responsabilité unique et ne communique qu'avec ses couches adjacentes immédiates, ce qui garantit séparation des préoccupations, testabilité et maintenabilité.

```
┌──────────────────────────────────────────────────────┐
│  COUCHE PRÉSENTATION        controller                │
│  Reçoit les requêtes HTTP, valide, retourne les DTO  │
└──────────────────────────────┬───────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────┐
│  COUCHE MÉTIER              service / service.impl    │
│  Contient toute la logique fonctionnelle             │
└──────────────────────────────┬───────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────┐
│  COUCHE ACCÈS AUX DONNÉES   repository               │
│  Persistance, requêtes JPA                            │
└──────────────────────────────┬───────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────┐
│  COUCHE PERSISTANCE         MySQL                    │
│  Base de données relationnelle                        │
└──────────────────────────────────────────────────────┘
```

Les couches transversales (`security`, `exception`, `config`, `validation`, `utils`, `constants`, `documentation`) traversent l'ensemble de l'application et fournissent des services partagés.

### 6.2 Arborescence logique du backend

L'organisation des packages reflète fidèlement la séparation des responsabilités :

```
com.smartpresence
├── config             Configuration de l'application et des composants
├── controller         Points d'entrée HTTP (API REST)
├── package-info
├── service            Interfaces de la couche métier
├── service.impl       Implémentations de la couche métier
├── repository         Accès aux données (Spring Data JPA)
├── entity             Modèle objet persistant (entités JPA)
├── dto                Objets de transfert (entrée/sortie de l'API)
├── mapper             Conversion entité ↔ DTO (MapStruct)
├── security           Authentification, autorisation, JWT, API KEY
├── exception          Gestion centralisée des erreurs
├── deserialization
├── documentation      Configuration de la documentation (Swagger/OpenAPI)
├── validation         Règles de validation métier personnalisées
├── utils              Utilitaires transverses
├── constants          Constantes et énumérations du projet
```

### 6.3 Rôle de chaque package

- **`config`** — Centralise la configuration des composants techniques : sources de données, paramètres de sécurité, beans applicatifs, configuration CORS, paramètres JWT, etc. Toute paramétrage technique trouve sa place ici plutôt que dans le code métier.
- **`controller`** — Couche de présentation. Expose les endpoints REST, reçoit les requêtes, délègue à la couche service et retourne des DTO. **Aucune logique métier ne doit s'y trouver.** Son rôle se limite à l'orchestration HTTP.
- **`service`** — Interfaces définissant le contrat de la couche métier. Permettent le découplage entre la couche présentation et l'implémentation, et facilitent les tests unitaires.
- **`service.impl`** — Implémentations concrètes des services. C'est ici que réside **toute la logique fonctionnelle** du système : règles de gestion, orchestrations, calculs, validations métier complexes.
- **`repository`** — Couche d'accès aux données. Repositories Spring Data JPA abstrayant la persistance et offrant des méthodes de requêtage déclaratives.
- **`entity`** — Modèle objet persistant, mappé aux tables de la base MySQL. Ces classes représentent le schéma relationnel et ne doivent **jamais** être exposées directement à l'extérieur de l'API.
- **`dto`** — Data Transfer Objects. Représentation contractuelle des données échangées avec les clients (ESP32, Flutter). Découplés des entités, ils permettent de n'exposer que les champs pertinents.
- **`mapper`** — Composants de conversion entre entités et DTO. Gérés par MapStruct pour garantir performance et fiabilité, en évitant tout code de conversion manuel.
- **`security`** — Mécanismes d'authentification et d'autisisation : filtres JWT, gestion des clés d'API ESP32, configuration de la chaîne de sécurité Spring Security, gestion des rôles.
- **`exception`** — Gestion centralisée et uniformisée des erreurs. Capture des exceptions métier et techniques, transformation en réponses HTTP cohérentes.
- **`validation`** — Règles de validation personnalisées, au‑delà des annotations standard de Bean Validation, pour exprimer des contraintes métier spécifiques (par exemple : `device` obligatoire si `source = ESP32`, optionnel si `source = MANUEL`).
- **`utils`** — Utilitaires transverses réutilisables (formatage, conversions techniques, helpers). Ne doit jamais contenir de logique métier.
- **`constants`** — Constantes, énumérations et paramètres globaux du projet. Centralisation évitant la dispersion des valeurs magiques dans le code.
- **`documentation`** — Configuration de la documentation de l'API (Swagger/OpenAPI) : descriptions, regroupements, exemples, métadonnées.

---

## 7. Architecture de sécurité

La sécurité est un axe transversal et non négociable du projet. Elle s'applique à chaque couche et chaque communication.

### 7.1 Authentification par JWT

Les **utilisateurs humains** (administrateurs, enseignants, responsables de scolarité, superviseurs) s'authentifient via **JWT (JSON Web Token)**. Le jeton, signé et à expiration contrôlée, est transmis à chaque requête. Ce mécanisme **stateless** s'intègre naturellement à l'architecture REST et facilite la scalabilité horizontale.

### 7.2 Authentification des appareils ESP32 par clé d'API

Les **appareils ESP32** ne s'authentifient pas par JWT (ils n'ont pas de compte humain). Ils utilisent une **clé d'API dédiée** (`API KEY ESP32`), propre à chaque appareil, validée par le backend. Ce mécanisme permet d'identifier, de tracer et de révoquer individuellement chaque dispositif. Toute requête provenant d'un appareil non enregistré ou désactivé est rejetée. La clé est stockée sous forme **hachée** dans la base.

### 7.3 Chiffrement HTTPS

**Toutes les communications** — entre ESP32 et backend, entre Flutter et backend — transitent en **HTTPS** (TLS). Aucune donnée ne circule en clair sur le réseau. Cela protège l'intégrité et la confidentialité des événements de présence et des identifiants.

### 7.4 Hachage des mots de passe avec BCrypt

Les mots de passe des utilisateurs ne sont **jamais** stockés en clair. Ils sont hachés au moyen de **BCrypt**, algorithme de hachage adaptatif et salé, résistant aux attaques par dictionnaire et par table arc‑en‑ciel. Le sel est unique à chaque mot de passe.

### 7.5 Validation systématique des entrées

Toute donnée entrante — payload JSON, paramètres de requête, paramètres de path — fait l'objet d'une **validation déclarative** (Bean Validation) et de **validations métier** spécifiques (couche service) lorsque les contraintes ne peuvent être exprimées par JPA seul.

### 7.6 Gestion globale des exceptions

Aucune erreur n'échappe au système. Un mécanisme de **gestion globale des exceptions** intercepte les erreurs métier comme techniques et les transforme en réponses HTTP cohérentes et informatives, sans jamais divulguer d'information sensible sur l'implémentation (pas de trace de pile exposée en production).

### 7.7 Journalisation (logs)

Le système produit des **logs structurés** à chaque niveau : authentifications, opérations sensibles, échecs, synchronisations ESP32, erreurs. Ces journaux assurent l'**auditabilité** et le **diagnostic** du système en exploitation, et contribuent à la traçabilité exigée d'un système d'entreprise.

---

## 8. Communication ESP32

La communication entre le dispositif embarqué et le backend est conçue pour être **résiliente**, **sécurisée** et **autonome** face aux aléas du réseau.

### 8.1 Identification locale

L'identification biométrique est **entièrement réalisée localement** sur le capteur AS608, piloté par l'ESP32. Aucune dépendance réseau n'est requise pour autoriser un pointage : la latence est minimale et la disponibilité maximale. Le backend n'intervient jamais dans la décision d'identification.

### 8. du role de correspondance biométrique

Le capteur AS608 stocke les empreintes dans des **slots** internes. L'ESP32 conserve en NVS une table de correspondance `{slot AS608 → studentId (UUID)}`. À l'identification, l'ESP32 lit le slot reconnu, récupère l'UUID correspondant et l'envoie comme `studentId`. Le backend n'a jamais connaissance des slots ni des empreintes ; il ne stocke qu'un **identifiant de correspondance logique** (`biometricId`) sur l'entité `Etudiant`.

### 8.3 Stockage NVS

Les événements de présence générés sont persistés dans la mémoire **NVS (Non‑Volatile Storage)** de l'ESP32 avant toute transmission. Cette persistance garantit qu'aucun événement n'est perdu, même en cas de coupure électrique ou de redémarrage du dispositif.

### 8.4 Synchronisation différée

La transmission au backend n'est pas bloquante : elle se fait en **synchronisation différée**. L'ESP32 tente d'envoyer les événements accumulés dès que la connexion est disponible, sans impacter l'expérience utilisateur locale. Ce mécanisme découple l'identification temps réel de la persistance centrale.

### 8.5 Gestion du mode hors ligne

En l'absence de réseau, l'appareil entre en **mode hors ligne** : il continue d'identifier les étudiants et d'enregistrer localement les présences. La file d'attente persistée dans la NVS est vidée progressivement dès le retour de la connectivité.

### 8.6 Gestion des erreurs & Reprise automatique

Chaque étape de communication est protégée : timeouts, erreurs HTTP, pertes de connexion, réponses inattendues. Un mécanisme de **reprise automatique** garantit qu'un événement non acquitté par le backend est retransmis. La détection des doublons côté backend (via `deviceId`, `studentId`, horodatage) assure l'**idempotence** du processus : un même événement transmis plusieurs fois n'est enregistré qu'une seule fois.

---

## 9. Base de données

La base de données relationnelle MySQL matérialise le modèle persistant du système. Elle est organisée autour d'entités métier clairement définies et reliées entre elles.

### 9.1 Principales entités

| Entité | Rôle |
|---|---|
| **Utilisateur** | Compte d'accès au système. Identifiants, mot de passe haché, **ensemble de rôles** (`Set<Role>`). |
| **Role** | Rôle applicatif d'un utilisateur (RBAC). Code unique, libellé, description. |
| **Etudiant** | Étudiant du référentiel. Identifiant logique, informations administratives, **`biometricId`** (référence de correspondance logique, **aucune empreinte stockée**), rattachements. |
| **Enseignant** | Enseignant du référentiel. Rattaché 1‑1 à un compte `Utilisateur`, intervient dans plusieurs classes. |
| **Classe** | Groupe d'étudiants. Composition, rattachement à une promotion. |
| **Promotion** | Promotion universitaire (filière, année, niveau). Contient des classes. |
| **Sala** | Salle physique équipée. Associée à un appareil ESP32. |
| **Presence** | Événement de présence. Référence l'étudiant, le device (si ESP32), la date, l'heure, le statut, la source (`ESP32`/`MANUEL`). |
| **Device** | Appareil ESP32 déclaré et autorisé. Identifiant, clé d'API hachée, **adresse MAC**, état, salle, **dernière synchronisation**. |
| **HistoriqueSynchronisation** | Journal des opérations de synchronisation ESP32 → backend. Statut, nombre d'événements, **nombre de tentatives** (évaluation fiabilité). |

### 9.2 Relations entre entités (v2)

Le modèle relationnel **v2** introduit une évolution majeure de la gestion des rôles : la relation `Utilisateur ↔ Role` est désormais **N‑N** (au lieu de N‑1), afin de supporter un véritable RBAC multi‑rôles.

- **Utilisateur ↔ Role** : relation **N‑N** via la table `utilisateurs_roles`. Un utilisateur possède un **ensemble de rôles** (`Set<Role>`), ce qui permet de cumuler plusieurs responsabilités (par ex. `ENSEIGNANT` + `SUPERVISEUR`). _Cette évolution remplace la relation N‑1 historique et constitue la nouvelle référence officielle._
- **Utilisateur ↔ Enseignant** : relation **1‑1**. `Enseignant` référence `Utilisateur` (son compte de connexion JWT). Un administrateur est un `Utilisateur` sans profil `Enseignant`.
- **Promotion ↔ Classe** : une promotion contient plusieurs classes ; une classe appartient à une promotion (relation 1‑N).
- **Classe ↔ Etudiant** : une classe contient plusieurs étudiants ; un étudiant est rattaché à une classe (relation 1‑N).
- **Promotion ↔ Etudiant** : relation dérivée via la classe d'appartenance.
- **Enseignant ↔ Classe** : un enseignant intervient dans une ou plusieurs classes (relation N‑N via `classes_enseignants`).
- **Salle ↔ Device** : une salle est équipée d'un appareil ESP32 ; un appareil est installé dans une salle (relation 1‑1).
- **Device ↔ Presence** : un appareil produit plusieurs événements de présence (relation 1‑N). **Nullable** pour les présences de source `MANUEL`.
- **Etudiant ↔ Presence** : un étudiant est associé à plusieurs événements de présence au cours du temps (relation 1‑N).
- **Device ↔ HistoriqueSynchronisation** : un appareil génère plusieurs entrées d'historique de synchronisation (relation 1‑N).

### 9.3 Règle métier sur la relation Presence → Device

La relation `Presence → Device` obéit à une règle métier explicite, contrôlée par la couche service et la validation métier (et non par JPA seul) :

```
Si source = ESP32  →  device obligatoire
Si source = MANUEL →  device optionnel (peut être null)
```

> **Rappel de confidentialité** : aucune entité ne stocke d'empreinte digitale ou de modèle biométrique. Le champ `Etudiant.biometricId` est une **référence logique de correspondance** (un identifiant), jamais une empreinte.

---

## 10. Convention de développement

Les conventions ci‑dessous sont **obligatoires**. Elles s'imposent à tout contributeur, humain ou IA. Leur respect conditionne la qualité, la cohérence et l'évolutivité du projet.

- **Clean Architecture** — Séparation stricte des responsabilités et dépendance unidirectionnelle vers les couches internes.
- **SOLID** — Application rigoureuse des cinq principes.
- **Clean Code** — Code lisible, nommage explicite, fonctions courtes, absence de duplication, commentaires uniquement lorsque nécessaire.
- **Repository Pattern** — Tout accès aux données transite par la couche repository.
- **DTO Pattern** — Les échanges avec l'extérieur utilisent exclusivement des DTO, jamais d'entités.
- **Mapper Pattern** — Les conversions entité ↔ DTO sont confiées à MapStruct.
- **Validation Bean** — Toute donnée entrante est validée de façon déclarative avant traitement ; les contraintes conditionnelles sont assurées par la couche service.
- **Documentation Swagger** — Chaque endpoint est documenté.
- **Injection par constructeur** — Les dépendances sont injectées via constructeur, garantissant immuabilité, testabilité et explicité.
- **Aucune logique métier dans les Controllers** — Les controllers se limitent à l'orchestration HTTP.
- **Ne jamais exposer directement les Entity** — Les entités JPA sont internes ; seuls des DTO traversent la frontière de l'API.

---

## 11. Conventions REST

### 11.1 Format de réponse uniforme

**Toutes** les API du projet retournent une réponse au format uniforme suivant :

| Champ | Description |
|---|---|
| `success` | Booléen indiquant le succès (`true`) ou l'échec (`false`) de l'opération. |
| `message` | Message descriptif à destination du client (humain ou machine). |
| `data` | Charge utile métier (objet, collection, ou `null`). |
| `timestamp` | Horodatage de la réponse. |

### 11.2 Respect des codes HTTP

| Code | Signification | Usage |
|---|---|---|
| `200 OK` | Succès | Lecture, mise à jour réussie. |
| `201 Created` | Ressource créée | Création d'une entité. |
| `204 No Content` | Succès sans contenu | Suppression réussie. |
| `400 Bad Request` | Requête invalide | Validation échouée, payload malformé. |
| `401 Unauthorized` | Non authentifié | Jeton absent, invalide ou expiré. |
| `403 Forbidden` | Non autorisé | Droits insuffisants pour l'opération. |
| `404 Not Found` | Ressource introuvable | Identifiant inexistant. |
| `409 Conflict` | Conflit | Doublon, violation d'unicité. |
| `500 Internal Server Error` | Erreur serveur | Erreur inattendue. |

---

## 12. Évolutions futures

L'architecture est conçue dès l'origine pour absorber des évolutions majeures sans refonte structurelle :

- **Notifications** — Alerte automatique (absentéisme, synchronisation échouée, anomalie d'appareil).
- **Export PDF** — Génération de rapports et de relevés de présence imprimables.
- **Export Excel** — Export tabulaire des données pour traitement analytique externe.
- **Dashboard** — Tableau de bord avancé, indicateurs temps réel, graphiques et tendances.
- **Rapports** — Rapports périodiques automatisés.
- **Versionnement d'API** — Gestion explicite des versions (`/api/v1`, `/api/v2`).
- **Audit avancé** — Traçabilité fine de toute opération sensible via `createdBy` / `updatedBy` (champs déjà anticipés dans l'entité de base `BaseAuditableEntity`).
- **Historique** — Conservation et consultation de l'historique complet des modifications.
- **Docker Compose** — Orchestration multi‑conteneurs.
- **CI/CD** — Intégration et déploiement continus automatisés.
- **Cloud** — Déploiement sur infrastructure cloud, scalabilité horizontale.

---

## 13. Contraintes du projet

Les contraintes suivantes sont **impératives**. Elles définissent l'identité del projet et ne doivent jamais être transgressées.

- **Le backend ne stocke jamais les empreintes.** `Etudiant.biometricId` est une référence logique, jamais une empreinte.
- **Le capteur AS608 réalise toute l'identification.** La décision d'identification est prise localement.
- **Le backend ne reçoit qu'un identifiant.** L'ESP32 transmet uniquement l'identifiant logique de l'étudiant reconnu, accompagné des métadonnées de présence, jamais l'empreinte.
- **Flutter communique uniquement avec le backend.** L'application mobile n'a aucun accès direct aux appareils ESP32.
- **L'ESP32 communique uniquement avec le backend.** Le dispositif embarqué n'échange qu'avec le serveur central, via une API sécurisée.
- **Toutes les communications sont sécurisées.** HTTPS systématique, JWT pour les humains, clé d'API (hachée) pour les appareils ESP32, validation des entrées, hachage BCrypt.

---

## 14. Règles pour l'IA

Cette section est destinée aux **assistants IA** intervenant sur le projet SmartPresence.

- **Toujours analyser avant de coder.** Comprendre le besoin, le contexte et l'impact avant d'écrire la moindre ligne de code.
- **Toujours proposer une architecture avant d'écrire du code.**
- **Ne jamais générer plusieurs dizaines de fichiers simultanément.**
- **Construire le projet étape par étape.**
- **Respecter systématiquement l'architecture existante.**
- **Ne jamais casser la compatibilité avec les fichiers déjà créés.**
- **Toujours privilégier la qualité, la maintenabilité et l'évolutivité.**
- **Considérer que le projet sera présenté devant un jury de Master** et qu'il devra respecter les standards professionnels d'une application d'entreprise.

---

*Document de référence officiel du projet **SmartPresence** — version du modèle v2. Toute évolution de ce document doit être validée et tracée.*
