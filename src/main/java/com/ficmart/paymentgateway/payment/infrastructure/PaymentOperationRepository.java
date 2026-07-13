package com.ficmart.paymentgateway.payment.infrastructure;

import com.ficmart.paymentgateway.payment.domain.PaymentOperation;
import com.ficmart.paymentgateway.payment.domain.PaymentOperationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentOperationRepository
        extends JpaRepository<PaymentOperation, UUID> {

    Optional<PaymentOperation> findByOperationTypeAndIdempotencyKey(
            PaymentOperationType operationType,
            String idempotencyKey
    );
}
