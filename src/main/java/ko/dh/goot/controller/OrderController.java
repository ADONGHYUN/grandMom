package ko.dh.goot.controller;


import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ko.dh.goot.dto.OrderRequest;
import ko.dh.goot.dto.OrderResponse;
import ko.dh.goot.dto.Product;
import ko.dh.goot.service.OrderService;
import ko.dh.goot.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
@Log4j2
@Controller
@RequiredArgsConstructor
@RequestMapping("order")
public class OrderController {
	
	@Value("${portone.store-id}")
    private String storeId;
	
	@Value("${portone.channel-key}")
    private String kakaoChannelKey;
	
	private final ProductService productService;
	private final OrderService orderService;
	//private final PaymentService paymentService;
	
	 // 주문 페이지로 이동
    @GetMapping("/detail")
    public String orderPage(@RequestParam("productId") int productId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            Model model) {
    	System.out.println("주문상세로 이동");
        Product product = productService.selectProductById(productId);
        model.addAttribute("product", product);
        model.addAttribute("quantity", quantity);
        model.addAttribute("storeId", storeId);
        model.addAttribute("kakaoChannelKey", kakaoChannelKey);
        System.out.println("product::");
        System.out.println(product);
        return "order/orderDetail"; // order.html 템플릿 렌더링
    }

    @PostMapping("/prepareOrder")
    public ResponseEntity<Map<String, Object>> prepareOrder(@RequestBody OrderRequest orderRequest) {
        // ⚠️ [보안 필수] 실제로는 세션이나 Spring Security를 통해 userId를 가져와야 함
        String currentUserId = "user-1234"; // 임시 사용자 ID

        try {
            // 💡 Service 호출: 금액 재계산, DB 저장, orderId 반환
        	OrderResponse response = orderService.prepareOrder(orderRequest, currentUserId);

            // 클라이언트에게 orderId와 서버 확정 금액을 반환
            return ResponseEntity.ok(Map.of(
                "orderId", response.getOrderId(),
                "expectedAmount", response.getExpectedAmount() 
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 재고 부족, 상품 없음 등의 비즈니스 로직 에러
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            // 기타 서버 에러
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "주문 생성 중 알 수 없는 서버 오류가 발생했습니다."
            ));
        }
    }
    
    // ✅ 포트원에서 결제 완료 후 호출
    @PostMapping("/completePayment")
    public ResponseEntity<?> completePayment(@RequestBody Map<String, Object> payload) {
        try {
        	System.out.println("/complete 호출");
            String paymentId = (String) payload.get("paymentId");
            Object orderIdObj = payload.get("orderId");

            if(paymentId == null || orderIdObj == null) {
            	System.out.println("completePayment null오류");
            }
            
            Long orderId;
            if (orderIdObj instanceof Integer) {
                orderId = ((Integer) orderIdObj).longValue();
            
            // 2. JSON 파서가 Long으로 파싱한 경우 (값이 클 때)
            } else if (orderIdObj instanceof Long) {
                orderId = (Long) orderIdObj;

            // 3. String 등 예상치 못한 타입으로 온 경우 (매우 드물지만 안전 대비)
            } else {
                throw new IllegalArgumentException("주문 ID의 형식이 올바르지 않습니다.");
            }
            
            orderService.verifyPayment(paymentId, orderId);
            return ResponseEntity.ok().body(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", e.getMessage()));
        }
    }
}
