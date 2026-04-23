# ⚕️ PharmaStock Pro

> **Système de gestion de stock de pharmacie avancé**  
> Application web full-stack Java Spring Boot + React — Projet PFA

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-3.4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Security-black?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

</div>

---

## 📋 Table des matières

- [À propos](#-à-propos)
- [Fonctionnalités](#-fonctionnalités)
- [Architecture](#-architecture)
- [Stack technologique](#-stack-technologique)
- [Structure du projet](#-structure-du-projet)
- [Installation](#-installation)
- [Comptes de démonstration](#-comptes-de-démonstration)
- [API REST](#-api-rest)
- [Captures d'écran](#-captures-décran)
- [Règles métier clés](#-règles-métier-clés)
- [Tests](#-tests)
- [Auteurs](#-auteurs)

---

## 🏥 À propos

**PharmaStock Pro** est une application web professionnelle de gestion de stock destinée aux pharmacies modernes. Elle résout les problèmes quotidiens des pharmacies :

| Problème sans système | Solution PharmaStock Pro |
|---|---|
| Ruptures de stock non détectées | ✅ Alertes automatiques dès le seuil atteint |
| Médicaments périmés vendus | ✅ Blocage automatique des lots expirés |
| Impossible de tracer un lot rappelé | ✅ Traçabilité complète lot → patient |
| Commandes fournisseurs manuelles | ✅ Bons de commande numériques avec suivi |
| Aucune visibilité sur les KPIs | ✅ Dashboard analytique temps réel |

---

## ✨ Fonctionnalités

### 🔄 Gestion du stock
- Catalogue complet des médicaments (DCI, dosage, forme galénique, code-barres)
- Traçabilité par lot (numéro de lot + date d'expiration obligatoires)
- **Règle FEFO automatique** — le lot le plus proche de l'expiration est toujours consommé en premier
- Barre de stock visuelle avec code couleur (vert / orange / rouge)
- Inventaire physique avec rapport d'écarts

### 🔔 Alertes intelligentes (7 types)
- `STOCK_FAIBLE` — stock sous le seuil minimal configuré
- `RUPTURE` — stock épuisé (quantité = 0)
- `PEREMPTION_30J` — lot expire dans moins de 30 jours
- `PEREMPTION_7J` — lot expire dans moins de 7 jours *(critique)*
- `LOT_EXPIRE` — lot périmé, blocage automatique de la vente
- `RAPPEL_LOT` — blocage manuel par le pharmacien (rappel fabricant)
- `ANOMALIE_RECEPTION` — écart entre commande et livraison

> Les alertes sont vérifiées en temps réel à chaque mouvement de stock **ET** chaque nuit à 01h00 via un job planifié Spring Scheduler.

### 🛒 Interface de caisse
- Recherche instantanée par nom, DCI ou scan code-barres
- Panier multi-articles avec remises par article
- Calcul automatique du rendu monnaie
- Gestion des ordonnances (blocage si médicament sous prescription sans ordonnance)
- Application FEFO transparente pour le caissier

### 📊 Rapports PDF
- Rapport de stock (état complet avec valeurs)
- Rapport des ventes (CA par période)
- Rapport des péremptions (codes couleur selon urgence)
- Rapport des mouvements de stock

### 🔐 Sécurité
- Authentification JWT (token 15 min + refresh 7 jours)
- Mots de passe BCrypt (coût 10)
- Verrouillage automatique après 5 tentatives échouées
- Contrôle d'accès par rôle sur chaque endpoint (`@PreAuthorize`)
- Journal d'audit immuable (traçabilité réglementaire)

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────┐
│        Frontend React + Tailwind CSS             │
│           http://localhost:3000                  │
└─────────────────────┬────────────────────────────┘
                      │ HTTP/JSON + JWT Token
┌─────────────────────▼────────────────────────────┐
│         API REST — Spring Boot 3.2               │
│              http://localhost:8080               │
│  ┌──────────┐ ┌──────────┐ ┌───────────────────┐│
│  │Controller│►│ Service  │►│    Repository JPA ││
│  └──────────┘ └──────────┘ └───────────────────┘│
└─────────────────────┬────────────────────────────┘
                      │ JPA / Hibernate
┌─────────────────────▼────────────────────────────┐
│            PostgreSQL 15                         │
│         pharmastock (14 tables)                  │
└──────────────────────────────────────────────────┘
```

---

## 🛠 Stack technologique

### Backend
| Technologie | Version | Usage |
|---|---|---|
| Java | 17 LTS | Langage principal |
| Spring Boot | 3.2.x | Framework web |
| Spring Data JPA / Hibernate | 6.x | ORM |
| Spring Security | 6.x | Authentification & autorisation |
| PostgreSQL | 15 | Base de données |
| Flyway | 9.x | Migrations BDD |
| jjwt | 0.12.5 | JSON Web Tokens |
| iText 7 | 7.2.5 | Génération PDF |
| Apache POI | 5.2.5 | Export Excel |
| Springdoc OpenAPI | 2.3 | Documentation Swagger |
| JUnit 5 + Mockito | — | Tests unitaires |
| Maven | 3.9+ | Build & dépendances |

### Frontend
| Technologie | Version | Usage |
|---|---|---|
| React | 18.2 | UI |
| React Router | 6.22 | Navigation |
| Tailwind CSS | 3.4 | Styles |
| @tanstack/react-query | 5.25 | Cache & fetching |
| Axios | 1.6 | Appels HTTP |
| React Hook Form | 7.51 | Formulaires |
| Recharts | 2.12 | Graphiques |
| react-hot-toast | 2.4 | Notifications |
| lucide-react | 0.344 | Icônes |
| date-fns | 3.3 | Manipulation de dates |

---

## 📁 Structure du projet

```
pharmastock/
├── pharmastock-backend/                    # API Spring Boot
│   ├── pom.xml
│   └── src/main/java/ma/pharmacie/pharmastock/
│       ├── PharmaStockApplication.java     # Point d'entrée
│       ├── config/
│       │   ├── SecurityConfig.java         # JWT + CORS + rôles
│       │   └── DataInitializer.java        # Init données au démarrage
│       ├── entity/                         # 13 entités JPA
│       │   ├── Medicament.java
│       │   ├── Lot.java                    # Traçabilité par lot
│       │   ├── Vente.java
│       │   ├── LigneVente.java
│       │   ├── AlerteStock.java
│       │   ├── MouvementStock.java
│       │   ├── CommandeFournisseur.java
│       │   ├── Ordonnance.java
│       │   ├── Utilisateur.java
│       │   ├── Categorie.java
│       │   ├── Fournisseur.java
│       │   ├── AuditLog.java
│       │   └── ...
│       ├── enums/                          # 10 enums métier
│       ├── repository/                     # Repositories Spring Data JPA
│       ├── service/impl/
│       │   ├── StockService.java           # ⭐ Logique FEFO
│       │   ├── VenteService.java           # Vente + audit
│       │   └── AuthService.java            # Login + JWT
│       ├── controller/                     # 11 controllers REST
│       ├── security/                       # JwtUtil + JwtAuthFilter
│       ├── scheduler/
│       │   └── AlerteScheduler.java        # Cron 01h00/nuit
│       └── exception/
│           └── GlobalExceptionHandler.java
│
├── pharmastock-frontend/                   # Interface React
│   ├── tailwind.config.js                  # Thème vert pharmacie
│   └── src/
│       ├── api/
│       │   ├── axiosClient.js              # Intercepteurs JWT
│       │   └── services.js                 # Tous les appels API
│       ├── context/AuthContext.jsx         # Auth globale
│       ├── components/layout/
│       │   ├── Sidebar.jsx                 # Navigation + rôles
│       │   └── Header.jsx                  # Alertes badge
│       └── pages/
│           ├── auth/       dashboard/      medicaments/
│           ├── stock/      ventes/         commandes/
│           ├── alertes/    rapports/       utilisateurs/
│           ├── inventaire/ fournisseurs/   ordonnances/
│           └── audit/
│
└── README.md
```

---

## 🚀 Installation

### Prérequis

- **Java 17+** — `java -version`
- **Maven 3.9+** — `mvn -version`
- **Node.js 18+** — `node -version`
- **PostgreSQL 15** — `psql --version`

### 1. Cloner le projet

```bash
git clone https://github.com/lakbita-khadija/PharmaStock.git
cd Pharmastock
```

### 2. Configurer la base de données

```bash
# Créer la base PostgreSQL
psql -U postgres -c "CREATE DATABASE pharmastock;"
```

Modifier `pharmastock-backend/src/main/resources/application.properties` si besoin :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pharmastock
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 3. Démarrer le backend

```bash
cd pharmastock-backend

# Compiler
mvn clean install -DskipTests

# Démarrer
mvn spring-boot:run
```

✅ Serveur prêt quand vous voyez :
```
Started PharmaStockApplication in X.XXX seconds
✅ 4 mot(s) de passe corrigé(s). Nouveau mot de passe : Admin123!
```

> **Swagger UI** : http://localhost:8080/swagger-ui.html

### 4. Démarrer le frontend

```bash
# Nouveau terminal
cd pharmastock-frontend

# Installer les dépendances (une seule fois)
npm install

# Configurer l'URL de l'API
cp .env.example .env.local
# .env.local contient : REACT_APP_API_URL=http://localhost:8080/api/v1

# Démarrer
npm start
```

✅ Application disponible sur **http://localhost:3000**

---

## 👥 Comptes de démonstration

| Rôle | Email | Mot de passe | Accès |
|---|---|---|---|
| 🔴 **Admin** | `admin@pharma.ma` | `Admin123!` | Complet |
| 🟢 **Pharmacien** | `pharma@pharma.ma` | `Admin123!` | Large |
| 🔵 **Caissier** | `caissier@pharma.ma` | `Admin123!` | Ventes uniquement |
| 🟠 **Gestionnaire stock** | `gestion@pharma.ma` | `Admin123!` | Stock & commandes |

---

## 🔌 API REST

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Connexion → JWT | Public |
| `GET` | `/api/v1/medicaments` | Catalogue paginé | Tous |
| `GET` | `/api/v1/medicaments/search?q=` | Recherche rapide (caisse) | Tous |
| `POST` | `/api/v1/medicaments` | Créer un médicament | PHARMACIEN+ |
| `GET` | `/api/v1/stock` | Vue stock par médicament | Tous |
| `GET` | `/api/v1/stock/{id}/lots` | Lots d'un médicament | Tous |
| `PUT` | `/api/v1/lots/{id}/bloquer` | Bloquer un lot | PHARMACIEN+ |
| `POST` | `/api/v1/ventes` | Créer une vente (FEFO auto) | CAISSIER+ |
| `DELETE` | `/api/v1/ventes/{id}` | Annuler une vente | PHARMACIEN+ |
| `POST` | `/api/v1/commandes` | Créer une commande fournisseur | PHARMACIEN, GESTIONNAIRE |
| `GET` | `/api/v1/alertes` | Liste des alertes actives | PHARMACIEN, GESTIONNAIRE |
| `PUT` | `/api/v1/alertes/{id}/acquitter` | Acquitter une alerte | PHARMACIEN+ |
| `GET` | `/api/v1/alertes/count` | Compteur (badge header) | Tous |
| `GET` | `/api/v1/dashboard/kpis` | KPIs temps réel | Tous |
| `GET` | `/api/v1/rapports/stock` | PDF rapport de stock | PHARMACIEN+ |
| `GET` | `/api/v1/rapports/ventes` | PDF rapport des ventes | PHARMACIEN+ |
| `GET` | `/api/v1/rapports/peremptions` | PDF rapport péremptions | PHARMACIEN+ |
| `GET` | `/api/v1/utilisateurs` | Liste des utilisateurs | ADMIN |
| `PUT` | `/api/v1/utilisateurs/{id}/deverrouiller` | Déverrouiller un compte | ADMIN |
| `GET` | `/api/v1/audit` | Journal d'audit | PHARMACIEN+ |

> Documentation interactive complète : **http://localhost:8080/swagger-ui.html**

---

## 📸 Captures d'écran

| Page | Description |
|---|---|
| Login | Thème vert pharmacie, comptes de démo |
| Dashboard | KPIs temps réel + graphique 30 jours |
| Médicaments | Catalogue paginé avec recherche |
| Stock & Lots | Vue FEFO avec expansion par lot |
| Caisse | Interface de vente avec scanner |
| Alertes | Centre de notifications avec acquittement |

---

## ⚙️ Règles métier clés

### 🔄 Règle FEFO (First Expired, First Out)

```
Lot A — expire le 15/01/2025 — 20 unités
Lot B — expire le 30/06/2025 — 50 unités

Vente de 25 unités → Le système utilise :
  ✅ 20 unités du Lot A (épuisé)
  ✅  5 unités du Lot B
```

Implémentée dans `StockService.sortieStockFefo()` — le caissier n'intervient pas.

### 🚨 Alertes automatiques

```
Chaque nuit à 01h00 (Spring Scheduler) :
  • Lots expirés        → statut EXPIRE + alerte BLOQUANT
  • Expiration dans 7j  → alerte CRITIQUE
  • Expiration dans 30j → alerte AVERTISSEMENT
  • Stock < seuil       → alerte STOCK_FAIBLE
  • Stock = 0           → alerte RUPTURE CRITIQUE
```

### 🔐 Sécurité

```
• JWT token : valide 15 minutes
• Refresh token : valide 7 jours
• BCrypt coût 10 pour les mots de passe
• Verrouillage après 5 tentatives échouées
• Journal d'audit immuable (lecture seule)
```

---

## 🧪 Tests

```bash
cd pharmastock-backend

# Lancer tous les tests
mvn test

# Tests unitaires couverts (StockServiceTest.java) :
# ✅ FEFO — consomme le lot le plus proche de l'expiration en premier
# ✅ FEFO — utilise plusieurs lots si un seul est insuffisant
# ✅ Exception si stock insuffisant
# ✅ Exception si lot périmé
# ✅ Création alerte quand stock < seuil
```

---

## 🗄️ Base de données

**14 tables** créées automatiquement par les migrations Flyway au premier démarrage :

```
utilisateurs        categories          fournisseurs
medicaments         lots                ordonnances
ventes              lignes_vente        commandes_fournisseurs
lignes_commande     bons_reception      alertes_stock
mouvements_stock    audit_log
```

---


<div align="center">

**⚕️ PharmaStock Pro** — Gérer le stock, protéger les patients.

</div>
