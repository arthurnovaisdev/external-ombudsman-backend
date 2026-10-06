package com.mbfreire.employee_reporting.security;

import com.mbfreire.employee_reporting.dto.response.ErrorResponseDTO;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authHeader.substring(7);

        try {

            String username =
                    jwtService.extractUsername(token);

            if (username != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(username);

                if (!userDetails.isEnabled()) {

                    SecurityContextHolder
                            .clearContext();

                    sendErrorResponse(
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Usuário desativado ou não autorizado."
                    );

                    return;
                }

                if (!jwtService.isTokenValid(
                        token,
                        userDetails
                )) {

                    SecurityContextHolder
                            .clearContext();

                    sendErrorResponse(
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Token de acesso inválido."
                    );

                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authToken);
            }

            filterChain.doFilter(
                    request,
                    response
            );

        } catch (ExpiredJwtException e) {

            SecurityContextHolder.clearContext();

            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Sessão expirada. Faça login novamente."
            );

        } catch (JwtException | IllegalArgumentException e) {

            SecurityContextHolder.clearContext();

            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Token de acesso inválido ou malformado."
            );

        } catch (UsernameNotFoundException e) {


            SecurityContextHolder.clearContext();

            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Usuário desativado ou não encontrado."
            );

        } catch (Exception e) {

            SecurityContextHolder.clearContext();

            sendErrorResponse(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Erro interno de autenticação."
            );
        }
    }

    private void sendErrorResponse(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status);

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        ErrorResponseDTO error =
                new ErrorResponseDTO(
                        status,
                        message,
                        LocalDateTime.now()
                );

        response.getWriter().write(
                objectMapper.writeValueAsString(error)
        );
    }
}