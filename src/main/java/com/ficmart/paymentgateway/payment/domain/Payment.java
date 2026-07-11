package com.ficmart.paymentgateway.payment.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "amount_in_cents")
    private Long amountInCents;

    @Column(name = "currency")
    private String currency;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "bank_authorization_id")
    private String bankAuthorizationId;

    @Column(name = "bank_capture_id")
    private String bankCaptureId;

    @Column(name = "bank_void_id")
    private String bankVoidId;

    @Column(name = "bank_refund_id")
    private String bankRefundId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

}