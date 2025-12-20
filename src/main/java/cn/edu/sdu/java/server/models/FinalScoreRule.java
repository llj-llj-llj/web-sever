package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * FinalScoreRule 最终成绩计算规则表实体类
 * 保存最终成绩计算的规则信息
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "final_score_rule")
public class FinalScoreRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ruleId;

    @Column(name = "exam_type")
    private String examType; // 考试类型：期中考试、期末考试、平时成绩等

    private Double weight; // 权重

    private String description; // 描述

    @Column(name = "is_default")
    private Boolean isDefault; // 是否默认规则

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}