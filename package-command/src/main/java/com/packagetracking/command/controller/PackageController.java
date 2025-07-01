package com.packagetracking.command.controller;

import com.packagetracking.command.dto.packages.PackageCancelResponse;
import com.packagetracking.command.dto.packages.PackageCreateRequest;
import com.packagetracking.command.dto.packages.PackageResponse;
import com.packagetracking.command.dto.packages.PackageUpdateRequest;
import com.packagetracking.command.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.modules.package-controllers-enabled", havingValue = "true", matchIfMissing = true)
public class PackageController {
    
    private final PackageService packageService;

    @PostMapping
    public ResponseEntity<PackageResponse> createPackage(@Valid @RequestBody PackageCreateRequest request) {
        PackageResponse response = packageService.createPackageSync(request);
        
        log.info("Pacote criado com sucesso: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/api/packages/" + response.getId())
                .header("X-Package-ID", response.getId())
                .body(response);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<PackageResponse> updatePackageStatus(
            @PathVariable String id, 
            @Valid @RequestBody PackageUpdateRequest request) {
        log.info("Atualizando status do pacote: {} para {}", id, request.getStatus());
        PackageResponse response = packageService.updatePackageStatus(id, request.getStatus());
        
        return ResponseEntity.ok()
                .header("X-Package-ID", id)
                .header("X-Status-Updated", request.getStatus())
                .body(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<PackageCancelResponse> cancelPackage(@PathVariable String id) {
        log.info("Cancelando pacote: {}", id);
        PackageCancelResponse response = packageService.cancelPackage(id);
        
        return ResponseEntity.ok()
                .header("X-Package-ID", id)
                .header("X-Cancellation-Date", response.getDataAtualizacao().toString())
                .body(response);
    }
    

} 