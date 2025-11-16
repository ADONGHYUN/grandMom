package ko.dh.goot.dao;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.dto.Order;

@Mapper
public interface OrderMapper {

	int insertOrder(Order order);
	int selectOrderExpectedAmount(Long orderId);


}
