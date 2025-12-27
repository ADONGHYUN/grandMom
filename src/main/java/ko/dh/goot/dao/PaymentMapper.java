package ko.dh.goot.dao;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.dto.Payment;
import ko.dh.goot.dto.PortOnePaymentResponse;

@Mapper
public interface PaymentMapper {
	int existsByPaymentId(String paymentId);
    void insertPayment(PortOnePaymentResponse portonePaymentDetails);
    Payment selectByOrderId(Long orderId);
    void updatePaymentStatus(Payment payment);
	
}