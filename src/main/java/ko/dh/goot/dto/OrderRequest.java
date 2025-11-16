package ko.dh.goot.dto;

public class OrderRequest {
    private Long productId;
    private Integer quantity;
    private String receiver; 
    private String phone;
    private String address;
    private String memo;
    private String orderName;  
    // totalAmount는 서버에서 재계산해야 안전하지만, 클라이언트가 보내주는 값도 받아서 참고할 수 있음
    private Integer clientTotalAmount;
    
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	public String getReceiver() {
		return receiver;
	}
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getMemo() {
		return memo;
	}
	public void setMemo(String memo) {
		this.memo = memo;
	}
	public String getOrderName() {
		return orderName;
	}
	public void setOrderName(String orderName) {
		this.orderName = orderName;
	}
	public Integer getClientTotalAmount() {
		return clientTotalAmount;
	}
	public void setClientTotalAmount(Integer clientTotalAmount) {
		this.clientTotalAmount = clientTotalAmount;
	} 

    
}