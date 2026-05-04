package com.tenslots.adapter.out.payment;

import com.tenslots.application.port.out.PaymentGatewayPort;
import com.tenslots.application.port.out.PaymentProcessorPort;
import com.tenslots.domain.payment.PaymentMethod;
import com.tenslots.global.api.code.common.ErrorCode;
import com.tenslots.global.exception.BusinessException;
import com.tenslots.global.exception.PaymentTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PaymentGatewayRouter implements PaymentProcessorPort {

    // Virtual Thread — PG I/O 대기 중 OS 스레드를 점유하지 않아 커넥션 고갈 없음
    private static final ExecutorService PG_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<PaymentMethod, PaymentGatewayPort> gatewayMap;
    private final long timeoutSeconds;

    public PaymentGatewayRouter(
            List<PaymentGatewayPort> gateways,
            @Value("${payment.gateway.timeout-seconds:10}") long timeoutSeconds
    ) {
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(PaymentGatewayPort::supportedMethod, Function.identity()));
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public PaymentGatewayPort.PaymentResult process(PaymentMethod method, PaymentGatewayPort.PaymentRequest request) {
        CompletableFuture<PaymentGatewayPort.PaymentResult> future =
                CompletableFuture.supplyAsync(() -> getGateway(method).process(request), PG_EXECUTOR);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // 백그라운드 스레드 누수 방지
            log.warn("PG call timed out - pgIdempotencyKey: {}", request.pgIdempotencyKey());
            throw new PaymentTimeoutException(request.pgIdempotencyKey());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public PaymentGatewayPort.PaymentResult inquiry(PaymentMethod method, String pgIdempotencyKey) {
        return getGateway(method).inquiry(pgIdempotencyKey);
    }

    @Override
    public void cancel(PaymentMethod method, String pgTransactionId) {
        getGateway(method).cancel(pgTransactionId);
    }

    @PreDestroy
    public void shutdown() {
        // 진행 중인 PG 호출이 완료될 때까지 대기해 PENDING 건 최소화
        PG_EXECUTOR.shutdown();
        try {
            if (!PG_EXECUTOR.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("PG executor did not terminate within {} seconds, forcing shutdown", timeoutSeconds);
                PG_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            PG_EXECUTOR.shutdownNow();
        }
    }

    private PaymentGatewayPort getGateway(PaymentMethod method) {
        PaymentGatewayPort gateway = gatewayMap.get(method);
        if (gateway == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_PAYMENT_METHOD);
        }
        return gateway;
    }
}
