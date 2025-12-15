CREATE TABLE IF NOT EXISTS transfers (
  id              UUID PRIMARY KEY,
  from_account_id UUID NOT NULL REFERENCES accounts(id),
  to_account_id   UUID NOT NULL REFERENCES accounts(id),
  amount          NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  status          VARCHAR(20) NOT NULL,
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);