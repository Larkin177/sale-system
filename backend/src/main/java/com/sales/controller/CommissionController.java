package com.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sales.dto.ApiResponse;
import com.sales.entity.Commission;
import com.sales.mapper.CommissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionMapper commissionMapper;

    @GetMapping
    public ApiResponse<Page<Commission>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Commission> pageParam = new Page<>(page, size);
        Page<Commission> result = commissionMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Commission>().orderByDesc(Commission::getCreatedAt));
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}/settle")
    public ApiResponse<Void> settle(@PathVariable Long id) {
        Commission commission = commissionMapper.selectById(id);
        if (commission == null) {
            return ApiResponse.error("分润记录不存在");
        }
        commission.setStatus("settled");
        commission.setSettledAt(java.time.LocalDateTime.now());
        commissionMapper.updateById(commission);
        return ApiResponse.success();
    }

    @PostMapping("/batch-settle")
    public ApiResponse<Void> batchSettle(@RequestBody java.util.Map<String, java.util.List<Long>> body) {
        java.util.List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.error("请选择要结算的记录");
        }
        for (Long id : ids) {
            Commission commission = commissionMapper.selectById(id);
            if (commission != null && "pending".equals(commission.getStatus())) {
                commission.setStatus("settled");
                commission.setSettledAt(java.time.LocalDateTime.now());
                commissionMapper.updateById(commission);
            }
        }
        return ApiResponse.success();
    }
}
