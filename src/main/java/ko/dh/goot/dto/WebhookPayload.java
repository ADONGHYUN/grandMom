package ko.dh.goot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WebhookPayload {

    private String type;
    private String timestamp;
    private Data data;

    @Getter
    @NoArgsConstructor
    public static class Data {
        private String paymentId;
        private String transactionId;
        private String storeId;
    }
}
