package com.guowy.workflow.webapp.config;

import com.google.common.collect.Lists;
import com.guowy.workflow.webapp.config.handler.CustomUserTaskParseHandler;
import com.guowy.workflow.webapp.config.valid.CustomProcessValidatorFactory;
import de.odysseus.el.ExpressionFactoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.StrongUuidGenerator;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.el.ExpressionFactory;
import javax.sql.DataSource;

/**
 * @author LiJingTang
 * @date 2020-05-13 11:06
 */
@Slf4j
@Configuration
public class MainConfig {

    @Value("${guowy.open-uuid:false}")
    private boolean openUuid;

    @Bean
    public SpringProcessEngineConfiguration engineConfig(DataSource dataSource, PlatformTransactionManager transactionManager,
                                                         CustomProcessValidatorFactory validatorFactory,
                                                         CustomUserTaskParseHandler userTaskParseHandler) {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        // 启动不创建表
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
        // 设置 数据库类型 减少启动连接
        configuration.setDatabaseType(ProcessEngineConfigurationImpl.DATABASE_TYPE_MYSQL);
        // 流程定义缓存限制个数
        configuration.setProcessDefinitionCacheLimit(200);
        // 添加自定义校验器
        configuration.setProcessValidator(validatorFactory.createDefaultProcessValidator());
        configuration.setPreBpmnParseHandlers(Lists.newArrayList(userTaskParseHandler));

        if (openUuid) {
            configuration.setIdGenerator(new StrongUuidGenerator());
            log.info("启动UUID策略");
        } else {
            log.info("使用默认ID策略");
        }
        return configuration;
    }

    @Bean
    public ProcessEngineFactoryBean factoryBean(ProcessEngineConfigurationImpl configuration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(configuration);
        return factoryBean;
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    public static final String MANAGEMENT = "managementService";

    @Bean(MANAGEMENT)
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    @Bean
    public ExpressionFactory expressionFactory() {
        return new ExpressionFactoryImpl();
    }

}
