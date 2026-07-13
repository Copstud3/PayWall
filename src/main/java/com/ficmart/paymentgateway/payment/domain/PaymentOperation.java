package com.ficmart.paymentgateway.payment.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_operations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type")
    private PaymentOperationType operationType;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "request_hash")
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentOperationStatus status;

    @Column(name = "response_data")
    private String responseData;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}