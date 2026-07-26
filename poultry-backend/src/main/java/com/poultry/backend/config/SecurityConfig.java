package com.poultry.backend.config;

import com.poultry.backend.security.CustomUserDetailsService;
import com.poultry.backend.security.JwtAccessDeniedHandler;
import com.poultry.backend.security.JwtAuthenticationEntryPoint;
import com.poultry.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // CORS properties passed from application.yml
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://smart-poultry-system.vercel.app}")
    private String allowedOriginsRaw;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:Authorization,Content-Type,Accept,Origin,X-Requested-With,Access-Control-Request-Method,Access-Control-Request-Headers}")
    private String allowedHeaders;

    @Value("${app.cors.exposed-headers:Authorization,Link,X-Total-Count}")
    private String exposedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Robustly parse and sanitize allowed origins (trimming whitespace)
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        for (String origin : origins) {
            if ("*".equals(origin)) {
                configuration.addAllowedOriginPattern("*");
            } else {
                configuration.addAllowedOrigin(origin);
                configuration.addAllowedOriginPattern(origin);
            }
        }

        // Methods
        List<String> methods = Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        configuration.setAllowedMethods(methods);

        // Headers
        if ("*".equals(allowedHeaders.trim())) {
            configuration.addAllowedHeader("*");
        } else {
            List<String> headers = Arrays.stream(allowedHeaders.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            configuration.setAllowedHeaders(headers);
        }

        // Exposed Headers
        List<String> exposed = Arrays.stream(exposedHeaders.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        configuration.setExposedHeaders(exposed);

        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS and Disable CSRF
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            
            // Handle exceptions
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(accessDeniedHandler)
            )
            
            // Session Management as Stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Endpoint Security Rules
            .authorizeHttpRequests(auth -> auth
                // Explicitly allow all OPTIONS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Allow public authentication & registration & weather & location & health endpoints
                .requestMatchers(
                        "/",
                        "/auth/login",
                        "/api/v1/auth/login",
                        "/auth/register",
                        "/api/v1/auth/register",
                        "/auth/register/owner",
                        "/api/v1/auth/register/owner",
                        "/auth/register/worker",
                        "/api/v1/auth/register/worker",
                        "/auth/verify-email",
                        "/api/v1/auth/verify-email",
                        "/auth/join-farm",
                        "/api/v1/auth/join-farm",
                        "/auth/join-farm-temp",
                        "/api/v1/auth/join-farm-temp",
                        "/auth/logout",
                        "/api/v1/auth/logout",
                        "/weather/**",
                        "/api/weather/**",
                        "/api/v1/weather/**",
                        "/location/**",
                        "/api/v1/location/**",
                        "/health",
                        "/api/v1/health",
                        "/actuator/health"
                ).permitAll()
                // Allow Swagger / OpenAPI documentation urls
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/webjars/**"
                ).permitAll()
                // Require authentication for all other resources
                .anyRequest().authenticated()
            );

        // Map the Custom Provider
        http.authenticationProvider(authenticationProvider());

        // Add custom JWT filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
