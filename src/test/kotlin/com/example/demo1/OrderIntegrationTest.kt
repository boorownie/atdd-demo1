package com.example.demo1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderIntegrationTest {

    companion object {
        private const val TOPIC_NAME = "order-topic"

        @Container
        val wiremockContainer = GenericContainer<Nothing>("wiremock/wiremock:2.35.0")
            .apply { withExposedPorts(8080) }

        @Container
        val localstack = LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.SNS)

        private lateinit var snsClient: SnsClient
        private lateinit var topicArn: String

        @JvmStatic
        @BeforeAll
        fun setup() {
            snsClient = SnsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SNS))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            localstack.accessKey,
                            localstack.secretKey
                        )
                    )
                )
                .region(Region.of(localstack.region))
                .build()

            // SNS 토픽 생성
            val createTopicResponse = snsClient.createTopic { it.name(TOPIC_NAME) }
            topicArn = createTopicResponse.topicArn()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            // WireMock 속성 등록
            registry.add("product.service.url") {
                "http://${wiremockContainer.host}:${wiremockContainer.getMappedPort(8080)}"
            }

            // LocalStack 속성 등록
            registry.add("aws.region") { localstack.region }
            registry.add("aws.credentials.access-key") { localstack.accessKey }
            registry.add("aws.credentials.secret-key") { localstack.secretKey }
            registry.add("aws.localstack.endpoint") { localstack.getEndpointOverride(LocalStackContainer.Service.SNS).toString() }
            registry.add("aws.sns.topic-arn") { topicArn }
        }
    }

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `주문 요청 시 상품 정보 조회 및 SNS 메시지 발행`() {
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

        // 선택적: SNS 메시지가 발행되었는지 확인하려면 SQS를 통해 구독 설정 후 확인하는 로직 추가 가능
    }
}