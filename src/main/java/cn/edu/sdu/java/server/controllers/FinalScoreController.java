package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.models.FinalScoreRule;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.FinalScoreCalculationService;
import cn.edu.sdu.java.server.util.CommonMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 最终成绩控制器
 */
@RestController
@RequestMapping("/api/final-score")
@RequiredArgsConstructor
@Slf4j
public class FinalScoreController {

    private final FinalScoreCalculationService finalScoreCalculationService;

    /**
     * 获取所有最终成绩计算规则
     */
    @GetMapping("/rules")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<DataResponse> getAllFinalScoreRules() {
        List<FinalScoreRule> rules = finalScoreCalculationService.getAllFinalScoreRules();
        return ResponseEntity.ok(CommonMethod.getReturnData(rules));
    }

    /**
     * 根据ID获取最终成绩计算规则
     */
    @GetMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<DataResponse> getFinalScoreRuleById(@PathVariable Integer ruleId) {
        Optional<FinalScoreRule> rule = finalScoreCalculationService.getFinalScoreRuleById(ruleId);
        if (rule.isPresent()) {
            return ResponseEntity.ok(CommonMethod.getReturnData(rule.get()));
        } else {
            return ResponseEntity.ok(CommonMethod.getReturnMessageError("规则不存在"));
        }
    }

    /**
     * 保存最终成绩计算规则
     */
    @PostMapping("/rules/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataResponse> saveFinalScoreRule(@RequestBody DataRequest dataRequest) {
        try {
            Map<String, Object> ruleData = dataRequest.getMap("rule");
            FinalScoreRule rule = new FinalScoreRule();
            
            if (ruleData.containsKey("ruleId") && ruleData.get("ruleId") != null) {
                rule.setRuleId((Integer) ruleData.get("ruleId"));
            }
            
            rule.setExamType((String) ruleData.get("examType"));
            rule.setWeight(((Number) ruleData.get("weight")).doubleValue());
            rule.setDescription((String) ruleData.get("description"));
            rule.setIsDefault((Boolean) ruleData.getOrDefault("isDefault", false));
            
            FinalScoreRule savedRule = finalScoreCalculationService.saveFinalScoreRule(rule);
            return ResponseEntity.ok(CommonMethod.getReturnData(savedRule));
        } catch (Exception e) {
            log.error("保存最终成绩规则失败", e);
            return ResponseEntity.ok(CommonMethod.getReturnMessageError("保存失败: " + e.getMessage()));
        }
    }

    /**
     * 删除最终成绩计算规则
     */
    @PostMapping("/rules/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataResponse> deleteFinalScoreRule(@RequestBody DataRequest dataRequest) {
        try {
            Integer ruleId = dataRequest.getInteger("ruleId");
            finalScoreCalculationService.deleteFinalScoreRule(ruleId);
            return ResponseEntity.ok(CommonMethod.getReturnMessageOK("删除成功"));
        } catch (Exception e) {
            log.error("删除最终成绩规则失败", e);
            return ResponseEntity.ok(CommonMethod.getReturnMessageError("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 计算学生单门课程的最终成绩
     */
    @PostMapping("/calculate/course")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<DataResponse> calculateCourseFinalScore(@RequestBody DataRequest dataRequest) {
        try {
            Integer studentId = dataRequest.getInteger("studentId");
            Integer courseId = dataRequest.getInteger("courseId");
            
            Double finalScore = finalScoreCalculationService.calculateCourseFinalScore(studentId, courseId);
            
            Map<String, Object> result = Map.of(
                "studentId", studentId,
                "courseId", courseId,
                "finalScore", finalScore
            );
            
            return ResponseEntity.ok(CommonMethod.getReturnData(result));
        } catch (Exception e) {
            log.error("计算课程最终成绩失败", e);
            return ResponseEntity.ok(CommonMethod.getReturnMessageError("计算失败: " + e.getMessage()));
        }
    }

    /**
     * 计算学生的总最终成绩
     */
    @PostMapping("/calculate/student")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<DataResponse> calculateStudentFinalScore(@RequestBody DataRequest dataRequest) {
        try {
            Integer studentId = dataRequest.getInteger("studentId");
            
            Double finalScore = finalScoreCalculationService.calculateStudentFinalScore(studentId);
            
            Map<String, Object> result = Map.of(
                "studentId", studentId,
                "finalScore", finalScore
            );
            
            return ResponseEntity.ok(CommonMethod.getReturnData(result));
        } catch (Exception e) {
            log.error("计算学生最终成绩失败", e);
            return ResponseEntity.ok(CommonMethod.getReturnMessageError("计算失败: " + e.getMessage()));
        }
    }

    /**
     * 批量计算所有学生的最终成绩
     */
    @PostMapping("/calculate/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataResponse> calculateAllStudentsFinalScore() {
        try {
            finalScoreCalculationService.calculateAllStudentsFinalScore();
            return ResponseEntity.ok(CommonMethod.getReturnMessageOK("批量计算完成"));
        } catch (Exception e) {
            log.error("批量计算最终成绩失败", e);
            return ResponseEntity.ok(CommonMethod.getReturnMessageError("批量计算失败: " + e.getMessage()));
        }
    }
}