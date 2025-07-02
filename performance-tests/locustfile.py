#!/usr/bin/env python3
"""
Teste de Performance com Locust - Foco em RPS dos GETs
Sistema de Rastreamento de Pacotes
"""

import json
import random
from locust import HttpUser, task, between, events


class GetEndpointsUser(HttpUser):
    """
    Usuário focado apenas em endpoints GET para medir RPS
    """
    
    # Tempo entre requisições (0.1-0.5 segundos para maximizar RPS)
    wait_time = between(0.1, 0.5)
    
    # IDs de pacotes para teste (baseados nos dados existentes)
    package_ids = [
        "pacote-026fbedc",
        "pacote-19faf864", 
        "pacote-34c6d8e1",
        "pacote-43bba473",
        "pacote-62e16dd4"
    ]
    
    def on_start(self):
        """Executado quando o usuário inicia"""
        self.client.verify = False  # Desabilitar verificação SSL para testes locais
        
    @task(4)  # Peso 4 - mais frequente
    def get_package_by_id(self):
        """Consulta pacote por ID - alta frequência"""
        package_id = random.choice(self.package_ids)
        
        with self.client.get(
            f"/api/packages/{package_id}",
            name="/api/packages/{id}",
            catch_response=True
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status code: {response.status_code}")
    
    @task(3)  # Peso 3
    def get_packages_list(self):
        """Lista todos os pacotes"""
        with self.client.get(
            "/api/packages",
            name="/api/packages",
            catch_response=True
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status code: {response.status_code}")
    
    @task(2)  # Peso 2
    def get_packages_paginated(self):
        """Lista pacotes paginados"""
        page = random.randint(0, 2)  # Páginas 0, 1, 2
        size = random.choice([5, 10, 20])  # Tamanhos diferentes
        
        with self.client.get(
            f"/api/packages/page?page={page}&size={size}",
            name="/api/packages/page",
            catch_response=True
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status code: {response.status_code}")
    
    @task(1)  # Peso 1 - menos frequente
    def get_packages_with_filters(self):
        """Lista pacotes com filtros"""
        filters = [
            "?sender=Empresa%20Teste",
            "?recipient=João%20Silva",
            "?sender=Empresa%20Teste&recipient=João%20Silva"
        ]
        
        filter_query = random.choice(filters)
        
        with self.client.get(
            f"/api/packages{filter_query}",
            name="/api/packages (com filtros)",
            catch_response=True
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Status code: {response.status_code}")


# Event listeners para métricas customizadas
@events.request.add_listener
def my_request_handler(request_type, name, response_time, response_length, response, context, exception, start_time, url, **kwargs):
    """Listener para capturar métricas customizadas"""
    if exception:
        print(f"❌ Erro na requisição {name}: {exception}")
    elif response and response.status_code >= 400:
        print(f"⚠️  Requisição {name} retornou status {response.status_code}")


@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Executado quando o teste inicia"""
    print("🚀 Iniciando teste de RPS - Endpoints GET")
    print(f"📊 Target: {environment.host}")
    print(f"👥 Usuários: {environment.runner.user_count if environment.runner else 'N/A'}")


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Executado quando o teste para"""
    print("✅ Teste de RPS concluído") 