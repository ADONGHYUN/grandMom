package ko.dh.goot.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.dto.Product;

@Mapper
public interface ProductMapper {

	List<Product> selectProductList(Map<String, Object> param);
	Product selectProductById(long productId);

}
