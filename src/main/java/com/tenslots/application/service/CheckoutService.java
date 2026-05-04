package com.tenslots.application.service;

import com.tenslots.application.port.in.CheckoutResponse;
import com.tenslots.application.port.in.CheckoutUseCase;
import com.tenslots.application.port.out.LoadProductPort;
import com.tenslots.application.port.out.LoadUserPort;
import com.tenslots.domain.product.Product;
import com.tenslots.global.api.code.common.ErrorCode;
import com.tenslots.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutService implements CheckoutUseCase {

    private final LoadProductPort loadProductPort;
    private final LoadUserPort loadUserPort;

    @Override
    public CheckoutResponse checkout(String productId, Long userId) {
        // 주문서 조회는 오픈 전에도 허용 — 사용자가 00시 전에 미리 준비할 수 있어야 함
        // openAt 제한은 실제 예약(BookingService)에서만 적용
        Product product = loadProductPort.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        Long point = loadUserPort.getAvailablePoint(userId);
        return CheckoutResponse.of(product, point);
    }
}
