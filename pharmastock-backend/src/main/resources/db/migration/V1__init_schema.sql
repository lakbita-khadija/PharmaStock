-- =============================================
-- PharmaStock Pro — Schéma initial PostgreSQL
-- Migration V1 — Flyway
-- =============================================

-- ── Enum types ──
CREATE TYPE role_utilisateur AS ENUM ('ADMIN','PHARMACIEN','CAISSIER','GESTIONNAIRE_STOCK');
CREATE TYPE statut_dispensation AS ENUM ('LIBRE','ORDONNANCE','STUPEFIANT','LISTE_I','LISTE_II');
CREATE TYPE statut_lot AS ENUM ('ACTIF','BLOQUE','EXPIRE','EPUISE');
CREATE TYPE statut_commande AS ENUM ('BROUILLON','ENVOYEE','RECUE_PARTIELLE','RECUE_TOTALE','ANNULEE');
CREATE TYPE statut_vente AS ENUM ('VALIDEE','ANNULEE');
CREATE TYPE type_mouvement AS ENUM ('ENTREE','SORTIE','RETOUR_CLIENT','RETOUR_FOURNISSEUR','AJUSTEMENT_INV','DESTRUCTION');
CREATE TYPE type_alerte AS ENUM ('STOCK_FAIBLE','RUPTURE','PEREMPTION_90J','PEREMPTION_30J','PEREMPTION_7J','LOT_EXPIRE','RAPPEL_LOT','ANOMALIE_RECEPTION');
CREATE TYPE niveau_alerte AS ENUM ('INFO','AVERTISSEMENT','CRITIQUE','BLOQUANT');
CREATE TYPE statut_alerte AS ENUM ('ACTIVE','ACQUITTEE','RESOLUE');
CREATE TYPE mode_paiement AS ENUM ('ESPECES','CARTE','ASSURANCE','VIREMENT');
CREATE TYPE statut_inventaire AS ENUM ('EN_COURS','VALIDE','ANNULE');
CREATE TYPE type_retour AS ENUM ('CLIENT','FOURNISSEUR');

-- ── Utilisateurs ──
CREATE TABLE utilisateurs (
    id                  BIGSERIAL PRIMARY KEY,
    nom                 VARCHAR(100)           NOT NULL,
    prenom              VARCHAR(100)           NOT NULL,
    email               VARCHAR(150)           NOT NULL UNIQUE,
    mot_de_passe        VARCHAR(255)           NOT NULL,
    role                role_utilisateur       NOT NULL DEFAULT 'CAISSIER',
    actif               BOOLEAN                NOT NULL DEFAULT TRUE,
    tentatives_echec    INTEGER                NOT NULL DEFAULT 0,
    derniere_connexion  TIMESTAMP,
    date_creation       TIMESTAMP              NOT NULL DEFAULT NOW(),
    date_modification   TIMESTAMP
);

-- ── Catégories ──
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    date_creation TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── Fournisseurs ──
CREATE TABLE fournisseurs (
    id              BIGSERIAL PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    raison_sociale  VARCHAR(200),
    adresse         TEXT,
    telephone       VARCHAR(20),
    email           VARCHAR(150),
    contact_nom     VARCHAR(200),
    actif           BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── Médicaments ──
CREATE TABLE medicaments (
    id                      BIGSERIAL PRIMARY KEY,
    nom_commercial          VARCHAR(200) NOT NULL,
    dci                     VARCHAR(200) NOT NULL,
    formegalenique          VARCHAR(100) NOT NULL,
    dosage                  VARCHAR(100) NOT NULL,
    code_barre              VARCHAR(30)  UNIQUE,
    code_atc                VARCHAR(20),
    prix_achat_ht           DECIMAL(10,2),
    prix_vente_ttc          DECIMAL(10,2) NOT NULL,
    seuil_minimal           INTEGER       NOT NULL DEFAULT 10,
    statut_dispensation     statut_dispensation NOT NULL DEFAULT 'LIBRE',
    actif                   BOOLEAN       NOT NULL DEFAULT TRUE,
    categorie_id            BIGINT        REFERENCES categories(id) ON DELETE SET NULL,
    fournisseur_id          BIGINT        REFERENCES fournisseurs(id) ON DELETE SET NULL,
    date_creation           TIMESTAMP     NOT NULL DEFAULT NOW(),
    date_modification       TIMESTAMP
);
CREATE INDEX idx_med_nom ON medicaments(nom_commercial);
CREATE INDEX idx_med_dci ON medicaments(dci);
CREATE INDEX idx_med_code_barre ON medicaments(code_barre);

-- ── Lots ──
CREATE TABLE lots (
    id                  BIGSERIAL PRIMARY KEY,
    numero_lot          VARCHAR(100) NOT NULL,
    date_fabrication    DATE,
    date_expiration     DATE NOT NULL,
    quantite_disponible INTEGER NOT NULL DEFAULT 0,
    statut              statut_lot NOT NULL DEFAULT 'ACTIF',
    medicament_id       BIGINT NOT NULL REFERENCES medicaments(id) ON DELETE CASCADE,
    date_creation       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(numero_lot, medicament_id)
);
CREATE INDEX idx_lot_medicament ON lots(medicament_id);
CREATE INDEX idx_lot_expiration ON lots(date_expiration);
CREATE INDEX idx_lot_statut ON lots(statut);

-- ── Commandes fournisseurs ──
CREATE TABLE commandes_fournisseurs (
    id                  BIGSERIAL PRIMARY KEY,
    numero_commande     VARCHAR(30) NOT NULL UNIQUE,
    date_creation       TIMESTAMP NOT NULL DEFAULT NOW(),
    date_envoi          TIMESTAMP,
    statut              statut_commande NOT NULL DEFAULT 'BROUILLON',
    montant_total       DECIMAL(12,2),
    fournisseur_id      BIGINT NOT NULL REFERENCES fournisseurs(id),
    createur_id         BIGINT NOT NULL REFERENCES utilisateurs(id)
);

CREATE TABLE lignes_commande (
    id                  BIGSERIAL PRIMARY KEY,
    quantite_commandee  INTEGER NOT NULL,
    prix_unitaire       DECIMAL(10,2),
    quantite_recue      INTEGER NOT NULL DEFAULT 0,
    commande_id         BIGINT NOT NULL REFERENCES commandes_fournisseurs(id) ON DELETE CASCADE,
    medicament_id       BIGINT NOT NULL REFERENCES medicaments(id)
);

-- ── Bons de réception ──
CREATE TABLE bons_reception (
    id                  BIGSERIAL PRIMARY KEY,
    numero_bon          VARCHAR(30) NOT NULL UNIQUE,
    date_reception      TIMESTAMP NOT NULL DEFAULT NOW(),
    statut              VARCHAR(20) NOT NULL DEFAULT 'PARTIEL',
    commande_id         BIGINT REFERENCES commandes_fournisseurs(id),
    receptionnaire_id   BIGINT NOT NULL REFERENCES utilisateurs(id)
);

CREATE TABLE lignes_reception (
    id                  BIGSERIAL PRIMARY KEY,
    quantite_recue      INTEGER NOT NULL,
    prix_achat_reel     DECIMAL(10,2),
    bon_reception_id    BIGINT NOT NULL REFERENCES bons_reception(id) ON DELETE CASCADE,
    lot_id              BIGINT NOT NULL REFERENCES lots(id)
);

-- ── Ordonnances ──
CREATE TABLE ordonnances (
    id                  BIGSERIAL PRIMARY KEY,
    numero_ordonnance   VARCHAR(50) NOT NULL UNIQUE,
    prescripteur        VARCHAR(200),
    patient_nom         VARCHAR(200),
    patient_naissance   DATE,
    date_prescription   DATE NOT NULL,
    date_validite       DATE,
    validee_par_id      BIGINT REFERENCES utilisateurs(id),
    date_creation       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── Ventes ──
CREATE TABLE ventes (
    id                  BIGSERIAL PRIMARY KEY,
    numero_vente        VARCHAR(30) NOT NULL UNIQUE,
    date_vente          TIMESTAMP NOT NULL DEFAULT NOW(),
    total_ttc           DECIMAL(12,2) NOT NULL,
    montant_donne       DECIMAL(12,2),
    rendu_monnaie       DECIMAL(12,2),
    mode_paiement       mode_paiement NOT NULL DEFAULT 'ESPECES',
    statut              statut_vente NOT NULL DEFAULT 'VALIDEE',
    motif_annulation    TEXT,
    caissier_id         BIGINT NOT NULL REFERENCES utilisateurs(id),
    ordonnance_id       BIGINT REFERENCES ordonnances(id),
    date_annulation     TIMESTAMP,
    annule_par_id       BIGINT REFERENCES utilisateurs(id)
);
CREATE INDEX idx_vente_date ON ventes(date_vente);
CREATE INDEX idx_vente_caissier ON ventes(caissier_id);

CREATE TABLE lignes_vente (
    id                  BIGSERIAL PRIMARY KEY,
    quantite_vendue     INTEGER NOT NULL,
    prix_unitaire       DECIMAL(10,2) NOT NULL,
    remise_pct          DECIMAL(5,2) NOT NULL DEFAULT 0,
    sous_total          DECIMAL(12,2) NOT NULL,
    vente_id            BIGINT NOT NULL REFERENCES ventes(id) ON DELETE CASCADE,
    lot_id              BIGINT NOT NULL REFERENCES lots(id),
    medicament_id       BIGINT NOT NULL REFERENCES medicaments(id)
);

-- ── Mouvements de stock ──
CREATE TABLE mouvements_stock (
    id                  BIGSERIAL PRIMARY KEY,
    type_operation      type_mouvement NOT NULL,
    quantite            INTEGER NOT NULL,
    quantite_avant      INTEGER NOT NULL,
    quantite_apres      INTEGER NOT NULL,
    reference_doc       VARCHAR(50),
    commentaire         TEXT,
    date_operation      TIMESTAMP NOT NULL DEFAULT NOW(),
    lot_id              BIGINT NOT NULL REFERENCES lots(id),
    medicament_id       BIGINT NOT NULL REFERENCES medicaments(id),
    utilisateur_id      BIGINT NOT NULL REFERENCES utilisateurs(id)
);
CREATE INDEX idx_mouv_medicament ON mouvements_stock(medicament_id);
CREATE INDEX idx_mouv_date ON mouvements_stock(date_operation);

-- ── Alertes ──
CREATE TABLE alertes_stock (
    id                  BIGSERIAL PRIMARY KEY,
    type_alerte         type_alerte NOT NULL,
    niveau              niveau_alerte NOT NULL,
    message             TEXT NOT NULL,
    statut              statut_alerte NOT NULL DEFAULT 'ACTIVE',
    commentaire_acquittement TEXT,
    date_creation       TIMESTAMP NOT NULL DEFAULT NOW(),
    date_acquittement   TIMESTAMP,
    medicament_id       BIGINT REFERENCES medicaments(id),
    lot_id              BIGINT REFERENCES lots(id),
    acquitte_par_id     BIGINT REFERENCES utilisateurs(id)
);
CREATE INDEX idx_alerte_statut ON alertes_stock(statut);
CREATE INDEX idx_alerte_niveau ON alertes_stock(niveau);

-- ── Inventaires ──
CREATE TABLE inventaires (
    id                  BIGSERIAL PRIMARY KEY,
    type                VARCHAR(20) NOT NULL DEFAULT 'TOTAL',
    responsable         VARCHAR(200),
    commentaire         TEXT,
    statut              statut_inventaire NOT NULL DEFAULT 'EN_COURS',
    nb_articles         INTEGER DEFAULT 0,
    nb_ecarts           INTEGER DEFAULT 0,
    date_debut          TIMESTAMP NOT NULL DEFAULT NOW(),
    date_validation     TIMESTAMP,
    valide_par_id       BIGINT REFERENCES utilisateurs(id)
);

CREATE TABLE lignes_inventaire (
    id                      BIGSERIAL PRIMARY KEY,
    quantite_theorique      INTEGER NOT NULL,
    quantite_physique       INTEGER,
    ecart                   INTEGER,
    inventaire_id           BIGINT NOT NULL REFERENCES inventaires(id) ON DELETE CASCADE,
    lot_id                  BIGINT NOT NULL REFERENCES lots(id),
    medicament_id           BIGINT NOT NULL REFERENCES medicaments(id)
);

-- ── Retours ──
CREATE TABLE retours (
    id                  BIGSERIAL PRIMARY KEY,
    numero_retour       VARCHAR(30) NOT NULL UNIQUE,
    type_retour         type_retour NOT NULL,
    motif               TEXT NOT NULL,
    quantite            INTEGER NOT NULL,
    date_retour         TIMESTAMP NOT NULL DEFAULT NOW(),
    lot_id              BIGINT NOT NULL REFERENCES lots(id),
    vente_id            BIGINT REFERENCES ventes(id),
    commande_id         BIGINT REFERENCES commandes_fournisseurs(id),
    traite_par_id       BIGINT NOT NULL REFERENCES utilisateurs(id)
);

-- ── Audit log ──
CREATE TABLE audit_log (
    id                  BIGSERIAL PRIMARY KEY,
    action              VARCHAR(100) NOT NULL,
    entite              VARCHAR(100) NOT NULL,
    id_entite           BIGINT,
    ancienne_valeur     TEXT,
    nouvelle_valeur     TEXT,
    adresse_ip          VARCHAR(50),
    timestamp           TIMESTAMP NOT NULL DEFAULT NOW(),
    utilisateur_id      BIGINT REFERENCES utilisateurs(id)
);
CREATE INDEX idx_audit_utilisateur ON audit_log(utilisateur_id);
CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_entite ON audit_log(entite);
