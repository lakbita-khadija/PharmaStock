# PharmaStock Pro — Guide de Finalisation

## DÉMARRAGE RAPIDE

### 1. Base de données
```sql
psql -U postgres -c "CREATE DATABASE pharmastock;"
```
Modifier application.properties avec vos identifiants PostgreSQL.

### 2. Backend
```bash
cd pharmastock-backend
mvn clean install -DskipTests
mvn spring-boot:run
# → http://localhost:8080/swagger-ui.html
```
DataInitializer corrige automatiquement les mots de passe BCrypt au démarrage.

### 3. Frontend
```bash
cd pharmastock-frontend
npm install
echo "REACT_APP_API_URL=http://localhost:8080/api/v1" > .env.local
npm start
# → http://localhost:3000
```

### 4. Comptes de test
| Email | Rôle | Mot de passe |
|-------|------|-------------|
| admin@pharma.ma | Admin | Admin123! |
| pharma@pharma.ma | Pharmacien | Admin123! |
| caissier@pharma.ma | Caissier | Admin123! |
| gestion@pharma.ma | Gestionnaire | Admin123! |

### 5. Tests
```bash
mvn test
```

## CE QUI RESTE À FAIRE

### OBLIGATOIRE
- [ ] Ajouter @JsonIgnore/@JsonManagedReference sur les entités pour éviter les boucles JSON
- [ ] Tester toutes les routes avec Swagger ou Thunder Client
- [ ] Vérifier CORS si frontend et backend sont sur des ports différents

### IMPORTANT
- [ ] Implémenter la query top médicaments dans DashboardController
- [ ] Ajouter les pages Fournisseurs, Ordonnances, Audit dans le frontend
- [ ] Compléter le rapport mouvements PDF

### BONUS
- [ ] WebSocket pour alertes push temps réel
- [ ] Export Excel Apache POI
- [ ] Module retours fournisseurs/clients

## CHECKLIST SOUTENANCE
- [ ] Rapport PFA rédigé
- [ ] Diagramme de classes UML
- [ ] Schéma ERD base de données
- [ ] Captures d'écran de toutes les pages
- [ ] PowerPoint de présentation (10-15 slides)
- [ ] Démonstration live préparée (scénario de vente complet)

## POINTS FORTS À METTRE EN AVANT
1. Architecture Spring Boot 3 professionnelle (Controller/Service/Repository)
2. Sécurité JWT + BCrypt + verrouillage de compte
3. Règle FEFO automatique (First Expired First Out)
4. Traçabilité complète par lot (réception → vente → rappel)
5. Alertes automatiques (scheduler nocturne + déclenchement temps réel)
6. Rapports PDF dynamiques (iText 7)
7. Tests unitaires et d'intégration (JUnit 5 + Mockito + MockMvc)
8. Interface Tailwind CSS thème vert pharmacie
