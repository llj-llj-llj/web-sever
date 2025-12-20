package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final FinalScoreCalculationService finalScoreCalculationService;
    
    /**
     * 计算指定课程和考试类型的排名（基于加权平均分）
     * @param courseId 课程ID
     * @param examType 考试类型
     */
    public void calculateRanking(Integer courseId, String examType) {
        log.info("开始计算排名 - 课程ID: {}, 考试类型: {}", courseId, examType);
        
        List<Score> scores = scoreRepository.findByCourseCourseIdAndExamTypeOrderByMarkDesc(courseId, examType);
        
        if (scores.isEmpty()) {
            log.warn("未找到成绩数据 - 课程ID: {}, 考试类型: {}", courseId, examType);
            return;
        }
        
        // 按学生分组，计算每个学生的加权平均分
        Map<Integer, List<Score>> scoresByStudent = scores.stream()
            .collect(Collectors.groupingBy(score -> score.getStudent().getPersonId()));
        
        // 创建学生成绩映射：学生ID -> 加权平均分
        Map<Integer, Double> studentWeightedScores = new java.util.HashMap<>();
        
        for (Map.Entry<Integer, List<Score>> entry : scoresByStudent.entrySet()) {
            Integer studentId = entry.getKey();
            List<Score> studentScores = entry.getValue();
            
            // 计算该学生该课程的加权平均分
            try {
                double weightedScore = finalScoreCalculationService.calculateCourseFinalScore(studentId, courseId);
                studentWeightedScores.put(studentId, weightedScore);
            } catch (Exception e) {
                log.warn("计算学生 {} 课程 {} 的加权平均分失败", studentId, courseId, e);
                // 如果计算失败，使用原始分数
                studentWeightedScores.put(studentId, studentScores.get(0).getMark().doubleValue());
            }
        }
        
        // 按加权平均分降序排序学生
        List<Map.Entry<Integer, Double>> sortedStudents = studentWeightedScores.entrySet()
            .stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        // 计算排名
        int currentRank = 0;
        Double lastScore = null;
        int rankCount = 0;
        
        for (Map.Entry<Integer, Double> entry : sortedStudents) {
            Integer studentId = entry.getKey();
            Double weightedScore = entry.getValue();
            
            rankCount++;
            
            // 处理并列排名
            if (lastScore == null || !weightedScore.equals(lastScore)) {
                currentRank = rankCount;
                lastScore = weightedScore;
            }
            
            // 更新该学生该课程所有成绩记录的排名
            List<Score> studentScores = scoresByStudent.get(studentId);
            for (Score score : studentScores) {
                score.setRanking(currentRank);
                log.debug("设置排名 - 学生ID: {}, 加权平均分: {}, 排名: {}", 
                         studentId, weightedScore, currentRank);
            }
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
    
    /**
     * 计算班级排名（基于加权平均分）
     * @param className 班级名称
     * @return 学生排名映射：学生ID -> 排名
     */
    public Map<Integer, Integer> calculateClassRanking(String className) {
        log.info("计算班级排名 - 班级: {}", className);
        
        // 获取班级中所有学生
        List<Integer> studentIds = scoreRepository.findStudentIdsByClassName(className);
        
        if (studentIds.isEmpty()) {
            log.warn("班级 {} 中没有学生", className);
            return new java.util.HashMap<>();
        }
        
        // 创建学生成绩映射：学生ID -> 加权平均分
        Map<Integer, Double> studentWeightedScores = new java.util.HashMap<>();
        
        for (Integer studentId : studentIds) {
            try {
                // 计算学生的总加权平均分
                double weightedScore = finalScoreCalculationService.calculateStudentFinalScore(studentId);
                studentWeightedScores.put(studentId, weightedScore);
            } catch (Exception e) {
                log.warn("计算学生 {} 的加权平均分失败", studentId, e);
                // 如果计算失败，使用0分
                studentWeightedScores.put(studentId, 0.0);
            }
        }
        
        // 按加权平均分降序排序学生
        List<Map.Entry<Integer, Double>> sortedStudents = studentWeightedScores.entrySet()
            .stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        // 计算排名
        Map<Integer, Integer> studentRankings = new java.util.HashMap<>();
        int currentRank = 0;
        Double lastScore = null;
        int rankCount = 0;
        
        for (Map.Entry<Integer, Double> entry : sortedStudents) {
            Integer studentId = entry.getKey();
            Double weightedScore = entry.getValue();
            
            rankCount++;
            
            // 处理并列排名
            if (lastScore == null || !weightedScore.equals(lastScore)) {
                currentRank = rankCount;
                lastScore = weightedScore;
            }
            
            studentRankings.put(studentId, currentRank);
            log.debug("设置班级排名 - 学生ID: {}, 加权平均分: {}, 排名: {}", 
                     studentId, weightedScore, currentRank);
        }
        
        return studentRankings;
    }
}