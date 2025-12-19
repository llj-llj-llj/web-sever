package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.MyCourseService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/myCourse")
public class MyCourseController {

    private final MyCourseService myCourseService;

    public MyCourseController(MyCourseService myCourseService) {
        this.myCourseService = myCourseService;
    }

    /**
     * 学生：已选课程列表
     */
    @PostMapping("/student/list")
    public DataResponse studentCourseList() {
        return myCourseService.getStudentMyCourseList();
    }

    /**
     * 学生：选课
     */
    @PostMapping("/student/select")
    public DataResponse selectCourse(
            @RequestParam Integer studentId,
            @RequestParam Integer courseId
    ) {
        return myCourseService.selectCourse(studentId, courseId);
    }

    /**
     * 学生：退课
     */
    @PostMapping("/student/drop")
    public DataResponse dropCourse(
            @RequestParam Integer studentId,
            @RequestParam Integer courseId
    ) {
        return myCourseService.dropCourse(studentId, courseId);
    }

    /**
     * 教师：所授课程
     */
    @PostMapping("/teacher/list")
    public DataResponse teacherCourseList() {
        return myCourseService.getTeacherMyCourseList();
    }
}
