package cn.edu.sdu.java.server.config;

import cn.edu.sdu.java.server.models.FinalScoreRule;
import cn.edu.sdu.java.server.repositorys.FinalScoreRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时初始化最终成绩规则
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinalScoreRuleInitializer implements ApplicationRunner {

    private final FinalScoreRuleRepository finalScoreRuleRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 检查是否已有规则
        List<FinalScoreRule> existingRules = finalScoreRuleRepository.findAll();
        
        if (existingRules.isEmpty()) {
            log.info("初始化默认最终成绩计算规则");
            
            // 创建默认规则
            FinalScoreRule finalExamRule = new FinalScoreRule();
            finalExamRule.setExamType("期末考试");
            finalExamRule.setWeight(0.6);
            finalExamRule.setDescription("期末考试权重60%");
            finalExamRule.setIsDefault(true);
            
            FinalScoreRule midTermRule = new FinalScoreRule();
            midTermRule.setExamType("期中考试");
            midTermRule.setWeight(0.3);
            midTermRule.setDescription("期中考试权重30%");
            midTermRule.setIsDefault(false);
            
            FinalScoreRule usualScoreRule = new FinalScoreRule();
            usualScoreRule.setExamType("平时成绩");
            usualScoreRule.setWeight(0.1);
            usualScoreRule.setDescription("平时成绩权重10%");
            usualScoreRule.setIsDefault(false);
            
            // 保存规则
            finalScoreRuleRepository.save(finalExamRule);
            finalScoreRuleRepository.save(midTermRule);
            finalScoreRuleRepository.save(usualScoreRule);
            
            log.info("默认最终成绩计算规则初始化完成");
        } else {
            log.info("最终成绩计算规则已存在，跳过初始化");
        }
    }
}