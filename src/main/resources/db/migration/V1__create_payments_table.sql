CREATE TABLE payments (
        id UUID PRIMARY KEY,
        payment_reference VARCHAR(100) NOT NULL UNIQUE,
        order_id VARCHAR(100) NOT NULL,
        customer_id VARCHAR(100) NOT NULL,
        amount_in_cents BIGINT NOT NULL,
        currency VARCHAR(10) NOT NULL,
        status VARCHAR(30) NOT NULL,

        bank_authorization_id VARCHAR(100),
        bank_capture_id VARCHAR(100),
        bank_void_id VARCHAR(100),
        bank_refund_id VARCHAR(100),

        failure_reason TEXT,

        authorized_at TIMESTAMP,
        captured_at TIMESTAMP,
        voided_at TIMESTAMP,
        refunded_at TIMESTAMP,

        created_at TIMESTAMP NOT NULL,
        updated_at TIMESTAMP NOT NULL
);

