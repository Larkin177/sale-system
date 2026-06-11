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

        doCalculate(order, sales);
    }

    /**
     * 认领订单时计算分润（订单可能已是 delivered 状态，salesId 由参数传入）
     */
    @Transactional
    public void calculateCommissionForClaim(Long orderId, Long salesId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return;
        Sales sales = salesMapper.selectById(salesId);
        if (sales == null) return;
        doCalculate(order, sales);
    }

    private void doCalculate(Order order, Sales sales) {
        // commissionRate 表示销售分润比例（如10表示销售拿10%）
        BigDecimal rate = sales.getCommissionRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal salesAmount = order.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformAmount = order.getAmount().subtract(salesAmount).setScale(2, RoundingMode.HALF_UP);

        // 创建分润记录
        Commission commission = new Commission();
        commission.setOrderId(order.getId());
        commission.setSalesId(sales.getId());
        commission.setAmount(salesAmount);
        commission.setAdminAmount(platformAmount);
        commission.setRate(sales.getCommissionRate());
        commission.setStatus("pending");
        commissionMapper.insert(commission);
    }
}
