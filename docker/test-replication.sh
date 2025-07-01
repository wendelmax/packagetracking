#!/bin/bash

echo "=== Testando Replicação MySQL Master-Slave ==="

echo "Aguardando containers..."
sleep 30

echo "Testando conexão com Master (mysql1)..."
docker exec mysql1 mysql -uapp_write -papp_write -e "SELECT 'Master OK' as status;" 2>/dev/null || echo "Erro: Master não está acessível"

echo "Testando conexão com Slave (mysql2)..."
docker exec mysql2 mysql -uapp_read -papp_read -e "SELECT 'Slave OK' as status;" 2>/dev/null || echo "Erro: Slave não está acessível"

echo "Verificando status da replicação..."
docker exec mysql2 mysql -e "SHOW SLAVE STATUS\G" 2>/dev/null | grep -E "(Slave_IO_Running|Slave_SQL_Running|Last_Error)" || echo "Replicação não configurada"

echo "Testando escrita no Master..."
docker exec mysql1 mysql -uapp_write -papp_write -e "
USE packagetracking;
INSERT INTO teste_replicacao (mensagem) VALUES ('Teste de replicação - $(date)');
SELECT 'Dados inseridos no Master' as result;
" 2>/dev/null || echo "Erro: Não foi possível inserir dados no Master"

echo "Aguardando replicação..."
sleep 5

echo "Verificando dados no Slave..."
docker exec mysql2 mysql -uapp_read -papp_read -e "
USE packagetracking;
SELECT * FROM teste_replicacao ORDER BY criado_em DESC LIMIT 5;
" 2>/dev/null || echo "Erro: Não foi possível ler dados do Slave"

echo "=== Teste concluído ===" 