package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Score;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
/*
 * Score 数据操作接口，主要实现Score数据的查询操作
 * List<Score> findByStudentPersonId(Integer personId);  根据关联的Student的student_id查询获得List<Score>对象集合,  命名规范
 */

@Repository
public interface ScoreRepository extends JpaRepository<Score,Integer> {
    List<Score> findByStudentPersonId(Integer personId);
    @Query(value="from Score where (?1=0 or student.personId=?1) and (?2=0 or course.courseId=?2)" )
    List<Score> findByStudentCourse(Integer personId, Integer courseId);

    @Query(value="from Score where student.personId=?1 and (?2=0 or course.name like %?2%)" )
    List<Score> findByStudentCourse(Integer personId, String courseName);

    @Query(value="select s.student.personId, count(s.scoreId), sum(s.mark),sum(s.course.credit),sum(s.course.credit* s.mark) from Score s where s.student.personId in ?1 group by s.student.personId" )
    List<?> getStudentStatisticsList(List<Integer> personId);

    // 新增：检查重复性 - studentId + courseId + examType 唯一性检查
    boolean existsByStudentPersonIdAndCourseCourseIdAndExamType(Integer personId, Integer courseId, String examType);
/*
    // 新增：按考试类型查询成绩
    @Query("FROM Score WHERE (:personId = 0 OR student.personId = :personId) " +
           "AND (:courseId = 0 OR course.courseId = :courseId) " +
           "AND (:examType IS NULL OR examType = :examType)")
    List<Score> findByStudentCourseAndExamType(@Param("personId") Integer personId, 
                                               @Param("courseId") Integer courseId, 
                                               @Param("examType") String examType);

    // 新增：分页查询成绩
    @Query("FROM Score WHERE (:personId = 0 OR student.personId = :personId) " +
           "AND (:courseId = 0 OR course.courseId = :courseId) " +
           "AND (:examType IS NULL OR examType = :examType)")
    Page<Score> findByStudentCourseAndExamType(@Param("personId") Integer personId, 
                                               @Param("courseId") Integer courseId, 
                                               @Param("examType") String examType, 
                                               Pageable pageable);
*/

    // 按考试类型查询成绩（不分页）
    @Query("""
    SELECT s FROM Score s
    WHERE (:personId = 0 OR s.student.personId = :personId)
      AND (:courseId = 0 OR s.course.courseId = :courseId)
      AND (:examType IS NULL OR s.examType = :examType)
""")
    List<Score> findByStudentCourseAndExamType(@Param("personId") Integer personId,
                                               @Param("courseId") Integer courseId,
                                               @Param("examType") String examType);

    // 分页查询成绩（关键：加 countQuery）
    @Query(
            value = """
    SELECT s FROM Score s
    WHERE (:personId = 0 OR s.student.personId = :personId)
      AND (:courseId = 0 OR s.course.courseId = :courseId)
      AND (:examType IS NULL OR s.examType = :examType)
  """,
            countQuery = """
    SELECT COUNT(s) FROM Score s
    WHERE (:personId = 0 OR s.student.personId = :personId)
      AND (:courseId = 0 OR s.course.courseId = :courseId)
      AND (:examType IS NULL OR s.examType = :examType)
  """
    )
    Page<Score> findByStudentCourseAndExamType(@Param("personId") Integer personId,
                                               @Param("courseId") Integer courseId,
                                               @Param("examType") String examType,
                                               Pageable pageable);

    // 新增：按课程和考试类型获取成绩，用于排名计算（按分数降序）
    @Query("FROM Score WHERE course.courseId = :courseId AND examType = :examType ORDER BY mark DESC")
    List<Score> findByCourseIdAndExamTypeOrderByMarkDesc(@Param("courseId") Integer courseId, 
                                                         @Param("examType") String examType);

    // 新增：统计查询 - 用于分页信息
    @Query("SELECT COUNT(s) FROM Score s WHERE " +
           "(:personId = 0 OR s.student.personId = :personId) AND " +
           "(:courseId = 0 OR s.course.courseId = :courseId) AND " +
           "(:examType IS NULL OR s.examType = :examType)")
    Long countByStudentCourseAndExamType(@Param("personId") Integer personId, 
                                         @Param("courseId") Integer courseId, 
                                         @Param("examType") String examType);

    // 新增：获取所有课程和考试类型的组合，用于批量重算排名
    @Query("SELECT DISTINCT s.course.courseId, s.examType FROM Score s WHERE s.examType IS NOT NULL")
    List<Object[]> findAllCourseExamTypeCombinations();

    // 新增：按课程和考试类型获取成绩，用于PDF生成（按分数降序）
    @Query("FROM Score WHERE course.courseId = :courseId AND examType = :examType ORDER BY mark DESC")
    List<Score> findByCourseCourseIdAndExamTypeOrderByMarkDesc(@Param("courseId") Integer courseId, 
                                                                @Param("examType") String examType);

    // 新增：按学生获取所有成绩，用于学生个人成绩单PDF生成
    /*@Query("FROM Score WHERE student.person.personId = :personId ORDER BY examType ASC, course.name ASC")
    List<Score> findByStudentPersonPersonIdOrderByExamTypeAscCourseNameAsc(@Param("personId") Integer personId);
    */
    @Query("""
    SELECT s FROM Score s
    WHERE s.student.personId = :personId
    ORDER BY s.examType ASC, s.course.name ASC
""")
    List<Score> findByStudentPersonIdOrderByExamTypeAscCourseNameAsc(@Param("personId") Integer personId);

}
