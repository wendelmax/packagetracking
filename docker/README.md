# MySQL Master-Slave com Docker

Configuração de MySQL com replicação master-slave usando Docker.

## Como rodar

### Opção 1: Script automático (recomendado)
```bash
# Na raiz do projeto
./build-and-run.sh
```

### Opção 2: Manual

#### 1. Subir os containers
```bash
cd docker
docker compose up -d
```

#### 2. Aguardar inicialização
Espere uns 60-90 segundos para os containers inicializarem.

#### 3. Criar usuários (obrigatório)
```bash
# Usuário para replicação
docker exec mysql1 mysql -uroot -proot -e "CREATE USER 'replicator'@'%' IDENTIFIED WITH mysql_native_password BY 'replicator'; GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%'; FLUSH PRIVILEGES;"

# Usuário para escrita
docker exec mysql1 mysql -uroot -proot -e "CREATE USER 'app_write'@'%' IDENTIFIED WITH mysql_native_password BY 'app_write'; GRANT ALL PRIVILEGES ON packagetracking.* TO 'app_write'@'%'; FLUSH PRIVILEGES;"

# Usuário para leitura
docker exec mysql1 mysql -uroot -proot -e "CREATE USER 'app_read'@'%' IDENTIFIED WITH mysql_native_password BY 'app_read'; GRANT SELECT ON packagetracking.* TO 'app_read'@'%'; FLUSH PRIVILEGES;"
```

#### 4. Testar
```bash
cd docker
./test-replication.sh
```

## O que cada container faz

### Infraestrutura
- **mysql1 (porta 3306)**: Master - para escrita
- **mysql2 (porta 3307)**: Slave - para leitura  
- **rabbitmq (porta 5672)**: Message broker

### Aplicação Spring Boot
- **package-service (porta 8080)**: Serviço principal com JPA ativado
- **event-producer (porta 8081)**: Serviço apenas para produção de eventos
- **event-consumer (porta 8082)**: Serviço apenas para consumo de eventos
- **package-query (porta 8083)**: Serviço de consultas (CQRS)

## URLs dos serviços

- **Package Service**: http://localhost:8080
- **Event Producer**: http://localhost:8081
- **Event Consumer**: http://localhost:8082
- **Package Query**: http://localhost:8083
- **RabbitMQ Management**: http://localhost:15672

## Usuários criados

- `root/root` - Administrador
- `replicator/replicator` - Para replicação
- `app_write/app_write` - Para escrita na aplicação
- `app_read/app_read` - Para leitura na aplicação

## Comandos úteis

```bash
# Ver status dos containers
docker ps

# Ver status da replicação
docker exec mysql2 mysql -e "SHOW SLAVE STATUS\G"

# Ver logs da aplicação
docker logs package-service
docker logs event-producer
docker logs event-consumer
docker logs package-query

# Parar tudo
cd docker
docker compose down

# Parar e apagar dados
cd docker
docker compose down -v

# Ver logs
docker logs mysql1
docker logs mysql2

# Build e deploy completo (na raiz)
./build-and-run.sh
```

## Problemas comuns

**Erro de autenticação**: Verifique se os usuários foram criados com `mysql_native_password`

**Slave não conecta**: Verifique se o usuário `replicator` existe no master

**Dados não replicam**: Verifique se a replicação está ativa com `SHOW SLAVE STATUS`

**Aplicação não inicia**: Verifique se o MySQL e RabbitMQ estão rodando

## Configuração na aplicação

Para o Spring Boot, use:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/packagetracking
    username: app_write
    password: app_write
```

Para leitura no slave:
```yaml
slave:
  datasource:
    url: jdbc:mysql://localhost:3307/packagetracking
    username: app_read
    password: app_read
```

## Perfis da aplicação

- **docker**: Serviço completo (JPA + Producer + Consumer)
- **producer**: Apenas produção de eventos
- **consumer**: Apenas consumo de eventos
- **query**: Serviço de consultas (CQRS)

## Arquivos importantes

- `docker-compose.yml` - Configuração dos containers
- `mysql/master/my.cnf` - Configuração do master
- `mysql/slave/my.cnf` - Configuração do slave
- `mysql/master/init.sql` - Script de inicialização do master
- `mysql/slave/init.sql` - Script de inicialização do slave
- `test-replication.sh` - Script para testar se está funcionando
- `build-and-run.sh` - Script para build e deploy completo (na raiz)
- `Dockerfile` - Build multi-stage da aplicação Spring Boot (na raiz)
- `.dockerignore` - Arquivos ignorados no build do Docker

## Sobre o Dockerfile

O Dockerfile usa multi-stage build para otimizar o tamanho da imagem:

1. **Build stage**: Usa Maven para compilar os módulos package-command e package-query
2. **Runtime stage**: Usa apenas JRE para executar os JARs compilados

Isso resulta em uma imagem final menor e mais segura. 