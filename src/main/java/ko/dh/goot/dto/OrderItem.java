package ko.dh.goot.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderItem {

    private Long orderItemId;

    private Long orderId;
    private Long productId;

    private String productName;
    private int productPrice;
    private int quantity;
    private int totalPrice;

    /**
     * JSON 문자열 그대로 저장
     * 예: {"color":"black","size":"L"}
     */
    private String optionInfo;

    /**
     * NONE, REQUESTED, PARTIAL, REFUNDED, FAILED
     */
    private String refundStatus;

    @Builder
    public OrderItem(
            Long orderId,
            Long productId,
            String productName,
            int productPrice,
            int quantity,
            int totalPrice,
            String optionInfo,
            String refundStatus
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.optionInfo = optionInfo;
        this.refundStatus = refundStatus;
    }
}
