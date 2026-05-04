package com.tenslots.domain.user;

import com.tenslots.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@Comment("사용자")
@SQLRestriction("deleted_at is null")
@SQLDelete(sql = "update users set deleted_at = now() where id = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("사용자 ID")
    private Long id;

    @Column(nullable = false, length = 100)
    @Comment("사용자명")
    private String name;

    @Column(nullable = false)
    @Comment("가용 포인트")
    private Long point;

    @Builder
    private User(String name, Long point) {
        this.name = name;
        this.point = point;
    }
}
