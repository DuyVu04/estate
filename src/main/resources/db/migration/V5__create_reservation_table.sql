CREATE TABLE reservations (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,

    user_id VARCHAR(36) NOT NULL,

    property_id VARCHAR(36) NOT NULL,

    status VARCHAR(20) NOT NULL,

    expires_at TIMESTAMP NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_reservation_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_reservation_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id)
);