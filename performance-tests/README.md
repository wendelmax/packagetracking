# 🚀 Testes de Performance - Package Tracking

Este diretório contém scripts e ferramentas para executar testes de performance completos na aplicação de rastreamento de pacotes.

## 📋 Pré-requisitos

- Docker e Docker Compose rodando
- Node.js 16+ e npm
- k6 (será instalado automaticamente se necessário)
- curl

## 🏗️ Arquitetura de Teste

```
performance-tests/
├── data_generator.js      # Gerador de dados de teste (Node.js)
├── metrics_collector.js   # Coletor de métricas da aplicação (Node.js)
├── load_test.js           # Script k6 para testes de carga
├── run_performance_test.sh # Script principal
└── README.md              # Este arquivo
```

## 🎯 Cenários de Teste

### 1. Geração de Dados
- **Pacotes**: 1000 pacotes por padrão (configurável)
- **Eventos**: 5 eventos por pacote (configurável)
- **Dados realistas**: Remetentes, destinatários, descrições e status variados

### 2. Testes de Carga
- **40%**: Consulta de pacote individual com eventos (cache Redis)
- **30%**: Consulta de pacote individual sem eventos
- **15%**: Lista de pacotes com filtros
- **15%**: Lista de pacotes paginada

### 3. Perfil de Carga
- **Rampa de subida**: 2 minutos até 10 usuários
- **Carga constante**: 5 minutos com 50 usuários
- **Pico de carga**: 2 minutos até 100 usuários
- **Carga máxima**: 5 minutos com 100 usuários
- **Rampa de descida**: 2 minutos até 0 usuários

## 🚀 Como Executar

### 1. Preparação
Certifique-se de que todos os containers estão rodando:

```bash
cd docker
docker compose up -d
cd ..
```

### 2. Instalar dependências Node.js

No diretório `performance-tests`, execute:

```bash
npm install axios commander uuid
```

### 3. Execução Completa
Execute o script principal que fará tudo automaticamente:

```bash
cd performance-tests
chmod +x run_performance_test.sh
./run_performance_test.sh
```

### 4. Execução Manual
Se preferir executar manualmente:

```bash
# 1. Gerar dados de teste
node data_generator.js --packages 1000 --events 5

# 2. Executar teste de carga
k6 run load_test.js

# 3. Coletar métricas da aplicação
node metrics_collector.js --url http://localhost:8083 --interval 5 --duration 60
```

## ⚙️ Configuração

### Variáveis de Ambiente

```bash
# URLs das APIs
export BASE_URL="http://localhost:8080"      # Package Ingestion
export QUERY_URL="http://localhost:8083"     # Package Query

# Volume de dados
export PACKAGES_COUNT=1000                   # Número de pacotes
export EVENTS_PER_PACKAGE=5                  # Eventos por pacote
```

### Parâmetros do Gerador

```bash
node data_generator.js --help
```

Opções disponíveis:
- `--packages`: Número de pacotes a criar (padrão: 1000)
- `--events`: Eventos por pacote (padrão: 5)
- `--url`: URL base da API (padrão: http://localhost:8080)

## 📊 Resultados

Os resultados são salvos em `results/YYYYMMDD_HHMMSS/` com:

- `performance_results.json`: Dados detalhados do k6
- `performance_results.csv`: Dados em formato CSV
- `application_metrics.json`: Métricas da aplicação via Spring Boot Actuator
- `performance_report.html`: Relatório HTML interativo
- `data_generation.log`: Log da geração de dados
- `performance_test.log`: Log do teste de carga
- `generated_packages.json`: IDs dos pacotes criados

### Métricas Coletadas

#### k6 (Performance de Carga)
- **Duração de resposta**: Média, P95, P99
- **Taxa de erro**: Porcentagem de requisições com erro
- **Throughput**: Requisições por segundo
- **Latência**: Tempo de resposta por endpoint

#### Spring Boot Actuator (Métricas da Aplicação)
- **JVM Memory**: Uso de memória da JVM
- **HTTP Requests**: Total de requisições HTTP
- **HikariCP Pool**: Conexões ativas no pool de banco de dados
- **Cache Redis**: Estatísticas de cache (gets, hits, misses)
- **Circuit Breaker**: Métricas do Resilience4j Circuit Breaker
- **System CPU**: Uso de CPU do sistema
- **Threads**: Número de threads ativas
- **Health Check**: Status de saúde da aplicação

## 🎯 Thresholds (Limites)

- **Duração P95**: < 500ms para consultas individuais
- **Taxa de erro**: < 10%
- **Duração média**: < 300ms para consultas sem eventos

## 🔍 Análise de Performance

### Cache Redis
- Pacotes com status `IN_TRANSIT` são cacheados por 1 hora
- Circuit breaker ativa fallback em caso de falha
- Performance esperada: 50-80% melhor para dados cacheados

### Banco de Dados
- MySQL Master/Slave para leitura/escrita
- Conexões otimizadas para alto volume
- Índices em campos de consulta frequente

### Monitoramento
- Métricas do Spring Boot Actuator
- Logs estruturados com correlation IDs
- Health checks automáticos

## 🛠️ Troubleshooting

### Problemas Comuns

1. **API não responde**
   ```bash
   # Verificar containers
   docker ps
   
   # Verificar logs
   docker logs package-query
   ```

2. **Erro de conexão com banco**
   ```bash
   # Verificar MySQL
   docker logs mysql1
   docker logs mysql2
   ```

3. **Redis não conecta**
   ```bash
   # Verificar Redis
   docker logs redis
   ```

4. **k6 não instala**
   ```bash
   # Instalar manualmente
   curl -L https://github.com/grafana/k6/releases/download/v0.47.0/k6-v0.47.0-linux-amd64.tar.gz | tar xz
   sudo cp k6-v0.47.0-linux-amd64/k6 /usr/local/bin/
   ```

### Logs Importantes

- `data_generation.log`: Progresso da geração de dados
- `performance_test.log`: Resultados do teste de carga
- Logs dos containers: `docker logs <container-name>`

## 📈 Interpretação dos Resultados

### Boa Performance
- Duração P95 < 500ms
- Taxa de erro < 5%
- Throughput > 100 req/s

### Performance Aceitável
- Duração P95 < 1000ms
- Taxa de erro < 10%
- Throughput > 50 req/s

### Problemas de Performance
- Duração P95 > 1000ms
- Taxa de erro > 10%
- Throughput < 50 req/s

## 🔧 Otimizações

### Cache
- Aumentar TTL do Redis para dados estáticos
- Implementar cache de segundo nível
- Usar cache distribuído para múltiplas instâncias

### Banco de Dados
- Otimizar queries com EXPLAIN
- Adicionar índices compostos
- Implementar read replicas adicionais

### Aplicação
- Aumentar pool de conexões
- Otimizar serialização JSON
- Implementar compressão gzip

## 📞 Suporte

Para dúvidas ou problemas:
1. Verificar logs em `results/`
2. Consultar documentação do k6
3. Verificar status dos containers Docker 