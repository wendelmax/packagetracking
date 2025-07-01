-- Script de inicialização para MySQL Master
-- Apenas criação de tabela e dados de teste

USE packagetracking;
CREATE TABLE IF NOT EXISTS teste_replicacao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mensagem VARCHAR(100),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO teste_replicacao (mensagem) VALUES ('Primeiro registro de teste');

-- Mostrar status do binlog
SHOW MASTER STATUS; 