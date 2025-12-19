package cn.edu.sdu.java.server.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 事务管理配置
 * 确保数据操作的事务安全和一致性
 */
@Configuration
@EnableTransactionManagement
@Slf4j
public class TransactionConfiguration {
    
    // 简化配置，使用Spring Boot默认的事务管理
    // 通过@EnableTransactionManagement启用声明式事务
    // 在Service层使用@Transactional注解即可
    
}