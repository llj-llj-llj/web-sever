package cn.edu.sdu.java.server.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/*
 * Selection 选课记录表
 * Integer id 主键
 * Student student 学生（多对一）
 * Course course 课程（多对一）
 */
@Entity
@Table(
        name = "student_course",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"})
)
@Getter
@Setter
public class CourseChoose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;
}
