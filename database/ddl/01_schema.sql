-- =============================================================================
-- Billing Simulation Agent  —  DDL (schema definition)
-- Target: Supabase (PostgreSQL)
-- Generic hackathon sample. No brand-specific names.
--
-- NOTE: This script is NOT wired into the application. It lives outside
--       src/ so Spring Boot does not auto-run it. Run it manually via the
--       Supabase SQL Editor, or with psql using your project connection string:
--       psql "postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres" -f 01_schema.sql
-- =============================================================================

-- gen_random_uuid() is available by default on Supabase; this is harmless if kept.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- IDENTITY
-- =========================================================
CREATE TABLE account (
    account_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    account_no      VARCHAR(50) UNIQUE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    user_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',   -- CUSTOMER / ADMIN
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =========================================================
-- RATE REFERENCE DATA (deterministic engine inputs)
-- =========================================================

-- 5 discount categories from the pricing agreement.
-- Multiple service codes share one category (e.g. EXPRESS + TWO_DAY + EXPRESS_SAVER = AIR).
-- discount_tier references this, not individual service codes.
CREATE TABLE discount_category (
    category_code   VARCHAR(30) PRIMARY KEY,        -- AIR / GROUND / INTL_EXPRESS_EXPORT / INTL_EXPRESS_IMPORT / INTL_STANDARD
    display_name    VARCHAR(60) NOT NULL
);

CREATE TABLE service_level (
    service_code       VARCHAR(20) PRIMARY KEY,     -- GROUND, EXPRESS, TWO_DAY, THREE_DAY ...
    display_name       VARCHAR(60) NOT NULL,
    transit_days       INT,
    is_air             BOOLEAN NOT NULL DEFAULT FALSE,
    discount_category  VARCHAR(30) REFERENCES discount_category(category_code)  -- which of the 5 categories
);

-- origin/destination zip prefixes -> zone
CREATE TABLE zone_matrix (
    id              BIGSERIAL PRIMARY KEY,
    origin_prefix   VARCHAR(5) NOT NULL,
    dest_prefix     VARCHAR(5) NOT NULL,
    zone            SMALLINT NOT NULL,
    UNIQUE(origin_prefix, dest_prefix)
);

-- versioned base (published) rate card
CREATE TABLE rate_card (
    rate_card_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version         VARCHAR(20) NOT NULL,           -- e.g. '2026-01'
    service_code    VARCHAR(20) NOT NULL REFERENCES service_level(service_code),
    zone            SMALLINT NOT NULL,
    weight_from_lb  NUMERIC(8,2) NOT NULL,
    weight_to_lb    NUMERIC(8,2) NOT NULL,
    base_rate       NUMERIC(10,2) NOT NULL,         -- published/list price
    effective_from  DATE NOT NULL,
    effective_to    DATE,
    UNIQUE(version, service_code, zone, weight_from_lb)
);
CREATE INDEX idx_ratecard_lookup ON rate_card(service_code, zone, weight_from_lb, effective_from);

CREATE TABLE dim_factor (
    service_code    VARCHAR(20) PRIMARY KEY REFERENCES service_level(service_code),
    divisor         INT NOT NULL                    -- dimensional weight divisor
);

-- minimum shipping charge floor (net can never fall below this)
CREATE TABLE min_shipping_charge (
    id              BIGSERIAL PRIMARY KEY,
    service_code    VARCHAR(20) NOT NULL REFERENCES service_level(service_code),
    floor_zone      SMALLINT NOT NULL,              -- zone used for the floor price
    floor_weight_lb NUMERIC(6,2) NOT NULL DEFAULT 1,
    addl_incentive_pct NUMERIC(5,4) NOT NULL DEFAULT 0,  -- extra reduction on the floor
    UNIQUE(service_code)
);

-- =========================================================
-- FUEL (index driven)
-- =========================================================
CREATE TABLE fuel_program (
    fuel_program    VARCHAR(30) PRIMARY KEY,        -- FUEL_STANDARD
    index_basis     VARCHAR(20) NOT NULL,           -- FUEL_INDEX
    formula_note    TEXT
);

CREATE TABLE fuel_index (
    id              BIGSERIAL PRIMARY KEY,
    fuel_program    VARCHAR(30) NOT NULL REFERENCES fuel_program(fuel_program),
    week_code       VARCHAR(10) NOT NULL,           -- '2026-W25'
    fuel_index      NUMERIC(6,3) NOT NULL,
    fuel_pct        NUMERIC(5,2) NOT NULL,          -- surcharge % applied to transportation
    effective_from  DATE NOT NULL,
    UNIQUE(fuel_program, week_code)
);

-- =========================================================
-- ACCESSORIALS (rule cards)
-- =========================================================
CREATE TABLE accessorial_type (
    code            VARCHAR(30) PRIMARY KEY,        -- RESIDENTIAL, DELIVERY_AREA, DEMAND ...
    display_name    VARCHAR(80) NOT NULL,
    default_fee     NUMERIC(10,2) NOT NULL,
    trigger_rule    VARCHAR(200),                   -- 'DeliveryType=Residential'
    apply_basis     VARCHAR(15) NOT NULL DEFAULT 'PER_PACKAGE'  -- PER_PACKAGE / PER_SHIPMENT
);

-- =========================================================
-- PRICING PROGRAMS (FLAT vs VOLUME_TIERED)
-- =========================================================
CREATE TABLE pricing_program (
    program_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(60) NOT NULL,
    type            VARCHAR(15) NOT NULL            -- FLAT | VOLUME_TIERED
);

-- one row per (program, discount category, volume band). FLAT = single band 0..NULL.
-- Engine lookup: resolve service_code -> discount_category, then pick the matching tier row.
CREATE TABLE discount_tier (
    id              BIGSERIAL PRIMARY KEY,
    program_id      UUID NOT NULL REFERENCES pricing_program(program_id),
    category_code   VARCHAR(30) NOT NULL REFERENCES discount_category(category_code),
    vol_min         INT NOT NULL DEFAULT 0,         -- avg weekly volume lower bound
    vol_max         INT,                            -- NULL = open ended (e.g. 71+)
    discount_pct    NUMERIC(5,4) NOT NULL,          -- 0.4400 = 44%
    UNIQUE(program_id, category_code, vol_min)
);

-- =========================================================
-- CONTRACT (consumed from pricing system; read-only inputs)
-- =========================================================
CREATE TABLE contract (
    contract_id          VARCHAR(40) PRIMARY KEY,    -- 'CTR-001'
    account_id           UUID NOT NULL REFERENCES account(account_id),
    program_id           UUID NOT NULL REFERENCES pricing_program(program_id),
    tier                 VARCHAR(30),
    fuel_program         VARCHAR(30) NOT NULL REFERENCES fuel_program(fuel_program),
    payment_terms        VARCHAR(20),                -- NET30
    late_payment_fee_pct NUMERIC(5,4) NOT NULL DEFAULT 0.0100,  -- 1% per month on overdue balance
    effective_from       DATE NOT NULL,
    effective_to         DATE,
    source_system        VARCHAR(40) DEFAULT 'PRICING_SYSTEM'
);

-- surcharge reductions / fuel caps / flat credits negotiated on the contract
CREATE TABLE contract_incentive (
    id               BIGSERIAL PRIMARY KEY,
    contract_id      VARCHAR(40) NOT NULL REFERENCES contract(contract_id),
    incentive_type   VARCHAR(30) NOT NULL,          -- ACCESSORIAL_REDUCE / FUEL_CAP / FLAT_CREDIT
    accessorial_code VARCHAR(30) REFERENCES accessorial_type(code),
    value            NUMERIC(10,4) NOT NULL,        -- 0.40 = 40% off residential
    unit             VARCHAR(10) NOT NULL           -- PCT / USD
);

-- =========================================================
-- PAYMENT PLANS (consolidated / special)
-- =========================================================
CREATE TABLE payment_plan (
    plan_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    plan_type       VARCHAR(15) NOT NULL            -- CONSOLIDATED / SPECIAL
);
CREATE TABLE payment_plan_account (
    id              BIGSERIAL PRIMARY KEY,
    plan_id         UUID NOT NULL REFERENCES payment_plan(plan_id),
    member_account_no VARCHAR(50) NOT NULL
);

-- =========================================================
-- CUSTOMER PROFILE (invoice rendering preferences)
-- =========================================================
CREATE TABLE customer_profile (
    account_id      UUID PRIMARY KEY REFERENCES account(account_id),
    invoice_frequency VARCHAR(15),                  -- WEEKLY / MONTHLY
    media           VARCHAR(10),                    -- PDF / CSV / XML
    sort_option     VARCHAR(30),
    language        VARCHAR(5) DEFAULT 'EN'
);

-- =========================================================
-- TRANSACTIONAL BASELINE (12 months of shipments)
-- =========================================================
CREATE TABLE shipment (
    shipment_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    contract_id     VARCHAR(40) REFERENCES contract(contract_id),
    tracking_number VARCHAR(30),
    ship_date       DATE NOT NULL,
    bill_week       VARCHAR(10),                    -- '2026-W25' (which week billed)
    origin_zip      VARCHAR(10) NOT NULL,
    dest_zip        VARCHAR(10) NOT NULL,
    zone            SMALLINT NOT NULL,
    service_code    VARCHAR(20) NOT NULL REFERENCES service_level(service_code),
    actual_weight   NUMERIC(8,2) NOT NULL,
    length_in       NUMERIC(6,2),
    width_in        NUMERIC(6,2),
    height_in       NUMERIC(6,2),
    billed_weight   NUMERIC(8,2) NOT NULL,          -- max(actual, dim)
    package_count   INT NOT NULL DEFAULT 1,
    residential     BOOLEAN NOT NULL DEFAULT FALSE,
    declared_value  NUMERIC(10,2),
    published_charge NUMERIC(10,2) NOT NULL,        -- before discount
    discount_amount  NUMERIC(10,2) NOT NULL DEFAULT 0,
    net_transport    NUMERIC(10,2) NOT NULL,        -- after discount, >= min charge
    fuel_charge      NUMERIC(10,2) NOT NULL DEFAULT 0,
    accessorial_charge NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_charge     NUMERIC(10,2) NOT NULL         -- invoiced baseline
);
CREATE INDEX idx_shipment_baseline ON shipment(account_id, ship_date);
CREATE INDEX idx_shipment_service  ON shipment(account_id, service_code);

-- itemized charge lines per shipment (mirrors invoice detail)
CREATE TABLE shipment_charge (
    id              BIGSERIAL PRIMARY KEY,
    shipment_id     UUID NOT NULL REFERENCES shipment(shipment_id),
    charge_type     VARCHAR(40) NOT NULL,           -- TRANSPORTATION / FUEL / RESIDENTIAL / INCENTIVE_CREDIT
    published_amount NUMERIC(10,2),                 -- list value (nullable for pure surcharges)
    amount          NUMERIC(10,2) NOT NULL          -- net value on the invoice
);

-- cached aggregate baseline (cost control / fast what-if)
CREATE TABLE baseline_snapshot (
    baseline_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    period_from     DATE NOT NULL,
    period_to       DATE NOT NULL,
    total_shipments INT NOT NULL,
    avg_weekly_volume NUMERIC(8,2) NOT NULL,        -- drives tier lookup
    total_cost      NUMERIC(14,2) NOT NULL,
    metrics_json    JSONB NOT NULL,                 -- spend by service/zone/weight band
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =========================================================
-- CHAT + SIMULATION AUDIT
-- =========================================================
CREATE TABLE conversation (
    conversation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    user_id         UUID NOT NULL REFERENCES app_user(user_id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_message (
    message_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversation(conversation_id),
    role            VARCHAR(15) NOT NULL,           -- USER / ASSISTANT
    content         TEXT NOT NULL,
    intent          VARCHAR(40),                    -- VOLUME_CHANGE, SERVICE_SHIFT, FACTUAL ...
    is_complete     BOOLEAN,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE simulation_scenario (
    scenario_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    conversation_id UUID REFERENCES conversation(conversation_id),
    baseline_id     UUID NOT NULL REFERENCES baseline_snapshot(baseline_id),
    scenario_type   VARCHAR(40) NOT NULL,           -- matches simulate endpoint
    input_params    JSONB NOT NULL,                 -- validated params / assumptions
    rate_card_version VARCHAR(20) NOT NULL,
    baseline_cost   NUMERIC(14,2) NOT NULL,
    projected_cost  NUMERIC(14,2) NOT NULL,
    delta_pct       NUMERIC(6,2) NOT NULL,
    result_json     JSONB NOT NULL,
    confidence      VARCHAR(10) NOT NULL,           -- HIGH / MEDIUM / LOW
    caveats         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_scenario_account ON simulation_scenario(account_id, created_at);

-- =========================================================
-- INVOICE (actual + projected)
-- =========================================================
CREATE TABLE invoice (
    invoice_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    scenario_id     UUID REFERENCES simulation_scenario(scenario_id), -- NULL = actual
    period_from     DATE NOT NULL,
    period_to       DATE NOT NULL,
    transportation  NUMERIC(14,2) NOT NULL,
    fuel            NUMERIC(14,2) NOT NULL,
    accessorial     NUMERIC(14,2) NOT NULL,
    incentive_credit NUMERIC(14,2) NOT NULL DEFAULT 0,
    total           NUMERIC(14,2) NOT NULL,
    is_projection   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE invoice_line (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      UUID NOT NULL REFERENCES invoice(invoice_id),
    section         VARCHAR(40) NOT NULL,           -- Transportation / Fuel / Residential ...
    description     VARCHAR(120),
    published_amount NUMERIC(12,2),
    amount          NUMERIC(12,2) NOT NULL
);

-- =========================================================
--***** RAG KNOWLEDGE (metadata; embeddings live in the vector store)
-- =========================================================
CREATE TABLE knowledge_article (
    article_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kind            VARCHAR(20) NOT NULL,           -- INVOICE_SECTION / CHARGE_EXPLANATION / POLICY
    key_code        VARCHAR(60) NOT NULL,           -- 'Fuel Surcharge' / 'DEMAND'
    business_reason TEXT NOT NULL,
    source_doc      VARCHAR(120),
    embedding_ref   VARCHAR(120)
);
