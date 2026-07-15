package com.ficmart.paymentgateway.payment;

import com.ficmart.paymentgateway.payment.api.dto.AuthorizeRequest;
import com.ficmart.paymentgateway.payment.infrastructure.bank.MockBankClient;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.BankAuthorizeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MockBankClient mockBankClient;

    @BeforeEach
    void setUp() {
        var bankResponse = new BankAuthorizeResponse();

        bankResponse.setAuthorizationId(
                "auth-" + UUID.randomUUID()
        );
        bankResponse.setStatus("authorized");

        when(mockBankClient.authorize(any(), anyString()))
                .thenReturn(bankResponse);
    }

    @Test
    void shouldAuthorizePaymentSuccessfully() throws Exception {
        var uniqueValue = UUID.randomUUID().toString();

        var request = createValidAuthorizationRequest(
                "order-success-" + uniqueValue,
                "customer-success-" + uniqueValue,
                5000L
        );

        mockMvc.perform(
                        post("/api/v1/payments/authorize")
                                .header(
                                        "Idempotency-Key",
                                        "authorize-success-" + uniqueValue
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.amountInCents").value(5000))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void shouldReturnSamePaymentWhenAuthorizationIsRetriedWithSameIdempotencyKey()
            throws Exception {

        var uniqueValue = UUID.randomUUID().toString();

        var request = createValidAuthorizationRequest(
                "order-retry-" + uniqueValue,
                "customer-retry-" + uniqueValue,
                5000L
        );

        var idempotencyKey = "authorize-retry-" + uniqueValue;

        var firstResponse = mockMvc.perform(
                        post("/api/v1/payments/authorize")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var secondResponse = mockMvc.perform(
                        post("/api/v1/payments/authorize")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var firstPaymentReference = objectMapper
                .readTree(firstResponse)
                .get("paymentReference")
                .asString();

        var secondPaymentReference = objectMapper
                .readTree(secondResponse)
                .get("paymentReference")
                .asString();

        assertEquals(
                firstPaymentReference,
                secondPaymentReference
        );
    }

    @Test
    void shouldReturnConflictWhenAuthorizationIsRetriedWithSameIdempotencyKeyAndDifferentRequestBody()
            throws Exception {

        var uniqueValue = UUID.randomUUID().toString();

        var request = createValidAuthorizationRequest(
                "order-conflict-" + uniqueValue,
                "customer-conflict-" + uniqueValue,
                5000L
        );

        var idempotencyKey = "authorize-conflict-" + uniqueValue;

        mockMvc.perform(
                        post("/api/v1/payments/authorize")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        request.setAmountInCents(8000L);

        mockMvc.perform(
                        post("/api/v1/payments/authorize")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnBadRequestWhenIdempotencyKeyIsMissing()
            throws Exception {

        var uniqueValue = UUID.randomUUID().toString();

        var request = createValidAuthorizationRequest(
                "order-missing-header-" + uniqueValue,
                "customer-missing-header-" + uniqueValue,
                5000L
        );

        mockMvc.perform(
                        post("/api/v1/payments/authorize")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenAuthorizationRequestIsInvalid()
            throws Exception {

        var request = createValidAuthorizationRequest(
                "",
                "customer-invalid-" + UUID.randomUUID(),
                0L
        );

        mockMvc.perform(
                        post("/api/v1/payments/authorize")
                                .header(
                                        "Idempotency-Key",
                                        "authorize-invalid-"
                                                + UUID.randomUUID()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());
    }

    private AuthorizeRequest createValidAuthorizationRequest(
            String orderId,
            String customerId,
            Long amountInCents
    ) {
        var request = new AuthorizeRequest();

        request.setOrderId(orderId);
        request.setCustomerId(customerId);
        request.setAmountInCents(amountInCents);
        request.setCurrency("USD");
        request.setCardNumber("4111111111111111");
        request.setCvv("123");
        request.setExpiryMonth(12);
        request.setExpiryYear(2030);

        return request;
    }
}