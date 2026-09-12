package com.lcwd.uploadservice.exceptions;

import com.lcwd.uploadservice.dto.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception) {
        ApiErrorResponse response = new ApiErrorResponse(exception.getMessage(), Collections.emptyList());

        return ResponseEntity.status(404).body(response);
    }
}
