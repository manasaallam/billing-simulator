-- =============================================================================
-- Billing Simulation Agent — Optimized DDL Schema
-- Target: Supabase / Google Cloud SQL PostgreSQL (v13+)
-- =============================================================================

-- Enable the pgvector extension for RAG search capabilities
CREATE EXTENSION IF NOT EXISTS vector;

-- =========================================================
-- IDENTITY
-- =========================================================

-- Company/business/customer account.
CREATE TABLE company (
    company_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name    VARCHAR(200) NOT NULL,

    -- Used during signup to map personal email users to this company.
    -- Example: ACME2026, FAST2026, GLOBAL2026
    access_key      VARCHAR(50) UNIQUE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / INACTIVE

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Application login user.
-- auth_provider_uid stores a BCrypt hash for local auth, or an external OAuth UID
-- (GCP Identity / Firebase) for seamless future migration.
CREATE TABLE app_user (
    user_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES company(company_id),
    full_name         VARCHAR(150),
    email             VARCHAR(255) UNIQUE NOT NULL,
    auth_provider_uid VARCHAR(255), -- BCrypt hash (local) or OAuth UID (GCP/Firebase)
    role              VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER', -- CUSTOMER / ADMIN

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login        TIMESTAMPTZ
);

CREATE INDEX idx_app_user_company ON app_user(company_id);
CREATE INDEX idx_app_user_email   ON app_user(email);

-- =========================================================
-- RATE REFERENCE DATA (deterministic engine inputs)
-- =========================================================

-- 5 discount categories from the pricing agreement.
CREATE TABLE discount_category (
    category_code   VARCHAR(30) PRIMARY KEY,        -- AIR / GROUND / INTL_EXPRESS_EXPORT / INTL_EXPRESS_IMPORT / INTL_STANDARD
    display_name    VARCHAR(60) NOT NULL
);

CREATE TABLE service_level (
    service_code       VARCHAR(20) PRIMARY KEY,     -- GROUND, EXPRESS, TWO_DAY, THREE_DAY ...
    display_name       VARCHAR(60) NOT NULL,
    transit_days       INT,
    is_air             BOOLEAN NOT NULL DEFAULT FALSE,
    discount_category  VARCHAR(30) REFERENCES discount_category(category_code)
);

CREATE TABLE zone_matrix (
    id              BIGSERIAL PRIMARY KEY,
    origin_prefix   VARCHAR(5) NOT NULL,
    dest_prefix     VARCHAR(5) NOT NULL,
    zone            SMALLINT NOT NULL,
    UNIQUE(origin_prefix, dest_prefix)
);

CREATE TABLE rate_card (
    rate_card_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version         VARCHAR(20) NOT NULL,
    service_code    VARCHAR(20) NOT NULL REFERENCES service_level(service_code),
    zone            SMALLINT NOT NULL,
    weight_from_lb  NUMERIC(8,2) NOT NULL,
    weight_to_lb    NUMERIC(8,2) NOT NULL,
    base_rate       NUMERIC(10,2) NOT NULL,
    effective_from  DATE NOT NULL,
    effective_to    DATE,
    UNIQUE(version, service_code, zone, weight_from_lb)
);
CREATE INDEX idx_ratecard_lookup ON rate_card(service_code, zone, weight_from_lb, effective_from);

CREATE TABLE dim_factor (
    service_code    VARCHAR(20) PRIMARY KEY REFERENCES service_level(service_code),
    divisor         INT NOT NULL
);

CREATE TABLE min_shipping_charge (
    id                 BIGSERIAL PRIMARY KEY,
    service_code       VARCHAR(20) NOT NULL REFERENCES service_level(service_code),
    floor_zone         SMALLINT NOT NULL,
    floor_weight_lb    NUMERIC(6,2) NOT NULL DEFAULT 1,
    addl_incentive_pct NUMERIC(5,4) NOT NULL DEFAULT 0,
    UNIQUE(service_code)
);

-- =========================================================
-- FUEL
-- =========================================================

CREATE TABLE fuel_program (
    fuel_program    VARCHAR(30) PRIMARY KEY,
    index_basis     VARCHAR(20) NOT NULL,
    formula_note    TEXT
);

CREATE TABLE fuel_index (
    id              BIGSERIAL PRIMARY KEY,
    fuel_program    VARCHAR(30) NOT NULL REFERENCES fuel_program(fuel_program),
    week_code       VARCHAR(10) NOT NULL,
    fuel_index      NUMERIC(6,3) NOT NULL,
    fuel_pct        NUMERIC(5,2) NOT NULL,
    effective_from  DATE NOT NULL,
    UNIQUE(fuel_program, week_code)
);

-- =========================================================
-- ACCESSORIALS
-- =========================================================

CREATE TABLE accessorial_type (
    code            VARCHAR(30) PRIMARY KEY,
    display_name    VARCHAR(80) NOT NULL,
    default_fee     NUMERIC(10,2) NOT NULL,
    trigger_rule    VARCHAR(200),
    apply_basis     VARCHAR(15) NOT NULL DEFAULT 'PER_PACKAGE'
);

-- =========================================================
-- PRICING PROGRAMS
-- =========================================================

CREATE TABLE pricing_program (
    program_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(60) NOT NULL,
    type            VARCHAR(15) NOT NULL -- FLAT / VOLUME_TIERED
);

CREATE TABLE discount_tier (
    id              BIGSERIAL PRIMARY KEY,
    program_id      UUID NOT NULL REFERENCES pricing_program(program_id),
    category_code   VARCHAR(30) NOT NULL REFERENCES discount_category(category_code),
    vol_min         INT NOT NULL DEFAULT 0,
    vol_max         INT,
    discount_pct    NUMERIC(5,4) NOT NULL,
    UNIQUE(program_id, category_code, vol_min)
);

-- =========================================================
-- CONTRACT
-- =========================================================

CREATE TABLE contract (
    contract_id          VARCHAR(40) PRIMARY KEY,
    company_id           UUID NOT NULL REFERENCES company(company_id),
    program_id           UUID NOT NULL REFERENCES pricing_program(program_id),
    tier                 VARCHAR(30),
    fuel_program         VARCHAR(30) NOT NULL REFERENCES fuel_program(fuel_program),
    payment_terms        VARCHAR(20),
    late_payment_fee_pct NUMERIC(5,4) NOT NULL DEFAULT 0.0100,
    effective_from       DATE NOT NULL,
    effective_to         DATE,
    source_system        VARCHAR(40) DEFAULT 'PRICING_SYSTEM'
);
CREATE INDEX idx_contract_company ON contract(company_id, effective_from, effective_to);

CREATE TABLE contract_incentive (
    id               BIGSERIAL PRIMARY KEY,
    contract_id      VARCHAR(40) NOT NULL REFERENCES contract(contract_id),
    incentive_type   VARCHAR(30) NOT NULL,
    accessorial_code VARCHAR(30) REFERENCES accessorial_type(code),
    value            NUMERIC(10,4) NOT NULL,
    unit             VARCHAR(10) NOT NULL
);

-- =========================================================
-- PAYMENT PLANS
-- =========================================================

CREATE TABLE payment_plan (
    plan_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES company(company_id),
    plan_type       VARCHAR(15) NOT NULL -- CONSOLIDATED / SPECIAL
);

CREATE TABLE payment_plan_company (
    id                BIGSERIAL PRIMARY KEY,
    plan_id           UUID NOT NULL REFERENCES payment_plan(plan_id),
    member_company_id UUID NOT NULL REFERENCES company(company_id)
);

-- =========================================================
-- CUSTOMER PROFILE
-- =========================================================

CREATE TABLE customer_profile (
    company_id        UUID PRIMARY KEY REFERENCES company(company_id),
    invoice_frequency VARCHAR(15),
    media             VARCHAR(10),
    sort_option       VARCHAR(30),
    language          VARCHAR(5) DEFAULT 'EN',
    profile_summary   TEXT,
    generated_at      TIMESTAMPTZ
);

-- =========================================================
-- TRANSACTIONAL BASELINE — 12 MONTHS SHIPMENT DATA
-- =========================================================

CREATE TABLE shipment (
    shipment_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id         UUID NOT NULL REFERENCES company(company_id),
    contract_id        VARCHAR(40) REFERENCES contract(contract_id),
    tracking_number    VARCHAR(30),
    ship_date          DATE NOT NULL,
    bill_week          VARCHAR(10),
    origin_zip         VARCHAR(10) NOT NULL,
    dest_zip           VARCHAR(10) NOT NULL,
    zone               SMALLINT NOT NULL,
    service_code       VARCHAR(20) NOT NULL REFERENCES service_level(service_code),
    actual_weight      NUMERIC(8,2) NOT NULL,
    length_in          NUMERIC(6,2),
    width_in           NUMERIC(6,2),
    height_in          NUMERIC(6,2),
    billed_weight      NUMERIC(8,2) NOT NULL,
    package_count      INT NOT NULL DEFAULT 1,
    residential        BOOLEAN NOT NULL DEFAULT FALSE,
    declared_value     NUMERIC(10,2),
    published_charge   NUMERIC(10,2) NOT NULL,
    discount_amount    NUMERIC(10,2) NOT NULL DEFAULT 0,
    net_transport      NUMERIC(10,2) NOT NULL,
    fuel_charge        NUMERIC(10,2) NOT NULL DEFAULT 0,
    accessorial_charge NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_charge       NUMERIC(10,2) NOT NULL
);
CREATE INDEX idx_shipment_pricing_lookup ON shipment(company_id, service_code, ship_date);
CREATE INDEX idx_shipment_baseline       ON shipment(company_id, ship_date);
CREATE INDEX idx_shipment_company_zone   ON shipment(company_id, zone);

CREATE TABLE shipment_charge (
    id               BIGSERIAL PRIMARY KEY,
    shipment_id      UUID NOT NULL REFERENCES shipment(shipment_id),
    charge_type      VARCHAR(40) NOT NULL,
    published_amount NUMERIC(10,2),
    amount           NUMERIC(10,2) NOT NULL
);

-- =========================================================
-- BASELINE SNAPSHOT
-- =========================================================

CREATE TABLE baseline_snapshot (
    baseline_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES company(company_id),
    period_from       DATE NOT NULL,
    period_to         DATE NOT NULL,
    total_shipments   INT NOT NULL,
    avg_weekly_volume NUMERIC(8,2) NOT NULL,
    total_cost        NUMERIC(14,2) NOT NULL,
    metrics_json      JSONB NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_baseline_company_period ON baseline_snapshot(company_id, period_from, period_to);

-- =========================================================
-- CHAT + SIMULATION AUDIT
-- =========================================================

CREATE TABLE conversation (
    conversation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES company(company_id),
    user_id         UUID NOT NULL REFERENCES app_user(user_id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_conversation_company ON conversation(company_id, created_at);
CREATE INDEX idx_conversation_user    ON conversation(user_id, created_at);

CREATE TABLE chat_message (
    message_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversation(conversation_id),
    role            VARCHAR(15) NOT NULL,   -- USER / ASSISTANT
    content         TEXT NOT NULL,
    intent          VARCHAR(40),
    is_complete     BOOLEAN,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_message_conversation ON chat_message(conversation_id, created_at);

CREATE TABLE simulation_scenario (
    scenario_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES company(company_id),
    conversation_id   UUID REFERENCES conversation(conversation_id),
    baseline_id       UUID NOT NULL REFERENCES baseline_snapshot(baseline_id),
    scenario_type     VARCHAR(40) NOT NULL,
    input_params      JSONB NOT NULL,
    rate_card_version VARCHAR(20) NOT NULL,
    baseline_cost     NUMERIC(14,2) NOT NULL,
    projected_cost    NUMERIC(14,2) NOT NULL,
    delta_pct         NUMERIC(6,2) NOT NULL,
    result_json       JSONB NOT NULL,
    confidence        VARCHAR(10) NOT NULL,  -- HIGH / MEDIUM / LOW
    caveats           JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_scenario_company      ON simulation_scenario(company_id, created_at);
CREATE INDEX idx_scenario_conversation ON simulation_scenario(conversation_id);

-- =========================================================
-- INVOICE — ACTUAL + PROJECTED
-- =========================================================

CREATE TABLE invoice (
    invoice_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id       UUID NOT NULL REFERENCES company(company_id),
    scenario_id      UUID REFERENCES simulation_scenario(scenario_id),
    period_from      DATE NOT NULL,
    period_to        DATE NOT NULL,
    transportation   NUMERIC(14,2) NOT NULL,
    fuel             NUMERIC(14,2) NOT NULL,
    accessorial      NUMERIC(14,2) NOT NULL,
    incentive_credit NUMERIC(14,2) NOT NULL DEFAULT 0,
    total            NUMERIC(14,2) NOT NULL,
    is_projection    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_invoice_company_period ON invoice(company_id, period_from, period_to);

CREATE TABLE invoice_line (
    id               BIGSERIAL PRIMARY KEY,
    invoice_id       UUID NOT NULL REFERENCES invoice(invoice_id),
    section          VARCHAR(40) NOT NULL,
    description      VARCHAR(120),
    published_amount NUMERIC(12,2),
    amount           NUMERIC(12,2) NOT NULL
);

-- =========================================================
-- RAG KNOWLEDGE BASE WITH NATIVE VECTOR SUPPORT
-- =========================================================

-- company_id NULL = global/general rules; non-NULL = company-specific contract logic.
CREATE TABLE knowledge_article (
    article_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID REFERENCES company(company_id),
    kind            VARCHAR(20) NOT NULL,   -- INVOICE_SECTION / CHARGE_EXPLANATION / POLICY
    key_code        VARCHAR(60) NOT NULL,   -- 'Fuel Surcharge' / 'DEMAND'
    content         TEXT NOT NULL,
    embedding       vector(1536) NOT NULL,
    business_reason TEXT NOT NULL,
    source_doc      VARCHAR(120),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_knowledge_company ON knowledge_article(company_id);
CREATE INDEX idx_knowledge_kind    ON knowledge_article(kind, key_code);
CREATE INDEX idx_knowledge_article_embedding
    ON knowledge_article USING hnsw (embedding vector_cosine_ops);
