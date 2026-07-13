package com.ficmart.paymentgateway.payment.application;

import com.ficmart.paymentgateway.payment.api.dto.*;
import com.ficmart.paymentgateway.payment.application.exceptions.*;
import com.ficmart.paymentgateway.payment.domain.*;
import com.ficmart.paymentgateway.payment.infrastructure.PaymentOperationRepository;
import com.ficmart.paymentgateway.payment.infrastructure.PaymentRepository;
import com.ficmart.paymentgateway.payment.infrastructure.bank.MockBankClient;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankAuthorizeRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankCaptureRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankRefundRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankVoidRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankAuthorizationException;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankCommunicationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MockBankClient mockBankClient;
    private final PaymentOperationRepository paymentOperationRepository;

    // Business logic to return a response when a payment is authorized
    public AuthorizeResponse authorisePayment(
            AuthorizeRequest request,
            String idempotencyKey
    ) {
        Payment payment;

        // Used later to detect whether the same idempotency key
        // was reused with a different authorization request.
        var requestHash = generateAuthorizationRequestHash(request);

        /*
         * TEMPORARY:
         * We are keeping the existing Payment-based idempotency lookup
         * until authorization is fully migrated to payment_operations.
         */
        var existingPayment =
                paymentRepository.findByIdempotencyKey(idempotencyKey);

        if (existingPayment.isPresent()) {
            payment = existingPayment.get();

            var requestDoesNotMatch =
                    !payment.getOrderId().equals(request.getOrderId())
                            || !payment.getCustomerId().equals(request.getCustomerId())
                            || !payment.getAmountInCents().equals(request.getAmountInCents())
                            || !payment.getCurrency().equalsIgnoreCase(request.getCurrency());

            if (requestDoesNotMatch) {
                throw new IdempotencyConflictException();
            }

            if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                return new AuthorizeResponse(
                        payment.getPaymentReference(),
                        payment.getStatus(),
                        payment.getAmountInCents(),
                        payment.getCurrency(),
                        "Existing authorization returned"
                );
            }

            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new PaymentAlreadyProcessedException();
            }

        } else {
            var paymentRef = "PAY-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 16)
                    .toUpperCase();

            payment = new Payment();

            payment.setPaymentReference(paymentRef);
            payment.setOrderId(request.getOrderId());
            payment.setCustomerId(request.getCustomerId());
            payment.setAmountInCents(request.getAmountInCents());
            payment.setCurrency(request.getCurrency());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());

            /*
             * TEMPORARY:
             * Keep storing the authorization idempotency key on Payment
             * until the migration to payment_operations is complete.
             */
            payment.setIdempotencyKey(idempotencyKey);

            payment = paymentRepository.save(payment);
        }

        /*
         * Create the authorization operation if it does not already exist.
         *
         * On a retry after an uncertain bank communication failure,
         * the existing PROCESSING operation will be reused.
         */
        var finalPayment = payment;

        var operation = paymentOperationRepository
                .findByOperationTypeAndIdempotencyKey(
                        PaymentOperationType.AUTHORIZE,
                        idempotencyKey
                )
                .orElseGet(() -> {
                    var newOperation = new PaymentOperation();

                    newOperation.setPayment(finalPayment);
                    newOperation.setOperationType(
                            PaymentOperationType.AUTHORIZE
                    );
                    newOperation.setIdempotencyKey(idempotencyKey);
                    newOperation.setRequestHash(requestHash);
                    newOperation.setStatus(
                            PaymentOperationStatus.PROCESSING
                    );

                    return paymentOperationRepository.save(newOperation);
                });

        /*
         * Safety check for payment_operations.
         *
         * The old Payment field comparison remains above temporarily,
         * but this is the check that will eventually replace it.
         */
        if (!operation.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException();
        }

        var bankRequest = new BankAuthorizeRequest(
                request.getAmountInCents(),
                request.getCardNumber(),
                request.getCvv(),
                request.getExpiryMonth(),
                request.getExpiryYear()
        );

        try {
            var bankResponse = mockBankClient.authorize(
                    bankRequest,
                    idempotencyKey
            );

            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setBankAuthorizationId(
                    bankResponse.getAuthorizationId()
            );
            payment.setAuthorizedAt(LocalDateTime.now());
            payment.setFailureReason(null);
            payment.setUpdatedAt(LocalDateTime.now());

            var authorizedPayment = paymentRepository.save(payment);

            operation.setStatus(PaymentOperationStatus.SUCCEEDED);
            operation.setErrorCode(null);
            operation.setErrorMessage(null);

            operation.setResponseData(
                    """
                    {
                      "paymentReference": "%s",
                      "status": "%s",
                      "amountInCents": %d,
                      "currency": "%s",
                      "message": "Payment authorized successfully"
                    }
                    """.formatted(
                            authorizedPayment.getPaymentReference(),
                            authorizedPayment.getStatus(),
                            authorizedPayment.getAmountInCents(),
                            authorizedPayment.getCurrency()
                    )
            );

            paymentOperationRepository.save(operation);

            return new AuthorizeResponse(
                    authorizedPayment.getPaymentReference(),
                    authorizedPayment.getStatus(),
                    authorizedPayment.getAmountInCents(),
                    authorizedPayment.getCurrency(),
                    "Payment authorized successfully"
            );

        } catch (BankAuthorizationException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            payment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            operation.setStatus(PaymentOperationStatus.FAILED);
            operation.setErrorCode("BANK_AUTHORIZATION_FAILED");
            operation.setErrorMessage(ex.getMessage());

            paymentOperationRepository.save(operation);

            throw ex;

        } catch (BankCommunicationException ex) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setFailureReason(
                    "Authorization outcome unknown. Retry with the same idempotency key."
            );
            payment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            /*
             * Keep the operation as PROCESSING because the bank outcome
             * is unknown. The request can safely be retried with the same key.
             */
            operation.setStatus(PaymentOperationStatus.PROCESSING);
            operation.setErrorCode("BANK_COMMUNICATION_ERROR");
            operation.setErrorMessage(
                    "Authorization outcome unknown. Retry with the same idempotency key."
            );

            paymentOperationRepository.save(operation);

            throw ex;
        }
    }

    // Business logic for returning a response on capture
    public CaptureResponse capturePayment(CaptureRequest request, String idempotencyKey) {
        var payment = paymentRepository.findByPaymentReference(request.paymentReference()).orElseThrow(PaymentRefNotFoundException::new);

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentAlreadyProcessedException();
        }

        if (payment.getBankAuthorizationId() == null) {
            throw new MissingBankAuthorizationException();
        }

        var bankRequest = new BankCaptureRequest();

        bankRequest.setAuthorizationId(payment.getBankAuthorizationId());
        bankRequest.setAmount(payment.getAmountInCents());

        var bankResponse = mockBankClient.capture(bankRequest, idempotencyKey);

        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setBankCaptureId(
                bankResponse.getCaptureId()
        );
        payment.setCapturedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        var savedPayment = paymentRepository.save(payment);

        return new CaptureResponse(
                savedPayment.getPaymentReference(),
                savedPayment.getStatus(),
                savedPayment.getAmountInCents(),
                savedPayment.getCurrency(),
                "Captured payment successfully"
        );
    }

    public VoidResponse voidPayment(VoidRequest request, String idempotencyKey) {
        var payment = paymentRepository.findByPaymentReference(request.paymentReference()).orElseThrow(PaymentRefNotFoundException::new);

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentAlreadyProcessedException();
        }

        if (payment.getBankAuthorizationId() == null) {
            throw new MissingBankAuthorizationException();
        }

        var bankVoidRequest = new BankVoidRequest();

        bankVoidRequest.setAuthorizationId(payment.getBankAuthorizationId());

        var bankResponse = mockBankClient.voidAuthorization(bankVoidRequest, idempotencyKey);

        payment.setStatus(PaymentStatus.VOIDED);
        payment.setBankVoidId(bankResponse.getVoidId());
        payment.setVoidedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        var savedPayment = paymentRepository.save(payment);

        return new VoidResponse(
                savedPayment.getPaymentReference(),
                savedPayment.getStatus(),
                savedPayment.getVoidedAt()
        );

    }


    public RefundResponse refundPayment(RefundRequest request, String idempotencyKey) {
        var payment = paymentRepository.findByPaymentReference(request.paymentReference()).orElseThrow(PaymentRefNotFoundException::new);

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new PaymentNotRefundableException();
        }

        if (payment.getBankCaptureId() == null) {
            throw new MissingBankCaptureException();
        }

        var bankRefundRequest = new BankRefundRequest();

        bankRefundRequest.setCaptureId(payment.getBankCaptureId());
        bankRefundRequest.setAmount(payment.getAmountInCents());

        var bankResponse = mockBankClient.refund(bankRefundRequest, idempotencyKey);

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setBankRefundId(bankResponse.getRefundId());
        payment.setRefundedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        var savedPayment = paymentRepository.save(payment);

        return new RefundResponse(
                savedPayment.getPaymentReference(),
                savedPayment.getStatus(),
                savedPayment.getAmountInCents(),
                savedPayment.getCurrency(),
                "Payment has been refunded successfully"
        );
    }


    private String generateAuthorizationRequestHash(AuthorizeRequest request) {
        String normalizedRequest = String.join(
                "|",
                request.getOrderId().trim(),
                request.getCustomerId().trim(),
                request.getAmountInCents().toString(),
                request.getCurrency().trim().toUpperCase()
        );

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = messageDigest.digest(
                    normalizedRequest.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 hashing algorithm is not available",
                    ex
            );
        }
    }

    public PaymentResponse getPaymentByReference(String paymentReference) {
        var payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentRefNotFoundException(
                        "Payment not found with reference: " + paymentReference
                ));

        return new PaymentResponse(
                payment.getPaymentReference(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmountInCents(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getAuthorizedAt(),
                payment.getCapturedAt(),
                payment.getVoidedAt(),
                payment.getRefundedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
