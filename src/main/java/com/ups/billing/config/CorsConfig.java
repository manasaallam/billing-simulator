package com.ups.billing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration for the API, allowing the React (Vite) dev server to
 * call the auth endpoints during local development, including sending/receiving
 * the {@code Authorization} bearer header for JWT-secured requests.
 *
 * <p>Allowed origins are configurable via the {@code app.cors.allowed-origins}
 * property (comma-separated), defaulting to the two localhost Vite origins.
 *
 * <p>Spring Security's {@code http.cors(Customizer.withDefaults())} picks up this
 * MVC CORS configuration automatically (via {@code HandlerMappingIntrospector})
 * so that preflight {@code OPTIONS} requests are permitted.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
