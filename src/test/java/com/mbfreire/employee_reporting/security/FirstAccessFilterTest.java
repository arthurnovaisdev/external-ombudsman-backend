package com.mbfreire.employee_reporting.security;

import com.mbfreire.employee_reporting.dto.response.ErrorResponseDTO;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FirstAccessFilterTest {

    private ObjectMapper mapper;
    private FirstAccessFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {

        mapper =
                mock(ObjectMapper.class);

        filterChain =
                mock(FilterChain.class);

        filter =
                new FirstAccessFilter(
                        mapper
                );
    }

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedRequestContinuesNormally() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        HttpMethod.GET.name(),
                        "/api/reports/mine"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void userWithPasswordAlreadyChangedCanAccessNormally() throws Exception {

        authenticateUser(
                true
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        HttpMethod.GET.name(),
                        "/api/reports/mine"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void firstAccessUserCanReadOwnProfile() throws Exception {

        authenticateUser(
                false
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        HttpMethod.GET.name(),
                        "/api/users/me"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void firstAccessUserCanChangePassword() throws Exception {

        authenticateUser(
                false
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        HttpMethod.PATCH.name(),
                        "/api/users/me/password"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void firstAccessUserCannotAccessOtherProtectedRoutes() throws Exception {

        authenticateUser(
                false
        );

        when(
                mapper.writeValueAsString(
                        any(ErrorResponseDTO.class)
                )
        ).thenReturn(
                """
                {
                  "status":403,
                  "erro":"É necessário alterar a senha provisória antes de utilizar o sistema."
                }
                """
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        HttpMethod.GET.name(),
                        "/api/reports/mine"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                403,
                response.getStatus()
        );

        verifyNoInteractions(
                filterChain
        );
    }

    @Test
    void firstAccessUserDoesNotBlockApiHealth() throws Exception {

        authenticateUser(
                false
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        HttpMethod.GET.name(),
                        "/api/health"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void firstAccessUserDoesNotBlockActuatorHealth() throws Exception {

        authenticateUser(
                false
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        HttpMethod.GET.name(),
                        "/actuator/health"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void firstAccessUserDoesNotBlockActuatorHealthGroups() throws Exception {

        authenticateUser(
                false
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        HttpMethod.GET.name(),
                        "/actuator/health/liveness"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    private void authenticateUser(
            boolean passwordChanged
    ) {

        User user =
                User.builder()
                        .id(UUID.randomUUID())
                        .name("Cliente Teste")
                        .username("cliente.teste")
                        .role(Role.CLIENT)
                        .active(true)
                        .passwordChanged(
                                passwordChanged
                        )
                        .build();

        UserDetailsImpl principal =
                mock(UserDetailsImpl.class);

        when(
                principal.getUser()
        ).thenReturn(
                user
        );

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        authentication
                );
    }
}