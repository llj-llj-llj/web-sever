package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.FinalScoreRule;
import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.repositorys.FinalScoreRuleRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 最终成绩计算服务类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinalScoreCalculationService {
    
    private final FinalScoreRuleRepository finalScoreRuleRepository;
    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;
    private final ExamWeightService examWeightService;
    
    /**
     * 计算单门课程的最终成绩（基于加权平均分）
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 最终成绩
     */
    @Transactional
    public Double calculateCourseFinalScore(Integer studentId, Integer courseId) {
        // 获取该学生该课程的所有成绩
        List<Score> scores = scoreRepository.findByStudentPersonIdAndCourseCourseId(studentId, courseId);
        
        if (scores.isEmpty()) {
            log.warn("学生 {} 课程 {} 没有成绩记录", studentId, courseId);
            return 0.0;
        }
        
        // 获取最终成绩计算规则
        List<FinalScoreRule> rules = finalScoreRuleRepository.findAll();
        
        if (rules.isEmpty()) {
            log.warn("没有配置最终成绩计算规则，使用默认权重");
            // 使用默认权重：期末考试60%，期中考试30%，平时成绩10%
            return calculateWithDefaultWeights(scores);
        }
        
        // 按考试类型分组成绩
        Map<String, List<Score>> scoresByExamType = new HashMap<>();
        for (Score score : scores) {
            String examType = score.getExamType();
            if (examType == null || examType.trim().isEmpty()) {
                log.warn("发现空考试类型，学生ID: {}, 课程ID: {}, 成绩ID: {}", 
                        studentId, courseId, score.getScoreId());
                examType = "期末考试"; // 默认为期末考试
            }
            scoresByExamType.computeIfAbsent(examType, k -> new java.util.ArrayList<>()).add(score);
        }
        
        // 计算加权平均分
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        
        for (FinalScoreRule rule : rules) {
            // 检查权重是否有效
            if (rule.getWeight() == null || rule.getWeight() <= 0) {
                log.warn("发现无效权重，考试类型: {}, 权重: {}, 跳过该规则", 
                        rule.getExamType(), rule.getWeight());
                continue;
            }
            
            List<Score> examTypeScores = scoresByExamType.get(rule.getExamType());
            if (examTypeScores != null && !examTypeScores.isEmpty()) {
                // 检查成绩是否有效
                List<Integer> validScores = examTypeScores.stream()
                    .map(Score::getMark)
                    .filter(mark -> mark != null && mark >= 0 && mark <= 100)
                    .collect(Collectors.toList());
                
                if (validScores.isEmpty()) {
                    log.warn("考试类型 {} 没有有效成绩，学生ID: {}, 课程ID: {}", 
                            rule.getExamType(), studentId, courseId);
                    continue;
                }
                
                // 计算该考试类型的平均分
                double avgScore = validScores.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
                
                weightedSum += avgScore * rule.getWeight();
                totalWeight += rule.getWeight();
                
                log.debug("考试类型: {}, 平均分: {}, 权重: {}, 加权分数: {}", 
                        rule.getExamType(), avgScore, rule.getWeight(), avgScore * rule.getWeight());
            } else {
                log.debug("考试类型 {} 没有成绩记录，学生ID: {}, 课程ID: {}", 
                        rule.getExamType(), studentId, courseId);
            }
        }
        
        if (totalWeight == 0) {
            log.warn("没有有效的成绩类型或权重，学生ID: {}, 课程ID: {}, 返回0", studentId, courseId);
            return 0.0;
        }
        
        double finalScore = weightedSum / totalWeight;
        
        // 确保最终成绩在合理范围内
        if (finalScore < 0) {
            log.warn("计算出的最终成绩为负数，学生ID: {}, 课程ID: {}, 成绩: {}, 设置为0", 
                    studentId, courseId, finalScore);
            finalScore = 0.0;
        } else if (finalScore > 100) {
            log.warn("计算出的最终成绩超过100，学生ID: {}, 课程ID: {}, 成绩: {}, 设置为100", 
                    studentId, courseId, finalScore);
            finalScore = 100.0;
        }
        
        // 保存到成绩表
        scoreRepository.updateFinalScoreByStudentAndCourse(studentId, courseId, finalScore);
        
        log.info("学生 {} 课程 {} 的最终成绩计算完成: {}", studentId, courseId, finalScore);
        return finalScore;
    }
    
    /**
     * 使用默认权重计算最终成绩
     * @param scores 成绩列表
     * @return 最终成绩
     */
    private Double calculateWithDefaultWeights(List<Score> scores) {
        Map<String, List<Score>> scoresByExamType = new HashMap<>();
        for (Score score : scores) {
            scoresByExamType.computeIfAbsent(score.getExamType(), k -> new java.util.ArrayList<>()).add(score);
        }
        
        double finalScore = 0.0;
        
        // 期末考试 60%
        if (scoresByExamType.containsKey("期末考试")) {
            double avgScore = scoresByExamType.get("期末考试").stream()
                .mapToInt(Score::getMark)
                .average()
                .orElse(0.0);
            finalScore += avgScore * 0.6;
        }
        
        // 期中考试 30%
        if (scoresByExamType.containsKey("期中考试")) {
            double avgScore = scoresByExamType.get("期中考试").stream()
                .mapToInt(Score::getMark)
                .average()
                .orElse(0.0);
            finalScore += avgScore * 0.3;
        }
        
        // 平时成绩 10%
        if (scoresByExamType.containsKey("平时成绩")) {
            double avgScore = scoresByExamType.get("平时成绩").stream()
                .mapToInt(Score::getMark)
                .average()
                .orElse(0.0);
            finalScore += avgScore * 0.1;
        }
        
        return finalScore;
    }
    
    /**
     * 计算学生的总最终成绩（基于学分加权）
     * @param studentId 学生ID
     * @return 总最终成绩
     */
    @Transactional
    public Double calculateStudentFinalScore(Integer studentId) {
        // 参数校验
        if (studentId == null || studentId <= 0) {
            log.error("无效的学生ID: {}", studentId);
            return 0.0;
        }
        
        // 获取学生所有课程成绩
        List<Score> scores;
        try {
            scores = scoreRepository.findByStudentPersonId(studentId);
        } catch (Exception e) {
            log.error("获取学生 {} 的成绩记录失败", studentId, e);
            return 0.0;
        }
        
        if (scores.isEmpty()) {
            log.warn("学生 {} 没有成绩记录", studentId);
            return 0.0;
        }
        
        double totalWeightedScore = 0.0;
        int totalCredits = 0;
        
        // 按课程分组
        Map<Integer, List<Score>> scoresByCourse = new HashMap<>();
        for (Score score : scores) {
            if (score == null || score.getCourse() == null) {
                log.warn("发现无效成绩记录，学生ID: {}, 成绩ID: {}", 
                        studentId, score != null ? score.getScoreId() : "null");
                continue;
            }
            
            Integer courseId = score.getCourse().getCourseId();
            if (courseId == null || courseId <= 0) {
                log.warn("发现无效课程ID，学生ID: {}, 成绩ID: {}", 
                        studentId, score.getScoreId());
                continue;
            }
            
            scoresByCourse.computeIfAbsent(courseId, k -> new ArrayList<>()).add(score);
        }
        
        if (scoresByCourse.isEmpty()) {
            log.warn("学生 {} 没有有效的课程成绩记录", studentId);
            return 0.0;
        }
        
        // 计算每门课程的最终成绩并按学分加权
        for (Map.Entry<Integer, List<Score>> entry : scoresByCourse.entrySet()) {
            Integer courseId = entry.getKey();
            List<Score> courseScores = entry.getValue();
            
            try {
                // 计算该课程的最终成绩
                Double courseFinalScore = calculateCourseFinalScore(studentId, courseId);
                
                // 获取课程学分
                Integer credit = courseScores.get(0).getCourse().getCredit();
                if (credit == null || credit <= 0) {
                    log.warn("课程 {} 的学分为0或无效，学生ID: {}", courseId, studentId);
                    credit = 1; // 默认学分为1
                }
                
                totalWeightedScore += courseFinalScore * credit;
                totalCredits += credit;
                
                log.debug("课程成绩计算 - 学生ID: {}, 课程ID: {}, 最终成绩: {}, 学分: {}, 加权分数: {}", 
                        studentId, courseId, courseFinalScore, credit, courseFinalScore * credit);
            } catch (Exception e) {
                log.error("计算学生 {} 课程 {} 的最终成绩失败", studentId, courseId, e);
                // 继续处理其他课程
            }
        }
        
        if (totalCredits == 0) {
            log.warn("学生 {} 的课程总学分为0", studentId);
            return 0.0;
        }
        
        double finalScore = totalWeightedScore / totalCredits;
        
        // 确保最终成绩在合理范围内
        if (finalScore < 0) {
            log.warn("计算出的学生总最终成绩为负数，学生ID: {}, 成绩: {}, 设置为0", 
                    studentId, finalScore);
            finalScore = 0.0;
        } else if (finalScore > 100) {
            log.warn("计算出的学生总最终成绩超过100，学生ID: {}, 成绩: {}, 设置为100", 
                    studentId, finalScore);
            finalScore = 100.0;
        }
        
        // 保存到学生表
        try {
            scoreRepository.updateStudentFinalScore(studentId, finalScore);
        } catch (Exception e) {
            log.error("保存学生 {} 的最终成绩失败", studentId, e);
            // 不影响返回结果，只记录错误
        }
        
        log.info("学生 {} 的总最终成绩计算完成: {}", studentId, finalScore);
        return finalScore;
    }
    
    /**
     * 批量计算所有学生的最终成绩
     */
    @Transactional
    public void calculateAllStudentsFinalScore() {
        log.info("开始批量计算所有学生的最终成绩");
        
        // 获取所有学生
        List<Student> students = studentRepository.findAll();
        
        for (Student student : students) {
            try {
                calculateStudentFinalScore(student.getPersonId());
            } catch (Exception e) {
                log.error("计算学生 {} 的最终成绩时出错: {}", student.getPersonId(), e.getMessage());
            }
        }
        
        log.info("批量计算所有学生的最终成绩完成");
    }
    
    /**
     * 获取所有最终成绩计算规则
     * @return 规则列表
     */
    public List<FinalScoreRule> getAllFinalScoreRules() {
        return finalScoreRuleRepository.findAll();
    }
    
    /**
     * 根据ID获取最终成绩计算规则
     * @param ruleId 规则ID
     * @return 规则对象
     */
    public Optional<FinalScoreRule> getFinalScoreRuleById(Integer ruleId) {
        return finalScoreRuleRepository.findById(ruleId);
    }
    
    /**
     * 保存最终成绩计算规则
     * @param rule 规则对象
     * @return 保存后的规则对象
     */
    @Transactional
    public FinalScoreRule saveFinalScoreRule(FinalScoreRule rule) {
        // 检查考试类型是否已存在（除了当前规则）
        if (rule.getRuleId() != null) {
            Optional<FinalScoreRule> existingRule = finalScoreRuleRepository.findByExamType(rule.getExamType());
            if (existingRule.isPresent() && !existingRule.get().getRuleId().equals(rule.getRuleId())) {
                throw new RuntimeException("考试类型 " + rule.getExamType() + " 已存在");
            }
        } else {
            if (finalScoreRuleRepository.existsByExamType(rule.getExamType())) {
                throw new RuntimeException("考试类型 " + rule.getExamType() + " 已存在");
            }
        }
        
        return finalScoreRuleRepository.save(rule);
    }
    
    /**
     * 删除最终成绩计算规则
     * @param ruleId 规则ID
     */
    @Transactional
    public void deleteFinalScoreRule(Integer ruleId) {
        finalScoreRuleRepository.deleteById(ruleId);
    }
}