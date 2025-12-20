package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.StudentLeave;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.repositorys.StudentLeaveRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.management.PlatformLoggingMXBean;
import java.util.*;

@Service
public class StudentLeaveService {
    @Value("${attach.folder}")    //环境配置变量获取
    private String attachFolder;  //服务器端数据存储
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentLeaveRepository studentLeaveRepository;
    private final PersonRepository personRepository;

    public StudentLeaveService(StudentRepository studentRepository, TeacherRepository teacherRepository, StudentLeaveRepository studentLeaveRepository, PersonRepository personRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.studentLeaveRepository = studentLeaveRepository;
        this.personRepository = personRepository;
    }

    public OptionItemList getTeacherItemOptionList(DataRequest dataRequest) {
        List<Person> personList = personRepository.findByType("2");  //获取所有类型为2(教师)的人员
        List<OptionItem> itemList = new ArrayList<>();
        for (Person p : personList) {
            itemList.add(new OptionItem(p.getPersonId(), p.getPersonId() + "", p.getNum() + "-" + p.getName()));
        }
        return new OptionItemList(0, itemList);
    }

    public DataResponse getStudentLeaveList(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        String userName = CommonMethod.getUsername();
        Integer personId = CommonMethod.getPersonId();
        Integer state = dataRequest.getInteger("state");
        if(state == null)
            state = -1;
        String search = dataRequest.getString("search");
        assert roleName != null;
        // 添加调试信息
        System.out.println("[DEBUG] getStudentLeaveList - roleName: " + roleName + ", userName: " + userName + ", personId: " + personId + ", state: " + state + ", search: " + search);
        List<StudentLeave> slList;
        switch (roleName) {
            case "ROLE_STUDENT":
                System.out.println("[DEBUG] 学生角色，使用studentId: " + personId + "查询");
                slList = studentLeaveRepository.getStudentLeaveList(-1, search, "", "", personId);
                System.out.println("[DEBUG] 学生角色查询结果数量: " + (slList != null ? slList.size() : 0));
                break;
            case "ROLE_TEACHER":
                System.out.println("[DEBUG] 教师角色，使用teacherNum: " + userName + "查询");
                slList = studentLeaveRepository.getStudentLeaveList(-1, search, "", userName, null);
                System.out.println("[DEBUG] 教师角色查询结果数量: " + (slList != null ? slList.size() : 0));
                break;
            case "ROLE_ADMIN":
                System.out.println("[DEBUG] 管理员角色，查询所有记录");
                slList = studentLeaveRepository.getStudentLeaveList(state, search, "", "", null);
                System.out.println("[DEBUG] 管理员角色查询结果数量: " + (slList != null ? slList.size() : 0));
                break;
            default:
                System.out.println("[DEBUG] 默认角色，查询所有记录");
                slList = studentLeaveRepository.getStudentLeaveList(state, search, "", "", null);
                System.out.println("[DEBUG] 默认角色查询结果数量: " + (slList != null ? slList.size() : 0));
                break;
        };
        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, Object> map;
        Student s;
        Teacher t;
        ComDataUtil di = ComDataUtil.getInstance();
        if (slList != null && !slList.isEmpty()) {
            for (StudentLeave sl : slList) {
                map = new HashMap<>();
                s = sl.getStudent();
                t = sl.getTeacher();
                map.put("studentLeaveId", sl.getStudentLeaveId());
                map.put("studentNum", s.getPerson().getNum());
                map.put("studentName", s.getPerson().getName());
                map.put("studentId", s.getPersonId());
                map.put("teacherName", t.getPerson().getNum() + t.getPerson().getName());
                map.put("state", sl.getState());
                map.put("stateName", di.getDictionaryLabelByValue("SHZTM", sl.getState()+""));
                map.put("reason", sl.getReason());
                map.put("leaveStartDate", sl.getLeaveStartDate());
                map.put("leaveEndDate", sl.getLeaveEndDate());
                map.put("attachment", sl.getAttachment());
                map.put("adminComment", sl.getAdminComment());
                map.put("teacherId", t.getPersonId());
                map.put("teacherComment", sl.getTeacherComment());
                dataList.add(map);
            }
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse studentLeaveSave(DataRequest dataRequest) {
        Integer state = dataRequest.getInteger("state");
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        Integer teacherId = dataRequest.getInteger("teacherId");
        String leaveStartDate = dataRequest.getString("leaveStartDate");
        String leaveEndDate = dataRequest.getString("leaveEndDate");
        String reason = dataRequest.getString("reason");
        String attachment = dataRequest.getString("attachment");
        StudentLeave sl = null;
        if(studentLeaveId != null && studentLeaveId > 0) {
            Optional<StudentLeave> op = studentLeaveRepository.findById(studentLeaveId);
            if(op.isPresent())
                sl = op.get();
        }
        if(sl == null) {
            sl = new StudentLeave();
            sl.setState(0);
            sl.setApplyTime(new Date());
            sl.setTeacherComment("");
            sl.setAdminComment("");
            sl.setStudent(studentRepository.findByPersonNum(CommonMethod.getUsername()).get());
        }
        if(teacherId != null && teacherId > 0) {
            Optional<Teacher> op = teacherRepository.findById(teacherId);
            if(op.isPresent())
                sl.setTeacher(op.get());
        }
        sl.setLeaveStartDate(leaveStartDate);
        sl.setLeaveEndDate(leaveEndDate);
        sl.setReason(reason);
        // 确保state不为null，使用默认值0
        sl.setState(state != null ? state : 0);
        sl.setAttachment(attachment);
        studentLeaveRepository.save(sl);
        return CommonMethod.getReturnMessageOK();
    }
    public DataResponse studentLeaveCheck(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        Integer state = dataRequest.getInteger("state");
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        String teacherComment = dataRequest.getString("teacherComment");
        String adminComment = dataRequest.getString("adminComment");
        StudentLeave sl = null;
        if(studentLeaveId != null && studentLeaveId > 0) {
            Optional<StudentLeave> op = studentLeaveRepository.findById(studentLeaveId);
            if(op.isPresent())
                sl = op.get();
        }
        if(sl == null) {
            return CommonMethod.getReturnMessageOK();
        }
        if("ROLE_ADMIN".equals(roleName)) {
            sl.setAdminComment(adminComment);
            sl.setAdminTime(new Date());
            sl.setState(state+2);
        } else if("ROLE_TEACHER".equals(roleName)) {
            sl.setTeacherComment(teacherComment);
            sl.setTeacherTime(new Date());
            sl.setState(state);
        }
        studentLeaveRepository.save(sl);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse uploadAttachment(byte[] barr, String uploader, String fileName) {
        try {
            // 确保目录存在
            File dir = new File(attachFolder);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // 保存文件
            String remoteFile = "leave/" + uploader + "/" + System.currentTimeMillis() + "_" + fileName;
            File file = new File(attachFolder + remoteFile);
            // 确保子目录存在
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            OutputStream os = new FileOutputStream(file);
            os.write(barr);
            os.close();
            // 返回文件路径
            Map<String, Object> result = new HashMap<>();
            result.put("filePath", remoteFile);
            return CommonMethod.getReturnData(result);
        } catch (Exception e) {
            return CommonMethod.getReturnMessageError("上传错误：" + e.getMessage());
        }
    }

    public void exportStudentLeaveList(DataRequest dataRequest, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        // 获取请假列表数据
        String roleName = CommonMethod.getRoleName();
        String userName = CommonMethod.getUsername();
        Integer personId = CommonMethod.getPersonId();
        Integer state = dataRequest.getInteger("state");
        if(state == null)
            state = -1;
        String search = dataRequest.getString("search");
        assert roleName != null;
        List<StudentLeave> slList = switch (roleName) {
            case "ROLE_STUDENT" -> studentLeaveRepository.getStudentLeaveList(-1, search, "", "", personId);
            case "ROLE_TEACHER" -> studentLeaveRepository.getStudentLeaveList(-1, search, "", userName, null);
            case "ROLE_ADMIN" -> studentLeaveRepository.getStudentLeaveList(state, search, "", "", null);
            default -> null;
        };
        
        // 创建Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("请假数据列表");
        
        // 设置表头
        String[] headers = {"学号", "姓名", "教师", "请假时间范围", "请假原因", "状态", "教师备注", "管理员备注"};
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 填充数据
        ComDataUtil di = ComDataUtil.getInstance();
        if (slList != null && !slList.isEmpty()) {
            int rowNum = 1;
            for (StudentLeave sl : slList) {
                Row row = sheet.createRow(rowNum++);
                Student s = sl.getStudent();
                Teacher t = sl.getTeacher();
                
                row.createCell(0).setCellValue(s.getPerson().getNum());
                row.createCell(1).setCellValue(s.getPerson().getName());
                row.createCell(2).setCellValue(t.getPerson().getNum() + t.getPerson().getName());
                row.createCell(3).setCellValue(sl.getLeaveStartDate() + "至" + sl.getLeaveEndDate());
                row.createCell(4).setCellValue(sl.getReason());
                row.createCell(5).setCellValue(di.getDictionaryLabelByValue("SHZTM", sl.getState()+""));
                row.createCell(6).setCellValue(sl.getTeacherComment());
                row.createCell(7).setCellValue(sl.getAdminComment());
            }
        }
        
        // 设置列宽自适应
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=请假数据列表_" + System.currentTimeMillis() + ".xlsx");
        
        // 写入响应
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}