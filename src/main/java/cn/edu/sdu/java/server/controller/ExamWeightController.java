package cn.edu.sdu.java.server.controller;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.ExamWeightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 考试权重控制器
 * 处理考试权重相关的HTTP请求
 */
@RestController
@RequestMapping("/api/examWeight")
public class ExamWeightController {
    
    @Autowired
    private ExamWeightService weightService;
    
    /**
     * 获取权重列表
     */
    @PostMapping("/getWeightList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getWeightList(@RequestBody DataRequest dataRequest) {
        return weightService.getWeightList(dataRequest);
    }
    
    /**
     * 保存权重配置
     */
    @PostMapping("/weightSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse weightSave(@RequestBody DataRequest dataRequest) {
        return weightService.weightSave(dataRequest);
    }
    
    /**
     * 删除权重配置
     */
    @PostMapping("/weightDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse weightDelete(@RequestBody DataRequest dataRequest) {
        return weightService.weightDelete(dataRequest);
    }
    
    /**
     * 获取指定课程的权重配置
     */
    @GetMapping("/getCourseWeights/{courseId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getCourseWeights(@PathVariable Integer courseId) {
        return weightService.getCourseWeightsResponse(courseId);
    }
}