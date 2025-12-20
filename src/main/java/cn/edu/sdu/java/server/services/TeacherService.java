package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final PersonRepository personRepository;

    public TeacherService(TeacherRepository teacherRepository,
                          PersonRepository personRepository) {
        this.teacherRepository = teacherRepository;
        this.personRepository = personRepository;
    }

    /**
     * 教师列表
     */
    public DataResponse getTeacherList(DataRequest dataRequest) {
        List<Teacher> list = teacherRepository.findAll();
        DataResponse res = new DataResponse();
        res.setCode(0);
        res.setData(list);
        res.setMsg(null);

        return res;
    }

    /**
     * 删除教师
     */
    @Transactional
    public DataResponse teacherDelete(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        if (personId == null) {
            return new DataResponse();
        }

        teacherRepository.deleteById(personId);
        personRepository.deleteById(personId);
        DataResponse res = new DataResponse();
        res.setCode(0);
        res.setMsg("删除成功");

        return res;
    }

    /**
     * 新增 / 编辑教师
     */
    @Transactional
    public DataResponse teacherEditSave(DataRequest dataRequest) {

        Integer personId = dataRequest.getInteger("personId");
        Map<String, Object> personMap = dataRequest.getMap("person");

        Person person;
        Teacher teacher;

        if (personId == null) {
            person = new Person();
        } else {
            person = personRepository.findById(personId)
                    .orElseThrow(() -> new RuntimeException("Person不存在"));
        }

        // ===== Person（和 Student 完全一致）=====
        person.setNum((String) personMap.get("num"));
        person.setName((String) personMap.get("name"));
        person.setDept((String) personMap.get("dept"));
        person.setCard((String) personMap.get("card"));
        person.setGender((String) personMap.get("gender"));
        person.setBirthday((String) personMap.get("birthday"));
        person.setEmail((String) personMap.get("email"));
        person.setPhone((String) personMap.get("phone"));
        person.setAddress((String) personMap.get("address"));

        personRepository.save(person);

        // ===== Teacher =====
        if (personId == null) {
            teacher = new Teacher();
        } else {
            teacher = teacherRepository.findById(person.getPersonId())
                    .orElseGet(Teacher::new);
        }

        teacher.setPerson(person);
        teacher.setTitle(dataRequest.getString("title"));
        teacher.setDegree(dataRequest.getString("degree"));

        teacherRepository.save(teacher);
        DataResponse res = new DataResponse();
        res.setCode(0);
        res.setMsg("保存成功");
        res.setData(person.getPersonId());
        return res;
    }
    public DataResponse getTeacherInfo(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");

        Teacher teacher = teacherRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        DataResponse res = new DataResponse();
        res.setCode(0);
        res.setData(teacher);
        return res;
    }

}
