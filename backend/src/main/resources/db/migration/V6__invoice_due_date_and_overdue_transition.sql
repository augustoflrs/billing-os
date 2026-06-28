-- Add due_date to invoice table
ALTER TABLE invoicing.invoice ADD COLUMN due_date timestamp with time zone;

-- Index for the overdue scheduler query
CREATE INDEX idx_invoice_due_date ON invoicing.invoice (due_date)
    WHERE current_status_id IN ('inv_issued', 'inv_partial');

-- Add missing inv_partial → inv_overdue transition
INSERT INTO invoicing.status_transition (id, entity_type, from_status_id, to_status_id)
VALUES ('inv_par_to_ovd01', 'INVOICE', 'inv_partial', 'inv_overdue');
