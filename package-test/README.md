# Módulo de Teste - Sistema de Rastreamento de Pacotes

Este módulo contém todos os testes automatizados para o sistema de rastreamento de pacotes, substituindo os scripts bash por testes Java robustos e integrados.

## Objetivo

Converter os scripts de teste bash em testes Java estruturados, proporcionando:
- SUCCESS: Integração com CI/CD
- SUCCESS: Relatórios estruturados
- SUCCESS: Assertions robustas
- SUCCESS: Isolamento de testes
- SUCCESS: Execução paralela
- SUCCESS: Debugging avançado

## 📁 Estrutura do Módulo

```
package-test/
├── src/test/java/com/packagetracking/test/
│   ├── integration/          # Testes de integração
│   │   ├── EventIngestionIntegrationTest.java
│   │   └── DatabaseReplicationIntegrationTest.java
│   ├── e2e/                  # Testes end-to-end
│   │   └── PackageTrackingE2ETest.java
│   ├── performance/          # Testes de performance
│   │   └── SystemPerformanceTest.java
│   └── utils/                # Utilitários para testes
│       └── TestDataBuilder.java
└── src/test/resources/       # Recursos de teste
    ├── application-test.yml
    ├── init-master.sql
    └── init-slave.sql
```

## 🧪 Tipos de Teste

### 1. Testes de Integração (`integration/`)
- **EventIngestionIntegrationTest**: Testa ingestão de eventos via RabbitMQ
- **DatabaseReplicationIntegrationTest**: Testa replicação MySQL Master-Slave

### 2. Testes End-to-End (`e2e/`)
- **PackageTrackingE2ETest**: Testa todo o fluxo do sistema

### 3. Testes de Performance (`performance/`)
- **SystemPerformanceTest**: Testes de carga e stress

## Como Executar

### Executar Todos os Testes
```bash
mvn test
```

### Executar Testes de Integração
```bash
mvn test -Dtest="*IntegrationTest"
```

### Executar Testes E2E
```bash
mvn test -Dtest="*E2ETest"
```

### Executar Testes de Performance
```bash
mvn test -Dtest="*PerformanceTest"
```

### Executar Testes Específicos
```bash
# Teste de ingestão de eventos
mvn test -Dtest="EventIngestionIntegrationTest"

# Teste de replicação de banco
mvn test -Dtest="DatabaseReplicationIntegrationTest"

# Teste E2E completo
mvn test -Dtest="PackageTrackingE2ETest"
```

## Configuração

### Dependências Principais
- **TestContainers**: Para containers Docker em testes
- **REST Assured**: Para testes de API
- **Awaitility**: Para testes assíncronos
- **WireMock**: Para mock de APIs externas

### Configuração de Containers
Os testes usam TestContainers para criar automaticamente:
- MySQL Master/Slave
- RabbitMQ
- Outros serviços necessários

## Relatórios

### Relatórios JUnit
Os testes geram relatórios XML que podem ser integrados com:
- Jenkins
- GitHub Actions
- GitLab CI
- Outros sistemas de CI/CD

### Relatórios de Performance
Testes de performance geram relatórios detalhados com:
- Latência (P50, P95, P99)
- Throughput (req/s)
- Uso de recursos (CPU, Memória)
- Taxa de erro

## Migração dos Scripts Bash

### Scripts Convertidos

| Script Original | Teste Java | Status |
|----------------|------------|--------|
| `test-event-ingestion.sh` | `EventIngestionIntegrationTest` | SUCCESS |
| `e2e-test-working.sh` | `PackageTrackingE2ETest` | SUCCESS |
| `test-replication.sh` | `DatabaseReplicationIntegrationTest` | SUCCESS |
| `run-performance-tests.sh` | `SystemPerformanceTest` | SUCCESS |

### Benefícios da Conversão

**Antes (Scripts Bash):**
```bash
# Manual, propenso a erros
curl -s -X POST http://localhost:8080/api/packages \
  -H "Content-Type: application/json" \
  -d '{"description": "test"}'
```

**Depois (Testes Java):**
```java
// Automatizado, com assertions robustas
given()
    .contentType(ContentType.JSON)
    .body(packageRequest)
    .when()
    .post("/api/packages")
    .then()
    .statusCode(201)
    .body("id", notNullValue());
```

## Desenvolvimento

### Adicionando Novos Testes

1. **Teste de Integração:**
```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class NovoIntegrationTest {
    // Implementação
}
```

2. **Teste E2E:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class NovoE2ETest {
    // Implementação
}
```

3. **Teste de Performance:**
```java
@SpringBootTest
@ActiveProfiles("test")
class NovoPerformanceTest {
    // Implementação
}
```

### Boas Práticas

1. **Nomenclatura**: Use sufixos `*Test`, `*IntegrationTest`, `*E2ETest`, `*PerformanceTest`
2. **Isolamento**: Cada teste deve ser independente
3. **Cleanup**: Sempre limpe dados de teste
4. **Assertions**: Use assertions específicas e descritivas
5. **Logging**: Adicione logs informativos para debug

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run Tests
  run: |
    mvn clean test
    mvn test -Dtest="*IntegrationTest"
    mvn test -Dtest="*E2ETest"
```

### Jenkins Pipeline Example
```groovy
stage('Test') {
    steps {
        sh 'mvn clean test'
        sh 'mvn test -Dtest="*IntegrationTest"'
        sh 'mvn test -Dtest="*E2ETest"'
    }
    post {
        always {
            publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
        }
    }
}
```

## Resultado

Com este módulo de teste, você tem:
- SUCCESS: **100% de cobertura** dos cenários dos scripts bash
- SUCCESS: **Execução automatizada** em CI/CD
- SUCCESS: **Relatórios estruturados** e detalhados
- SUCCESS: **Debugging avançado** com IDE
- SUCCESS: **Manutenibilidade** superior
- SUCCESS: **Escalabilidade** para novos testes

Os scripts bash ainda podem ser mantidos para uso manual, mas os testes Java são a base principal para qualidade e automação. 