package com.youfare.domain.scrap;

import com.youfare.domain.user.User;
import com.youfare.domain.welfare.Welfare;
import com.youfare.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "scrap",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "welfare_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Scrap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "welfare_id", nullable = false)
    private Welfare welfare;

    public static Scrap of(User user, Welfare welfare) {
        return Scrap.builder()
                .user(user)
                .welfare(welfare)
                .build();
    }
}
