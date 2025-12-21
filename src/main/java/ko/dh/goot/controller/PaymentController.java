package ko.dh.goot.controller;


import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import ko.dh.goot.dto.PaymentReadyResponse;
import ko.dh.goot.service.PaymentService;

import org.springframework.ui.Model;


@Controller
@RequiredArgsConstructor
@RequestMapping("payment")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payment")
    public String paymentPage() {
    	System.out.println("paymentPage:::ㄴㄴㄴㄴ");
        return "payment/payment";
    }
 
    @PostMapping("/ready")
    public String kakaoPayReady(@RequestParam("amount") int amount) {
    	System.out.println("kakaoPayReady 맵핑");
    	long orderId = System.currentTimeMillis();
        String redirectUrl = paymentService.createKakaoPayReady(orderId, amount);
        return "redirect:" + redirectUrl;
    }

    @GetMapping("/approve")
    public String approve(@RequestParam(value = "orderId", required = false) String orderId,
                          @RequestParam(value = "transactionId", required = false) String transactionId,
                          Model model) {
        model.addAttribute("orderId", orderId);
        model.addAttribute("transactionId", transactionId);
        return "payment_complete";
    }

    @PostMapping("/handlePaymentWebhook")
    public ResponseEntity<?> handlePaymentWebhook(
    		@RequestBody String payload,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestHeader("webhook-timestamp") String webhookTimestamp){
    	
        paymentService.handlePaymentWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
        
        
        return ResponseEntity.ok(Map.of("message", "PG사 웹훅 처리 성공 및 주문 완료"));
    }
}