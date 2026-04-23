package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.AuditLog;
import ma.pharmacie.pharmastock.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
@Tag(name = "Audit", description = "Journal d'audit des opérations")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "Consulter le journal d'audit")
    public ResponseEntity<Page<AuditLog>> getAll(
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        LocalDateTime from = null;
        LocalDateTime to = null;

        if (dateFrom != null && !dateFrom.isBlank()) {
            from = LocalDate.parse(dateFrom).atStartOfDay();
        }

        if (dateTo != null && !dateTo.isBlank()) {
            to = LocalDate.parse(dateTo).atTime(23, 59, 59);
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<AuditLog> result;

        if (utilisateurId != null && from != null && to != null) {
            result = auditLogRepository.findByUtilisateurIdAndTimestampBetweenWithUtilisateur(
                    utilisateurId, from, to, pageable
            );
        } else if (utilisateurId != null && from != null) {
            result = auditLogRepository.findByUtilisateurIdAndTimestampGreaterThanEqualWithUtilisateur(
                    utilisateurId, from, pageable
            );
        } else if (utilisateurId != null && to != null) {
            result = auditLogRepository.findByUtilisateurIdAndTimestampLessThanEqualWithUtilisateur(
                    utilisateurId, to, pageable
            );
        } else if (utilisateurId != null) {
            result = auditLogRepository.findByUtilisateurIdWithUtilisateur(utilisateurId, pageable);
        } else if (from != null && to != null) {
            result = auditLogRepository.findByTimestampBetweenWithUtilisateur(from, to, pageable);
        } else if (from != null) {
            result = auditLogRepository.findByTimestampGreaterThanEqualWithUtilisateur(from, pageable);
        } else if (to != null) {
            result = auditLogRepository.findByTimestampLessThanEqualWithUtilisateur(to, pageable);
        } else {
            result = auditLogRepository.findAllWithUtilisateur(pageable);
        }

        return ResponseEntity.ok(result);
    }
}