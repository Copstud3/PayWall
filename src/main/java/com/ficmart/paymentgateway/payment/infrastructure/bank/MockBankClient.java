package com.ficmart.paymentgateway.payment.infrastructure.bank;

import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankAuthorizeRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankAuthorizeResponse;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankErrorResponse;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankAuthorizationException;
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
}