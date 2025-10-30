package com.spring.canary.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.spring.canary.category.infrastructure.mybatis")
public class MyBatisConfig { }
