CREATE INDEX idx_transfer_from_created ON transfers (from_account_id, created_at);

-- Поиск входящих переводов
CREATE INDEX idx_transfer_to ON transfers (to_account_id);


-- Индекс для таблицы accounts (из аннотации @Table)
-- Примечание: у вас уже есть unique constraint из V3, но в Java коде
-- явно прописан еще и обычный индекс с именем idx_owner_currency.
-- Создадим его, чтобы Hibernate validate был доволен.
CREATE INDEX idx_owner_currency ON accounts (owner_name, currency);