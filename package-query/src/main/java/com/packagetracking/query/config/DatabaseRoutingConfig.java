package com.packagetracking.query.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "spring.datasource.slave.url")
@Slf4j
public class DatabaseRoutingConfig {

    @Value("${spring.datasource.slave.url}")
    private String slaveUrl;

    @Value("${spring.datasource.slave.username}")
    private String slaveUsername;

    @Value("${spring.datasource.slave.password}")
    private String slavePassword;

    @Value("${spring.datasource.master.url}")
    private String masterUrl;

    @Value("${spring.datasource.master.username}")
    private String masterUsername;

    @Value("${spring.datasource.master.password}")
    private String masterPassword;

    @Bean
    public DataSource slaveDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(slaveUrl);
        dataSource.setUsername(slaveUsername);
        dataSource.setPassword(slavePassword);
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        dataSource.setMaximumPoolSize(150);
        dataSource.setMinimumIdle(30);
        dataSource.setConnectionTimeout(5000);
        dataSource.setIdleTimeout(300000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setLeakDetectionThreshold(20000);
        dataSource.setValidationTimeout(2000);
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setConnectionInitSql("SELECT 1");
        dataSource.setKeepaliveTime(30000);
        dataSource.setPoolName("HikariPool-slave");
        dataSource.setAutoCommit(true);
        
        return dataSource;
    }

    @Bean
    public DataSource masterDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(masterUrl);
        dataSource.setUsername(masterUsername);
        dataSource.setPassword(masterPassword);
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        dataSource.setMaximumPoolSize(50);
        dataSource.setMinimumIdle(10);
        dataSource.setConnectionTimeout(3000);
        dataSource.setIdleTimeout(300000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setLeakDetectionThreshold(15000);
        dataSource.setValidationTimeout(1000);
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setConnectionInitSql("SELECT 1");
        dataSource.setKeepaliveTime(30000);
        dataSource.setPoolName("HikariPool-master-fallback");
        dataSource.setAutoCommit(true);
        
        return dataSource;
    }

    @Bean
    public AbstractRoutingDataSource routingDataSource() {
        DatabaseRoutingDataSource routingDataSource = new DatabaseRoutingDataSource();
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DatabaseType.SLAVE, slaveDataSource());
        targetDataSources.put(DatabaseType.MASTER, masterDataSource());
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(slaveDataSource());
        
        return routingDataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return new LazyConnectionDataSourceProxy(routingDataSource());
    }

    public enum DatabaseType {
        SLAVE, MASTER
    }

    public static class DatabaseRoutingDataSource extends AbstractRoutingDataSource {
        
        private static final ThreadLocal<DatabaseType> contextHolder = new ThreadLocal<>();
        
        @Override
        protected Object determineCurrentLookupKey() {
            DatabaseType databaseType = contextHolder.get();
            if (databaseType == null) {
                databaseType = DatabaseType.SLAVE;
                contextHolder.set(databaseType);
            }
            
            log.debug("Usando datasource: {}", databaseType);
            return databaseType;
        }
        
        public static void setDatabaseType(DatabaseType databaseType) {
            contextHolder.set(databaseType);
            log.info("Alterando para datasource: {}", databaseType);
        }
        
        public static DatabaseType getDatabaseType() {
            return contextHolder.get();
        }
        
        public static void clearDatabaseType() {
            contextHolder.remove();
        }
        
        @Override
        protected DataSource determineTargetDataSource() {
            DataSource dataSource = super.determineTargetDataSource();
            
            if (getDatabaseType() == DatabaseType.SLAVE) {
                try {
                    dataSource.getConnection().close();
                } catch (Exception e) {
                    log.warn("Slave não disponível, fazendo failover para master: {}", e.getMessage());
                    setDatabaseType(DatabaseType.MASTER);
                    dataSource = super.determineTargetDataSource();
                }
            }
            
            return dataSource;
        }
    }
} 