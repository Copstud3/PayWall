package com.ficmart.paymentgateway.payment.infrastructure.bank;

import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.*;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankAuthorizationException;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankCaptureException;
import lombok.RequiredArgsConstructor;
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
            var response = restClient
                    .post()
                    .uri("/api/v1/authorizations")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError()
                                    || status.is5xxServerError(),
                            (httpRequest, httpResponse) -> {

                                BankErrorResponse bankError =
                                        jsonMapper.readValue(
                                                httpResponse.getBody(),
                                                BankErrorResponse.class
                                        );

                                throw new BankAuthorizationException(
                                        httpResponse.getStatusCode(),
                                        bankError.getError(),
                                        bankError.getMessage()
                                );
                            }
                    )
                    .body(BankAuthorizeResponse.class);

            if (response == null) {
                throw new BankAuthorizationException(
                        null,
                        "empty_bank_response",
                        "The mock bank returned an empty response"
                );
            }
            if (!"approved".equalsIgnoreCase(response.getStatus())) {
                throw new BankAuthorizationException(
                        null,
                        "authorization_rejected",
                        "The bank rejected the authorization"
                );
            }

            return response;
        } catch (BankAuthorizationException ex) {
            throw ex;
        }  catch (Exception ex) {
            throw new BankAuthorizationException(
                    null,
                    "bank_unavailable",
                    "The mock bank is currently unavailable"
            );
        }
    }

    public BankCaptureResponse capture(BankCaptureRequest bankRequest, String idempotencyKey) {
        try {
            var response = restClient
                    .post()
                    .uri("/api/v1/captures")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bankRequest)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError()
                                    || status.is5xxServerError(),
                            (httpRequest, httpResponse) -> {

                                var bankError =
                                        jsonMapper.readValue(
                                                httpResponse.getBody(),
                                                BankErrorResponse.class
                                        );

                                throw new BankCaptureException(
                                        httpResponse.getStatusCode(),
                                        bankError.getError(),
                                        bankError.getMessage()
                                );
                            }
                    )
                    .body(BankCaptureResponse.class);

            if (response == null) {
                throw new BankCaptureException(
                        null,
                        "empty_bank_response",
                        "The mock bank returned an empty response"
                );
            }
            if (!"captured".equalsIgnoreCase(response.getStatus())) {
                throw new BankCaptureException(
                        null,
                        "capture_rejected",
                        "The bank rejected the capture"
                );
            }
            return response;
        } catch (BankCaptureException ex) {
            throw ex;
        }  catch (Exception ex) {
            throw new BankCaptureException(
                    null,
                    "bank_unavailable",
                    "The mock bank is currently unavailable"
            );
        }
    }
}