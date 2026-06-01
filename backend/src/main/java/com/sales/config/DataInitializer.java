package com.sales.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sales.entity.Admin;
import com.sales.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminMapper adminMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        // 确保admin存在且密码正确
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, "admin"));
        if (admin == null) {
            admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            adminMapper.insert(admin);
            log.info("管理员账号已创建: admin / admin123");
        } else {
            // 验证密码是否正确，不正确则重置
            if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode("admin123"));
                adminMapper.updateById(admin);
                log.info("管理员密码已重置: admin123");
            }
        }
    }
}
