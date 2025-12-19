package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
 * 学生选课表 StudentCourse 数据操作接口
 */
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Integer> {

    @Query("""
        select c from Course c
        join StudentCourse sc on c.courseId = sc.courseId
        where sc.studentId = :studentId
    """)
    List<Course> findCoursesByStudentId(Integer studentId);

    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);

    void deleteByStudentIdAndCourseId(Integer studentId, Integer courseId);
}




