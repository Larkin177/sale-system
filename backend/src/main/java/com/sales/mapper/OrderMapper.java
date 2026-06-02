package com.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sales.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Update("UPDATE orders SET sales_id = #{salesId}, claimed_at = NOW(), status = 'bound' " +
            "WHERE id = #{orderId} AND sales_id IS NULL AND status = 'paid'")
    int claimOrder(@Param("orderId") Long orderId, @Param("salesId") Long salesId);

    @Select("SELECT amount, sales_id, order_no, created_at FROM orders WHERE customer_phone = #{phone} AND status IN ('paid','bound','settled') ORDER BY created_at DESC LIMIT 1")
    Map<String, Object> findLastOrder_byPhone(@Param("phone") String phone);
}
