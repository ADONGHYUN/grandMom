package ko.dh.goot.dao;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.dto.OrderItem;

@Mapper
public interface OrderItemMapper {

	int insertOrderItem(OrderItem orderItem);



}
