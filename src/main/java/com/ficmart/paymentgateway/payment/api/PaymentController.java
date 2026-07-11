package com.ficmart.paymentgateway.payment.api;

import com.ficmart.paymentgateway.payment.api.dto.AuthorizeRequest;
import com.ficmart.paymentgateway.payment.api.dto.AuthorizeResponse;
import com.ficmart.paymentgateway.payment.api.dto.CaptureRequest;
import com.ficmart.paymentgateway.payment.api.dto.CaptureResponse;
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
    public ResponseEntity<CaptureResponse> capture(@RequestBody CaptureRequest request) {
        var payment = paymentService.capturePayment(request);
        return ResponseEntity.ok(payment);
    }
}
