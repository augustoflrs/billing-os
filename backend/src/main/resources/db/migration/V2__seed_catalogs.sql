-- =============================================================
-- V2: Seed catalog / reference data
-- MH El Salvador official codelists
-- =============================================================

-- ─── Departments (14 departamentos de El Salvador) ────────────

INSERT INTO invoicing.catalog_department (code, name) VALUES
('01', 'Ahuachapán'),
('02', 'Santa Ana'),
('03', 'Sonsonate'),
('04', 'Chalatenango'),
('05', 'La Libertad'),
('06', 'San Salvador'),
('07', 'Cuscatlán'),
('08', 'La Paz'),
('09', 'Cabañas'),
('10', 'San Vicente'),
('11', 'Usulután'),
('12', 'San Miguel'),
('13', 'Morazán'),
('14', 'La Unión');

-- ─── Municipalities (sample — extend with full MH list) ───────

INSERT INTO invoicing.catalog_municipality (code, department_code, name) VALUES
-- San Salvador
('0601', '06', 'San Salvador'),
('0602', '06', 'Apopa'),
('0603', '06', 'Ayutuxtepeque'),
('0604', '06', 'Cuscatancingo'),
('0605', '06', 'Delgado'),
('0606', '06', 'El Paisnal'),
('0607', '06', 'Guazapa'),
('0608', '06', 'Ilopango'),
('0609', '06', 'Mejicanos'),
('0610', '06', 'Nejapa'),
('0611', '06', 'Panchimalco'),
('0612', '06', 'Rosario de Mora'),
('0613', '06', 'San Marcos'),
('0614', '06', 'San Martín'),
('0615', '06', 'Santa Tecla'),
('0616', '06', 'Santo Tomás'),
('0617', '06', 'Soyapango'),
('0618', '06', 'Tonacatepeque'),
-- La Libertad
('0501', '05', 'Santa Tecla'),
('0502', '05', 'Antiguo Cuscatlán'),
('0503', '05', 'Ciudad Arce'),
('0504', '05', 'Colón'),
('0505', '05', 'Comasagua'),
('0506', '05', 'Chiltiupán'),
('0507', '05', 'Huizúcar'),
('0508', '05', 'Jayaque'),
('0509', '05', 'Jicalapa'),
('0510', '05', 'La Libertad'),
('0511', '05', 'Nuevo Cuscatlán'),
('0512', '05', 'Quezaltepeque'),
('0513', '05', 'San Juan Opico'),
('0514', '05', 'Talnique'),
('0515', '05', 'Tamanique'),
('0516', '05', 'Teotepeque'),
('0517', '05', 'Tepecoyo'),
('0518', '05', 'Zaragoza');

-- ─── DTE Document Types (MH catalog) ─────────────────────────

INSERT INTO invoicing.dte_document_type (code, name, active) VALUES
('01', 'Factura', true),
('03', 'Comprobante de Crédito Fiscal', true),
('04', 'Nota de Remisión', true),
('05', 'Nota de Crédito', true),
('06', 'Nota de Débito', true),
('07', 'Comprobante de Retención', true),
('08', 'Comprobante de Liquidación', true),
('09', 'Documento Contable de Liquidación', true),
('11', 'Facturas de Exportación', true),
('14', 'Factura de Sujeto Excluido', true),
('15', 'Comprobante de Donación', true);

-- ─── DTE Event Types ─────────────────────────────────────────

INSERT INTO invoicing.dte_event_type (code, name) VALUES
('SUBMISSION',         'DTE Submitted to MH'),
('ACCEPTANCE',         'DTE Accepted by MH'),
('REJECTION',          'DTE Rejected by MH'),
('RETRY',              'DTE Submission Retry'),
('CONTINGENCY_ENTRY',  'Entered Contingency Mode'),
('CONTINGENCY_EXIT',   'Exited Contingency Mode'),
('INVALIDATION',       'DTE Invalidation Submitted'),
('INVALIDATION_ACCEPTED', 'DTE Invalidation Accepted');

-- ─── Payment Methods ──────────────────────────────────────────

INSERT INTO invoicing.payment_method (code, name, active) VALUES
('CASH',         'Efectivo',            true),
('TRANSFER',     'Transferencia Bancaria', true),
('CARD_DEBIT',   'Tarjeta de Débito',   true),
('CARD_CREDIT',  'Tarjeta de Crédito',  true),
('CHECK',        'Cheque',              true),
('CRYPTO',       'Criptomoneda',        true),
('OTHER',        'Otro',                true);

-- ─── Tax Definitions (El Salvador) ───────────────────────────

INSERT INTO invoicing.tax_definition (code, name, rate, active) VALUES
('IVA',     'IVA 13%',                 0.1300, true),
('IVA_EXE', 'Operación Exenta de IVA', 0.0000, true),
('IVA_NSD', 'No Sujeto IVA',           0.0000, true),
('RENTA',   'Retención de Renta 10%',  0.1000, true),
('IVA_EXP', 'IVA Exportación 0%',      0.0000, true);

-- ─── Relationship Types ───────────────────────────────────────

INSERT INTO invoicing.relationship_type (code, name, description) VALUES
('ORDER_TO_INVOICE',   'Order → Invoice',   'Customer order converted to invoice'),
('INVOICE_TO_DTE',     'Invoice → DTE',     'Invoice fiscal document'),
('INVOICE_TO_PAYMENT', 'Invoice → Payment', 'Payment allocated to invoice'),
('CREDIT_NOTE_TO_INVOICE', 'Credit Note → Invoice', 'Credit note references original invoice'),
('DEBIT_NOTE_TO_INVOICE',  'Debit Note → Invoice',  'Debit note references original invoice');
