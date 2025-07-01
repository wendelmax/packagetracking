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

### Variáveis de Ambiente

#### Package Service (8080)
```bash
SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_URL=jdbc:mysql://mysql1:3306/packagetracking
SPRING_DATASOURCE_USERNAME=app_write
SPRING_DATASOURCE_PASSWORD=app_write
```

#### Event Producer (8081)
```bash
SPRING_PROFILES_ACTIVE=producer
SPRING_DATASOURCE_URL=jdbc:mysql://mysql1:3306/packagetracking
SPRING_DATASOURCE_USERNAME=app_write
SPRING_DATASOURCE_PASSWORD=app_write
```

#### Event Consumer (8082)
```bash
SPRING_PROFILES_ACTIVE=consumer
SPRING_DATASOURCE_URL=jdbc:mysql://mysql2:3306/packagetracking
SPRING_DATASOURCE_USERNAME=app_read
SPRING_DATASOURCE_PASSWORD=app_read
```

#### Package Query (8083)
```bash
SPRING_PROFILES_ACTIVE=query
SPRING_DATASOURCE_URL=jdbc:mysql://mysql2:3306/packagetracking
SPRING_DATASOURCE_USERNAME=app_read
SPRING_DATASOURCE_PASSWORD=app_read
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

### Perfis Spring Boot
- `docker`: Desenvolvimento com Docker
- `producer`: Apenas produção de eventos
- `consumer`: Apenas consumo de eventos
- `query`: Apenas consultas
