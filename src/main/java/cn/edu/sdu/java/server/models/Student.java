package cn.edu.sdu.java.server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Student学生表实体类 保存每个学生的信息，
 * Integer personId 学生表 student 主键 person_id 与Person表主键相同
 * Person person 关联到该用户所用的Person对象，账户所对应的人员信息 person_id 关联 person 表主键 person_id
 * String major 专业
 * String className 班级
 *
 */
@Getter
@Setter
@Entity
@Table(	name = "student",
        uniqueConstraints = {
        })
public class Student {
    @Id
    private Integer personId;

    @OneToOne
    @MapsId
    @JoinColumn(name="person_id")
    private Person person;

    @Size(max = 20)
    private String major;

    @Size(max = 50)
    private String className;
    
    @Column(name = "final_score")
    private Double finalScore; // 学生总最终成绩（按学分加权）

    public Integer getPersonId() {
        return personId;
    }

    public Person getPerson() {
        return person;
    }

    public String getMajor() {
        return major;
    }

    public String getClassName() {
        return className;
    }

    public void setPersonId(Integer personId) {
        this.personId = personId;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}


