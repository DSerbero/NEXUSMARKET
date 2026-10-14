-- Persistencia de los agregados Seller, Warehouse, Inventory, Invoice,
-- Shipment, Return y Refund. Complementa V1__checkout_persistence.sql.
-- Convenciones heredadas de V1: claves String(36) tipo UUID, importes
-- DECIMAL(19,2), fechas TIMESTAMP(6), codigos de catalogo en VARCHAR.

CREATE TABLE sellers (
    user_identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    CONSTRAINT fk_sellers_user FOREIGN KEY (user_identifier) REFERENCES users (identifier)
);

CREATE TABLE warehouses (
    identifier BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    location VARCHAR(500) NOT NULL,
    owner_type VARCHAR(20) NOT NULL,
    owner_identifier VARCHAR(36) NOT NULL,
    responsible_user_identifier VARCHAR(36),
    CONSTRAINT fk_warehouses_owner FOREIGN KEY (owner_identifier) REFERENCES users (identifier),
    CONSTRAINT fk_warehouses_responsible FOREIGN KEY (responsible_user_identifier) REFERENCES users (identifier)
);

CREATE TABLE inventory (
    identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    product_identifier VARCHAR(36) NOT NULL,
    warehouse_identifier BIGINT NOT NULL,
    available_quantity INTEGER NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    movement_date TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_inventory_product_warehouse UNIQUE (product_identifier, warehouse_identifier),
    CONSTRAINT ck_inventory_quantity CHECK (available_quantity >= 0),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_identifier) REFERENCES warehouses (identifier)
);

CREATE TABLE invoices (
    identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    order_identifier VARCHAR(36) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    issue_date TIMESTAMP(6) NOT NULL,
    status_code VARCHAR(20) NOT NULL,
    CONSTRAINT uk_invoices_order UNIQUE (order_identifier),
    CONSTRAINT ck_invoices_total CHECK (total_amount > 0),
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_identifier) REFERENCES orders (identifier)
);

CREATE TABLE shipments (
    identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    order_identifier VARCHAR(36) NOT NULL,
    origin_warehouse_identifier BIGINT NOT NULL,
    operator_identifier VARCHAR(36) NOT NULL,
    status_code VARCHAR(20) NOT NULL,
    dispatch_date TIMESTAMP(6),
    CONSTRAINT uk_shipments_order UNIQUE (order_identifier),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_identifier) REFERENCES orders (identifier),
    CONSTRAINT fk_shipments_warehouse FOREIGN KEY (origin_warehouse_identifier) REFERENCES warehouses (identifier),
    CONSTRAINT fk_shipments_operator FOREIGN KEY (operator_identifier) REFERENCES users (identifier)
);

CREATE TABLE returns (
    identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    order_identifier VARCHAR(36) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status_code VARCHAR(20) NOT NULL,
    request_date TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_returns_order FOREIGN KEY (order_identifier) REFERENCES orders (identifier)
);

CREATE TABLE refunds (
    identifier VARCHAR(36) NOT NULL PRIMARY KEY,
    return_identifier VARCHAR(36) NOT NULL,
    refunded_amount DECIMAL(19, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    status_code VARCHAR(20) NOT NULL,
    processing_date TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_refunds_return UNIQUE (return_identifier),
    CONSTRAINT ck_refunds_amount CHECK (refunded_amount > 0),
    CONSTRAINT fk_refunds_return FOREIGN KEY (return_identifier) REFERENCES returns (identifier)
);
