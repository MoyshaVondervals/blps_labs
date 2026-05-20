CREATE TABLE IF NOT EXISTS customers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(50),
    dolibarr_thirdparty_id BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sellers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS couriers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(50),
    available       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS products (
    id              BIGSERIAL PRIMARY KEY,
    seller_id       BIGINT NOT NULL REFERENCES sellers(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           NUMERIC(12, 2) NOT NULL,
    available       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    seller_id       BIGINT NOT NULL REFERENCES sellers(id),
    courier_id      BIGINT REFERENCES couriers(id),
    status          VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    total_price     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    seller_notified_at      TIMESTAMP,
    courier_notified_at     TIMESTAMP,
    courier_assigned_at     TIMESTAMP,
    courier_arrived_at      TIMESTAMP,
    cancelled_at            TIMESTAMP,
    delivered_at            TIMESTAMP,
    dolibarr_invoice_id     BIGINT,
    dolibarr_invoice_ref    VARCHAR(255),
    invoice_created_at      TIMESTAMP,
    cancel_reason           VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      BIGINT REFERENCES products(id),
    product_name    VARCHAR(255) NOT NULL,
    quantity        INT NOT NULL DEFAULT 1,
    price           NUMERIC(12, 2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL PRIMARY KEY,
    recipient_type  VARCHAR(50) NOT NULL,
    recipient_id    BIGINT NOT NULL,
    order_id        BIGINT NOT NULL REFERENCES orders(id),
    external_event_id UUID UNIQUE,
    message         TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS external_event_id UUID;
CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_external_event_id
    ON notifications(external_event_id)
    WHERE external_event_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID PRIMARY KEY,
    source_service  VARCHAR(100) NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    BIGINT NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    event_key       VARCHAR(255) NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'NEW',
    attempts        INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMP,
    last_error      TEXT
);

CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_seller ON orders(seller_id);
CREATE INDEX IF NOT EXISTS idx_orders_courier ON orders(courier_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_type, recipient_id);
CREATE INDEX IF NOT EXISTS idx_outbox_events_pending ON outbox_events(source_service, status, next_attempt_at, created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_products_seller ON products(seller_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items(product_id);
