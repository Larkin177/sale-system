package com.sales.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",           // 登录/注册
                        "/api/sales/register",    // 销售注册
                        "/api/config",            // 公开配置
                        "/api/pay/**",            // 支付
                        "/api/payment/**",        // 支付二维码
                        "/api/download/**",       // 下载
                        "/api/leaderboard/**",    // 排行榜
                        "/api/auth/validate",     // 授权码验证（客户端调用）
                        "/api/auth/redeem",       // 授权码核销（客户端调用）
                        "/api/site-settings",     // 站点设置（客户端读取）
                        "/api/customer/**",       // 客户端公开接口
                        "/api/verification/**",   // 验证码
                        "/api/captcha/**",          // 图形验证码
                        "/api/alipay/**"            // 支付宝回调
                );
    }
}
