# Testes Unitários - Módulo Query

Este diretório contém os testes unitários e de integração para o módulo query do sistema de rastreamento de pacotes.

## Estrutura dos Testes

### 1. Testes de Serviço (`service/`)
- **PackageQueryServiceTest.java**: Testes unitários para o `PackageQueryService`
  - Testa consulta de pacotes com e sem eventos
  - Testa filtros por remetente e destinatário
  - Testa paginação
  - Testa métodos assíncronos
  - Testa tratamento de erros

### 2. Testes de Controller (`controller/`)
- **PackageQueryControllerTest.java**: Testes unitários para o `PackageQueryController`
  - Testa todos os endpoints REST
  - Testa parâmetros opcionais usando `Optional`
  - Testa respostas HTTP corretas
  - Testa tratamento de erros
  - Testa métodos assíncronos

### 3. Testes de Configuração (`config/`)
- **VirtualThreadConfigTest.java**: Testes para configuração de Virtual Threads
- **CacheConfigTest.java**: Testes para configuração de cache

### 4. Testes de Integração (`integration/`)
- **PackageQueryIntegrationTest.java**: Testes de integração end-to-end
  - Testa fluxo completo com banco de dados H2
  - Testa endpoints REST com MockMvc
  - Testa validação de respostas JSON
  - Testa cenários de erro

### 5. Testes de Performance (`performance/`)
- **PackageQueryPerformanceTest.java**: Testes de performance
  - Testa tempo de resposta dos métodos
  - Testa concorrência
  - Compara performance síncrona vs assíncrona

## Configuração de Teste

### Arquivo de Configuração (`resources/application-test.yml`)
- Usa banco H2 em memória
- Configurações otimizadas para teste
- Logs reduzidos para melhor performance

### Dependências de Teste
- JUnit 5
- Mockito
- Spring Boot Test
- H2 Database
- MockMvc

## Como Executar os Testes

### Executar todos os testes
```bash
cd package-query
mvn test
```

### Executar testes específicos
```bash
# Apenas testes unitários
mvn test -Dtest=*Test

# Apenas testes de integração
mvn test -Dtest=*IntegrationTest

# Apenas testes de performance
mvn test -Dtest=*PerformanceTest

# Teste específico
mvn test -Dtest=PackageQueryServiceTest
```

### Executar com cobertura
```bash
mvn test jacoco:report
```

## Cobertura de Testes

Os testes cobrem:

### PackageQueryService
- SUCCESS: Consulta de pacote com eventos
- SUCCESS: Consulta de pacote sem eventos
- SUCCESS: Pacote não encontrado
- SUCCESS: Filtros por remetente
- SUCCESS: Filtros por destinatário
- SUCCESS: Filtros combinados
- SUCCESS: Paginação
- SUCCESS: Consulta por status
- SUCCESS: Eventos de rastreamento
- SUCCESS: Métodos assíncronos
- SUCCESS: Tratamento de status nulo

### PackageQueryController
- SUCCESS: Endpoints síncronos
- SUCCESS: Endpoints assíncronos
- SUCCESS: Parâmetros opcionais
- SUCCESS: Respostas HTTP corretas
- SUCCESS: Tratamento de erros
- SUCCESS: Validação de entrada

### Configurações
- SUCCESS: Virtual Threads
- SUCCESS: Cache
- SUCCESS: Configurações de banco

## Cenários de Teste

### Cenários de Sucesso
1. **Consulta de pacote com eventos**: Verifica se retorna pacote e eventos
2. **Consulta de pacote sem eventos**: Verifica se retorna apenas pacote
3. **Filtros de busca**: Testa filtros por remetente, destinatário e ambos
4. **Paginação**: Verifica funcionamento correto da paginação
5. **Métodos assíncronos**: Testa execução assíncrona

### Cenários de Erro
1. **Pacote não encontrado**: Verifica tratamento de erro 404
2. **Erro interno**: Verifica tratamento de erro 500
3. **Parâmetros inválidos**: Verifica validação de entrada

### Cenários de Performance
1. **Tempo de resposta**: Verifica se está dentro dos limites aceitáveis
2. **Concorrência**: Testa múltiplas requisições simultâneas
3. **Comparação síncrono vs assíncrono**: Avalia ganhos de performance

## Boas Práticas Seguidas

1. **Arrange-Act-Assert**: Estrutura clara dos testes
2. **Mocks apropriados**: Uso correto de mocks para isolamento
3. **Testes independentes**: Cada teste é independente dos outros
4. **Nomes descritivos**: Nomes de métodos explicam o que está sendo testado
5. **Cobertura abrangente**: Testa casos de sucesso e erro
6. **Performance**: Inclui testes de performance
7. **Integração**: Testa fluxo completo

## Manutenção

### Adicionando Novos Testes
1. Siga a estrutura existente
2. Use nomes descritivos
3. Inclua casos de sucesso e erro
4. Documente cenários complexos
5. Mantenha testes independentes

### Atualizando Testes
1. Execute todos os testes antes de fazer mudanças
2. Atualize testes quando alterar comportamento
3. Mantenha cobertura de código alta
4. Verifique performance dos testes

## Relatórios

Após executar os testes, você pode encontrar:
- Relatórios de cobertura em `target/site/jacoco/`
- Logs de teste em `target/surefire-reports/`
- Relatórios de performance no console
