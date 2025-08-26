CREATE TABLE IF NOT EXISTS accounts (
  id              UUID PRIMARY KEY,
  owner_name      VARCHAR(100) NOT NULL,
  currency        CHAR(3) NOT NULL,
  balance         NUMERIC(19,2) NOT NULL DEFAULT 0,
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);