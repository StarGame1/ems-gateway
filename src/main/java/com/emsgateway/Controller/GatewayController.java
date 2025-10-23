package com.emsgateway.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@RestController
@RequestMapping("/api")
public class GatewayController {

    @Value("${service.auth.url}")
    private String authServiceUrl;

    @Value("${service.user.url}")
    private String userServiceUrl;

    @Value("${service.device.url}")
    private String deviceServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @RequestMapping("/auth/**")
    public ResponseEntity<?> routeAuthService(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        return forwardRequest(authServiceUrl, body, request);
    }

    @RequestMapping("/users/**")
    public ResponseEntity<?> routeUserService(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        return forwardRequest(userServiceUrl, body, request);
    }

    @RequestMapping("/devices/**")
    public ResponseEntity<?> routeDeviceService(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        return forwardRequest(deviceServiceUrl, body, request);
    }

    private ResponseEntity<?> forwardRequest(String serviceUrl,
                                             String body,
                                             HttpServletRequest request) {
        try {
            String path = extractPath(request.getRequestURI());
            String fullUrl = serviceUrl + path;

            if (request.getQueryString() != null) {
                fullUrl += "?" + request.getQueryString();
            }

            HttpHeaders headers = copyHeaders(request);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            HttpMethod method = HttpMethod.valueOf(request.getMethod());

            System.out.println("Forwarding to: " + fullUrl + " (Method: " + method + ")");

            ResponseEntity<?> response = restTemplate.exchange(
                    fullUrl,
                    method,
                    entity,
                    String.class
            );

            return response;

        } catch (RestClientException e) {
            System.err.println("Service unavailable: " + e.getMessage());
            return ResponseEntity.status(503).body("Service unavailable");
        } catch (Exception e) {
            System.err.println("Error forwarding request: " + e.getMessage());
            return ResponseEntity.status(502).body("Bad gateway");
        }
    }

    private String extractPath(String fullUri) {
        return fullUri;
    }

    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!shouldSkipHeader(headerName)) {
                headers.set(headerName, request.getHeader(headerName));
            }
        }

        return headers;
    }

    private boolean shouldSkipHeader(String headerName) {
        String lowerCase = headerName.toLowerCase();
        return lowerCase.equals("host") ||
                lowerCase.equals("content-length");
    }
}