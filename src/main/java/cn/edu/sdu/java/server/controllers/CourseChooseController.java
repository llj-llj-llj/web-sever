package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.CourseChooseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courseChoose")
@CrossOrigin
public class CourseChooseController {

    private final CourseChooseService service;

    public CourseChooseController(CourseChooseService service) {
        this.service = service;
    }

    /** 学生已选课程 ID */
    @PostMapping("/list")
    public DataResponse list(@RequestBody DataRequest request) {
        return service.getMyCourseList(request);
    }

    /** 学生选课 */
    @PostMapping("/select")
    public DataResponse select(@RequestBody DataRequest request) {
        return service.selectCourse(request);
    }

    /** 学生退课 */
    @PostMapping("/drop")
    public DataResponse drop(@RequestBody DataRequest request) {
        return service.dropCourse(request);
    }
}
