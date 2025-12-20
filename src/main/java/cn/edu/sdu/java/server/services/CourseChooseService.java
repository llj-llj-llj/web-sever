package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.CourseChoose;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseChooseRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseChooseService {

    private final CourseChooseRepository courseChooseRepository;

    public CourseChooseService(CourseChooseRepository repo) {
        this.courseChooseRepository = repo;
    }

    /** 学生已选课程 ID 列表 */
    public DataResponse getSelectedCourseList(DataRequest request) {
        Integer studentId = request.getInteger("studentId");

        List<CourseChoose> list =
                courseChooseRepository.findByStudentId(studentId);

        List<Integer> courseIds = new ArrayList<>();
        for (CourseChoose cc : list) {
            courseIds.add(cc.getCourseId());
        }

        return CommonMethod.getReturnData(courseIds);
    }

    /** 选课 */
    public DataResponse selectCourse(DataRequest request) {
        Integer studentId = request.getInteger("studentId");
        Integer courseId = request.getInteger("courseId");

        if (courseChooseRepository
                .existsByStudentIdAndCourseId(studentId, courseId)) {
            return CommonMethod.getReturnMessageError("该课程已选");
        }

        CourseChoose cc = new CourseChoose();
        cc.setStudentId(studentId);
        cc.setCourseId(courseId);

        courseChooseRepository.save(cc);

        return CommonMethod.getReturnMessageOK("选课成功");
    }

    /** 退课 */
    public DataResponse dropCourse(DataRequest request) {
        Integer studentId = request.getInteger("studentId");
        Integer courseId = request.getInteger("courseId");

        CourseChoose cc =
                courseChooseRepository
                        .findByStudentIdAndCourseId(studentId, courseId);

        if (cc == null) {
            return CommonMethod.getReturnMessageError("未选该课程");
        }

        courseChooseRepository.delete(cc);
        return CommonMethod.getReturnMessageOK("退课成功");
    }
}
