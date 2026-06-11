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

            // Total commission (all)
            BigDecimal totalComm = getSum("amount", s.getId());
            // Settled commission
            BigDecimal settledComm = getSettledSum(s.getId());
            // Pending commission
            BigDecimal pendingComm = totalComm != null ? totalComm.subtract(settledComm != null ? settledComm : BigDecimal.ZERO) : BigDecimal.ZERO;


            m.put("totalSales", totalComm != null ? totalComm : BigDecimal.ZERO);
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

        BigDecimal totalSales = BigDecimal.ZERO;
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
        result.put("commissions", commissions);
        result.put("trend", trend);
        return ApiResponse.success(result);
    }

    // ===== Admin: Create settlement =====
    @PostMapping("/api/admin/commissions/settle")
    public ApiResponse<Void> createSettlement(@RequestBody Map<String, String> body) {
        Long salesId = Long.valueOf(body.get("salesId"));
        BigDecimal amount = new BigDecimal(body.get("amount"));
        Long adminId = body.get("adminId") != null ? Long.valueOf(body.get("adminId")) : null;

        SettlementRecord record = new SettlementRecord();
        record.setSalesId(salesId);
        record.setAmount(amount);
        record.setStatus("pending");
        record.setSettledBy(adminId);
        settlementRecordMapper.insert(record);

        log.info("创建结算记录 - 销售ID: {}, 金额: {}", salesId, amount);
        return ApiResponse.success();
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
        s.setCommissionRate(new BigDecimal(body.get("rate")));
        salesMapper.updateById(s);
        return ApiResponse.success();
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
        result.put("commissions", commissions);
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
