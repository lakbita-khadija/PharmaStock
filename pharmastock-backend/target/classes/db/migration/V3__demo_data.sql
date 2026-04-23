-- =============================================
-- PharmaStock Pro — Données de démonstration
-- Migration V3 — Flyway
-- =============================================

-- Lots de démonstration avec dates variées
INSERT INTO lots (numero_lot, date_fabrication, date_expiration, quantite_disponible, statut, medicament_id) VALUES
-- Doliprane : lot expirant dans 5j (alerte critique), lot normal
('DOL-2024-001', '2023-01-10', CURRENT_DATE + INTERVAL '5 days',  15, 'ACTIF', 1),
('DOL-2024-002', '2024-01-10', CURRENT_DATE + INTERVAL '200 days', 80, 'ACTIF', 1),

-- Efferalgan : stock normal
('EFF-2024-001', '2024-02-01', CURRENT_DATE + INTERVAL '150 days', 40, 'ACTIF', 2),

-- Amoxicilline : lot expirant dans 25j (avertissement) + lot périmé (bloqué)
('AMX-2023-001', '2022-06-01', CURRENT_DATE - INTERVAL '3 days',  5, 'EXPIRE', 3),
('AMX-2024-001', '2024-03-01', CURRENT_DATE + INTERVAL '25 days', 12, 'ACTIF', 3),
('AMX-2024-002', '2024-04-01', CURRENT_DATE + INTERVAL '300 days', 30, 'ACTIF', 3),

-- Augmentin : stock faible (sous seuil de 10)
('AUG-2024-001', '2024-01-15', CURRENT_DATE + INTERVAL '90 days', 6, 'ACTIF', 4),

-- Amlor : lots normaux
('AML-2024-001', '2024-02-20', CURRENT_DATE + INTERVAL '365 days', 25, 'ACTIF', 5),

-- Glucophage
('GLU-2024-001', '2024-01-01', CURRENT_DATE + INTERVAL '180 days', 60, 'ACTIF', 6),

-- Aerius
('AER-2024-001', '2024-03-10', CURRENT_DATE + INTERVAL '240 days', 35, 'ACTIF', 7),

-- Ibuprofène
('IBU-2024-001', '2024-02-05', CURRENT_DATE + INTERVAL '120 days', 90, 'ACTIF', 8),

-- Vitamine C
('VIT-2024-001', '2024-01-20', CURRENT_DATE + INTERVAL '270 days', 45, 'ACTIF', 9),

-- Oméprazole
('OMP-2024-001', '2024-03-01', CURRENT_DATE + INTERVAL '180 days', 30, 'ACTIF', 10),

-- Ventoline : lot bloqué (rappel fabricant)
('VEN-2023-001', '2023-08-01', CURRENT_DATE + INTERVAL '60 days', 8, 'BLOQUE', 11),
('VEN-2024-001', '2024-02-01', CURRENT_DATE + INTERVAL '400 days', 15, 'ACTIF', 11),

-- Bétadine : stock très faible
('BET-2024-001', '2024-03-15', CURRENT_DATE + INTERVAL '365 days', 3, 'ACTIF', 12);

-- Alertes de démonstration (cohérentes avec les lots ci-dessus)
INSERT INTO alertes_stock (type_alerte, niveau, message, statut, medicament_id) VALUES
('PEREMPTION_7J',  'CRITIQUE',      'PÉREMPTION IMMINENTE — Doliprane 1000mg : lot DOL-2024-001 expire dans 5 jours (15 unités en stock)',     'ACTIVE', 1),
('PEREMPTION_30J', 'AVERTISSEMENT', 'Péremption dans 25 jours — Amoxicilline 500mg : lot AMX-2024-001 expire le ' || (CURRENT_DATE + INTERVAL '25 days')::text, 'ACTIVE', 3),
('LOT_EXPIRE',     'BLOQUANT',      'LOT EXPIRÉ — Amoxicilline 500mg : lot AMX-2023-001 a expiré. Destruction requise.',                       'ACTIVE', 3),
('STOCK_FAIBLE',   'AVERTISSEMENT', 'Stock faible — Augmentin 1g : 6 unité(s) en stock (seuil minimal : 10)',                                  'ACTIVE', 4),
('STOCK_FAIBLE',   'CRITIQUE',      'Stock très faible — Bétadine 10% : 3 unité(s) en stock (seuil minimal : 15)',                             'ACTIVE', 12),
('RAPPEL_LOT',     'CRITIQUE',      'LOT BLOQUÉ — Ventoline 100mcg : lot VEN-2023-001 bloqué pour rappel fabricant',                           'ACTIVE', 11);
