package com.mbfreire.employee_reporting.security;

import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApiRateLimitFilterTest {

    private static final String IP =
            "203.0.113.10";

    private RateLimitService rateLimitService;
    private ObjectMapper mapper;
    private ApiRateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() throws Exception {

        rateLimitService =
                mock(RateLimitService.class);

        mapper =
                mock(ObjectMapper.class);

        filterChain =
                mock(FilterChain.class);

        filter =
                new ApiRateLimitFilter(
                        rateLimitService,
                        mapper
                );

        when(
                mapper.writeValueAsString(
                        any()
                )
        ).thenReturn(
                "{}"
        );
    }

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void routeWithoutRateLimitContinuesNormally()
            throws Exception {

        MockHttpServletRequest request =
                request(
                        "GET",
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

        verifyNoInteractions(
                rateLimitService
        );
    }

    @Test
    void loginUsesFiveRequestsPerMinute()
            throws Exception {

        assertAllowedRule(
                "POST",
                "/api/auth/login",
                "login:ip:" + IP,
                5,
                Duration.ofMinutes(1)
        );
    }

    @Test
    void forgotPasswordUsesThreeRequestsPerFifteenMinutes()
            throws Exception {

        assertAllowedRule(
                "POST",
                "/api/auth/forgot-password",
                "forgot-password:ip:" + IP,
                3,
                Duration.ofMinutes(15)
        );
    }

    @Test
    void resetPasswordUsesFiveRequestsPerFifteenMinutes()
            throws Exception {

        assertAllowedRule(
                "POST",
                "/api/auth/reset-password",
                "reset-password:ip:" + IP,
                5,
                Duration.ofMinutes(15)
        );
    }

    @Test
    void reportCreationUsesTenRequestsPerHour()
            throws Exception {

        assertAllowedRule(
                "POST",
                "/api/reports",
                "report-create:ip:" + IP,
                10,
                Duration.ofHours(1)
        );
    }

    @Test
    void clientMessageUsesThirtyRequestsPerMinute()
            throws Exception {

        assertAllowedRule(
                "POST",
                "/api/reports/mine/DEN-2026-ABCD2345/messages",
                "client-report-message:ip:" + IP,
                30,
                Duration.ofMinutes(1)
        );
    }

    @Test
    void adminMessageUsesThirtyRequestsPerMinute()
            throws Exception {

        assertAllowedRule(
                "POST",
                "/api/reports/admin/DEN-2026-ABCD2345/messages",
                "admin-report-message:ip:" + IP,
                30,
                Duration.ofMinutes(1)
        );
    }

    @Test
    void attachmentUploadUsesTwentyRequestsPerHour()
            throws Exception {

        assertAllowedRule(
                "POST",
                "/api/reports/mine/DEN-2026-ABCD2345/attachments",
                "attachment-upload:ip:" + IP,
                20,
                Duration.ofHours(1)
        );
    }

    @Test
    void clientAttachmentDownloadUsesOneHundredTwentyRequestsPerMinute()
            throws Exception {

        assertAllowedRule(
                "GET",
                "/api/reports/mine/DEN-2026-ABCD2345/attachments/"
                        + UUID.randomUUID(),
                "attachment-download:ip:" + IP,
                120,
                Duration.ofMinutes(1)
        );
    }

    @Test
    void adminAttachmentDownloadUsesOneHundredTwentyRequestsPerMinute()
            throws Exception {

        assertAllowedRule(
                "GET",
                "/api/reports/admin/DEN-2026-ABCD2345/attachments/"
                        + UUID.randomUUID(),
                "attachment-download:ip:" + IP,
                120,
                Duration.ofMinutes(1)
        );
    }

    @Test
    void exceededLoginLimitReturns429()
            throws Exception {

        when(
                rateLimitService.allow(
                        "login:ip:" + IP,
                        5,
                        Duration.ofMinutes(1)
                )
        ).thenReturn(
                false
        );

        MockHttpServletRequest request =
                request(
                        "POST",
                        "/api/auth/login"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                429,
                response.getStatus()
        );

        assertEquals(
                "60",
                response.getHeader(
                        "Retry-After"
                )
        );

        assertEquals(
                "application/json;charset=UTF-8",
                response.getContentType()
        );

        verifyNoInteractions(
                filterChain
        );
    }

    @Test
    void authenticatedUserUsesUserIdAndIpAsIdentifier()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        User user =
                User.builder()
                        .id(userId)
                        .name("Cliente A")
                        .username("cliente.a")
                        .role(Role.CLIENT)
                        .active(true)
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

        String expectedKey =
                "report-create:user:"
                        + userId
                        + ":ip:"
                        + IP;

        when(
                rateLimitService.allow(
                        expectedKey,
                        10,
                        Duration.ofHours(1)
                )
        ).thenReturn(
                true
        );

        MockHttpServletRequest request =
                request(
                        "POST",
                        "/api/reports"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                rateLimitService
        ).allow(
                expectedKey,
                10,
                Duration.ofHours(1)
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void cloudflareIpHasPriorityOverRemoteAddress()
            throws Exception {

        String cloudflareIp =
                "198.51.100.25";

        when(
                rateLimitService.allow(
                        "login:ip:" + cloudflareIp,
                        5,
                        Duration.ofMinutes(1)
                )
        ).thenReturn(
                true
        );

        MockHttpServletRequest request =
                request(
                        "POST",
                        "/api/auth/login"
                );

        request.addHeader(
                "CF-Connecting-IP",
                "  " + cloudflareIp + "  "
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                rateLimitService
        ).allow(
                "login:ip:" + cloudflareIp,
                5,
                Duration.ofMinutes(1)
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    private void assertAllowedRule(
            String method,
            String uri,
            String expectedKey,
            int maxRequests,
            Duration window
    ) throws Exception {

        when(
                rateLimitService.allow(
                        expectedKey,
                        maxRequests,
                        window
                )
        ).thenReturn(
                true
        );

        MockHttpServletRequest request =
                request(
                        method,
                        uri
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                rateLimitService
        ).allow(
                expectedKey,
                maxRequests,
                window
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    private MockHttpServletRequest request(
            String method,
            String uri
    ) {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        method,
                        uri
                );

        request.setRemoteAddr(
                IP
        );

        return request;
    }
}