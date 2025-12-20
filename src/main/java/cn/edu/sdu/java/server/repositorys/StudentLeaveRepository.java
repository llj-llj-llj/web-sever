package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.StudentLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentLeaveRepository extends JpaRepository<StudentLeave,Integer> {
    @Query("""
select sl from StudentLeave sl
left join sl.teacher t
left join t.person tp
where
 (?1 < 0 or sl.state = ?1)
 and (
      ?2 = '' or
      sl.student.person.name like %?2% or
      tp.name like %?2% or
      sl.reason like %?2%
 )
 and (?3 = '' or sl.student.person.num = ?3)
 and (?4 = '' or tp.num = ?4)
""")
    List<StudentLeave> getStudentLeaveList(
            Integer state,
            String search,
            String studentNum,
            String teacherNum
    );


    @Query(value="select s.student.personId, count(s.studentLeaveId) from StudentLeave s where s.student.personId in ?1 group by s.student.personId" )
    List<?> getStudentStatisticsList(List<Integer> personId);
}
