-- H2-compatible seed data for dev profile
-- Mirrors the essential records from database/dml/02_seed_data.sql

-- COMPANY
INSERT INTO company (company_id, company_name, access_key, status, created_at, updated_at) VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Demo Customer Inc.', 'DEMO2026', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Demo user: demo@customer.com / password123
INSERT INTO app_user (user_id, company_id, full_name, email, auth_provider_uid, role, created_at) VALUES
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Demo User', 'demo@customer.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CUSTOMER', CURRENT_TIMESTAMP);

-- CONTRACT
INSERT INTO contract (contract_id, company_id, program_id, tier, fuel_program, payment_terms, late_payment_fee_pct, effective_from) VALUES
  ('CTR-001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
   '22222222-2222-2222-2222-222222222222', 'STANDARD', 'FUEL_STANDARD', 'NET30', 0.0100, DATE '2026-01-01');

-- PRICING PROGRAMS
INSERT INTO pricing_program (program_id, name, type) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Everyday Savings (Flat)',   'FLAT'),
  ('22222222-2222-2222-2222-222222222222', 'Save as You Grow (Tiered)', 'VOLUME_TIERED');

-- DISCOUNT CATEGORIES
INSERT INTO discount_category (category_code, display_name) VALUES
  ('AIR',                'Domestic Air Services'),
  ('GROUND',             'Domestic Ground Services'),
  ('INTL_EXPRESS_EXPORT','International Express Export'),
  ('INTL_EXPRESS_IMPORT','International Express Import'),
  ('INTL_STANDARD',      'International Standard');

-- SERVICE LEVELS
INSERT INTO service_level (service_code, display_name, transit_days, is_air, discount_category) VALUES
  ('GROUND',          'Ground',                        4, FALSE, 'GROUND'),
  ('GROUND_RES',      'Ground Residential',            4, FALSE, 'GROUND'),
  ('THREE_DAY',       '3 Day Select',                  3, FALSE, 'GROUND'),
  ('TWO_DAY',         '2 Day Air',                     2, TRUE,  'AIR'),
  ('EXPRESS_SAVER',   'Express Saver',                 1, TRUE,  'AIR'),
  ('EXPRESS',         'Next Day Express',              1, TRUE,  'AIR'),
  ('INTL_EXP_EXPORT', 'International Express Export',  2, TRUE,  'INTL_EXPRESS_EXPORT'),
  ('INTL_EXP_IMPORT', 'International Express Import',  2, TRUE,  'INTL_EXPRESS_IMPORT'),
  ('INTL_STANDARD',   'International Standard',        5, FALSE, 'INTL_STANDARD');

-- DIM FACTORS
INSERT INTO dim_factor (service_code, divisor) VALUES
  ('GROUND', 139), ('GROUND_RES', 139), ('THREE_DAY', 139),
  ('TWO_DAY', 166), ('EXPRESS_SAVER', 166), ('EXPRESS', 166),
  ('INTL_EXP_EXPORT', 166), ('INTL_EXP_IMPORT', 166), ('INTL_STANDARD', 166);

-- ACCESSORIAL TYPES
INSERT INTO accessorial_type (code, display_name, default_fee, trigger_rule, apply_basis) VALUES
  ('RESIDENTIAL',            'Residential Surcharge',                5.25, 'DeliveryType=Residential',       'PER_PACKAGE'),
  ('DELIVERY_AREA',          'Delivery Area Surcharge',              7.75, 'Zip in DAS table',               'PER_PACKAGE'),
  ('DELIVERY_AREA_EXT',      'Delivery Area Surcharge Extended',     9.25, 'Zip in DAS Extended table',      'PER_PACKAGE'),
  ('ADDL_HANDLING',          'Additional Handling',                 15.00, 'Oversize/Irregular',             'PER_PACKAGE'),
  ('DEMAND',                 'Demand Surcharge',                     3.50, 'PeakSeason=true',                'PER_PACKAGE'),
  ('SATURDAY',               'Saturday Delivery',                   16.00, 'DeliveryDay=Saturday',           'PER_SHIPMENT'),
  ('DECLARED_VALUE',         'Declared Value',                       3.00, 'DeclaredValue>100',              'PER_PACKAGE'),
  ('PREMIUM_AIR',            'Premium Air Fee',                      4.25, 'Service=EXPRESS',                'PER_PACKAGE');

-- FUEL PROGRAM + WEEKLY INDEX
INSERT INTO fuel_program (fuel_program, index_basis, formula_note) VALUES
  ('FUEL_STANDARD', 'FUEL_INDEX', 'Fuel % derived from weekly fuel index band.');

INSERT INTO fuel_index (fuel_program, week_code, fuel_index, fuel_pct, effective_from) VALUES
  ('FUEL_STANDARD', '2026-W22', 5.010, 14.00, DATE '2026-05-25'),
  ('FUEL_STANDARD', '2026-W23', 5.120, 14.50, DATE '2026-06-01'),
  ('FUEL_STANDARD', '2026-W24', 5.180, 14.75, DATE '2026-06-08'),
  ('FUEL_STANDARD', '2026-W25', 5.210, 15.00, DATE '2026-06-15');

-- ZONE MATRIX
INSERT INTO zone_matrix (origin_prefix, dest_prefix, zone) VALUES
  ('30', '30', 2),
  ('30', '35', 3),
  ('30', '60', 5),
  ('30', '75', 5),
  ('30', '90', 8);

-- RATE CARD
INSERT INTO rate_card (rate_card_id, version, service_code, zone, weight_from_lb, weight_to_lb, base_rate, effective_from) VALUES
  (RANDOM_UUID(),'2026-01','GROUND',2, 0,  1,  9.50, DATE '2026-01-01'),
  (RANDOM_UUID(),'2026-01','GROUND',2, 1,  5, 11.20, DATE '2026-01-01'),
  (RANDOM_UUID(),'2026-01','GROUND',5, 1,  5, 15.80, DATE '2026-01-01'),
  (RANDOM_UUID(),'2026-01','GROUND',5, 5, 20, 24.60, DATE '2026-01-01'),
  (RANDOM_UUID(),'2026-01','GROUND',8, 5, 20, 31.40, DATE '2026-01-01'),
  (RANDOM_UUID(),'2026-01','THREE_DAY',5, 1,  5, 22.00, DATE '2026-01-01'),
  (RANDOM_UUID(),'2026-01','THREE_DAY',5, 5, 20, 34.00, DATE '2026-01-01'),
  (RANDOM_UUID(),'2026-01','EXPRESS',5, 1,  5, 48.00, DATE '2026-01-01'),
  (RANDOM_UUID(),'2026-01','EXPRESS',5, 5, 20, 72.00, DATE '2026-01-01'),
  (RANDOM_UUID(),'2026-01','EXPRESS',8, 5, 20, 95.00, DATE '2026-01-01');

-- DISCOUNT TIERS (VOLUME_TIERED program)
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('22222222-2222-2222-2222-222222222222','GROUND',  0, 10,   0.3600),
  ('22222222-2222-2222-2222-222222222222','GROUND', 11, 30,   0.4400),
  ('22222222-2222-2222-2222-222222222222','GROUND', 31, 40,   0.4600),
  ('22222222-2222-2222-2222-222222222222','GROUND', 41, 50,   0.5000),
  ('22222222-2222-2222-2222-222222222222','GROUND', 51, 70,   0.5100),
  ('22222222-2222-2222-2222-222222222222','GROUND', 71, NULL, 0.5200),
  ('22222222-2222-2222-2222-222222222222','AIR',  0, 10,   0.6000),
  ('22222222-2222-2222-2222-222222222222','AIR', 11, 30,   0.6600),
  ('22222222-2222-2222-2222-222222222222','AIR', 31, 40,   0.6800),
  ('22222222-2222-2222-2222-222222222222','AIR', 41, 50,   0.7200),
  ('22222222-2222-2222-2222-222222222222','AIR', 51, 70,   0.7400),
  ('22222222-2222-2222-2222-222222222222','AIR', 71, NULL, 0.7500),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT',  0, 10,   0.5500),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 11, 30,   0.6500),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 31, 40,   0.7000),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 41, 50,   0.7100),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 51, 70,   0.7200),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 71, NULL, 0.7400),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT',  0, 10,   0.4700),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 11, 30,   0.5300),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 31, 40,   0.5400),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 41, 50,   0.5500),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 51, 70,   0.5600),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 71, NULL, 0.5800),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD',  0, 10,   0.2700),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 11, 30,   0.3200),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 31, 40,   0.3300),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 41, 50,   0.3400),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 51, 70,   0.3500),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 71, NULL, 0.3700);

-- CONTRACT INCENTIVES (surcharge reductions)
INSERT INTO contract_incentive (contract_id, incentive_type, accessorial_code, incentive_value, unit) VALUES
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'RESIDENTIAL',             0.40, 'PCT'),
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'DELIVERY_AREA',           0.40, 'PCT'),
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'DELIVERY_AREA_EXT',       0.40, 'PCT');

-- BASELINE SNAPSHOT
INSERT INTO baseline_snapshot (baseline_id, company_id, period_from, period_to, total_shipments,
                               avg_weekly_volume, total_cost, metrics_json) VALUES
  (RANDOM_UUID(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', DATE '2025-06-01', DATE '2026-05-31', 1040,
   20.0, 502000.00,
   '{"spend_by_service":{"GROUND":300000,"EXPRESS":178000,"THREE_DAY":24000},"spend_by_zone":{"2":40000,"5":300000,"8":162000},"avgWeightLb":9.4,"residentialPct":0.35}');

