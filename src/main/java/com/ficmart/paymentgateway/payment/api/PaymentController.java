package com.ficmart.paymentgateway.payment.api;

import com.ficmart.paymentgateway.payment.api.dto.*;
import com.ficmart.paymentgateway.payment.application.PaymentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/payments")
@RestController
@AllArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizeResponse> authorize(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody AuthorizeRequest request) {
        var payment = paymentService.authorisePayment(request, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/capture")
    public ResponseEntity<CaptureResponse> capture(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody CaptureRequest request) {
        var payment = paymentService.capturePayment(request, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/void")
    public ResponseEntity<VoidResponse> voidPayment(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody VoidRequest request) {
        var payment = paymentService.voidPayment(request, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refundPayment(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody RefundRequest request) {
        var payment = paymentService.refundPayment(request, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

}
