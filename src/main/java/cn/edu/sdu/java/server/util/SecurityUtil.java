package cn.edu.sdu.java.server.util;

import cn.edu.sdu.java.server.services.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 * 用于获取当前用户信息和权限检查
 */
public class SecurityUtil {
    
    /**
     * 获取当前登录用户的ID
     * @return 用户ID，如果未登录返回null
     */
    public static Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        return null;
    }
    
    /**
     * 获取当前登录用户的用户名
     * @return 用户名，如果未登录返回null
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return null;
    }
    
    /**
     * 获取当前登录用户的真实姓名
     * @return 真实姓名，如果未登录返回null
     */
    public static String getCurrentUserRealName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getPerName();
        }
        return null;
    }
    
    /**
     * 检查当前用户是否已登录
     * @return 是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !(authentication.getPrincipal() instanceof String);
    }
    
    /**
     * 检查当前用户是否具有指定权限
     * @param authority 权限名称
     * @return 是否具有权限
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(authority));
        }
        return false;
    }
    
    /**
     * 获取当前用户的所有权限
     * @return 权限集合
     */
    @SuppressWarnings("unchecked")
    public static java.util.Collection<org.springframework.security.core.GrantedAuthority> getCurrentUserAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return (java.util.Collection<org.springframework.security.core.GrantedAuthority>) userDetails.getAuthorities();
        }
        return java.util.Collections.emptyList();
    }
}