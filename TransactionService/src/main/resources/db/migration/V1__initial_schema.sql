CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status varchar(255) NOT NULL,
    use_cashback_balance BOOLEAN NOT NULL
);

CREATE TABLE transaction_item (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    category varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    price DECIMAL(19, 2) NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type varchar(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    status varchar(255) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_event_status_created_at ON outbox_events(status, created_at);
CREATE INDEX idx_transaction_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_transaction_user_id ON transactions(user_id);