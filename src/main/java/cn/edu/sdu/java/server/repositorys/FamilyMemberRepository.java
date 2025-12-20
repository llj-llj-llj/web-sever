package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember,Integer> {
    List<FamilyMember> findByStudentPersonId(Integer personId);


   //新增删除操作
    @Modifying
    @Transactional
    void deleteByStudentPersonId(Integer personId);
}
