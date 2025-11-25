ALTER TABLE transfers
ADD COLUMN fee NUMERIC(19, 2) DEFAULT 0 NOT NULL;

ALTER TABLE transfers
ADD COLUMN idempotency_key UUID;

ALTER TABLE transfers
ADD CONSTRAINT uq_transfer_idempotency_key UNIQUE (idempotency_key);