-- Script de inicialização para MySQL Master
-- Apenas criação de tabela e dados de teste

USE packagetracking;
CREATE TABLE IF NOT EXISTS teste_replicacao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mensagem VARCHAR(100),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO teste_replicacao (mensagem) VALUES ('Primeiro registro de teste');

-- Ajuste do campo id para VARCHAR(32) e criação de índice parcial para tracking_events
SET @alter_id := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'packagetracking' AND table_name = 'tracking_events' AND column_name = 'id' AND data_type = 'varchar' AND character_maximum_length = 32) = 0,
    'ALTER TABLE tracking_events MODIFY COLUMN id VARCHAR(32) NOT NULL',
    'SELECT "Campo id já está como VARCHAR(32)"'
));
PREPARE alter_id_stmt FROM @alter_id;
EXECUTE alter_id_stmt;
DEALLOCATE PREPARE alter_id_stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = 'packagetracking' AND table_name = 'tracking_events' AND index_name = 'idx_tracking_id_prefix') = 0,
    'CREATE INDEX idx_tracking_id_prefix ON tracking_events (id(8))',
    'SELECT "Índice já existe"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Mostrar status do binlog
SHOW MASTER STATUS; 