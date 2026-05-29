package txnflow.paymentadapterservice.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static txnflow.paymentadapterservice.constant.RazorpayConstant.SIGNATURE_HEADER;
import txnflow.paymentadapterservice.service.RazorpayWebhookService;

@RestController
@RequestMapping("/public/razorpay")
@RequiredArgsConstructor
public class RazorpayController {

    private final RazorpayWebhookService razorpayWebhookService;

    @GetMapping(
            value = "/checkout",
            produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<Resource> checkoutPage() {

        Resource resource = new ClassPathResource("static/checkout.html");

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }

    @PostMapping("/webhooks")
    public ResponseEntity<Void> handleWebhook(
            @NotNull @RequestBody String payload,
            @NotNull @RequestHeader(SIGNATURE_HEADER) String signature
    ) {
        razorpayWebhookService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();

    }
}