package com.tenslots.domain.booking;

import com.tenslots.domain.common.BaseEntity;
import com.tenslots.domain.common.InvalidStateTransitionException;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Comment("예약")
@SQLRestriction("deleted_at is null")
@SQLDelete(sql = "update booking set deleted_at = now() where id = ?")
@Table(indexes = {
        @Index(name = "idx_booking_user_product_status", columnList = "user_id, product_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking extends BaseEntity {

    @Id
    @Tsid
    @Column(length = 13)
    @Comment("예약 ID (TSID)")
    private String id;

    @Column(nullable = false)
    @Comment("사용자 ID")
    private Long userId;

    @Column(length = 13, nullable = false)
    @Comment("상품 ID")
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("예약 상태 (PENDING/CONFIRMED/FAILED)")
    private BookingStatus status;

    @Column(columnDefinition = "DATETIME")
    @Comment("확정일시 (UTC)")
    private OffsetDateTime confirmedAt;

    @Builder
    private Booking(Long userId, String productId, BookingStatus status) {
        this.userId = userId;
        this.productId = productId;
        this.status = status;
    }

    public void confirm() {
        if (this.status != BookingStatus.PENDING) {
            throw new InvalidStateTransitionException(this.status.name(), BookingStatus.CONFIRMED.name());
        }
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void fail() {
        if (this.status != BookingStatus.PENDING) {
            throw new InvalidStateTransitionException(this.status.name(), BookingStatus.FAILED.name());
        }
        this.status = BookingStatus.FAILED;
    }
}
