package com.ficmart.paymentgateway.payment.api;

import com.ficmart.paymentgateway.payment.api.dto.*;
import com.ficmart.paymentgateway.payment.application.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Payments",
        description = "APIs for authorizing, capturing, voiding, refunding and retrieving payments."
)
@RequestMapping("/api/v1/payments")
@RestController
@AllArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;


    @Operation(
            summary = "Authorize a payment",
            description = "Creates a payment authorization with the acquiring bank."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment authorized successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Bank unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/authorize")
    public ResponseEntity<AuthorizeResponse> authorize(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AuthorizeRequest request
    ) {
        var payment = paymentService.authorisePayment(request, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

    @Operation(
            summary = "Capture a payment",
            description = "Captures a previously authorized payment through the acquiring bank."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment captured successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Payment cannot be captured",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Bank unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/capture")
    public ResponseEntity<CaptureResponse> capture(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CaptureRequest request
    ) {
        var payment = paymentService.capturePayment(request, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

    @Operation(
            summary = "Void an authorized payment",
            description = "Voids a previously authorized payment before it has been captured."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment voided successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Payment cannot be voided in its current state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Bank unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/void")
    public ResponseEntity<VoidResponse> voidPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody VoidRequest request
    ) {
        var payment = paymentService.voidPayment(request, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

    @Operation(
            summary = "Refund a captured payment",
            description = "Refunds a previously captured payment through the acquiring bank."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment refunded successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Payment cannot be refunded in its current state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Bank unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refundPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RefundRequest request
    ) {
        var payment = paymentService.refundPayment(request, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

    @Operation(
            summary = "Retrieve a payment",
            description = "Returns the details and current status of a payment using its payment reference."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment retrieved successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{paymentReference}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable String paymentReference
    ) {
        var payment = paymentService.getPaymentByReference(paymentReference);
        return ResponseEntity.ok(payment);
    }

    @Operation(
            summary = "Retrieve payments by order ID",
            description = "Returns all payments associated with the specified order ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(
            @PathVariable String orderId
    ) {
        var payments = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }
}