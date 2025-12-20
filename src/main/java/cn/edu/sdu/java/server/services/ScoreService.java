package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreService {
    private final CourseRepository courseRepository;
    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;
    private final ScoreValidationService validationService;
    private final RankingCalculationService rankingService;
    private final ExamWeightService examWeightService;
    private final FinalScoreCalculationService finalScoreCalculationService;
    public OptionItemList getStudentItemOptionList( DataRequest dataRequest) {
        List<Student> sList = studentRepository.findStudentListByNumName("");  //数据库查询操作
        List<OptionItem> itemList = new ArrayList<>();
        for (Student s : sList) {
            itemList.add(new OptionItem( s.getPersonId(),s.getPersonId()+"", s.getPerson().getNum()+"-"+s.getPerson().getName()));
        }
        return new OptionItemList(0, itemList);
    }

    public OptionItemList getCourseItemOptionList(DataRequest dataRequest) {
        List<Course> sList = courseRepository.findAll();  //数据库查询操作
        List<OptionItem> itemList = new ArrayList<>();
        for (Course c : sList) {
            itemList.add(new OptionItem(c.getCourseId(),c.getCourseId()+"", c.getNum()+"-"+c.getName()));
        }
        return new OptionItemList(0, itemList);
    }

    public DataResponse getScoreList(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        if(personId == null)
            personId = 0;
        Integer courseId = dataRequest.getInteger("courseId");
        if(courseId == null)
            courseId = 0;
        String examType = dataRequest.getString("examType");
        
        List<Score> sList = scoreRepository.findByStudentCourseAndExamType(personId, courseId, examType);  //数据库查询操作
        List<Map<String,Object>> dataList = new ArrayList<>();
        Map<String,Object> m;
        for (Score s : sList) {
            m = new HashMap<>();
            m.put("scoreId", s.getScoreId());
            m.put("personId", s.getStudent().getPersonId());
            m.put("courseId", s.getCourse().getCourseId());
            m.put("studentNum",s.getStudent().getPerson().getNum());
            m.put("studentName",s.getStudent().getPerson().getName());
            m.put("className",s.getStudent().getClassName());
            m.put("courseNum",s.getCourse().getNum());
            m.put("courseName",s.getCourse().getName());
            m.put("credit", s.getCourse().getCredit());
            m.put("mark", s.getMark());
            m.put("examType", s.getExamType() != null ? s.getExamType() : "");
            m.put("ranking", s.getRanking() != null ? s.getRanking() : 0);
            dataList.add(m);
        }
        return CommonMethod.getReturnData(dataList);
    }

    /**
     * 分页查询成绩列表
     * @param dataRequest 包含分页参数和查询条件
     * @return 分页成绩数据
     */
    public DataResponse getScoreListPaged(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        Integer courseId = dataRequest.getInteger("courseId");
        String examType = dataRequest.getString("examType");
        Integer page = dataRequest.getInteger("page");
        Integer size = dataRequest.getInteger("size");
        String sortField = dataRequest.getString("sortField");
        String sortOrder = dataRequest.getString("sortOrder");
        
        // 设置默认值
        if (personId == null) personId = 0;
        if (courseId == null) courseId = 0;
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0) size = 10;
        if (sortField == null) sortField = "scoreId";
        if (sortOrder == null) sortOrder = "desc";
        
        try {
            // 创建排序对象
            Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sort = Sort.by(direction, sortField);
            
            // 创建分页对象
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // 执行分页查询
            Page<Score> scorePage = scoreRepository.findByStudentCourseAndExamType(
                personId, courseId, examType, pageable);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (Score s : scorePage.getContent()) {
                Map<String, Object> m = new HashMap<>();
                m.put("scoreId", s.getScoreId());
                m.put("personId", s.getStudent().getPersonId());
                m.put("courseId", s.getCourse().getCourseId());
                m.put("studentNum", s.getStudent().getPerson().getNum());
                m.put("studentName", s.getStudent().getPerson().getName());
                m.put("className", s.getStudent().getClassName());
                m.put("courseNum", s.getCourse().getNum());
                m.put("courseName", s.getCourse().getName());
                m.put("credit", s.getCourse().getCredit());
                m.put("mark", s.getMark());
                m.put("examType", s.getExamType() != null ? s.getExamType() : "");
                m.put("ranking", s.getRanking() != null ? s.getRanking() : 0);
                dataList.add(m);
            }
            
            // 构建分页响应
            Map<String, Object> result = new HashMap<>();
            result.put("content", dataList);
            result.put("totalElements", scorePage.getTotalElements());
            result.put("totalPages", scorePage.getTotalPages());
            result.put("size", scorePage.getSize());
            result.put("number", scorePage.getNumber());
            result.put("first", scorePage.isFirst());
            result.put("last", scorePage.isLast());
            
            // 计算加权平均分
            if (courseId > 0) {
                Double weightedAverage = examWeightService.calculateWeightedAverage(dataList, courseId);
                result.put("weightedAverage", weightedAverage);
            }
            
            log.info("分页查询成绩成功 - 页码: {}, 大小: {}, 总数: {}", 
                    page, size, scorePage.getTotalElements());
            
            return CommonMethod.getReturnData(result);
            
        } catch (Exception e) {
            log.error("分页查询成绩失败", e);
            return CommonMethod.getReturnMessageError("查询失败: " + e.getMessage());
        }
    }
    @Transactional
    public DataResponse scoreSave(DataRequest dataRequest) {
        // 1. 参数验证
        ScoreValidationService.ValidationResult validation = validationService.validateScoreSave(dataRequest);
        if (validation.hasErrors()) {
            log.warn("成绩保存验证失败: {}", validation.getErrorMessage());
            return CommonMethod.getReturnMessageError(validation.getErrorMessage());
        }
        
        Integer personId = dataRequest.getInteger("personId");
        Integer courseId = dataRequest.getInteger("courseId");
        Integer mark = dataRequest.getInteger("mark");
        String examType = dataRequest.getString("examType");
        Integer scoreId = dataRequest.getInteger("scoreId");
        
        try {
            // 2. 检查重复性（新增时）
            if (scoreId == null || scoreId == 0) {
                boolean exists = scoreRepository.existsByStudentPersonIdAndCourseCourseIdAndExamType(
                    personId, courseId, examType);
                if (exists) {
                    log.warn("成绩已存在 - 学生ID: {}, 课程ID: {}, 考试类型: {}", personId, courseId, examType);
                    return CommonMethod.getReturnMessageError("该学生在此课程的这个考试类型中已有成绩记录");
                }
            }
            
            // 3. 获取或创建成绩对象
            Score s = null;
            if (scoreId != null && scoreId > 0) {
                Optional<Score> op = scoreRepository.findById(scoreId);
                if (op.isPresent()) {
                    s = op.get();
                    // 检查是否修改了关键信息导致重复
                    if (!s.getStudent().getPersonId().equals(personId) || 
                        !s.getCourse().getCourseId().equals(courseId) ||
                        !Objects.equals(s.getExamType(), examType)) {
                        
                        boolean exists = scoreRepository.existsByStudentPersonIdAndCourseCourseIdAndExamType(
                            personId, courseId, examType);
                        if (exists) {
                            log.warn("修改后成绩已存在 - 学生ID: {}, 课程ID: {}, 考试类型: {}", personId, courseId, examType);
                            return CommonMethod.getReturnMessageError("修改后的成绩记录已存在");
                        }
                    }
                }
            }
            
            if (s == null) {
                s = new Score();
                s.setStudent(studentRepository.findById(personId)
                    .orElseThrow(() -> new RuntimeException("学生不存在: " + personId)));
                s.setCourse(courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("课程不存在: " + courseId)));
            }
            
            // 4. 设置成绩信息
            s.setMark(mark);
            s.setExamType(examType);
            
            // 5. 保存成绩
            scoreRepository.save(s);
            log.info("成绩保存成功 - 成绩ID: {}, 学生ID: {}, 课程ID: {}, 考试类型: {}, 分数: {}", 
                    s.getScoreId(), personId, courseId, examType, mark);
            
            // 6. 自动计算排名
            rankingService.calculateRanking(courseId, examType);
            
            // 7. 自动计算最终成绩
            try {
                // 计算该学生该课程的最终成绩
                finalScoreCalculationService.calculateCourseFinalScore(courseId, personId);
                
                // 计算该学生的总最终成绩
                finalScoreCalculationService.calculateStudentFinalScore(personId);
                
                log.info("自动计算最终成绩完成 - 学生ID: {}, 课程ID: {}", personId, courseId);
            } catch (Exception e) {
                log.warn("自动计算最终成绩失败 - 学生ID: {}, 课程ID: {}, 错误: {}", 
                        personId, courseId, e.getMessage());
                // 不影响成绩保存的主流程，只记录警告日志
            }
            
            return CommonMethod.getReturnMessageOK();
            
        } catch (Exception e) {
            log.error("成绩保存失败", e);
            return CommonMethod.getReturnMessageError("保存失败: " + e.getMessage());
        }
    }
    @Transactional
    public DataResponse scoreDelete(DataRequest dataRequest) {
        Integer scoreId = dataRequest.getInteger("scoreId");
        if (scoreId == null || scoreId == 0) {
            return CommonMethod.getReturnMessageError("成绩ID不能为空");
        }
        
        try {
            Optional<Score> op = scoreRepository.findById(scoreId);
            if (op.isPresent()) {
                Score s = op.get();
                Integer courseId = s.getCourse().getCourseId();
                String examType = s.getExamType();
                
                scoreRepository.delete(s);
                log.info("成绩删除成功 - 成绩ID: {}, 学生ID: {}, 课程ID: {}, 考试类型: {}", 
                        scoreId, s.getStudent().getPersonId(), courseId, examType);
                
                // 重新计算排名
                if (courseId != null && examType != null) {
                    rankingService.calculateRanking(courseId, examType);
                }
                
                // 重新计算最终成绩
                try {
                    Integer personId = s.getStudent().getPersonId();
                    // 计算该学生该课程的最终成绩
                    finalScoreCalculationService.calculateCourseFinalScore(courseId, personId);
                    
                    // 计算该学生的总最终成绩
                    finalScoreCalculationService.calculateStudentFinalScore(personId);
                    
                    log.info("删除后重新计算最终成绩完成 - 学生ID: {}, 课程ID: {}", personId, courseId);
                } catch (Exception e) {
                    log.warn("删除后重新计算最终成绩失败 - 学生ID: {}, 课程ID: {}, 错误: {}", 
                            s.getStudent().getPersonId(), courseId, e.getMessage());
                    // 不影响成绩删除的主流程，只记录警告日志
                }
                
                return CommonMethod.getReturnMessageOK();
            } else {
                log.warn("成绩不存在 - 成绩ID: {}", scoreId);
                return CommonMethod.getReturnMessageError("成绩记录不存在");
            }
        } catch (Exception e) {
            log.error("成绩删除失败 - 成绩ID: {}", scoreId, e);
            return CommonMethod.getReturnMessageError("删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取学生最终成绩分析
     * @param personId 学生ID
     * @return 学生最终成绩分析数据
     */
    public DataResponse getStudentFinalScoreAnalysis(Integer personId) {
        try {
            // 参数校验
            if (personId == null || personId <= 0) {
                return CommonMethod.getReturnMessageError("无效的学生ID");
            }
            
            // 获取学生所有成绩
            List<Score> scores;
            try {
                scores = scoreRepository.findByStudentPersonId(personId);
            } catch (Exception e) {
                log.error("获取学生 {} 的成绩记录失败", personId, e);
                return CommonMethod.getReturnMessageError("获取成绩记录失败: " + e.getMessage());
            }
            
            if (scores.isEmpty()) {
                return CommonMethod.getReturnMessageError("该学生没有成绩记录");
            }
            
            // 过滤无效成绩记录
            List<Score> validScores = scores.stream()
                .filter(score -> score != null && 
                               score.getCourse() != null && 
                               score.getCourse().getCourseId() != null)
                .collect(Collectors.toList());
                
            if (validScores.isEmpty()) {
                return CommonMethod.getReturnMessageError("该学生没有有效的成绩记录");
            }
            
            // 按课程分组
            Map<Integer, List<Score>> scoresByCourse = validScores.stream()
                .collect(Collectors.groupingBy(score -> score.getCourse().getCourseId()));
            
            // 计算每门课程的最终成绩
            List<Map<String, Object>> courseFinalScores = new ArrayList<>();
            double totalWeightedScore = 0.0;
            int totalCredits = 0;
            
            for (Map.Entry<Integer, List<Score>> entry : scoresByCourse.entrySet()) {
                Integer courseId = entry.getKey();
                List<Score> courseScores = entry.getValue();
                
                try {
                    // 计算该课程的最终成绩
                    Double courseFinalScore = finalScoreCalculationService.calculateCourseFinalScore(personId, courseId);
                    
                    // 获取课程信息
                    Course course = courseScores.get(0).getCourse();
                    if (course == null) {
                        log.warn("课程信息为空，学生ID: {}, 课程ID: {}", personId, courseId);
                        continue;
                    }
                    
                    // 检查学分是否有效
                    Integer credit = course.getCredit();
                    if (credit == null || credit <= 0) {
                        log.warn("课程 {} 的学分为0或无效，学生ID: {}", courseId, personId);
                        credit = 1; // 默认学分为1
                    }
                    
                    // 按学分加权计算总分
                    totalWeightedScore += courseFinalScore * credit;
                    totalCredits += credit;
                    
                    // 构建课程成绩信息
                    Map<String, Object> courseInfo = new HashMap<>();
                    courseInfo.put("courseId", courseId);
                    courseInfo.put("courseName", course.getName() != null ? course.getName() : "未知课程");
                    courseInfo.put("courseNum", course.getNum() != null ? course.getNum() : "未知编号");
                    courseInfo.put("credit", credit);
                    courseInfo.put("finalScore", courseFinalScore);
                    
                    // 按考试类型分组显示成绩
                    Map<String, List<Score>> scoresByExamType = courseScores.stream()
                        .filter(score -> score.getExamType() != null)
                        .collect(Collectors.groupingBy(score -> score.getExamType()));
                    
                    // 如果没有有效的考试类型，使用默认值
                    if (scoresByExamType.isEmpty()) {
                        log.warn("课程 {} 没有有效的考试类型，学生ID: {}", courseId, personId);
                        scoresByExamType.put("期末考试", courseScores);
                    }
                    
                    List<Map<String, Object>> examTypeScores = new ArrayList<>();
                    for (Map.Entry<String, List<Score>> examEntry : scoresByExamType.entrySet()) {
                        Map<String, Object> examScore = new HashMap<>();
                        examScore.put("examType", examEntry.getKey());
                        
                        // 过滤有效成绩并取最高分
                        Integer maxScore = examEntry.getValue().stream()
                            .filter(score -> score.getMark() != null && score.getMark() >= 0 && score.getMark() <= 100)
                            .mapToInt(Score::getMark)
                            .max()
                            .orElse(0);
                        
                        examScore.put("score", maxScore);
                        examTypeScores.add(examScore);
                    }
                    
                    courseInfo.put("examTypeScores", examTypeScores);
                    courseFinalScores.add(courseInfo);
                } catch (Exception e) {
                    log.error("处理课程 {} 成绩时出错，学生ID: {}", courseId, personId, e);
                    // 继续处理其他课程
                }
            }
            
            if (courseFinalScores.isEmpty()) {
                return CommonMethod.getReturnMessageError("无法获取有效的课程成绩信息");
            }
            
            // 计算总加权平均分
            double weightedAverageScore = totalCredits > 0 ? totalWeightedScore / totalCredits : 0.0;
            
            // 确保加权平均分在合理范围内
            if (weightedAverageScore < 0) {
                weightedAverageScore = 0.0;
            } else if (weightedAverageScore > 100) {
                weightedAverageScore = 100.0;
            }
            
            // 计算班级排名
            Integer classRanking = 0;
            try {
                classRanking = calculateClassRanking(personId, weightedAverageScore);
            } catch (Exception e) {
                log.error("计算班级排名失败，学生ID: {}", personId, e);
            }
            
            // 获取学生基本信息
            String studentName = "未知学生";
            String studentNum = "未知学号";
            String className = "未知班级";
            
            try {
                if (scores.get(0) != null && scores.get(0).getStudent() != null && scores.get(0).getStudent().getPerson() != null) {
                    studentName = scores.get(0).getStudent().getPerson().getName() != null ? 
                                 scores.get(0).getStudent().getPerson().getName() : "未知学生";
                    studentNum = scores.get(0).getStudent().getPerson().getNum() != null ? 
                                scores.get(0).getStudent().getPerson().getNum() : "未知学号";
                }
                if (scores.get(0) != null && scores.get(0).getStudent() != null) {
                    className = scores.get(0).getStudent().getClassName() != null ? 
                                scores.get(0).getStudent().getClassName() : "未知班级";
                }
            } catch (Exception e) {
                log.error("获取学生基本信息失败，学生ID: {}", personId, e);
            }
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("studentId", personId);
            result.put("studentName", studentName);
            result.put("studentNum", studentNum);
            result.put("className", className);
            result.put("courseFinalScores", courseFinalScores);
            result.put("totalCredits", totalCredits);
            result.put("weightedAverageScore", weightedAverageScore);
            result.put("classRanking", classRanking);
            
            return CommonMethod.getReturnData(result);
            
        } catch (Exception e) {
            log.error("获取学生最终成绩分析失败 - 学生ID: {}", personId, e);
            return CommonMethod.getReturnMessageError("获取成绩分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取班级最终成绩分析
     * @param courseId 课程ID
     * @param examType 考试类型
     * @return 班级最终成绩分析数据
     */
    public DataResponse getClassFinalScoreAnalysis(Integer courseId, String examType) {
        try {
            // 参数校验
            if (courseId != null && courseId <= 0) {
                return CommonMethod.getReturnMessageError("无效的课程ID");
            }
            
            // 获取课程所有成绩
            List<Score> scores;
            try {
                if (courseId != null && courseId > 0) {
                    scores = scoreRepository.findByCourseCourseId(courseId);
                } else {
                    scores = scoreRepository.findAll();
                }
            } catch (Exception e) {
                log.error("获取成绩记录失败 - 课程ID: {}", courseId, e);
                return CommonMethod.getReturnMessageError("获取成绩记录失败: " + e.getMessage());
            }
            
            if (scores.isEmpty()) {
                return CommonMethod.getReturnMessageError("没有成绩记录");
            }
            
            // 过滤无效成绩记录
            List<Score> validScores = scores.stream()
                .filter(score -> score != null && 
                               score.getStudent() != null && 
                               score.getStudent().getClassName() != null &&
                               score.getStudent().getPersonId() != null)
                .collect(Collectors.toList());
                
            if (validScores.isEmpty()) {
                return CommonMethod.getReturnMessageError("没有有效的成绩记录");
            }
            
            // 按班级分组
            Map<String, List<Score>> scoresByClass = validScores.stream()
                .collect(Collectors.groupingBy(score -> score.getStudent().getClassName()));
            
            List<Map<String, Object>> classAnalyses = new ArrayList<>();
            
            for (Map.Entry<String, List<Score>> classEntry : scoresByClass.entrySet()) {
                String className = classEntry.getKey();
                List<Score> classScores = classEntry.getValue();
                
                try {
                    // 按学生分组
                    Map<Integer, List<Score>> scoresByStudent = classScores.stream()
                        .collect(Collectors.groupingBy(score -> score.getStudent().getPersonId()));
                    
                    List<Double> studentFinalScores = new ArrayList<>();
                    
                    // 计算每个学生的最终成绩
                    for (Map.Entry<Integer, List<Score>> studentEntry : scoresByStudent.entrySet()) {
                        Integer studentId = studentEntry.getKey();
                        
                        try {
                            Double finalScore = null;
                            
                            // 如果指定了课程ID，计算该课程的最终成绩
                            if (courseId != null && courseId > 0) {
                                finalScore = finalScoreCalculationService.calculateCourseFinalScore(studentId, courseId);
                            } else {
                                // 计算学生的总最终成绩
                                finalScore = finalScoreCalculationService.calculateStudentFinalScore(studentId);
                            }
                            
                            // 确保成绩在合理范围内
                            if (finalScore != null) {
                                if (finalScore < 0) {
                                    finalScore = 0.0;
                                } else if (finalScore > 100) {
                                    finalScore = 100.0;
                                }
                                studentFinalScores.add(finalScore);
                            }
                        } catch (Exception e) {
                            log.error("计算学生 {} 的最终成绩失败", studentId, e);
                            // 继续处理其他学生
                        }
                    }
                    
                    if (studentFinalScores.isEmpty()) {
                        log.warn("班级 {} 没有有效的学生最终成绩", className);
                        continue;
                    }
                    
                    // 计算班级统计数据
                    double averageScore = studentFinalScores.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                    
                    double highestScore = studentFinalScores.stream()
                        .mapToDouble(Double::doubleValue)
                        .max()
                        .orElse(0.0);
                    
                    double lowestScore = studentFinalScores.stream()
                        .mapToDouble(Double::doubleValue)
                        .min()
                        .orElse(0.0);
                    
                    // 计算及格率（60分及以上为及格）
                    long passCount = studentFinalScores.stream()
                        .mapToDouble(Double::doubleValue)
                        .filter(score -> score >= 60.0)
                        .count();
                    
                    double passRate = studentFinalScores.size() > 0 ? (double) passCount / studentFinalScores.size() : 0.0;
                    
                    // 构建班级分析结果
                    Map<String, Object> classAnalysis = new HashMap<>();
                    classAnalysis.put("className", className);
                    classAnalysis.put("studentCount", studentFinalScores.size());
                    classAnalysis.put("averageScore", averageScore);
                    classAnalysis.put("highestScore", highestScore);
                    classAnalysis.put("lowestScore", lowestScore);
                    classAnalysis.put("passRate", passRate);
                    
                    classAnalyses.add(classAnalysis);
                } catch (Exception e) {
                    log.error("处理班级 {} 成绩分析时出错", className, e);
                    // 继续处理其他班级
                }
            }
            
            if (classAnalyses.isEmpty()) {
                return CommonMethod.getReturnMessageError("无法获取有效的班级成绩分析");
            }
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("classAnalyses", classAnalyses);
            
            return CommonMethod.getReturnData(result);
            
        } catch (Exception e) {
            log.error("获取班级最终成绩分析失败 - 课程ID: {}, 考试类型: {}", courseId, examType, e);
            return CommonMethod.getReturnMessageError("获取班级成绩分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算学生在班级中的排名
     * @param personId 学生ID
     * @param studentFinalScore 学生最终成绩
     * @return 班级排名
     */
    private Integer calculateClassRanking(Integer personId, double studentFinalScore) {
        try {
            // 获取学生信息
            Score studentScore = scoreRepository.findByStudentPersonId(personId).get(0);
            String className = studentScore.getStudent().getClassName();
            
            // 使用排名计算服务计算班级排名
            Map<Integer, Integer> classRankings = rankingService.calculateClassRanking(className);
            
            // 返回当前学生的排名
            return classRankings.getOrDefault(personId, 0);
            
        } catch (Exception e) {
            log.error("计算班级排名失败 - 学生ID: {}", personId, e);
            return 0;
        }
    }

}
