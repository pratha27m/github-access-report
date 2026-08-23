package com.githubAccess.githubAccessReport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GitHubConfig {

    @Bean
    public WebClient githubWebClient(
            @Value("${github.api-url}") String apiUrl,
            @Value("${github.token}") String token) {

        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "Bearer " + token)
                .defaultHeader(HttpHeaders.ACCEPT,
                        "application/vnd.github+json")
                .build();
    }
}
