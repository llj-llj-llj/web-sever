package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.services.ScoreService;
import cn.edu.sdu.java.server.services.ExcelImportService;
import cn.edu.sdu.java.server.services.PdfGenerationService;
import cn.edu.sdu.java.server.services.PermissionService;
import cn.edu.sdu.java.server.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/score")
public class ScoreController {
    private final ScoreService scoreService;
    private final ExcelImportService excelImportService;
    private final PdfGenerationService pdfGenerationService;
    private final PermissionService permissionService;
    
    public ScoreController(ScoreService scoreService, ExcelImportService excelImportService, 
                          PdfGenerationService pdfGenerationService, PermissionService permissionService) {
        this.scoreService = scoreService;
        this.excelImportService = excelImportService;
        this.pdfGenerationService = pdfGenerationService;
        this.permissionService = permissionService;
    }
    @PostMapping("/getStudentItemOptionList")
    public OptionItemList getStudentItemOptionList(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.getStudentItemOptionList(dataRequest);
    }

    @PostMapping("/getCourseItemOptionList")
    public OptionItemList getCourseItemOptionList(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.getCourseItemOptionList(dataRequest);
    }

    @PostMapping("/getScoreList")
    public DataResponse getScoreList(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.getScoreList(dataRequest);
    }

    @PostMapping("/getScoreListPaged")
    public DataResponse getScoreListPaged(@Valid @RequestBody DataRequest dataRequest) {
        return scoreService.getScoreListPaged(dataRequest);
    }
    @PostMapping("/scoreSave")
    public DataResponse scoreSave(@Valid @RequestBody DataRequest dataRequest) {
        // 权限检查
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return cn.edu.sdu.java.server.util.CommonMethod.getReturnMessageError("用户未登录");
        }
        
        if (!permissionService.canImportScores(currentUserId)) {
            return cn.edu.sdu.java.server.util.CommonMethod.getReturnMessageError("没有权限保存成绩");
        }
        
        return scoreService.scoreSave(dataRequest);
    }
    @PostMapping("/scoreDelete")
    public DataResponse scoreDelete(@Valid @RequestBody DataRequest dataRequest) {
        // 权限检查
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return cn.edu.sdu.java.server.util.CommonMethod.getReturnMessageError("用户未登录");
        }
        
        if (!permissionService.canImportScores(currentUserId)) {
            return cn.edu.sdu.java.server.util.CommonMethod.getReturnMessageError("没有权限删除成绩");
        }
        
        return scoreService.scoreDelete(dataRequest);
    }

    @PostMapping("/importScoreExcel")
    public DataResponse importScoreExcel(@RequestParam("file") MultipartFile file) {
        // 权限检查
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return cn.edu.sdu.java.server.util.CommonMethod.getReturnMessageError("用户未登录");
        }
        
        if (!permissionService.canImportScores(currentUserId)) {
            return cn.edu.sdu.java.server.util.CommonMethod.getReturnMessageError("没有权限导入成绩");
        }
        
        return excelImportService.importScoreFromExcel(file);
    }

    @PostMapping("/generateCourseScorePdf")
    public DataResponse generateCourseScorePdf(@RequestParam("courseId") Integer courseId, 
                                             @RequestParam("examType") String examType) {
        return pdfGenerationService.generateCourseScorePdf(courseId, examType);
    }

    @PostMapping("/generateStudentScorePdf")
    public DataResponse generateStudentScorePdf(@RequestParam("personId") Integer personId) {
        return pdfGenerationService.generateStudentScorePdf(personId);
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

    @PostMapping("/getStudentFinalScoreAnalysis")
    public DataResponse getStudentFinalScoreAnalysis(@Valid @RequestBody DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        
        // 如果没有提供学生ID，使用当前登录用户ID
        if (personId == null || personId == 0) {
            personId = SecurityUtil.getCurrentUserId();
            if (personId == null) {
                return cn.edu.sdu.java.server.util.CommonMethod.getReturnMessageError("用户未登录");
            }
        }
        
        return scoreService.getStudentFinalScoreAnalysis(personId);
    }

    @PostMapping("/getClassFinalScoreAnalysis")
    public DataResponse getClassFinalScoreAnalysis(@Valid @RequestBody DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        String examType = dataRequest.getString("examType");
        
        return scoreService.getClassFinalScoreAnalysis(courseId, examType);
    }
}
