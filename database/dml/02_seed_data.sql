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

-- ---------------------------------------------------------
-- FUEL PROGRAM + WEEKLY INDEX
-- ---------------------------------------------------------
INSERT INTO fuel_program (fuel_program, index_basis, formula_note) VALUES
  ('FUEL_STANDARD', 'FUEL_INDEX', 'Fuel % derived from weekly fuel index band.');

INSERT INTO fuel_index (fuel_program, week_code, fuel_index, fuel_pct, effective_from) VALUES
  -- July 2025
  ('FUEL_STANDARD', '2025-W27', 5.520, 16.50, DATE '2025-06-30'),
  ('FUEL_STANDARD', '2025-W28', 5.490, 16.25, DATE '2025-07-07'),
  ('FUEL_STANDARD', '2025-W30', 5.460, 16.00, DATE '2025-07-21'),
  -- August 2025
  ('FUEL_STANDARD', '2025-W32', 5.440, 16.00, DATE '2025-08-04'),
  ('FUEL_STANDARD', '2025-W33', 5.410, 15.75, DATE '2025-08-11'),
  ('FUEL_STANDARD', '2025-W35', 5.380, 15.50, DATE '2025-08-25'),
  -- September 2025
  ('FUEL_STANDARD', '2025-W36', 5.350, 15.50, DATE '2025-09-01'),
  ('FUEL_STANDARD', '2025-W38', 5.300, 15.25, DATE '2025-09-15'),
  ('FUEL_STANDARD', '2025-W39', 5.260, 15.00, DATE '2025-09-22'),
  -- October 2025
  ('FUEL_STANDARD', '2025-W41', 5.200, 14.75, DATE '2025-10-06'),
  ('FUEL_STANDARD', '2025-W42', 5.150, 14.50, DATE '2025-10-13'),
  ('FUEL_STANDARD', '2025-W43', 5.100, 14.25, DATE '2025-10-20'),
  -- November 2025
  ('FUEL_STANDARD', '2025-W45', 5.030, 14.00, DATE '2025-11-03'),
  ('FUEL_STANDARD', '2025-W46', 4.980, 13.75, DATE '2025-11-10'),
  ('FUEL_STANDARD', '2025-W47', 4.940, 13.50, DATE '2025-11-17'),
  -- December 2025
  ('FUEL_STANDARD', '2025-W49', 4.890, 13.25, DATE '2025-12-01'),
  ('FUEL_STANDARD', '2025-W50', 4.850, 13.00, DATE '2025-12-08'),
  ('FUEL_STANDARD', '2025-W51', 4.820, 12.75, DATE '2025-12-15'),
  -- January 2026
  ('FUEL_STANDARD', '2026-W01', 4.790, 12.75, DATE '2025-12-29'),
  ('FUEL_STANDARD', '2026-W02', 4.760, 12.50, DATE '2026-01-05'),
  ('FUEL_STANDARD', '2026-W04', 4.730, 12.25, DATE '2026-01-19'),
  -- February 2026
  ('FUEL_STANDARD', '2026-W06', 4.710, 12.00, DATE '2026-02-02'),
  ('FUEL_STANDARD', '2026-W07', 4.730, 12.25, DATE '2026-02-09'),
  ('FUEL_STANDARD', '2026-W08', 4.760, 12.50, DATE '2026-02-16'),
  -- March 2026
  ('FUEL_STANDARD', '2026-W10', 4.790, 12.75, DATE '2026-03-02'),
  ('FUEL_STANDARD', '2026-W11', 4.820, 13.00, DATE '2026-03-09'),
  ('FUEL_STANDARD', '2026-W13', 4.870, 13.25, DATE '2026-03-23'),
  -- April 2026
  ('FUEL_STANDARD', '2026-W14', 4.910, 13.50, DATE '2026-03-30'),
  ('FUEL_STANDARD', '2026-W16', 4.950, 13.75, DATE '2026-04-13'),
  ('FUEL_STANDARD', '2026-W17', 4.980, 14.00, DATE '2026-04-20'),
  -- May 2026
  ('FUEL_STANDARD', '2026-W18', 5.000, 14.00, DATE '2026-04-27'),
  ('FUEL_STANDARD', '2026-W19', 5.020, 14.00, DATE '2026-05-04'),
  ('FUEL_STANDARD', '2026-W21', 5.050, 14.00, DATE '2026-05-18'),
  -- June 2026
  ('FUEL_STANDARD', '2026-W22', 5.010, 14.00, DATE '2026-05-25'),
  ('FUEL_STANDARD', '2026-W23', 5.120, 14.50, DATE '2026-06-01'),
  ('FUEL_STANDARD', '2026-W24', 5.180, 14.75, DATE '2026-06-08'),
  ('FUEL_STANDARD', '2026-W25', 5.210, 15.00, DATE '2026-06-15'),
  -- July 2026
  ('FUEL_STANDARD', '2026-W26', 5.230, 15.00, DATE '2026-06-22'),
  ('FUEL_STANDARD', '2026-W27', 5.240, 15.25, DATE '2026-06-29'),
  ('FUEL_STANDARD', '2026-W28', 5.250, 15.25, DATE '2026-07-07');

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
  ('a1b2c3d4-e5f6-4a7b-8c9d-e0f1a2b3c4d5', 'Everyday Savings (Flat)',   'FLAT'),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c', 'Save as You Grow (Tiered)', 'VOLUME_TIERED');

-- FLAT program discounts (single band 0..NULL) — all 5 service categories
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('a1b2c3d4-e5f6-4a7b-8c9d-e0f1a2b3c4d5','GROUND',              0, NULL, 0.3900),
  ('a1b2c3d4-e5f6-4a7b-8c9d-e0f1a2b3c4d5','AIR',                 0, NULL, 0.6250),
  ('a1b2c3d4-e5f6-4a7b-8c9d-e0f1a2b3c4d5','INTL_EXPRESS_EXPORT', 0, NULL, 0.5000),
  ('a1b2c3d4-e5f6-4a7b-8c9d-e0f1a2b3c4d5','INTL_EXPRESS_IMPORT', 0, NULL, 0.4000),
  ('a1b2c3d4-e5f6-4a7b-8c9d-e0f1a2b3c4d5','INTL_STANDARD',       0, NULL, 0.3000);

-- VOLUME_TIERED program discounts — all 5 categories, all 6 volume bands
-- Engine lookup: service_code -> discount_category -> pick row matching avg_weekly_volume
-- GROUND (GROUND, GROUND_RES, THREE_DAY all use this)
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','GROUND',  0, 10,   0.3600),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','GROUND', 11, 30,   0.4400),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','GROUND', 31, 40,   0.4600),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','GROUND', 41, 50,   0.5000),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','GROUND', 51, 70,   0.5100),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','GROUND', 71, NULL, 0.5200);
-- AIR (EXPRESS, EXPRESS_SAVER, TWO_DAY all use this — one set of tiers, not three)
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','AIR',  0, 10,   0.6000),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','AIR', 11, 30,   0.6600),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','AIR', 31, 40,   0.6800),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','AIR', 41, 50,   0.7200),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','AIR', 51, 70,   0.7400),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','AIR', 71, NULL, 0.7500);
-- INTL_EXPRESS_EXPORT
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_EXPORT',  0, 10,   0.5500),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_EXPORT', 11, 30,   0.6500),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_EXPORT', 31, 40,   0.7000),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_EXPORT', 41, 50,   0.7100),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_EXPORT', 51, 70,   0.7200),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_EXPORT', 71, NULL, 0.7400);
-- INTL_EXPRESS_IMPORT
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_IMPORT',  0, 10,   0.4700),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_IMPORT', 11, 30,   0.5300),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_IMPORT', 31, 40,   0.5400),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_IMPORT', 41, 50,   0.5500),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_IMPORT', 51, 70,   0.5600),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_EXPRESS_IMPORT', 71, NULL, 0.5800);
-- INTL_STANDARD
INSERT INTO discount_tier (program_id, category_code, vol_min, vol_max, discount_pct) VALUES
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_STANDARD',  0, 10,   0.2700),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_STANDARD', 11, 30,   0.3200),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_STANDARD', 31, 40,   0.3300),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_STANDARD', 41, 50,   0.3400),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_STANDARD', 51, 70,   0.3500),
  ('f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c','INTL_STANDARD', 71, NULL, 0.3700);

-- ---------------------------------------------------------
-- ACCOUNT + USER + PROFILE
-- ---------------------------------------------------------
INSERT INTO company (company_id, company_name, access_key, status) VALUES
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f', 'Demo Customer Inc.', 'DEMO2026', 'ACTIVE');

-- auth_provider_uid holds a BCrypt hash (local auth) or an OAuth provider UID; placeholder here
INSERT INTO app_user (company_id, full_name, email, auth_provider_uid, role) VALUES
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f', 'Demo User', 'demo@customer.com', '{bcrypt-placeholder}', 'CUSTOMER'),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f', 'Diya',      'diya@customer.com', '{bcrypt-placeholder}', 'CUSTOMER');

INSERT INTO customer_profile (company_id, invoice_frequency, media, sort_option, language) VALUES
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f', 'WEEKLY', 'PDF', 'SHIP_DATE', 'EN');

-- ---------------------------------------------------------
-- CONTRACT (demo account runs on the VOLUME_TIERED program)
-- ---------------------------------------------------------
INSERT INTO contract (contract_id, company_id, program_id, tier, fuel_program, payment_terms, late_payment_fee_pct, effective_from) VALUES
  ('CTR-001', 'c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f',
   'f6e5d4c3-b2a1-4f9e-8d7c-6b5a4f3e2d1c', 'STANDARD', 'FUEL_STANDARD', 'NET30', 0.0100, DATE '2026-01-01');

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
INSERT INTO shipment (company_id, contract_id, tracking_number, ship_date, bill_week,
                      origin_zip, dest_zip, zone, service_code, actual_weight, billed_weight,
                      package_count, residential, published_charge, discount_amount, net_transport,
                      fuel_charge, accessorial_charge, total_charge) VALUES
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','1A0001', DATE '2026-06-16','2026-W25',
   '30301','60601',5,'GROUND', 8.0, 8.0, 1, TRUE, 24.60, 10.82, 13.78, 2.07, 3.15, 19.00),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','1A0002', DATE '2026-06-16','2026-W25',
   '30301','90210',8,'EXPRESS',12.0,12.0, 1, FALSE, 95.00, 62.70, 32.30, 4.85, 4.25, 41.40),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','1A0003', DATE '2026-06-17','2026-W25',
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
INSERT INTO baseline_snapshot (company_id, period_from, period_to, total_shipments,
                               avg_weekly_volume, total_cost, metrics_json) VALUES
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f', DATE '2025-06-01', DATE '2026-05-31', 1040,
   20.0, 502000.00,
   '{"spendByService":{"GROUND":300000,"EXPRESS":178000,"THREE_DAY":24000},
     "spendByZone":{"2":40000,"5":300000,"8":162000},
     "avgWeightLb":9.4,"residentialPct":0.35}');

-- =========================================================
-- PATCH: International import charge types (added 2026-07-11)
-- Source: International Package Services invoice sample
-- Run in Supabase SQL Editor if seed data is already loaded.
-- =========================================================
INSERT INTO accessorial_type (code, display_name, default_fee, trigger_rule, apply_basis) VALUES
  ('DUTY',           'Customs Duty',                  0.00, 'ImportShipment=true',  'PER_SHIPMENT'),
  ('VAT',            'Value Added Tax',               0.00, 'ImportShipment=true',  'PER_SHIPMENT'),
  ('BROKERAGE_FEE',  'Third Party Disbursement Fee',  0.00, 'BrokerRequired=true',  'PER_SHIPMENT'),
  ('TRAILER_PICKUP', 'Trailer Pickup Adjustment',     2.50, 'PickupType=Trailer',   'PER_SHIPMENT');

-- =========================================================
-- PATCH: Additional last-year (2025) baseline data (added 2026-07-14)
-- Adds more zones, more services, and a wider spread of shipment
-- dates across Jul–Dec 2025. Illustrative values; engine recomputes.
-- Run in Supabase SQL Editor if seed data is already loaded.
-- =========================================================

-- ---------------------------------------------------------
-- ZONE MATRIX — additional origin/dest prefixes and zones (4, 6, 7)
-- ---------------------------------------------------------
INSERT INTO zone_matrix (origin_prefix, dest_prefix, zone) VALUES
  ('30', '10', 7),   -- ATL -> NY
  ('30', '20', 6),   -- ATL -> DC
  ('30', '33', 4),   -- ATL -> FL
  ('30', '80', 6),   -- ATL -> CO
  ('30', '98', 8),   -- ATL -> WA
  ('60', '30', 5),   -- CHI -> ATL
  ('60', '90', 7),   -- CHI -> LA
  ('75', '10', 6),   -- DAL -> NY
  ('90', '30', 8),   -- LA  -> ATL
  ('10', '90', 8);   -- NY  -> LA

-- ---------------------------------------------------------
-- RATE CARD — last-year published rates
-- version '2025-01', effective from 2025-01-01
-- Covers the services/zones used by the 2025 shipments below.
-- ---------------------------------------------------------
INSERT INTO rate_card (version, service_code, zone, weight_from_lb, weight_to_lb, base_rate, effective_from) VALUES
  -- GROUND
  ('2025-01','GROUND',       2,  0,  1,  9.00, DATE '2025-01-01'),
  ('2025-01','GROUND',       2,  1,  5, 10.80, DATE '2025-01-01'),
  ('2025-01','GROUND',       2,  5, 20, 23.50, DATE '2025-01-01'),
  ('2025-01','GROUND',       3,  1,  5, 13.40, DATE '2025-01-01'),
  ('2025-01','GROUND',       3,  5, 20, 26.00, DATE '2025-01-01'),
  ('2025-01','GROUND',       4,  1,  5, 15.20, DATE '2025-01-01'),
  ('2025-01','GROUND',       5,  1,  5, 15.20, DATE '2025-01-01'),
  ('2025-01','GROUND',       5,  5, 20, 23.80, DATE '2025-01-01'),
  ('2025-01','GROUND',       7,  1,  5, 18.50, DATE '2025-01-01'),
  ('2025-01','GROUND',       7,  5, 20, 29.00, DATE '2025-01-01'),
  ('2025-01','GROUND',       7, 20, 50, 33.80, DATE '2025-01-01'),
  -- GROUND_RES
  ('2025-01','GROUND_RES',   4,  1,  5, 16.20, DATE '2025-01-01'),
  ('2025-01','GROUND_RES',   5,  5, 20, 24.60, DATE '2025-01-01'),
  ('2025-01','GROUND_RES',   6,  5, 20, 22.40, DATE '2025-01-01'),
  ('2025-01','GROUND_RES',   8,  5, 20, 31.40, DATE '2025-01-01'),
  -- THREE_DAY
  ('2025-01','THREE_DAY',    3,  1,  5, 22.00, DATE '2025-01-01'),
  ('2025-01','THREE_DAY',    5,  1,  5, 22.00, DATE '2025-01-01'),
  ('2025-01','THREE_DAY',    5,  5, 20, 34.00, DATE '2025-01-01'),
  ('2025-01','THREE_DAY',    6,  1,  5, 26.00, DATE '2025-01-01'),
  ('2025-01','THREE_DAY',    7,  5, 20, 30.00, DATE '2025-01-01'),
  -- TWO_DAY (air)
  ('2025-01','TWO_DAY',      5,  5, 20, 62.00, DATE '2025-01-01'),
  ('2025-01','TWO_DAY',      6,  1,  5, 52.00, DATE '2025-01-01'),
  ('2025-01','TWO_DAY',      6,  5, 20, 58.00, DATE '2025-01-01'),
  ('2025-01','TWO_DAY',      7,  5, 20, 68.00, DATE '2025-01-01'),
  -- EXPRESS_SAVER (air)
  ('2025-01','EXPRESS_SAVER',4,  1,  5, 66.00, DATE '2025-01-01'),
  ('2025-01','EXPRESS_SAVER',6,  1,  5, 70.00, DATE '2025-01-01'),
  ('2025-01','EXPRESS_SAVER',7,  5, 20, 82.00, DATE '2025-01-01'),
  ('2025-01','EXPRESS_SAVER',8,  5, 20, 88.00, DATE '2025-01-01'),
  -- EXPRESS (air)
  ('2025-01','EXPRESS',      5,  1,  5, 48.00, DATE '2025-01-01'),
  ('2025-01','EXPRESS',      5,  5, 20, 72.00, DATE '2025-01-01'),
  ('2025-01','EXPRESS',      7,  1,  5, 78.00, DATE '2025-01-01'),
  ('2025-01','EXPRESS',      8,  5, 20, 95.00, DATE '2025-01-01');

-- ---------------------------------------------------------
-- SAMPLE SHIPMENTS — last year (Jul–Dec 2025), varied zones & services
-- Ground category discount ~44% (tiered band 11–30), Air category ~66%.
-- net_transport = published_charge - discount_amount
-- total_charge  = net_transport + fuel_charge + accessorial_charge
-- ---------------------------------------------------------
INSERT INTO shipment (company_id, contract_id, tracking_number, ship_date, bill_week,
                      origin_zip, dest_zip, zone, service_code, actual_weight, billed_weight,
                      package_count, residential, published_charge, discount_amount, net_transport,
                      fuel_charge, accessorial_charge, total_charge) VALUES
  -- July 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0001', DATE '2025-07-01','2025-W27','30301','35244',3,'GROUND',       5.0,  5.0, 1, FALSE, 13.40,  5.90,  7.50, 1.24,  0.00,  8.74),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0002', DATE '2025-07-01','2025-W27','30301','33101',4,'GROUND_RES',  12.0, 12.0, 1, TRUE,  26.80, 11.79, 15.01, 2.48,  3.15, 20.64),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0003', DATE '2025-07-08','2025-W28','30301','10001',7,'EXPRESS',      4.0,  4.0, 1, FALSE, 78.00, 51.48, 26.52, 4.31,  0.00, 30.83),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0004', DATE '2025-07-08','2025-W28','30301','20500',6,'TWO_DAY',      9.0,  9.0, 1, FALSE, 58.00, 38.28, 19.72, 3.20,  0.00, 22.92),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0005', DATE '2025-07-21','2025-W30','30301','60601',5,'THREE_DAY',    6.0,  6.0, 1, FALSE, 24.00, 10.56, 13.44, 2.15,  0.00, 15.59),
  -- August 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0006', DATE '2025-08-04','2025-W32','30301','75201',5,'GROUND',       3.0,  3.0, 1, FALSE, 15.80,  6.95,  8.85, 1.42,  0.00, 10.27),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0007', DATE '2025-08-04','2025-W32','30301','98101',8,'EXPRESS_SAVER', 7.0,  7.0, 1, FALSE, 88.00, 58.08, 29.92, 4.79,  0.00, 34.71),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0008', DATE '2025-08-11','2025-W33','30301','90210',8,'EXPRESS',     15.0, 15.0, 1, TRUE,  95.00, 62.70, 32.30, 5.09,  3.15, 40.54),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0009', DATE '2025-08-11','2025-W33','30301','80202',6,'GROUND_RES',  10.0, 10.0, 1, TRUE,  22.40,  9.86, 12.54, 1.97,  3.15, 17.66),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0010', DATE '2025-08-25','2025-W35','30301','30302',2,'GROUND',       2.0,  2.0, 1, FALSE, 11.20,  4.93,  6.27, 0.97,  0.00,  7.24),
  -- September 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0011', DATE '2025-09-01','2025-W36','60601','30301',5,'TWO_DAY',      8.0,  8.0, 1, FALSE, 62.00, 40.92, 21.08, 3.27,  0.00, 24.35),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0012', DATE '2025-09-15','2025-W38','30301','10001',7,'THREE_DAY',   11.0, 11.0, 1, FALSE, 30.00, 13.20, 16.80, 2.56,  0.00, 19.36),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0013', DATE '2025-09-15','2025-W38','30301','33139',4,'GROUND',       4.0,  4.0, 1, TRUE,  16.20,  7.13,  9.07, 1.38,  3.15, 13.60),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0014', DATE '2025-09-22','2025-W39','30301','90045',8,'EXPRESS',     20.0, 20.0, 1, FALSE, 95.00, 62.70, 32.30, 4.85, 15.00, 52.15),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0015', DATE '2025-09-22','2025-W39','75201','10001',6,'EXPRESS_SAVER', 5.0,  5.0, 1, FALSE, 70.00, 46.20, 23.80, 3.57,  0.00, 27.37),
  -- October 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0016', DATE '2025-10-06','2025-W41','30301','60601',5,'GROUND',      18.0, 18.0, 1, FALSE, 24.60, 10.82, 13.78, 2.03,  0.00, 15.81),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0017', DATE '2025-10-06','2025-W41','30301','20500',6,'TWO_DAY',      6.0,  6.0, 1, FALSE, 52.00, 34.32, 17.68, 2.61,  0.00, 20.29),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0018', DATE '2025-10-13','2025-W42','30301','98101',8,'GROUND_RES',  14.0, 14.0, 1, TRUE,  31.40, 13.82, 17.58, 2.55,  3.15, 23.28),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0019', DATE '2025-10-13','2025-W42','30301','35201',3,'THREE_DAY',    3.0,  3.0, 1, FALSE, 22.00,  9.68, 12.32, 1.79,  0.00, 14.11),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0020', DATE '2025-10-20','2025-W43','90210','30301',8,'EXPRESS',      9.0,  9.0, 1, FALSE, 90.00, 59.40, 30.60, 4.36,  0.00, 34.96),
  -- November 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0021', DATE '2025-11-03','2025-W45','30301','75201',5,'GROUND',       7.0,  7.0, 1, FALSE, 20.00,  8.80, 11.20, 1.57,  0.00, 12.77),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0022', DATE '2025-11-03','2025-W45','30301','33101',4,'EXPRESS_SAVER', 4.0,  4.0, 1, TRUE,  66.00, 43.56, 22.44, 3.14,  3.15, 28.73),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0023', DATE '2025-11-10','2025-W46','30301','10001',7,'GROUND',      22.0, 22.0, 1, FALSE, 33.80, 14.87, 18.93, 2.60,  0.00, 21.53),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0024', DATE '2025-11-10','2025-W46','60601','90210',7,'TWO_DAY',     12.0, 12.0, 1, FALSE, 68.00, 44.88, 23.12, 3.18,  0.00, 26.30),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0025', DATE '2025-11-17','2025-W47','30301','80202',6,'THREE_DAY',    5.0,  5.0, 1, FALSE, 26.00, 11.44, 14.56, 1.97,  0.00, 16.53),
  -- December 2025 (peak season — DEMAND surcharge on select shipments)
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0026', DATE '2025-12-01','2025-W49','30301','30302',2,'GROUND',       1.0,  1.0, 1, FALSE,  9.50,  4.18,  5.32, 0.70,  0.00,  6.02),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0027', DATE '2025-12-08','2025-W50','30301','90210',8,'EXPRESS',     16.0, 16.0, 1, TRUE,  95.00, 62.70, 32.30, 4.20,  6.65, 43.15),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0028', DATE '2025-12-08','2025-W50','30301','60601',5,'GROUND_RES',   9.0,  9.0, 1, TRUE,  24.60, 10.82, 13.78, 1.79,  6.65, 22.22),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0029', DATE '2025-12-15','2025-W51','30301','10001',7,'EXPRESS_SAVER', 6.0,  6.0, 1, FALSE, 82.00, 54.12, 27.88, 3.55,  3.50, 34.93),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0030', DATE '2025-12-15','2025-W51','30301','20500',6,'THREE_DAY',    8.0,  8.0, 1, FALSE, 28.00, 12.32, 15.68, 2.00,  0.00, 17.68);

-- ---------------------------------------------------------
-- SAMPLE SHIPMENTS — current year (Jan–May 2026), varied zones & services
-- Fills the gap between the 2025 baseline and the Jun 2026 slice above.
-- net_transport = published_charge - discount_amount
-- total_charge  = net_transport + fuel_charge + accessorial_charge
-- ---------------------------------------------------------
INSERT INTO shipment (company_id, contract_id, tracking_number, ship_date, bill_week,
                      origin_zip, dest_zip, zone, service_code, actual_weight, billed_weight,
                      package_count, residential, published_charge, discount_amount, net_transport,
                      fuel_charge, accessorial_charge, total_charge) VALUES
  -- January 2026
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0031', DATE '2026-01-05','2026-W02','30301','60601',5,'GROUND',        6.0,  6.0, 1, FALSE, 15.80,  6.95,  8.85, 1.11,  0.00,  9.96),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0032', DATE '2026-01-19','2026-W04','30301','10001',7,'EXPRESS',       5.0,  5.0, 1, FALSE, 78.00, 51.48, 26.52, 3.25,  0.00, 29.77),
  -- February 2026
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0033', DATE '2026-02-02','2026-W06','30301','33101',4,'GROUND_RES',   11.0, 11.0, 1, TRUE,  26.80, 11.79, 15.01, 1.80,  3.15, 19.96),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0034', DATE '2026-02-09','2026-W07','30301','20500',6,'TWO_DAY',       8.0,  8.0, 1, FALSE, 58.00, 38.28, 19.72, 2.42,  0.00, 22.14),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0035', DATE '2026-02-16','2026-W08','30301','75201',5,'THREE_DAY',     4.0,  4.0, 1, FALSE, 22.00,  9.68, 12.32, 1.54,  0.00, 13.86),
  -- March 2026
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0036', DATE '2026-03-02','2026-W10','30301','98101',8,'EXPRESS_SAVER',  7.0,  7.0, 1, FALSE, 88.00, 58.08, 29.92, 3.81,  0.00, 33.73),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0037', DATE '2026-03-09','2026-W11','30301','35201',3,'GROUND',        3.0,  3.0, 1, FALSE, 13.40,  5.90,  7.50, 0.98,  0.00,  8.48),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0038', DATE '2026-03-23','2026-W13','60601','30301',5,'TWO_DAY',      10.0, 10.0, 1, FALSE, 62.00, 40.92, 21.08, 2.79,  0.00, 23.87),
  -- April 2026
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0039', DATE '2026-03-30','2026-W14','30301','90210',8,'EXPRESS',      18.0, 18.0, 1, TRUE,  95.00, 62.70, 32.30, 4.36,  3.15, 39.81),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0040', DATE '2026-04-13','2026-W16','30301','80202',6,'GROUND_RES',    9.0,  9.0, 1, TRUE,  22.40,  9.86, 12.54, 1.72,  3.15, 17.41),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0041', DATE '2026-04-20','2026-W17','30301','10001',7,'THREE_DAY',    12.0, 12.0, 1, FALSE, 30.00, 13.20, 16.80, 2.35,  0.00, 19.15),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0042', DATE '2026-04-27','2026-W18','75201','10001',6,'EXPRESS_SAVER',  5.0,  5.0, 1, FALSE, 70.00, 46.20, 23.80, 3.33,  0.00, 27.13),
  -- May 2026
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0043', DATE '2026-05-04','2026-W19','30301','30302',2,'GROUND',        2.0,  2.0, 1, FALSE, 11.20,  4.93,  6.27, 0.88,  0.00,  7.15),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0044', DATE '2026-05-18','2026-W21','30301','33139',4,'GROUND',        5.0,  5.0, 1, TRUE,  16.20,  7.13,  9.07, 1.27,  3.15, 13.49),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2A0045', DATE '2026-05-18','2026-W21','90210','30301',8,'EXPRESS',       9.0,  9.0, 1, FALSE, 90.00, 59.40, 30.60, 4.28,  0.00, 34.88);

-- ---------------------------------------------------------
-- SAMPLE SHIPMENTS — additional last-year (2025) volume, varied zones & services
-- Adds density to the Jul–Dec 2025 baseline window.
-- net_transport = published_charge - discount_amount
-- total_charge  = net_transport + fuel_charge + accessorial_charge
-- ---------------------------------------------------------
INSERT INTO shipment (company_id, contract_id, tracking_number, ship_date, bill_week,
                      origin_zip, dest_zip, zone, service_code, actual_weight, billed_weight,
                      package_count, residential, published_charge, discount_amount, net_transport,
                      fuel_charge, accessorial_charge, total_charge) VALUES
  -- July 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0001', DATE '2025-07-07','2025-W28','30301','30302',2,'GROUND',        4.0,  4.0, 1, FALSE, 11.20,  4.93,  6.27, 1.02,  0.00,  7.29),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0002', DATE '2025-07-21','2025-W30','30301','33101',4,'EXPRESS_SAVER',  6.0,  6.0, 1, FALSE, 66.00, 43.56, 22.44, 3.59,  0.00, 26.03),
  -- August 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0003', DATE '2025-08-04','2025-W32','30301','20500',6,'THREE_DAY',     7.0,  7.0, 1, FALSE, 26.00, 11.44, 14.56, 2.33,  0.00, 16.89),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0004', DATE '2025-08-25','2025-W35','60601','30301',5,'EXPRESS',      10.0, 10.0, 1, FALSE, 72.00, 47.52, 24.48, 3.79,  0.00, 28.27),
  -- September 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0005', DATE '2025-09-01','2025-W36','30301','98101',8,'GROUND',       16.0, 16.0, 1, FALSE, 31.40, 13.82, 17.58, 2.72,  0.00, 20.30),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0006', DATE '2025-09-15','2025-W38','30301','90210',8,'TWO_DAY',      12.0, 12.0, 1, TRUE,  68.00, 44.88, 23.12, 3.53,  3.15, 29.80),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0007', DATE '2025-09-22','2025-W39','30301','35201',3,'GROUND',        3.0,  3.0, 1, FALSE, 13.40,  5.90,  7.50, 1.13,  0.00,  8.63),
  -- October 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0008', DATE '2025-10-06','2025-W41','30301','10001',7,'EXPRESS_SAVER',  8.0,  8.0, 1, FALSE, 82.00, 54.12, 27.88, 4.11,  0.00, 31.99),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0009', DATE '2025-10-20','2025-W43','30301','80202',6,'GROUND_RES',   11.0, 11.0, 1, TRUE,  22.40,  9.86, 12.54, 1.79,  3.15, 17.48),
  -- November 2025
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0010', DATE '2025-11-03','2025-W45','90210','30301',8,'EXPRESS',      14.0, 14.0, 1, FALSE, 90.00, 59.40, 30.60, 4.28,  0.00, 34.88),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0011', DATE '2025-11-17','2025-W47','30301','75201',5,'THREE_DAY',     9.0,  9.0, 1, FALSE, 34.00, 14.96, 19.04, 2.57,  0.00, 21.61),
  -- December 2025 (peak season — DEMAND surcharge on select shipments)
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0012', DATE '2025-12-01','2025-W49','30301','60601',5,'GROUND',       20.0, 20.0, 1, FALSE, 24.60, 10.82, 13.78, 1.83,  0.00, 15.61),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0013', DATE '2025-12-08','2025-W50','75201','10001',6,'EXPRESS_SAVER',  5.0,  5.0, 1, FALSE, 70.00, 46.20, 23.80, 3.09,  3.50, 30.39),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0014', DATE '2025-12-15','2025-W51','30301','20500',6,'TWO_DAY',       7.0,  7.0, 1, FALSE, 58.00, 38.28, 19.72, 2.51,  3.50, 25.73),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0015', DATE '2025-12-15','2025-W51','30301','33139',4,'GROUND',        4.0,  4.0, 1, TRUE,  16.20,  7.13,  9.07, 1.16,  6.65, 16.88);

-- ---------------------------------------------------------
-- SAMPLE SHIPMENTS — additional mixed 2025 + 2026 volume, varied zones & services
-- net_transport = published_charge - discount_amount
-- total_charge  = net_transport + fuel_charge + accessorial_charge
-- ---------------------------------------------------------
INSERT INTO shipment (company_id, contract_id, tracking_number, ship_date, bill_week,
                      origin_zip, dest_zip, zone, service_code, actual_weight, billed_weight,
                      package_count, residential, published_charge, discount_amount, net_transport,
                      fuel_charge, accessorial_charge, total_charge) VALUES
  -- Additional 2025 (last year)
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0016', DATE '2025-08-11','2025-W33','30301','10001',7,'GROUND',        4.0,  4.0, 1, FALSE, 18.50,  8.14, 10.36, 1.63,  0.00, 11.99),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0017', DATE '2025-09-01','2025-W36','30301','20500',6,'EXPRESS_SAVER',  6.0,  6.0, 1, FALSE, 70.00, 46.20, 23.80, 3.69,  0.00, 27.49),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0018', DATE '2025-10-13','2025-W42','30301','90210',8,'GROUND',       12.0, 12.0, 1, FALSE, 31.40, 13.82, 17.58, 2.55,  0.00, 20.13),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0019', DATE '2025-11-10','2025-W46','30301','33101',4,'THREE_DAY',     5.0,  5.0, 1, FALSE, 22.00,  9.68, 12.32, 1.69,  0.00, 14.01),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0020', DATE '2025-12-01','2025-W49','60601','90210',7,'EXPRESS',      11.0, 11.0, 1, FALSE, 78.00, 51.48, 26.52, 3.51,  0.00, 30.03),
  -- Additional 2026 (current year)
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0021', DATE '2026-05-25','2026-W22','30301','60601',5,'GROUND',        7.0,  7.0, 1, FALSE, 20.00,  8.80, 11.20, 1.57,  0.00, 12.77),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0022', DATE '2026-06-01','2026-W23','30301','10001',7,'TWO_DAY',       9.0,  9.0, 1, FALSE, 68.00, 44.88, 23.12, 3.35,  0.00, 26.47),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0023', DATE '2026-06-08','2026-W24','30301','33139',4,'GROUND_RES',   10.0, 10.0, 1, TRUE,  16.20,  7.13,  9.07, 1.34,  3.15, 13.56),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0024', DATE '2026-06-15','2026-W25','30301','98101',8,'EXPRESS_SAVER',  6.0,  6.0, 1, FALSE, 88.00, 58.08, 29.92, 4.49,  0.00, 34.41),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0025', DATE '2026-06-22','2026-W26','30301','35201',3,'GROUND',        3.0,  3.0, 1, FALSE, 13.40,  5.90,  7.50, 1.13,  0.00,  8.63),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0026', DATE '2026-06-29','2026-W27','30301','80202',6,'THREE_DAY',     8.0,  8.0, 1, FALSE, 26.00, 11.44, 14.56, 2.22,  0.00, 16.78),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0027', DATE '2026-07-07','2026-W28','30301','90210',8,'EXPRESS',      15.0, 15.0, 1, TRUE,  95.00, 62.70, 32.30, 4.93,  3.15, 40.38),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0028', DATE '2026-07-07','2026-W28','30301','20500',6,'TWO_DAY',       5.0,  5.0, 1, FALSE, 52.00, 34.32, 17.68, 2.70,  0.00, 20.38),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0029', DATE '2026-06-15','2026-W25','75201','10001',6,'EXPRESS_SAVER',  7.0,  7.0, 1, FALSE, 82.00, 54.12, 27.88, 4.18,  0.00, 32.06),
  ('c9d8e7f6-a5b4-4c3d-9e8f-7a6b5c4d3e2f','CTR-001','2B0030', DATE '2026-06-08','2026-W24','30301','30302',2,'GROUND',        2.0,  2.0, 1, FALSE, 11.20,  4.93,  6.27, 0.92,  0.00,  7.19);

-- =========================================================
-- RAG KNOWLEDGE (knowledge_article)  —  RUN LAST
-- Requires the pgvector extension and the knowledge_article table
-- (see 01_schema.sql). content is NOT NULL; embedding is left NULL
-- and generated at runtime by the app.
-- The other seed inserts above do NOT depend on this section, so it
-- is placed at the end to keep them loadable even if pgvector/the
-- knowledge_article table is not yet available.
-- =========================================================

-- Invoice section meanings + domestic charge explanations
INSERT INTO knowledge_article (kind, key_code, content, business_reason, source_doc) VALUES
  ('INVOICE_SECTION','Transportation Charges','Base cost of moving the package.','Base cost of moving the package.', 'invoice_template'),
  ('INVOICE_SECTION','Incentive Credit','Contractual discount applied to base transportation.','Contractual discount applied to base transportation.', 'invoice_template'),
  ('INVOICE_SECTION','Fuel Surcharge','Adjustment based on the weekly fuel index.','Adjustment based on the weekly fuel index.', 'invoice_template'),
  ('CHARGE_EXPLANATION','DEMAND','Applied during peak shipping volume periods.','Applied during peak shipping volume periods.', 'surcharge_guide'),
  ('CHARGE_EXPLANATION','RESIDENTIAL','Applied for delivery to a residential address.','Applied for delivery to a residential address.', 'surcharge_guide'),
  ('CHARGE_EXPLANATION','DELIVERY_AREA','Applied for delivery to an extended/remote area.','Applied for delivery to an extended/remote area.', 'surcharge_guide'),
  ('POLICY','MIN_SHIPPING_CHARGE','A package is billed the greater of its discounted net or the published minimum charge.','A package is billed the greater of its discounted net or the published minimum charge.', 'incentive_agreement'),
  ('POLICY','DISCOUNT_SCOPE','Incentives apply only to base transportation rates, not to surcharges (except those explicitly listed).','Incentives apply only to base transportation rates, not to surcharges (except those explicitly listed).', 'incentive_agreement');

-- International import invoice sections + charge explanations + policies
INSERT INTO knowledge_article (kind, key_code, content, business_reason, source_doc) VALUES
  ('INVOICE_SECTION',   'Government Charges',         'Groups customs duty and VAT assessed on international import shipments by the importing country.',                                                         'Groups customs duty and VAT assessed on international import shipments by the importing country.',                                                         'intl_invoice_template'),
  ('INVOICE_SECTION',   'Brokerage Charges',          'Fees paid to the customs broker on your behalf to facilitate customs clearance.',                                                                          'Fees paid to the customs broker on your behalf to facilitate customs clearance.',                                                                          'intl_invoice_template'),
  ('INVOICE_SECTION',   'Shipping Charge Corrections','Adjustments applied after initial billing when the original service, weight, or zone is corrected. Common causes: non-corrugated packaging, zone errors.', 'Adjustments applied after initial billing when the original service, weight, or zone is corrected. Common causes: non-corrugated packaging, zone errors.', 'corrections_guide'),
  ('CHARGE_EXPLANATION','DUTY',                       'Customs duty imposed by the importing country based on the declared value and commodity type of the shipment.',                                             'Customs duty imposed by the importing country based on the declared value and commodity type of the shipment.',                                             'intl_surcharge_guide'),
  ('CHARGE_EXPLANATION','VAT',                        'Value Added Tax applied by the importing country on the goods being imported. Rate varies by country and commodity.',                                       'Value Added Tax applied by the importing country on the goods being imported. Rate varies by country and commodity.',                                       'intl_surcharge_guide'),
  ('CHARGE_EXPLANATION','BROKERAGE_FEE',              'Third-party disbursement fee paid to the customs broker for processing import documentation on your behalf.',                                               'Third-party disbursement fee paid to the customs broker for processing import documentation on your behalf.',                                               'intl_surcharge_guide'),
  ('CHARGE_EXPLANATION','TRAILER_PICKUP',             'Applied when a trailer pickup is arranged instead of a standard driver pickup.',                                                                            'Applied when a trailer pickup is arranged instead of a standard driver pickup.',                                                                            'surcharge_guide'),
  ('POLICY',            'ACH_PAYMENT_REQUIRED',       'For certain accounts, incentives are conditioned on payment by ACH transfer or credit/debit card. Paying by other methods may forfeit contract discounts.','For certain accounts, incentives are conditioned on payment by ACH transfer or credit/debit card. Paying by other methods may forfeit contract discounts.','carrier_agreement'),
  ('POLICY',            'UNDELIVERABLE_RETURN',       'Undeliverable packages are returned under the same service used for the original shipment. Original incentives apply to the return leg.',                   'Undeliverable packages are returned under the same service used for the original shipment. Original incentives apply to the return leg.',                   'carrier_agreement');
