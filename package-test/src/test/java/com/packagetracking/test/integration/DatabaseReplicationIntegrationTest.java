package com.packagetracking.test.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração para replicação MySQL Master-Slave
 * Substitui o script test-replication.sh
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DatabaseReplicationIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysqlMaster = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("packagetracking")
        .withUsername("app_write")
        .withPassword("app_write")
        .withInitScript("init-master.sql")
        .withCommand("--server-id=1", "--log-bin=mysql-bin", "--binlog-format=ROW");

    @Container
    private static final MySQLContainer<?> mysqlSlave = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("packagetracking")
        .withUsername("app_read")
        .withPassword("app_read")
        .withInitScript("init-slave.sql")
        .withCommand("--server-id=2", "--relay-log=relay-bin");

    private Connection masterConnection;
    private Connection slaveConnection;

    @BeforeEach
    void setUp() throws Exception {
        await().atMost(60, TimeUnit.SECONDS)
            .until(() -> mysqlMaster.isRunning() && mysqlSlave.isRunning());

        masterConnection = DriverManager.getConnection(
            mysqlMaster.getJdbcUrl(),
            mysqlMaster.getUsername(),
            mysqlMaster.getPassword()
        );

        slaveConnection = DriverManager.getConnection(
            mysqlSlave.getJdbcUrl(),
            mysqlSlave.getUsername(),
            mysqlSlave.getPassword()
        );

        setupReplication();
    }

    @Test
    void testDatabaseReplication() throws Exception {
        System.out.println("=== Testando Replicação MySQL Master-Slave ===");

        System.out.println("1. Testando conexão com Master...");
        testMasterConnection();

        System.out.println("2. Testando conexão com Slave...");
        testSlaveConnection();

        System.out.println("3. Verificando status da replicação...");
        verifyReplicationStatus();

        System.out.println("4. Testando escrita no Master...");
        String testMessage = "Teste de replicação - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        insertDataInMaster(testMessage);

        System.out.println("5. Aguardando replicação...");
        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> verifyDataInSlave(testMessage));

        System.out.println("6. Verificando dados no Slave...");
        verifyDataInSlave(testMessage);

        System.out.println("=== Teste de replicação concluído com sucesso! ===");
    }

    private void testMasterConnection() throws Exception {
        try (Statement stmt = masterConnection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 'Master OK' as status");
            assertTrue(rs.next());
            assertEquals("Master OK", rs.getString("status"));
            System.out.println("SUCCESS: Master está acessível");
        } catch (Exception e) {
            System.out.println("ERROR: Master não está acessível - " + e.getMessage());
            throw e;
        }
    }

    private void testSlaveConnection() throws Exception {
        try (Statement stmt = slaveConnection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 'Slave OK' as status");
            assertTrue(rs.next());
            assertEquals("Slave OK", rs.getString("status"));
            System.out.println("SUCCESS: Slave está acessível");
        } catch (Exception e) {
            System.out.println("ERROR: Slave não está acessível - " + e.getMessage());
            throw e;
        }
    }

    private void verifyReplicationStatus() throws Exception {
        try (Statement stmt = slaveConnection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS");
            
            if (rs.next()) {
                String ioRunning = rs.getString("Slave_IO_Running");
                String sqlRunning = rs.getString("Slave_SQL_Running");
                String lastError = rs.getString("Last_Error");
                
                System.out.println("  - Slave_IO_Running: " + ioRunning);
                System.out.println("  - Slave_SQL_Running: " + sqlRunning);
                
                if (lastError != null && !lastError.isEmpty()) {
                    System.out.println("  - Last_Error: " + lastError);
                }
                
                if ("Yes".equals(ioRunning) && "Yes".equals(sqlRunning)) {
                    System.out.println("SUCCESS: Replicação está funcionando corretamente");
                } else {
                    System.out.println("WARNING: Replicação pode não estar funcionando corretamente");
                }
            } else {
                System.out.println("WARNING: Replicação não configurada ou não encontrada");
            }
        } catch (Exception e) {
            System.out.println("ERROR: Erro ao verificar status da replicação: " + e.getMessage());
        }
    }

    private void insertDataInMaster(String message) throws Exception {
        try (Statement stmt = masterConnection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS teste_replicacao (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    mensagem VARCHAR(255) NOT NULL,
                    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            String insertSql = "INSERT INTO teste_replicacao (mensagem) VALUES ('" + message + "')";
            int rowsAffected = stmt.executeUpdate(insertSql);
            
            assertEquals(1, rowsAffected);
            System.out.println("SUCCESS: Dados inseridos no Master: " + rowsAffected + " linha(s)");
            
        } catch (Exception e) {
            System.out.println("ERROR: Não foi possível inserir dados no Master - " + e.getMessage());
            throw e;
        }
    }

    private boolean verifyDataInSlave(String expectedMessage) {
        try (Statement stmt = slaveConnection.createStatement()) {
            ResultSet rs = stmt.executeQuery("""
                SELECT * FROM teste_replicacao 
                WHERE mensagem = '%s' 
                ORDER BY criado_em DESC 
                LIMIT 5
                """.formatted(expectedMessage));
            
            boolean found = rs.next();
            if (found) {
                System.out.println("SUCCESS: Dados encontrados no Slave: " + rs.getString("mensagem"));
            }
            return found;
        } catch (Exception e) {
            System.out.println("ERROR: Não foi possível ler dados do Slave - " + e.getMessage());
            return false;
        }
    }

    private void setupReplication() throws Exception {
        System.out.println("Configurando replicação Master-Slave...");
        
        String masterHost = mysqlMaster.getHost();
        int masterPort = mysqlMaster.getMappedPort(3306);
        String masterUser = mysqlMaster.getUsername();
        String masterPassword = mysqlMaster.getPassword();
        
        try (Statement stmt = slaveConnection.createStatement()) {
            stmt.execute("STOP SLAVE");
            
            String changeMasterSql = String.format("""
                CHANGE MASTER TO 
                MASTER_HOST='%s',
                MASTER_PORT=%d,
                MASTER_USER='%s',
                MASTER_PASSWORD='%s',
                MASTER_LOG_FILE='mysql-bin.000001',
                MASTER_LOG_POS=4
                """, masterHost, masterPort, masterUser, masterPassword);
            
            stmt.execute(changeMasterSql);
            
            stmt.execute("START SLAVE");
            
            System.out.println("SUCCESS: Replicação configurada");
        } catch (Exception e) {
            System.out.println("WARNING: Erro ao configurar replicação: " + e.getMessage());
        }
    }
} 