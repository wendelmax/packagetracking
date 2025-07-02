package com.packagetracking.query.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

@Aspect
@Component
@Slf4j
public class DatabaseFailoverInterceptor {

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object handleDatabaseFailover(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (DataAccessException | SQLException e) {
            log.warn("Erro de acesso ao banco detectado: {}", e.getMessage());
            
            if (DatabaseRoutingConfig.DatabaseRoutingDataSource.getDatabaseType() == 
                DatabaseRoutingConfig.DatabaseType.SLAVE) {
                
                log.info("Fazendo failover do slave para master");
                DatabaseRoutingConfig.DatabaseRoutingDataSource.setDatabaseType(
                    DatabaseRoutingConfig.DatabaseType.MASTER
                );
                
                try {
                    return joinPoint.proceed();
                } catch (Exception retryException) {
                    log.error("Erro persistente mesmo após failover: {}", retryException.getMessage());
                    throw retryException;
                }
            }
            
            throw e;
        }
    }
} 