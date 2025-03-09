package com.example.demo1

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

@Configuration
class SNSConfig {
    @Value("\${aws.region}")
    private lateinit var region: String

    @Value("\${aws.credentials.access-key}")
    private lateinit var accessKey: String

    @Value("\${aws.credentials.secret-key}")
    private lateinit var secretKey: String

    @Value("\${aws.localstack.endpoint}")
    private lateinit var endpoint: String

    @Bean
    fun snsClient(): SnsClient {
        return SnsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )
            .endpointOverride(URI(endpoint))
            .build()
    }

    @Bean
    fun sqsClient(): SqsClient {
        return SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )
            .endpointOverride(URI(endpoint))
            .build()
    }
}
