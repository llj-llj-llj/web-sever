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

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreService {
    private final CourseRepository courseRepository;
    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;
    private final ScoreValidationService validationService;
    private final RankingCalculationService rankingService;
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
            m.put("scoreId", s.getScoreId()+"");
            m.put("personId",s.getStudent().getPersonId()+"");
            m.put("courseId",s.getCourse().getCourseId()+"");
            m.put("studentNum",s.getStudent().getPerson().getNum());
            m.put("studentName",s.getStudent().getPerson().getName());
            m.put("className",s.getStudent().getClassName());
            m.put("courseNum",s.getCourse().getNum());
            m.put("courseName",s.getCourse().getName());
            m.put("credit",""+s.getCourse().getCredit());
            m.put("mark",""+s.getMark());
            m.put("examType", s.getExamType() != null ? s.getExamType() : "");
            m.put("ranking", s.getRanking() != null ? s.getRanking().toString() : "");
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

}
