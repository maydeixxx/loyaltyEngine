CREATE TABLE IF NOT EXISTS cashback_rules(
    id UUID PRIMARY KEY,
    category varchar(255) NOT NULL UNIQUE,
    percentage DECIMAL(19, 2) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cashback_rules_category_dates ON cashback_rules(category, valid_from, valid_to);