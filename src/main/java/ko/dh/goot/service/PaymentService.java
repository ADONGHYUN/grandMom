package ko.dh.goot.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.dao.OrderMapper;
import ko.dh.goot.dao.PaymentMapper;
import ko.dh.goot.dto.Payment;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    
    private final OrderMapper orderMapper;

    @Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.api-secret}")
    private String apiSecret;

    public String getAccessToken() {
        // 1. URL을 새로운 로그인/인증 경로로 변경
        String tokenUrl = "https://api.portone.io/login/api-secret"; 
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 2. V2 규격: store_id와 api_secret을 바디에 담아 보냅니다.
        Map<String, String> body = new HashMap<>();
        body.put("storeId", storeId);     // store_id  -> storeId 로 변경 (추정)
        body.put("apiSecret", apiSecret);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, entity, String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.getBody());
            
            // 3. 응답 구조 확인 후 토큰 추출 (응답 필드명이 다를 수 있음)
            // 이 경로의 응답은 'access_token'을 최상위 또는 'data' 노드 아래에 바로 포함할 수 있습니다.
            JsonNode accessTokenNode = json.get("accessToken");
            
            if (accessTokenNode == null) {
                // 혹시 토큰 발급은 성공했는데 필드명이 바뀌거나 응답이 비정상일 경우의 안전 장치
                 throw new RuntimeException("PortOne 응답에서 'accessToken' 필드를 찾을 수 없습니다. 응답: " + response.getBody());
            }
            
            return accessTokenNode.asText(); 
            
        } catch (Exception e) {
            // 기존 예외 처리 유지
            System.err.println("액세스 토큰 발급 실패: " + e.getMessage());
            throw new RuntimeException("액세스 토큰 발급 중 오류 발생", e);
        }
    }
    
    
    public String createKakaoPayReady(Long orderId, int amount) {
        
        // 1. 액세스 토큰 발급
        String accessToken = getAccessToken(); 
        
        System.out.println("accessToken ::");
        System.out.println(accessToken);
        // 2. URL 설정: 404/405 문제를 해결했던 경로를 사용합니다.
        String url = "https://api.portone.io/payments/ready"; 
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 발급받은 토큰을 Authorization: Bearer {Token} 형태로 설정
        headers.set("Authorization", "Bearer " + accessToken); 

        // 3. 요청 본문 (Body) 설정 - 일반 V2 API는 카멜 케이스 필드명을 사용했을 가능성이 높습니다.
        Map<String, Object> body = new HashMap<>();
        body.put("storeId", storeId);
        body.put("channelKey", "kakaopay"); 
        body.put("orderName", "테스트 상품");
        body.put("orderId", String.valueOf(orderId));
        body.put("totalAmount", amount);
        body.put("currency", "KRW");
        body.put("redirectUrl", "http://localhost:8080/payment/approve");
        body.put("webhookUrl", "http://localhost:8080/payment/webhook");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            System.out.println("결제준비 요청 시작");
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("결제준비 응답 결과: " + response.getBody());
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.getBody());

            // 응답 JSON 구조 확인 후 필드 추출
            JsonNode responseNode = json.get("response"); // V1/V2 일반 API 응답은 'response' 노드를 가질 수 있음

            String redirectUrl = responseNode.get("next_redirect_pc_url").asText();
            String tid = responseNode.get("transactionId").asText(); // PortOne에서 사용하는 결제 고유 번호

            // 4. DB 저장
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setPaymentMethod("KAKAOPAY");
            payment.setPaymentStatus("READY");
            payment.setTid(tid);
            payment.setAmount(amount);
            payment.setCancelAmount(0);
            payment.setCreatedAt(LocalDateTime.now());

            paymentMapper.insertPayment(payment);

            return redirectUrl;
            
        } catch (Exception e) {
            System.err.println("결제 준비 중 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("결제 준비 중 오류 발생: " + e.getMessage(), e);
        }
    }


    public void handlePaymentWebhook(Map<String, Object> payload) {
        try {
            // 페이로드 구조는 문서 보면서 확인
            String status = (String) payload.get("status");
            String orderIdStr = (String) payload.get("orderId");

            if ("PAID".equalsIgnoreCase(status) && orderIdStr != null) {
                Long orderId = Long.valueOf(orderIdStr);
                Payment existing = paymentMapper.selectByOrderId(orderId);
                if (existing != null) {
                    existing.setPaymentStatus("PAID");
                    existing.setApprovedAt(LocalDateTime.now());
                    paymentMapper.updatePaymentStatus(existing);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Webhook 처리 오류: " + e.getMessage(), e);
        }
    }

    
}
