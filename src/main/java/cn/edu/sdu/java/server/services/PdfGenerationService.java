package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PDF生成服务
 * 负责生成成绩单、成绩统计等PDF文档
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {
    
    private final ScoreRepository scoreRepository;
    private final CourseRepository courseRepository;
    
    /**
     * 生成课程成绩单PDF
     * @param courseId 课程ID
     * @param examType 考试类型
     * @return PDF文件的字节数组
     */
    public DataResponse generateCourseScorePdf(Integer courseId, String examType) {
        try {
            // 验证参数
            if (courseId == null || courseId <= 0) {
                return CommonMethod.getReturnMessageError("课程ID不能为空");
            }
            
            if (examType == null || examType.trim().isEmpty()) {
                return CommonMethod.getReturnMessageError("考试类型不能为空");
            }
            
            // 查询课程信息
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return CommonMethod.getReturnMessageError("课程不存在");
            }
            
            // 查询成绩数据
            List<Score> scores = scoreRepository.findByCourseCourseIdAndExamTypeOrderByMarkDesc(courseId, examType);
            if (scores.isEmpty()) {
                return CommonMethod.getReturnMessageError("没有找到相关成绩数据");
            }
            
            // 生成PDF
            byte[] pdfBytes = generateCourseScorePdfContent(course, examType, scores);
            
            Map<String, Object> result = Map.of(
                "fileName", getCourseScoreFileName(course, examType),
                "fileSize", pdfBytes.length,
                "data", pdfBytes
            );
            
            log.info("课程成绩单PDF生成成功 - 课程: {}, 考试类型: {}, 成绩数量: {}", 
                    course.getName(), examType, scores.size());
            
            return CommonMethod.getReturnData(result, "PDF生成成功");
            
        } catch (Exception e) {
            log.error("生成课程成绩单PDF失败", e);
            return CommonMethod.getReturnMessageError("PDF生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成学生个人成绩单PDF
     * @param personId 学生ID
     * @return PDF文件的字节数组
     */
    public DataResponse generateStudentScorePdf(Integer personId) {
        try {
            // 验证参数
            if (personId == null || personId <= 0) {
                return CommonMethod.getReturnMessageError("学生ID不能为空");
            }
            
            // 查询学生成绩数据
            List<Score> scores = scoreRepository.findByStudentPersonPersonIdOrderByExamTypeAscCourseNameAsc(personId);
            if (scores.isEmpty()) {
                return CommonMethod.getReturnMessageError("没有找到该学生的成绩数据");
            }
            
            // 生成PDF
            byte[] pdfBytes = generateStudentScorePdfContent(scores);
            
            String studentName = scores.get(0).getStudent().getPerson().getName();
            Map<String, Object> result = Map.of(
                "fileName", getStudentScoreFileName(studentName),
                "fileSize", pdfBytes.length,
                "data", pdfBytes
            );
            
            log.info("学生个人成绩单PDF生成成功 - 学生: {}, 成绩数量: {}", studentName, scores.size());
            
            return CommonMethod.getReturnData(result, "PDF生成成功");
            
        } catch (Exception e) {
            log.error("生成学生个人成绩单PDF失败", e);
            return CommonMethod.getReturnMessageError("PDF生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成课程成绩单PDF内容
     */
    private byte[] generateCourseScorePdfContent(Course course, String examType, List<Score> scores) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // 设置中文字体
        BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Font headerFont = new Font(baseFont, 12, Font.BOLD);
        Font contentFont = new Font(baseFont, 10);
        Font smallFont = new Font(baseFont, 8);
        
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, outputStream);
        document.open();
        
        // 标题
        Paragraph title = new Paragraph(course.getName() + " - " + examType + " 成绩单", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // 课程信息
        Paragraph courseInfo = new Paragraph("课程编号: " + course.getNum() + 
                                           "  |  课程名称: " + course.getName() + 
                                           "  |  学分: " + course.getCredit(), contentFont);
        courseInfo.setSpacingAfter(10);
        document.add(courseInfo);
        
        // 统计信息
        addStatisticsInfo(document, scores, contentFont);
        
        // 成绩表格
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20);
        
        // 表头
        String[] headers = {"排名", "学号", "姓名", "班级", "分数", "考试类型"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
        
        // 数据行
        DecimalFormat df = new DecimalFormat("#0.0");
        for (Score score : scores) {
            table.addCell(new PdfPCell(new Phrase(String.valueOf(score.getRanking()), contentFont)));
            table.addCell(new PdfPCell(new Phrase(score.getStudent().getPerson().getNum(), contentFont)));
            table.addCell(new PdfPCell(new Phrase(score.getStudent().getPerson().getName(), contentFont)));
            table.addCell(new PdfPCell(new Phrase(score.getStudent().getClassName(), contentFont)));
            table.addCell(new PdfPCell(new Phrase(df.format(score.getMark()), contentFont)));
            table.addCell(new PdfPCell(new Phrase(score.getExamType(), contentFont)));
        }
        
        document.add(table);
        
        // 页脚信息
        Paragraph footer = new Paragraph("生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), smallFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setSpacingBefore(30);
        document.add(footer);
        
        document.close();
        return outputStream.toByteArray();
    }
    
    /**
     * 生成学生个人成绩单PDF内容
     */
    private byte[] generateStudentScorePdfContent(List<Score> scores) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // 设置中文字体
        BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Font headerFont = new Font(baseFont, 12, Font.BOLD);
        Font contentFont = new Font(baseFont, 10);
        Font smallFont = new Font(baseFont, 8);
        
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, outputStream);
        document.open();
        
        // 标题
        String studentName = scores.get(0).getStudent().getPerson().getName();
        String studentNum = scores.get(0).getStudent().getPerson().getNum();
        Paragraph title = new Paragraph(studentName + " 个人成绩单", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // 学生信息
        Paragraph studentInfo = new Paragraph("学号: " + studentNum + 
                                            "  |  姓名: " + studentName + 
                                            "  |  班级: " + scores.get(0).getStudent().getClassName(), contentFont);
        studentInfo.setSpacingAfter(10);
        document.add(studentInfo);
        
        // 统计信息
        addStudentStatisticsInfo(document, scores, contentFont);
        
        // 成绩表格
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20);
        
        // 表头
        String[] headers = {"课程编号", "课程名称", "学分", "考试类型", "分数", "排名"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
        
        // 数据行
        DecimalFormat df = new DecimalFormat("#0.0");
        for (Score score : scores) {
            table.addCell(new PdfPCell(new Phrase(score.getCourse().getNum(), contentFont)));
            table.addCell(new PdfPCell(new Phrase(score.getCourse().getName(), contentFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(score.getCourse().getCredit()), contentFont)));
            table.addCell(new PdfPCell(new Phrase(score.getExamType(), contentFont)));
            table.addCell(new PdfPCell(new Phrase(df.format(score.getMark()), contentFont)));
            table.addCell(new PdfPCell(new Phrase(score.getRanking() != null ? String.valueOf(score.getRanking()) : "-", contentFont)));
        }
        
        document.add(table);
        
        // 页脚信息
        Paragraph footer = new Paragraph("生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), smallFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setSpacingBefore(30);
        document.add(footer);
        
        document.close();
        return outputStream.toByteArray();
    }
    
    /**
     * 添加统计信息到文档
     */
    private void addStatisticsInfo(Document document, List<Score> scores, Font font) throws DocumentException {
        if (scores.isEmpty()) return;
        
        double average = scores.stream().mapToDouble(Score::getMark).average().orElse(0);
        double maxScore = scores.stream().mapToDouble(Score::getMark).max().orElse(0);
        double minScore = scores.stream().mapToDouble(Score::getMark).min().orElse(0);
        long passCount = scores.stream().filter(s -> s.getMark() >= 60).count();
        double passRate = (double) passCount / scores.size() * 100;
        
        DecimalFormat df = new DecimalFormat("#0.0");
        
        Paragraph stats = new Paragraph("统计信息: " +
                                      "平均分: " + df.format(average) + 
                                      "  |  最高分: " + df.format(maxScore) + 
                                      "  |  最低分: " + df.format(minScore) + 
                                      "  |  及格率: " + df.format(passRate) + "%", font);
        stats.setSpacingAfter(10);
        document.add(stats);
    }
    
    /**
     * 添加学生统计信息到文档
     */
    private void addStudentStatisticsInfo(Document document, List<Score> scores, Font font) throws DocumentException {
        if (scores.isEmpty()) return;
        
        double average = scores.stream().mapToDouble(Score::getMark).average().orElse(0);
        double totalCredit = scores.stream().mapToDouble(s -> s.getCourse().getCredit()).sum();
        double weightedAverage = scores.stream()
                .mapToDouble(s -> s.getMark() * s.getCourse().getCredit())
                .sum() / totalCredit;
        long passCount = scores.stream().filter(s -> s.getMark() >= 60).count();
        double passRate = (double) passCount / scores.size() * 100;
        
        DecimalFormat df = new DecimalFormat("#0.0");
        
        Paragraph stats = new Paragraph("统计信息: " +
                                      "平均分: " + df.format(average) + 
                                      "  |  加权平均分: " + df.format(weightedAverage) + 
                                      "  |  总学分: " + df.format(totalCredit) + 
                                      "  |  及格率: " + df.format(passRate) + "%", font);
        stats.setSpacingAfter(10);
        document.add(stats);
    }
    
    /**
     * 获取课程成绩单文件名
     */
    private String getCourseScoreFileName(Course course, String examType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return course.getName() + "_" + examType + "_成绩单_" + timestamp + ".pdf";
    }
    
    /**
     * 获取学生成绩单文件名
     */
    private String getStudentScoreFileName(String studentName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return studentName + "_个人成绩单_" + timestamp + ".pdf";
    }
}