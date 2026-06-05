package com.sales.controller;

import com.sales.dto.ApiResponse;
import com.sales.entity.Download;
import com.sales.entity.Order;
import com.sales.entity.Product;
import com.sales.mapper.DownloadMapper;
import com.sales.mapper.OrderMapper;
import com.sales.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadMapper downloadMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    // 下载次数限制
    private static final int MAX_DOWNLOAD_COUNT = 10;

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getDownloadInfo(@RequestParam String token,
                                                           HttpServletRequest request) {
        // 验证token格式
        if (token == null || token.isEmpty() || !token.startsWith("dl-")) {
            return ApiResponse.error("下载链接无效");
        }

        Download download = downloadMapper.selectOne(
                new LambdaQueryWrapper<Download>()
                        .eq(Download::getDownloadToken, token));

        if (download == null) {
            log.warn("下载链接不存在: {}", token);
            return ApiResponse.error("下载链接无效");
        }

        // 检查是否过期
        if (download.getExpireAt().isBefore(java.time.LocalDateTime.now())) {
            log.warn("下载链接已过期: {}", token);
            return ApiResponse.error("下载链接已过期");
        }

        // 检查下载次数限制
        if (download.getDownloadCount() >= MAX_DOWNLOAD_COUNT) {
            log.warn("下载次数已达上限: {}, 当前次数: {}", token, download.getDownloadCount());
            return ApiResponse.error("下载次数已达上限，请联系客服");
        }

        // 记录下载IP（可选：用于安全审计）
        String clientIp = request.getRemoteAddr();
        log.info("下载信息查询 - token: {}, IP: {}, 当前下载次数: {}",
                token, clientIp, download.getDownloadCount());

        Map<String, Object> result = new HashMap<>();
        result.put("expireAt", download.getExpireAt());
        result.put("downloadCount", download.getDownloadCount());
        result.put("orderId", download.getOrderId());
        result.put("maxDownloads", MAX_DOWNLOAD_COUNT);

        return ApiResponse.success(result);
    }

    @GetMapping("/file")
    public void downloadFile(@RequestParam String token,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        // 验证token格式
        if (token == null || token.isEmpty() || !token.startsWith("dl-")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "下载链接无效");
            return;
        }

        Download download = downloadMapper.selectOne(
                new LambdaQueryWrapper<Download>()
                        .eq(Download::getDownloadToken, token));

        if (download == null) {
            log.warn("下载链接不存在: {}", token);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "下载链接无效");
            return;
        }

        // 检查是否过期
        if (download.getExpireAt().isBefore(java.time.LocalDateTime.now())) {
            log.warn("下载链接已过期: {}", token);
            response.sendError(HttpServletResponse.SC_GONE, "下载链接已过期");
            return;
        }

        // 检查下载次数限制
        if (download.getDownloadCount() >= MAX_DOWNLOAD_COUNT) {
            log.warn("下载次数已达上限: {}, 当前次数: {}", token, download.getDownloadCount());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "下载次数已达上限，请联系客服");
            return;
        }

        // 更新下载次数
        download.setDownloadCount(download.getDownloadCount() + 1);
        downloadMapper.updateById(download);

        // 获取订单和产品的下载URL
        String downloadUrl = null;
        if (download.getOrderId() != null) {
            Order order = orderMapper.selectById(download.getOrderId());
            if (order != null && order.getProductId() != null) {
                Product product = productMapper.selectById(order.getProductId());
                if (product != null) {
                    downloadUrl = product.getDownloadUrl();
                }
            }
        }

        String clientIp = request.getRemoteAddr();
        log.info("文件下载 - token: {}, IP: {}, 下载次数: {}",
                token, clientIp, download.getDownloadCount());

        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            // 重定向到实际下载地址
            response.sendRedirect(downloadUrl);
        } else {
            // 产品未配置下载地址，返回提示
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"message\":\"产品未配置下载地址，请联系管理员\"}");
        }
    }
}
