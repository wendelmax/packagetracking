#!/bin/bash

echo "=== EXECUTANDO TESTES DO MÓDULO PACKAGE-TEST ==="
echo "Data/Hora: $(date)"
echo ""

# Configurações
TEST_MODULE="package-test"
REPORTS_DIR="test-reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Criar diretório de relatórios
mkdir -p ${REPORTS_DIR}

# Função para executar testes com relatório
run_tests() {
    local test_type=$1
    local test_pattern=$2
    local report_file="${REPORTS_DIR}/${test_type}-${TIMESTAMP}.xml"
    
    echo "=== EXECUTANDO ${test_type} ==="
    echo "Padrão: ${test_pattern}"
    echo "Relatório: ${report_file}"
    echo ""
    
    cd ${TEST_MODULE}
    
    mvn test -Dtest="${test_pattern}" \
        -Dmaven.test.failure.ignore=true \
        -Dsurefire.reportFormat=xml \
        -Dsurefire.reportDirectory=../${REPORTS_DIR} \
        -q
    
    if [ $? -eq 0 ]; then
        echo "SUCCESS: ${test_type} executados com sucesso!"
    else
        echo "WARNING: ${test_type} tiveram alguns problemas"
    fi
    
    cd ..
    echo ""
}

# Função para executar testes de performance
run_performance_tests() {
    echo "=== EXECUTANDO TESTES DE PERFORMANCE ==="
    echo "Relatório: ${REPORTS_DIR}/performance-${TIMESTAMP}.txt"
    echo ""
    
    cd ${TEST_MODULE}
    
    # Executar testes de performance com output detalhado
    echo "=== TESTES DE PERFORMANCE - $(date) ===" > ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    echo "Sistema: $(uname -a)" >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    echo "Java: $(java -version 2>&1 | head -1)" >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    echo "" >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    
    # Testes de performance
    echo "Executando teste de latência..."
    echo "=== TESTE DE LATÊNCIA ===" >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    mvn test -Dtest="*PerformanceTest#latencyTest*" -q >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt 2>&1
    echo "" >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    
    echo "Executando teste de throughput..."
    echo "=== TESTE DE THROUGHPUT ===" >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    mvn test -Dtest="*PerformanceTest#throughputTest*" -q >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt 2>&1
    echo "" >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    
    echo "Executando teste de stress..."
    echo "=== TESTE DE STRESS ===" >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    mvn test -Dtest="*PerformanceTest#stressTest*" -q >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt 2>&1
    echo "" >> ../${REPORTS_DIR}/performance-${TIMESTAMP}.txt
    
            echo "SUCCESS: Testes de performance concluídos!"
    cd ..
    echo ""
}

# Menu principal
show_menu() {
    echo "Escolha o tipo de teste:"
    echo "1) Todos os testes"
    echo "2) Testes de integração"
    echo "3) Testes E2E"
    echo "4) Testes de performance"
    echo "5) Teste específico"
    echo "6) Sair"
    echo ""
    read -p "Digite sua opção (1-6): " choice
}

# Executar teste específico
run_specific_test() {
    echo ""
    echo "Testes disponíveis:"
    echo "1) EventIngestionIntegrationTest"
    echo "2) DatabaseReplicationIntegrationTest"
    echo "3) PackageTrackingE2ETest"
    echo "4) SystemPerformanceTest"
    echo ""
    read -p "Digite o nome do teste: " test_name
    
    if [ -n "$test_name" ]; then
        echo "Executando teste: ${test_name}"
        cd ${TEST_MODULE}
        mvn test -Dtest="${test_name}" -q
        cd ..
    else
        echo "Nome do teste não informado"
    fi
}

# Verificar se o módulo existe
if [ ! -d "${TEST_MODULE}" ]; then
    echo "ERROR: Módulo ${TEST_MODULE} não encontrado!"
    echo "Certifique-se de estar no diretório raiz do projeto."
    exit 1
fi

# Verificar se Maven está disponível
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven não encontrado!"
    echo "Instale o Maven para executar os testes."
    exit 1
fi

# Menu interativo
while true; do
    show_menu
    
    case $choice in
        1)
            echo "Executando todos os testes..."
            run_tests "TODOS" "*Test"
            run_performance_tests
            ;;
        2)
            run_tests "INTEGRAÇÃO" "*IntegrationTest"
            ;;
        3)
            run_tests "E2E" "*E2ETest"
            ;;
        4)
            run_performance_tests
            ;;
        5)
            run_specific_test
            ;;
        6)
            echo "Saindo..."
            break
            ;;
        *)
            echo "Opção inválida. Tente novamente."
            ;;
    esac
    
    echo ""
    read -p "Pressione Enter para continuar..."
    echo ""
done

echo ""
echo "=== RESUMO ==="
echo "Relatórios gerados em: ${REPORTS_DIR}/"
echo "Timestamp: ${TIMESTAMP}"
echo ""
echo "Arquivos gerados:"
ls -la ${REPORTS_DIR}/*${TIMESTAMP}* 2>/dev/null || echo "Nenhum relatório gerado"

echo ""
echo "=== ACESSO AOS RELATÓRIOS ==="
echo "Para visualizar relatórios XML:"
echo "  - Use ferramentas como Jenkins, SonarQube ou plugins de IDE"
echo ""
echo "Para visualizar relatórios de performance:"
echo "  - cat ${REPORTS_DIR}/performance-${TIMESTAMP}.txt"
echo ""
echo "COMPLETED: Execução de testes concluída!" 