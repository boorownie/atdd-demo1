package com.example.demo1

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orders")
class OrderController(private val productClient: ProductClient) {

    @PostMapping
    fun createOrder(@RequestBody orderRequest: OrderRequest): Map<String, Any> {
        val product = productClient.getProduct(orderRequest.product)
        // 간단한 응답 반환
        return mapOf(
            "status" to "success",
            "product" to product
        )
    }
}