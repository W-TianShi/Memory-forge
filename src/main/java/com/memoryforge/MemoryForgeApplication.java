package com.memoryforge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.memoryforge.mapper") // 关键：扫描你的Mapper包
public class MemoryForgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemoryForgeApplication.class, args);
    }
}