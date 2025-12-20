package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.ExamWeight;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.ExamWeightRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 考试权重服务类
 * 处理考试权重的增删改查和加权平均分计算
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamWeightService {
    
    private final ExamWeightRepository weightRepository;
    private final CourseRepository courseRepository;
    
    // 默认权重配置
    private static final Map<String, Double> DEFAULT_WEIGHTS = Map.of(
        "期末考试", 0.6,
        "期中考试", 0.3,
        "平时成绩", 0.1,
        "模拟考试", 0.2
    );
    
    /**
     * 获取权重列表
     */
    public DataResponse getWeightList(DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        
        List<ExamWeight> weightList;
        if (courseId != null && courseId > 0) {
            weightList = weightRepository.findByCourseCourseId(courseId);
        } else {
            weightList = weightRepository.findAll();
        }
        
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (ExamWeight weight : weightList) {
            Map<String, Object> m = new HashMap<>();
            m.put("weightId", weight.getWeightId());
            m.put("courseId", weight.getCourse().getCourseId());
            m.put("courseName", weight.getCourse().getName());
            m.put("examType", weight.getExamType());
            m.put("weight", weight.getWeight());
            m.put("description", weight.getDescription());
            m.put("createdAt", weight.getCreatedAt());
            m.put("updatedAt", weight.getUpdatedAt());
            dataList.add(m);
        }
        
        return CommonMethod.getReturnData(dataList);
    }
    
    /**
     * 保存权重配置
     */
    @Transactional
    public DataResponse weightSave(DataRequest dataRequest) {
        Integer weightId = dataRequest.getInteger("weightId");
        Integer courseId = dataRequest.getInteger("courseId");
        String examType = dataRequest.getString("examType");
        Double weight = dataRequest.getDouble("weight");
        String description = dataRequest.getString("description");
        
        // 参数验证
        if (courseId == null || courseId <= 0) {
            return CommonMethod.getReturnMessageError("课程ID不能为空");
        }
        
        if (examType == null || examType.trim().isEmpty()) {
            return CommonMethod.getReturnMessageError("考试类型不能为空");
        }
        
        if (weight == null || weight < 0 || weight > 1) {
            return CommonMethod.getReturnMessageError("权重必须在0-1之间");
        }
        
        // 验证考试类型
        if (!isValidExamType(examType)) {
            return CommonMethod.getReturnMessageError("考试类型必须是：期中考试、期末考试、平时成绩、模拟考试");
        }
        
        try {
            // 检查课程是否存在
            Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在: " + courseId));
            
            // 检查重复性（新增时）
            if (weightId == null || weightId == 0) {
                boolean exists = weightRepository.existsByCourseCourseIdAndExamType(courseId, examType);
                if (exists) {
                    return CommonMethod.getReturnMessageError("该课程的此考试类型已存在权重配置");
                }
            }
            
            // 获取或创建权重对象
            ExamWeight w = null;
            if (weightId != null && weightId > 0) {
                Optional<ExamWeight> op = weightRepository.findById(weightId);
                if (op.isPresent()) {
                    w = op.get();
                    // 检查是否修改了关键信息导致重复
                    if (!w.getCourse().getCourseId().equals(courseId) || 
                        !Objects.equals(w.getExamType(), examType)) {
                        
                        boolean exists = weightRepository.existsByCourseCourseIdAndExamType(courseId, examType);
                        if (exists) {
                            return CommonMethod.getReturnMessageError("修改后的权重配置已存在");
                        }
                    }
                }
            }
            
            if (w == null) {
                w = new ExamWeight();
                w.setCourse(course);
            }
            
            // 设置权重信息
            w.setExamType(examType);
            w.setWeight(weight);
            w.setDescription(description);
            
            // 保存权重
            weightRepository.save(w);
            log.info("权重保存成功 - 权重ID: {}, 课程ID: {}, 考试类型: {}, 权重: {}", 
                    w.getWeightId(), courseId, examType, weight);
            
            return CommonMethod.getReturnMessageOK();
            
        } catch (Exception e) {
            log.error("权重保存失败", e);
            return CommonMethod.getReturnMessageError("保存失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除权重配置
     */
    @Transactional
    public DataResponse weightDelete(DataRequest dataRequest) {
        Integer weightId = dataRequest.getInteger("weightId");
        if (weightId == null || weightId == 0) {
            return CommonMethod.getReturnMessageError("权重ID不能为空");
        }
        
        try {
            Optional<ExamWeight> op = weightRepository.findById(weightId);
            if (op.isPresent()) {
                ExamWeight w = op.get();
                weightRepository.delete(w);
                log.info("权重删除成功 - 权重ID: {}, 课程ID: {}, 考试类型: {}", 
                        weightId, w.getCourse().getCourseId(), w.getExamType());
                return CommonMethod.getReturnMessageOK();
            } else {
                log.warn("权重配置不存在 - 权重ID: {}", weightId);
                return CommonMethod.getReturnMessageError("权重配置不存在");
            }
        } catch (Exception e) {
            log.error("权重删除失败 - 权重ID: {}", weightId, e);
            return CommonMethod.getReturnMessageError("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定课程的权重配置
     */
    public Map<String, Double> getCourseWeights(Integer courseId) {
        Map<String, Double> weights = new HashMap<>();
        
        // 获取课程特定的权重配置
        List<ExamWeight> courseWeights = weightRepository.findByCourseCourseId(courseId);
        for (ExamWeight w : courseWeights) {
            weights.put(w.getExamType(), w.getWeight());
        }
        
        // 如果没有特定配置，使用默认权重
        for (Map.Entry<String, Double> entry : DEFAULT_WEIGHTS.entrySet()) {
            weights.putIfAbsent(entry.getKey(), entry.getValue());
        }
        
        return weights;
    }
    
    /**
     * 计算加权平均分
     */
    public Double calculateWeightedAverage(List<Map<String, Object>> scores, Integer courseId) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        
        Map<String, Double> weights = getCourseWeights(courseId);
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        
        for (Map<String, Object> score : scores) {
            String examType = (String) score.get("examType");
            Integer mark = (Integer) score.get("mark");
            
            if (examType != null && mark != null && weights.containsKey(examType)) {
                double weight = weights.get(examType);
                totalWeightedScore += mark * weight;
                totalWeight += weight;
            }
        }
        
        // 避免除以0
        return totalWeight > 0 ? totalWeightedScore / totalWeight : 0.0;
    }
    
    /**
     * 验证考试类型是否有效
     */
    private boolean isValidExamType(String examType) {
        return DEFAULT_WEIGHTS.containsKey(examType);
    }
    
    /**
     * 获取指定课程的权重配置（用于API响应）
     */
    public DataResponse getCourseWeightsResponse(Integer courseId) {
        Map<String, Double> weights = getCourseWeights(courseId);
        return CommonMethod.getReturnData(weights);
    }
    
    /**
     * 初始化默认权重配置
     */
    @Transactional
    public void initializeDefaultWeights() {
        log.info("开始初始化默认权重配置");
        
        // 检查是否已有默认权重配置
        List<ExamWeight> existingDefaults = weightRepository.findByCourseCourseId(0);
        if (!existingDefaults.isEmpty()) {
            log.info("默认权重配置已存在，跳过初始化");
            return;
        }
        
        // 创建一个虚拟课程用于存储默认权重
        Course defaultCourse = new Course();
        defaultCourse.setCourseId(0);
        defaultCourse.setName("默认配置");
        
        // 创建默认权重配置
        for (Map.Entry<String, Double> entry : DEFAULT_WEIGHTS.entrySet()) {
            ExamWeight weight = new ExamWeight();
            weight.setCourse(defaultCourse);
            weight.setExamType(entry.getKey());
            weight.setWeight(entry.getValue());
            weight.setDescription("系统默认权重配置");
            weightRepository.save(weight);
        }
        
        log.info("默认权重配置初始化完成");
    }
}