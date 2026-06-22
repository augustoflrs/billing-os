-- =============================================================
-- V3: Status machine — status_catalog + status_transition
-- Uses gen_random_uuid() cast to text for ULID-like IDs
-- (Replace with application-generated ULIDs in production)
-- =============================================================

-- ─── INVOICE statuses ─────────────────────────────────────────

INSERT INTO invoicing.status_catalog (id, entity_type, code, name, description, is_initial, is_final, active) VALUES
('inv_draft',     'INVOICE', 'DRAFT',     'Borrador',    'Invoice created but not confirmed',      true,  false, true),
('inv_issued',    'INVOICE', 'ISSUED',    'Emitida',     'Invoice confirmed and number assigned',  false, false, true),
('inv_partial',   'INVOICE', 'PARTIAL',   'Pago Parcial','Partially paid',                         false, false, true),
('inv_paid',      'INVOICE', 'PAID',      'Pagada',      'Fully paid',                             false, true,  true),
('inv_cancelled', 'INVOICE', 'CANCELLED', 'Anulada',     'Cancelled with MH invalidation',         false, true,  true),
('inv_overdue',   'INVOICE', 'OVERDUE',   'Vencida',     'Past due date, unpaid balance',          false, false, true);

-- ─── DTE statuses ─────────────────────────────────────────────

INSERT INTO invoicing.status_catalog (id, entity_type, code, name, description, is_initial, is_final, active) VALUES
('dte_draft',        'DTE', 'DRAFT',       'Borrador',         'Payload built, not yet queued',      true,  false, true),
('dte_queued',       'DTE', 'QUEUED',      'En Cola',          'Outbox event written',               false, false, true),
('dte_submitted',    'DTE', 'SUBMITTED',   'Enviado a MH',     'Transmitted to MH API',             false, false, true),
('dte_accepted',     'DTE', 'ACCEPTED',    'Aceptado por MH',  'MH returned acceptance',            false, true,  true),
('dte_rejected',     'DTE', 'REJECTED',    'Rechazado por MH', 'MH returned rejection',             false, true,  true),
('dte_contingency',  'DTE', 'CONTINGENCY', 'Contingencia',     'MH unreachable, max retries hit',   false, false, true),
('dte_invalidated',  'DTE', 'INVALIDATED', 'Invalidado',       'Cancellation accepted by MH',       false, true,  true),
('dte_blocked',      'DTE', 'BLOCKED',     'Bloqueado',        'No active certificate available',   false, false, true);

-- ─── PAYMENT statuses ─────────────────────────────────────────

INSERT INTO invoicing.status_catalog (id, entity_type, code, name, description, is_initial, is_final, active) VALUES
('pay_pending',   'PAYMENT', 'PENDING',   'Pendiente',  'Payment recorded, not yet confirmed', true,  false, true),
('pay_confirmed', 'PAYMENT', 'CONFIRMED', 'Confirmado', 'Payment confirmed',                   false, true,  true),
('pay_voided',    'PAYMENT', 'VOIDED',    'Anulado',    'Payment voided',                      false, true,  true);

-- ─── DTE_QUEUE statuses ───────────────────────────────────────

INSERT INTO invoicing.status_catalog (id, entity_type, code, name, description, is_initial, is_final, active) VALUES
('dtq_pending',   'DTE_QUEUE', 'PENDING',   'Pendiente',  'Awaiting transmission attempt', true,  false, true),
('dtq_sent',      'DTE_QUEUE', 'SENT',      'Enviado',    'Attempt made',                  false, true,  true),
('dtq_failed',    'DTE_QUEUE', 'FAILED',    'Fallido',    'Max retries exceeded',          false, true,  true);

-- ─── INVOICE transitions ─────────────────────────────────────

INSERT INTO invoicing.status_transition (id, entity_type, from_status_id, to_status_id, active) VALUES
('it_01', 'INVOICE', 'inv_draft',   'inv_issued',    true),  -- confirm
('it_02', 'INVOICE', 'inv_issued',  'inv_partial',   true),  -- partial payment
('it_03', 'INVOICE', 'inv_issued',  'inv_paid',      true),  -- full payment
('it_04', 'INVOICE', 'inv_issued',  'inv_cancelled', true),  -- cancel
('it_05', 'INVOICE', 'inv_issued',  'inv_overdue',   true),  -- scheduled job
('it_06', 'INVOICE', 'inv_partial', 'inv_paid',      true),  -- remaining payment
('it_07', 'INVOICE', 'inv_partial', 'inv_cancelled', true),  -- cancel with refund
('it_08', 'INVOICE', 'inv_overdue', 'inv_paid',      true),  -- late payment
('it_09', 'INVOICE', 'inv_overdue', 'inv_cancelled', true);  -- write-off

-- ─── DTE transitions ─────────────────────────────────────────

INSERT INTO invoicing.status_transition (id, entity_type, from_status_id, to_status_id, active) VALUES
('dt_01', 'DTE', 'dte_draft',       'dte_queued',      true),  -- outbox written
('dt_02', 'DTE', 'dte_queued',      'dte_submitted',   true),  -- transmission attempt
('dt_03', 'DTE', 'dte_queued',      'dte_contingency', true),  -- MH offline at queue time
('dt_04', 'DTE', 'dte_submitted',   'dte_accepted',    true),  -- MH accepts
('dt_05', 'DTE', 'dte_submitted',   'dte_rejected',    true),  -- MH rejects
('dt_06', 'DTE', 'dte_submitted',   'dte_queued',      true),  -- retry after timeout
('dt_07', 'DTE', 'dte_submitted',   'dte_contingency', true),  -- max retries
('dt_08', 'DTE', 'dte_contingency', 'dte_queued',      true),  -- manual retry after MH recovery
('dt_09', 'DTE', 'dte_accepted',    'dte_invalidated', true),  -- cancellation accepted
('dt_10', 'DTE', 'dte_draft',       'dte_blocked',     true),  -- no certificate
('dt_11', 'DTE', 'dte_blocked',     'dte_queued',      true);  -- certificate uploaded, retry

-- ─── PAYMENT transitions ─────────────────────────────────────

INSERT INTO invoicing.status_transition (id, entity_type, from_status_id, to_status_id, active) VALUES
('pt_01', 'PAYMENT', 'pay_pending',   'pay_confirmed', true),
('pt_02', 'PAYMENT', 'pay_pending',   'pay_voided',    true),
('pt_03', 'PAYMENT', 'pay_confirmed', 'pay_voided',    true);
