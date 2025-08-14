CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    balance DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    from_account_id UUID NOT NULL,
    to_account_id UUID NOT NULL,
    amount_debited DECIMAL(19,2) NOT NULL,
    amount_credited DECIMAL(19,2) NOT NULL,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    transaction_fee DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE fx_rates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(19,4) NOT NULL
);

-- FEE_CONFIG TABLE
CREATE TABLE fee_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    global_fee_percentage DECIMAL(5,4) NOT NULL
);
