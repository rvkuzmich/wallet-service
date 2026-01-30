package ru.kuzmich.walletservice.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.kuzmich.walletservice.dto.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(WalletNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleWalletNotFoundException(
      WalletNotFoundException ex, WebRequest request) {
    log.error("Wallet not found: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.NOT_FOUND.value())
        .error("Not Found")
        .message(ex.getMessage())
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientFundsException(
      InsufficientFundsException ex, WebRequest request) {
    log.error("Insufficient funds: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Bad Request")
        .message(ex.getMessage())
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, WebRequest request) {
    log.error("Validation error: {}", ex.getMessage());

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage()));

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Validation Failed")
        .message("Invalid request parameters")
        .path(request.getDescription(false).replace("uri=", ""))
        .validationErrors(errors)
        .build();

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {
    log.error("Constraint violation: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Bad Request")
        .message("Constraint violation")
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex, WebRequest request) {
    log.error("Failed to parse JSON request: {}", ex.getMessage());

    String message = "Invalid JSON format";
    Throwable rootCause = ex.getRootCause();

    if (rootCause instanceof JsonParseException) {
      message = "Malformed JSON request";
    } else if (rootCause instanceof JsonMappingException) {
      message = "Invalid JSON structure";
    }

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Bad Request")
        .message(message)
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
      OptimisticLockingFailureException ex, WebRequest request) {
    log.error("Concurrent modification detected: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.CONFLICT.value())
        .error("Conflict")
        .message("Concurrent modification detected. Please retry.")
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
      HttpMediaTypeNotSupportedException ex, WebRequest request) {
    log.error("Unsupported media type: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
        .error("Unsupported Media Type")
        .message("Content type '" + ex.getContentType() + "' is not supported")
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(error, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotAcceptableException(
      HttpMediaTypeNotAcceptableException ex, WebRequest request) {
    log.error("Not acceptable media type: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.NOT_ACCEPTABLE.value())
        .error("Not Acceptable")
        .message("Requested media type is not acceptable")
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException ex, WebRequest request) {
    log.error("Missing request parameter: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Bad Request")
        .message("Missing required parameter: " + ex.getParameterName())
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, WebRequest request) {
    log.error("Unexpected error: ", ex);

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error("Internal Server Error")
        .message("An unexpected error occurred")
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
