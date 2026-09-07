# BackendSmartPresence

> **Backend Spring Boot du projet SmartPresence.**
> Système biométrique embarqué de gestion automatisée de présence universitaire.
> _Référence : [`../SmartPresence_CONTEXT.md`](../SmartPresence_CONTEXT.md)_

---

## Stack technique

| Technologie | Version | Rôle |
|---|---|---|
| **Java** | 25 (LTS) | Langage |
| **Spring Boot** | 3.5.16 | Framework applicatif |
| **Spring Security** | (via Boot) | Sécurité, authentification/autorisation |
| **JWT (jjwt)** | 0.12.6 | Jetons d'authentification stateless |
| **Spring Data JPA** | (via Boot) | Accès aux données |
| **Hibernate** | (via Boot) | ORM |
| **MySQL** | 8.x | Base de données relationnelle |
| **Bean Validation** | (via Boot) | Validation déclarative |
| **Lombok** | (via Boot) | Réduction du boilerplate |
| **MapStruct** | 1.6.3 | Mappers entité ↔ DTO |
| **springdoc-openapi** | 2.8.17 | Documentation Swagger/OpenAPI |
| **Maven** | 3.9.9 (Wrapper) | Build |

---

## Prérequis

| Outil | Requis |
|---|---|
| **JDK 25** | ✅ Obligatoire (déjà configuré dans IntelliJ : `.jdks/ms-25.0.4`) |
| **Docker** | Optionnel (pour MySQL via `docker-compose`) |
| **MySQL 8** | Obligatoire en local, **ou** via Docker (recommandé) |

> ℹ️ **Maven n'a pas besoin d'être installé** : le **Maven Wrapper** (`./mvnw`) embarque
> la version 3.9.9 et la télécharge au premier lancement.

---

## 1. Démarrer la base de données (MySQL)

### Option A — Avec Docker (recommandé)

```bash
docker compose up -d mysql
```

MySQL écoute sur `localhost:3306`, base `smartpresence_db` créée automatiquement.

### Option B — MySQL local

Créer la base :

```sql
CREATE DATABASE smartpresence_db;
```

---

## 2. Lancer le backend

### Avec IntelliJ IDEA (recommandé)

1. Ouvrir le dossier `BackendSmartPresence/` comme projet.
2. Configurer le **JDK 25** (Project Structure → SDK).
3. Exécuter la classe `SmartPresenceApplication`.

### En ligne de commande (Git Bash)

```bash
export JAVA_HOME="C:/Users/YACOUBA SANOGO/.jdks/ms-25.0.4"
./mvnw spring-boot:run
```

### Tout en un avec Docker Compose (backend + MySQL)

```bash
docker compose up --build
```

---

## 3. Vérifier le démarrage

| URL | Description |
|---|---|
| `http://localhost:8080/api/swagger-ui.html` | Interface Swagger UI |
| `http://localhost:8080/api/v3/api-docs` | Spécification OpenAPI (JSON) |

---

## 4. Compiler / packager

```bash
# Compilation seule
./mvnw clean compile

# Packaging en JAR exécutable (sans tests)
./mvnw clean package -DskipTests

# Packaging avec tests (nécessite MySQL démarré)
./mvnw clean package
```

---

## Configuration

La configuration se fait via [`src/main/resources/application.yml`](src/main/resources/application.yml),
avec surcharge par variables d'environnement :

| Variable | Défaut | Description |
|---|---|---|
| `DB_HOST` | `localhost` | Hôte MySQL |
| `DB_PORT` | `3306` | Port MySQL |
| `DB_NAME` | `smartpresence_db` | Nom de la base |
| `DB_USERNAME` | `root` | Utilisateur MySQL |
| `DB_PASSWORD` | `root` | Mot de passe MySQL |
| `JWT_SECRET` | (clé de dev) | Secret de signature JWT |
| `JWT_EXPIRATION_MS` | `3600000` | Durée de validité du JWT (ms) |
| `DEVICE_API_KEY` | (clé de dev) | Clé d'API des appareils ESP32 |

> ⚠️ **Production** : toujours surcharger les secrets via variables d'environnement.

---

## Architecture des packages

```
com.smartpresence
├── config            Configuration technique (datasource, sécurité, beans)
├── controller        API REST (orchestration HTTP, aucune logique métier)
├── service           Interfaces de la couche métier
├── service.impl      Implémentations de la logique métier
├── repository        Accès aux données (Spring Data JPA)
├── entity            Entités JPA (jamais exposées directement)
├── dto               Objets de transfert (contrat API)
├── mapper            Conversions entité ↔ DTO (MapStruct)
├── security          Authentification JWT, clé d'API ESP32, rôles
├── exception         Gestion centralisée des erreurs
├── validation        Validations métier personnalisées
├── utils             Utilitaires transverses
├── constants         Constantes et énumérations
└── documentation     Configuration Swagger/OpenAPI
```

_Détail complet dans [`../SmartPresence_CONTEXT.md`](../SmartPresence_CONTEXT.md) §6._

---

## Statut

- [x] Initialisation du projet Spring Boot (Maven, Java 25)
- [x] Arborescence des 14 packages
- [x] Configuration MySQL + Swagger + Journalisation
- [x] Maven Wrapper
- [x] Docker + docker-compose
- [ ] Entités métier (étape 2)
- [ ] Sécurité JWT + clé d'API ESP32
- [ ] DTO, Mappers, Services, Controllers
- [ ] Endpoints REST
