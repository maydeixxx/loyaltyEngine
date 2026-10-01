CREATE TABLE users (
    id UUID PRIMARY KEY,
    first_name varchar(255) NOT NULL,
    last_name varchar(255) NOT NULL,
    email varchar(255) NOT NULL UNIQUE,
    hashed_password varchar(255) NOT NULL,
    role varchar(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    event_type varchar(255) NOT NULL,
    payload TEXT NOT NULL,
    status varchar(255) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_event_status_created_at ON outbox_events(status, created_at);