-- =============================================
-- PharmaStock Pro — Données initiales
-- Migration V2 — Flyway
-- =============================================

-- Mot de passe = BCrypt de "Admin123!" pour tous les comptes
INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role) VALUES
('Alami',    'Mohammed', 'admin@pharma.ma',     '$2a$12$LQv3c1yqBwEHXQcvdHDku.HZFyGFVmMDEfxQFjCMY5wRTtnJrte6O', 'ADMIN'),
('Benali',   'Fatima',   'pharma@pharma.ma',    '$2a$12$LQv3c1yqBwEHXQcvdHDku.HZFyGFVmMDEfxQFjCMY5wRTtnJrte6O', 'PHARMACIEN'),
('Chraibi',  'Youssef',  'caissier@pharma.ma',  '$2a$12$LQv3c1yqBwEHXQcvdHDku.HZFyGFVmMDEfxQFjCMY5wRTtnJrte6O', 'CAISSIER'),
('Daoudi',   'Salma',    'gestion@pharma.ma',   '$2a$12$LQv3c1yqBwEHXQcvdHDku.HZFyGFVmMDEfxQFjCMY5wRTtnJrte6O', 'GESTIONNAIRE_STOCK');

-- Catégories thérapeutiques
INSERT INTO categories (nom, description) VALUES
('Analgésiques',         'Médicaments contre la douleur'),
('Antibiotiques',        'Médicaments antibactériens'),
('Antihypertenseurs',    'Traitement de la tension artérielle'),
('Antidiabétiques',      'Traitement du diabète'),
('Antihistaminiques',    'Traitement des allergies'),
('Anti-inflammatoires',  'Réduction de l''inflammation'),
('Vitamines et Minéraux','Compléments nutritionnels'),
('Dermatologie',         'Soins de la peau'),
('Gastro-entérologie',   'Troubles digestifs'),
('Respiratoire',         'Maladies respiratoires');

-- Fournisseurs
INSERT INTO fournisseurs (nom, raison_sociale, telephone, email, adresse) VALUES
('COOPER Maroc',         'Cooper Pharma SA',       '0522-345678', 'commandes@cooper.ma', 'Casablanca, Zone Industrielle'),
('SOTHEMA',              'SOTHEMA SA',             '0537-123456', 'ventes@sothema.ma',   'Rabat, Ain Aouda'),
('LAPROPHAN',            'LAPROPHAN SA',           '0522-987654', 'info@laprophan.ma',   'Casablanca'),
('MAPHAR',               'MAPHAR SA',              '0522-456789', 'stock@maphar.ma',     'Casablanca, Bouskoura'),
('PHARMA 5',             'Pharma 5 SA',            '0522-111222', 'orders@pharma5.ma',   'Casablanca');

-- Médicaments (exemples représentatifs)
INSERT INTO medicaments (nom_commercial, dci, formegalenique, dosage, code_barre, prix_achat_ht, prix_vente_ttc, seuil_minimal, statut_dispensation, categorie_id, fournisseur_id) VALUES
('Doliprane',       'Paracétamol',      'Comprimé',  '1000 mg',  '3400935648609', 3.50,  12.50,  30, 'LIBRE',      1, 1),
('Efferalgan',      'Paracétamol',      'Comprimé effervescent', '500 mg', '3400922456891', 4.20, 15.00, 20, 'LIBRE', 1, 1),
('Amoxicilline',    'Amoxicilline',     'Gélule',    '500 mg',   '3400935112233', 8.00,  28.00,  15, 'ORDONNANCE', 2, 2),
('Augmentin',       'Amoxicilline/Acide clavulanique', 'Comprimé', '1g', '3400935223344', 25.00, 65.00, 10, 'ORDONNANCE', 2, 2),
('Amlor',           'Amlodipine',       'Comprimé',  '5 mg',     '3400935334455', 15.00, 45.00,  10, 'ORDONNANCE', 3, 3),
('Glucophage',      'Metformine',       'Comprimé',  '850 mg',   '3400935445566', 12.00, 35.00,  15, 'ORDONNANCE', 4, 4),
('Aerius',          'Desloratadine',    'Comprimé',  '5 mg',     '3400935556677', 18.00, 52.00,  10, 'LIBRE',      5, 5),
('Ibuprofène',      'Ibuprofène',       'Comprimé',  '400 mg',   '3400935667788', 5.00,  18.00,  20, 'LIBRE',      6, 1),
('Vitamine C',      'Acide ascorbique', 'Comprimé effervescent', '1000 mg', '3400935778899', 6.00, 22.00, 25, 'LIBRE', 7, 2),
('Oméprazole',      'Oméprazole',       'Gélule',    '20 mg',    '3400935889900', 7.00,  25.00,  20, 'LIBRE',      9, 3),
('Ventoline',       'Salbutamol',       'Spray',     '100 mcg',  '3400935990011', 22.00, 68.00,  8,  'ORDONNANCE', 10, 4),
('Bétadine',        'Povidone iodée',   'Solution',  '10%',      '3400936001122', 9.00,  32.00,  15, 'LIBRE',      8, 5);
