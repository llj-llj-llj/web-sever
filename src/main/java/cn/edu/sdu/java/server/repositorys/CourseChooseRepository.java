package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.CourseChoose;
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
}
