#!/bin/bash

echo "=== Build e Deploy do Sistema de Rastreamento ==="

echo "Parando containers existentes..."
cd docker
docker compose down
cd ..

echo "Fazendo build das imagens..."
cd docker
docker compose build
cd ..

echo "Subindo containers..."
cd docker
docker compose up -d
cd ..

echo "Aguardando inicialização dos serviços..."
sleep 90

echo "Criando usuários MySQL..."
docker exec mysql1 mysql -uroot -proot -e "CREATE USER IF NOT EXISTS 'replicator'@'%' IDENTIFIED WITH mysql_native_password BY 'replicator'; GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%'; FLUSH PRIVILEGES;" 2>/dev/null
docker exec mysql1 mysql -uroot -proot -e "CREATE USER IF NOT EXISTS 'app_write'@'%' IDENTIFIED WITH mysql_native_password BY 'app_write'; GRANT ALL PRIVILEGES ON packagetracking.* TO 'app_write'@'%'; FLUSH PRIVILEGES;" 2>/dev/null
docker exec mysql1 mysql -uroot -proot -e "CREATE USER IF NOT EXISTS 'app_read'@'%' IDENTIFIED WITH mysql_native_password BY 'app_read'; GRANT SELECT ON packagetracking.* TO 'app_read'@'%'; FLUSH PRIVILEGES;" 2>/dev/null

echo "Testando replicação MySQL..."
cd docker
./test-replication.sh
cd ..

echo "Verificando status dos serviços..."
docker ps

echo "=== Deploy concluído ==="
echo "Serviços disponíveis:"
echo "- Package Service: http://localhost:8080"
echo "- Event Producer: http://localhost:8081"
echo "- Event Consumer: http://localhost:8082"
echo "- Package Query: http://localhost:8083"
echo "- RabbitMQ Management: http://localhost:15672" 