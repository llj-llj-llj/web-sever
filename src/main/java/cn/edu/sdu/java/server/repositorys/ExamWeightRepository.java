package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.ExamWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 考试权重数据操作接口
 */
@Repository
public interface ExamWeightRepository extends JpaRepository<ExamWeight, Integer> {
    
    /**
     * 根据课程ID和考试类型查找权重
     */
    Optional<ExamWeight> findByCourseCourseIdAndExamType(Integer courseId, String examType);
    
    /**
     * 根据课程ID查找所有权重配置
     */
    List<ExamWeight> findByCourseCourseId(Integer courseId);
    
    /**
     * 查找所有指定考试类型的权重配置
     */
    List<ExamWeight> findByExamType(String examType);
    
    /**
     * 检查权重配置是否存在
     */
    boolean existsByCourseCourseIdAndExamType(Integer courseId, String examType);
    
    /**
     * 获取指定课程的所有权重总和
     */
    @Query("SELECT SUM(e.weight) FROM ExamWeight e WHERE e.course.courseId = :courseId")
    Double getTotalWeightByCourseId(@Param("courseId") Integer courseId);
    
    /**
     * 获取指定课程的默认权重配置（如果没有特定配置）
     */
    @Query(value = "SELECT * FROM exam_weight WHERE courseId = 0 AND examType = :examType", nativeQuery = true)
    Optional<ExamWeight> findDefaultWeightByExamType(@Param("examType") String examType);
}