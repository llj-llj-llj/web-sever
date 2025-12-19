package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 排名计算服务
 * 负责成绩排名的自动计算和更新
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RankingCalculationService {
    
    private final ScoreRepository scoreRepository;
    
    /**
     * 计算指定课程和考试类型的排名
     * @param courseId 课程ID
     * @param examType 考试类型
     */
    public void calculateRanking(Integer courseId, String examType) {
        log.info("开始计算排名 - 课程ID: {}, 考试类型: {}", courseId, examType);
        
        List<Score> scores = scoreRepository.findByCourseIdAndExamTypeOrderByMarkDesc(courseId, examType);
        
        if (scores.isEmpty()) {
            log.warn("未找到成绩数据 - 课程ID: {}, 考试类型: {}", courseId, examType);
            return;
        }
        
        int currentRank = 0;
        Integer lastScore = null;
        int rankCount = 0;
        
        for (Score score : scores) {
            rankCount++;
            
            // 处理并列排名
            if (lastScore == null || !score.getMark().equals(lastScore)) {
                currentRank = rankCount;
                lastScore = score.getMark();
            }
            
            score.setRanking(currentRank);
            log.debug("设置排名 - 学生ID: {}, 分数: {}, 排名: {}", 
                     score.getStudent().getPersonId(), score.getMark(), currentRank);
        }
        
        scoreRepository.saveAll(scores);
        log.info("排名计算完成 - 共处理 {} 条记录", scores.size());
    }
    
    /**
     * 重新计算所有排名
     * 用于数据修复或批量更新后的排名重算
     */
    @Transactional
    public void recalculateAllRankings() {
        log.info("开始重新计算所有排名");
        
        // 获取所有不同的课程和考试类型组合
        List<Object[]> combinations = scoreRepository.findAllCourseExamTypeCombinations();
        
        int totalCombinations = combinations.size();
        int processedCount = 0;
        
        for (Object[] combination : combinations) {
            Integer courseId = (Integer) combination[0];
            String examType = (String) combination[1];
            
            try {
                calculateRanking(courseId, examType);
                processedCount++;
                log.info("进度: {}/{} - 课程ID: {}, 考试类型: {}", 
                        processedCount, totalCombinations, courseId, examType);
            } catch (Exception e) {
                log.error("计算排名失败 - 课程ID: {}, 考试类型: {}, 错误: {}", 
                         courseId, examType, e.getMessage(), e);
            }
        }
        
        log.info("所有排名重新计算完成 - 总计: {}, 成功: {}", totalCombinations, processedCount);
    }
    
    /**
     * 计算单个学生的排名
     * @param personId 学生ID
     * @param courseId 课程ID  
     * @param examType 考试类型
     */
    public void calculateStudentRanking(Integer personId, Integer courseId, String examType) {
        log.info("计算学生排名 - 学生ID: {}, 课程ID: {}, 考试类型: {}", personId, courseId, examType);
        
        // 重新计算该课程该考试类型的所有排名
        calculateRanking(courseId, examType);
    }
}