import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// Métricas customizadas
const errorRate = new Rate('errors');
const packageQueryTrend = new Trend('package_query_duration');
const packageListTrend = new Trend('package_list_duration');

// Configuração do teste
export const options = {
  stages: [
    { duration: '2m', target: 10 },  // Rampa de subida
    { duration: '5m', target: 50 },  // Carga constante
    { duration: '2m', target: 100 }, // Pico de carga
    { duration: '5m', target: 100 }, // Carga máxima
    { duration: '2m', target: 0 },   // Rampa de descida
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% das requisições devem ser < 500ms
    http_req_failed: ['rate<0.1'],    // Taxa de erro < 10%
    errors: ['rate<0.1'],             // Taxa de erro customizada < 10%
  },
};

// Carregar IDs dos pacotes gerados
let packageIds = [];
try {
  const packagesData = JSON.parse(open('./generated_packages.json'));
  packageIds = packagesData.packages || [];
  console.log(`Carregados ${packageIds.length} IDs de pacotes para teste`);
} catch (e) {
  console.log('Arquivo generated_packages.json não encontrado, usando IDs mock');
  packageIds = [
    'pkg-12345678', 'pkg-87654321', 'pkg-11111111', 'pkg-22222222',
    'pkg-33333333', 'pkg-44444444', 'pkg-55555555', 'pkg-66666666'
  ];
}

// URLs base
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083';  // Porta do package-query
const PACKAGE_QUERY_URL = `${BASE_URL}/api/packages`;

// Função para gerar dados aleatórios
function getRandomPackageId() {
  return packageIds[Math.floor(Math.random() * packageIds.length)];
}

function getRandomSender() {
  const senders = [
    'Amazon Brasil', 'Mercado Livre', 'Magazine Luiza', 'Americanas',
    'Casas Bahia', 'Extra', 'Ponto Frio', 'Submarino', 'Netshoes'
  ];
  return senders[Math.floor(Math.random() * senders.length)];
}

function getRandomRecipient() {
  const recipients = [
    'João Silva', 'Maria Santos', 'Pedro Oliveira', 'Ana Costa',
    'Carlos Ferreira', 'Lucia Rodrigues', 'Roberto Almeida'
  ];
  return recipients[Math.floor(Math.random() * recipients.length)];
}

// Cenários de teste
export default function () {
  const scenario = Math.random();
  
  if (scenario < 0.4) {
    // 40% - Consulta de pacote individual com eventos
    testPackageQueryWithEvents();
  } else if (scenario < 0.7) {
    // 30% - Consulta de pacote individual sem eventos
    testPackageQueryWithoutEvents();
  } else if (scenario < 0.85) {
    // 15% - Lista de pacotes com filtros
    testPackageListWithFilters();
  } else {
    // 15% - Lista de pacotes paginada
    testPackageListPaginated();
  }
  
  sleep(1);
}

function testPackageQueryWithEvents() {
  const packageId = getRandomPackageId();
  const url = `${PACKAGE_QUERY_URL}/${packageId}?includeEvents=true`;
  
  const startTime = Date.now();
  const response = http.get(url, {
    headers: {
      'Accept': 'application/json',
      'User-Agent': 'k6-load-test'
    }
  });
  const duration = Date.now() - startTime;
  
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'has package data': (r) => r.json('id') === packageId,
    'has events array': (r) => Array.isArray(r.json('events')),
  });
  
  packageQueryTrend.add(duration);
  errorRate.add(!success);
  
  if (!success) {
    console.log(`Erro na consulta de pacote com eventos: ${response.status} - ${response.body}`);
  }
}

function testPackageQueryWithoutEvents() {
  const packageId = getRandomPackageId();
  const url = `${PACKAGE_QUERY_URL}/${packageId}?includeEvents=false`;
  
  const startTime = Date.now();
  const response = http.get(url, {
    headers: {
      'Accept': 'application/json',
      'User-Agent': 'k6-load-test'
    }
  });
  const duration = Date.now() - startTime;
  
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 300ms': (r) => r.timings.duration < 300,
    'has package data': (r) => r.json('id') === packageId,
    'no events array': (r) => !r.json('events') || r.json('events') === null,
  });
  
  packageQueryTrend.add(duration);
  errorRate.add(!success);
  
  if (!success) {
    console.log(`Erro na consulta de pacote sem eventos: ${response.status} - ${response.body}`);
  }
}

function testPackageListWithFilters() {
  const useSender = Math.random() > 0.5;
  const useRecipient = Math.random() > 0.5;
  
  let url = PACKAGE_QUERY_URL;
  const params = [];
  
  if (useSender) {
    params.push(`sender=${encodeURIComponent(getRandomSender())}`);
  }
  if (useRecipient) {
    params.push(`recipient=${encodeURIComponent(getRandomRecipient())}`);
  }
  
  if (params.length > 0) {
    url += '?' + params.join('&');
  }
  
  const startTime = Date.now();
  const response = http.get(url, {
    headers: {
      'Accept': 'application/json',
      'User-Agent': 'k6-load-test'
    }
  });
  const duration = Date.now() - startTime;
  
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 1000ms': (r) => r.timings.duration < 1000,
    'has packages array': (r) => Array.isArray(r.json()),
    'has X-Total-Count header': (r) => r.headers['X-Total-Count'] !== undefined,
  });
  
  packageListTrend.add(duration);
  errorRate.add(!success);
  
  if (!success) {
    console.log(`Erro na consulta de lista de pacotes: ${response.status} - ${response.body}`);
  }
}

function testPackageListPaginated() {
  const page = Math.floor(Math.random() * 10);
  const size = Math.floor(Math.random() * 20) + 10; // 10-30
  const useSender = Math.random() > 0.5;
  
  let url = `${PACKAGE_QUERY_URL}/page?page=${page}&size=${size}`;
  
  if (useSender) {
    url += `&sender=${encodeURIComponent(getRandomSender())}`;
  }
  
  const startTime = Date.now();
  const response = http.get(url, {
    headers: {
      'Accept': 'application/json',
      'User-Agent': 'k6-load-test'
    }
  });
  const duration = Date.now() - startTime;
  
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 800ms': (r) => r.timings.duration < 800,
    'has pagination data': (r) => r.json('content') !== undefined,
    'has X-Total-Elements header': (r) => r.headers['X-Total-Elements'] !== undefined,
    'has X-Total-Pages header': (r) => r.headers['X-Total-Pages'] !== undefined,
  });
  
  packageListTrend.add(duration);
  errorRate.add(!success);
  
  if (!success) {
    console.log(`Erro na consulta paginada: ${response.status} - ${response.body}`);
  }
}

// Função executada no início do teste
export function setup() {
  console.log('Iniciando teste de performance da API de consulta de pacotes');
  console.log(`URL base: ${BASE_URL}`);
  console.log(`Pacotes disponíveis para teste: ${packageIds.length}`);
  
  // Verificar se a API está respondendo
  const healthCheck = http.get(`${BASE_URL}/actuator/health`);
  if (healthCheck.status !== 200) {
    throw new Error(`API não está respondendo: ${healthCheck.status}`);
  }
  
  console.log('API está respondendo corretamente');
  return { packageIds };
}

// Função executada no final do teste
export function teardown(data) {
  console.log('Teste de performance concluído');
  console.log(`Total de pacotes testados: ${data.packageIds.length}`);
} 