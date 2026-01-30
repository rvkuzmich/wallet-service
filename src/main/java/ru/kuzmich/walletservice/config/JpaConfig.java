package ru.kuzmich.walletservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "ru.kuzmich.walletservice.repository",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class JpaConfig {

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("ru.kuzmich.walletservice.model");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setGenerateDdl(false);
    vendorAdapter.setShowSql(true);
    em.setJpaVendorAdapter(vendorAdapter);

    Properties properties = new Properties();
    properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    properties.put("hibernate.hbm2ddl.auto", "validate");
    properties.put("hibernate.show_sql", "true");
    properties.put("hibernate.format_sql", "true");
    properties.put("hibernate.physical_naming_strategy",
        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

    em.setJpaProperties(properties);
    return em;
  }

  @Bean
  public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
    return transactionManager;
  }
}