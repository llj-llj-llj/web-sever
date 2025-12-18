package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.PdfGenerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/teaching")
public class TeachingController {
    
    private final PdfGenerationService pdfGenerationService;
    
    public TeachingController(PdfGenerationService pdfGenerationService) {
        this.pdfGenerationService = pdfGenerationService;
    }
    
    @PostMapping("/generateScoreReport")
    public DataResponse generateScoreReport(@Valid @RequestBody DataRequest dataRequest) {
        // 从请求中提取参数
        Integer personId = dataRequest.getInteger("personId");
        Integer courseId = dataRequest.getInteger("courseId");
        String examType = dataRequest.getString("examType");
        
        // 如果提供了学生ID，生成学生个人成绩单
        if (personId != null && personId > 0) {
            return pdfGenerationService.generateStudentScorePdf(personId);
        }
        
        // 如果提供了课程ID和考试类型，生成课程成绩单
        if (courseId != null && courseId > 0 && examType != null && !examType.trim().isEmpty()) {
            return pdfGenerationService.generateCourseScorePdf(courseId, examType);
        }
        
        // 如果都没有提供，返回错误
        return cn.edu.sdu.java.server.util.CommonMethod.getReturnMessageError("请提供学生ID或课程ID和考试类型");
    }
}