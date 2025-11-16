package ko.dh.goot.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.dao.OrderMapper;
import ko.dh.goot.dao.ProductMapper;
import ko.dh.goot.dto.Order;
import ko.dh.goot.dto.OrderRequest;
import ko.dh.goot.dto.OrderResponse;
import ko.dh.goot.dto.Product;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	
	@Value("${portone.api-secret}")
    private String apiSecret;
	
	private final ProductMapper productMapper;
	
	private final OrderMapper orderMapper;

	public OrderResponse prepareOrder(OrderRequest orderRequest, String currentUserId) {

		Product product = productMapper.selectProductById(orderRequest.getProductId());
        
        if (product == null) {
            throw new IllegalArgumentException("상품 정보가 존재하지 않습니다.");
        }
        if (product.getStock() < orderRequest.getQuantity()) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }
        
        int serverCalculatedAmount = product.getPrice() * orderRequest.getQuantity();
        
        Order order = Order.builder()
                .userId(currentUserId)
                .orderName(orderRequest.getOrderName())
                .totalAmount(serverCalculatedAmount)
                .orderStatus("PAYMENT_READY")
                .receiverName(orderRequest.getReceiver())
                .receiverPhone(orderRequest.getPhone())
                .receiverAddress(orderRequest.getAddress())
                .deliveryMemo(orderRequest.getMemo())
                .build();
        
        int rowCount = orderMapper.insertOrder(order);

        if (rowCount != 1) {
            // 💡 주문 저장이 실패했으므로 예외 발생 및 트랜잭션 롤백 유도
            throw new IllegalStateException("주문 데이터 저장에 실패했습니다. 영향 받은 행: " + rowCount);
        }
        
		return new OrderResponse(order.getOrderId(), serverCalculatedAmount);
	}
	
	public void verifyPayment(String paymentId, Long orderId) {
        RestTemplate restTemplate = new RestTemplate();
        
        try {
            String url = "https://api.portone.io/payments/" + paymentId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "PortOne " + apiSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            // 2. PG사 API 호출 및 응답 받기
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            // 3. JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode paymentData = mapper.readTree(response.getBody());
            
            // 4. 결제 상태 및 금액 추출
            String status = paymentData.get("status").asText();
            
            // ⚠️ 주의: PG사 응답 구조에 따라 'amount' 노드의 유효성을 먼저 확인해야 합니다.
            JsonNode totalAmountNode = paymentData.at("/amount/total");
            if (!totalAmountNode.isInt() && !totalAmountNode.isTextual()) {
                throw new IllegalStateException("PG 응답에서 결제 금액('amount/total')을 찾을 수 없습니다.");
            }
            int totalAmount = totalAmountNode.asInt();

            // 5. DB에 저장된 예상 금액 조회
            int expectedAmount = orderMapper.selectOrderExpectedAmount(orderId);

            // 6. 금액 불일치 검증 (가장 중요한 보안 로직)
            if (totalAmount != expectedAmount) {
                // 💡 PG사에는 성공했으나, 금액이 다르면 결제를 취소해야 합니다.
                // PortOne 취소 API를 호출하는 로직이 이 자리에 추가되어야 합니다.
                throw new IllegalStateException("결제 금액 불일치: PG 결제금액 (" + totalAmount + ") vs. DB 예상금액 (" + expectedAmount + "). 위조 의심.");
            }

            // 7. PG 상태 검증
            if (!"PAID".equals(status)) {
                // 💡 결제가 PAID 상태가 아니면 비즈니스 예외 발생
                throw new IllegalStateException("결제 승인 실패: PG사 응답 상태가 'PAID'가 아닙니다. 현재 상태: " + status);
            }

            // 8. 검증 완료 (후속 작업 진행 준비)
            System.out.println("결제 검증 성공 및 금액 일치 확인: " + paymentId);

        } catch (HttpClientErrorException e) {
            // PG사 API 호출 중 4xx (Bad Request, Unauthorized) 또는 5xx (Server Error) 발생
            throw new RuntimeException("PG사 통신 오류: " + e.getResponseBodyAsString(), e);
        } catch (JsonProcessingException e) {
            // JSON 파싱 오류
            throw new RuntimeException("PG 응답 JSON 파싱 실패", e);
        } catch (Exception e) {
            // 기타 모든 예외를 RuntimeException으로 감싸서 트랜잭션 롤백 유도
            throw new RuntimeException("결제 검증 중 예상치 못한 오류 발생: " + e.getMessage(), e);
        }
    }

}
