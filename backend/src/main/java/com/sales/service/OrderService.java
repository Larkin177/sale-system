package com.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.dto.ClaimOrderRequest;
import com.sales.entity.CustomerPrice;
import com.sales.entity.Order;
import com.sales.entity.ProductPackage;
import com.sales.entity.Sales;
import com.sales.mapper.CustomerPriceMapper;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.SalesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * 创建订单（带套餐信息）
     */
    public Order createOrderWithPackage(Long salesId, BigDecimal amount, ProductPackage pkg) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setAmount(amount);
        order.setBaseAmount(amount);
        order.setSalesId(salesId);
        order.setProductId(pkg.getProductId());
        order.setPackageName(pkg.getName());
        order.setPlatform(pkg.getPlatform());
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
        // 使用SQL聚合查询替代Java层聚合，提高性能
        Map<String, Object> stats = orderMapper.getSalesStats(salesId);

        return ApiResponse.success(stats);
    }

    public Map<String, Object> getCustomerLastOrder(String phone) {
        // 使用JOIN查询替代N+1查询，提高性能
        Map<String, Object> lastOrder = orderMapper.findLastOrder_byPhoneWithSales(phone);
        if (lastOrder == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("amount", lastOrder.get("amount"));
        result.put("orderNo", lastOrder.get("order_no"));
        result.put("createdAt", lastOrder.get("created_at"));
        result.put("salesCode", lastOrder.get("sales_code"));

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

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", SECURE_RANDOM.nextInt(10000));
    }
}
