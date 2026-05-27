package com.schoolerp.school_erp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityInterceptorTest {

    @InjectMocks
    private SecurityInterceptor interceptor;

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        UserContext.clear();
    }

    @Test
    void testPreHandleAuthEndpoint() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        assertTrue(result);
        verifyNoInteractions(tokenService);
    }

    @Test
    void testPreHandleMissingAuthHeader() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/exams/create");
        when(request.getHeader("Authorization")).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Missing or invalid Authorization header"));
    }

    @Test
    void testPreHandleInvalidToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/exams/create");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalidtoken");
        when(tokenService.parseToken("invalidtoken")).thenThrow(new IllegalArgumentException("Invalid token"));

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Invalid or expired token"));
    }

    @Test
    void testPreHandleNoAnnotationOnMethodOrClass() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/exams/gradebook");
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");

        UUID userId = UUID.randomUUID();
        AuthToken authToken = new AuthToken(userId, "encrypted-roles-str");
        when(tokenService.parseToken("validtoken")).thenReturn(authToken);
        when(tokenService.decryptRoles("encrypted-roles-str")).thenReturn(Set.of("STUDENT"));

        when(handlerMethod.getMethodAnnotation(RequiresRoles.class)).thenReturn(null);
        Class<?> mockControllerClass = Object.class;
        when(handlerMethod.getBeanType()).thenAnswer(invocation -> mockControllerClass);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        assertEquals(userId, UserContext.getCurrentUser());
        assertTrue(UserContext.getCurrentRoles().contains("STUDENT"));
    }

    @Test
    void testPreHandleAuthorizedUser() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/exams/create");
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");

        UUID userId = UUID.randomUUID();
        AuthToken authToken = new AuthToken(userId, "encrypted-roles-str");
        when(tokenService.parseToken("validtoken")).thenReturn(authToken);
        when(tokenService.decryptRoles("encrypted-roles-str")).thenReturn(Set.of("ADMIN"));

        RequiresRoles requiresRoles = mock(RequiresRoles.class);
        when(requiresRoles.value()).thenReturn(new String[]{"ADMIN", "TEACHER"});
        when(handlerMethod.getMethodAnnotation(RequiresRoles.class)).thenReturn(requiresRoles);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        assertEquals(userId, UserContext.getCurrentUser());
        assertTrue(UserContext.getCurrentRoles().contains("ADMIN"));
    }

    @Test
    void testPreHandleUnauthorizedUser() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/exams/create");
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");

        UUID userId = UUID.randomUUID();
        AuthToken authToken = new AuthToken(userId, "encrypted-roles-str");
        when(tokenService.parseToken("validtoken")).thenReturn(authToken);
        when(tokenService.decryptRoles("encrypted-roles-str")).thenReturn(Set.of("STUDENT"));

        RequiresRoles requiresRoles = mock(RequiresRoles.class);
        when(requiresRoles.value()).thenReturn(new String[]{"ADMIN", "TEACHER"});
        when(handlerMethod.getMethodAnnotation(RequiresRoles.class)).thenReturn(requiresRoles);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(responseWriter.toString().contains("Forbidden: Insufficient privileges"));
    }
}
