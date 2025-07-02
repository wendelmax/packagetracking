#!/bin/bash

# Script para executar testes de performance com Locust
# Sistema de Rastreamento de Pacotes

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurações
LOCUSTFILE="locustfile.py"
RESULTS_DIR="locust-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="locust-test-${TIMESTAMP}.log"
HOST="http://localhost:8083"  # Package Query
USERS=20
SPAWN_RATE=5
RUN_TIME="1m"

echo -e "${BLUE}🚀 Iniciando Teste de Performance com Locust${NC}"
echo -e "${BLUE}============================================${NC}"

# Verificar se o ambiente virtual existe
if [ ! -d "locust-env" ]; then
    echo -e "${YELLOW}🔧 Criando ambiente virtual Python...${NC}"
    python3 -m venv locust-env
fi

# Ativar ambiente virtual e verificar Locust
echo -e "${YELLOW}🔧 Ativando ambiente virtual...${NC}"
source locust-env/bin/activate

# Verificar se o Locust está instalado no ambiente virtual
if ! locust --version &> /dev/null; then
    echo -e "${RED}❌ Locust não encontrado. Instalando...${NC}"
    pip install locust
fi

# Verificar se o arquivo de teste existe
if [ ! -f "$LOCUSTFILE" ]; then
    echo -e "${RED}❌ Arquivo de teste não encontrado: $LOCUSTFILE${NC}"
    exit 1
fi

# Criar diretório de resultados
mkdir -p "$RESULTS_DIR"

echo -e "${YELLOW}📋 Configurações do Teste:${NC}"
echo -e "   Locustfile: $LOCUSTFILE"
echo -e "   Host: $HOST"
echo -e "   Usuários: $USERS"
echo -e "   Spawn Rate: $SPAWN_RATE/seg"
echo -e "   Tempo de Execução: $RUN_TIME"
echo -e "   Results Dir: $RESULTS_DIR"
echo -e "   Log File: $LOG_FILE"

# Verificar se os serviços estão rodando
echo -e "${YELLOW}🔍 Verificando se os serviços estão rodando...${NC}"

# Verificar Package Query (8083)
if curl -s http://localhost:8083/actuator/health > /dev/null; then
    echo -e "${GREEN}✅ Package Query (8083) está rodando${NC}"
else
    echo -e "${RED}❌ Package Query (8083) não está respondendo${NC}"
    echo -e "${YELLOW}💡 Execute: docker compose -f docker/docker-compose.yml up -d${NC}"
    exit 1
fi

# Verificar Package Command (8080)
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo -e "${GREEN}✅ Package Command (8080) está rodando${NC}"
else
    echo -e "${RED}❌ Package Command (8080) não está respondendo${NC}"
    echo -e "${YELLOW}💡 Execute: docker compose -f docker/docker-compose.yml up -d${NC}"
    exit 1
fi

echo -e "${GREEN}🎯 Todos os serviços estão rodando!${NC}"

# Função para executar teste em modo headless
run_headless_test() {
    echo -e "${BLUE}⚡ Executando teste Locust em modo headless...${NC}"
    echo -e "${YELLOW}⏱️  Executando por $RUN_TIME...${NC}"
    
    locust \
        --headless \
        --host="$HOST" \
        --users="$USERS" \
        --spawn-rate="$SPAWN_RATE" \
        --run-time="$RUN_TIME" \
        --html="$RESULTS_DIR/report-${TIMESTAMP}.html" \
        --csv="$RESULTS_DIR/results-${TIMESTAMP}" \
        --logfile="$LOG_FILE" \
        --loglevel=INFO \
        --locustfile="$LOCUSTFILE"
}

# Função para executar teste com interface web
run_web_test() {
    echo -e "${BLUE}🌐 Iniciando interface web do Locust...${NC}"
    echo -e "${YELLOW}📊 Acesse: http://localhost:8089${NC}"
    echo -e "${YELLOW}⏹️  Pressione Ctrl+C para parar${NC}"
    
    locust \
        --host="$HOST" \
        --locustfile="$LOCUSTFILE" \
        --logfile="$LOG_FILE" \
        --loglevel=INFO
}

# Perguntar o modo de execução
echo -e "${YELLOW}🎛️  Escolha o modo de execução:${NC}"
echo -e "   1) Headless (automático, $RUN_TIME)"
echo -e "   2) Interface Web (manual)"
read -p "   Digite sua escolha (1 ou 2): " choice

case $choice in
    1)
        run_headless_test
        ;;
    2)
        run_web_test
        ;;
    *)
        echo -e "${RED}❌ Escolha inválida. Executando em modo headless...${NC}"
        run_headless_test
        ;;
esac

# Verificar se o teste foi executado com sucesso
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Teste executado com sucesso!${NC}"
    echo -e "${BLUE}📊 Resultados salvos em:${NC}"
    echo -e "   HTML Report: $RESULTS_DIR/report-${TIMESTAMP}.html"
    echo -e "   CSV Results: $RESULTS_DIR/results-${TIMESTAMP}_stats.csv"
    echo -e "   Log: $LOG_FILE"
    
    # Abrir o relatório HTML se possível
    if command -v xdg-open &> /dev/null && [ -f "$RESULTS_DIR/report-${TIMESTAMP}.html" ]; then
        echo -e "${YELLOW}🌐 Abrindo relatório HTML...${NC}"
        xdg-open "$RESULTS_DIR/report-${TIMESTAMP}.html" 2>/dev/null || true
    fi
    
    # Mostrar resumo dos resultados
    echo -e "${BLUE}📈 Resumo dos Resultados:${NC}"
    if [ -f "$RESULTS_DIR/results-${TIMESTAMP}_stats.csv" ]; then
        echo -e "${GREEN}   Relatório CSV gerado com sucesso${NC}"
        echo -e "${YELLOW}   Verifique o arquivo para métricas detalhadas${NC}"
    fi
    
else
    echo -e "${RED}❌ Erro ao executar o teste Locust${NC}"
    echo -e "${YELLOW}📋 Verifique o log: $LOG_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}🎉 Teste de performance concluído!${NC}"
echo -e "${BLUE}============================================${NC}" 