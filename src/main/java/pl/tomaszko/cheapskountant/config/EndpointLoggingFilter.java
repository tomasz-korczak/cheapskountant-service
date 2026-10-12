package pl.tomaszko.cheapskountant.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class EndpointLoggingFilter extends OncePerRequestFilter {

    private static final int REQUEST_CACHE_LIMIT = 10 * 1024 * 1024;

    private final List<String> secrets;

    public EndpointLoggingFilter(
            @Value("${app.api-key:}") String apiKey,
            @Value("${spring.ai.openai.api-key:}") String openRouterKey,
            @Value("${spring.datasource.password:}") String databasePassword) {
        this.secrets = secrets(apiKey, openRouterKey, databasePassword);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean multipart = multipart(request);
        ContentCachingRequestWrapper cachingRequest = multipart
                ? null
                : new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);
        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);
        HttpServletRequest requestToUse = cachingRequest != null ? cachingRequest : request;
        Exception failure = null;
        try {
            filterChain.doFilter(requestToUse, cachingResponse);
        }
        catch (ServletException | IOException | RuntimeException ex) {
            failure = ex;
            throw ex;
        }
        finally {
            try {
                log.info(redact(message(requestToUse, cachingRequest, cachingResponse, failure)));
            }
            finally {
                cachingResponse.copyBodyToResponse();
            }
        }
    }

    private String message(
            HttpServletRequest request,
            ContentCachingRequestWrapper cachingRequest,
            ContentCachingResponseWrapper response,
            Exception failure) {
        StringBuilder message = new StringBuilder();
        message.append("endpoint ").append(request.getMethod()).append(' ').append(request.getRequestURI());
        appendParameters(message, request);
        if (multipart(request)) {
            appendMultipart(message, request);
        }
        else if (cachingRequest != null) {
            appendBody(message, cachingRequest);
        }
        message.append("\nresponse status: ").append(response.getStatus());
        message.append("\nresponse body: ").append(payload(response.getContentAsByteArray(), response.getContentType()));
        if (failure != null) {
            message.append("\nfailure: ").append(failure.getClass().getSimpleName());
            if (failure.getMessage() != null) {
                message.append(": ").append(failure.getMessage());
            }
        }
        return message.toString();
    }

    private void appendParameters(StringBuilder message, HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        if (!parameters.isEmpty()) {
            message.append("\nparameters: ").append(format(parameters));
            return;
        }
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            message.append("\nparameters: ").append(query);
        }
    }

    private String format(Map<String, String[]> parameters) {
        StringBuilder formatted = new StringBuilder();
        parameters.forEach((name, values) -> {
            for (String value : values) {
                if (formatted.length() > 0) {
                    formatted.append('&');
                }
                formatted.append(name).append('=').append(value);
            }
        });
        return formatted.toString();
    }

    private void appendMultipart(StringBuilder message, HttpServletRequest request) {
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            multipartRequest.getMultiFileMap().forEach((name, files) -> {
                for (MultipartFile file : files) {
                    appendFile(message, name, file.getOriginalFilename(), file.getContentType(), file.getSize());
                }
            });
            return;
        }
        appendParts(message, request);
    }

    private void appendParts(StringBuilder message, HttpServletRequest request) {
        try {
            for (Part part : request.getParts()) {
                String filename = part.getSubmittedFileName();
                if (filename != null && !filename.isBlank()) {
                    appendFile(message, part.getName(), filename, part.getContentType(), part.getSize());
                }
                else if (request.getParameter(part.getName()) == null) {
                    message.append("\nrequest parameter: ")
                            .append(part.getName())
                            .append('=')
                            .append(partValue(part));
                }
            }
        }
        catch (IOException | ServletException ex) {
            message.append("\nrequest parts: unavailable");
        }
    }

    private void appendFile(StringBuilder message, String name, String filename, String contentType, long size) {
        message.append("\nrequest part: name=")
                .append(name)
                .append(", filename=")
                .append(filename)
                .append(", content type=")
                .append(contentType)
                .append(", size=")
                .append(size);
    }

    private void appendBody(StringBuilder message, ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0 && request.getContentType() == null) {
            return;
        }
        String body = payload(content, request.getContentType());
        int contentLength = request.getContentLength();
        if (contentLength > content.length || (contentLength < 0 && content.length == REQUEST_CACHE_LIMIT)) {
            body = body + "...[truncated]";
        }
        message.append("\nrequest body: ").append(body);
    }

    private String partValue(Part part) {
        try (InputStream input = part.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            return "";
        }
    }

    private String payload(byte[] content, String contentType) {
        if (content.length == 0) {
            return "";
        }
        if (contentType != null && !textual(contentType)) {
            return "content type=" + contentType + ", size=" + content.length;
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private boolean textual(String contentType) {
        String type = contentType.toLowerCase(Locale.ROOT);
        return type.startsWith("text/")
                || type.contains("json")
                || type.contains("xml")
                || type.contains("application/x-www-form-urlencoded");
    }

    private boolean multipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }

    private String redact(String text) {
        String redacted = text;
        for (String secret : secrets) {
            redacted = redacted.replace(secret, "[redacted]");
        }
        return redacted;
    }

    private static List<String> secrets(String apiKey, String openRouterKey, String databasePassword) {
        List<String> values = new ArrayList<>();
        add(values, apiKey);
        add(values, openRouterKey);
        add(values, databasePassword);
        values.sort(Comparator.comparingInt(String::length).reversed());
        return List.copyOf(values);
    }

    private static void add(List<String> values, String secret) {
        if (secret != null && !secret.isBlank() && !values.contains(secret)) {
            values.add(secret);
        }
    }
}
