package com.regisx001.dQul.logs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("dQul-logs API")
                .description("Read/ops API for the Data Quality Platform logs microservice. " +
                        "Ingestion is Kafka-native; this API only reads and administers log entries.")
                .version("v1"));
    }
}
