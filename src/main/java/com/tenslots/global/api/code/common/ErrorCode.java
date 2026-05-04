package com.tenslots.global.api.code.common;

import com.tenslots.global.api.code.ErrorResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorResultCode {

    // 400
    REQUEST_CONTENT_INVALID(HttpStatus.BAD_REQUEST, "올바르지 않은 요청 데이터입니다."),
    REQUIRED_PARAMETER_MISSED(HttpStatus.BAD_REQUEST, "필수 요청값이 존재하지 않습니다."),
    BOOKING_NOT_OPEN_YET(HttpStatus.BAD_REQUEST, "아직 판매 시작 전입니다."),
    INVALID_PAYMENT_COMBINATION(HttpStatus.BAD_REQUEST, "올바르지 않은 결제 수단 조합입니다."),
    PAYMENT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "결제에 실패했습니다."),

    // 404
    REQUEST_PATH_INVALID(HttpStatus.NOT_FOUND, "올바르지 않은 요청 경로입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // 405
    HTTP_METHOD_INVALID(HttpStatus.METHOD_NOT_ALLOWED, "올바르지 않은 HTTP 메서드입니다."),

    // 409
    STOCK_SOLD_OUT(HttpStatus.CONFLICT, "재고가 모두 소진되었습니다."),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "이미 처리 중인 요청입니다."),

    UNSUPPORTED_PAYMENT_METHOD(HttpStatus.BAD_REQUEST, "지원하지 않는 결제 수단입니다."),

    // 503
    LOCK_CONFLICT(HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 처리할 수 없습니다. 잠시 후 다시 시도해 주세요."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생하였습니다.");

    private final HttpStatus status;
    private final String message;
}
