package com.example.demo1


import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

@RestController
@RequestMapping("/orders")
class OrderController(
    private val productClient: ProductClient,
    private val snsClient: SnsClient
) {
    @Value("\${aws.sns.topic-arn}")
    private lateinit var topicArn: String

    @PostMapping
    fun createOrder(@RequestBody orderRequest: OrderRequest): Map<String, Any> {
        val product = productClient.getProduct(orderRequest.product)

        // SNS 메시지 발행
        val publishRequest = PublishRequest.builder()
            .topicArn(topicArn)
            .message("Order placed for ${orderRequest.product}")
            .build()

        snsClient.publish(publishRequest)

        return mapOf(
            "status" to "success",
            "product" to product
        )
    }
}