package com.sivalabs.bookstore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for development environment.
 *
 * <p>This configuration enables Cross-Origin Resource Sharing (CORS) for the Next.js development
 * server running on http://localhost:3000. It is only active when the 'dev' profile is enabled.
 *
 * <p><strong>Production Note:</strong> In production, CORS is not needed because both frontend
 * and backend are served through nginx reverse proxy on the same origin (http://localhost:8080).
 * The nginx configuration routes:
 * <ul>
 *   <li>/ → frontend-next:3000 (Next.js server)</li>
 *   <li>/api/* → monolith:8080 (Spring Boot backend)</li>
 * </ul>
 *
 * <p><strong>Development Scenarios:</strong>
 * <ul>
 *   <li><strong>Docker Compose:</strong> No CORS needed (nginx proxy)</li>
 *   <li><strong>Local dev server (./dev.sh):</strong> CORS needed (direct backend access)</li>
 * </ul>
 *
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 */
@Configuration
@Profile("dev")
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Configure CORS mappings to allow Next.js dev server to access backend APIs.
     *
     * <p>Configuration details:
     * <ul>
     *   <li><strong>Allowed Origin:</strong> http://localhost:3000 (Next.js dev server)</li>
     *   <li><strong>Allowed Methods:</strong> GET, POST, PUT, DELETE, OPTIONS</li>
     *   <li><strong>Allow Credentials:</strong> true (for session cookie support)</li>
     *   <li><strong>Max Age:</strong> 3600 seconds (1 hour preflight cache)</li>
     * </ul>
     *
     * @param registry the CORS registry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
