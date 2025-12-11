package in.vipinshivhare.invoicegeneratorapi.config;

import in.vipinshivhare.invoicegeneratorapi.security.ClerkJwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClerkJwtAuthFilter jwtAuthFilter;

    // Provide comma separated allowed origins in application.properties or use default
    @Value("${app.cors.allowed-origins:https://invoicee-generator.netlify.app,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        // Apply CORS configuration source (used by Spring's cors support)
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Allow public endpoints
                        .requestMatchers("/", "/health", "/api/webhooks/**").permitAll()
                        // Allow preflight OPTIONS for all endpoints
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Ensure CorsFilter runs before security filters so it handles preflight and sets headers
        http.addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter.class);

        // Add JWT filter AFTER the CorsFilter (explicit ordering) so that OPTIONS requests
        // are not blocked by authentication logic.
        http.addFilterAfter(jwtAuthFilter, CorsFilter.class);

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Split comma-separated origins and trim spaces
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // Allow common headers that browsers send and any custom headers you expect
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        config.setExposedHeaders(List.of("Authorization")); // if you expose any headers to the client
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply to all endpoints
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
