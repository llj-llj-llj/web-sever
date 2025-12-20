package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 考试权重实体类
 * 存储不同科目和考试类型的权重配置
 */
@Getter
@Setter
@Entity
@Table(name = "exam_weight")
public class ExamWeight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer weightId;

    @ManyToOne
    @JoinColumn(name = "courseId", nullable = false)
    private Course course;

    @Column(name = "examType", nullable = false)
    private String examType; // 考试类型：期中考试、期末考试、平时成绩、模拟考试

    @Column(name = "weight", nullable = false)
    private Double weight; // 权重值，如0.6表示60%

    @Column(name = "description")
    private String description; // 权重说明

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