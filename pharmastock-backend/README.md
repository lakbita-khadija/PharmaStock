# PharmaStock Pro — Backend
## Spring Boot 3 · Java 17 · PostgreSQL · JWT

---

## Prérequis
- Java 17+
- Maven 3.9+
- PostgreSQL 15 (ou MySQL 8)

## Installation rapide

```bash
# 1. Créer la base de données PostgreSQL
psql -U postgres -c "CREATE DATABASE pharmastock;"

# 2. Configurer application.properties
#    Modifier : spring.datasource.username / password

# 3. Compiler et démarrer
mvn clean install -DskipTests
mvn spring-boot:run
```

## L'application démarre sur : http://localhost:8080
## Swagger UI : http://localhost:8080/swagger-ui.html

---

## Architecture des packages

```
src/main/java/ma/pharmacie/pharmastock/
├── PharmaStockApplication.java       ← Point d'entrée + @EnableScheduling
│
├── config/
│   └── SecurityConfig.java           ← Spring Security + CORS + JWT
│
├── security/
│   ├── JwtUtil.java                  ← Génération/validation JWT
│   ├── JwtAuthFilter.java            ← Filtre HTTP JWT
│   └── UserDetailsServiceImpl.java   ← Chargement utilisateur
│
├── entity/                           ← Entités JPA (@Entity)
│   ├── Utilisateur.java
│   ├── Medicament.java
│   ├── Categorie.java
│   ├── Fournisseur.java
│   ├── Lot.java                      ← Traçabilité par lot
│   ├── Vente.java + LigneVente.java
│   ├── CommandeFournisseur.java + LigneCommande.java
│   ├── AlerteStock.java              ← Alertes automatiques
│   ├── MouvementStock.java           ← Historique complet
│   ├── Ordonnance.java
│   └── AuditLog.java                 ← Journal d'audit
│
├── enums/                            ← 10 enums métier
│
├── repository/                       ← Spring Data JPA Repositories
│   ├── MedicamentRepository.java     ← Recherche DCI, barcode, filtre
│   ├── LotRepository.java            ← FEFO, expiration, rappels
│   ├── VenteRepository.java          ← Agrégats CA, comptage
│   ├── AlerteStockRepository.java    ← Compteurs, filtres statut
│   └── ...
│
├── service/impl/
│   ├── AuthService.java              ← Login + JWT + lockout
│   ├── StockService.java             ← FEFO + mouvements + alertes
│   └── VenteService.java             ← Vente complète + audit
│
├── controller/                       ← REST Controllers (@RestController)
│   ├── AuthController.java           ← /api/v1/auth/**
│   ├── MedicamentController.java     ← /api/v1/medicaments/**
│   ├── StockController.java          ← /api/v1/stock/** + /lots/**
│   ├── VenteController.java          ← /api/v1/ventes/**
│   ├── CommandeController.java       ← /api/v1/commandes/**
│   ├── AlerteController.java         ← /api/v1/alertes/**
│   ├── DashboardController.java      ← /api/v1/dashboard/**
│   ├── RapportController.java        ← /api/v1/rapports/** (PDF iText)
│   ├── CategorieController.java
│   ├── FournisseurController.java
│   └── UtilisateurController.java
│
├── exception/
│   └── GlobalExceptionHandler.java   ← Réponses d'erreur uniformes
│
└── scheduler/
    └── AlerteScheduler.java          ← Cron 01h00 : péremptions + seuils
```

---

## Migrations Flyway

```
src/main/resources/db/migration/
├── V1__init_schema.sql    ← Schéma complet (14 tables, types PostgreSQL)
└── V2__seed_data.sql      ← Données initiales (utilisateurs, médicaments)
```

---

## Endpoints principaux

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | /api/v1/auth/login | Connexion → JWT |
| GET | /api/v1/medicaments | Catalogue paginé |
| GET | /api/v1/medicaments/search?q= | Recherche caisse |
| GET | /api/v1/stock | Vue stock par médicament |
| POST | /api/v1/ventes | Créer une vente (FEFO auto) |
| GET | /api/v1/alertes | Alertes actives |
| PUT | /api/v1/alertes/{id}/acquitter | Acquitter une alerte |
| GET | /api/v1/dashboard/kpis | KPIs temps réel |
| GET | /api/v1/rapports/stock | PDF rapport stock |
| GET | /api/v1/rapports/ventes | PDF rapport ventes |

---

## Sécurité

- Authentification **JWT** (token 15min + refresh 7j)
- Mots de passe **BCrypt** (coût 12)
- **Verrouillage** compte après 5 tentatives échouées
- **CORS** configuré pour http://localhost:3000
- Contrôle d'accès par rôle sur chaque endpoint (`@PreAuthorize`)

---

## Logique métier clé

### Règle FEFO (First Expired, First Out)
Lors de chaque vente, `StockService.sortieStockFefo()` :
1. Récupère les lots actifs triés par `dateExpiration ASC`
2. Consomme le lot le plus proche de l'expiration en premier
3. Enregistre un `MouvementStock` par lot consommé
4. Vérifie le seuil et crée une alerte si nécessaire

### Alertes automatiques
Le `AlerteScheduler` s'exécute chaque nuit à 01h00 :
- Lots expirés → statut EXPIRE + alerte BLOQUANT
- Lots expirant dans 7j → alerte CRITIQUE
- Lots expirant dans 30j → alerte AVERTISSEMENT
- Stock < seuil minimal → alerte STOCK_FAIBLE
