package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(	name = "student_leave",
        uniqueConstraints = {
        })
public class StudentLeave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer studentLeaveId;

    @ManyToOne
    @JoinColumn(name="studentId")
    private Student student;

    @ManyToOne
    @JoinColumn(name="teacherId")
    private Teacher teacher;

    @Size(max=50)
    private String leaveStartDate;
    @Size(max=50)
    private String leaveEndDate;
    @Size(max=100)
    private String reason;
    private Integer state;
    private Date applyTime;
    @Size(max=100)
    private String teacherComment;
    private Date teacherTime;
    @Size(max=100)
    private String adminComment;
    private Date adminTime;
    @Size(max=200)
    private String attachment;

    public Student getStudent() {
        return student;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public Integer getStudentLeaveId() {
        return studentLeaveId;
    }

    public Integer getState() {
        return state;
    }

    public String getReason() {
        return reason;
    }

    public String getLeaveStartDate() {
        return leaveStartDate;
    }

    public String getLeaveEndDate() {
        return leaveEndDate;
    }

    public String getAttachment() {
        return attachment;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public String getTeacherComment() {
        return teacherComment;
    }

    public Date getApplyTime() {
        return applyTime;
    }

    public Date getAdminTime() {
        return adminTime;
    }

    public Date getTeacherTime() {
        return teacherTime;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public void setApplyTime(Date applyTime) {
        this.applyTime = applyTime;
    }

    public void setTeacherComment(String teacherComment) {
        this.teacherComment = teacherComment;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public void setLeaveStartDate(String leaveStartDate) {
        this.leaveStartDate = leaveStartDate;
    }

    public void setLeaveEndDate(String leaveEndDate) {
        this.leaveEndDate = leaveEndDate;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public void setAdminTime(Date adminTime) {
        this.adminTime = adminTime;
    }

    public void setTeacherTime(Date teacherTime) {
        this.teacherTime = teacherTime;
    }

    public void setStudentLeaveId(Integer studentLeaveId) {
        this.studentLeaveId = studentLeaveId;
    }
}