-- =============================================================================
-- Billing Simulation Agent  —  DML (seed / sample data)
-- Target: Supabase (PostgreSQL)   |   Run AFTER 01_schema.sql
-- Generic hackathon sample values only (no brand-specific names).
--
-- NOTE: Not wired into the application. Run manually via the Supabase SQL
--       Editor, or with psql using your project connection string:
--       psql "postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres" -f 02_seed_data.sql
-- =============================================================================

-- ---------------------------------------------------------
-- DISCOUNT CATEGORIES (5 categories from the pricing agreement)
-- ---------------------------------------------------------
INSERT INTO discount_category (category_code, display_name) VALUES
  ('AIR',                'Domestic Air Services'),
  ('GROUND',             'Domestic Ground Services'),
  ('INTL_EXPRESS_EXPORT','International Express Export'),
  ('INTL_EXPRESS_IMPORT','International Express Import'),
  ('INTL_STANDARD',      'International Standard');

-- ---------------------------------------------------------
-- SERVICE LEVELS (each mapped to its discount category)
-- ---------------------------------------------------------
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

-- ---------------------------------------------------------
-- DIM FACTORS
-- ---------------------------------------------------------
INSERT INTO dim_factor (service_code, divisor) VALUES
  ('GROUND', 139), ('GROUND_RES', 139), ('THREE_DAY', 139),
  ('TWO_DAY', 166), ('EXPRESS_SAVER', 166), ('EXPRESS', 166),
  ('INTL_EXP_EXPORT', 166), ('INTL_EXP_IMPORT', 166), ('INTL_STANDARD', 166);

-- ---------------------------------------------------------
-- ACCESSORIAL RULE CARDS
-- Surcharge discounts from the pricing agreement (40% off these 7 codes for volume-tiered):
--   Domestic: RESIDENTIAL, DELIVERY_AREA, DELIVERY_AREA_EXT
--   International: RESIDENTIAL_INTL, DELIVERY_AREA_IMPORT, DELIVERY_AREA_IMPORT_EXT, DELIVERY_AREA_EXPORT_EXT
-- ---------------------------------------------------------
INSERT INTO accessorial_type (code, display_name, default_fee, trigger_rule, apply_basis) VALUES
  ('RESIDENTIAL',            'Residential Surcharge',                      5.25, 'DeliveryType=Residential',                              'PER_PACKAGE'),
  ('DELIVERY_AREA',          'Delivery Area Surcharge',                    7.75, 'Zip in DAS table',                                      'PER_PACKAGE'),
  ('DELIVERY_AREA_EXT',      'Delivery Area Surcharge Extended',           9.25, 'Zip in DAS Extended table',                             'PER_PACKAGE'),
  ('RESIDENTIAL_INTL',       'Residential Surcharge (Import/Export)',      5.25, 'DeliveryType=Residential AND Direction in (IMPORT,EXPORT)','PER_PACKAGE'),
  ('DELIVERY_AREA_IMPORT',   'Import Delivery Area Surcharge',             7.75, 'Zip in DAS table AND Direction=IMPORT',                 'PER_PACKAGE'),
  ('DELIVERY_AREA_IMPORT_EXT','Import Delivery Area Surcharge Extended',   9.25, 'Zip in DAS Extended table AND Direction=IMPORT',        'PER_PACKAGE'),
  ('DELIVERY_AREA_EXPORT_EXT','Export Delivery Area Surcharge Extended',   9.25, 'Zip in DAS Extended table AND Direction=EXPORT',        'PER_PACKAGE'),
  ('ADDL_HANDLING',          'Additional Handling',                       15.00, 'Oversize/Irregular',                                    'PER_PACKAGE'),
  ('DEMAND',                 'Demand Surcharge',                           3.50, 'PeakSeason=true',                                       'PER_PACKAGE'),
  ('SATURDAY',               'Saturday Delivery',                         16.00, 'DeliveryDay=Saturday',                                  'PER_SHIPMENT'),
  ('DECLARED_VALUE',         'Declared Value',                             3.00, 'DeclaredValue>100',                                     'PER_PACKAGE'),
  ('PREMIUM_AIR',            'Premium Air Fee',                            4.25, 'Service=EXPRESS',                                       'PER_PACKAGE');
  ('SATURDAY',      'Saturday Delivery',         16.00, 'DeliveryDay=Saturday',     'PER_SHIPMENT'),
  ('DECLARED_VALUE','Declared Value',             3.00, 'DeclaredValue>100',        'PER_PACKAGE'),
  ('PREMIUM_AIR',   'Premium Air Fee',            4.25, 'Service=EXPRESS',          'PER_PACKAGE');

-- ---------------------------------------------------------
-- FUEL PROGRAM + WEEKLY INDEX
-- ---------------------------------------------------------
INSERT INTO fuel_program (fuel_program, index_basis, formula_note) VALUES
  ('FUEL_STANDARD', 'FUEL_INDEX', 'Fuel % derived from weekly fuel index band.');

INSERT INTO fuel_index (fuel_program, week_code, fuel_index, fuel_pct, effective_from) VALUES
  ('FUEL_STANDARD', '2026-W22', 5.010, 14.00, DATE '2026-05-25'),
  ('FUEL_STANDARD', '2026-W23', 5.120, 14.50, DATE '2026-06-01'),
  ('FUEL_STANDARD', '2026-W24', 5.180, 14.75, DATE '2026-06-08'),
  ('FUEL_STANDARD', '2026-W25', 5.210, 15.00, DATE '2026-06-15');

-- ---------------------------------------------------------
-- ZONE MATRIX (sample origin/dest prefix -> zone)
-- ---------------------------------------------------------
INSERT INTO zone_matrix (origin_prefix, dest_prefix, zone) VALUES
  ('30', '30', 2),
  ('30', '35', 3),
  ('30', '60', 5),
  ('30', '75', 5),
  ('30', '90', 8);

-- ---------------------------------------------------------
-- RATE CARD (published rates; a few weight breaks per service/zone)
-- version '2026-01', effective from 2026-01-01
-- ---------------------------------------------------------
INSERT INTO rate_card (version, service_code, zone, weight_from_lb, weight_to_lb, base_rate, effective_from) VALUES
  -- GROUND
  ('2026-01','GROUND',2, 0,  1,  9.50, DATE '2026-01-01'),
  ('2026-01','GROUND',2, 1,  5, 11.20, DATE '2026-01-01'),
  ('2026-01','GROUND',5, 1,  5, 15.80, DATE '2026-01-01'),
  ('2026-01','GROUND',5, 5, 20, 24.60, DATE '2026-01-01'),
  ('2026-01','GROUND',8, 5, 20, 31.40, DATE '2026-01-01'),
  -- THREE_DAY
  ('2026-01','THREE_DAY',5, 1,  5, 22.00, DATE '2026-01-01'),
  ('2026-01','THREE_DAY',5, 5, 20, 34.00, DATE '2026-01-01'),
  -- EXPRESS (air)
  ('2026-01','EXPRESS',5, 1,  5, 48.00, DATE '2026-01-01'),
  ('2026-01','EXPRESS',5, 5, 20, 72.00, DATE '2026-01-01'),
  ('2026-01','EXPRESS',8, 5, 20, 95.00, DATE '2026-01-01');

-- ---------------------------------------------------------
-- MINIMUM SHIPPING CHARGE FLOORS
-- ---------------------------------------------------------
INSERT INTO min_shipping_charge (service_code, floor_zone, floor_weight_lb, addl_incentive_pct) VALUES
  ('GROUND',    2, 1, 0.00),
  ('THREE_DAY', 5, 1, 0.25);   -- 25% off the 3 Day Select minimum charge

-- ---------------------------------------------------------
-- PRICING PROGRAMS
--   FLAT          = single band (kept for comparison)
--   VOLUME_TIERED = chosen program for the demo account
-- ---------------------------------------------------------
INSERT INTO pricing_program (program_id, name, type) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Everyday Savings (Flat)',   'FLAT'),
  ('22222222-2222-2222-2222-222222222222', 'Save as You Grow (Tiered)', 'VOLUME_TIERED');

-- FLAT program discounts (single band 0..NULL) — all 5 service categories
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('11111111-1111-1111-1111-111111111111','GROUND',              0, NULL, 0.3900),
  ('11111111-1111-1111-1111-111111111111','AIR',                 0, NULL, 0.6250),
  ('11111111-1111-1111-1111-111111111111','INTL_EXPRESS_EXPORT', 0, NULL, 0.5000),
  ('11111111-1111-1111-1111-111111111111','INTL_EXPRESS_IMPORT', 0, NULL, 0.4000),
  ('11111111-1111-1111-1111-111111111111','INTL_STANDARD',       0, NULL, 0.3000);

-- VOLUME_TIERED program discounts — all 5 categories, all 6 volume bands
-- Engine lookup: service_code -> discount_category -> pick row matching avg_weekly_volume
-- GROUND (GROUND, GROUND_RES, THREE_DAY all use this)
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('22222222-2222-2222-2222-222222222222','GROUND',  0, 10,   0.3600),
  ('22222222-2222-2222-2222-222222222222','GROUND', 11, 30,   0.4400),
  ('22222222-2222-2222-2222-222222222222','GROUND', 31, 40,   0.4600),
  ('22222222-2222-2222-2222-222222222222','GROUND', 41, 50,   0.5000),
  ('22222222-2222-2222-2222-222222222222','GROUND', 51, 70,   0.5100),
  ('22222222-2222-2222-2222-222222222222','GROUND', 71, NULL, 0.5200);
-- AIR (EXPRESS, EXPRESS_SAVER, TWO_DAY all use this — one set of tiers, not three)
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('22222222-2222-2222-2222-222222222222','AIR',  0, 10,   0.6000),
  ('22222222-2222-2222-2222-222222222222','AIR', 11, 30,   0.6600),
  ('22222222-2222-2222-2222-222222222222','AIR', 31, 40,   0.6800),
  ('22222222-2222-2222-2222-222222222222','AIR', 41, 50,   0.7200),
  ('22222222-2222-2222-2222-222222222222','AIR', 51, 70,   0.7400),
  ('22222222-2222-2222-2222-222222222222','AIR', 71, NULL, 0.7500);
-- INTL_EXPRESS_EXPORT
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT',  0, 10,   0.5500),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 11, 30,   0.6500),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 31, 40,   0.7000),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 41, 50,   0.7100),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 51, 70,   0.7200),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_EXPORT', 71, NULL, 0.7400);
-- INTL_EXPRESS_IMPORT
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT',  0, 10,   0.4700),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 11, 30,   0.5300),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 31, 40,   0.5400),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 41, 50,   0.5500),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 51, 70,   0.5600),
  ('22222222-2222-2222-2222-222222222222','INTL_EXPRESS_IMPORT', 71, NULL, 0.5800);
-- INTL_STANDARD
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD',  0, 10,   0.2700),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 11, 30,   0.3200),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 31, 40,   0.3300),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 41, 50,   0.3400),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 51, 70,   0.3500),
  ('22222222-2222-2222-2222-222222222222','INTL_STANDARD', 71, NULL, 0.3700);

-- ---------------------------------------------------------
-- ACCOUNT + USER + PROFILE
-- ---------------------------------------------------------
INSERT INTO account (account_id, name, account_no) VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Demo Customer Inc.', 'ACCT-1001');

-- password_hash is a placeholder; replace with a real BCrypt hash in the app
INSERT INTO app_user (account_id, email, password_hash, role) VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'demo@customer.com', '{bcrypt-placeholder}', 'CUSTOMER');

INSERT INTO customer_profile (account_id, invoice_frequency, media, sort_option, language) VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'WEEKLY', 'PDF', 'SHIP_DATE', 'EN');

-- ---------------------------------------------------------
-- CONTRACT (demo account runs on the VOLUME_TIERED program)
-- ---------------------------------------------------------
INSERT INTO contract (contract_id, account_id, program_id, tier, fuel_program, payment_terms, effective_from) VALUES
  ('CTR-001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
   '22222222-2222-2222-2222-222222222222', 'STANDARD', 'FUEL_STANDARD', 'NET30', DATE '2026-01-01');

-- surcharge reductions on the contract
-- 40% off all 7 discountable surcharge codes (domestic + international)
INSERT INTO contract_incentive (contract_id, incentive_type, accessorial_code, value, unit) VALUES
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'RESIDENTIAL',             0.40, 'PCT'),
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'DELIVERY_AREA',           0.40, 'PCT'),
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'DELIVERY_AREA_EXT',       0.40, 'PCT'),
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'RESIDENTIAL_INTL',        0.40, 'PCT'),
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'DELIVERY_AREA_IMPORT',    0.40, 'PCT'),
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'DELIVERY_AREA_IMPORT_EXT',0.40, 'PCT'),
  ('CTR-001', 'ACCESSORIAL_REDUCE', 'DELIVERY_AREA_EXPORT_EXT',0.40, 'PCT');

-- ---------------------------------------------------------
-- SAMPLE SHIPMENTS (small baseline slice)
-- values are illustrative; the engine recomputes precisely
-- ---------------------------------------------------------
INSERT INTO shipment (account_id, contract_id, tracking_number, ship_date, bill_week,
                      origin_zip, dest_zip, zone, service_code, actual_weight, billed_weight,
                      package_count, residential, published_charge, discount_amount, net_transport,
                      fuel_charge, accessorial_charge, total_charge) VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','CTR-001','1A0001', DATE '2026-06-16','2026-W25',
   '30301','60601',5,'GROUND', 8.0, 8.0, 1, TRUE, 24.60, 10.82, 13.78, 2.07, 3.15, 19.00),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','CTR-001','1A0002', DATE '2026-06-16','2026-W25',
   '30301','90210',8,'EXPRESS',12.0,12.0, 1, FALSE, 95.00, 62.70, 32.30, 4.85, 4.25, 41.40),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','CTR-001','1A0003', DATE '2026-06-17','2026-W25',
   '30301','75201',5,'GROUND', 3.0, 3.0, 1, FALSE, 15.80, 6.95, 8.85, 1.33, 0.00, 10.18);

-- itemized charge lines for shipment 1A0001 (shows published + net)
INSERT INTO shipment_charge (shipment_id, charge_type, published_amount, amount)
SELECT shipment_id, 'TRANSPORTATION', 24.60, 13.78 FROM shipment WHERE tracking_number = '1A0001';
INSERT INTO shipment_charge (shipment_id, charge_type, published_amount, amount)
SELECT shipment_id, 'INCENTIVE_CREDIT', NULL, -10.82 FROM shipment WHERE tracking_number = '1A0001';
INSERT INTO shipment_charge (shipment_id, charge_type, published_amount, amount)
SELECT shipment_id, 'FUEL', NULL, 2.07 FROM shipment WHERE tracking_number = '1A0001';
INSERT INTO shipment_charge (shipment_id, charge_type, published_amount, amount)
SELECT shipment_id, 'RESIDENTIAL', 5.25, 3.15 FROM shipment WHERE tracking_number = '1A0001';

-- ---------------------------------------------------------
-- BASELINE SNAPSHOT (frozen aggregate — drives tier lookups)
-- ---------------------------------------------------------
INSERT INTO baseline_snapshot (account_id, period_from, period_to, total_shipments,
                               avg_weekly_volume, total_cost, metrics_json) VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', DATE '2025-06-01', DATE '2026-05-31', 1040,
   20.0, 502000.00,
   '{"spendByService":{"GROUND":300000,"EXPRESS":178000,"THREE_DAY":24000},
     "spendByZone":{"2":40000,"5":300000,"8":162000},
     "avgWeightLb":9.4,"residentialPct":0.35}');

-- ---------------------------------------------------------
-- RAG KNOWLEDGE (invoice section meanings + charge explanations)
-- ---------------------------------------------------------
INSERT INTO knowledge_article (kind, key_code, business_reason, source_doc) VALUES
  ('INVOICE_SECTION','Transportation Charges','Base cost of moving the package.', 'invoice_template'),
  ('INVOICE_SECTION','Incentive Credit','Contractual discount applied to base transportation.', 'invoice_template'),
  ('INVOICE_SECTION','Fuel Surcharge','Adjustment based on the weekly fuel index.', 'invoice_template'),
  ('CHARGE_EXPLANATION','DEMAND','Applied during peak shipping volume periods.', 'surcharge_guide'),
  ('CHARGE_EXPLANATION','RESIDENTIAL','Applied for delivery to a residential address.', 'surcharge_guide'),
  ('CHARGE_EXPLANATION','DELIVERY_AREA','Applied for delivery to an extended/remote area.', 'surcharge_guide'),
  ('POLICY','MIN_SHIPPING_CHARGE','A package is billed the greater of its discounted net or the published minimum charge.', 'incentive_agreement'),
  ('POLICY','DISCOUNT_SCOPE','Incentives apply only to base transportation rates, not to surcharges (except those explicitly listed).', 'incentive_agreement');
