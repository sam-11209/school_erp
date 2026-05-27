package com.schoolerp.school_erp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        // Skip auth for authentication endpoints
        if (path.startsWith("/api/auth")) {
            return true;
        }

        // Get Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
            return false;
        }

        String tokenStr = authHeader.substring(7);
        AuthToken token;
        Set<String> roles;
        try {
            token = tokenService.parseToken(tokenStr);
            roles = tokenService.decryptRoles(token.getEncryptedRoles());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
            return false;
        }

        // Set request security context
        UserContext.setCurrentUser(token.getUserId());
        UserContext.setCurrentRoles(roles);

        // Check annotations if handler is a Controller method
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequiresRoles annotation = handlerMethod.getMethodAnnotation(RequiresRoles.class);
            if (annotation == null) {
                annotation = handlerMethod.getBeanType().getAnnotation(RequiresRoles.class);
            }

            if (annotation != null) {
                boolean authorized = false;
                for (String reqRole : annotation.value()) {
                    if (roles.contains(reqRole.toUpperCase())) {
                        authorized = true;
                        break;
                    }
                }

                if (!authorized) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Forbidden: Insufficient privileges\"}");
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
