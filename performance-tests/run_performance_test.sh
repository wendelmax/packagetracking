#!/bin/bash

# Script para executar testes de performance completos
# Inclui geração de dados e execução de testes de carga

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurações
BASE_URL=${BASE_URL:-"http://localhost:8080"}
QUERY_URL=${QUERY_URL:-"http://localhost:8083"}  # Porta do package-query
PACKAGES_COUNT=${PACKAGES_COUNT:-1000}
EVENTS_PER_PACKAGE=${EVENTS_PER_PACKAGE:-5}
RESULTS_DIR="results/$(date +%Y%m%d_%H%M%S)"
SPRING_PROFILE=${SPRING_PROFILE:-"default"}

# Função para log colorido
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO: $1${NC}"
}

# Função para verificar se um comando existe
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Função para verificar se o Docker está rodando
check_docker() {
    if ! command_exists docker; then
        error "Docker não encontrado. Instale o Docker primeiro."
        exit 1
    fi
    
    if ! docker info >/dev/null 2>&1; then
        error "Docker não está rodando. Inicie o Docker primeiro."
        exit 1
    fi
    
    log "✅ Docker está rodando"
}

# Função para configurar profile de performance
configure_performance_profile() {
    log "Configurando configurações balanceadas para performance"
    
    info "Usando configurações balanceadas otimizadas:"
    info "  - Persistence: habilitado"
    info "  - Queues: habilitado"
    info "  - Endpoints: package"
    info "  - Virtual Threads: 100"
    info "  - Pool de conexões: otimizado por container"
    info "  - Cache: habilitado"
    info "  - Circuit Breaker: habilitado"
}

# Função para subir os containers
start_containers() {
    log "Verificando se os containers estão rodando..."
    
    # Verificar se os containers principais estão rodando
    if docker ps --format "table {{.Names}}" | grep -q "mysql1\|redis\|package-command\|package-query" 2>/dev/null; then
        log "✅ Containers já estão rodando"
        return 0
    fi
    
    log "🚀 Subindo containers com Docker Compose para performance..."
    
    # Navegar para o diretório docker
    cd ../docker
    
    # Parar containers existentes se houver
    docker compose down 2>/dev/null || true
    
    # Subir containers com configurações balanceadas otimizadas
    docker compose up -d --build
    
    if [ $? -eq 0 ]; then
        log "✅ Containers de performance iniciados com sucesso"
    else
        error "❌ Falha ao iniciar containers de performance"
        exit 1
    fi
    
    # Voltar para o diretório original
    cd ../performance-tests
    
    # Aguardar um pouco para os containers inicializarem
    log "Aguardando inicialização dos containers..."
    sleep 15
}

# Função para verificar se a API está respondendo
check_api_health() {
    local url=$1
    local service=$2
    local max_attempts=${3:-30}
    
    log "Verificando saúde da API $service em $url"
    
    for i in {1..$max_attempts}; do
        if curl -s -f "$url/actuator/health" >/dev/null 2>&1; then
            log "✅ API $service está respondendo"
            return 0
        fi
        
        warn "Tentativa $i/$max_attempts: API $service não está respondendo, aguardando..."
        sleep 2
    done
    
    error "❌ API $service não está respondendo após $max_attempts tentativas"
    return 1
}

# Função para instalar dependências
install_dependencies() {
    log "Verificando dependências..."
    
    # Verificar Node.js
    if ! command_exists node; then
        error "Node.js não encontrado. Instale Node.js 16+"
        exit 1
    fi
    # Verificar npm
    if ! command_exists npm; then
        error "npm não encontrado. Instale o npm"
        exit 1
    fi
    
    # Instalar dependências Node.js
    log "Instalando dependências Node.js..."
    npm install --no-audit --no-fund axios commander uuid >/dev/null 2>&1 || {
        error "Falha ao instalar dependências Node.js. Execute: npm install axios commander uuid"
        exit 1
    }
    
    # Verificar k6
    if ! command_exists k6; then
        warn "k6 não encontrado. Instalando..."
        if command_exists curl; then
            curl -L https://github.com/grafana/k6/releases/download/v0.47.0/k6-v0.47.0-linux-amd64.tar.gz | tar xz
            sudo cp k6-v0.47.0-linux-amd64/k6 /usr/local/bin/
            rm -rf k6-v0.47.0-linux-amd64
        else
            error "curl não encontrado. Instale k6 manualmente: https://k6.io/docs/getting-started/installation/"
            exit 1
        fi
    fi
    
    log "✅ Todas as dependências estão instaladas"
}

# Função para criar diretório de resultados
create_results_dir() {
    log "Criando diretório de resultados: $RESULTS_DIR"
    mkdir -p "$RESULTS_DIR"
    
    # Copiar scripts para o diretório de resultados
    cp data_generator.js "$RESULTS_DIR/"
    cp metrics_collector.js "$RESULTS_DIR/"
    cp load_test.js "$RESULTS_DIR/"
    cp "$0" "$RESULTS_DIR/"
}

# Função para gerar dados de teste
generate_test_data() {
    log "Iniciando geração de dados de teste..."
    log "Pacotes: $PACKAGES_COUNT, Eventos por pacote: $EVENTS_PER_PACKAGE"
    
    cd "$RESULTS_DIR"
    
    node data_generator.js \
        --packages "$PACKAGES_COUNT" \
        --events "$EVENTS_PER_PACKAGE" \
        --url "$BASE_URL" \
        2>&1 | tee data_generation.log
    
    if [ $? -eq 0 ]; then
        log "✅ Dados de teste gerados com sucesso"
        
        # Mostrar estatísticas
        if [ -f generated_packages.json ]; then
            local total_packages=$(jq -r '.total_packages' generated_packages.json 2>/dev/null || echo "N/A")
            local total_events=$(jq -r '.total_events' generated_packages.json 2>/dev/null || echo "N/A")
            local generation_time=$(jq -r '.generation_time' generated_packages.json 2>/dev/null || echo "N/A")
            
            info "Estatísticas da geração:"
            info "  - Pacotes criados: $total_packages"
            info "  - Eventos criados: $total_events"
            info "  - Tempo de geração: ${generation_time}s"
        fi
    else
        error "❌ Falha na geração de dados de teste"
        exit 1
    fi
    
    cd - >/dev/null
}

# Função para executar teste de performance
run_performance_test() {
    log "Iniciando teste de performance..."
    
    cd "$RESULTS_DIR"
    
    # Configurar variáveis de ambiente para k6
    export BASE_URL="$QUERY_URL"
    
    # Iniciar coleta de métricas da aplicação em background
    log "Iniciando coleta de métricas da aplicação..."
    node metrics_collector.js \
        --url "$QUERY_URL" \
        --interval 5 \
        --duration 1200 \
        --output application_metrics.json &
    
    METRICS_PID=$!
    log "Coletor de métricas iniciado com PID: $METRICS_PID"
    
    # Aguardar um pouco para a coleta de métricas começar
    sleep 5
    
    # Executar teste k6
    k6 run \
        --out json=performance_results.json \
        --out csv=performance_results.csv \
        load_test.js \
        2>&1 | tee performance_test.log
    
    if [ $? -eq 0 ]; then
        log "✅ Teste de performance concluído com sucesso"
    else
        error "❌ Falha no teste de performance"
        # Parar coleta de métricas mesmo em caso de erro
        if [ ! -z "$METRICS_PID" ]; then
            kill $METRICS_PID 2>/dev/null || true
        fi
        exit 1
    fi
    
    # Aguardar um pouco mais para coletar métricas pós-teste
    log "Aguardando coleta final de métricas..."
    sleep 10
    
    # Parar coleta de métricas
    if [ ! -z "$METRICS_PID" ]; then
        log "Parando coleta de métricas (PID: $METRICS_PID)..."
        kill $METRICS_PID 2>/dev/null || true
        wait $METRICS_PID 2>/dev/null || true
    fi
    
    cd - >/dev/null
}

# Função para gerar relatório
generate_report() {
    log "Gerando relatório de performance..."
    
    cd "$RESULTS_DIR"
    
    # Criar relatório HTML simples
    cat > performance_report.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>Relatório de Performance - Package Tracking</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .metric { display: inline-block; margin: 10px; padding: 10px; background: #e8f4f8; border-radius: 3px; }
        .success { color: green; }
        .warning { color: orange; }
        .error { color: red; }
        pre { background: #f5f5f5; padding: 10px; border-radius: 3px; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="header">
        <h1>📊 Relatório de Performance - Package Tracking</h1>
        <p>Data: <span id="test-date"></span></p>
    </div>
    
    <div class="section">
        <h2>📈 Resumo do Teste (k6)</h2>
        <div id="summary"></div>
    </div>
    
    <div class="section">
        <h2>🔧 Métricas da Aplicação (Spring Boot Actuator)</h2>
        <div id="app-metrics">
            <p>Carregando métricas da aplicação...</p>
        </div>
    </div>
    
    <div class="section">
        <h2>🔧 Configuração</h2>
        <div id="configuration"></div>
    </div>
    
    <div class="section">
        <h2>📋 Logs</h2>
        <h3>Geração de Dados</h3>
        <pre id="data-generation-log"></pre>
        
        <h3>Teste de Performance</h3>
        <pre id="performance-test-log"></pre>
    </div>
    
    <script>
        // Carregar dados do relatório
        fetch('performance_results.json')
            .then(response => response.json())
            .then(data => {
                document.getElementById('test-date').textContent = new Date().toLocaleString('pt-BR');
                
                const summary = document.getElementById('summary');
                const metrics = data.metrics;
                
                if (metrics) {
                    const httpReqDuration = metrics['http_req_duration'];
                    const httpReqFailed = metrics['http_req_failed'];
                    
                    summary.innerHTML = `
                        <div class="metric">
                            <strong>Duração Média:</strong><br>
                            ${httpReqDuration ? (httpReqDuration.avg || 0).toFixed(2) + 'ms' : 'N/A'}
                        </div>
                        <div class="metric">
                            <strong>Duração P95:</strong><br>
                            ${httpReqDuration ? (httpReqDuration['p(95)'] || 0).toFixed(2) + 'ms' : 'N/A'}
                        </div>
                        <div class="metric">
                            <strong>Taxa de Erro:</strong><br>
                            ${httpReqFailed ? ((httpReqFailed.rate || 0) * 100).toFixed(2) + '%' : 'N/A'}
                        </div>
                        <div class="metric">
                            <strong>Requisições/s:</strong><br>
                            ${metrics['http_reqs'] ? (metrics['http_reqs'].rate || 0).toFixed(2) : 'N/A'}
                        </div>
                    `;
                }
            })
            .catch(error => {
                console.error('Erro ao carregar dados:', error);
            });
        
        // Carregar métricas da aplicação
        fetch('application_metrics.json')
            .then(response => response.json())
            .then(data => {
                const summary = data.collection_info;
                const metrics = data.metrics;
                
                if (metrics && metrics.length > 0) {
                    const lastSample = metrics[metrics.length - 1];
                    const metricsSummary = lastSample.metrics;
                    
                    let html = \`
                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 15px;">
                            <div style="background: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #007acc;">
                                <h3>📊 Informações da Coleta</h3>
                                <p><strong>Total de amostras:</strong> \${summary.total_samples}</p>
                                <p><strong>Intervalo de coleta:</strong> \${summary.collection_interval}s</p>
                                <p><strong>Período:</strong> \${summary.start_time} até \${summary.end_time}</p>
                            </div>
                    \`;
                    
                    // Métricas de JVM
                    if (metricsSummary.jvm_memory && metricsSummary.jvm_memory.status === 'success') {
                        html += \`
                            <div style="background: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #28a745;">
                                <h3>🧠 Memória JVM</h3>
                                <p><strong>Memória usada:</strong> \${(metricsSummary.jvm_memory.value / 1024 / 1024).toFixed(2)} MB</p>
                            </div>
                        \`;
                    }
                    
                    // Métricas de HTTP
                    if (metricsSummary.http_requests && metricsSummary.http_requests.status === 'success') {
                        html += \`
                            <div style="background: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #ffc107;">
                                <h3>🌐 Requisições HTTP</h3>
                                <p><strong>Total de requisições:</strong> \${metricsSummary.http_requests.value}</p>
                            </div>
                        \`;
                    }
                    
                    // Métricas de Pool de Conexões
                    if (metricsSummary.hikari_pool && metricsSummary.hikari_pool.status === 'success') {
                        html += \`
                            <div style="background: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #17a2b8;">
                                <h3>🔗 Pool de Conexões (HikariCP)</h3>
                                <p><strong>Conexões ativas:</strong> \${metricsSummary.hikari_pool.value}</p>
                            </div>
                        \`;
                    }
                    
                    // Métricas de Cache
                    const cacheMetrics = Object.keys(metricsSummary).filter(key => key.startsWith('cache_'));
                    if (cacheMetrics.length > 0) {
                        html += \`
                            <div style="background: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #dc3545;">
                                <h3>💾 Cache Redis</h3>
                        \`;
                        cacheMetrics.forEach(metric => {
                            if (metricsSummary[metric].status === 'success') {
                                html += \`<p><strong>\${metric}:</strong> \${metricsSummary[metric].value}</p>\`;
                            }
                        });
                        html += \`</div>\`;
                    }
                    
                    // Métricas de Circuit Breaker
                    const cbMetrics = Object.keys(metricsSummary).filter(key => key.startsWith('circuit_breaker_'));
                    if (cbMetrics.length > 0) {
                        html += \`
                            <div style="background: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #6f42c1;">
                                <h3>⚡ Circuit Breaker</h3>
                        \`;
                        cbMetrics.forEach(metric => {
                            if (metricsSummary[metric].status === 'success') {
                                html += \`<p><strong>\${metric}:</strong> \${metricsSummary[metric].value}</p>\`;
                            }
                        });
                        html += \`</div>\`;
                    }
                    
                    html += \`</div>\`;
                    document.getElementById('app-metrics').innerHTML = html;
                } else {
                    document.getElementById('app-metrics').innerHTML = '<p style="color: red;">Nenhuma métrica da aplicação disponível</p>';
                }
            })
            .catch(error => {
                document.getElementById('app-metrics').innerHTML = '<p style="color: red;">Erro ao carregar métricas da aplicação: ' + error + '</p>';
            });
        
        // Carregar logs
        fetch('data_generation.log')
            .then(response => response.text())
            .then(text => {
                document.getElementById('data-generation-log').textContent = text;
            })
            .catch(error => {
                document.getElementById('data-generation-log').textContent = 'Log não disponível';
            });
        
        fetch('performance_test.log')
            .then(response => response.text())
            .then(text => {
                document.getElementById('performance-test-log').textContent = text;
            })
            .catch(error => {
                document.getElementById('performance-test-log').textContent = 'Log não disponível';
            });
    </script>
</body>
</html>
EOF
    
    log "✅ Relatório HTML gerado: $RESULTS_DIR/performance_report.html"
    
    # Mostrar resumo final
    info "📊 Resumo do teste de performance:"
    info "  - Diretório de resultados: $RESULTS_DIR"
    info "  - Relatório HTML: $RESULTS_DIR/performance_report.html"
    info "  - Dados JSON: $RESULTS_DIR/performance_results.json"
    info "  - Dados CSV: $RESULTS_DIR/performance_results.csv"
    info "  - Métricas da Aplicação: $RESULTS_DIR/application_metrics.json"
    info "  - Logs: $RESULTS_DIR/*.log"
    
    cd - >/dev/null
}

# Função principal
main() {
    log "🚀 Iniciando teste de performance completo"
    log "Configurações:"
    log "  - Base URL: $BASE_URL"
    log "  - Query URL: $QUERY_URL"
    log "  - Profile: $SPRING_PROFILE"
    log "  - Pacotes: $PACKAGES_COUNT"
    log "  - Eventos por pacote: $EVENTS_PER_PACKAGE"
    log "  - Diretório de resultados: $RESULTS_DIR"
    log ""
    
    # Verificar se estamos no diretório correto
    if [ ! -f "data_generator.js" ] || [ ! -f "load_test.js" ]; then
        error "Execute este script do diretório performance-tests/"
        exit 1
    fi
    
    # Verificar Docker
    check_docker
    
    # Instalar dependências
    install_dependencies
    
    # Configurar profile de performance
    configure_performance_profile
    
    # Subir containers se necessário
    start_containers
    
    # Verificar saúde das APIs
    check_api_health "$BASE_URL" "Package Ingestion" || exit 1
    check_api_health "$QUERY_URL" "Package Query" 3 || warn "Package Query não está respondendo - continuando apenas com geração de dados"
    
    # Verificar se o event-ingestion também está respondendo (opcional)
    EVENT_INGESTION_URL="http://localhost:8081"
    check_api_health "$EVENT_INGESTION_URL" "Event Ingestion" || warn "Event Ingestion não está respondendo (pode ser normal se não estiver configurado)"
    
    # Criar diretório de resultados
    create_results_dir
    
    # Gerar dados de teste
    generate_test_data
    
    # Aguardar um pouco para garantir que os dados foram processados
    log "Aguardando processamento dos dados..."
    sleep 10
    
    # Executar teste de performance
    run_performance_test
    
    # Gerar relatório
    generate_report
    
    log "🎉 Teste de performance concluído com sucesso!"
    log "📁 Resultados disponíveis em: $RESULTS_DIR"
}

# Função para limpar recursos
cleanup() {
    log "Limpando recursos..."
    
    # Parar coleta de métricas se estiver rodando
    if [ ! -z "$METRICS_PID" ]; then
        log "Parando coleta de métricas..."
        kill $METRICS_PID 2>/dev/null || true
    fi
    
    # Parar containers se solicitado
    if [ "$CLEANUP_CONTAINERS" = "true" ]; then
        log "Parando containers de performance..."
        cd ../docker
        docker compose down
        cd ../performance-tests
    fi
    
    log "Limpeza concluída"
}

# Função para limpar containers de performance
cleanup_performance_containers() {
    log "Limpando containers de performance..."
    
    cd ../docker
    
    # Parar e remover containers de performance
    docker compose down --volumes --remove-orphans
    
    # Limpar imagens relacionadas
    docker rmi $(docker images -q packagetracking_package-ingestion 2>/dev/null) 2>/dev/null || true
    docker rmi $(docker images -q packagetracking_event-ingestion 2>/dev/null) 2>/dev/null || true
    docker rmi $(docker images -q packagetracking_event-consumer 2>/dev/null) 2>/dev/null || true
    docker rmi $(docker images -q packagetracking_package-query 2>/dev/null) 2>/dev/null || true
    
    cd ../performance-tests
    
    log "Containers de performance limpos"
}

# Configurar trap para limpeza em caso de erro
trap cleanup EXIT

# Executar função principal
main "$@" 