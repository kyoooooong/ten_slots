package com.tenslots.adapter.in.web;

import com.tenslots.application.port.in.CheckoutResponse;
import com.tenslots.application.port.in.CheckoutUseCase;
import com.tenslots.global.api.code.common.SuccessCode;
import com.tenslots.global.api.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Checkout", description = "주문서 API")
@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutUseCase checkoutUseCase;

    @Operation(summary = "주문서 조회", description = "상품 정보와 사용자 보유 포인트를 조회합니다.")
    @GetMapping("/{productId}")
    public SuccessResponse<CheckoutResponse> checkout(
            @Parameter(description = "상품 ID") @PathVariable String productId,
            @Parameter(description = "사용자 ID") @RequestParam Long userId
    ) {
        return SuccessResponse.of(SuccessCode.CHECKOUT_SUCCESS, checkoutUseCase.checkout(productId, userId));
    }
}
