package com.mbfreire.employee_reporting.security;

import com.mbfreire.employee_reporting.dto.response.ErrorResponseDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FirstAccessFilter extends OncePerRequestFilter {

    private final ObjectMapper mapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof UserDetailsImpl principal)) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        if (principal
                .getUser()
                .isPasswordChanged()) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String uri =
                request.getRequestURI();

        String method =
                request.getMethod();

        if (isPublicRoute(uri)) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        boolean allowed =
                (
                        HttpMethod.GET.matches(method)
                                && uri.equals("/api/users/me")
                )
                        ||
                        (
                                HttpMethod.PATCH.matches(method)
                                        && uri.equals(
                                        "/api/users/me/password"
                                )
                        );

        if (allowed) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        ErrorResponseDTO error =
                new ErrorResponseDTO(
                        HttpServletResponse.SC_FORBIDDEN,
                        "É necessário alterar a senha provisória antes de utilizar o sistema.",
                        LocalDateTime.now()
                );

        response.getWriter().write(
                mapper.writeValueAsString(
                        error
                )
        );
    }

    private boolean isPublicRoute(
            String uri
    ) {

        return uri.equals("/api/health")
                || uri.equals("/actuator/health")
                || uri.startsWith("/actuator/health/")
                || uri.equals("/api/auth/login")
                || uri.equals("/api/auth/forgot-password")
                || uri.equals("/api/auth/reset-password")
                || uri.startsWith("/swagger-ui/")
                || uri.equals("/swagger-ui.html")
                || uri.equals("/v3/api-docs")
                || uri.startsWith("/v3/api-docs/");
    }
}