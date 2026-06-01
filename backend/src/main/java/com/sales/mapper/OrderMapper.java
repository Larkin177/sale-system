package com.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sales.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Update("UPDATE orders SET sales_id = #{salesId}, claimed_at = NOW(), status = 'bound' " +
            "WHERE id = #{orderId} AND sales_id IS NULL AND status = 'paid'")
    int claimOrder(@Param("orderId") Long orderId, @Param("salesId") Long salesId);
}
