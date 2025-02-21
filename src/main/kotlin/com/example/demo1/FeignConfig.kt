package com.example.demo1

import feign.Logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignConfig {
    @Bean
    fun feignLoggerLevel(): Logger.Level = Logger.Level.FULL
}