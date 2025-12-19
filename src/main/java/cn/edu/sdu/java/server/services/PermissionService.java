package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.User;
import cn.edu.sdu.java.server.repositorys.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 权限控制服务
 * 用于检查用户操作权限
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {
    
    private final UserRepository userRepository;
    
    /**
     * 检查用户是否有权限操作指定学生的成绩
     * @param userId 当前用户ID
     * @param targetStudentId 目标学生ID
     * @return 是否有权限
     */
    public boolean canAccessStudentScore(Integer userId, Integer targetStudentId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("用户不存在: {}", userId);
                return false;
            }
            
            // 管理员可以访问所有学生成绩
            if (isAdmin(user)) {
                return true;
            }
            
            // 教师只能访问自己班级的学生成绩
            if (isTeacher(user)) {
                // 这里需要根据实际业务逻辑判断教师是否有权限访问该学生
                // 简化实现：假设教师可以访问所有学生成绩
                return true;
            }
            
            // 学生只能访问自己的成绩
            if (isStudent(user)) {
                return userId.equals(targetStudentId);
            }
            
            return false;
        } catch (Exception e) {
            log.error("权限检查失败", e);
            return false;
        }
    }
    
    /**
     * 检查用户是否有权限操作指定课程的成绩
     * @param userId 当前用户ID
     * @param courseId 课程ID
     * @return 是否有权限
     */
    public boolean canAccessCourseScore(Integer userId, Integer courseId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("用户不存在: {}", userId);
                return false;
            }
            
            // 管理员可以访问所有课程成绩
            if (isAdmin(user)) {
                return true;
            }
            
            // 教师只能访问自己教授的课程成绩
            if (isTeacher(user)) {
                // 这里需要根据实际业务逻辑判断教师是否教授该课程
                // 简化实现：假设教师可以访问所有课程成绩
                return true;
            }
            
            // 学生不能直接访问课程成绩列表，只能查看自己的成绩
            return false;
        } catch (Exception e) {
            log.error("权限检查失败", e);
            return false;
        }
    }
    
    /**
     * 检查用户是否有权限导入成绩
     * @param userId 当前用户ID
     * @return 是否有权限
     */
    public boolean canImportScores(Integer userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("用户不存在: {}", userId);
                return false;
            }
            
            // 记录用户类型信息
            String userTypeName = user.getUserType() != null ? user.getUserType().getName() : "NULL";
            log.info("检查用户 {} 的导入成绩权限，用户类型: {}", userId, userTypeName);
            
            // 只有管理员和教师可以导入成绩
            boolean hasPermission = isAdmin(user) || isTeacher(user);
            log.info("用户 {} 导入成绩权限检查结果: {}", userId, hasPermission);
            
            return hasPermission;
        } catch (Exception e) {
            log.error("权限检查失败", e);
            return false;
        }
    }
    
    /**
     * 检查用户是否有权限导出成绩
     * @param userId 当前用户ID
     * @return 是否有权限
     */
    public boolean canExportScores(Integer userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("用户不存在: {}", userId);
                return false;
            }
            
            // 管理员和教师可以导出成绩
            // 学生可以导出自己的成绩
            return isAdmin(user) || isTeacher(user) || isStudent(user);
        } catch (Exception e) {
            log.error("权限检查失败", e);
            return false;
        }
    }
    
    /**
     * 检查用户是否为管理员
     */
    private boolean isAdmin(User user) {
        if (user.getUserType() == null) {
            return false;
        }
        String typeName = user.getUserType().getName();
        return "ROLE_ADMIN".equals(typeName) || 
               "ADMIN".equals(typeName) ||
               "管理员".equals(typeName);
    }
    
    /**
     * 检查用户是否为教师
     */
    private boolean isTeacher(User user) {
        if (user.getUserType() == null) {
            return false;
        }
        String typeName = user.getUserType().getName();
        return "ROLE_TEACHER".equals(typeName) || 
               "TEACHER".equals(typeName) ||
               "教师".equals(typeName);
    }
    
    /**
     * 检查用户是否为学生
     */
    private boolean isStudent(User user) {
        if (user.getUserType() == null) {
            return false;
        }
        String typeName = user.getUserType().getName();
        return "ROLE_STUDENT".equals(typeName) || 
               "STUDENT".equals(typeName) ||
               "学生".equals(typeName);
    }
    
    /**
     * 获取用户可访问的学生ID集合
     * @param userId 当前用户ID
     * @return 可访问的学生ID集合
     */
    public Set<Integer> getAccessibleStudentIds(Integer userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return Set.of();
            }
            
            // 管理员可以访问所有学生
            if (isAdmin(user)) {
                // 这里应该返回所有学生ID，简化实现
                return Set.of();
            }
            
            // 教师可以访问自己班级的学生
            if (isTeacher(user)) {
                // 这里应该返回教师班级的学生ID，简化实现
                return Set.of();
            }
            
            // 学生只能访问自己
            if (isStudent(user)) {
                return Set.of(userId);
            }
            
            return Set.of();
        } catch (Exception e) {
            log.error("获取可访问学生ID失败", e);
            return Set.of();
        }
    }
}