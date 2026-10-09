package pl.tomaszko.cheapskountant.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

    @Bean
    Filter apiKeyFilter(@Value("${app.api-key}") String apiKey) {
        return new ApiKeyFilter(apiKey);
    }

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    static final class ApiKeyFilter extends OncePerRequestFilter {

        private final String apiKey;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            if (!requiresApiKey(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            if (!authorized(request.getHeader(HttpHeaders.AUTHORIZATION))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"reason\":\"not authorized\",\"explanation\":\"A valid API key is required.\"}");
                return;
            }
            filterChain.doFilter(request, response);
        }

        private boolean requiresApiKey(HttpServletRequest request) {
            if (!"POST".equalsIgnoreCase(request.getMethod())) {
                return false;
            }
            String path = request.getServletPath();
            if (path == null || path.isEmpty()) {
                path = request.getRequestURI();
            }
            return "/api/receipt".equals(path) || "/api/transcription".equals(path);
        }

        private boolean authorized(String header) {
            if (header == null || !header.startsWith("Bearer ")) {
                return false;
            }
            String presented = header.substring("Bearer ".length()).trim();
            return MessageDigest.isEqual(
                    presented.getBytes(StandardCharsets.UTF_8),
                    apiKey.getBytes(StandardCharsets.UTF_8));
        }
    }
}
