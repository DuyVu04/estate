CREATE TABLE payments (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,

    reservation_id VARCHAR(36) NOT NULL,

    amount DECIMAL(15, 2) NOT NULL,

    payment_method VARCHAR(30) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    transaction_ref VARCHAR(100),

    idempotency_key VARCHAR(100) NOT NULL,

    paid_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payment_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    CONSTRAINT uq_payment_idempotency_key
        UNIQUE (idempotency_key)
);
