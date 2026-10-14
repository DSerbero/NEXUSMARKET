CREATE TABLE users (
    identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    normalized_email VARCHAR(320) NOT NULL,
    role_code VARCHAR(40) NOT NULL,
    status_code VARCHAR(40) NOT NULL,
    CONSTRAINT uk_users_normalized_email UNIQUE (normalized_email)
);

CREATE TABLE user_credentials (
    user_identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT fk_credentials_user FOREIGN KEY (user_identifier) REFERENCES users (identifier)
);

CREATE TABLE buyers (
    user_identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    primary_address VARCHAR(500) NOT NULL,
    buyer_status_code VARCHAR(40) NOT NULL,
    CONSTRAINT fk_buyers_user FOREIGN KEY (user_identifier) REFERENCES users (identifier)
);

CREATE TABLE buyer_additional_addresses (
    user_identifier VARCHAR(36) NOT NULL,
    position INTEGER NOT NULL,
    address VARCHAR(500) NOT NULL,
    PRIMARY KEY (user_identifier, position),
    CONSTRAINT fk_buyer_addresses_buyer FOREIGN KEY (user_identifier) REFERENCES buyers (user_identifier)
);

CREATE TABLE carts (
    buyer_identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    creation_date TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_carts_buyer FOREIGN KEY (buyer_identifier) REFERENCES buyers (user_identifier)
);

CREATE TABLE cart_items (
    identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    cart_identifier VARCHAR(36) NOT NULL,
    position INTEGER NOT NULL,
    product_identifier VARCHAR(36) NOT NULL,
    product_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT uk_cart_item_position UNIQUE (cart_identifier, position),
    CONSTRAINT uk_cart_product UNIQUE (cart_identifier, product_identifier),
    CONSTRAINT ck_cart_item_quantity CHECK (quantity > 0),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_identifier) REFERENCES carts (buyer_identifier)
);

CREATE TABLE orders (
    identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    buyer_identifier VARCHAR(36) NOT NULL,
    status_code VARCHAR(40) NOT NULL,
    creation_date TIMESTAMP(6) NOT NULL,
    completion_date TIMESTAMP(6),
    version BIGINT NOT NULL,
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_identifier) REFERENCES buyers (user_identifier)
);

CREATE TABLE order_items (
    identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    order_identifier VARCHAR(36) NOT NULL,
    position INTEGER NOT NULL,
    product_identifier VARCHAR(36) NOT NULL,
    product_type VARCHAR(20) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    quantity INTEGER NOT NULL,
    seller_identifier VARCHAR(36),
    digital_asset_reference VARCHAR(1000),
    CONSTRAINT uk_order_item_position UNIQUE (order_identifier, position),
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_item_price CHECK (unit_price > 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_identifier) REFERENCES orders (identifier)
);

CREATE TABLE order_item_variants (
    order_item_identifier VARCHAR(36) NOT NULL,
    position INTEGER NOT NULL,
    variant_value VARCHAR(200) NOT NULL,
    PRIMARY KEY (order_item_identifier, position),
    CONSTRAINT fk_order_item_variants_item FOREIGN KEY (order_item_identifier) REFERENCES order_items (identifier)
);