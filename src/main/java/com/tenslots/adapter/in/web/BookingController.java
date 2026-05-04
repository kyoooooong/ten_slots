package com.tenslots.adapter.in.web;

import com.tenslots.adapter.in.web.dto.BookingRequest;
import com.tenslots.application.port.in.BookingResponse;
import com.tenslots.application.port.in.BookingUseCase;
import com.tenslots.global.api.code.common.SuccessCode;
import com.tenslots.global.api.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Booking", description = "예약/결제 API")
@RestController
@RequestMapping("/api/v1/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingUseCase bookingUseCase;

    @Operation(summary = "예약 및 결제", description = "결제를 진행하고 예약을 확정합니다. 멱등성 키 필수.")
    @PostMapping
    public SuccessResponse<BookingResponse> book(@Valid @RequestBody BookingRequest request) {
        return SuccessResponse.of(SuccessCode.BOOKING_SUCCESS, bookingUseCase.book(request.toCommand()));
    }
}
