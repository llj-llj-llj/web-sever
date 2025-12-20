package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public DataResponse getCourseList(DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        if(numName == null)
            numName = "";
        List<Course> cList = courseRepository.findCourseListByNumName(numName);  //数据库查询操作
        List<Map<String,Object>> dataList = new ArrayList<>();
        Map<String,Object> m;
        Course pc;
        for (Course c : cList) {
            m = new HashMap<>();
            m.put("courseId", c.getCourseId());
            m.put("num",c.getNum());
            m.put("name",c.getName());
            m.put("credit",c.getCredit());
            m.put("coursePath",c.getCoursePath());

            m.put("classTime", c.getClassTime());
            m.put("location", c.getLocation());

            pc =c.getPreCourse();
            if(pc != null) {
                m.put("preCourse",pc.getName());
                m.put("preCourseId",pc.getCourseId());
            }
            dataList.add(m);
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse courseSave(DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        String num = dataRequest.getString("num");
        String name = dataRequest.getString("name");
        String coursePath = dataRequest.getString("coursePath");
        Integer credit = dataRequest.getInteger("credit");
        Integer preCourseId = dataRequest.getInteger("preCourseId");
        String classTime = dataRequest.getString("classTime");
        String location = dataRequest.getString("location");
        Integer personId = dataRequest.getInteger("personId");

        Course c;

        // 1️⃣ 先保证 c 一定不为 null
        if (courseId != null) {
            c = courseRepository.findById(courseId).orElse(new Course());
        } else {
            c = new Course();
        }

        // 2️⃣ 前序课
        Course pc = null;
        if (preCourseId != null) {
            pc = courseRepository.findById(preCourseId).orElse(null);
        }

        // 3️⃣ 赋值
        c.setNum(num);
        c.setName(name);
        c.setCredit(credit);
        c.setCoursePath(coursePath);
        c.setPreCourse(pc);
        c.setClassTime(classTime);
        c.setLocation(location);

        // 4️⃣ personId 可为 null（重点）
        if (personId != null) {
            c.setPersonId(Long.valueOf(personId));
        } else {
            c.setPersonId(null);
        }

        // 5️⃣ 保存
        courseRepository.save(c);

        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse courseDelete(DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        Optional<Course> op;
        Course c= null;
        if(courseId != null) {
            op = courseRepository.findById(courseId);
            if(op.isPresent()) {
                c = op.get();
                courseRepository.delete(c);
            }
        }
        return CommonMethod.getReturnMessageOK();
    }
    public DataResponse getTeacherCourseList(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        if (personId == null) {
            return CommonMethod.getReturnMessageError("教师ID不能为空");
        }

        List<Course> list =
                courseRepository.findByPersonId(Long.valueOf(personId));

        List<Map<String, Object>> dataList = new ArrayList<>();

        for (Course c : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("courseId", c.getCourseId());
            m.put("num", c.getNum());
            m.put("name", c.getName());
            m.put("credit", c.getCredit());
            m.put("classTime", c.getClassTime());
            m.put("location", c.getLocation());
            dataList.add(m);
        }

        return CommonMethod.getReturnData(dataList);
    }


}
