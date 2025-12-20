package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.StudentCourse;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.StudentCourseRepository;
import cn.edu.sdu.java.server.repositorys.TeacherCourseRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 我的课程 Service
 * 功能说明：
 *  - 学生：查询自己已选课程
 *  - 教师：查询自己所授课程
 *  - 管理员：查询全部课程
 */
@Service
public class MyCourseService {

    private final StudentCourseRepository studentCourseRepository;
    private final TeacherCourseRepository teacherCourseRepository;
    private final CourseRepository courseRepository;

    public MyCourseService(StudentCourseRepository studentCourseRepository,
                           TeacherCourseRepository teacherCourseRepository,
                           CourseRepository courseRepository) {
        this.studentCourseRepository = studentCourseRepository;
        this.teacherCourseRepository = teacherCourseRepository;
        this.courseRepository = courseRepository;
    }

    // 学生已选课程
    public DataResponse getStudentMyCourseList() {
        Integer studentId = CommonMethod.getPersonId();
        List<Course> list = studentCourseRepository.findCoursesByStudentId(studentId);
        return new DataResponse(0, list, "success");
    }

    // 教师所授课程
    public DataResponse getTeacherMyCourseList() {
        Integer teacherId = CommonMethod.getPersonId();
        List<Course> list = teacherCourseRepository.findCoursesByTeacherId(teacherId);
        return new DataResponse(0, list, "success");
    }

    // 学生选课
    public DataResponse selectCourse(Integer studentId, Integer courseId) {
        if (studentCourseRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            return new DataResponse(1, null, "已选该课程");
        }
        StudentCourse sc = new StudentCourse();
        sc.setStudentId(studentId);
        sc.setCourseId(courseId);
        studentCourseRepository.save(sc);
        return new DataResponse(0, null, "选课成功");
    }

    // 学生退课
    public DataResponse dropCourse(Integer studentId, Integer courseId) {
        studentCourseRepository.deleteByStudentIdAndCourseId(studentId, courseId);
        return new DataResponse(0, null, "退课成功");
    }
}
