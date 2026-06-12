package com.ohgiraffers.backend.ai;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
public class AiClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient(120)));
    }

    @Bean("aiAnalysisClient")
    public WebClient aiAnalysisClient(
            AiClientProperties properties,
            @Value("${skinai.ai.request-timeout-seconds}") long requestTimeoutSeconds
    ) {
        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient(requestTimeoutSeconds)))
                .build();
    }

    private HttpClient httpClient(long responseTimeoutSeconds) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds));
    }
}
