package com.ficmart.paymentgateway.payment;

import com.ficmart.paymentgateway.payment.api.dto.AuthorizeRequest;
import com.ficmart.paymentgateway.payment.api.dto.CaptureRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.MockBankClient;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankAuthorizeResponse;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankCaptureRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankCaptureResponse;
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
class PaymentCaptureIntegrationTest {

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

        when(mockBankClient.capture(any(), anyString()))
                .thenAnswer(invocation -> {
                    var request = invocation.getArgument(
                            0,
                            BankCaptureRequest.class
                    );

                    var response = new BankCaptureResponse();

                    response.setCaptureId(
                            "capture-" + UUID.randomUUID()
                    );
                    response.setAuthorizationId(
                            request.getAuthorizationId()
                    );
                    response.setAmount(
                            request.getAmount()
                    );
                    response.setCurrency("USD");
                    response.setStatus("captured");

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

    private ResultActions capturePayment(
            String paymentReference,
            String idempotencyKey
    ) throws Exception {
        var request = new CaptureRequest(paymentReference);

        return mockMvc.perform(
                post("/api/v1/payments/capture")
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
    void shouldCaptureAuthorizedPaymentSuccessfully()
            throws Exception {

        var paymentReference = authorizePayment();
        var idempotencyKey =
                "capture-" + UUID.randomUUID();

        capturePayment(paymentReference, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("CAPTURED")
                )
                .andExpect(
                        jsonPath("$.paymentReference")
                                .value(paymentReference)
                )
                .andExpect(
                        jsonPath("$.amountInCents")
                                .value(5000)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                );
    }

    @Test
    void shouldReturnStoredResponseWhenCaptureRequestIsRetried()
            throws Exception {

        var paymentReference = authorizePayment();
        var idempotencyKey =
                "capture-retry-" + UUID.randomUUID();

        capturePayment(paymentReference, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("CAPTURED")
                )
                .andExpect(
                        jsonPath("$.paymentReference")
                                .value(paymentReference)
                );

        capturePayment(paymentReference, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("CAPTURED")
                )
                .andExpect(
                        jsonPath("$.paymentReference")
                                .value(paymentReference)
                );
    }

    @Test
    void shouldReturnConflictWhenCaptureIdempotencyKeyIsReusedForDifferentPayment()
            throws Exception {

        var firstPaymentReference = authorizePayment();
        var secondPaymentReference = authorizePayment();

        var captureIdempotencyKey =
                "capture-conflict-" + UUID.randomUUID();

        capturePayment(
                firstPaymentReference,
                captureIdempotencyKey
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("CAPTURED")
                )
                .andExpect(
                        jsonPath("$.paymentReference")
                                .value(firstPaymentReference)
                );

        capturePayment(
                secondPaymentReference,
                captureIdempotencyKey
        )
                .andExpect(status().isConflict());
    }
}