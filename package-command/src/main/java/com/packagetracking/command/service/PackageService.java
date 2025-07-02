package com.packagetracking.command.service;

import com.packagetracking.command.constants.MessageConstants;
import com.packagetracking.command.dto.packages.PackageCancelResponse;
import com.packagetracking.command.dto.packages.PackageCreateEnrichedRequest;
import com.packagetracking.command.dto.packages.PackageCreateRequest;
import com.packagetracking.command.dto.packages.PackageResponse;
import com.packagetracking.command.entity.Package;
import com.packagetracking.command.entity.PackageStatus;
import com.packagetracking.command.repository.PackageJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {
    
    private final ExternalApiService externalApiService;
    private final PackageJpaRepository packageJpaRepository;

    @Transactional
    public PackageResponse createPackageSync(PackageCreateRequest request) {
        try {

            Boolean isHoliday = externalApiService.isHoliday(LocalDate.now()).block();
            String funFact = externalApiService.getDogFunFact().block();
            
            String packageId = generatePackageId();

            LocalDate deliveryDate = LocalDate.parse(request.getEstimatedDeliveryDate(), 
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            PackageCreateEnrichedRequest enrichedRequest = PackageCreateEnrichedRequest.builder()
                .id(packageId)
                .description(request.getDescription())
                .funFact(funFact)  
                .sender(request.getSender())
                .recipient(request.getRecipient())
                .isHolliday(isHoliday)
                .estimatedDeliveryDate(deliveryDate)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
            
            Package packageEntity = Package.builder()
                .id(enrichedRequest.getId())
                .description(enrichedRequest.getDescription())
                .funFact(enrichedRequest.getFunFact())
                .sender(enrichedRequest.getSender())
                .recipient(enrichedRequest.getRecipient())
                .isHolliday(enrichedRequest.getIsHolliday())
                .estimatedDeliveryDate(enrichedRequest.getEstimatedDeliveryDate())
                .status(PackageStatus.CREATED)
                .createdAt(enrichedRequest.getCreatedAt())
                .updatedAt(enrichedRequest.getUpdatedAt())
                .build();

            Package savedPackage = packageJpaRepository.save(packageEntity);
            
            log.info(MessageConstants.PACKAGE_CREATED_SUCCESS, packageId);
            
            return PackageResponse.builder()
                .id(savedPackage.getId())
                .description(savedPackage.getDescription())
                .sender(savedPackage.getSender())
                .recipient(savedPackage.getRecipient())
                .status(savedPackage.getStatus().name())
                .createdAt(savedPackage.getCreatedAt())
                .updatedAt(savedPackage.getUpdatedAt())
                .build();
                
        } catch (Exception e) {
            log.error("Erro ao criar pacote: {}", e.getMessage(), e);
            throw new RuntimeException(MessageConstants.ERROR_CREATING_PACKAGE, e);
        }
    }

    private String generatePackageId() {
        return MessageConstants.PACKAGE_ID_PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }

    @Transactional
    public PackageResponse updatePackageStatus(String id, String newStatus) {
        try {
            Package packageEntity = packageJpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(MessageConstants.PACKAGE_NOT_FOUND + id));
            
            validateStatusTransition(packageEntity.getStatus(), newStatus);
            
            PackageStatus status = PackageStatus.valueOf(newStatus);
            packageEntity.setStatus(status);
            
            if (status == PackageStatus.DELIVERED) {
                packageEntity.setDeliveredAt(Instant.now());
            }
            
            Package savedPackage = packageJpaRepository.save(packageEntity);
            
            log.info(MessageConstants.PACKAGE_STATUS_UPDATED_SUCCESS, id, newStatus);
            
            return PackageResponse.builder()
                .id(savedPackage.getId())
                .description(savedPackage.getDescription())
                .sender(savedPackage.getSender())
                .recipient(savedPackage.getRecipient())
                .status(savedPackage.getStatus().name())
                .createdAt(savedPackage.getCreatedAt())
                .updatedAt(savedPackage.getUpdatedAt())
                .build();
                
        } catch (Exception e) {
            log.error("Erro ao atualizar status do pacote {}: {}", id, e.getMessage(), e);
            throw new RuntimeException(MessageConstants.ERROR_UPDATING_PACKAGE_STATUS, e);
        }
    }

    /**
     * Valida transição de status do pacote
     * Fluxo obrigatório: CREATED -> IN_TRANSIT -> DELIVERED
     * Cancelamento: CREATED -> CANCELLED
     */
    private void validateStatusTransition(PackageStatus currentStatus, String newStatus) {
        PackageStatus targetStatus = PackageStatus.valueOf(newStatus);
        
        switch (currentStatus) {
            case CREATED:
                if (targetStatus != PackageStatus.IN_TRANSIT && 
                    targetStatus != PackageStatus.CANCELLED) {
                    throw new IllegalArgumentException(MessageConstants.CREATED_STATUS_TRANSITION_ERROR);
                }
                break;
            case IN_TRANSIT:
                if (targetStatus != PackageStatus.DELIVERED) {
                    throw new IllegalArgumentException(MessageConstants.IN_TRANSIT_STATUS_TRANSITION_ERROR);
                }
                break;
            case DELIVERED:
                throw new IllegalArgumentException(MessageConstants.DELIVERED_STATUS_ERROR);
            case CANCELLED:
                throw new IllegalArgumentException(MessageConstants.CANCELLED_STATUS_ERROR);
        }
    }

    @Transactional
    public PackageCancelResponse cancelPackage(String id) {
        try {
            Package packageEntity = packageJpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(MessageConstants.PACKAGE_NOT_FOUND + id));
            
            if (packageEntity.getStatus() != PackageStatus.CREATED) {
                throw new IllegalArgumentException(MessageConstants.CANNOT_CANCEL_PACKAGE_IN_TRANSIT);
            }
            
            packageEntity.setStatus(PackageStatus.CANCELLED);
            Package savedPackage = packageJpaRepository.save(packageEntity);
            
            log.info(MessageConstants.PACKAGE_CANCELED_SUCCESS, id);
            
            return PackageCancelResponse.builder()
                .id(savedPackage.getId())
                .status(savedPackage.getStatus().name())
                .dataAtualizacao(savedPackage.getUpdatedAt())
                .build();
                
        } catch (Exception e) {
            log.error("Erro ao cancelar pacote {}: {}", id, e.getMessage(), e);
            throw new RuntimeException(MessageConstants.ERROR_CANCELING_PACKAGE, e);
        }
    }
} 