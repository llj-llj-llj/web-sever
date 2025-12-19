package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Excel导入服务
 * 负责成绩数据的Excel文件导入处理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {
    
    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final ScoreValidationService validationService;
    private final RankingCalculationService rankingService;
    
    /**
     * 导入Excel文件中的成绩数据
     * @param file Excel文件
     * @return 导入结果
     */
    @Transactional
    public DataResponse importScoreFromExcel(MultipartFile file) {
        if (file.isEmpty()) {
            return CommonMethod.getReturnMessageError("文件不能为空");
        }
        
        if (!isExcelFile(file)) {
            return CommonMethod.getReturnMessageError("请上传Excel文件(.xlsx或.xls)");
        }
        
        // 设置默认考试类型为期末考试
        String examType = "期末考试";
        
        try {
            List<ScoreImportResult> results = processExcelFile(file, examType);
            
            // 统计结果
            long successCount = results.stream().filter(r -> r.isSuccess()).count();
            long errorCount = results.size() - successCount;
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", results.size());
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            result.put("results", results);
            
            String message = String.format("导入完成！总计：%d，成功：%d，失败：%d", 
                    results.size(), successCount, errorCount);
            
            log.info("Excel导入完成 - {}", message);
            return CommonMethod.getReturnData(result, message);
            
        } catch (Exception e) {
            log.error("Excel导入失败", e);
            return CommonMethod.getReturnMessageError("导入失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理Excel文件
     */
    private List<ScoreImportResult> processExcelFile(MultipartFile file, String examType) 
            throws IOException {
        
        List<ScoreImportResult> results = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表
            
            // 检查表头
            if (!validateHeaders(sheet)) {
                throw new RuntimeException("Excel表头格式不正确，应为：学号、姓名、课程号、课程名、分数");
            }
            
            // 处理数据行（从第2行开始，跳过表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                ScoreImportResult result = processRow(row, examType, i + 1);
                results.add(result);
            }
            
            // 重新计算所有受影响课程的排名
            recalculateRankings(results, examType);
        }
        
        return results;
    }
    
    /**
     * 验证Excel表头
     */
    private boolean validateHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return false;
        
        String[] expectedHeaders = {"学号", "姓名", "课程号", "课程名", "分数"};
        
        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !cell.getStringCellValue().trim().equals(expectedHeaders[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 处理单行数据
     */
    private ScoreImportResult processRow(Row row, String examType, int rowNum) {
        ScoreImportResult result = new ScoreImportResult(rowNum);
        
        try {
            // 读取数据
            String studentNum = getCellStringValue(row.getCell(0));
            String studentName = getCellStringValue(row.getCell(1));
            String courseNum = getCellStringValue(row.getCell(2));
            String courseName = getCellStringValue(row.getCell(3));
            Integer mark = getCellIntegerValue(row.getCell(4));
            
            // 基本验证
            if (studentNum.isEmpty() || studentName.isEmpty() || 
                courseNum.isEmpty() || courseName.isEmpty() || mark == null) {
                result.setError("数据不完整");
                return result;
            }
            
            // 查找学生
            Student student = studentRepository.findByPersonNum(studentNum)
                .filter(s -> s.getPerson().getName().equals(studentName))
                .orElse(null);
            
            if (student == null) {
                result.setError("学生不存在");
                return result;
            }
            
            // 查找课程
            Course course = courseRepository.findByNum(courseNum)
                .filter(c -> c.getName().equals(courseName))
                .orElse(null);
            
            if (course == null) {
                result.setError("课程不存在");
                return result;
            }
            
            // 检查重复
            if (scoreRepository.existsByStudentPersonIdAndCourseCourseIdAndExamType(
                    student.getPersonId(), course.getCourseId(), examType)) {
                result.setError("成绩已存在");
                return result;
            }
            
            // 创建成绩记录
            Score score = new Score();
            score.setStudent(student);
            score.setCourse(course);
            score.setMark(mark);
            score.setExamType(examType);
            
            scoreRepository.save(score);
            
            result.setSuccess(true);
            result.setMessage("导入成功");
            result.setScoreId(score.getScoreId());
            result.setCourseId(course.getCourseId());
            
            log.debug("行 {} 导入成功 - 学生: {}, 课程: {}, 分数: {}", 
                     rowNum, studentNum, courseNum, mark);
            
        } catch (Exception e) {
            result.setError("处理异常: " + e.getMessage());
            log.warn("行 {} 导入失败", rowNum, e);
        }
        
        return result;
    }
    
    /**
     * 重新计算排名
     */
    private void recalculateRankings(List<ScoreImportResult> results, String examType) {
        Set<Integer> courseIds = new HashSet<>();
        
        for (ScoreImportResult result : results) {
            if (result.isSuccess() && result.getCourseId() != null) {
                courseIds.add(result.getCourseId());
            }
        }
        
        for (Integer courseId : courseIds) {
            try {
                rankingService.calculateRanking(courseId, examType);
            } catch (Exception e) {
                log.error("重新计算排名失败 - 课程ID: {}, 考试类型: {}", courseId, examType, e);
            }
        }
    }
    
    /**
     * 检查是否为Excel文件
     */
    private boolean isExcelFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"));
    }
    
    /**
     * 验证考试类型
     */
    private boolean isValidExamType(String examType) {
        return Arrays.asList("期中考试", "期末考试", "平时成绩", "模拟考试").contains(examType);
    }
    
    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
    
    /**
     * 获取单元格整数值
     */
    private Integer getCellIntegerValue(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    return value.isEmpty() ? null : Integer.parseInt(value);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 导入结果类
     */
    public static class ScoreImportResult {
        private int rowNumber;
        private boolean success;
        private String message;
        private String error;
        private Integer scoreId;
        private Integer courseId;
        
        public ScoreImportResult(int rowNumber) {
            this.rowNumber = rowNumber;
            this.success = false;
        }
        
        // Getters and Setters
        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public Integer getScoreId() { return scoreId; }
        public void setScoreId(Integer scoreId) { this.scoreId = scoreId; }
        
        public Integer getCourseId() { return courseId; }
        public void setCourseId(Integer courseId) { this.courseId = courseId; }
    }
}