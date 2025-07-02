package com.packagetracking.query.controller;

import com.packagetracking.query.service.DatabaseHealthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/database")
@Slf4j
public class DatabaseHealthController {

    @Autowired
    private DatabaseHealthService databaseHealthService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("currentDatabase", databaseHealthService.getCurrentDatabase());
        health.put("slaveHealthy", databaseHealthService.isSlaveHealthy());
        health.put("masterHealthy", databaseHealthService.isMasterHealthy());
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }

    @PostMapping("/failover/master")
    public ResponseEntity<Map<String, String>> forceFailoverToMaster() {
        log.info("Failover manual para master solicitado");
        databaseHealthService.forceFailoverToMaster();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Failover para master executado");
        response.put("currentDatabase", databaseHealthService.getCurrentDatabase());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/switch/slave")
    public ResponseEntity<Map<String, String>> switchToSlave() {
        log.info("Alteração manual para slave solicitada");
        databaseHealthService.switchToSlave();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Alteração para slave executada");
        response.put("currentDatabase", databaseHealthService.getCurrentDatabase());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDetailedStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("currentDatabase", databaseHealthService.getCurrentDatabase());
        status.put("slaveHealthy", databaseHealthService.isSlaveHealthy());
        status.put("masterHealthy", databaseHealthService.isMasterHealthy());
        status.put("failoverEnabled", true);
        status.put("autoRecovery", true);
        status.put("timestamp", System.currentTimeMillis());
        
        Map<String, String> strategy = new HashMap<>();
        strategy.put("primary", "SLAVE");
        strategy.put("fallback", "MASTER");
        strategy.put("autoFailover", "true");
        strategy.put("autoRecovery", "true");
        status.put("strategy", strategy);
        
        return ResponseEntity.ok(status);
    }
} 