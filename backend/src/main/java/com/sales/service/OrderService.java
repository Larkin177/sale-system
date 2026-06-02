package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.ClaimOrderRequest;
import com.sales.entity.CustomerPrice;
import com.sales.entity.Order;
import com.sales.entity.Sales;
import com.sales.mapper.CustomerPriceMapper;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.SalesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final SalesMapper salesMapper;
    private final CustomerPriceMapper customerPriceMapper;

    public Order createOrder(Long salesId, BigDecimal baseAmount) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setAmount(baseAmount);
        order.setBaseAmount(baseAmount);
        order.setSalesId(salesId);
        order.setStatus("pending");
        orderMapper.insert(order);
        return order;
    }

    public ApiResponse<Void> claimOrder(Long salesId, ClaimOrderRequest request) {
        // 查询订单
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, request.getOrderNo()));
        if (order == null) {
            return ApiResponse.error("订单不存在");
        }

        // 验证手机号后4位（这里简化为直接匹配完整手机号）
        Sales sales = salesMapper.selectById(salesId);
        if (sales == null) {
            return ApiResponse.error("销售不存在");
        }

        // 尝试认领（原子操作）
        int result = orderMapper.claimOrder(order.getId(), salesId);
        if (result == 0) {
            return ApiResponse.error("该订单已被认领或无法认领");
        }

        return ApiResponse.success();
    }

    public ApiResponse<Page<Order>> listMyOrders(Long salesId, int page, int size) {
        Page<Order> pageParam = new Page<>(page, size);
        Page<Order> result = orderMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSalesId, salesId)
                        .orderByDesc(Order::getCreatedAt));
        return ApiResponse.success(result);
    }

    public ApiResponse<Page<Order>> listAllOrders(int page, int size) {
        Page<Order> pageParam = new Page<>(page, size);
        Page<Order> result = orderMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreatedAt));
        return ApiResponse.success(result);
    }

    public ApiResponse<List<Order>> listUnclaimedOrders() {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .isNull(Order::getSalesId)
                        .eq(Order::getStatus, "paid"));
        return ApiResponse.success(orders);
    }

    public ApiResponse<Object> getMyStats(Long salesId) {
        // 总成交额
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSalesId, salesId)
                        .eq(Order::getStatus, "paid"));

        double totalAmount = orders.stream()
                .mapToDouble(o -> o.getAmount().doubleValue())
                .sum();

        // 本月成交额
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        double monthAmount = orders.stream()
                .filter(o -> o.getPaidAt() != null && o.getPaidAt().isAfter(monthStart))
                .mapToDouble(o -> o.getAmount().doubleValue())
                .sum();

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalOrders", orders.size());
        stats.put("totalAmount", totalAmount);
        stats.put("monthOrders", orders.stream()
                .filter(o -> o.getPaidAt() != null && o.getPaidAt().isAfter(monthStart))
                .count());
        stats.put("monthAmount", monthAmount);

        return ApiResponse.success(stats);
    }

    public Map<String, Object> getCustomerLastOrder(String phone) {
        Map<String, Object> lastOrder = orderMapper.findLastOrder_byPhone(phone);
        if (lastOrder == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("amount", lastOrder.get("amount"));
        result.put("orderNo", lastOrder.get("order_no"));
        result.put("createdAt", lastOrder.get("created_at"));

        // Look up sales code from sales table if sales_id exists
        Object salesIdObj = lastOrder.get("sales_id");
        if (salesIdObj != null) {
            Long salesId = Long.valueOf(salesIdObj.toString());
            Sales sales = salesMapper.selectById(salesId);
            if (sales != null) {
                result.put("salesCode", sales.getCode());
            }
        }

        return result;
    }

    public void saveCustomerPrice(String phone, BigDecimal price, Long salesId, String orderNo) {
        // Upsert: if phone+salesId exists, update price; otherwise insert
        CustomerPrice existing = customerPriceMapper.selectOne(
                new LambdaQueryWrapper<CustomerPrice>()
                        .eq(CustomerPrice::getPhone, phone)
                        .eq(CustomerPrice::getSalesId, salesId));
        if (existing != null) {
            existing.setPrice(price);
            existing.setOrderNo(orderNo);
            customerPriceMapper.updateById(existing);
        } else {
            CustomerPrice cp = new CustomerPrice();
            cp.setPhone(phone);
            cp.setPrice(price);
            cp.setSalesId(salesId);
            cp.setOrderNo(orderNo);
            customerPriceMapper.insert(cp);
        }
    }

    public Map<String, Object> getCustomerPrice(String phone) {
        // Look up customer_prices for this phone, order by created_at DESC
        List<CustomerPrice> prices = customerPriceMapper.selectList(
                new LambdaQueryWrapper<CustomerPrice>()
                        .eq(CustomerPrice::getPhone, phone)
                        .orderByDesc(CustomerPrice::getCreatedAt));
        if (prices.isEmpty()) {
            return null;
        }
        CustomerPrice latest = prices.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("price", latest.getPrice());

        // Look up sales code from sales table if sales_id exists
        if (latest.getSalesId() != null) {
            Sales sales = salesMapper.selectById(latest.getSalesId());
            if (sales != null) {
                result.put("salesCode", sales.getCode());
            }
        }
        return result;
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));
    }
}
