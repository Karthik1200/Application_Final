package com.example.Application.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token — obtain from POST /api/auth/login and prefix with 'Bearer '"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI vrgtOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Smart Hotel Visitor Route Guidance & Real-Time Tracking API")
                .description("""
                    Enterprise-grade REST API for the VRGT System.

                    **Roles:** ADMIN · SECURITY_GUARD · RECEPTIONIST · HOST

                    **Auth flow:**
                    1. `POST /api/auth/login` — receive JWT
                    2. Add header `Authorization: Bearer <token>` to all protected calls

                    **WebSocket:** Connect to `/ws` (STOMP over SockJS)
                    Subscribe to topics: `/topic/gate-events` · `/topic/reception-queue` ·
                    `/topic/tracking` · `/topic/notifications` · `/topic/emergency`
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("VRGT Engineering")
                    .email("dev@vrgt.in"))
                .license(new License()
                    .name("Private — All rights reserved")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local development"),
                new Server().url("https://vrgt.example.com").description("Production")
            ));
    }
}
