package com.youfare.domain.welfare;

import com.youfare.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "welfare")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Welfare extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    private WelfareCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer targetAgeMin;

    private Integer targetAgeMax;

    private String region;

    private String incomeCondition;

    private LocalDate applyStartDate;

    private LocalDate applyEndDate;

    private String sourceUrl;

    public void update(String title, WelfareCategory category, String description,
                       Integer targetAgeMin, Integer targetAgeMax, String region,
                       String incomeCondition, LocalDate applyStartDate, LocalDate applyEndDate,
                       String sourceUrl) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.targetAgeMin = targetAgeMin;
        this.targetAgeMax = targetAgeMax;
        this.region = region;
        this.incomeCondition = incomeCondition;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.sourceUrl = sourceUrl;
    }
}
