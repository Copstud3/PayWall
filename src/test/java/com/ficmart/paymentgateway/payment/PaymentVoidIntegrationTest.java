package com.ficmart.paymentgateway.payment;

import com.ficmart.paymentgateway.payment.api.dto.AuthorizeRequest;
import com.ficmart.paymentgateway.payment.api.dto.VoidRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.MockBankClient;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankAuthorizeResponse;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankVoidRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankVoidResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentVoidIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private MockBankClient mockBankClient;

    @BeforeEach
    void setUpBankResponses() {
        when(mockBankClient.authorize(any(), anyString()))
                .thenAnswer(invocation -> {
                    var response = new BankAuthorizeResponse();

                    response.setAuthorizationId(
                            "auth-" + UUID.randomUUID()
                    );
                    response.setStatus("authorized");

                    return response;
                });

        when(mockBankClient.voidAuthorization(any(), anyString()))
                .thenAnswer(invocation -> {
                    var request = invocation.getArgument(
                            0,
                            BankVoidRequest.class
                    );

                    var response = new BankVoidResponse();

                    response.setAuthorizationId(
                            request.getAuthorizationId()
                    );
                    response.setVoidId(
                            "void-" + UUID.randomUUID()
                    );
                    response.setStatus("voided");

                    return response;
                });
    }

    private String authorizePayment() throws Exception {
        var request = new AuthorizeRequest();

        request.setOrderId(
                "order-" + UUID.randomUUID()
        );
        request.setCustomerId(
                "customer-" + UUID.randomUUID()
        );
        request.setAmountInCents(5000L);
        request.setCurrency("USD");
        request.setCardNumber("4111111111111111");
        request.setCvv("123");
        request.setExpiryMonth(12);
        request.setExpiryYear(2030);

        var response = mockMvc.perform(
                        post("/api/v1/payments/authorize")
                                .header(
                                        "Idempotency-Key",
                                        "authorize-" + UUID.randomUUID()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("AUTHORIZED")
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        return jsonMapper
                .readTree(response)
                .get("paymentReference")
                .asString();
    }

    private ResultActions voidPayment(
            String paymentReference,
            String idempotencyKey
    ) throws Exception {
        var request = new VoidRequest(paymentReference);

        return mockMvc.perform(
                post("/api/v1/payments/void")
                        .header(
                                "Idempotency-Key",
                                idempotencyKey
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                jsonMapper.writeValueAsString(request)
                        )
        );
    }

    @Test
    void shouldVoidAuthorizedPaymentSuccessfully()
            throws Exception {

        var paymentReference = authorizePayment();
        var idempotencyKey =
                "void-" + UUID.randomUUID();

        voidPayment(paymentReference, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("VOIDED")
                )
                .andExpect(
                        jsonPath("$.paymentReference")
                                .value(paymentReference)
                )
                .andExpect(
                        jsonPath("$.voidedAt")
                                .exists()
                );
    }

    @Test
    void shouldReturnStoredResponseWhenVoidRequestIsRetried()
            throws Exception {

        var paymentReference = authorizePayment();
        var idempotencyKey =
                "void-retry-" + UUID.randomUUID();

        voidPayment(paymentReference, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("VOIDED")
                )
                .andExpect(
                        jsonPath("$.paymentReference")
                                .value(paymentReference)
                );

        voidPayment(paymentReference, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("VOIDED")
                )
                .andExpect(
                        jsonPath("$.paymentReference")
                                .value(paymentReference)
                );
    }

    @Test
    void shouldReturnConflictWhenVoidIdempotencyKeyIsReusedForDifferentPayment()
            throws Exception {

        var firstPaymentReference = authorizePayment();
        var secondPaymentReference = authorizePayment();

        var idempotencyKey =
                "void-conflict-" + UUID.randomUUID();

        voidPayment(firstPaymentReference, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("VOIDED")
                )
                .andExpect(
                        jsonPath("$.paymentReference")
                                .value(firstPaymentReference)
                );

        voidPayment(secondPaymentReference, idempotencyKey)
                .andExpect(status().isConflict());
    }
}