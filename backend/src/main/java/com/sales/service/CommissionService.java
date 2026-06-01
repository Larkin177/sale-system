package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.entity.Commission;
import com.sales.entity.Order;
import com.sales.entity.Sales;
import com.sales.mapper.CommissionMapper;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.SalesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionMapper commissionMapper;
    private final OrderMapper orderMapper;
    private final SalesMapper salesMapper;

    @Transactional
    public void calculateCommission(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getSalesId() == null || !"paid".equals(order.getStatus())) {
            return;
        }

        Sales sales = salesMapper.selectById(order.getSalesId());
        if (sales == null) {
            return;
        }

        // 计算分润
        BigDecimal rate = sales.getCommissionRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal adminAmount = order.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal salesAmount = order.getAmount().subtract(adminAmount).setScale(2, RoundingMode.HALF_UP);

        // 创建分润记录
        Commission commission = new Commission();
        commission.setOrderId(orderId);
        commission.setSalesId(order.getSalesId());
        commission.setAmount(salesAmount);
        commission.setAdminAmount(adminAmount);
        commission.setRate(sales.getCommissionRate());
        commission.setStatus("pending");
        commissionMapper.insert(commission);
    }
}
