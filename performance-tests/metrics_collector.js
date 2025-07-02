#!/usr/bin/env node

/**
 * Coletor de métricas da aplicação via Spring Boot Actuator (Node.js)
 * Uso: node metrics_collector.js --url http://localhost:8083 --interval 5 --duration 60 --output metrics.json
 */

const fs = require('fs');
const axios = require('axios');
const { program } = require('commander');

program
  .option('--url <string>', 'URL da aplicação', 'http://localhost:8083')
  .option('--interval <number>', 'Intervalo de coleta em segundos', '5')
  .option('--duration <number>', 'Duração da coleta em segundos', '300')
  .option('--output <string>', 'Arquivo de saída para as métricas');

program.parse(process.argv);
const options = program.opts();

const BASE_URL = options.url.replace(/\/$/, '');
const INTERVAL = parseInt(options.interval, 10);
const DURATION = parseInt(options.duration, 10);
const OUTPUT = options.output || `application_metrics_${new Date().toISOString().replace(/[-:T.]/g, '').slice(0, 14)}.json`;

const endpoints = {
  jvm_memory: '/actuator/metrics/jvm.memory.used',
  jvm_gc: '/actuator/metrics/jvm.gc.pause',
  http_requests: '/actuator/metrics/http.server.requests',
  cache_stats: '/actuator/metrics/cache.gets',
  hikari_pool: '/actuator/metrics/hikaricp.connections',
  redis_metrics: '/actuator/metrics/redis.connections',
  circuit_breaker: '/actuator/metrics/resilience4j.circuitbreaker.calls',
  system_cpu: '/actuator/metrics/system.cpu.usage',
  system_memory: '/actuator/metrics/jvm.memory.max',
  thread_info: '/actuator/metrics/jvm.threads.live',
  info: '/actuator/info',
  health: '/actuator/health'
};

async function fetchMetric(endpoint) {
  try {
    const res = await axios.get(BASE_URL + endpoint, { timeout: 5000 });
    return res.data;
  } catch (err) {
    return { error: err.message };
  }
}

async function collectAllMetrics() {
  const timestamp = new Date().toISOString();
  const metrics = {};
  for (const [name, endpoint] of Object.entries(endpoints)) {
    metrics[name] = await fetchMetric(endpoint);
  }
  // Coletar métricas detalhadas de cache
  if (metrics.cache_stats && metrics.cache_stats.availableTags) {
    for (const tag of metrics.cache_stats.availableTags) {
      if (tag.tag === 'cache') {
        for (const value of tag.values) {
          const detail = await fetchMetric(`/actuator/metrics/cache.gets?tag=cache:${value}`);
          metrics[`cache_${value}_gets`] = detail;
        }
      }
    }
  }
  // Coletar métricas detalhadas de circuit breaker
  if (metrics.circuit_breaker && metrics.circuit_breaker.availableTags) {
    for (const tag of metrics.circuit_breaker.availableTags) {
      if (tag.tag === 'kind') {
        for (const value of tag.values) {
          const detail = await fetchMetric(`/actuator/metrics/resilience4j.circuitbreaker.calls?tag=kind:${value}`);
          metrics[`circuit_breaker_${value}`] = detail;
        }
      }
    }
  }
  return { timestamp, metrics };
}

function summarize(metricsArr) {
  const summary = {};
  if (!metricsArr.length) return summary;
  const keys = Object.keys(metricsArr[0].metrics);
  for (const key of keys) {
    const values = metricsArr.map(m => {
      const metric = m.metrics[key];
      if (metric && metric.measurements && metric.measurements[0]) {
        return metric.measurements[0].value;
      }
      if (metric && typeof metric.value === 'number') {
        return metric.value;
      }
      return null;
    }).filter(v => typeof v === 'number');
    if (values.length) {
      summary[key] = {
        count: values.length,
        min: Math.min(...values),
        max: Math.max(...values),
        avg: values.reduce((a, b) => a + b, 0) / values.length,
        last: values[values.length - 1]
      };
    }
  }
  return summary;
}

async function main() {
  const metricsArr = [];
  const start = Date.now();
  const end = start + DURATION * 1000;
  console.log(`Iniciando coleta de métricas a cada ${INTERVAL}s por ${DURATION}s...`);
  while (Date.now() < end) {
    const m = await collectAllMetrics();
    metricsArr.push(m);
    await new Promise(r => setTimeout(r, INTERVAL * 1000));
  }
  // Salvar arquivo
  const result = {
    collection_info: {
      start_time: metricsArr[0]?.timestamp,
      end_time: metricsArr[metricsArr.length - 1]?.timestamp,
      total_samples: metricsArr.length,
      collection_interval: INTERVAL
    },
    metrics: metricsArr
  };
  fs.writeFileSync(OUTPUT, JSON.stringify(result, null, 2));
  console.log(`\n✅ Métricas salvas em: ${OUTPUT}`);
  // Resumo
  const summary = summarize(metricsArr);
  console.log('\nResumo das principais métricas:');
  for (const [key, val] of Object.entries(summary)) {
    console.log(`- ${key}: média=${val.avg.toFixed(2)}, máx=${val.max.toFixed(2)}, último=${val.last.toFixed(2)}`);
  }
}

main().catch(err => {
  console.error('Erro na coleta de métricas:', err);
  process.exit(1); 