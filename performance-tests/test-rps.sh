#!/bin/bash

# Script para testar RPS dos endpoints GET
# Sistema de Rastreamento de Pacotes

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurações para teste de RPS
LOCUSTFILE="locustfile.py"
RESULTS_DIR="locust-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="rps-test-${TIMESTAMP}.log"
HOST="http://localhost:8083"

echo -e "${BLUE}🚀 Teste de RPS - Endpoints GET${NC}"
echo -e "${BLUE}================================${NC}"

# Ativar ambiente virtual
echo -e "${YELLOW}🔧 Ativando ambiente virtual...${NC}"
source locust-env/bin/activate

# Verificar se os serviços estão rodando
echo -e "${YELLOW}🔍 Verificando se os serviços estão rodando...${NC}"

if curl -s http://localhost:8083/actuator/health > /dev/null; then
    echo -e "${GREEN}✅ Package Query (8083) está rodando${NC}"
else
    echo -e "${RED}❌ Package Query (8083) não está respondendo${NC}"
    exit 1
fi

echo -e "${GREEN}🎯 Serviço está rodando!${NC}"

# Criar diretório de resultados
mkdir -p "$RESULTS_DIR"

# Configurações de teste
echo -e "${YELLOW}📋 Configurações do Teste RPS:${NC}"
echo -e "   Locustfile: $LOCUSTFILE"
echo -e "   Host: $HOST"
echo -e "   Foco: Endpoints GET apenas"
echo -e "   Wait Time: 0.1-0.5s entre requisições"
echo -e "   Results Dir: $RESULTS_DIR"
echo -e "   Log File: $LOG_FILE"

echo -e "${BLUE}🎛️  Escolha o cenário de teste:${NC}"
echo -e "   1) Teste Rápido (10 usuários, 30s)"
echo -e "   2) Teste Médio (20 usuários, 1m)"
echo -e "   3) Teste Agressivo (50 usuários, 2m)"
echo -e "   4) Interface Web (manual)"
read -p "   Digite sua escolha (1-4): " choice

case $choice in
    1)
        USERS=10
        SPAWN_RATE=5
        RUN_TIME="30s"
        echo -e "${BLUE}⚡ Executando teste rápido...${NC}"
        ;;
    2)
        USERS=20
        SPAWN_RATE=10
        RUN_TIME="1m"
        echo -e "${BLUE}⚡ Executando teste médio...${NC}"
        ;;
    3)
        USERS=50
        SPAWN_RATE=20
        RUN_TIME="2m"
        echo -e "${BLUE}⚡ Executando teste agressivo...${NC}"
        ;;
    4)
        echo -e "${BLUE}🌐 Iniciando interface web...${NC}"
        echo -e "${YELLOW}📊 Acesse: http://localhost:8089${NC}"
        echo -e "${YELLOW}⏹️  Pressione Ctrl+C para parar${NC}"
        
        locust \
            --host="$HOST" \
            --locustfile="$LOCUSTFILE" \
            --logfile="$LOG_FILE" \
            --loglevel=INFO
        exit 0
        ;;
    *)
        echo -e "${RED}❌ Escolha inválida. Executando teste médio...${NC}"
        USERS=20
        SPAWN_RATE=10
        RUN_TIME="1m"
        ;;
esac

echo -e "${YELLOW}⏱️  Executando por $RUN_TIME...${NC}"

# Executar teste headless
locust \
    --headless \
    --host="$HOST" \
    --users="$USERS" \
    --spawn-rate="$SPAWN_RATE" \
    --run-time="$RUN_TIME" \
    --html="$RESULTS_DIR/rps-report-${TIMESTAMP}.html" \
    --csv="$RESULTS_DIR/rps-results-${TIMESTAMP}" \
    --logfile="$LOG_FILE" \
    --loglevel=INFO \
    --locustfile="$LOCUSTFILE"

# Verificar se o teste foi executado com sucesso
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Teste RPS executado com sucesso!${NC}"
    echo -e "${BLUE}📊 Resultados salvos em:${NC}"
    echo -e "   HTML Report: $RESULTS_DIR/rps-report-${TIMESTAMP}.html"
    echo -e "   CSV Results: $RESULTS_DIR/rps-results-${TIMESTAMP}_stats.csv"
    echo -e "   Log: $LOG_FILE"
    
    # Mostrar resumo dos resultados
    echo -e "${BLUE}📈 Resumo dos Resultados RPS:${NC}"
    if [ -f "$RESULTS_DIR/rps-results-${TIMESTAMP}_stats.csv" ]; then
        echo -e "${GREEN}   Relatório CSV gerado com sucesso${NC}"
        
        # Tentar mostrar algumas métricas básicas
        if command -v tail &> /dev/null; then
            echo -e "${YELLOW}   Últimas linhas do relatório:${NC}"
            tail -5 "$RESULTS_DIR/rps-results-${TIMESTAMP}_stats.csv" 2>/dev/null || true
        fi
    fi
    
    # Abrir o relatório HTML se possível
    if command -v xdg-open &> /dev/null && [ -f "$RESULTS_DIR/rps-report-${TIMESTAMP}.html" ]; then
        echo -e "${YELLOW}🌐 Abrindo relatório HTML...${NC}"
        xdg-open "$RESULTS_DIR/rps-report-${TIMESTAMP}.html" 2>/dev/null || true
    fi
    
else
    echo -e "${RED}❌ Erro ao executar o teste RPS${NC}"
    echo -e "${YELLOW}📋 Verifique o log: $LOG_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}🎉 Teste de RPS concluído!${NC}"
echo -e "${BLUE}================================${NC}" 