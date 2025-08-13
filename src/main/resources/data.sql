-- Pre seed data --

INSERT INTO account (id, name, balance, currency) VALUES
('11111111-1111-1111-1111-111111111111', 'Alice', 1000.00, 'USD'),
('22222222-2222-2222-2222-222222222222', 'Bob', 500.00, 'AUD');

INSERT INTO fx_rate (from_currency, to_currency, rate) VALUES
('USD', 'AUD', 2.0),
('AUD', 'USD', 0.5);

INSERT INTO fee_config (fee_percentage) VALUES
(0.01);