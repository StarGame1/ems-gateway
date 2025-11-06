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
            System.out.println("Request body: " + body);
            System.out.println("Request headers: " + headers);


            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    method,
                    entity,
                    String.class
            );

            System.out.println("Response received: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());
            System.out.println("Response headers: " + response.getHeaders());

            ResponseEntity<String> finalResponse = ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());

            System.out.println("Returning to frontend: " + finalResponse.getStatusCode() + " - " + finalResponse.getBody());


            return finalResponse;

        } catch (Exception e) {
            System.err.println("Error in forwardRequest: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Gateway error: " + e.getMessage());
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