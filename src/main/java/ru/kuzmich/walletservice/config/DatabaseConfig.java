//package ru.kuzmich.walletservice.config;
//
//import com.zaxxer.hikari.HikariDataSource;
//import java.util.Properties;
//import javax.sql.DataSource;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//@Configuration
//@EnableTransactionManagement
//public class DatabaseConfig {
//
//  @Value("${spring.datasource.url}")
//  private String dbUrl;
//
//  @Value("${spring.datasource.username}")
//  private String dbUsername;
//
//  @Value("${spring.datasource.password}")
//  private String dbPassword;
//
//  @Value("${spring.datasource.hikari.maximum-pool-size:20}")
//  private int maxPoolSize;
//
//  @Value("${spring.datasource.hikari.minimum-idle:5}")
//  private int minIdle;
//
//  @Value("${spring.datasource.hikari.connection-timeout:30000}")
//  private int connectionTimeout;
//
//  @Bean
//  public DataSource dataSource() {
//    HikariDataSource dataSource = new HikariDataSource();
//    dataSource.setJdbcUrl(dbUrl);
//    dataSource.setUsername(dbUsername);
//    dataSource.setPassword(dbPassword);
//    dataSource.setMaximumPoolSize(maxPoolSize);
//    dataSource.setMinimumIdle(minIdle);
//    dataSource.setConnectionTimeout(connectionTimeout);
//    dataSource.setPoolName("WalletServicePool");
//    dataSource.setAutoCommit(false);
//
//    dataSource.addDataSourceProperty("reWriteBatchedInserts", "true");
//    dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
//    dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
//
//    return dataSource;
//  }
//
//  @Bean
//  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
//    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
//    em.setDataSource(dataSource());
//    em.setPackagesToScan("ru.kuzmich.walletservice.model");
//
//    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
//    em.setJpaVendorAdapter(vendorAdapter);
//
//    Properties properties = new Properties();
//    properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
//    properties.put("hibernate.jdbc.batch_size", "20");
//    properties.put("hibernate.order_inserts", "true");
//    properties.put("hibernate.order_updates", "true");
//    properties.put("hibernate.generate_statistics", "true");
//    properties.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
//    properties.put("hibernate.connection.provider_disables_autocommit", "true");
//
//    em.setJpaProperties(properties);
//    return em;
//  }
//
//  @Bean
//  public PlatformTransactionManager transactionManager() {
//    JpaTransactionManager transactionManager = new JpaTransactionManager();
//    transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
//    return transactionManager;
//  }
//}
