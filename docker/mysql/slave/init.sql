-- Script de inicialização para MySQL Slave
-- Apenas configuração de replicação

-- Aguardar um pouco para o master estar pronto
SELECT SLEEP(30);

-- Configurar replicação com posição inicial
CHANGE MASTER TO
    MASTER_HOST='mysql1',
    MASTER_PORT=3306,
    MASTER_USER='replicator',
    MASTER_PASSWORD='replicator',
    MASTER_LOG_FILE='mysql-bin.000001',
    MASTER_LOG_POS=0,
    MASTER_SSL=0,
    GET_MASTER_PUBLIC_KEY=1;

-- Iniciar replicação
START SLAVE;

-- Verificar status da replicação
SHOW SLAVE STATUS\G 