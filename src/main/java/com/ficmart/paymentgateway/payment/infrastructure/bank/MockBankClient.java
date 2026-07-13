package com.ficmart.paymentgateway.payment.infrastructure.bank;

import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.*;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class MockBankClient {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public BankAuthorizeResponse authorize(
            BankAuthorizeRequest request,
            String idempotencyKey
    ) {
        try {
            var response = postToBank(
                    "/api/v1/authorizations",
                    request,
                    idempotencyKey,
                    BankAuthorizeResponse.class
            );

            if (!"approved".equalsIgnoreCase(response.getStatus())) {
                throw new BankAuthorizationException(
                        null,
                        "authorization_rejected",
                        "The bank rejected the authorization request"
                );
            }

            return response;

        } catch (BankOperationException ex) {
            throw new BankAuthorizationException(
                    ex.getStatus(),
                    ex.getErrorCode(),
                    ex.getMessage(),
                    ex
            );
        }
    }

    public BankCaptureResponse capture(
            BankCaptureRequest request,
            String idempotencyKey
    ) {
        var response = postToBank(
                "/api/v1/captures",
                request,
                idempotencyKey,
                BankCaptureResponse.class
        );

        if (!"captured".equalsIgnoreCase(response.getStatus())) {
            throw new BankCaptureException(
                    null,
                    "capture_rejected",
                    "The bank rejected the capture"
            );
        }

        return response;
    }

    public BankVoidResponse voidAuthorization(BankVoidRequest bankRequest, String idempotencyKey) {
        var response = postToBank(
                "/api/v1/voids",
                bankRequest,
                idempotencyKey,
                BankVoidResponse.class
        );

        if (!"voided".equalsIgnoreCase(response.getStatus())) {
            throw new BankVoidException(
                    null,
                    "void_rejected",
                    "The bank rejected the void request"
            );
        }

        return response;
    }

    public BankRefundResponse refund (BankRefundRequest request, String idempotencyKey) {
        var response = postToBank(
                "/api/v1/refunds",
                request,
                idempotencyKey,
                BankRefundResponse.class
        );

        if (!"refunded".equalsIgnoreCase(response.getStatus())) {
            throw new BankRefundException(
                    null,
                    "refund rejected",
                    "The bank rejected the refund request"
            );
        }

        return response;
    }

    private <T> T postToBank(
            String endpoint,
            Object request,
            String idempotencyKey,
            Class<T> responseType
    ) {
        try {
            var response = restClient
                    .post()
                    .uri(endpoint)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (httpRequest, httpResponse) -> {
                                var bankError = jsonMapper.readValue(
                                        httpResponse.getBody(),
                                        BankErrorResponse.class
                                );

                                throw new BankOperationException(
                                        httpResponse.getStatusCode(),
                                        bankError.getError(),
                                        bankError.getMessage()
                                );
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (httpRequest, httpResponse) -> {
                                throw new BankCommunicationException(
                                        "The mock bank encountered an internal error"
                                );
                            }
                    )
                    .body(responseType);

            if (response == null) {
                throw new BankCommunicationException(
                        "The mock bank returned an empty response"
                );
            }

            return response;

        } catch (BankOperationException | BankCommunicationException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new BankCommunicationException(
                    "The mock bank is currently unavailable",
                    ex
            );
        }
    }
}