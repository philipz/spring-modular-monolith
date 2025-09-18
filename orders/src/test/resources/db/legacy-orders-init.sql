CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE IF NOT EXISTS orders.orders (
    id               BIGSERIAL PRIMARY KEY,
    order_number     TEXT      NOT NULL UNIQUE,
    customer_name    TEXT      NOT NULL,
    customer_email   TEXT      NOT NULL,
    customer_phone   TEXT      NOT NULL,
    delivery_address TEXT      NOT NULL,
    product_code     TEXT      NOT NULL,
    product_name     TEXT      NOT NULL,
    product_price    TEXT      NOT NULL,
    quantity         INT       NOT NULL,
    status           TEXT      NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP
);

TRUNCATE TABLE orders.orders;

INSERT INTO orders.orders (
    order_number,
    customer_name,
    customer_email,
    customer_phone,
    delivery_address,
    product_code,
    product_name,
    product_price,
    quantity,
    status,
    created_at,
    updated_at
) VALUES
('LEG-100', 'Legacy Alice', 'alice.legacy@example.com', '+12025550123', '742 Evergreen Terrace', 'P100', 'Legacy Book', '19.99', 1, 'NEW', '2024-01-10 09:15:00', '2024-01-10 09:15:00'),
('LEG-101', 'Legacy Bob', 'bob.legacy@example.com', '+12025550124', '1313 Mockingbird Lane', 'P200', 'Legacy Gadget', '49.99', 2, 'SHIPPED', '2024-01-11 12:30:00', '2024-01-11 20:45:00');
