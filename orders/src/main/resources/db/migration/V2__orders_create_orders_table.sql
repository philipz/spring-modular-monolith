SET search_path TO orders;

CREATE SEQUENCE IF NOT EXISTS order_id_seq START WITH 100 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS orders
(
    id               BIGINT    NOT NULL DEFAULT nextval('orders.order_id_seq'),
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
    updated_at       TIMESTAMP,
    PRIMARY KEY (id)
);
