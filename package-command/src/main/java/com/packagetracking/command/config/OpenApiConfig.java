package com.packagetracking.command.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Package Tracking Command API")
                .description("API para criação, atualização e gerenciamento de pacotes e eventos de rastreamento")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Package Tracking Team")
                    .email("dev@packagetracking.com")
                    .url("https://packagetracking.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Servidor de Desenvolvimento - Package Ingestion"),
                new Server()
                    .url("http://localhost:8081")
                    .description("Servidor de Desenvolvimento - Event Ingestion"),
                new Server()
                    .url("http://localhost:8082")
                    .description("Servidor de Desenvolvimento - Event Consumer"),
                new Server()
                    .url("https://api.packagetracking.com")
                    .description("Servidor de Produção")
            ));
    }
} 