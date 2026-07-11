package com.ficmart.paymentgateway.payment.application;

import com.ficmart.paymentgateway.payment.api.dto.AuthorizeRequest;
import com.ficmart.paymentgateway.payment.api.dto.AuthorizeResponse;
import com.ficmart.paymentgateway.payment.api.dto.CaptureRequest;
import com.ficmart.paymentgateway.payment.api.dto.CaptureResponse;
import com.ficmart.paymentgateway.payment.application.exceptions.IdempotencyConflictException;
import com.ficmart.paymentgateway.payment.application.exceptions.PaymentAlreadyProcessedException;
import com.ficmart.paymentgateway.payment.application.exceptions.PaymentRefNotFoundException;
import com.ficmart.paymentgateway.payment.domain.Payment;
import com.ficmart.paymentgateway.payment.domain.PaymentStatus;
import com.ficmart.paymentgateway.payment.infrastructure.PaymentRepository;
import com.ficmart.paymentgateway.payment.infrastructure.bank.MockBankClient;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankAuthorizeRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankAuthorizationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MockBankClient mockBankClient;

    // Business logic to return a response when a payment is authorized
    public AuthorizeResponse authorisePayment(AuthorizeRequest request, String idempotencyKey) {
        var existingPayment =
                paymentRepository.findByIdempotencyKey(idempotencyKey);

        if (existingPayment.isPresent()) {
            var payment = existingPayment.get();

            var requestDoesNotMatch =
                            !payment.getOrderId().equals(request.getOrderId())
                            || !payment.getCustomerId().equals(request.getCustomerId())
                            || !payment.getAmountInCents().equals(request.getAmountInCents())
                            || !payment.getCurrency().equals(request.getCurrency());

            if (requestDoesNotMatch) {
                throw new IdempotencyConflictException();
            }

            return new AuthorizeResponse(
                    payment.getPaymentReference(),
                    payment.getStatus(),
                    payment.getAmountInCents(),
                    payment.getCurrency(),
                    "Existing authorization returned"
            );
        }


        var paymentRef = "PAY-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase();
        var payment = new Payment();

        payment.setPaymentReference(paymentRef);
        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmountInCents(request.getAmountInCents());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);

        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setIdempotencyKey(idempotencyKey);

        var savedPayment = paymentRepository.save(payment);

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

            savedPayment.setStatus(PaymentStatus.AUTHORIZED);
            savedPayment.setBankAuthorizationId(
                    bankResponse.getAuthorizationId()
            );
            savedPayment.setAuthorizedAt(LocalDateTime.now());
            savedPayment.setUpdatedAt(LocalDateTime.now());

            var authorizedPayment = paymentRepository.save(savedPayment);

            return new AuthorizeResponse(
                    authorizedPayment.getPaymentReference(),
                    authorizedPayment.getStatus(),
                    authorizedPayment.getAmountInCents(),
                    authorizedPayment.getCurrency(),
                    "Payment authorized successfully"
            );

        } catch (BankAuthorizationException ex) {
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setFailureReason(ex.getMessage());
            savedPayment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(savedPayment);

            throw ex;
        }


    }

    // Business logic for returning a response on capture
    public CaptureResponse capturePayment(CaptureRequest request) {
        var payment = paymentRepository.findByPaymentReference(request.paymentReference()).orElse(null);
        if (payment == null) {
            throw new PaymentRefNotFoundException();
        }
        if (!payment.getStatus().equals(PaymentStatus.AUTHORIZED)) {
            throw new PaymentAlreadyProcessedException();
        }
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setUpdatedAt(LocalDateTime.now());
        var savedPayment = paymentRepository.save(payment);

        return new CaptureResponse(
                savedPayment.getPaymentReference(),
                savedPayment.getStatus(),
                "Captured payment successfully"
        );
    }
}
