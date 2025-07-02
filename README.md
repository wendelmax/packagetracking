# Sistema de Rastreamento de Pacotes - CQRS

Sistema de rastreamento de pacotes em alta escala baseado em arquitetura CQRS (Command Query Responsibility Segregation) com MySQL master-slave e RabbitMQ.

## Arquitetura

O sistema é composto pelos seguintes serviços:

### 1. **Package Service** (Porta 8080)
- Serviço completo com JPA para operações de escrita
- Criação, atualização e cancelamento de pacotes
- Integração com APIs externas (verificação de feriados, fatos curiosos)
- Banco de dados MySQL master para escritas

### 2. **Event Producer** (Porta 8081)
- Apenas produção de eventos via RabbitMQ
- Processamento assíncrono de alta carga
- Banco de dados MySQL master para escritas

### 3. **Event Consumer** (Porta 8082)
- Apenas consumo de eventos via RabbitMQ
- Processamento assíncrono de eventos de tracking
- Banco de dados MySQL slave para leituras

### 4. **Package Query** (Porta 8083)
- Apenas consultas de pacotes e eventos
- Banco de dados MySQL slave para leituras
- Otimizado para consultas rápidas

## Infraestrutura

### Banco de Dados MySQL Master-Slave
- **Master (mysql1:3306)**: Todas as escritas
- **Slave (mysql2:3307)**: Todas as leituras
- **Replicação**: Automática via binlog

### RabbitMQ
- Mensageria entre serviços
- Processamento assíncrono
- Management UI na porta 15672

## Pré-requisitos

- Java 21
- Maven 3.8+
- Docker e Docker Compose

## Executando a Aplicação

### Opção 1: Docker Compose (Recomendado)

```bash
# Na raiz do projeto
cd docker
docker-compose up -d
```

### Opção 2: Build e Run Manual

```bash
# Build do projeto
./build-and-run.sh

# Ou manualmente:
mvn clean install
cd docker
docker-compose up -d
```

### Verificar se tudo está rodando

```bash
# Verificar containers
docker ps

# Verificar logs
docker logs package-service
docker logs event-producer
docker logs event-consumer
docker logs package-query
```

## Documentação da API (Swagger/OpenAPI)

### Endpoints do Swagger por Instância

#### Package Command (Porta 8080)
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs
- **Descrição**: APIs para criação, atualização e cancelamento de pacotes

#### Event Ingestion (Porta 8081)
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **API Docs**: http://localhost:8081/api-docs
- **Descrição**: APIs para recebimento de eventos de rastreamento

#### Event Consumer (Porta 8082)
- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **API Docs**: http://localhost:8082/api-docs
- **Descrição**: APIs para processamento de eventos de rastreamento

#### Package Query (Porta 8083)
- **Swagger UI**: http://localhost:8083/swagger-ui.html
- **API Docs**: http://localhost:8083/api-docs
- **Descrição**: APIs para consulta de pacotes e eventos de rastreamento

## Endpoints da API

### Package Service (http://localhost:8080)

#### Criar Pacote
```bash
POST /api/v1/packages
Content-Type: application/json

{
  "trackingCode": "PKG123456789",
  "description": "Livros para entrega",
  "sender": "Loja ABC",
  "recipient": "João Silva",
  "estimatedDeliveryDate": "2025-01-24"
}
```

#### Atualizar Pacote
```bash
PUT /api/v1/packages/{id}
Content-Type: application/json

{
  "description": "Livros atualizados",
  "estimatedDeliveryDate": "2025-01-25"
}
```

#### Cancelar Pacote
```bash
DELETE /api/v1/packages/{id}
```

#### Criar Evento de Tracking
```bash
POST /api/v1/tracking-events
Content-Type: application/json

{
  "packageId": 1,
  "eventType": "IN_TRANSIT",
  "description": "Pacote em trânsito",
  "location": "Centro de Distribuição São Paulo"
}
```

### Package Query (http://localhost:8083)

#### Consultar Pacote
```bash
GET /api/v1/packages/{id}
```

#### Listar Pacotes
```bash
GET /api/v1/packages?page=0&size=20
```

#### Consultar Eventos de um Pacote
```bash
GET /api/v1/packages/{id}/events
```

## Monitoramento

### Health Checks
- http://localhost:8080/actuator/health (Package Service)
- http://localhost:8081/actuator/health (Event Producer)
- http://localhost:8082/actuator/health (Event Consumer)
- http://localhost:8083/actuator/health (Package Query)

### Métricas
- http://localhost:8080/actuator/metrics
- http://localhost:8081/actuator/metrics
- http://localhost:8082/actuator/metrics
- http://localhost:8083/actuator/metrics

### RabbitMQ Management
- http://localhost:15672 (guest/guest)

## Estratégias de Escalabilidade

### 1. **Banco de Dados**
- MySQL master-slave para separação de leitura/escrita
- Pool de conexões HikariCP configurado
- Índices otimizados para consultas frequentes

### 2. **Processamento Assíncrono**
- RabbitMQ para mensageria entre serviços
- Separação de responsabilidades (producer/consumer)
- Processamento independente de eventos

### 3. **Arquitetura CQRS**
- Separação clara entre comandos e consultas
- Otimizações específicas para cada tipo de operação
- Escalabilidade independente

### 4. **Monitoramento**
- Spring Boot Actuator para health checks e métricas
- Logs estruturados
- RabbitMQ Management para monitoramento de filas

## Configuração de Ambiente

### Abordagem Balanceada de Configuração

O projeto utiliza uma abordagem balanceada com um único `application.yml` por módulo, permitindo sobrescrever valores através de variáveis de ambiente.

#### Principais Variáveis de Ambiente

```bash
# Configurações do Servidor
SERVER_PORT=8080                    # package-command
SERVER_PORT=8083                    # package-query

# Configurações do Banco de Dados
SPRING_DATASOURCE_URL=jdbc:mysql://mysql1:3306/packagetracking
SPRING_DATASOURCE_USERNAME=app_write
SPRING_DATASOURCE_PASSWORD=app_write

# Configurações do Redis (package-query)
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379

# Configurações do RabbitMQ (package-command)
RABBIT_MQ_HOST=rabbitmq
RABBIT_MQ_PORT=5672
RABBIT_MQ_ADDRESS=amqp://rabbitmq:5672/packagetracking

# Configurações de Recursos (package-command)
PERSISTENCE_ENABLED=true
QUEUES_ENABLED=false
ENDPOINTS_TYPE=package
```



## Desenvolvimento

### Estrutura do Projeto
```
packagetracking/
├── package-command/          # Módulo de comandos (escritas)
├── package-query/           # Módulo de consultas (leituras)
├── docker/                  # Configurações Docker
├── Dockerfile              # Multi-stage build
└── build-and-run.sh        # Script de build e deploy
```

### Configuração Balanceada

O projeto utiliza uma abordagem balanceada com um único `application.yml` por módulo:

#### Módulo Command (Package Service)
- Configuração única com variáveis de ambiente
- Controle de recursos via variáveis:
  - `PERSISTENCE_ENABLED`: Habilita persistência no banco de dados
  - `QUEUES_ENABLED`: Habilita processamento de filas RabbitMQ
  - `ENDPOINTS_TYPE`: Controla endpoints REST (package/events/none)

#### Regras de Configuração por Container:
- **package-ingestion**: `PERSISTENCE_ENABLED=true`, `QUEUES_ENABLED=false`, `ENDPOINTS_TYPE=package`
- **event-ingestion**: `PERSISTENCE_ENABLED=false`, `QUEUES_ENABLED=true`, `ENDPOINTS_TYPE=events` (Producer)
- **event-consumer**: `PERSISTENCE_ENABLED=true`, `QUEUES_ENABLED=true`, `ENDPOINTS_TYPE=none` (Consumer)

**Regras Importantes:**
- **Producer e Consumer são mutuamente exclusivos**: Não podem estar habilitados simultaneamente
- **Producer**: Habilitado apenas quando `endpoints=events`
- **Consumer**: Habilitado apenas quando `endpoints=none`

#### Módulo Query (Package Query)
- Configuração única com variáveis de ambiente
- Configurações de cache e failover via variáveis
- Otimizações automáticas baseadas no ambiente

### Configuração de Failover (Módulo Query)

O módulo query implementa **failover automático** entre MySQL Master e Slave:

#### Endpoints de Monitoramento de Failover

```bash
# Status geral dos bancos
GET http://localhost:8083/api/database/health

# Status detalhado
GET http://localhost:8083/api/database/status

# Forçar failover para master
POST http://localhost:8083/api/database/failover/master

# Voltar para slave (se saudável)
POST http://localhost:8083/api/database/switch/slave
```

#### Funcionalidades de Failover

- ✅ **Detecção Automática**: Monitoramento contínuo da saúde dos bancos
- ✅ **Failover Transparente**: Mudança automática quando slave falha
- ✅ **Recuperação Automática**: Retorno ao slave quando recupera
- ✅ **Controle Manual**: Endpoints para gerenciamento manual
- ✅ **Logs Detalhados**: Rastreamento de todas as mudanças

#### Configuração de Usuários MySQL

O script `build-and-run.sh` cria automaticamente os usuários necessários:

```bash
# Master (mysql1) - Escritas
app_write: Todas as operações
app_read: Apenas leituras (fallback)

# Slave (mysql2) - Leituras
app_read: Apenas leituras (primário)
```

#### Testando o Failover

```bash
# 1. Verificar status inicial
curl http://localhost:8083/api/database/health

# 2. Simular falha do slave
docker stop mysql2

# 3. Verificar failover automático
curl http://localhost:8083/api/database/health

# 4. Recuperar o slave
docker start mysql2

# 5. Verificar retorno automático
curl http://localhost:8083/api/database/health
```

## Testes de Performance

Para executar testes de performance completos:

```bash
cd performance-tests
./run_performance_test.sh
```

O script inclui:
- Geração automática de dados de teste
- Execução de testes de carga com k6
- Coleta de métricas via Spring Boot Actuator
- Geração de relatório HTML completo
- **Uso automático de configurações** via variáveis de ambiente

### Configurações de Performance

O script usa automaticamente configurações otimizadas via variáveis de ambiente:
- **Pool de conexões**: Configurado via `HIKARI_MAXIMUM_POOL_SIZE`
- **Threads**: Configurado via `TOMCAT_THREADS_MAX`
- **Timeout**: Configurado via `SERVER_CONNECTION_TIMEOUT`
- **Virtual Threads**: Configurado via `JAVA_VIRTUAL_THREADS_MAX_COUNT`
- **Cache**: Configurações otimizadas para Redis
- **Circuit Breaker**: Configurações para alta disponibilidade

### Docker Compose

O script usa o `docker-compose.yml` padrão que:
- Configura todos os serviços com variáveis de ambiente otimizadas
- Reconstrói as imagens com `--build` para garantir configurações corretas
- Otimiza para performance com configurações específicas
- Garante consistência entre todos os containers

### Limpeza de Containers

```bash
# Limpar containers de performance
cd docker
docker compose -f docker-compose.performance.yml down --volumes --remove-orphans
docker rmi $(docker images -q packagetracking_* 2>/dev/null) 2>/dev/null || true
```
