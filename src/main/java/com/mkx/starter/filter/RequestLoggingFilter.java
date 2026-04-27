package com.mkx.starter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    // ANSI Color Codes
    private static final String RESET = "\u001B[0m";
    private static final String KEY_COLOR = "\u001B[36m";   // Cyan
    private static final String STRING_COLOR = "\u001B[32m"; // Green
    private static final String NUMBER_COLOR = "\u001B[35m"; // Magenta
    private static final String BRACE_COLOR = "\u001B[37m";  // White/Grey

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        filterChain.doFilter(requestWrapper, responseWrapper);
        long timeTaken = System.currentTimeMillis() - startTime;

        String requestBody = getStringValue(requestWrapper.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = getStringValue(responseWrapper.getContentAsByteArray(), response.getCharacterEncoding());

        log.info("[HTTP] {} {} | Status: {} | Time: {}ms", 
                request.getMethod(), request.getRequestURI(), response.getStatus(), timeTaken);
        
        if (requestBody != null && !requestBody.isEmpty()) {
            System.out.println("\u001B[33m[PAYLOAD] Request:\u001B[0m");
            System.out.println(colorizeJson(prettifyJson(requestBody)));
        }
        
        if (responseBody != null && !responseBody.isEmpty()) {
            String color = response.getStatus() < 400 ? "\u001B[32m" : "\u001B[31m";
            System.out.println(color + "[PAYLOAD] Response:\u001B[0m");
            System.out.println(colorizeJson(prettifyJson(responseBody)));
        }

        responseWrapper.copyBodyToResponse();
    }

    private String colorizeJson(String json) {
        if (json == null) return null;
        
        // 1. Colorize Braces and Brackets
        json = json.replaceAll("([{}\\[\\]])", BRACE_COLOR + "$1" + RESET);
        
        // 2. Colorize Keys (quoted strings followed by a colon)
        json = json.replaceAll("\"(\\w+)\"\\s*:", KEY_COLOR + "\"$1\"" + RESET + ":");
        
        // 3. Colorize String Values (quoted strings that are NOT keys)
        // This regex ensures it only matches strings after a colon
        json = json.replaceAll(":\\s*\"(.*?)\"", ": " + STRING_COLOR + "\"$1\"" + RESET);
        
        // 4. Colorize Numbers/Booleans (only if they follow a colon)
        json = json.replaceAll(":\\s*(true|false|\\d+(\\.\\d+)?)", ": " + NUMBER_COLOR + "$1" + RESET);
        
        return json;
    }

    private String prettifyJson(String json) {
        try {
            Object obj = objectMapper.readValue(json, Object.class);
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return json;
        }
    }

    private String getStringValue(byte[] contentAsByteArray, String characterEncoding) {
        try {
            return new String(contentAsByteArray, 0, contentAsByteArray.length, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            return "[Unknown Encoding]";
        }
    }
}
