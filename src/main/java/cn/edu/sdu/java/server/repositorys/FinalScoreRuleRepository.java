package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.FinalScoreRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * FinalScoreRule 数据操作接口，主要实现FinalScoreRule数据的查询操作
 */
@Repository
public interface FinalScoreRuleRepository extends JpaRepository<FinalScoreRule, Integer> {
    
    // 根据考试类型查询规则
    Optional<FinalScoreRule> findByExamType(String examType);
    
    // 查询所有默认规则
    List<FinalScoreRule> findByIsDefaultTrue();
    
    // 检查考试类型是否已存在
    boolean existsByExamType(String examType);
    
    // 根据规则ID删除规则
    void deleteByRuleId(Integer ruleId);
}