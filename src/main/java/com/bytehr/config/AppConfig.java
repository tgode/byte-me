package com.bytehr.config;

import com.bytehr.config.RagProperties;
import com.bytehr.config.SourceProperties;
import org.apache.tika.Tika;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({SourceProperties.class, RagProperties.class})
public class AppConfig {

    @Bean
    public Tika tika() {
        return new Tika();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
