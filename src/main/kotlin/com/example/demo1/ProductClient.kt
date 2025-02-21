package com.example.demo1

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Component
@FeignClient(name = "productClient", url = "\${product.service.url}")
interface ProductClient {
    @GetMapping("/products/{name}")
    fun getProduct(@PathVariable name: String): Map<String, Any>
}