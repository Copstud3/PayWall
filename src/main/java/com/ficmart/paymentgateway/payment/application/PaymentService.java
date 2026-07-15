package com.ficmart.paymentgateway.payment.application;

import com.ficmart.paymentgateway.payment.api.dto.AuthorizeRequest;
import com.ficmart.paymentgateway.payment.api.dto.AuthorizeResponse;
import com.ficmart.paymentgateway.payment.api.dto.CaptureRequest;
import com.ficmart.paymentgateway.payment.api.dto.CaptureResponse;
import com.ficmart.paymentgateway.payment.api.dto.PaymentResponse;
import com.ficmart.paymentgateway.payment.api.dto.RefundRequest;
import com.ficmart.paymentgateway.payment.api.dto.RefundResponse;
import com.ficmart.paymentgateway.payment.api.dto.VoidRequest;
import com.ficmart.paymentgateway.payment.api.dto.VoidResponse;
import com.ficmart.paymentgateway.payment.application.exceptions.IdempotencyConflictException;
import com.ficmart.paymentgateway.payment.application.exceptions.MissingBankAuthorizationException;
import com.ficmart.paymentgateway.payment.application.exceptions.MissingBankCaptureException;
import com.ficmart.paymentgateway.payment.application.exceptions.PaymentAlreadyProcessedException;
import com.ficmart.paymentgateway.payment.application.exceptions.PaymentNotRefundableException;
import com.ficmart.paymentgateway.payment.application.exceptions.PaymentRefNotFoundException;
import com.ficmart.paymentgateway.payment.application.mapper.PaymentMapper;
import com.ficmart.paymentgateway.payment.domain.Payment;
import com.ficmart.paymentgateway.payment.domain.PaymentOperation;
import com.ficmart.paymentgateway.payment.domain.PaymentOperationStatus;
import com.ficmart.paymentgateway.payment.domain.PaymentOperationType;
import com.ficmart.paymentgateway.payment.domain.PaymentStatus;
import com.ficmart.paymentgateway.payment.infrastructure.PaymentOperationRepository;
import com.ficmart.paymentgateway.payment.infrastructure.PaymentRepository;
import com.ficmart.paymentgateway.payment.infrastructure.bank.MockBankClient;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankAuthorizationException;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankCommunicationException;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOperationRepository paymentOperationRepository;
    private final MockBankClient mockBankClient;
    private final JsonMapper jsonMapper;
    private final PaymentMapper paymentMapper;

    /*
     * Authorization still temporarily uses Payment.idempotencyKey.
     * The payment_operations record is also created so authorization
     * can later be fully migrated without changing the table again.
     */
    @Transactional(
            noRollbackFor = {
                    BankAuthorizationException.class,
                    BankCommunicationException.class
            }
    )
    public AuthorizeResponse authorisePayment(
            AuthorizeRequest request,
            String idempotencyKey
    ) {
        var requestHash = generateRequestHash(request);

        var existingOperation = findOperation(
                PaymentOperationType.AUTHORIZE,
                idempotencyKey
        );

        if (existingOperation != null) {
            validateRequestHash(existingOperation, requestHash);

            if (existingOperation.getStatus()
                    == PaymentOperationStatus.SUCCEEDED) {

                return readStoredResponse(
                        existingOperation,
                        AuthorizeResponse.class
                );
            }
        }

        Payment payment;

        var existingPayment =
                paymentRepository.findByIdempotencyKey(idempotencyKey);

        if (existingPayment.isPresent()) {
            payment = existingPayment.get();

            validateAuthorizationRequest(payment, request);

            /*
             * Recovery for an older authorization saved before its
             * payment operation was marked as successful.
             */
            if (payment.getStatus() == PaymentStatus.AUTHORIZED
                    && payment.getBankAuthorizationId() != null) {

                var response = createAuthorizeResponse(payment);

                if (existingOperation == null) {
                    existingOperation = createOperation(
                            payment,
                            PaymentOperationType.AUTHORIZE,
                            idempotencyKey,
                            requestHash
                    );
                }

                markOperationSucceeded(existingOperation, response);

                return response;
            }

            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new PaymentAlreadyProcessedException();
            }

        } else {
            payment = createPendingPayment(request, idempotencyKey);
        }

        var operation = existingOperation;

        if (operation == null) {
            operation = createOperation(
                    payment,
                    PaymentOperationType.AUTHORIZE,
                    idempotencyKey,
                    requestHash
            );
        }


        var bankRequest = paymentMapper.toBankAuthorizeRequest(request);

        try {
            var bankResponse = mockBankClient.authorize(
                    bankRequest,
                    idempotencyKey
            );

            var now = LocalDateTime.now();

            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setBankAuthorizationId(
                    bankResponse.getAuthorizationId()
            );
            payment.setAuthorizedAt(now);
            payment.setFailureReason(null);
            payment.setUpdatedAt(now);

            var savedPayment = paymentRepository.save(payment);

            var response = createAuthorizeResponse(savedPayment);

            markOperationSucceeded(operation, response);

            return response;

        } catch (BankCommunicationException exception) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setFailureReason(
                    "Authorization outcome unknown. Retry with the same idempotency key."
            );
            payment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            markOperationProcessing(
                    operation,
                    "Authorization outcome unknown. Retry with the same idempotency key."
            );

            throw exception;

        } catch (BankAuthorizationException exception) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(exception.getMessage());
            payment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            markOperationFailed(
                    operation,
                    exception.getErrorCode(),
                    exception.getMessage()
            );

            throw exception;
        }
    }

    @Transactional(
            noRollbackFor = {
                    BankCommunicationException.class,
                    BankOperationException.class
            }
    )
    public CaptureResponse capturePayment(
            CaptureRequest request,
            String idempotencyKey
    ) {
        var requestHash = generateRequestHash(request);

        var operation = findOperation(
                PaymentOperationType.CAPTURE,
                idempotencyKey
        );

        if (operation != null) {
            validateRequestHash(operation, requestHash);

            if (operation.getStatus()
                    == PaymentOperationStatus.SUCCEEDED) {

                return readStoredResponse(
                        operation,
                        CaptureResponse.class
                );
            }
        }

        var payment = findPayment(request.paymentReference());

        /*
         * Recovery scenario:
         * the payment was captured, but the operation was not marked
         * successful before the application stopped.
         */
        if (operation != null
                && payment.getStatus() == PaymentStatus.CAPTURED
                && payment.getBankCaptureId() != null) {

            var response = createCaptureResponse(payment);

            markOperationSucceeded(operation, response);

            return response;
        }

        validatePaymentForCapture(payment);

        if (operation == null) {
            operation = createOperation(
                    payment,
                    PaymentOperationType.CAPTURE,
                    idempotencyKey,
                    requestHash
            );
        }

        var bankRequest = paymentMapper.toBankCaptureRequest(payment);

        try {
            var bankResponse = mockBankClient.capture(
                    bankRequest,
                    idempotencyKey
            );

            var now = LocalDateTime.now();

            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setBankCaptureId(bankResponse.getCaptureId());
            payment.setCapturedAt(now);
            payment.setFailureReason(null);
            payment.setUpdatedAt(now);

            var savedPayment = paymentRepository.save(payment);

            var response = createCaptureResponse(savedPayment);

            markOperationSucceeded(operation, response);

            return response;

        } catch (BankCommunicationException exception) {
            markOperationProcessing(
                    operation,
                    "Capture outcome unknown. Retry with the same idempotency key."
            );

            throw exception;

        } catch (BankOperationException exception) {
            markOperationFailed(
                    operation,
                    exception.getErrorCode(),
                    exception.getMessage()
            );

            throw exception;
        }
    }

    @Transactional(
            noRollbackFor = {
                    BankCommunicationException.class,
                    BankOperationException.class
            }
    )
    public VoidResponse voidPayment(
            VoidRequest request,
            String idempotencyKey
    ) {
        var requestHash = generateRequestHash(request);

        var operation = findOperation(
                PaymentOperationType.VOID,
                idempotencyKey
        );

        if (operation != null) {
            validateRequestHash(operation, requestHash);

            if (operation.getStatus()
                    == PaymentOperationStatus.SUCCEEDED) {

                return readStoredResponse(
                        operation,
                        VoidResponse.class
                );
            }
        }

        var payment = findPayment(request.paymentReference());

        /*
         * Recovery scenario:
         * the payment was voided, but the operation response was not saved.
         */
        if (operation != null
                && payment.getStatus() == PaymentStatus.VOIDED
                && payment.getBankVoidId() != null) {

            var response = createVoidResponse(payment);

            markOperationSucceeded(operation, response);

            return response;
        }

        validatePaymentForVoid(payment);

        if (operation == null) {
            operation = createOperation(
                    payment,
                    PaymentOperationType.VOID,
                    idempotencyKey,
                    requestHash
            );
        }

        var bankRequest = paymentMapper.toBankVoidRequest(payment);

        try {
            var bankResponse = mockBankClient.voidAuthorization(
                    bankRequest,
                    idempotencyKey
            );

            var now = LocalDateTime.now();

            payment.setStatus(PaymentStatus.VOIDED);
            payment.setBankVoidId(bankResponse.getVoidId());
            payment.setVoidedAt(now);
            payment.setFailureReason(null);
            payment.setUpdatedAt(now);

            var savedPayment = paymentRepository.save(payment);

            var response = createVoidResponse(savedPayment);

            markOperationSucceeded(operation, response);

            return response;

        } catch (BankCommunicationException exception) {
            markOperationProcessing(
                    operation,
                    "Void outcome unknown. Retry with the same idempotency key."
            );

            throw exception;

        } catch (BankOperationException exception) {
            markOperationFailed(
                    operation,
                    exception.getErrorCode(),
                    exception.getMessage()
            );

            throw exception;
        }
    }

    @Transactional(
            noRollbackFor = {
                    BankCommunicationException.class,
                    BankOperationException.class
            }
    )
    public RefundResponse refundPayment(
            RefundRequest request,
            String idempotencyKey
    ) {
        var requestHash = generateRequestHash(request);

        var operation = findOperation(
                PaymentOperationType.REFUND,
                idempotencyKey
        );

        if (operation != null) {
            validateRequestHash(operation, requestHash);

            if (operation.getStatus()
                    == PaymentOperationStatus.SUCCEEDED) {

                return readStoredResponse(
                        operation,
                        RefundResponse.class
                );
            }
        }

        var payment = findPayment(request.paymentReference());

        /*
         * Recovery scenario:
         * the payment was refunded, but the operation response was not saved.
         */
        if (operation != null
                && payment.getStatus() == PaymentStatus.REFUNDED
                && payment.getBankRefundId() != null) {

            var response = createRefundResponse(payment);

            markOperationSucceeded(operation, response);

            return response;
        }

        validatePaymentForRefund(payment);

        if (operation == null) {
            operation = createOperation(
                    payment,
                    PaymentOperationType.REFUND,
                    idempotencyKey,
                    requestHash
            );
        }

        var bankRequest = paymentMapper.toBankRefundRequest(payment);

        try {
            var bankResponse = mockBankClient.refund(
                    bankRequest,
                    idempotencyKey
            );

            var now = LocalDateTime.now();

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setBankRefundId(bankResponse.getRefundId());
            payment.setRefundedAt(now);
            payment.setFailureReason(null);
            payment.setUpdatedAt(now);

            var savedPayment = paymentRepository.save(payment);

            var response = createRefundResponse(savedPayment);

            markOperationSucceeded(operation, response);

            return response;

        } catch (BankCommunicationException exception) {
            markOperationProcessing(
                    operation,
                    "Refund outcome unknown. Retry with the same idempotency key."
            );

            throw exception;

        } catch (BankOperationException exception) {
            markOperationFailed(
                    operation,
                    exception.getErrorCode(),
                    exception.getMessage()
            );

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(
            String paymentReference
    ) {
        var payment = findPayment(paymentReference);

        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(
            String orderId
    ) {
        return paymentRepository
                .findAllByOrderId(orderId)
                .stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }

    private Payment createPendingPayment(
            AuthorizeRequest request,
            String idempotencyKey
    ) {
        var now = LocalDateTime.now();

        var paymentReference = "PAY-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();

        var payment = new Payment();

        payment.setPaymentReference(paymentReference);
        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmountInCents(request.getAmountInCents());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        return paymentRepository.save(payment);
    }

    private Payment findPayment(String paymentReference) {
        return paymentRepository
                .findByPaymentReference(paymentReference)
                .orElseThrow(PaymentRefNotFoundException::new);
    }

    private PaymentOperation findOperation(
            PaymentOperationType operationType,
            String idempotencyKey
    ) {
        return paymentOperationRepository
                .findByOperationTypeAndIdempotencyKey(
                        operationType,
                        idempotencyKey
                )
                .orElse(null);
    }

    private PaymentOperation createOperation(
            Payment payment,
            PaymentOperationType operationType,
            String idempotencyKey,
            String requestHash
    ) {
        var now = LocalDateTime.now();

        var operation = new PaymentOperation();

        operation.setPayment(payment);
        operation.setOperationType(operationType);
        operation.setIdempotencyKey(idempotencyKey);
        operation.setRequestHash(requestHash);
        operation.setStatus(PaymentOperationStatus.PROCESSING);
        operation.setCreatedAt(now);
        operation.setUpdatedAt(now);

        return paymentOperationRepository.save(operation);
    }

    private void validateAuthorizationRequest(
            Payment payment,
            AuthorizeRequest request
    ) {
        var requestDoesNotMatch =
                !payment.getOrderId().equals(request.getOrderId())
                        || !payment.getCustomerId()
                        .equals(request.getCustomerId())
                        || !payment.getAmountInCents()
                        .equals(request.getAmountInCents())
                        || !payment.getCurrency()
                        .equalsIgnoreCase(request.getCurrency());

        if (requestDoesNotMatch) {
            throw new IdempotencyConflictException();
        }
    }

    private void validateRequestHash(
            PaymentOperation operation,
            String requestHash
    ) {
        if (!operation.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException();
        }
    }

    private void validatePaymentForCapture(Payment payment) {
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentAlreadyProcessedException();
        }

        if (payment.getBankAuthorizationId() == null) {
            throw new MissingBankAuthorizationException();
        }
    }

    private void validatePaymentForVoid(Payment payment) {
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentAlreadyProcessedException();
        }

        if (payment.getBankAuthorizationId() == null) {
            throw new MissingBankAuthorizationException();
        }
    }

    private void validatePaymentForRefund(Payment payment) {
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new PaymentNotRefundableException();
        }

        if (payment.getBankCaptureId() == null) {
            throw new MissingBankCaptureException();
        }
    }

    private AuthorizeResponse createAuthorizeResponse(
            Payment payment
    ) {
        return paymentMapper.toAuthorizeResponse(
                payment,
                "Payment Authorized successfully"
        );
    }

    private CaptureResponse createCaptureResponse(
            Payment payment
    ) {
        return paymentMapper.toCaptureResponse(
                payment,
                "Captured payment successfully"
        );
    }

    private VoidResponse createVoidResponse(Payment payment) {
        return paymentMapper.toVoidResponse(
                payment
        );
    }

    private RefundResponse createRefundResponse(
            Payment payment
    ) {
        return paymentMapper.toRefundResponse(
                payment,
                "Payment refunded successfully"
        );
    }

    private void markOperationSucceeded(
            PaymentOperation operation,
            Object response
    ) {
        operation.setStatus(PaymentOperationStatus.SUCCEEDED);
        operation.setResponseData(serializeResponse(response));
        operation.setErrorCode(null);
        operation.setErrorMessage(null);
        operation.setUpdatedAt(LocalDateTime.now());

        paymentOperationRepository.save(operation);
    }

    private void markOperationProcessing(
            PaymentOperation operation,
            String errorMessage
    ) {
        operation.setStatus(PaymentOperationStatus.PROCESSING);
        operation.setErrorCode("BANK_COMMUNICATION_ERROR");
        operation.setErrorMessage(errorMessage);
        operation.setUpdatedAt(LocalDateTime.now());

        paymentOperationRepository.save(operation);
    }

    private void markOperationFailed(
            PaymentOperation operation,
            String errorCode,
            String errorMessage
    ) {
        operation.setStatus(PaymentOperationStatus.FAILED);
        operation.setErrorCode(errorCode);
        operation.setErrorMessage(errorMessage);
        operation.setUpdatedAt(LocalDateTime.now());

        paymentOperationRepository.save(operation);
    }

    private String generateRequestHash(Object request) {
        try {
            var requestJson = jsonMapper.writeValueAsString(request);

            var digest = MessageDigest.getInstance("SHA-256");

            var hashBytes = digest.digest(
                    requestJson.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Failed to generate request hash",
                    exception
            );
        }
    }

    private String serializeResponse(Object response) {
        try {
            return jsonMapper.writeValueAsString(response);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Could not serialize payment operation response",
                    exception
            );
        }
    }

    private <T> T readStoredResponse(
            PaymentOperation operation,
            Class<T> responseType
    ) {
        if (operation.getResponseData() == null
                || operation.getResponseData().isBlank()) {

            throw new IllegalStateException(
                    "Successful payment operation has no saved response"
            );
        }

        try {
            return jsonMapper.readValue(
                    operation.getResponseData(),
                    responseType
            );
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Could not deserialize stored payment operation response",
                    exception
            );
        }
    }

    private PaymentResponse mapToPaymentResponse(
            Payment payment
    ) {
        return paymentMapper.toPaymentResponse(payment);
    }
}