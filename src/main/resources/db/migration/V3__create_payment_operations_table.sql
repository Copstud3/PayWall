CREATE TABLE payment_operations (
                                    id UUID PRIMARY KEY,
                                    payment_id UUID NOT NULL,
                                    operation_type VARCHAR(30) NOT NULL,
                                    idempotency_key VARCHAR(255) NOT NULL,
                                    request_hash VARCHAR(255) NOT NULL,
                                    status VARCHAR(30) NOT NULL,
                                    response_data TEXT,
                                    error_code VARCHAR(100),
                                    error_message TEXT,
                                    created_at TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMP NOT NULL,

                                    CONSTRAINT fk_payment_operations_payment
                                        FOREIGN KEY (payment_id)
                                            REFERENCES payments(id),

                                    CONSTRAINT uk_payment_operation_idempotency
                                        UNIQUE (operation_type, idempotency_key)
);