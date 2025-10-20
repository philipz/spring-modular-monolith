package com.sivalabs.bookstore.catalog.web;

import com.sivalabs.bookstore.catalog.domain.ProductNotFoundException;
import com.sivalabs.bookstore.common.models.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice(assignableTypes = {ProductRestController.class})
class CatalogExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CatalogExceptionHandler.class);

    /**
     * Handle ProductNotFoundException for both REST API and Web UI
     * - REST API (/api/*): Returns JSON ErrorResponse
     * - Web UI: Returns ProblemDetail (for now, can be enhanced to return error view)
     */
    @ExceptionHandler(ProductNotFoundException.class)
    Object handle(ProductNotFoundException e, HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        log.warn("Product not found for path {}: {}", requestPath, e.getMessage());

        // Check if this is a REST API request
        if (isRestApiRequest(request)) {
            // Return JSON ErrorResponse for REST API
            ErrorResponse errorResponse =
                    new ErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage(), LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } else {
            // Return ProblemDetail for Web UI (Spring handles content negotiation)
            // This can be enhanced in the future to return a custom error view
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
            problemDetail.setTitle("Product Not Found");
            problemDetail.setProperty("timestamp", Instant.now());
            return problemDetail;
        }
    }

    /**
     * Determine if the request is for REST API endpoints
     * Checks both path pattern and Accept header
     */
    private boolean isRestApiRequest(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");

        // Check if path starts with /api/
        if (requestPath != null && requestPath.startsWith("/api/")) {
            return true;
        }

        // Check if Accept header prefers JSON
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            return true;
        }

        return false;
    }
}
