package com.tenslots.global.api.handler;

import com.tenslots.domain.common.InvalidStateTransitionException;
import com.tenslots.domain.stock.StockOverflowException;
import com.tenslots.global.api.code.common.ErrorCode;
import com.tenslots.global.api.response.ApiResponse;
import com.tenslots.global.api.response.FailureResponse;
import com.tenslots.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse handleBusinessException(BusinessException e) {
        return ApiResponse.failure(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public FailureResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse(ErrorCode.REQUEST_CONTENT_INVALID.getMessage());
        return ApiResponse.failure(ErrorCode.REQUEST_CONTENT_INVALID, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ApiResponse.failure(ErrorCode.REQUEST_CONTENT_INVALID);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return ApiResponse.failure(ErrorCode.REQUIRED_PARAMETER_MISSED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse handleNoResourceFoundException(NoResourceFoundException e) {
        return ApiResponse.failure(ErrorCode.REQUEST_PATH_INVALID);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return ApiResponse.failure(ErrorCode.HTTP_METHOD_INVALID);
    }

    // 상태머신 규칙 위반 — 정상 플로우에서 발생하면 안 되는 프로그래밍 오류
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ApiResponse handleInvalidStateTransitionException(InvalidStateTransitionException e) {
        log.error("Invalid state transition detected: {}", e.getMessage(), e);
        return ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // 재고 상한 초과 — 결제 실패 복구 중 비정상 호출
    @ExceptionHandler(StockOverflowException.class)
    public ApiResponse handleStockOverflowException(StockOverflowException e) {
        log.error("Stock overflow detected: {}", e.getMessage(), e);
        return ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
