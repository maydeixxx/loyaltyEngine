CREATE TABLE products(
    product_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title varchar(255) NOT NULL,
    description TEXT NOT NULL,
    status varchar(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);