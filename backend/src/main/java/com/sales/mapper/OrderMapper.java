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

    @Select("SELECT amount, sales_id, order_no, created_at FROM orders WHERE customer_phone = #{phone} AND status IN ('paid','delivered','redeemed','bound','settled') ORDER BY created_at DESC LIMIT 1")
    Map<String, Object> findLastOrder_byPhone(@Param("phone") String phone);

    /**
     * 使用JOIN查询客户最近订单，包含销售信息，避免N+1查询
     */
    @Select("SELECT o.amount, o.sales_id, o.order_no, o.created_at, s.code as sales_code " +
            "FROM orders o " +
            "LEFT JOIN sales s ON o.sales_id = s.id " +
            "WHERE o.customer_phone = #{phone} AND o.status IN ('paid','delivered','redeemed','bound','settled') " +
            "ORDER BY o.created_at DESC LIMIT 1")
    Map<String, Object> findLastOrder_byPhoneWithSales(@Param("phone") String phone);

    /**
     * 使用SQL聚合查询销售业绩统计，避免Java层聚合
     */
    @Select("SELECT " +
            "COUNT(*) as totalOrders, " +
            "COALESCE(SUM(amount), 0) as totalAmount, " +
            "COUNT(CASE WHEN paid_at >= DATE_FORMAT(NOW(), '%Y-%m-01') THEN 1 END) as monthOrders, " +
            "COALESCE(SUM(CASE WHEN paid_at >= DATE_FORMAT(NOW(), '%Y-%m-01') THEN amount ELSE 0 END), 0) as monthAmount " +
            "FROM orders " +
            "WHERE sales_id = #{salesId} AND status IN ('paid','delivered','redeemed')")
    Map<String, Object> getSalesStats(@Param("salesId") Long salesId);
}
