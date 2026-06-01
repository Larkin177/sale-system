package com.sales;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sales.mapper")
public class SalesApplication {
    public static void main(String[] args) {
        SpringApplication.run(SalesApplication.class, args);
    }
}
