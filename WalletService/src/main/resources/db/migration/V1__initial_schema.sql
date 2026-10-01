CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    balance DECIMAL(19, 2) NOT NULL,
    status varchar(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    transaction_id UUID NOT NULL UNIQUE,
    amount DECIMAL(19, 2) NOT NULL,
    type varchar(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    description varchar(255) NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type varchar(255) NOT NULL,
    payload TEXT NOT NULL,
    status varchar(255) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE INDEX idx_outbox_event_status_created_at ON outbox_events(status, created_at);
CREATE INDEX idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);