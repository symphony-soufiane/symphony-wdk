package com.symphony.bdk.workflow.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.symphony.bdk.workflow.versioning",
    transactionManagerRef = "transactionManager")
@Profile("!test")
@Slf4j
public class WorkflowDataSourceConfiguration {

  @Bean("datasource")
  @Primary
  @ConfigurationProperties("spring.datasource.wdk")
  public DataSource wdkDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  @Primary
  public PlatformTransactionManager transactionManager(@Qualifier("datasource") DataSource dataSource) {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setDataSource(dataSource);
    return transactionManager;
  }

  @Bean
  @ConditionalOnPropertyNotEmpty("wdk.workflows.path")
  public Object configStateCheck() {
    throw new IllegalStateException(
        "Workflow folder watcher must be disabled while using workflow management API. "
            + "Please remove 'wdk.workflows.path' property from the configuration file.");
  }

}
