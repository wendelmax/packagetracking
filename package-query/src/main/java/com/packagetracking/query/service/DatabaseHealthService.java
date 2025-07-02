package com.packagetracking.query.service;

import com.packagetracking.query.config.DatabaseRoutingConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class DatabaseHealthService {

    @Autowired
    private DataSource slaveDataSource;

    @Autowired
    private DataSource masterDataSource;

    private final AtomicBoolean slaveHealthy = new AtomicBoolean(true);
    private final AtomicBoolean masterHealthy = new AtomicBoolean(true);

    @Scheduled(fixedRate = 30000)
    public void checkDatabaseHealth() {
        checkSlaveHealth();
        checkMasterHealth();
        
        if (slaveHealthy.get() && 
            DatabaseRoutingConfig.DatabaseRoutingDataSource.getDatabaseType() == 
            DatabaseRoutingConfig.DatabaseType.MASTER) {
            
            log.info("Slave recuperado, voltando para uso normal");
            DatabaseRoutingConfig.DatabaseRoutingDataSource.setDatabaseType(
                DatabaseRoutingConfig.DatabaseType.SLAVE
            );
        }
    }

    private void checkSlaveHealth() {
        try (Connection connection = slaveDataSource.getConnection()) {
            connection.createStatement().execute("SELECT 1");
            if (!slaveHealthy.get()) {
                log.info("Slave voltou a funcionar");
                slaveHealthy.set(true);
            }
        } catch (SQLException e) {
            if (slaveHealthy.get()) {
                log.warn("Slave ficou indisponível: {}", e.getMessage());
                slaveHealthy.set(false);
                
                if (DatabaseRoutingConfig.DatabaseRoutingDataSource.getDatabaseType() == 
                    DatabaseRoutingConfig.DatabaseType.SLAVE) {
                    
                    log.info("Fazendo failover automático para master");
                    DatabaseRoutingConfig.DatabaseRoutingDataSource.setDatabaseType(
                        DatabaseRoutingConfig.DatabaseType.MASTER
                    );
                }
            }
        }
    }

    private void checkMasterHealth() {
        try (Connection connection = masterDataSource.getConnection()) {
            connection.createStatement().execute("SELECT 1");
            if (!masterHealthy.get()) {
                log.info("Master voltou a funcionar");
                masterHealthy.set(true);
            }
        } catch (SQLException e) {
            if (masterHealthy.get()) {
                log.error("Master ficou indisponível: {}", e.getMessage());
                masterHealthy.set(false);
            }
        }
    }

    public boolean isSlaveHealthy() {
        return slaveHealthy.get();
    }

    public boolean isMasterHealthy() {
        return masterHealthy.get();
    }

    public String getCurrentDatabase() {
        return DatabaseRoutingConfig.DatabaseRoutingDataSource.getDatabaseType().name();
    }

    public void forceFailoverToMaster() {
        log.info("Forçando failover para master");
        DatabaseRoutingConfig.DatabaseRoutingDataSource.setDatabaseType(
            DatabaseRoutingConfig.DatabaseType.MASTER
        );
    }

    public void switchToSlave() {
        if (slaveHealthy.get()) {
            log.info("Alterando para slave");
            DatabaseRoutingConfig.DatabaseRoutingDataSource.setDatabaseType(
                DatabaseRoutingConfig.DatabaseType.SLAVE
            );
        } else {
            log.warn("Não é possível alterar para slave - não está saudável");
        }
    }
} 