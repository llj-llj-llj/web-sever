package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.StudentLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentLeaveRepository extends JpaRepository<StudentLeave,Integer> {
    @Query(value = "select sl.* from student_leave sl left join student s on sl.student_id = s.person_id left join person sp on s.person_id = sp.person_id left join teacher t on sl.teacher_id = t.person_id left join person tp on t.person_id = tp.person_id where (?1 <0 or sl.state = ?1) and (?2='' or sp.name like %?2% or tp.name like %?2% or sl.reason like %?2%) and (?3='' or sp.num =?3) and (?4='' or tp.num =?4) and (?5 is null or sl.student_id =?5)", nativeQuery = true)
    List<StudentLeave> getStudentLeaveList(Integer state, String search, String studentNum, String teacherNum, Integer studentId);

    @Query(value="select s.student.personId, count(s.studentLeaveId) from StudentLeave s where s.student.personId in ?1 group by s.student.personId" )
    List<?> getStudentStatisticsList(List<Integer> personId);
}
