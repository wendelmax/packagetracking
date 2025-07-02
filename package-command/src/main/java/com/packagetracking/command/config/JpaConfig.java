package com.packagetracking.command.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "com.packagetracking.command.repository")
@EnableTransactionManagement
@ConditionalOnProperty(name = "app.resources.persistence", havingValue = "true")
@Slf4j
public class JpaConfig implements CommandLineRunner {
    
    @Autowired
    private DataSource dataSource;
    
    public JpaConfig() {
        log.info("Configuração JPA habilitada");
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Testa a conexão na inicialização
        try (var connection = dataSource.getConnection()) {
            log.info("Conexão com banco de dados estabelecida com sucesso");
        } catch (Exception e) {
            log.error("Erro ao conectar com banco de dados: {}", e.getMessage());
            // Não falha a aplicação, apenas loga o erro
        }
    }
} 