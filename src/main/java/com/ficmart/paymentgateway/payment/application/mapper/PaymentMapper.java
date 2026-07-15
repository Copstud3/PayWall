package com.ficmart.paymentgateway.payment.application.mapper;

import com.ficmart.paymentgateway.payment.api.dto.*;
import com.ficmart.paymentgateway.payment.domain.Payment;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "amount", source = "amountInCents")
    @Mapping(target = "cardNumber", source = "cardNumber")
    @Mapping(target = "cvv", source = "cvv")
    @Mapping(target = "expiryMonth", source = "expiryMonth")
    @Mapping(target = "expiryYear", source = "expiryYear")
    BankAuthorizeRequest toBankAuthorizeRequest(AuthorizeRequest request);

    @Mapping(target = "authorizationId", source = "bankAuthorizationId")
    @Mapping(target = "amount", source = "amountInCents")
    BankCaptureRequest toBankCaptureRequest(Payment payment);

    @Mapping(target = "authorizationId", source = "bankAuthorizationId")
    BankVoidRequest toBankVoidRequest(Payment payment);

    @Mapping(target = "captureId", source = "bankCaptureId")
    @Mapping(target = "amount", source = "amountInCents")
    BankRefundRequest toBankRefundRequest(Payment payment);

    default AuthorizeResponse toAuthorizeResponse(
            Payment payment,
            String message
    ) {
        return new AuthorizeResponse(
                payment.getPaymentReference(),
                payment.getStatus(),
                payment.getAmountInCents(),
                payment.getCurrency(),
                message
        );
    }

    default CaptureResponse toCaptureResponse(
            Payment payment,
            String message
    ) {
        return new CaptureResponse(
                payment.getPaymentReference(),
                payment.getStatus(),
                payment.getAmountInCents(),
                payment.getCurrency(),
                message
        );
    }

    default VoidResponse toVoidResponse(
            Payment payment
    ) {
        return new VoidResponse(
                payment.getPaymentReference(),
                payment.getStatus(),
                payment.getVoidedAt()
        );
    }

    default RefundResponse toRefundResponse(
            Payment payment,
            String message
    ) {
        return new RefundResponse(
                payment.getPaymentReference(),
                payment.getStatus(),
                payment.getAmountInCents(),
                payment.getCurrency(),
                message
        );
    }

    default PaymentResponse toPaymentResponse(Payment payment) {
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
                payment.getCapturedAt(),
                payment.getRefundedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}