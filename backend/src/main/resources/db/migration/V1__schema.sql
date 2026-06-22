-- =============================================================
-- V1: Full invoicing schema
-- =============================================================

CREATE SCHEMA IF NOT EXISTS invoicing;

-- Sequences
CREATE SEQUENCE invoicing.audit_log_id_seq
    INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE;

-- ─── Lookup / catalog tables ──────────────────────────────────

CREATE TABLE invoicing.audit_log (
    id bigint NOT NULL DEFAULT nextval('invoicing.audit_log_id_seq'),
    entity_name varchar(100) NOT NULL,
    entity_id varchar(26) NOT NULL,
    operation varchar(20) NOT NULL,
    old_values jsonb NULL,
    new_values jsonb NULL,
    changed_by varchar(100) NOT NULL,
    changed_at timestamptz NOT NULL,
    CONSTRAINT audit_log_pkey PRIMARY KEY (id)
);

CREATE TABLE invoicing.catalog_department (
    code varchar(10) NOT NULL,
    name varchar(100) NOT NULL,
    CONSTRAINT catalog_department_pkey PRIMARY KEY (code)
);

CREATE TABLE invoicing.catalog_municipality (
    code varchar(10) NOT NULL,
    department_code varchar(10) NOT NULL,
    name varchar(100) NOT NULL,
    CONSTRAINT catalog_municipality_pkey PRIMARY KEY (code),
    CONSTRAINT catalog_municipality_department_code_fkey
        FOREIGN KEY (department_code) REFERENCES invoicing.catalog_department(code)
);

CREATE TABLE invoicing.economic_activity (
    code varchar(20) NOT NULL,
    name varchar(255) NOT NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT economic_activity_pkey PRIMARY KEY (code)
);

CREATE TABLE invoicing.dte_document_type (
    code varchar(10) NOT NULL,
    name varchar(100) NOT NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT dte_document_type_pkey PRIMARY KEY (code)
);

CREATE TABLE invoicing.dte_event_type (
    code varchar(30) NOT NULL,
    name varchar(100) NOT NULL,
    CONSTRAINT dte_event_type_pkey PRIMARY KEY (code)
);

CREATE TABLE invoicing.payment_method (
    code varchar(20) NOT NULL,
    name varchar(100) NOT NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT payment_method_pkey PRIMARY KEY (code)
);

CREATE TABLE invoicing.tax_definition (
    code varchar(20) NOT NULL,
    name varchar(100) NOT NULL,
    rate numeric(8,4) NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT tax_definition_pkey PRIMARY KEY (code)
);

CREATE TABLE invoicing.relationship_type (
    code varchar(50) NOT NULL,
    name varchar(100) NOT NULL,
    description text NULL,
    CONSTRAINT relationship_type_pkey PRIMARY KEY (code)
);

CREATE TABLE invoicing.status_catalog (
    id varchar(26) NOT NULL,
    entity_type varchar(50) NOT NULL,
    code varchar(50) NOT NULL,
    name varchar(100) NOT NULL,
    description text NULL,
    is_initial bool DEFAULT false NOT NULL,
    is_final bool DEFAULT false NOT NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT status_catalog_entity_type_code_key UNIQUE (entity_type, code),
    CONSTRAINT status_catalog_pkey PRIMARY KEY (id)
);

CREATE TABLE invoicing.status_transition (
    id varchar(26) NOT NULL,
    entity_type varchar(50) NOT NULL,
    from_status_id varchar(26) NOT NULL,
    to_status_id varchar(26) NOT NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT status_transition_pkey PRIMARY KEY (id),
    CONSTRAINT status_transition_from_status_id_fkey
        FOREIGN KEY (from_status_id) REFERENCES invoicing.status_catalog(id),
    CONSTRAINT status_transition_to_status_id_fkey
        FOREIGN KEY (to_status_id) REFERENCES invoicing.status_catalog(id)
);

-- ─── App users (outside invoicing schema) ─────────────────────

CREATE TABLE IF NOT EXISTS public.app_user (
    id varchar(26) NOT NULL,
    username varchar(100) NOT NULL,
    password_hash varchar(255) NOT NULL,
    active bool DEFAULT true NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT app_user_pkey PRIMARY KEY (id),
    CONSTRAINT app_user_username_key UNIQUE (username)
);

-- ─── Company & structure ──────────────────────────────────────

CREATE TABLE invoicing.company (
    id varchar(26) NOT NULL,
    legal_name varchar(255) NOT NULL,
    trade_name varchar(255) NULL,
    nit varchar(20) NOT NULL,
    nrc varchar(20) NULL,
    economic_activity_code varchar(20) NULL,
    email varchar(255) NULL,
    phone varchar(50) NULL,
    active bool DEFAULT true NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(100) NOT NULL,
    updated_at timestamptz NULL,
    updated_by varchar(100) NULL,
    version int8 DEFAULT 0 NOT NULL,
    CONSTRAINT company_pkey PRIMARY KEY (id),
    CONSTRAINT company_nit_key UNIQUE (nit),
    CONSTRAINT company_economic_activity_code_fkey
        FOREIGN KEY (economic_activity_code) REFERENCES invoicing.economic_activity(code)
);

CREATE TABLE invoicing.certificate (
    id varchar(26) NOT NULL,
    company_id varchar(26) NOT NULL,
    alias varchar(100) NOT NULL,
    certificate_path text NOT NULL,
    valid_from timestamptz NOT NULL,
    valid_to timestamptz NOT NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT certificate_pkey PRIMARY KEY (id),
    CONSTRAINT certificate_company_id_fkey
        FOREIGN KEY (company_id) REFERENCES invoicing.company(id)
);

CREATE TABLE invoicing.company_branch (
    id varchar(26) NOT NULL,
    company_id varchar(26) NOT NULL,
    code varchar(20) NOT NULL,
    name varchar(255) NOT NULL,
    address_line1 varchar(255) NOT NULL,
    department_code varchar(10) NULL,
    municipality_code varchar(10) NULL,
    phone varchar(50) NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT company_branch_company_id_code_key UNIQUE (company_id, code),
    CONSTRAINT company_branch_pkey PRIMARY KEY (id),
    CONSTRAINT company_branch_company_id_fkey
        FOREIGN KEY (company_id) REFERENCES invoicing.company(id),
    CONSTRAINT company_branch_department_code_fkey
        FOREIGN KEY (department_code) REFERENCES invoicing.catalog_department(code),
    CONSTRAINT company_branch_municipality_code_fkey
        FOREIGN KEY (municipality_code) REFERENCES invoicing.catalog_municipality(code)
);

CREATE TABLE invoicing.point_of_sale (
    id varchar(26) NOT NULL,
    branch_id varchar(26) NOT NULL,
    code varchar(20) NOT NULL,
    name varchar(255) NOT NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT point_of_sale_branch_id_code_key UNIQUE (branch_id, code),
    CONSTRAINT point_of_sale_pkey PRIMARY KEY (id),
    CONSTRAINT point_of_sale_branch_id_fkey
        FOREIGN KEY (branch_id) REFERENCES invoicing.company_branch(id)
);

CREATE TABLE invoicing.invoice_sequence (
    id varchar(26) NOT NULL,
    point_of_sale_id varchar(26) NOT NULL,
    document_type_code varchar(10) NOT NULL,
    current_value int8 NOT NULL DEFAULT 0,
    CONSTRAINT invoice_sequence_pkey PRIMARY KEY (id),
    CONSTRAINT invoice_sequence_point_of_sale_id_document_type_code_key
        UNIQUE (point_of_sale_id, document_type_code),
    CONSTRAINT invoice_sequence_document_type_code_fkey
        FOREIGN KEY (document_type_code) REFERENCES invoicing.dte_document_type(code),
    CONSTRAINT invoice_sequence_point_of_sale_id_fkey
        FOREIGN KEY (point_of_sale_id) REFERENCES invoicing.point_of_sale(id)
);

-- ─── Customer ────────────────────────────────────────────────

CREATE TABLE invoicing.customer (
    id varchar(26) NOT NULL,
    customer_number varchar(30) NOT NULL,
    legal_name varchar(255) NOT NULL,
    trade_name varchar(255) NULL,
    email varchar(255) NULL,
    phone varchar(50) NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL,
    created_by varchar(100) NOT NULL,
    updated_at timestamptz NULL,
    updated_by varchar(100) NULL,
    version int8 DEFAULT 0 NOT NULL,
    CONSTRAINT customer_customer_number_key UNIQUE (customer_number),
    CONSTRAINT customer_pkey PRIMARY KEY (id)
);

CREATE TABLE invoicing.customer_address (
    id varchar(26) NOT NULL,
    customer_id varchar(26) NOT NULL,
    address_line1 varchar(255) NOT NULL,
    department_code varchar(10) NULL,
    municipality_code varchar(10) NULL,
    is_default bool DEFAULT false NOT NULL,
    CONSTRAINT customer_address_pkey PRIMARY KEY (id),
    CONSTRAINT customer_address_customer_id_fkey
        FOREIGN KEY (customer_id) REFERENCES invoicing.customer(id),
    CONSTRAINT customer_address_department_code_fkey
        FOREIGN KEY (department_code) REFERENCES invoicing.catalog_department(code),
    CONSTRAINT customer_address_municipality_code_fkey
        FOREIGN KEY (municipality_code) REFERENCES invoicing.catalog_municipality(code)
);

CREATE TABLE invoicing.customer_tax_profile (
    id varchar(26) NOT NULL,
    customer_id varchar(26) NOT NULL,
    document_type varchar(20) NOT NULL,
    document_number varchar(50) NOT NULL,
    nit varchar(20) NULL,
    nrc varchar(20) NULL,
    economic_activity_code varchar(20) NULL,
    CONSTRAINT customer_tax_profile_customer_id_key UNIQUE (customer_id),
    CONSTRAINT customer_tax_profile_pkey PRIMARY KEY (id),
    CONSTRAINT customer_tax_profile_customer_id_fkey
        FOREIGN KEY (customer_id) REFERENCES invoicing.customer(id),
    CONSTRAINT customer_tax_profile_economic_activity_code_fkey
        FOREIGN KEY (economic_activity_code) REFERENCES invoicing.economic_activity(code)
);

-- ─── Billable items ───────────────────────────────────────────

CREATE TABLE invoicing.billable_item (
    id varchar(26) NOT NULL,
    item_type varchar(20) NOT NULL,
    sku varchar(100) NULL,
    code varchar(100) NULL,
    name varchar(255) NOT NULL,
    description text NULL,
    active bool DEFAULT true NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(100) NOT NULL,
    updated_at timestamptz NULL,
    updated_by varchar(100) NULL,
    version int8 DEFAULT 0 NOT NULL,
    CONSTRAINT billable_item_pkey PRIMARY KEY (id)
);

CREATE TABLE invoicing.item_price (
    id varchar(26) NOT NULL,
    billable_item_id varchar(26) NOT NULL,
    currency_code char(3) DEFAULT 'USD' NOT NULL,
    unit_price numeric(18,4) NOT NULL,
    valid_from timestamptz NOT NULL,
    valid_to timestamptz NULL,
    active bool DEFAULT true NOT NULL,
    CONSTRAINT item_price_pkey PRIMARY KEY (id),
    CONSTRAINT item_price_billable_item_id_fkey
        FOREIGN KEY (billable_item_id) REFERENCES invoicing.billable_item(id)
);

-- ─── Orders ───────────────────────────────────────────────────

CREATE TABLE invoicing.customer_order (
    id varchar(26) NOT NULL,
    order_number varchar(50) NOT NULL,
    customer_id varchar(26) NOT NULL,
    order_date timestamptz NOT NULL,
    subtotal_amount numeric(18,4) NOT NULL,
    discount_amount numeric(18,4) NOT NULL DEFAULT 0,
    tax_amount numeric(18,4) NOT NULL DEFAULT 0,
    total_amount numeric(18,4) NOT NULL,
    current_status_id varchar(26) NULL,
    CONSTRAINT customer_order_discount_amount_check CHECK (discount_amount >= 0),
    CONSTRAINT customer_order_subtotal_amount_check CHECK (subtotal_amount >= 0),
    CONSTRAINT customer_order_tax_amount_check CHECK (tax_amount >= 0),
    CONSTRAINT customer_order_total_amount_check CHECK (total_amount >= 0),
    CONSTRAINT customer_order_order_number_key UNIQUE (order_number),
    CONSTRAINT customer_order_pkey PRIMARY KEY (id),
    CONSTRAINT customer_order_current_status_id_fkey
        FOREIGN KEY (current_status_id) REFERENCES invoicing.status_catalog(id),
    CONSTRAINT customer_order_customer_id_fkey
        FOREIGN KEY (customer_id) REFERENCES invoicing.customer(id)
);
CREATE INDEX idx_order_customer ON invoicing.customer_order USING btree (customer_id);

CREATE TABLE invoicing.order_line (
    id varchar(26) NOT NULL,
    order_id varchar(26) NOT NULL,
    billable_item_id varchar(26) NULL,
    description text NOT NULL,
    quantity numeric(18,4) NOT NULL,
    unit_price numeric(18,4) NOT NULL,
    subtotal_amount numeric(18,4) NOT NULL,
    discount_amount numeric(18,4) NOT NULL DEFAULT 0,
    tax_amount numeric(18,4) NOT NULL DEFAULT 0,
    total_amount numeric(18,4) NOT NULL,
    CONSTRAINT order_line_pkey PRIMARY KEY (id),
    CONSTRAINT order_line_billable_item_id_fkey
        FOREIGN KEY (billable_item_id) REFERENCES invoicing.billable_item(id),
    CONSTRAINT order_line_order_id_fkey
        FOREIGN KEY (order_id) REFERENCES invoicing.customer_order(id)
);

-- ─── Invoices ─────────────────────────────────────────────────

CREATE TABLE invoicing.invoice (
    id varchar(26) NOT NULL,
    invoice_number varchar(50) NULL,
    customer_id varchar(26) NOT NULL,
    point_of_sale_id varchar(26) NOT NULL,
    document_type_code varchar(10) NOT NULL,
    invoice_date timestamptz NOT NULL,
    subtotal_amount numeric(18,4) NOT NULL,
    discount_amount numeric(18,4) NOT NULL DEFAULT 0,
    tax_amount numeric(18,4) NOT NULL DEFAULT 0,
    total_amount numeric(18,4) NOT NULL,
    paid_amount numeric(18,4) DEFAULT 0 NOT NULL,
    balance_amount numeric(18,4) NOT NULL,
    current_status_id varchar(26) NULL,
    CONSTRAINT invoice_paid_lte_total CHECK (paid_amount <= total_amount),
    CONSTRAINT invoice_amounts_positive CHECK (
        subtotal_amount >= 0 AND discount_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0
    ),
    CONSTRAINT invoice_invoice_number_key UNIQUE (invoice_number),
    CONSTRAINT invoice_pkey PRIMARY KEY (id),
    CONSTRAINT invoice_current_status_id_fkey
        FOREIGN KEY (current_status_id) REFERENCES invoicing.status_catalog(id),
    CONSTRAINT invoice_customer_id_fkey
        FOREIGN KEY (customer_id) REFERENCES invoicing.customer(id),
    CONSTRAINT invoice_document_type_code_fkey
        FOREIGN KEY (document_type_code) REFERENCES invoicing.dte_document_type(code),
    CONSTRAINT invoice_point_of_sale_id_fkey
        FOREIGN KEY (point_of_sale_id) REFERENCES invoicing.point_of_sale(id)
);
CREATE INDEX idx_invoice_customer ON invoicing.invoice USING btree (customer_id);
CREATE INDEX idx_invoice_date ON invoicing.invoice USING btree (invoice_date);
CREATE INDEX idx_invoice_status ON invoicing.invoice USING btree (current_status_id);

CREATE TABLE invoicing.invoice_line (
    id varchar(26) NOT NULL,
    invoice_id varchar(26) NOT NULL,
    billable_item_id varchar(26) NULL,
    item_name varchar(255) NOT NULL,
    item_description text NULL,
    quantity numeric(18,4) NOT NULL,
    unit_price numeric(18,4) NOT NULL,
    subtotal_amount numeric(18,4) NOT NULL,
    discount_amount numeric(18,4) NOT NULL DEFAULT 0,
    tax_amount numeric(18,4) NOT NULL DEFAULT 0,
    total_amount numeric(18,4) NOT NULL,
    CONSTRAINT invoice_line_pkey PRIMARY KEY (id),
    CONSTRAINT invoice_line_billable_item_id_fkey
        FOREIGN KEY (billable_item_id) REFERENCES invoicing.billable_item(id),
    CONSTRAINT invoice_line_invoice_id_fkey
        FOREIGN KEY (invoice_id) REFERENCES invoicing.invoice(id)
);

CREATE TABLE invoicing.invoice_line_tax (
    id varchar(26) NOT NULL,
    invoice_line_id varchar(26) NOT NULL,
    tax_code varchar(20) NOT NULL,
    rate numeric(8,4) NOT NULL,
    taxable_amount numeric(18,4) NOT NULL,
    tax_amount numeric(18,4) NOT NULL,
    CONSTRAINT invoice_line_tax_pkey PRIMARY KEY (id),
    CONSTRAINT invoice_line_tax_invoice_line_id_fkey
        FOREIGN KEY (invoice_line_id) REFERENCES invoicing.invoice_line(id),
    CONSTRAINT invoice_line_tax_tax_code_fkey
        FOREIGN KEY (tax_code) REFERENCES invoicing.tax_definition(code)
);

-- ─── DTE ─────────────────────────────────────────────────────

CREATE TABLE invoicing.invoice_dte (
    id varchar(26) NOT NULL,
    invoice_id varchar(26) NOT NULL,
    generation_code varchar(100) NOT NULL,
    control_number varchar(100) NOT NULL,
    submitted_at timestamptz NULL,
    accepted_at timestamptz NULL,
    mh_code varchar(50) NULL,
    mh_message text NULL,
    request_payload jsonb NOT NULL,
    response_payload jsonb NULL,
    current_status_id varchar(26) NULL,
    CONSTRAINT invoice_dte_control_number_key UNIQUE (control_number),
    CONSTRAINT invoice_dte_generation_code_key UNIQUE (generation_code),
    CONSTRAINT invoice_dte_invoice_id_key UNIQUE (invoice_id),
    CONSTRAINT invoice_dte_pkey PRIMARY KEY (id),
    CONSTRAINT invoice_dte_current_status_id_fkey
        FOREIGN KEY (current_status_id) REFERENCES invoicing.status_catalog(id),
    CONSTRAINT invoice_dte_invoice_id_fkey
        FOREIGN KEY (invoice_id) REFERENCES invoicing.invoice(id)
);

CREATE TABLE invoicing.invoice_dte_event (
    id varchar(26) NOT NULL,
    invoice_dte_id varchar(26) NOT NULL,
    event_time timestamptz NOT NULL,
    request_json jsonb NULL,
    response_json jsonb NULL,
    event_type_code varchar(30) NULL,
    CONSTRAINT invoice_dte_event_pkey PRIMARY KEY (id),
    CONSTRAINT invoice_dte_event_event_type_code_fkey
        FOREIGN KEY (event_type_code) REFERENCES invoicing.dte_event_type(code),
    CONSTRAINT invoice_dte_event_invoice_dte_id_fkey
        FOREIGN KEY (invoice_dte_id) REFERENCES invoicing.invoice_dte(id)
);
CREATE INDEX idx_dte_event ON invoicing.invoice_dte_event USING btree (invoice_dte_id);

CREATE TABLE invoicing.dte_transmission_queue (
    id varchar(26) NOT NULL,
    invoice_dte_id varchar(26) NOT NULL,
    attempt_count int4 DEFAULT 0 NOT NULL,
    next_attempt_at timestamptz NULL,
    last_error text NULL,
    current_status_id varchar(26) NULL,
    CONSTRAINT dte_transmission_queue_pkey PRIMARY KEY (id),
    CONSTRAINT dte_transmission_queue_current_status_id_fkey
        FOREIGN KEY (current_status_id) REFERENCES invoicing.status_catalog(id),
    CONSTRAINT dte_transmission_queue_invoice_dte_id_fkey
        FOREIGN KEY (invoice_dte_id) REFERENCES invoicing.invoice_dte(id)
);
CREATE INDEX idx_dte_queue_status ON invoicing.dte_transmission_queue USING btree (current_status_id);
CREATE INDEX idx_dte_queue_next_attempt ON invoicing.dte_transmission_queue USING btree (next_attempt_at);

-- ─── Payments ─────────────────────────────────────────────────

CREATE TABLE invoicing.payment (
    id varchar(26) NOT NULL,
    payment_date timestamptz NOT NULL,
    amount numeric(18,4) NOT NULL,
    reference_number varchar(100) NULL,
    current_status_id varchar(26) NULL,
    payment_method_code varchar(20) NULL,
    CONSTRAINT payment_pkey PRIMARY KEY (id),
    CONSTRAINT payment_current_status_id_fkey
        FOREIGN KEY (current_status_id) REFERENCES invoicing.status_catalog(id),
    CONSTRAINT payment_payment_method_code_fkey
        FOREIGN KEY (payment_method_code) REFERENCES invoicing.payment_method(code)
);

CREATE TABLE invoicing.payment_allocation (
    id varchar(26) NOT NULL,
    payment_id varchar(26) NOT NULL,
    invoice_id varchar(26) NOT NULL,
    allocated_amount numeric(18,4) NOT NULL,
    CONSTRAINT payment_allocation_pkey PRIMARY KEY (id),
    CONSTRAINT payment_allocation_invoice_id_fkey
        FOREIGN KEY (invoice_id) REFERENCES invoicing.invoice(id),
    CONSTRAINT payment_allocation_payment_id_fkey
        FOREIGN KEY (payment_id) REFERENCES invoicing.payment(id)
);
CREATE INDEX idx_payment_allocation_invoice ON invoicing.payment_allocation USING btree (invoice_id);

-- ─── Cross-entity ─────────────────────────────────────────────

CREATE TABLE invoicing.outbox_event (
    id varchar(26) NOT NULL,
    aggregate_type varchar(100) NOT NULL,
    aggregate_id varchar(26) NOT NULL,
    event_type varchar(100) NOT NULL,
    payload jsonb NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL,
    processed_at timestamptz NULL,
    CONSTRAINT outbox_event_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_outbox_status ON invoicing.outbox_event USING btree (status);

CREATE TABLE invoicing.entity_status_history (
    id varchar(26) NOT NULL,
    entity_type varchar(50) NOT NULL,
    entity_id varchar(26) NOT NULL,
    old_status_id varchar(26) NULL,
    new_status_id varchar(26) NOT NULL,
    changed_at timestamptz NOT NULL,
    changed_by varchar(100) NOT NULL,
    reason text NULL,
    CONSTRAINT entity_status_history_pkey PRIMARY KEY (id),
    CONSTRAINT entity_status_history_new_status_id_fkey
        FOREIGN KEY (new_status_id) REFERENCES invoicing.status_catalog(id),
    CONSTRAINT entity_status_history_old_status_id_fkey
        FOREIGN KEY (old_status_id) REFERENCES invoicing.status_catalog(id)
);
CREATE INDEX idx_status_history_entity ON invoicing.entity_status_history USING btree (entity_type, entity_id);
CREATE INDEX idx_status_history_date ON invoicing.entity_status_history USING btree (changed_at);

CREATE TABLE invoicing.entity_reference (
    id varchar(26) NOT NULL,
    source_entity_type varchar(50) NOT NULL,
    source_entity_id varchar(26) NOT NULL,
    target_entity_type varchar(50) NOT NULL,
    target_entity_id varchar(26) NOT NULL,
    relationship_type_code varchar(50) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(100) NOT NULL,
    CONSTRAINT chk_entity_reference_types CHECK (
        source_entity_type IN ('ORDER','INVOICE','DTE','PAYMENT') AND
        target_entity_type IN ('ORDER','INVOICE','DTE','PAYMENT')
    ),
    CONSTRAINT entity_reference_pkey PRIMARY KEY (id),
    CONSTRAINT entity_reference_relationship_type_fkey
        FOREIGN KEY (relationship_type_code) REFERENCES invoicing.relationship_type(code)
);
CREATE INDEX idx_entity_reference_source ON invoicing.entity_reference USING btree (source_entity_type, source_entity_id);
CREATE INDEX idx_entity_reference_target ON invoicing.entity_reference USING btree (target_entity_type, target_entity_id);

CREATE TABLE invoicing.document_file (
    id varchar(26) NOT NULL,
    invoice_id varchar(26) NULL,
    invoice_dte_id varchar(26) NULL,
    file_type varchar(30) NOT NULL,
    storage_key text NOT NULL,
    content_hash varchar(128) NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT document_file_pkey PRIMARY KEY (id),
    CONSTRAINT document_file_invoice_dte_id_fkey
        FOREIGN KEY (invoice_dte_id) REFERENCES invoicing.invoice_dte(id),
    CONSTRAINT document_file_invoice_id_fkey
        FOREIGN KEY (invoice_id) REFERENCES invoicing.invoice(id)
);
