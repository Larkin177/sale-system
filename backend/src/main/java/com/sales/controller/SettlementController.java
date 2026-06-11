package com.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.dto.ApiResponse;
import com.sales.entity.*;
import com.sales.mapper.*;
import com.sales.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SettlementController {

    private final SalesMapper salesMapper;
    private final CommissionMapper commissionMapper;
    private final SalesPaymentCodeMapper salesPaymentCodeMapper;
    private final SettlementRecordMapper settlementRecordMapper;
    private final CommissionRateLogMapper commissionRateLogMapper;
    private final OrderMapper orderMapper;

    // ===== Helper: get order details for a list of commissions =====
    private List<Map<String, Object>> enrichCommissions(List<Commission> commissions) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Commission c : commissions) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("orderId", c.getOrderId());
            m.put("salesId", c.getSalesId());
            m.put("amount", c.getAmount());
            m.put("adminAmount", c.getAdminAmount());
            m.put("rate", c.getRate());
            m.put("status", c.getStatus());
            m.put("settledAt", c.getSettledAt());
            m.put("createdAt", c.getCreatedAt());

            // 查询订单信息
            Order order = orderMapper.selectById(c.getOrderId());
            if (order != null) {
                m.put("orderNo", order.getOrderNo());
                m.put("orderAmount", order.getAmount());
                m.put("orderStatus", order.getStatus());
                m.put("orderCreatedAt", order.getCreatedAt());
            }
            result.add(m);
        }
        return result;
    }

    // ===== Helper: get totalSales (sum of order amounts) for a sales =====
    private BigDecimal getTotalSales(Long salesId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSalesId, salesId));
        return orders.stream()
                .map(Order::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ===== Admin: Sales summary =====
    @GetMapping("/api/admin/commissions/summary")
    public ApiResponse<List<Map<String, Object>>> getSalesSummary() {
        List<Sales> sales = salesMapper.selectList(
                new LambdaQueryWrapper<Sales>().eq(Sales::getStatus, "active"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Sales s : sales) {
            Map<String, Object> m = new HashMap<>();
            m.put("salesId", s.getId());
            m.put("salesName", s.getName());
            m.put("salesCode", s.getCode());
            m.put("commissionRate", s.getCommissionRate());

            // Total sales (sum of order amounts)
            BigDecimal totalSales = getTotalSales(s.getId());
            // Total commission (all)
            BigDecimal totalComm = getSum("amount", s.getId());
            // Settled commission = 总分润
            BigDecimal settledComm = getSettledSum(s.getId());
            // Pending commission
            BigDecimal pendingComm = totalComm != null ? totalComm.subtract(settledComm != null ? settledComm : BigDecimal.ZERO) : BigDecimal.ZERO;

            m.put("totalSales", totalSales);
            m.put("totalCommission", totalComm);
            m.put("settledCommission", settledComm);
            m.put("pendingCommission", pendingComm);

            // Last settlement date
            SettlementRecord lastSettle = settlementRecordMapper.selectOne(
                    new LambdaQueryWrapper<SettlementRecord>()
                            .eq(SettlementRecord::getSalesId, s.getId())
                            .eq(SettlementRecord::getStatus, "completed")
                            .orderByDesc(SettlementRecord::getSettledAt)
                            .last("LIMIT 1"));
            m.put("lastSettledAt", lastSettle != null ? lastSettle.getSettledAt() : null);

            result.add(m);
        }
        return ApiResponse.success(result);
    }

    // ===== Admin: Sales detail =====
    @GetMapping("/api/admin/commissions/sales/{salesId}")
    public ApiResponse<Map<String, Object>> getSalesDetail(@PathVariable Long salesId) {
        Sales s = salesMapper.selectById(salesId);
        if (s == null) return ApiResponse.error("销售不存在");

        List<Commission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getSalesId, salesId)
                        .orderByDesc(Commission::getCreatedAt));

        BigDecimal totalSales = getTotalSales(salesId);
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal settled = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;

        for (Commission c : commissions) {
            totalCommission = totalCommission.add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
            if ("settled".equals(c.getStatus())) {
                settled = settled.add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
            } else {
                pending = pending.add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
            }
        }

        // Monthly trend
        List<Map<String, Object>> trend = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getSalesId, salesId)
                        .orderByAsc(Commission::getCreatedAt))
                .stream().collect(java.util.stream.Collectors.groupingBy(
                    c -> c.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                    java.util.stream.Collectors.summingDouble(c -> c.getAmount() != null ? c.getAmount().doubleValue() : 0.0)
                )).entrySet().stream().map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("month", e.getKey());
                    m.put("amount", e.getValue());
                    return m;
                }).sorted(Comparator.comparing(m -> (String) m.get("month")))
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("salesName", s.getName());
        result.put("salesCode", s.getCode());
        result.put("commissionRate", s.getCommissionRate());
        result.put("totalSales", totalSales);
        result.put("totalCommission", totalCommission);
        result.put("settledCommission", settled);
        result.put("pendingCommission", pending);
        result.put("commissions", enrichCommissions(commissions));
        result.put("trend", trend);

        // 结算记录（含凭证）
        List<SettlementRecord> settleRecords = settlementRecordMapper.selectList(
                new LambdaQueryWrapper<SettlementRecord>()
                        .eq(SettlementRecord::getSalesId, salesId)
                        .orderByDesc(SettlementRecord::getCreatedAt));
        result.put("settlementRecords", settleRecords);

        return ApiResponse.success(result);
    }

    // ===== Admin: Create settlement =====
    @PostMapping("/api/admin/commissions/settle")
    public ApiResponse<Map<String, Object>> createSettlement(@RequestBody Map<String, String> body) {
        Long salesId = Long.valueOf(body.get("salesId"));
        BigDecimal amount = new BigDecimal(body.get("amount"));
        Long adminId = body.get("adminId") != null ? Long.valueOf(body.get("adminId")) : null;

        SettlementRecord record = new SettlementRecord();
        record.setSalesId(salesId);
        record.setAmount(amount);
        record.setStatus("completed");
        record.setSettledBy(adminId);
        record.setSettledAt(LocalDateTime.now());
        settlementRecordMapper.insert(record);

        // 将待结算分润标记为已结算
        commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getSalesId, salesId)
                        .eq(Commission::getStatus, "pending"))
                .forEach(c -> {
                    c.setStatus("settled");
                    c.setSettledAt(LocalDateTime.now());
                    commissionMapper.updateById(c);
                });

        log.info("结算完成 - 销售ID: {}, 金额: {}", salesId, amount);
        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        return ApiResponse.success(result);
    }

    // ===== Admin: Upload settlement proof =====
    @PutMapping("/api/admin/commissions/settle/{id}/proof")
    public ApiResponse<Void> uploadProof(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SettlementRecord record = settlementRecordMapper.selectById(id);
        if (record == null) return ApiResponse.error("结算记录不存在");
        record.setProofUrl(body.get("proofUrl"));
        record.setAdminNote(body.get("note"));
        record.setStatus("completed");
        record.setSettledAt(LocalDateTime.now());
        settlementRecordMapper.updateById(record);

        // Also mark pending commissions as settled for this sales
        commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getSalesId, record.getSalesId())
                        .eq(Commission::getStatus, "pending"))
                .forEach(c -> {
                    c.setStatus("settled");
                    c.setSettledAt(LocalDateTime.now());
                    commissionMapper.updateById(c);
                });

        log.info("结算完成 - ID: {}, 销售ID: {}, 金额: {}", id, record.getSalesId(), record.getAmount());
        return ApiResponse.success();
    }

    // ===== Admin: Update commission rate =====
    @PutMapping("/api/admin/sales/{id}/rate")
    public ApiResponse<Void> updateRate(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Sales s = salesMapper.selectById(id);
        if (s == null) return ApiResponse.error("销售不存在");
        BigDecimal oldRate = s.getCommissionRate();
        BigDecimal newRate = new BigDecimal(body.get("rate"));
        s.setCommissionRate(newRate);
        salesMapper.updateById(s);

        // 记录比例变更
        CommissionRateLog log = new CommissionRateLog();
        log.setSalesId(id);
        log.setOldRate(oldRate);
        log.setNewRate(newRate);
        log.setCreatedBy(body.get("adminId") != null ? Long.valueOf(body.get("adminId")) : null);
        commissionRateLogMapper.insert(log);

        return ApiResponse.success();
    }

    // ===== Sales/Admin: Get rate change logs =====
    @GetMapping("/api/sales/rate-logs")
    public ApiResponse<List<CommissionRateLog>> getRateLogs(@RequestParam Long salesId) {
        return ApiResponse.success(commissionRateLogMapper.selectList(
                new LambdaQueryWrapper<CommissionRateLog>()
                        .eq(CommissionRateLog::getSalesId, salesId)
                        .orderByDesc(CommissionRateLog::getCreatedAt)));
    }

    // ===== Sales: Get my payment code =====
    @GetMapping("/api/sales/payment-code")
    public ApiResponse<List<SalesPaymentCode>> getMyPaymentCode(@RequestParam Long salesId) {
        return ApiResponse.success(salesPaymentCodeMapper.selectList(
                new LambdaQueryWrapper<SalesPaymentCode>().eq(SalesPaymentCode::getSalesId, salesId)));
    }

    // ===== Sales: Save payment code =====
    @PostMapping("/api/sales/payment-code")
    public ApiResponse<Void> savePaymentCode(@RequestBody SalesPaymentCode code) {
        // Delete existing code of same type
        salesPaymentCodeMapper.delete(
                new LambdaQueryWrapper<SalesPaymentCode>()
                        .eq(SalesPaymentCode::getSalesId, code.getSalesId())
                        .eq(SalesPaymentCode::getCodeType, code.getCodeType()));
        salesPaymentCodeMapper.insert(code);
        return ApiResponse.success();
    }

    // ===== Sales: Get my commissions =====
    @GetMapping("/api/sales/commissions")
    public ApiResponse<Map<String, Object>> getMyCommissions(@RequestParam Long salesId) {
        Sales s = salesMapper.selectById(salesId);
        if (s == null) return ApiResponse.error("销售不存在");

        List<Commission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getSalesId, salesId)
                        .orderByDesc(Commission::getCreatedAt));

        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal settled = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;

        for (Commission c : commissions) {
            totalCommission = totalCommission.add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
            if ("settled".equals(c.getStatus())) {
                settled = settled.add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
            } else {
                pending = pending.add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("commissionRate", s.getCommissionRate());
        result.put("totalCommission", totalCommission);
        result.put("settledCommission", settled);
        result.put("pendingCommission", pending);
        result.put("commissions", enrichCommissions(commissions));

        // 结算记录（含凭证）
        List<SettlementRecord> settleRecords = settlementRecordMapper.selectList(
                new LambdaQueryWrapper<SettlementRecord>()
                        .eq(SettlementRecord::getSalesId, salesId)
                        .orderByDesc(SettlementRecord::getCreatedAt));
        result.put("settlementRecords", settleRecords);
        return ApiResponse.success(result);
    }

    // ===== Helpers =====
    private BigDecimal getSum(String field, Long salesId) {
        List<Commission> list = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>().eq(Commission::getSalesId, salesId));
        return list.stream()
                .map(Commission::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getSettledSum(Long salesId) {
        List<Commission> list = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getSalesId, salesId)
                        .eq(Commission::getStatus, "settled"));
        return list.stream()
                .map(Commission::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
