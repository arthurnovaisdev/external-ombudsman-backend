package com.mbfreire.employee_reporting.config;

import com.mbfreire.employee_reporting.dto.response.ErrorResponseDTO;
import com.mbfreire.employee_reporting.security.ApiRateLimitFilter;
import com.mbfreire.employee_reporting.security.FirstAccessFilter;
import com.mbfreire.employee_reporting.security.JWTAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final FirstAccessFilter firstAccessFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> {})

                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(e -> e
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                        .accessDeniedHandler(
                                accessDeniedHandler
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // HEALTH CHECK

                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.HEAD,
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/health"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.HEAD,
                                "/api/health"
                        ).permitAll()


                        // PUBLIC AUTHENTICATION

                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()


                        // SWAGGER
                        // Development only.
                        // Disabled in production profile.

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()


                        // ADMIN - USER REGISTRATION

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register"
                        ).hasRole("ADMIN")


                        // OWN USER ACCOUNT

                        .requestMatchers(
                                "/api/users/me/**"
                        ).authenticated()


                        // ADMIN - USER MANAGEMENT

                        .requestMatchers(
                                "/api/users/**"
                        ).hasRole("ADMIN")



                        // ADMIN - REPORTS
                        // Includes details, messages, attachments and close

                        .requestMatchers(
                                "/api/reports/admin/**"
                        ).hasRole("ADMIN")


                        // CLIENT - CREATE REPORT

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/reports"
                        ).hasRole("CLIENT")


                        // CLIENT - OWN REPORTS
                        // Includes details, messages and attachments

                        .requestMatchers(
                                "/api/reports/mine/**"
                        ).hasRole("CLIENT")


                        // CATEGORIES

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/categories"
                        ).hasAnyRole(
                                "CLIENT",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/categories"
                        ).hasRole("ADMIN")


                        // DENY BY DEFAULT

                        .anyRequest().denyAll()
                )


                // JWT

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )


                // FIRST ACCESS

                .addFilterAfter(
                        firstAccessFilter,
                        JWTAuthenticationFilter.class
                )


                // RATE LIMIT

                .addFilterAfter(
                        apiRateLimitFilter,
                        FirstAccessFilter.class
                );

        return http.build();
    }


    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(
            ObjectMapper mapper
    ) {

        return (request, response, authException) -> {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            var error =
                    new ErrorResponseDTO(
                            401,
                            "Token ausente, inválido ou expirado",
                            LocalDateTime.now()
                    );

            response.getWriter().write(
                    mapper.writeValueAsString(error)
            );
        };
    }


    @Bean
    public AccessDeniedHandler accessDeniedHandler(
            ObjectMapper mapper
    ) {

        return (request, response, accessDeniedException) -> {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            var error =
                    new ErrorResponseDTO(
                            403,
                            "Você não tem permissão para acessar este recurso",
                            LocalDateTime.now()
                    );

            response.getWriter().write(
                    mapper.writeValueAsString(error)
            );
        };
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }
}