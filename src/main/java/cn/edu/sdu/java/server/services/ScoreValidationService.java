package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 成绩验证服务
 * 负责成绩相关的业务逻辑校验
 */
@Service
public class ScoreValidationService {
    
    /**
     * 验证成绩保存请求
     */
    public ValidationResult validateScoreSave(DataRequest dataRequest) {
        ValidationResult result = new ValidationResult();
        
        // 参数完整性检查
        if (dataRequest.getInteger("personId") == null) {
            result.addError("personId", "学生ID不能为空");
        }
        if (dataRequest.getInteger("courseId") == null) {
            result.addError("courseId", "课程ID不能为空");
        }
        if (dataRequest.getInteger("mark") == null) {
            result.addError("mark", "分数不能为空");
        }
        if (!StringUtils.hasText(dataRequest.getString("examType"))) {
            result.addError("examType", "考试类型不能为空");
        }
        
        // 分数范围检查
        Integer mark = dataRequest.getInteger("mark");
        if (mark != null && (mark < 0 || mark > 100)) {
            result.addError("mark", "分数必须在0-100之间");
        }
        
        // 考试类型检查
        String examType = dataRequest.getString("examType");
        if (StringUtils.hasText(examType) && !isValidExamType(examType)) {
            result.addError("examType", "考试类型必须是：期中考试、期末考试、平时成绩、模拟考试");
        }
        
        return result;
    }
    
    /**
     * 验证考试类型是否有效
     */
    private boolean isValidExamType(String examType) {
        return Arrays.asList("期中考试", "期末考试", "平时成绩", "模拟考试").contains(examType);
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private Map<String, String> errors = new HashMap<>();
        
        public void addError(String field, String message) {
            errors.put(field, message);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public Map<String, String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return "";
            }
            return String.join(", ", errors.values());
        }
    }
}