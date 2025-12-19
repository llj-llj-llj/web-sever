package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.TeacherCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
 * 教师授课表 TeacherCourse 数据操作接口
 */
public interface TeacherCourseRepository extends JpaRepository<TeacherCourse, Integer> {

    @Query("""
        select c from Course c
        join TeacherCourse tc on c.courseId = tc.courseId
        where tc.teacherId = :teacherId
    """)
    List<Course> findCoursesByTeacherId(Integer teacherId);
}

