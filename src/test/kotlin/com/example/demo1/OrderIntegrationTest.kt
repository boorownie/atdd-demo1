package com.example.demo1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderIntegrationTest {

    companion object {
        @Container
        val wiremockContainer = GenericContainer<Nothing>("wiremock/wiremock:2.35.0")
            .apply { withExposedPorts(8080) }

        @JvmStatic
        @DynamicPropertySource
        fun registerWiremockProperties(registry: DynamicPropertyRegistry) {
            registry.add("product.service.url") {
                "http://${wiremockContainer.host}:${wiremockContainer.getMappedPort(8080)}"
            }
        }
    }

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `주문 요청 시 상품 정보 조회`() {
        // WireMock Stub 설정
        wiremockContainer.execInContainer(
            "curl", "-X", "POST", "-d",
            """
            {
              "request": {
                "method": "GET",
                "url": "/products/Apple"
              },
              "response": {
                "status": 200,
                "body": "{\"name\": \"Apple\", \"price\": 1000}",
                "headers": {
                  "Content-Type": "application/json"
                }
              }
            }
            """.trimIndent(),
            "http://localhost:8080/__admin/mappings"
        )

        val response = restTemplate.postForEntity(
            "/orders",
            OrderRequest("Apple"),
            Map::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("success", response.body?.get("status"))
        val product = response.body?.get("product") as Map<*, *>
        assertEquals("Apple", product["name"])
        assertEquals(1000, product["price"])
    }
}
