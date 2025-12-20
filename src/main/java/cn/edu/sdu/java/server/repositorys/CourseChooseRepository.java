package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseChoose;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseChooseRepository
        extends JpaRepository<CourseChoose, Integer> {

    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);

    List<CourseChoose> findByStudentId(Integer studentId);

    CourseChoose findByStudentIdAndCourseId(Integer studentId, Integer courseId);

    @Query("""
        select c
        from Course c
        join CourseChoose cc on cc.courseId = c.courseId
        where cc.studentId = :studentId
    """)
    List<Course> findCoursesByStudentId(@Param("studentId") Integer studentId);
}
