package com.tenslots.domain.payment;

import com.tenslots.domain.common.BaseEntity;
import com.tenslots.domain.common.InvalidStateTransitionException;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Comment("결제")
@SQLRestriction("deleted_at is null")
@SQLDelete(sql = "update payment set deleted_at = now() where id = ?")
@Table(indexes = {
        @Index(name = "idx_payment_booking_id", columnList = "booking_id"),
        @Index(name = "idx_payment_pg_idempotency_key", columnList = "pg_idempotency_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @Tsid
    @Column(length = 13)
    @Comment("결제 ID (TSID)")
    private String id;

    @Column(length = 13, nullable = false)
    @Comment("예약 ID")
    private String bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("결제 수단")
    private PaymentMethod method;

    @Column(nullable = false, precision = 12, scale = 2)
    @Comment("결제 금액")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("결제 상태 (REQUESTED/APPROVED/CONFIRMED/FAILED)")
    private PaymentStatus status;

    @Column(length = 255)
    @Comment("PG사 거래번호")
    private String pgTransactionId;

    @Column(nullable = false, length = 255)
    @Comment("PG사 멱등키")
    private String pgIdempotencyKey;

    @Builder
    private Payment(String bookingId, PaymentMethod method, BigDecimal amount, PaymentStatus status, String pgTransactionId, String pgIdempotencyKey) {
        this.bookingId = bookingId;
        this.method = method;
        this.amount = amount;
        this.status = status;
        this.pgTransactionId = pgTransactionId;
        this.pgIdempotencyKey = pgIdempotencyKey;
    }

    public void approve(String pgTransactionId) {
        if (this.status != PaymentStatus.REQUESTED) {
            throw new InvalidStateTransitionException(this.status.name(), PaymentStatus.APPROVED.name());
        }
        this.status = PaymentStatus.APPROVED;
        this.pgTransactionId = pgTransactionId;
    }

    public void confirm() {
        if (this.status != PaymentStatus.APPROVED) {
            throw new InvalidStateTransitionException(this.status.name(), PaymentStatus.CONFIRMED.name());
        }
        this.status = PaymentStatus.CONFIRMED;
    }

    public void fail() {
        if (this.status == PaymentStatus.CONFIRMED) {
            throw new InvalidStateTransitionException(this.status.name(), PaymentStatus.FAILED.name());
        }
        this.status = PaymentStatus.FAILED;
    }
}
