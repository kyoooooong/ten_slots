package com.tenslots.global.api.code.common;

import com.tenslots.global.api.code.SuccessResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements SuccessResultCode {

    CHECKOUT_SUCCESS(HttpStatus.OK, "주문서 조회 성공"),
    BOOKING_SUCCESS(HttpStatus.CREATED, "예약 및 결제 완료");

    private final HttpStatus status;
    private final String message;
}
