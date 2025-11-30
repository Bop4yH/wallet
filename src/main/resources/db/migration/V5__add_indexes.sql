CREATE INDEX idx_transfer_from_created ON transfers (from_account_id, created_at);

CREATE INDEX idx_transfer_to ON transfers (to_account_id);

CREATE INDEX idx_owner_currency ON accounts (owner_name, currency);