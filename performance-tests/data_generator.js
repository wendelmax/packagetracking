#!/usr/bin/env node

/**
 * Gerador de dados de teste para Package Tracking (Node.js)
 * Gera pacotes e eventos via API REST
 * Uso: node data_generator.js --packages 1000 --events 5 --url http://localhost:8080
 */

const fs = require('fs');
const axios = require('axios');
const { program } = require('commander');
const { v4: uuidv4 } = require('uuid');

// Argumentos de linha de comando
program
  .option('--packages <number>', 'Quantidade de pacotes', '1000')
  .option('--events <number>', 'Eventos por pacote', '5')
  .option('--url <string>', 'URL base da API', 'http://localhost:8080');

program.parse(process.argv);
const options = program.opts();

const PACKAGES_COUNT = parseInt(options.packages, 10);
const EVENTS_PER_PACKAGE = parseInt(options.events, 10);
const BASE_URL = options.url.replace(/\/$/, '');

// Utilitários para geração de dados
const randomFromArray = arr => arr[Math.floor(Math.random() * arr.length)];
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

const senders = ['João', 'Maria', 'Carlos', 'Ana', 'Pedro', 'Fernanda', 'Lucas', 'Juliana'];
const recipients = ['Rafael', 'Beatriz', 'Gabriel', 'Larissa', 'Marcos', 'Patrícia', 'Bruno', 'Camila'];
const descriptions = ['Livros', 'Roupas', 'Eletrônicos', 'Brinquedos', 'Documentos', 'Acessórios', 'Calçados', 'Alimentos'];
const statusList = ['IN_TRANSIT', 'DELIVERED', 'CANCELLED', 'PENDING'];

async function createPackage(pkg) {
  try {
    const res = await axios.post(`${BASE_URL}/packages`, pkg, { timeout: 5000 });
    return res.data;
  } catch (err) {
    return { error: err.message };
  }
}

async function createEvent(packageId, event) {
  try {
    const res = await axios.post(`${BASE_URL}/packages/${packageId}/events`, event, { timeout: 5000 });
    return res.data;
  } catch (err) {
    return { error: err.message };
  }
}

async function main() {
  const start = Date.now();
  const generated = [];
  let totalEvents = 0;

  console.log(`Iniciando geração de ${PACKAGES_COUNT} pacotes, ${EVENTS_PER_PACKAGE} eventos por pacote...`);

  for (let i = 0; i < PACKAGES_COUNT; i++) {
    const pkg = {
      sender: randomFromArray(senders),
      recipient: randomFromArray(recipients),
      description: randomFromArray(descriptions),
      status: randomFromArray(statusList),
      trackingCode: uuidv4().slice(0, 12).toUpperCase()
    };

    const createdPkg = await createPackage(pkg);
    if (createdPkg && createdPkg.id) {
      const packageId = createdPkg.id;
      const events = [];
      for (let j = 0; j < EVENTS_PER_PACKAGE; j++) {
        const event = {
          type: 'STATUS_UPDATE',
          status: randomFromArray(statusList),
          description: `Evento ${j + 1} para pacote ${packageId}`,
          timestamp: new Date(Date.now() - Math.floor(Math.random() * 100000000)).toISOString()
        };
        const createdEvent = await createEvent(packageId, event);
        events.push(createdEvent);
        totalEvents++;
        await sleep(10); // evitar sobrecarga
      }
      generated.push({ package: createdPkg, events });
    } else {
      generated.push({ package: { ...pkg, error: createdPkg.error || 'Erro desconhecido' }, events: [] });
    }

    if ((i + 1) % 50 === 0 || i === PACKAGES_COUNT - 1) {
      console.log(`Pacotes criados: ${i + 1}/${PACKAGES_COUNT}`);
    }
    await sleep(5); // evitar sobrecarga
  }

  // Salvar resultado
  const result = {
    total_packages: generated.length,
    total_events: totalEvents,
    generation_time: ((Date.now() - start) / 1000).toFixed(2),
    data: generated
  };
  fs.writeFileSync('generated_packages.json', JSON.stringify(result, null, 2));
  console.log(`\n✅ Geração concluída: ${generated.length} pacotes, ${totalEvents} eventos.`);
  console.log(`Tempo total: ${result.generation_time}s`);
  console.log('Arquivo salvo: generated_packages.json');
}

main().catch(err => {
  console.error('Erro na geração:', err);
  process.exit(1);
}); 