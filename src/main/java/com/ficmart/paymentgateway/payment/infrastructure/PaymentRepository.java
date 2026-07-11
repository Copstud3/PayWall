package com.ficmart.paymentgateway.payment.infrastructure;

import com.ficmart.paymentgateway.payment.domain.Payment;
import com.ficmart.paymentgateway.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPaymentReference(String paymentReference);

    List<Payment> findAllByOrderId(String orderId);
    List<Payment> findAllByCustomerId(String customerId);
    List<Payment> findAllByStatus(PaymentStatus status);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
 }